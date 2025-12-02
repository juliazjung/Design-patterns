/**
 * Factory para criar comandos a partir de strings
 */
public class ComandoFactory {
    
    public static Comando criarComando(String entrada, No no, MetricasObserver metricas) {
        String[] partes = entrada.trim().split("\\s+", 2);
        String comando = partes[0].toLowerCase();
        
        switch (comando) {
            case "broadcast":
                if (partes.length < 2) {
                    throw new IllegalArgumentException("Uso: broadcast <mensagem>");
                }
                return new ComandoBroadcast(no, partes[1]);
            
            case "connect":
                if (partes.length < 2) {
                    throw new IllegalArgumentException("Uso: connect <idNo>");
                }
                return new ComandoConectar(no, partes[1]);
            
            case "metricas":
                return new ComandoMostrarMetricas(no, metricas);
            
            case "estado":
                return new ComandoMostrarEstado(no);
            
            case "ativar":
                return new ComandoMudarEstado(no, "ativar");
            
            case "recuperar":
                return new ComandoMudarEstado(no, "recuperar");
            
            case "falhar":
                if (partes.length < 2) {
                    throw new IllegalArgumentException("Uso: falhar <motivo>");
                }
                return new ComandoMudarEstado(no, "falhar", partes[1]);
            
            case "falha":
                if (partes.length < 2) {
                    throw new IllegalArgumentException("Uso: falha <tipo> <on/off>");
                }
                String[] subPartes = partes[1].split("\\s+");
                if (subPartes.length != 2) {
                    throw new IllegalArgumentException("Uso: falha <omissao|tempo> <on|off>");
                }
                boolean ativar = subPartes[1].equalsIgnoreCase("on");
                return new ComandoAtivarFalha(no, subPartes[0], ativar);
            
            case "shutdown":
            case "exit":
                return new ComandoDesligar(no, metricas);
            
            default:
                throw new IllegalArgumentException("Comando desconhecido: " + comando);
        }
    }
}