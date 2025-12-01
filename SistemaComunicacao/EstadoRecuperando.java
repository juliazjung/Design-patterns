import java.rmi.RemoteException;

/**
 * Estado Concreto: Nó está se recuperando de uma falha
 */
public class EstadoRecuperando implements EstadoNo {
    private final String motivoFalhaAnterior;
    private final long timestampInicioRecuperacao;
    private int tentativasRecuperacao;
    
    public EstadoRecuperando(String motivoFalhaAnterior) {
        this.motivoFalhaAnterior = motivoFalhaAnterior;
        this.timestampInicioRecuperacao = System.currentTimeMillis();
        this.tentativasRecuperacao = 0;
    }
    
    @Override
    public void receberMensagem(Mensagem msg, No contexto) throws RemoteException {
        // Durante recuperação, aceita mensagens mas loga
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Recebendo mensagem durante recuperação");
        contexto.processarMensagemRecebida(msg);
    }
    
    @Override
    public void enviarMensagem(Mensagem msg, No contexto) throws RemoteException {
        // Durante recuperação, permite envios limitados
        tentativasRecuperacao++;
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Enviando mensagem durante recuperação (tentativa " + tentativasRecuperacao + ")");
        contexto.processarEnvioMensagem(msg);
        
        // Se conseguiu enviar, considera recuperado
        if (tentativasRecuperacao >= 3) {
            GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
                "Recuperação bem-sucedida após " + tentativasRecuperacao + " tentativas");
            contexto.setEstado(new EstadoAtivo());
        }
    }
    
    @Override
    public void processarHeartbeat(No contexto) throws RemoteException {
        // Responde heartbeat durante recuperação
        tentativasRecuperacao++;
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Heartbeat durante recuperação (tentativa " + tentativasRecuperacao + ")");
    }
    
    @Override
    public void desligar(No contexto) {
        // Pode desligar durante recuperação
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Transição de estado: RECUPERANDO -> INATIVO (recuperação abortada)");
        contexto.setEstado(new EstadoInativo());
        contexto.finalizarRecursos();
    }
    
    @Override
    public void ativar(No contexto) {
        // Força conclusão da recuperação
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Forçando conclusão da recuperação: RECUPERANDO -> ATIVO");
        contexto.setEstado(new EstadoAtivo());
    }
    
    @Override
    public void recuperar(No contexto) {
        // Já está recuperando
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Nó já está em processo de recuperação");
    }
    
    @Override
    public String getNomeEstado() {
        return "RECUPERANDO";
    }
    
    @Override
    public boolean podeTransitarPara(EstadoNo novoEstado) {
        // Pode transitar para Ativo ou Inativo
        return novoEstado instanceof EstadoAtivo || 
               novoEstado instanceof EstadoInativo;
    }
}