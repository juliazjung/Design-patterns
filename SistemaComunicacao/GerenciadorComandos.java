import java.util.Stack;
import java.util.ArrayList;
import java.util.List;

/**
 * Padrão Command: Invoker que gerencia execução e histórico de comandos
 */
public class GerenciadorComandos {
    private Stack<Comando> historico;
    private Stack<Comando> desfeitos;
    private List<Comando> todosComandos; // Para auditoria
    
    public GerenciadorComandos() {
        this.historico = new Stack<>();
        this.desfeitos = new Stack<>();
        this.todosComandos = new ArrayList<>();
    }
    
    // Executa um comando e adiciona ao histórico
    public boolean executarComando(Comando comando) {
        boolean sucesso = comando.executar();
        
        if (sucesso) {
            historico.push(comando);
            desfeitos.clear(); // Limpa redo após nova ação
            todosComandos.add(comando);
            
            GerenciadorLog.getInstancia().registrarGlobal(
                "Comando executado: " + comando.getNome());
        }
        
        return sucesso;
    }
    
    public boolean desfazerUltimo() {
        if (historico.isEmpty()) {
            System.out.println("Nenhum comando para desfazer");
            return false;
        }
        
        Comando comando = historico.pop();
        if (!comando.podeDesfazer()) {
            System.out.println("Comando '" + comando.getNome() + "' não pode ser desfeito");
            historico.push(comando); // Devolve ao histórico
            return false;
        }
        
        boolean sucesso = comando.desfazer();
        if (sucesso) {
            desfeitos.push(comando);
            GerenciadorLog.getInstancia().registrarGlobal(
                "Comando desfeito: " + comando.getNome());
        }
        
        return sucesso;
    }
    
    public boolean refazerUltimo() {
        if (desfeitos.isEmpty()) {
            System.out.println("Nenhum comando para refazer");
            return false;
        }
        
        Comando comando = desfeitos.pop();
        boolean sucesso = comando.executar();
        if (sucesso) {
            historico.push(comando);
            GerenciadorLog.getInstancia().registrarGlobal(
                "Comando refeito: " + comando.getNome());
        }
        
        return sucesso;
    }
    
    public void mostrarHistorico() {
        System.out.println("\n=== HISTÓRICO DE COMANDOS ===");
        if (todosComandos.isEmpty()) {
            System.out.println("Nenhum comando executado ainda");
        } else {
            for (int i = 0; i < todosComandos.size(); i++) {
                Comando cmd = todosComandos.get(i);
                System.out.println((i + 1) + ". " + cmd.getNome() + 
                                 " - " + cmd.getResultado());
            }
        }
        System.out.println("============================\n");
    }
    
    public boolean executarMacro(List<Comando> comandos) {
        System.out.println("Executando macro com " + comandos.size() + " comandos...");
        for (Comando comando : comandos) {
            if (!executarComando(comando)) {
                System.out.println("Macro interrompida em: " + comando.getNome());
                return false;
            }
        }
        System.out.println("Macro executada com sucesso");
        return true;
    }
}