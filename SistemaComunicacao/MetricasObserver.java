import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Observador concreto que coleta métricas
 */
public class MetricasObserver implements EventoMensagemObserver {
    private final ConcurrentHashMap<String, AtomicInteger> mensagensRecebidas;
    private final ConcurrentHashMap<String, AtomicInteger> mensagensEntregues;
    private final ConcurrentHashMap<String, AtomicInteger> acksEnviados;
    private final ConcurrentHashMap<String, AtomicInteger> nacksEnviados;
    private final ConcurrentHashMap<String, AtomicInteger> falhasDetectadas;
    private final ConcurrentHashMap<String, AtomicInteger> reenvios;
    
    public MetricasObserver() {
        this.mensagensRecebidas = new ConcurrentHashMap<>();
        this.mensagensEntregues = new ConcurrentHashMap<>();
        this.acksEnviados = new ConcurrentHashMap<>();
        this.nacksEnviados = new ConcurrentHashMap<>();
        this.falhasDetectadas = new ConcurrentHashMap<>();
        this.reenvios = new ConcurrentHashMap<>();
    }
    
    @Override
    public void onMensagemRecebida(String idNo, Mensagem msg) {
        mensagensRecebidas.computeIfAbsent(idNo, k -> new AtomicInteger(0))
                          .incrementAndGet();
    }
    
    @Override
    public void onMensagemEntregue(String idNo, Mensagem msg) {
        mensagensEntregues.computeIfAbsent(idNo, k -> new AtomicInteger(0))
                          .incrementAndGet();
    }
    
    @Override
    public void onAckEnviado(String idNo, Mensagem msg) {
        acksEnviados.computeIfAbsent(idNo, k -> new AtomicInteger(0))
                    .incrementAndGet();
    }
    
    @Override
    public void onNackEnviado(String idNo, String destinatario, int ultimaSeqRecebida) {
        nacksEnviados.computeIfAbsent(idNo, k -> new AtomicInteger(0))
                     .incrementAndGet();
    }
    
    @Override
    public void onFalhaVizinho(String idNo, String motivoFalha) {
        falhasDetectadas.computeIfAbsent(idNo, k -> new AtomicInteger(0))
                        .incrementAndGet();
    }
    
    @Override
    public void onMensagemReeviada(String idNo, Mensagem msg, int tentativa) {
        reenvios.computeIfAbsent(idNo, k -> new AtomicInteger(0))
                .incrementAndGet();
    }
    
    // Imprime relatório de métricas
    public void imprimirRelatorio() {
        System.out.println("\n=== RELATÓRIO DE MÉTRICAS ===");
        System.out.println("Mensagens Recebidas: " + mensagensRecebidas);
        System.out.println("Mensagens Entregues: " + mensagensEntregues);
        System.out.println("ACKs Enviados: " + acksEnviados);
        System.out.println("NACKs Enviados: " + nacksEnviados);
        System.out.println("Falhas Detectadas: " + falhasDetectadas);
        System.out.println("Reenvios: " + reenvios);
        System.out.println("============================\n");
    }
}