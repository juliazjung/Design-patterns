/**
 * Comando para enviar broadcast
 */
public class ComandoBroadcast extends ComandoBase {
    private String mensagem;
    
    public ComandoBroadcast(No no, String mensagem) {
        super(no);
        this.mensagem = mensagem;
    }
    
    @Override
    protected void executarInterno() throws Exception {
        if (mensagem == null || mensagem.trim().isEmpty()) {
            throw new IllegalArgumentException("Mensagem não pode ser vazia");
        }
        no.broadcast(mensagem);
        resultado = "Mensagem enviada para todos os vizinhos";
    }
    
    @Override
    protected void desfazerInterno() throws Exception {
        // Broadcast não pode ser desfeito (mensagens já foram enviadas)
        throw new UnsupportedOperationException("Broadcast não pode ser desfeito");
    }
    
    @Override
    public String getNome() {
        return "broadcast";
    }
}