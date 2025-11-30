import java.io.Serializable;

public class Mensagem implements Serializable {
    private String senderId;
    private int sequenceNumber;
    private String conteudo;
    private long timestamp;

    public Mensagem(String senderId, int sequenceNumber, String conteudo) {
        this.senderId = senderId;
        this.sequenceNumber = sequenceNumber;
        this.conteudo = conteudo;
        this.timestamp = System.currentTimeMillis();
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