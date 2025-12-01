import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

/** Padrão Singleton: Gerenciador único do RMI Registry
 ** Thread-safe usando inicialização tardia com double-checked locking **/
public class RegistryManager {
    private static volatile RegistryManager instancia;
    private Registry registry;
    private static final int PORTA_PADRAO = 1099;
    
    //Construtor privado para prevenir instanciação externa
    private RegistryManager() throws RemoteException {
        inicializarRegistry();
    }
    
    /** Obtém a instância única do RegistryManager (thread-safe)
     ** Double-checked locking para performance **/
    public static RegistryManager getInstancia() throws RemoteException {
        if (instancia == null) {
            synchronized (RegistryManager.class) {
                if (instancia == null) {
                    instancia = new RegistryManager();
                }
            }
        }
        return instancia;
    }
    
    //Inicializa ou conecta ao Registry existente
    private void inicializarRegistry() throws RemoteException {
        try {
            // Tenta conectar ao Registry existente
            registry = LocateRegistry.getRegistry(PORTA_PADRAO);
            registry.list(); // Testa conexão
            System.out.println("Conectado ao Registry existente na porta " + PORTA_PADRAO);
        } catch (RemoteException e) {
            // Se não existir, cria um novo
            registry = LocateRegistry.createRegistry(PORTA_PADRAO);
            System.out.println("Registry criado na porta " + PORTA_PADRAO);
        }
    }
    
    //Registra um nó no Registry
    public void registrarNo(String idNo, NoInterface no) throws RemoteException {
        registry.rebind(idNo, no);
        System.out.println(idNo + " registrado no RMI Registry");
    }
    
    //Remove um nó do Registry
    public void removerNo(String idNo) throws RemoteException, NotBoundException {
        registry.unbind(idNo);
        System.out.println(idNo + " removido do RMI Registry");
    }
    
    //Busca um nó no Registry
    public NoInterface buscarNo(String idNo) throws RemoteException, NotBoundException {
        return (NoInterface) registry.lookup(idNo);
    }
    
    //Lista todos os nós registrados
    public String[] listarNos() throws RemoteException {
        return registry.list();
    }
    
    //Reconecta ao Registry em caso de falha
    public synchronized void reconectar() throws RemoteException {
        inicializarRegistry();
    }
    
    //Previne clonagem do Singleton
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Singleton não pode ser clonado");
    }
}