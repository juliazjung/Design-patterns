import java.rmi.*;
import java.rmi.server.UnicastRemoteObject;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

public class No extends UnicastRemoteObject implements NoInterface {
    // Estado do nó
    private String idNo;
    private List<NoInterface> vizinhos;
    private AtomicInteger contadorSequencia;
    private EstadoNo estado;

    // Controle de mensagens
    private Map<String, Set<String>> mensagensEntregues;
    private Map<String, Mensagem> mensagensPendentes;
    private BlockingQueue<Mensagem> filaMensagens;

    // Infraestrutura
    private ScheduledExecutorService executor;

    // Para Atomic Broadcast (FIFO)
    private final Map<String, Integer> ultimaSequencia; // Controla a última sequência recebida de cada nó
    private final Map<String, ConcurrentSkipListMap<Integer, Mensagem>> mensagensForaDeOrdem;

    // Variáveis para controle de falhas - Atualizado
    private EstrategiaFalha estrategiaFalha;

    private static long ACK_TIMEOUT_MS = 3000; // 3 segundos
    private static final int MAX_RETRIES = 3;

    // NOVO: Gerenciador de eventos
    private GerenciadorEventos gerenciadorEventos;

    public No(String idNo) throws RemoteException {
        this.idNo = idNo;
        this.vizinhos = new CopyOnWriteArrayList<>();
        this.contadorSequencia = new AtomicInteger(0);
        this.estado = new EstadoAtivo();

        this.mensagensEntregues = new ConcurrentHashMap<>();
        this.mensagensPendentes = new ConcurrentHashMap<>();
        this.ultimaSequencia = new ConcurrentHashMap<>();
        this.mensagensForaDeOrdem = new ConcurrentHashMap<>();
        this.filaMensagens = new PriorityBlockingQueue<>(11,
                Comparator.comparingLong(Mensagem::getTimestamp));

        this.executor = Executors.newScheduledThreadPool(3);

        this.estrategiaFalha = new SemFalha();

        this.gerenciadorEventos = new GerenciadorEventos();
        this.gerenciadorEventos.adicionarObservador(new LogObserver());

        executor.submit(this::processarMensagens);
        executor.submit(this::enviarHeartbeats);

        GerenciadorLog.getInstancia().registrar(idNo, 
            "Nó iniciado no estado: " + estado.getNomeEstado());
    }

    public void broadcast(String conteudo) {
        try {
            Mensagem msg = MensagemFactory.criarMensagem(
                idNo, 
                contadorSequencia.incrementAndGet(), 
                conteudo
            );
            
            // Delega ao estado
            estado.enviarMensagem(msg, this);
            
        } catch (Exception e) {
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Erro ao broadcast: " + e.getMessage());
        }
    }

