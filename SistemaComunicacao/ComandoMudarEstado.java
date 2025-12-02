/**
 * Comando para mudar estado do nó (ativar, recuperar, falhar)
 */
public class ComandoMudarEstado extends ComandoBase {
    private String acao; // "ativar", "recuperar", "falhar"
    private String motivo; // apenas para "falhar"
    private EstadoNo estadoAnterior;
    
    public ComandoMudarEstado(No no, String acao) {
        this(no, acao, null);
    }
    
    public ComandoMudarEstado(No no, String acao, String motivo) {
        super(no);
        this.acao = acao;
        this.motivo = motivo;
    }
    
    @Override
    protected void executarInterno() throws Exception {
        // Guarda estado anterior para poder desfazer
        estadoAnterior = no.getEstado();
        
        switch (acao) {
            case "ativar":
                no.ativar();
                resultado = "Nó ativado";
                break;
            
            case "recuperar":
                no.recuperar();
                resultado = "Recuperação iniciada";
                break;
            
            case "falhar":
                if (motivo == null || motivo.trim().isEmpty()) {
                    throw new IllegalArgumentException("Motivo da falha não pode ser vazio");
                }
                no.entrarEmFalha(motivo);
                resultado = "Nó entrou em estado de falha: " + motivo;
                break;
            
            default:
                throw new IllegalArgumentException("Ação inválida: " + acao);
        }
    }
    
    @Override
    protected void desfazerInterno() throws Exception {
        no.setEstado(estadoAnterior);
        resultado = "Estado restaurado para: " + estadoAnterior.getNomeEstado();
    }
    
    @Override
    public boolean podeDesfazer() {
        return true; // Mudanças de estado podem ser desfeitas
    }
    
    @Override
    public String getNome() {
        if (acao.equals("falhar")) {
            return "falhar " + motivo;
        }
        return acao;
    }
}