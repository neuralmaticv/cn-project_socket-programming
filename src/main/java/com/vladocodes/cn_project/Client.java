package com.vladocodes.cn_project;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Scanner;

public class Client {
    private final int port = Server.PORT;
    private final String hostname = "localhost";
    private final InetAddress address;

    public static void main(String[] args) throws IOException {
        Client client = new Client();
        System.err.println("Connecting to the port " + Server.PORT);
        client.execute();
    }

    public Client() throws IOException {
        this.address = InetAddress.getByName(this.hostname);
    }

    private void execute() {
        try (Socket clientSocket = new Socket(this.address, this.port);
             Scanner scanner = new Scanner(System.in);
             BufferedReader input = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             PrintWriter output = new PrintWriter(clientSocket.getOutputStream(), true)) {

            System.out.printf("Connected to the server %s:%d\n", this.hostname, this.port);
            System.out.print("[?] Unesite ime: ");
            String username = scanner.next();
            output.println(username);

            String response = input.readLine();
            if (response.equals("OK")) {
                Thread rt = new ClientReadThread(clientSocket, username);
                Thread wt = new ClientWriteThread(clientSocket, username);

                rt.start();
                wt.start();

                rt.join();
                wt.join();
            } else {
                System.out.println(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
