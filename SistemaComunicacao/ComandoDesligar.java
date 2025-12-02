/**
 * Comando para desligar o nó
 */
public class ComandoDesligar extends ComandoBase {
    private MetricasObserver metricas;
    
    public ComandoDesligar(No no, MetricasObserver metricas) {
        super(no);
        this.metricas = metricas;
    }
    
    @Override
    protected void executarInterno() throws Exception {
        metricas.imprimirRelatorio();
        no.desligar();
        RegistryManager.getInstancia().removerNo(no.getIdNo());
        GerenciadorLog.getInstancia().registrar(no.getIdNo(), "Nó desligado");
        GerenciadorLog.getInstancia().fecharLogs();
        resultado = "Nó desligado com sucesso";
    }
    
    @Override
    protected void desfazerInterno() throws Exception {
        // Desligar não pode ser desfeito
        throw new UnsupportedOperationException("Desligar não pode ser desfeito");
    }
    
    @Override
    public String getNome() {
        return "shutdown";
    }
}