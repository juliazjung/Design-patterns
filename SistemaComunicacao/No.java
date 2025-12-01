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

    // Controle de mensagens
    private Map<String, Set<String>> mensagensEntregues;
    private Map<String, Mensagem> mensagensPendentes;
    private BlockingQueue<Mensagem> filaMensagens;

    // Infraestrutura
    private ScheduledExecutorService executor;
    private boolean ativo;

    // Para Atomic Broadcast (FIFO)
    private final Map<String, Integer> ultimaSequencia; // Controla a última sequência recebida de cada nó
    private final Map<String, ConcurrentSkipListMap<Integer, Mensagem>> mensagensForaDeOrdem;

    // Variáveis para controle de falhas - Atualizado
    private EstrategiaFalha estrategiaFalha;

    private static long ACK_TIMEOUT_MS = 3000; // 3 segundos
    private static final int MAX_RETRIES = 3;

    public No(String idNo) throws RemoteException {
        this.idNo = idNo;
        this.vizinhos = new CopyOnWriteArrayList<>();
        this.contadorSequencia = new AtomicInteger(0);
        this.mensagensEntregues = new ConcurrentHashMap<>();
        this.mensagensPendentes = new ConcurrentHashMap<>();
        this.ultimaSequencia = new ConcurrentHashMap<>();
        this.mensagensForaDeOrdem = new ConcurrentHashMap<>();
        this.filaMensagens = new PriorityBlockingQueue<>(11,
                Comparator.comparingLong(Mensagem::getTimestamp));
        this.executor = Executors.newScheduledThreadPool(3);
        this.ativo = true;

        this.estrategiaFalha = new SemFalha();

        executor.submit(this::processarMensagens);
        executor.submit(this::enviarHeartbeats);

        GerenciadorLog.getInstancia().registrar(idNo, "Nó iniciado. Aguardando conexões...");
    }

    public void broadcast(String conteudo) {
        Mensagem msg = new Mensagem(idNo, contadorSequencia.incrementAndGet(), conteudo);
        
        GerenciadorLog.getInstancia().registrar(idNo, "Broadcasting mensagem: " + msg);
       
        mensagensPendentes.put(msg.getUniqueId(), msg);
        executor.schedule(() -> verificarACK(msg, 0), ACK_TIMEOUT_MS, TimeUnit.MILLISECONDS);

        for (NoInterface vizinho : vizinhos) {
            try {
                vizinho.receive(msg);
            } catch (RemoteException e) {
                GerenciadorLog.getInstancia().registrar(idNo, "Falha ao enviar para vizinho: " + e.getMessage());
                tratarFalhaVizinho(vizinho);
            }
        }
    }

    @Override
    public synchronized void receive(Mensagem msg) throws RemoteException {
        if (!ativo)
            throw new RemoteException("Nó inativo");

        // Aplicação da estratégia
        try {
            boolean deveProcessar = estrategiaFalha.processar(msg, new RegistradorLog(idNo));
            if (!deveProcessar) {
                return; // Estratégia decidiu descartar a mensagem
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            GerenciadorLog.getInstancia().registrar(idNo, "Processamento interrompido durante aplicação de estratégia de falha");
            return;
        }

        GerenciadorLog.getInstancia().registrar(idNo, 
            "Recebida mensagem de " + msg.getSenderId() + " [SEQ:" + msg.getSequenceNumber() + "]");

        String senderId = msg.getSenderId();
        int seqNumber = msg.getSequenceNumber();
        String idMsg = msg.getUniqueId();

        // Verificar ordem FIFO
        int ultimaSeq = ultimaSequencia.getOrDefault(senderId, 0);

        GerenciadorLog.getInstancia().registrar(idNo, 
            "Última sequência de " + senderId + ": " + ultimaSeq);

        if (seqNumber == ultimaSeq + 1) {
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Mensagem na ordem correta, processando...");
            
            if (!mensagensEntregues.computeIfAbsent(senderId, k -> ConcurrentHashMap.newKeySet()).contains(idMsg)) {
                filaMensagens.add(msg);
                ultimaSequencia.put(senderId, seqNumber);
                enviarACK(senderId, msg);

                // Verificar se há mensagens subsequentes armazenadas
                processarMensagensForaDeOrdem(senderId);
            }
        } else if (seqNumber > ultimaSeq + 1) {
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Mensagem fora de ordem (esperava " + (ultimaSeq + 1) + "), armazenando e enviando NACK");
            
            mensagensForaDeOrdem.computeIfAbsent(senderId, k -> new ConcurrentSkipListMap<>())
                    .put(seqNumber, msg);
            enviarNACK(senderId, ultimaSeq);
        } else {
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Mensagem duplicada ou antiga [SEQ:" + seqNumber + "], descartando");
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
                enviarACK(senderId, msg);
            }

            mensagens.remove(nextExpected);
            nextExpected++;
        }
    }

    private void enviarACK(String senderId, Mensagem msg) {
        try {
            // Usa o singleton do Registry
            NoInterface sender = RegistryManager.getInstancia().buscarNo(senderId);
            sender.ack(msg);
        } catch (Exception e) {
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Falha no ACK para " + senderId + ": " + e.getMessage());
            try {
                // Reconexão simplificada
                RegistryManager.getInstancia().reconectar();
                NoInterface sender = RegistryManager.getInstancia().buscarNo(senderId);
                sender.ack(msg);
            } catch (Exception ex) {
                GerenciadorLog.getInstancia().registrar(idNo, "Falha ao reenviar ACK");
            }
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

        GerenciadorLog.getInstancia().registrar(idNo, 
            "Delivering mensagem [" + idMsg + "]: " + msg.getConteudo());
    }

    private void processarMensagens() {
        while (ativo) {
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
        while (ativo) {
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
        GerenciadorLog.getInstancia().registrar(idNo, 
            "Tratando falha do vizinho");
        vizinhos.remove(vizinhoFalho);
    }

    @Override
    public void ack(Mensagem msg) throws RemoteException {
        GerenciadorLog.getInstancia().registrar(idNo, 
            "Enviando ACK para " + msg.getSenderId() + " [SEQ:" + msg.getSequenceNumber() + "]");
        mensagensPendentes.remove(msg.getUniqueId());
    }

    private void verificarACK(Mensagem msg, int tentativa) {
        if (!ativo || tentativa >= MAX_RETRIES) {
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Falha crítica: não foi possível entregar mensagem após " + MAX_RETRIES + " tentativas");
            mensagensPendentes.remove(msg.getUniqueId()); // Limpeza final
            return;
        }

        if (mensagensPendentes.containsKey(msg.getUniqueId())) {
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Reenviando mensagem [Tentativa " + (tentativa + 1) + "]: " + msg.getUniqueId());

            // Reenviar apenas para vizinhos que não confirmaram
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

    @Override
    public void heartbeat() throws RemoteException {
        // Resposta ao heartbeat
    }

    public void desligar() {
        ativo = false;
        executor.shutdownNow();
        
        try {
            RegistryManager.getInstancia().removerNo(idNo);
        } catch (Exception e) {
            GerenciadorLog.getInstancia().registrar(idNo, 
                "Erro ao desregistrar nó: " + e.getMessage());
        }
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
}