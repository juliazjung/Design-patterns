import java.rmi.RemoteException;
import java.util.Arrays;
import java.util.Scanner;

public class Principal {
    public static void main(String[] args) {
        if (args.length != 1) {
            System.err.println("Uso: java Principal <ID_No>");
            System.err.println("Exemplo: java Principal No1");
            System.exit(1);
        }

        String idNo = args[0];

        try {
            // Usa o Singleton do Registry
            RegistryManager registryManager = RegistryManager.getInstancia();
            
            No no = new No(idNo);

            // Adiciona observador de métricas
            MetricasObserver metricas = new MetricasObserver();
            no.adicionarObservador(metricas);
            
            // Registra usando o singleton
            registryManager.registrarNo(idNo, no);
            
            // Lista nós usando o singleton
            GerenciadorLog.getInstancia().registrarGlobal(
                "Nós registrados: " + Arrays.toString(registryManager.listarNos()));

            // Interface de controle
            Scanner scanner = new Scanner(System.in);
            System.out.println("\nComandos disponíveis para " + idNo + ":");
            System.out.println("  connect <no> - Conectar a outro nó");
            System.out.println("  broadcast <mensagem> - Enviar mensagem");
            System.out.println("  falha omissao <on/off> - Ativar/desativar falha por omissão");
            System.out.println("  falha tempo <on/off> - Ativar/desativar falha por temporização");
            System.out.println("  shutdown - Desligar este nó");
            System.out.println("  exit - Sair do programa");

            while (true) {
                System.out.print(idNo + "> ");
                String comando = scanner.nextLine();

                if (comando.equalsIgnoreCase("exit") || comando.equalsIgnoreCase("shutdown")) {
                    // Imprime métricas antes de sair
                    metricas.imprimirRelatorio();

                    no.desligar();
                    // Usa singleton
                    registryManager.removerNo(idNo);
                    GerenciadorLog.getInstancia().registrar(idNo, "Nó desligado");
                    GerenciadorLog.getInstancia().fecharLogs();
                    break;

                } else if (comando.startsWith("connect ")) {
                    String outroNoId = comando.substring(8).trim();
                    try {
                        System.out.println("Procurando " + outroNoId + " no registry...");
                        
                        // Usa singleton
                        NoInterface outroNo = registryManager.buscarNo(outroNoId);
                        
                        System.out.println("Encontrado, estabelecendo conexão...");
                        no.adicionarVizinho(outroNo);
                        System.out.println("Conexão bilateral estabelecida com " + outroNoId);
                    } catch (Exception e) {
                        System.err.println("ERRO: " + e.getMessage());
                    }

                } else if (comando.startsWith("broadcast ")) {
                    String mensagem = comando.substring(10).trim();
                    no.broadcast(mensagem);
                    System.out.println("Mensagem enviada para todos os vizinhos");
                
                } else if (comando.startsWith("falha omissao ")) {
                    String[] parts = comando.split(" ");
                    if (parts.length == 3) {
                        try {
                            boolean ativar = parts[2].equalsIgnoreCase("on");
                            no.setSimularFalhaOmissao(ativar);
                            System.out.println("Falha por omissão " + (ativar ? "ativada" : "desativada"));
                        } catch (RemoteException e) {
                            System.err.println("Erro: " + e.getMessage());
                        }
                    }

                } else if (comando.startsWith("falha tempo ")) {
                    String[] parts = comando.split(" ");
                    if (parts.length == 3) {
                        try {
                            boolean ativar = parts[2].equalsIgnoreCase("on");
                            no.setSimularFalhaTemporizacao(ativar);
                            System.out.println("Falha por temporização " + (ativar ? "ativada" : "desativada"));
                        } catch (RemoteException e) {
                            System.err.println("Erro: " + e.getMessage());
                        }
                    }
                    
                // NOVO COMANDO: mostrar métricas
                else if (comando.equalsIgnoreCase("metricas")) {
                    metricas.imprimirRelatorio();
                }

                } else {
                    System.out.println("Comando inválido");
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