    /**
     * NOVO: Método auxiliar chamado pelo estado
     */
    public void processarEnvioMensagem(Mensagem msg) throws RemoteException {
        GerenciadorLog.getInstancia().registrar(idNo, "Broadcasting mensagem: " + msg);
        mensagensPendentes.put(msg.getUniqueId(), msg);
        executor.schedule(() -> verificarACK(msg, 0), ACK_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        for (NoInterface vizinho : vizinhos) {
            try {
                vizinho.receive(msg);
            } catch (RemoteException e) {
                GerenciadorLog.getInstancia().registrar(idNo, 
                    "Falha ao enviar para vizinho: " + e.getMessage());
                tratarFalhaVizinho(vizinho);
            }
        }
    }

    @Override
    public synchronized void receive(Mensagem msg) throws RemoteException {
        // Delega ao estado atual
        estado.receberMensagem(msg, this);
    }

    /**
     * NOVO: Método auxiliar chamado pelo estado
     * Contém a lógica real de processamento
     */
    public void processarMensagemRecebida(Mensagem msg) throws RemoteException {
        // Aplicação da estratégia
        try {
            boolean deveProcessar = estrategiaFalha.processar(msg, new RegistradorLog(idNo));
            if (!deveProcessar) {
                return;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Processamento interrompido");
            return;
        }

        gerenciadorEventos.notificarMensagemRecebida(idNo, msg);

        String senderId = msg.getSenderId();
        int seqNumber = msg.getSequenceNumber();
        String idMsg = msg.getUniqueId();
        int ultimaSeq = ultimaSequencia.getOrDefault(senderId, 0);

        if (seqNumber == ultimaSeq + 1) {
            if (!mensagensEntregues.computeIfAbsent(senderId, k -> ConcurrentHashMap.newKeySet()).contains(idMsg)) {
                filaMensagens.add(msg);
                ultimaSequencia.put(senderId, seqNumber);
                enviarACK(senderId, ultimaSeq);
                processarMensagensForaDeOrdem(senderId);
            }
        } else if (seqNumber > ultimaSeq + 1) {
            mensagensForaDeOrdem.computeIfAbsent(senderId, k -> new ConcurrentSkipListMap<>())
                    .put(seqNumber, msg);
            enviarNACK(senderId, ultimaSeq);
        }
    }

    private void enviarNACK(String senderId, int lastReceivedSeq) {
        try {
            NoInterface sender = RegistryManager.getInstancia().buscarNo(senderId);
            sender.handleNACK(idNo, lastReceivedSeq);
        } catch (Exception e) {
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Falha ao enviar NACK para " + senderId);
        }
    }

    private void processarMensagensForaDeOrdem(String senderId) {
        ConcurrentSkipListMap<Integer, Mensagem> mensagens = mensagensForaDeOrdem.get(senderId);
        if (mensagens == null)
            return;

        int nextExpected = ultimaSequencia.getOrDefault(senderId, -1) + 1;

        while (true) {
            Mensagem msg = mensagens.get(nextExpected);
            if (msg == null)
                break;

            if (!mensagensEntregues.get(senderId).contains(msg.getUniqueId())) {
                filaMensagens.add(msg);
                ultimaSequencia.put(senderId, nextExpected);
                enviarACK(senderId, 0);
            }

            mensagens.remove(nextExpected);
            nextExpected++;
        }
    }

    private void enviarACK(String senderId, int lastReceivedSeq) {
        try {
            NoInterface sender = RegistryManager.getInstancia().buscarNo(senderId);
            
            // Notifica observadores
            gerenciadorEventos.notificarNackEnviado(idNo, senderId, lastReceivedSeq);
            
            sender.handleNACK(idNo, lastReceivedSeq);
        } catch (Exception e) {
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Falha ao enviar NACK para " + senderId);
        }
    }

    @Override
    public void handleNACK(String senderId, int lastReceivedSeq) throws RemoteException {
        GerenciadorLog.getInstancia().registrar(idNo, 
            "Recebido NACK de " + senderId + ", última seq recebida: " + lastReceivedSeq);

        // Enviar todas as mensagens pendentes a partir da sequência esperada
        for (Mensagem msg : mensagensPendentes.values()) {
            if (msg.getSenderId().equals(idNo) && msg.getSequenceNumber() > lastReceivedSeq) {
                try {
                    NoInterface destinatario = obterNo(senderId);
                    GerenciadorLog.getInstancia().registrar(idNo, 
                        "Reenviando mensagem [" + msg.getSequenceNumber() + "] para " + senderId);
                    destinatario.receive(msg);
                } catch (Exception e) {
                    GerenciadorLog.getInstancia().registrar(idNo, 
                        "Falha crítica ao reenviar mensagem para " + senderId);
                }
            }
        }
    }

    private void deliver(Mensagem msg) {
        String idMsg = msg.getUniqueId();
        String sender = msg.getSenderId();

        mensagensEntregues.computeIfAbsent(sender, k -> ConcurrentHashMap.newKeySet())
                .add(idMsg);

        gerenciadorEventos.notificarMensagemEntregue(idNo, msg);
    }

    private void processarMensagens() {
        while (estado.getNomeEstado().contains("ATIVO")) {
            try {
                Mensagem msg = filaMensagens.take(); // Ordena por timestamp
                deliver(msg);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                GerenciadorLog.getInstancia().registrar(idNo, 
                    "Processador de mensagens interrompido");
            }
        }
    }

    private void enviarHeartbeats() {
        while (estado.getNomeEstado().contains("ATIVO")) {
            try {
                Thread.sleep(2000);
                for (NoInterface vizinho : new ArrayList<>(vizinhos)) { // Cópia para evitar concorrência
                    try {
                        long startTime = System.currentTimeMillis();
                        vizinho.heartbeat();
                        long rtt = System.currentTimeMillis() - startTime;

                        // Ajustar timeout dinamicamente baseado no RTT
                        if (rtt > ACK_TIMEOUT_MS) {
                            ACK_TIMEOUT_MS = rtt * 2; // Timeout = 2x RTT
                        }
                    } catch (RemoteException e) {
                        GerenciadorLog.getInstancia().registrar(idNo, 
                            "Falha no heartbeat para " + vizinho);
                        tratarFalhaVizinho(vizinho);
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void tratarFalhaVizinho(NoInterface vizinhoFalho) {
        // Notifica observadores
        gerenciadorEventos.notificarFalhaVizinho(idNo, "Falha de comunicação com vizinho");
        vizinhos.remove(vizinhoFalho);
    }

    @Override
    public void ack(Mensagem msg) throws RemoteException {
        gerenciadorEventos.notificarAckEnviado(idNo, msg);
        mensagensPendentes.remove(msg.getUniqueId());
    }

    private void verificarACK(Mensagem msg, int tentativa) {
        if (!estado.getNomeEstado().contains("ATIVO") || tentativa >= MAX_RETRIES) {
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Falha crítica: não foi possível entregar mensagem após " + MAX_RETRIES + " tentativas");
            mensagensPendentes.remove(msg.getUniqueId());
            return;
        }

        if (mensagensPendentes.containsKey(msg.getUniqueId())) {
            // Notifica observadores sobre reenvio
            gerenciadorEventos.notificarMensagemReeviada(idNo, msg, tentativa + 1);

            for (NoInterface vizinho : vizinhos) {
                try {
                    if (!mensagensEntregues.getOrDefault(vizinho.toString(), Collections.emptySet())
                            .contains(msg.getUniqueId())) {
                        vizinho.receive(msg);
                    }
                } catch (RemoteException e) {
                    GerenciadorLog.getInstancia().registrar(idNo, "Falha no reenvio para " + vizinho);
                }
            }

            long novoTimeout = ACK_TIMEOUT_MS * (1 << tentativa);
            executor.schedule(() -> verificarACK(msg, tentativa + 1), novoTimeout, TimeUnit.MILLISECONDS);
        }
    }
    
    // NOVO: Método para adicionar observadores personalizados
    public void adicionarObservador(EventoMensagemObserver observador) {
        gerenciadorEventos.adicionarObservador(observador);
    }
    
    // NOVO: Método para remover observadores
    public void removerObservador(EventoMensagemObserver observador) {
        gerenciadorEventos.removerObservador(observador);
    }

    @Override
    public void heartbeat() throws RemoteException {
        estado.processarHeartbeat(this);
    }

    public void desligar() {
        estado.desligar(this);
    }

    // NOVO: Chamado pelo estado para finalizar recursos
    public void finalizarRecursos() {
        executor.shutdownNow();
        try {
            RegistryManager.getInstancia().removerNo(idNo);
        } catch (Exception e) {
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Erro ao desregistrar nó: " + e.getMessage());
        }
    }
    
    // NOVO: Chamado pelo estado para inicializar recursos
    public void inicializarRecursos() {
        // Reinicia executor se necessário
        if (executor.isShutdown()) {
            executor = Executors.newScheduledThreadPool(3);
            executor.submit(this::processarMensagens);
            executor.submit(this::enviarHeartbeats);
        }
    }
    
    //  NOVO: Chamado pelo estado para iniciar recuperação
    public void iniciarRecuperacao() {
        GerenciadorLog.getInstancia().registrar(idNo, 
            "Iniciando processo de recuperação...");
        // Lógica de recuperação (reconectar vizinhos, limpar filas, etc.)
        mensagensPendentes.clear();
    }
    
    // NOVO: Métodos para controle de estado
    public void setEstado(EstadoNo novoEstado) {
        if (estado.podeTransitarPara(novoEstado)) {
            EstadoNo estadoAnterior = this.estado;
            this.estado = novoEstado;
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Estado alterado: " + estadoAnterior.getNomeEstado() + 
                " -> " + novoEstado.getNomeEstado());
        } else {
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Transição inválida: " + estado.getNomeEstado() + 
                " -> " + novoEstado.getNomeEstado());
        }
    }
    
    public EstadoNo getEstado() {
        return estado;
    }
    
    public String getIdNo() {
        return idNo;
    }
    
    // NOVO: Comandos de controle de estado
    public void ativar() {
        estado.ativar(this);
    }
    
    public void recuperar() {
        estado.recuperar(this);
    }
    
    public void entrarEmFalha(String motivo) {
        GerenciadorLog.getInstancia().registrar(idNo, 
            "Entrando em estado de falha: " + motivo);
        setEstado(new EstadoEmFalha(motivo));
    }

    public void adicionarVizinho(NoInterface vizinho) throws RemoteException {
        if (!vizinhos.contains(vizinho)) {
            vizinhos.add(vizinho);
            // Conecta de volta automaticamente
            try {
                vizinho.adicionarVizinho(this);
            } catch (RemoteException e) {
                GerenciadorLog.getInstancia().registrar(idNo, "Falha ao estabelecer conexão recíproca");
            }
        }
    }

    private NoInterface obterNo(String idNo) throws RemoteException {
        try {
            // Usa singleton
            return RegistryManager.getInstancia().buscarNo(idNo);
        } catch (NotBoundException e) {
            GerenciadorLog.getInstancia().registrar(this.idNo, 
                "Nó " + idNo + " não encontrado no registry");
            throw new RemoteException("Nó não encontrado", e);
        } catch (RemoteException e) {
            GerenciadorLog.getInstancia().registrar(this.idNo, 
                "Falha no registry, tentando reconectar...");
            try {
                RegistryManager.getInstancia().reconectar();
                return RegistryManager.getInstancia().buscarNo(idNo);
            } catch (Exception ex) {
                GerenciadorLog.getInstancia().registrar(this.idNo, 
                    "Falha crítica no registry");
                throw new RemoteException("Registry inacessível", ex);
            }
        }
    }

    // Métodos simplificados
    public void setSimularFalhaOmissao(boolean ativar) throws RemoteException {
        if (ativar) {
            this.estrategiaFalha = new FalhaOmissao();
            GerenciadorLog.getInstancia().registrar(this.idNo, "Modo falha por omissão: ATIVADO");
        } else {
            this.estrategiaFalha = new SemFalha();
            GerenciadorLog.getInstancia().registrar(this.idNo, "Modo falha por omissão: desativado");
        }
    }

    public void setSimularFalhaTemporizacao(boolean ativar) throws RemoteException {
        if (ativar) {
            this.estrategiaFalha = new FalhaTemporizacao();
            GerenciadorLog.getInstancia().registrar(this.idNo, "Modo falha por temporização: ATIVADO");
        } else {
            this.estrategiaFalha = new SemFalha();
            GerenciadorLog.getInstancia().registrar(this.idNo, "Modo falha por temporização: desativado");
        }
    }

    // NOVO: Método para configurar estratégia customizada
    public void setEstrategiaFalha(EstrategiaFalha estrategia) throws RemoteException {
        this.estrategiaFalha = estrategia;
        GerenciadorLog.getInstancia().registrar(this.idNo, 
            "Estratégia de falha alterada para: " + estrategia.getNome());
    }

    // NOVO: Remove vizinho (para undo de connect)
    public void removerVizinho(NoInterface vizinho) throws RemoteException {
        if (vizinhos.contains(vizinho)) {
            vizinhos.remove(vizinho);
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Vizinho removido");
        }
    }
    
    // NOVO: Retorna estratégia atual (para undo)
    public EstrategiaFalha getEstrategiaFalha() {
        return estrategiaFalha;
    }
}