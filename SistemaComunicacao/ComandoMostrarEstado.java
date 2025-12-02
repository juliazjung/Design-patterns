/**
 * Comando para mostrar o estado atual do nó
 */
public class ComandoMostrarEstado extends ComandoBase {
    
    public ComandoMostrarEstado(No no) {
        super(no);
    }
    
    @Override
    protected void executarInterno() throws Exception {
        String estadoAtual = no.getEstado().getNomeEstado();
        System.out.println("Estado atual: " + estadoAtual);
        resultado = "Estado: " + estadoAtual;
    }
    
    @Override
    protected void desfazerInterno() throws Exception {
        // Não pode desfazer consulta de estado
        throw new UnsupportedOperationException();
    }
    
    @Override
    public String getNome() {
        return "estado";
    }
}