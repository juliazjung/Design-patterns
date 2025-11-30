/**
 * Padrão Strategy: Interface que define o contrato para estratégias de falha
 */
public interface EstrategiaFalha {
    /**
     * Processa uma mensagem aplicando a estratégia de falha
     * @param msg Mensagem a ser processada
     * @param log Registrador de log para auditoria
     * @return true se a mensagem deve continuar sendo processada, false se deve ser descartada
     * @throws InterruptedException se a thread for interrompida durante o processamento
     */
    boolean processar(Mensagem msg, RegistradorLog log) throws InterruptedException;
    
    /**
     * Retorna o nome da estratégia para logging
     */
    String getNome();
}
