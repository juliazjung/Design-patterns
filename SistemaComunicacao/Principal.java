import java.util.Scanner;

public class Principal {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java Principal <ID_No>");
            System.exit(1);
        }

        String idNo = args[0];

        try {
            RegistryManager registryManager = RegistryManager.getInstancia();
            No no = new No(idNo);
            
            MetricasObserver metricas = new MetricasObserver();
            no.adicionarObservador(metricas);
            
            registryManager.registrarNo(idNo, no);
            
            // Cria gerenciador de comandos
            GerenciadorComandos gerenciadorComandos = new GerenciadorComandos();
            
            Scanner scanner = new Scanner(System.in);
            System.out.println("\nComandos disponíveis para " + idNo + ":");
            System.out.println("  connect <no> - Conectar a outro nó");
            System.out.println("  broadcast <mensagem> - Enviar mensagem");
            System.out.println("  falha <tipo> <on/off> - Ativar/desativar falha");
            System.out.println("  estado - Ver estado atual");
            System.out.println("  metricas - Mostrar métricas");
            System.out.println("  undo - Desfazer último comando");
            System.out.println("  redo - Refazer comando desfeito");
            System.out.println("  historico - Ver histórico de comandos");
            System.out.println("  shutdown/exit - Desligar nó");

            while (true) {
                System.out.print(idNo + "> ");
                String entrada = scanner.nextLine();
                
                // Comandos especiais do gerenciador
                if (entrada.equalsIgnoreCase("undo")) {
                    gerenciadorComandos.desfazerUltimo();
                    continue;
                }
                
                if (entrada.equalsIgnoreCase("redo")) {
                    gerenciadorComandos.refazerUltimo();
                    continue;
                }
                
                if (entrada.equalsIgnoreCase("historico")) {
                    gerenciadorComandos.mostrarHistorico();
                    continue;
                }
                
                // Cria e executa comando via factory
                try {
                    Comando comando = ComandoFactory.criarComando(entrada, no, metricas);
                    boolean sucesso = gerenciadorComandos.executarComando(comando);
                    
                    if (sucesso) {
                        System.out.println(comando.getResultado());
                        
                        // Se for comando de shutdown, sai do loop
                        if (comando instanceof ComandoDesligar) {
                            break;
                        }
                    } else {
                        System.err.println("Falha ao executar: " + comando.getResultado());
                    }
                    
                } catch (IllegalArgumentException e) {
                    System.err.println("ERRO: " + e.getMessage());
                } catch (Exception e) {
                    System.err.println("ERRO inesperado: " + e.getMessage());
                    e.printStackTrace();
                }
            }
            
            scanner.close();
            System.exit(0);

        } catch (Exception e) {
            System.err.println("Erro no nó " + idNo + ":");
            e.printStackTrace();
        }
    }
}