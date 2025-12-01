import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Padrão Observer: Subject que gerencia observadores
 * Thread-safe usando CopyOnWriteArrayList
 */
public class GerenciadorEventos {
    private final List<EventoMensagemObserver> observadores;
    
    public GerenciadorEventos() {
        this.observadores = new CopyOnWriteArrayList<>();
    }
    
    // Registra um observador
    public void adicionarObservador(EventoMensagemObserver observador) {
        if (observador != null && !observadores.contains(observador)) {
            observadores.add(observador);
        }
    }
    
    // Remove um observador
    public void removerObservador(EventoMensagemObserver observador) {
        observadores.remove(observador);
    }
    
    // Notifica todos os observadores sobre mensagem recebida
    public void notificarMensagemRecebida(String idNo, Mensagem msg) {
        for (EventoMensagemObserver obs : observadores) {
            try {
                obs.onMensagemRecebida(idNo, msg);
            } catch (Exception e) {
                // Evita que exceção em um observador afete outros
                System.err.println("Erro ao notificar observador: " + e.getMessage());
            }
        }
    }
    
    // Notifica todos os observadores sobre mensagem entregue
    public void notificarMensagemEntregue(String idNo, Mensagem msg) {
        for (EventoMensagemObserver obs : observadores) {
            try {
                obs.onMensagemEntregue(idNo, msg);
            } catch (Exception e) {
                System.err.println("Erro ao notificar observador: " + e.getMessage());
            }
        }
    }
    
    // Notifica todos os observadores sobre ACK enviado
    public void notificarAckEnviado(String idNo, Mensagem msg) {
        for (EventoMensagemObserver obs : observadores) {
            try {
                obs.onAckEnviado(idNo, msg);
            } catch (Exception e) {
                System.err.println("Erro ao notificar observador: " + e.getMessage());
            }
        }
    }
    
    // Notifica todos os observadores sobre NACK enviado
    public void notificarNackEnviado(String idNo, String destinatario, int ultimaSeq) {
        for (EventoMensagemObserver obs : observadores) {
            try {
                obs.onNackEnviado(idNo, destinatario, ultimaSeq);
            } catch (Exception e) {
                System.err.println("Erro ao notificar observador: " + e.getMessage());
            }
        }
    }
    
    // Notifica todos os observadores sobre falha de vizinho
    public void notificarFalhaVizinho(String idNo, String motivo) {
        for (EventoMensagemObserver obs : observadores) {
            try {
                obs.onFalhaVizinho(idNo, motivo);
            } catch (Exception e) {
                System.err.println("Erro ao notificar observador: " + e.getMessage());
            }
        }
    }
    
    // Notifica todos os observadores sobre reenvio de mensagem
    public void notificarMensagemReeviada(String idNo, Mensagem msg, int tentativa) {
        for (EventoMensagemObserver obs : observadores) {
            try {
                obs.onMensagemReeviada(idNo, msg, tentativa);
            } catch (Exception e) {
                System.err.println("Erro ao notificar observador: " + e.getMessage());
            }
        }
    }
}