/**
 * Observador concreto que loga eventos
 */
public class LogObserver implements EventoMensagemObserver {
    
    @Override
    public void onMensagemRecebida(String idNo, Mensagem msg) {
        GerenciadorLog.getInstancia().registrar(idNo, 
            "Recebida mensagem de " + msg.getSenderId() + 
            " [SEQ:" + msg.getSequenceNumber() + "]");
    }
    
    @Override
    public void onMensagemEntregue(String idNo, Mensagem msg) {
        GerenciadorLog.getInstancia().registrar(idNo, 
            "Delivering mensagem [" + msg.getUniqueId() + "]: " + msg.getConteudo());
    }
    
    @Override
    public void onAckEnviado(String idNo, Mensagem msg) {
        GerenciadorLog.getInstancia().registrar(idNo, 
            "Enviando ACK para " + msg.getSenderId() + 
            " [SEQ:" + msg.getSequenceNumber() + "]");
    }
    
    @Override
    public void onNackEnviado(String idNo, String destinatario, int ultimaSeqRecebida) {
        GerenciadorLog.getInstancia().registrar(idNo, 
            "Enviando NACK para " + destinatario + 
            ", última seq recebida: " + ultimaSeqRecebida);
    }
    
    @Override
    public void onFalhaVizinho(String idNo, String motivoFalha) {
        GerenciadorLog.getInstancia().registrar(idNo, 
            "Tratando falha do vizinho: " + motivoFalha);
    }
    
    @Override
    public void onMensagemReeviada(String idNo, Mensagem msg, int tentativa) {
        GerenciadorLog.getInstancia().registrar(idNo, 
            "Reenviando mensagem [Tentativa " + tentativa + "]: " + msg.getUniqueId());
    }
}