package no.ntnu.datakomm;

import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket clientSocket;



    public ClientHandler(Socket clientSocket) {
        this.clientSocket = clientSocket;
    }


    public void run() {
        try {
            BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
            PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
            boolean mustRun = true;
            while (mustRun) {
                String nextLine = in.readLine();
                if (nextLine.equalsIgnoreCase("game over")) {
                    mustRun = false;
                    System.out.println("Game over");
                } else {
                    nextLine.replaceAll("[^0-9+]", "");
                    String[] numbers =  nextLine.split("\\+");
                    try {
                        System.out.println(numbers[0]);
                        System.out.println(numbers[1]);
                        System.out.println(numbers[2]);

                        int firstNum = Integer.parseInt(numbers[0]);
                        int secondNum = Integer.parseInt(numbers[1]);
                        int calc = firstNum + secondNum;
                        String response = "" + calc;
                        out.println(response);
                    } catch (Exception e ) {
                        out.println("Error");
                        System.out.println("Error in reading response");;
                    }
                }
            }
        clientSocket.close();


        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
