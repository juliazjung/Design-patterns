/**
 * Padrão Observer: Interface para observadores de eventos de mensagens
 */
public interface EventoMensagemObserver {
    // Notificado quando uma mensagem é recebida
    void onMensagemRecebida(String idNo, Mensagem msg);
    
    // Notificado quando uma mensagem é entregue com sucesso
    void onMensagemEntregue(String idNo, Mensagem msg);
    
    // Notificado quando um ACK é enviado
    void onAckEnviado(String idNo, Mensagem msg);
    
    // Notificado quando um NACK é enviado
    void onNackEnviado(String idNo, String destinatario, int ultimaSeqRecebida);
    
    // Notificado quando uma falha de vizinho é detectada
    void onFalhaVizinho(String idNo, String motivoFalha);
    
    // Notificado quando uma mensagem é reenviada
    void onMensagemReeviada(String idNo, Mensagem msg, int tentativa);
}