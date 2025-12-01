import java.io.Serializable;

public class Mensagem implements Serializable {
    private String senderId;
    private int sequenceNumber;
    private String conteudo;
    private long timestamp;

    // Construtor original (mantido para compatibilidade)
    public Mensagem(String senderId, int sequenceNumber, String conteudo) {
        this.senderId = senderId;
        this.sequenceNumber = sequenceNumber;
        this.conteudo = conteudo;
        this.timestamp = System.currentTimeMillis();
    }

    // NOVO: Construtor com timestamp customizado
    public Mensagem(String senderId, int sequenceNumber, String conteudo, long timestamp) {
        this.senderId = senderId;
        this.sequenceNumber = sequenceNumber;
        this.conteudo = conteudo;
        this.timestamp = timestamp;
    }

    public String getUniqueId() {
        return String.format("%s-%d-%d", senderId, sequenceNumber, timestamp);
    }

    public String getSenderId() { return senderId; }
    public int getSequenceNumber() { return sequenceNumber; }
    public String getConteudo() { return conteudo; }
    public long getTimestamp() { return timestamp; }

    //Representação JSON para logging
    @Override
    public String toString() {
        return String.format("{\"sender\":\"%s\","
                            + "\"seq\":%d,"
                            + "\"content\":\"%s\","
                            + "\"timestamp\":%d}",
                senderId, sequenceNumber, conteudo, timestamp);
    }
}