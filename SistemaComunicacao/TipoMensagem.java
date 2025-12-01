/**
 * Tipos de mensagens no sistema
 */
public enum TipoMensagem {
    NORMAL,      // Mensagem comum entre nós
    ACK,         // Confirmação de recebimento
    NACK,        // Não confirmação (fora de ordem)
    HEARTBEAT,   // Verificação de vida
    CONTROLE     // Mensagens de controle do sistema
}