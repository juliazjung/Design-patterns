import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/** Padrão Singleton: Gerenciador centralizado de logs
 ** Thread-safe para múltiplas threads registrando simultaneamente **/
public class GerenciadorLog {
    private static volatile GerenciadorLog instancia;
    private SimpleDateFormat formatadorData;
    private ConcurrentMap<String, PrintWriter> arquivosLog;
    private static final String DIRETORIO_LOGS = "logs/";
    
    private GerenciadorLog() {
        this.formatadorData = new SimpleDateFormat("HH:mm:ss.SSS");
        this.arquivosLog = new ConcurrentHashMap<>();
        
        // Cria diretório de logs se não existir
        File dir = new File(DIRETORIO_LOGS);
        if (!dir.exists()) {
            dir.mkdirs();
        }
    }
    
    //Obtém a instância única (thread-safe)
    public static GerenciadorLog getInstancia() {
        if (instancia == null) {
            synchronized (GerenciadorLog.class) {
                if (instancia == null) {
                    instancia = new GerenciadorLog();
                }
            }
        }
        return instancia;
    }
    
    /** Registra uma mensagem de log para um nó específico
     ** Thread-safe: múltiplos nós podem logar simultaneamente **/
    public synchronized void registrar(String idNo, String mensagem) {
        String dataFormatada = formatadorData.format(new Date());
        String logFormatado = String.format("[%s][%s] %s", dataFormatada, idNo, mensagem);
        
        // Log no console
        System.out.println(logFormatado);
        
        // Log em arquivo
        registrarEmArquivo(idNo, logFormatado);
    }
    
    //Registra em arquivo específico do nó
    private void registrarEmArquivo(String idNo, String mensagem) {
        try {
            PrintWriter writer = arquivosLog.computeIfAbsent(idNo, this::criarArquivoLog);
            writer.println(mensagem);
            writer.flush(); // Garante escrita imediata
        } catch (Exception e) {
            System.err.println("Erro ao escrever log em arquivo: " + e.getMessage());
        }
    }
    
    //Cria arquivo de log para um nó
    private PrintWriter criarArquivoLog(String idNo) {
        try {
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String nomeArquivo = DIRETORIO_LOGS + idNo + "_" + timestamp + ".log";
            return new PrintWriter(new FileWriter(nomeArquivo, true), true);
        } catch (IOException e) {
            System.err.println("Erro ao criar arquivo de log: " + e.getMessage());
            return null;
        }
    }
    
    //Registra mensagem em log global (sem nó específico)
    public synchronized void registrarGlobal(String mensagem) {
        registrar("SYSTEM", mensagem);
    }
    
    //Fecha todos os arquivos de log
    public synchronized void fecharLogs() {
        arquivosLog.values().forEach(writer -> {
            if (writer != null) {
                writer.close();
            }
        });
        arquivosLog.clear();
    }
    
    //Previne clonagem
    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Singleton não pode ser clonado");
    }
}