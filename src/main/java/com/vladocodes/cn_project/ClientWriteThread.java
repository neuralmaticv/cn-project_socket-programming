package com.vladocodes.cn_project;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class ClientWriteThread extends Thread {
    private final String username;
    private PrintWriter toServer;

    ClientWriteThread(Socket socket, String username) {
        this.username = username;

        try {
            this.toServer = new PrintWriter(socket.getOutputStream(), true);
        } catch (IOException e) {
            System.out.println("Error getting output stream: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // Send username
        // this.toServer.println(username);
        // Then send input to server line by line, until exit
        try (Scanner sc = new Scanner(System.in)) {
            String userInput;
            StringBuilder sb = new StringBuilder();
            sb.append("Dobro došli ").append(this.username).append("\n");
            sb.append("Opcije:").append("\n");
            sb.append("1. Unesite \"igraj\" za početak nove igre;\n");
            sb.append("2. Unesite \"posmatraj\" za posmatranje partije;\n");
            sb.append("3. Unesite \"info\" za statusne informacije.\n");
            sb.append("\t\t\t");
            System.out.println(sb);
            System.out.print("[?] Izaberite opciju:");

            do {
                // carriage return, rewrite previous msg
                System.out.printf("\r[%s]: ", this.username);
                userInput = sc.nextLine();

                if (userInput.equals("igraj")) {
                    System.out.print("[?] Unesite ime igrača:");
                    userInput = "igraj-" + sc.nextLine();
                } else if (userInput.equals("posmatraj")) {
                    System.out.print("[?] Unesite ID igre:");
                    userInput = "posmatraj-" + sc.nextLine();
                }

                toServer.println(userInput);
            } while (!userInput.equals("exit"));
        }
    }
}

