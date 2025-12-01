import java.rmi.RemoteException;

/**
 * Estado Concreto: Nó está ativo e funcionando normalmente
 */
public class EstadoAtivo implements EstadoNo {
    
    @Override
    public void receberMensagem(Mensagem msg, No contexto) throws RemoteException {
        // Estado ativo permite receber mensagens normalmente
        contexto.processarMensagemRecebida(msg);
    }
    
    @Override
    public void enviarMensagem(Mensagem msg, No contexto) throws RemoteException {
        // Estado ativo permite enviar mensagens
        contexto.processarEnvioMensagem(msg);
    }
    
    @Override
    public void processarHeartbeat(No contexto) throws RemoteException {
        // Responde normalmente ao heartbeat
        // Nada a fazer, está funcionando
    }
    
    @Override
    public void desligar(No contexto) {
        // Transição permitida: Ativo -> Inativo
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Transição de estado: ATIVO -> INATIVO");
        contexto.setEstado(new EstadoInativo());
        contexto.finalizarRecursos();
    }
    
    @Override
    public void ativar(No contexto) {
        // Já está ativo, nada a fazer
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Nó já está ativo");
    }
    
    @Override
    public void recuperar(No contexto) {
        // Não precisa recuperar, já está ativo
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Nó já está ativo, não precisa recuperar");
    }
    
    @Override
    public String getNomeEstado() {
        return "ATIVO";
    }
    
    @Override
    public boolean podeTransitarPara(EstadoNo novoEstado) {
        // Pode transitar para Inativo ou EmFalha
        return novoEstado instanceof EstadoInativo || 
               novoEstado instanceof EstadoEmFalha;
    }
}