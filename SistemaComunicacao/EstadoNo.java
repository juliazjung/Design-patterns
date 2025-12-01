import java.rmi.RemoteException;

/**
 * Padrão State: Interface que define comportamentos dependentes do estado
 */
public interface EstadoNo {
    /**
     * Tenta processar uma mensagem recebida
     * Comportamento varia de acordo com o estado
     */
    void receberMensagem(Mensagem msg, No contexto) throws RemoteException;
    
    /**
     * Tenta enviar uma mensagem
     * Comportamento varia de acordo com o estado
     */
    void enviarMensagem(Mensagem msg, No contexto) throws RemoteException;
    
    // Processa heartbeat
    void processarHeartbeat(No contexto) throws RemoteException;
    
    // Tenta desligar o nó
    void desligar(No contexto);
    
    // Tenta ativar o nó
    void ativar(No contexto);
    
    // Tenta recuperar de uma falha
    void recuperar(No contexto);
    
    // Retorna o nome do estado para logging
    String getNomeEstado();
    
    // Verifica se pode transitar para outro estado
    boolean podeTransitarPara(EstadoNo novoEstado);
}