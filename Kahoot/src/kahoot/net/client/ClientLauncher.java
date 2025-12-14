package kahoot.net.client;

import kahoot.ui.GameUI;

public class ClientLauncher {
    public static void main(String[] args) {

        if (args.length != 5) {
            System.out.println("Uso: java kahoot.net.client.ClientLauncher <IP> <PORT> <JOGO> <EQUIPA> <USERNAME>");
            System.out.println("Exemplo: java kahoot.net.client.ClientLauncher localhost 9090 JOGO1 A Joao");
            return;
        }

        String host = args[0];
        int port = Integer.parseInt(args[1]);
        String team = args[3];
        String username = args[4];

        GameUI ui = new GameUI(username, team);

        ui.connectToServer(host, port);
    }
}
