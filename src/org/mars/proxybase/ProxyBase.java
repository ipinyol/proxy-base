package org.mars.proxybase;

import java.net.*;
import java.io.*;

public class ProxyBase {
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;

        int port = 10000;	//default
        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.out.println(args[0] + " is not a valid port. Listening to port " + port);
        }

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("ProxyBase listening on port: " + port);
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + args[0]);
            System.exit(-1);
        }

        while (listening) {
            new ProxyThread(serverSocket.accept()).start();
        }
        serverSocket.close();
    }
}