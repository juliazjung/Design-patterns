/**
 * Comando para ativar simulação de falhas
 */
public class ComandoAtivarFalha extends ComandoBase {
    private String tipoFalha; // "omissao" ou "tempo"
    private boolean ativar;
    private EstrategiaFalha estrategiaAnterior;
    
    public ComandoAtivarFalha(No no, String tipoFalha, boolean ativar) {
        super(no);
        this.tipoFalha = tipoFalha;
        this.ativar = ativar;
    }
    
    @Override
    protected void executarInterno() throws Exception {
        // Guarda estratégia anterior para poder desfazer
        estrategiaAnterior = no.getEstrategiaFalha();
        
        if (tipoFalha.equals("omissao")) {
            no.setSimularFalhaOmissao(ativar);
            resultado = "Falha por omissão " + (ativar ? "ativada" : "desativada");
        } else if (tipoFalha.equals("tempo")) {
            no.setSimularFalhaTemporizacao(ativar);
            resultado = "Falha por temporização " + (ativar ? "ativada" : "desativada");
        } else {
            throw new IllegalArgumentException("Tipo de falha inválido: " + tipoFalha);
        }
    }
    
    @Override
    protected void desfazerInterno() throws Exception {
        no.setEstrategiaFalha(estrategiaAnterior);
        resultado = "Estratégia de falha restaurada";
    }
    
    @Override
    public boolean podeDesfazer() {
        return true;
    }
    
    @Override
    public String getNome() {
        return "falha " + tipoFalha + " " + (ativar ? "on" : "off");
    }
}