import java.rmi.RemoteException;

/**
 * Estado Concreto: Nó está inativo (desligado)
 */
public class EstadoInativo implements EstadoNo {
    
    @Override
    public void receberMensagem(Mensagem msg, No contexto) throws RemoteException {
        // Estado inativo rejeita mensagens
        throw new RemoteException("Nó inativo - não pode receber mensagens");
    }
    
    @Override
    public void enviarMensagem(Mensagem msg, No contexto) throws RemoteException {
        // Estado inativo rejeita envios
        throw new RemoteException("Nó inativo - não pode enviar mensagens");
    }
    
    @Override
    public void processarHeartbeat(No contexto) throws RemoteException {
        // Não responde heartbeat quando inativo
        throw new RemoteException("Nó inativo");
    }
    
    @Override
    public void desligar(No contexto) {
        // Já está inativo
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Nó já está inativo");
    }
    
    @Override
    public void ativar(No contexto) {
        // Transição permitida: Inativo -> Ativo
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Transição de estado: INATIVO -> ATIVO");
        contexto.setEstado(new EstadoAtivo());
        contexto.inicializarRecursos();
    }
    
    @Override
    public void recuperar(No contexto) {
        // Não pode recuperar, precisa ativar primeiro
        GerenciadorLog.getInstancia().registrar(contexto.getIdNo(), 
            "Nó inativo não pode recuperar. Use 'ativar' primeiro");
    }
    
    @Override
    public String getNomeEstado() {
        return "INATIVO";
    }
    
    @Override
    public boolean podeTransitarPara(EstadoNo novoEstado) {
        // Pode transitar apenas para Ativo
        return novoEstado instanceof EstadoAtivo;
    }
}