package com.vladocodes.cn_project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;

public class ClientReadThread extends Thread {
    private final String username;
    private BufferedReader fromServer;

    ClientReadThread(Socket socket, String username) {
        this.username = username;

        try {
            this.fromServer = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        } catch (IOException e) {
            System.out.println("Error getting input stream: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        // Continuously receive and print messages from the server
        while (true) {
            try {
                // Wait for message and print it
                String response = this.fromServer.readLine();
                if (response == null) {
                    System.err.println("\rConnection lost.");
                    return;
                }
                // carriage return, rewrite previous msg
                System.out.println("\r" + response);

                // Print prompt
                System.out.printf("\r[%s]: ", this.username);
            } catch (IOException e) {
                e.printStackTrace();
                break;
            }
        }
    }
}
