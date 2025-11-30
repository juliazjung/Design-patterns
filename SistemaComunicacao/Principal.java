import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
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
        Registry registro = null; // Inicialização explícita

        try {
            // Tenta conectar ao registry existente
            try {
                registro = LocateRegistry.getRegistry();
                registro.list(); // Testa se o registry está ativo
            } catch (Exception e) {
                // Se não existir, cria um novo (apenas o primeiro nó deve fazer isso)
                if (idNo.equals("No1")) {
                    registro = LocateRegistry.createRegistry(1099);
                    System.out.println("Registry criado na porta 1099");
                } else {
                    System.err.println("Registry não encontrado. Inicie o No1 primeiro.");
                    System.exit(1);
                }
            }

            // Garante que o registro foi inicializado
            if (registro == null) {
                System.err.println("Falha crítica: Registry não inicializado");
                System.exit(1);
            }

            // Criar o nó
            No no = new No(idNo);
            registro.rebind(idNo, no);
            System.out.println(idNo + " registrado no RMI Registry");
            System.out.println("Nós registrados: " + Arrays.toString(registro.list()));

            // Verificar nós disponíveis

            // Interface de controle
            Scanner scanner = new Scanner(System.in);
            System.out.println("\nComandos disponíveis para " + idNo + ":");
            System.out.println("  connect <no> - Conectar a outro nó (ex: connect No2)");
            System.out.println("  broadcast <mensagem> - Enviar mensagem para todos");
            System.out.println("  falha omissao <on/off> - Ativar/desativar falha por omissão");
            System.out.println("  falha tempo <on/off> - Ativar/desativar falha por temporização");
            System.out.println("  shutdown - Desligar este nó");
            System.out.println("  exit - Sair do programa");

            while (true) {
                System.out.print(idNo + "> ");
                String comando = scanner.nextLine();

                if (comando.equalsIgnoreCase("exit") || comando.equalsIgnoreCase("shutdown")) {
                    no.desligar();
                    registro.unbind(idNo);
                    System.out.println(idNo + " desligado");
                    break;

                } else if (comando.startsWith("connect ")) {
                    String outroNoId = comando.substring(8).trim();
                    try {
                        System.out.println("Procurando " + outroNoId + " no registry...");
                        NoInterface outroNo = (NoInterface) registro.lookup(outroNoId);
                        System.out.println("Encontrado " + outroNoId + ", estabelecendo conexão...");
                        no.adicionarVizinho(outroNo);
                        System.out.println("Conexão bilateral estabelecida com " + outroNoId);
                    } catch (NotBoundException e) {
                        System.err.println("ERRO: " + outroNoId + " não encontrado no registry.");
                    } catch (RemoteException e) {
                        System.err.println("Erro na conexão: " + e.getMessage());
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
                            System.err.println("Erro ao configurar falha: " + e.getMessage());
                        }
                    } else {
                        System.out.println("Uso: falha omissao <on/off>");
                    }

                } else if (comando.startsWith("falha tempo ")) {
                    String[] parts = comando.split(" ");
                    if (parts.length == 3) {
                        try {
                            boolean ativar = parts[2].equalsIgnoreCase("on");
                            no.setSimularFalhaTemporizacao(ativar);
                            System.out.println("Falha por temporização " + (ativar ? "ativada" : "desativada"));
                        } catch (RemoteException e) {
                            System.err.println("Erro ao configurar falha: " + e.getMessage());
                        }
                    }
                    
                } else {
                    System.out.println("Comando inválido");
                }
            }

            System.exit(0);

        } catch (

        Exception e) {
            System.err.println("Erro no nó " + idNo + ":");
            e.printStackTrace();
        }
    }
}