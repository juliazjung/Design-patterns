/**
 * Comando para mostrar métricas
 */
public class ComandoMostrarMetricas extends ComandoBase {
    private MetricasObserver metricas;
    
    public ComandoMostrarMetricas(No no, MetricasObserver metricas) {
        super(no);
        this.metricas = metricas;
    }
    
    @Override
    protected void executarInterno() throws Exception {
        metricas.imprimirRelatorio();
        resultado = "Métricas exibidas";
    }
    
    @Override
    protected void desfazerInterno() throws Exception {
        // Não pode desfazer exibição de métricas
        throw new UnsupportedOperationException();
    }
    
    @Override
    public String getNome() {
        return "metricas";
    }
}