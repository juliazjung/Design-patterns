/**
 * Padrão Factory Method: Fábrica para criação de mensagens
 * Centraliza lógica de criação, validação e geração de IDs
 */
public class MensagemFactory {
    private static final int TAMANHO_MAX_CONTEUDO = 1000; // caracteres
    
    // Cria uma mensagem padrão com validações
    public static Mensagem criarMensagem(String senderId, int sequenceNumber, String conteudo) 
            throws IllegalArgumentException {
        
        validarParametros(senderId, conteudo);
        return new Mensagem(senderId, sequenceNumber, conteudo);
    }
    
    // Cria mensagem de controle (ACK, NACK, HEARTBEAT)
    public static Mensagem criarMensagemControle(String senderId, int sequenceNumber, 
                                                  TipoMensagem tipo) {
        String conteudo = String.format("[CONTROLE:%s]", tipo.name());
        return new Mensagem(senderId, sequenceNumber, conteudo);
    }
    
    // Cria mensagem com timestamp customizado (para testes)
    public static Mensagem criarMensagemComTimestamp(String senderId, int sequenceNumber, 
                                                      String conteudo, long timestamp) {
        validarParametros(senderId, conteudo);
        return new Mensagem(senderId, sequenceNumber, conteudo, timestamp);
    }
    
    // Valida parâmetros antes de criar mensagem
    private static void validarParametros(String senderId, String conteudo) {
        if (senderId == null || senderId.trim().isEmpty()) {
            throw new IllegalArgumentException("SenderId não pode ser nulo ou vazio");
        }
        
        if (conteudo == null) {
            throw new IllegalArgumentException("Conteúdo não pode ser nulo");
        }
        
        if (conteudo.length() > TAMANHO_MAX_CONTEUDO) {
            throw new IllegalArgumentException(
                String.format("Conteúdo excede tamanho máximo de %d caracteres", 
                             TAMANHO_MAX_CONTEUDO));
        }
    }
}