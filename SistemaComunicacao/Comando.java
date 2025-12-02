/**
 * Padrão Command: Interface que define o contrato para comandos
 */
public interface Comando {
    /**
     * Executa o comando
     * @return true se executou com sucesso, false caso contrário
     */
    boolean executar();
    
    /**
     * Desfaz o comando (se possível)
     * @return true se desfez com sucesso, false caso contrário
     */
    boolean desfazer();
    
    // Verifica se o comando pode ser desfeito
    boolean podeDesfazer();
    
    // Retorna o nome do comando para histórico
    String getNome();
    
    // Retorna descrição do resultado da execução
    String getResultado();
}