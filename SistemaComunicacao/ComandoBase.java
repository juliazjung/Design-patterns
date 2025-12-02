/**
 * Classe base para comandos com funcionalidades comuns
 */
public abstract class ComandoBase implements Comando {
    protected No no;
    protected String resultado;
    protected boolean executado;
    
    public ComandoBase(No no) {
        this.no = no;
        this.executado = false;
    }
    
    @Override
    public boolean executar() {
        try {
            executarInterno();
            executado = true;
            return true;
        } catch (Exception e) {
            resultado = "Erro: " + e.getMessage();
            return false;
        }
    }
    
    // Método template para execução específica
    protected abstract void executarInterno() throws Exception;
    
    @Override
    public boolean desfazer() {
        if (!podeDesfazer() || !executado) {
            resultado = "Comando não pode ser desfeito";
            return false;
        }
        try {
            desfazerInterno();
            executado = false;
            return true;
        } catch (Exception e) {
            resultado = "Erro ao desfazer: " + e.getMessage();
            return false;
        }
    }
    
    protected abstract void desfazerInterno() throws Exception;
    
    @Override
    public boolean podeDesfazer() {
        return false; // Padrão: maioria dos comandos não pode ser desfeito
    }
    
    @Override
    public String getResultado() {
        return resultado != null ? resultado : "Executado com sucesso";
    }
}