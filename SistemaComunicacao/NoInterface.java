import java.rmi.Remote;
import java.rmi.RemoteException;

public interface NoInterface extends Remote {
    void receive(Mensagem msg) throws RemoteException;
    void ack(Mensagem msg) throws RemoteException;
    void heartbeat() throws RemoteException;
    void handleNACK(String senderId, int lastReceivedSeq) throws RemoteException;
    void adicionarVizinho(NoInterface vizinho) throws RemoteException;
    void broadcast(String conteudo) throws RemoteException;
    void setSimularFalhaOmissao(boolean ativar) throws RemoteException;
    void setSimularFalhaTemporizacao(boolean ativar) throws RemoteException;
}