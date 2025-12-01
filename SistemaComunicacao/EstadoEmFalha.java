import java.rmi.RemoteException;

/**
 * Estado Concreto: Nó detectou uma falha e está em modo de falha
 */
public class EstadoEmFalha implements EstadoNo {
    private final String motivoFalha;
    private final long timestampFalha;
    
    public EstadoEmFalha(String motivoFalha) {
        this.motivoFalha = motivoFalha;
        this.timestampFalha = System.currentTimeMillis();
    }
    
    @Override
    public void receberMensagem(Mensagem msg, No contexto) throws RemoteException {
        // Em falha, pode receber mas loga warning
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "WARNING: Recebendo mensagem em estado de falha");
        contexto.processarMensagemRecebida(msg);
    }
    
    @Override
    public void enviarMensagem(Mensagem msg, No contexto) throws RemoteException {
        // Em falha, tenta enviar mas pode falhar
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "WARNING: Tentando enviar mensagem em estado de falha");
        throw new RemoteException("Nó em estado de falha: " + motivoFalha);
    }
    
    @Override
    public void processarHeartbeat(No contexto) throws RemoteException {
        // Responde mas indica falha
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Heartbeat recebido em estado de falha");
    }
    
    @Override
    public void desligar(No contexto) {
        // Transição permitida: EmFalha -> Inativo
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Transição de estado: EM_FALHA -> INATIVO");
        contexto.setEstado(new EstadoInativo());
        contexto.finalizarRecursos();
    }
    
    @Override
    public void ativar(No contexto) {
        // Não pode ativar diretamente, precisa recuperar primeiro
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Nó em falha não pode ser ativado diretamente. Use 'recuperar' primeiro");
    }
    
    @Override
    public void recuperar(No contexto) {
        // Transição permitida: EmFalha -> Recuperando
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Transição de estado: EM_FALHA -> RECUPERANDO");
        contexto.setEstado(new EstadoRecuperando(motivoFalha));
        contexto.iniciarRecuperacao();
    }
    
    @Override
    public String getNomeEstado() {
        return "EM_FALHA";
    }
    
    public String getMotivoFalha() {
        return motivoFalha;
    }
    
    public long getTimestampFalha() {
        return timestampFalha;
    }
    
    @Override
    public boolean podeTransitarPara(EstadoNo novoEstado) {
        // ✅ Pode transitar para Recuperando ou Inativo
        return novoEstado instanceof EstadoRecuperando || 
               novoEstado instanceof EstadoInativo;
    }
}