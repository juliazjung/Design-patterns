import java.util.Random;

/**
 * Estratégia que simula falha por omissão (perda de mensagens)
 * Descarta mensagens com 30% de probabilidade
 */
public class FalhaOmissao implements EstrategiaFalha {
    private final Random random;
    private final double probabilidade;
    
    public FalhaOmissao() {
        this(0.3); // 30% de chance padrão
    }
    
    public FalhaOmissao(double probabilidade) {
        this.random = new Random();
        this.probabilidade = probabilidade;
    }
    
    @Override
    public boolean processar(Mensagem msg, RegistradorLog log) {
        if (random.nextDouble() < probabilidade) {
            log.registrar("OMISSÃO SIMULADA: " + msg.getUniqueId());
            return false; // Descarta a mensagem
        }
        return true; // Continua processamento
    }
    
    @Override
    public String getNome() {
        return "Falha por Omissão";
    }
}
