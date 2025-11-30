/**
 * Estratégia padrão: sem simulação de falhas
 */
public class SemFalha implements EstrategiaFalha {
    @Override
    public boolean processar(Mensagem msg, RegistradorLog log) {
        // Não aplica nenhuma falha, apenas continua o processamento
        return true;
    }
    
    @Override
    public String getNome() {
        return "Sem Falha";
    }
}
