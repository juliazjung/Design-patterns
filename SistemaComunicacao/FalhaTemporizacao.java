import java.util.Random;

/**
 * Estratégia que simula falha por temporização (atrasos)
 * Adiciona atraso aleatório de 1 a 5 segundos
 */
public class FalhaTemporizacao implements EstrategiaFalha {
    private final Random random;
    private final int delayMinimo;
    private final int delayMaximo;
    
    public FalhaTemporizacao() {
        this(1000, 5000); // 1 a 5 segundos padrão
    }
    
    public FalhaTemporizacao(int delayMinimo, int delayMaximo) {
        this.random = new Random();
        this.delayMinimo = delayMinimo;
        this.delayMaximo = delayMaximo;
    }
    
    @Override
    public boolean processar(Mensagem msg, RegistradorLog log) throws InterruptedException {
        int delayMillis = delayMinimo + random.nextInt(delayMaximo - delayMinimo);
        log.registrar("FALHA SIMULADA (TEMPORIZAÇÃO): Atrasando mensagem em " + delayMillis + "ms");
        Thread.sleep(delayMillis);
        return true; // Continua processamento após o atraso
    }
    
    @Override
    public String getNome() {
        return "Falha por Temporização";
    }
}
