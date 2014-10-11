package org.mars.proxybase;

import java.net.*;
import java.io.*;
import java.util.Properties;

public class ProxyBase {
	
	public static String DEFAULT_PORT_IN = "dport_in";
	public static String DEFAULT_PORT_OUT = "dport_out";
	public static String DEFAULT_HOST = "dhost";
	public static String DEFAULT_PROTOCOL = "dprotocol";
	public static String POST = "POST";
	public static String GET = "GET";
	
    public static void main(String[] args) throws IOException {
        ServerSocket serverSocket = null;
        boolean listening = true;
    	Properties prop = readProperties();
    	int port = 80;
    	String tempPort=prop.getProperty(DEFAULT_PORT_IN);
    	System.out.println(args.length);
    	if (args.length>0) {
    		tempPort=args[0];
    	}
        try {
            port = Integer.parseInt(tempPort);
        } catch (Exception e) {
            System.out.println(tempPort + " is not a valid port. Setting default port to " + port);
        }

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("ProxyBase listening on port: " + port);
        } catch (IOException e) {
            System.err.println("Could not listen on port: " + port);
            System.exit(-1);
        }

        while (listening) {
            new ProxyThread(serverSocket.accept(),prop).start();
        }
        serverSocket.close();
    }
    
    public static Properties readProperties() {
    	InputStream input = null;
    	Properties prop = new Properties();
    	try {
    		input = ProxyBase.class.getClassLoader().getResourceAsStream("org/mars/proxybase/config.properties");
    		prop.load(input);
    	} catch (IOException ex) {
    		System.out.println("Properties file not loaded!. Setting default values manually.");
    		prop.setProperty(DEFAULT_PORT_IN, "80");
    		prop.setProperty(DEFAULT_PORT_OUT, "8080");
    		prop.setProperty(DEFAULT_HOST, "127.0.0.1");
    		prop.setProperty(DEFAULT_PROTOCOL, "http");
    	} finally {
    		if(input!=null) {
    			try {
    				input.close();
    			} catch (Exception e) {
    				System.out.println("Not able to close properties file");
    			}
    		}
    	}
    	return prop;
    }
}