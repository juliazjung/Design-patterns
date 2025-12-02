/**
 * Comando para conectar a outro nó
 */
public class ComandoConectar extends ComandoBase {
    private String idNoDestino;
    private NoInterface noConectado;
    
    public ComandoConectar(No no, String idNoDestino) {
        super(no);
        this.idNoDestino = idNoDestino;
    }
    
    @Override
    protected void executarInterno() throws Exception {
        if (idNoDestino == null || idNoDestino.trim().isEmpty()) {
            throw new IllegalArgumentException("ID do nó não pode ser vazio");
        }
        
        GerenciadorLog.getInstancia().registrar(no.getIdNo(), 
            "Procurando " + idNoDestino + " no registry...");
        
        noConectado = RegistryManager.getInstancia().buscarNo(idNoDestino);
        
        GerenciadorLog.getInstancia().registrar(no.getIdNo(), 
            "Encontrado, estabelecendo conexão...");
        
        no.adicionarVizinho(noConectado);
        resultado = "Conexão bilateral estabelecida com " + idNoDestino;
    }
    
    @Override
    protected void desfazerInterno() throws Exception {
        if (noConectado != null) {
            no.removerVizinho(noConectado);
            resultado = "Conexão com " + idNoDestino + " desfeita";
        }
    }
    
    @Override
    public boolean podeDesfazer() {
        return true; // Conexões podem ser desfeitas
    }
    
    @Override
    public String getNome() {
        return "connect " + idNoDestino;
    }
}