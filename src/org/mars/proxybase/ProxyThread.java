package org.mars.proxybase;

import java.net.*;
import java.io.*;
import java.util.*;

public class ProxyThread extends Thread {
    private Socket socket = null;
    private static final int BUFFER_SIZE = 32768;
    private Properties prop = null; 
    public ProxyThread(Socket socket, Properties prop) {
        super("ProxyThread");
        this.socket = socket;
        this.prop = prop;
    }

    public void run() {
        //get input from user
        //send request to server
        //get response from server
        //send response to user

        try {
            DataOutputStream out =
		new DataOutputStream(socket.getOutputStream());
            BufferedReader in = new BufferedReader(
		new InputStreamReader(socket.getInputStream()));

            String inputLine;
            int cnt = 0;
            String postfix = "";
            ///////////////////////////////////
            //begin get request from client
            while ((inputLine = in.readLine()) != null) {
                try {
                    StringTokenizer tok = new StringTokenizer(inputLine);
                    tok.nextToken();
                } catch (Exception e) {
                    break;
                }
                //parse the first line of the request to find the url
                if (cnt == 0) {
                    String[] tokens = inputLine.split(" ");
                    postfix = tokens[1];
                    //can redirect this to output log
                    System.out.println("Request for : " + postfix);
                }

                cnt++;
            }
            //end get request from client
            ///////////////////////////////////


            BufferedReader rd = null;
            try {
                //begin send request to server, get response from server
            	String urlToCall="";
            	urlToCall = prop.getProperty(ProxyBase.DEFAULT_PROTOCOL)+"://";
            	urlToCall += prop.getProperty(ProxyBase.DEFAULT_HOST) + ":";
            	urlToCall += prop.getProperty(ProxyBase.DEFAULT_PORT_OUT);
            	urlToCall += postfix; // postfix already includes "/"
                URL url = new URL(urlToCall);
                URLConnection conn = url.openConnection();
                conn.setDoInput(true);
                //not doing HTTP posts
                conn.setDoOutput(false);
                System.out.println("Type is: " + conn.getContentType());
                System.out.println("content length: " + conn.getContentLength());
                System.out.println("allowed user interaction: " + conn.getAllowUserInteraction());
                System.out.println("content encoding: " + conn.getContentEncoding());
                System.out.println("content type: " + conn.getContentType());

                // Get the response
                InputStream is = null;
                HttpURLConnection huc = (HttpURLConnection)conn;
                if (conn.getContentLength() > 0) {
                    try {
                        is = conn.getInputStream();
                        rd = new BufferedReader(new InputStreamReader(is));
                    } catch (IOException ioe) {
                        System.out.println("********* IO EXCEPTION **********: " + ioe);
                    }
                }
                //end send request to server, get response from server
                ///////////////////////////////////

                //begin send response to client
                byte by[] = new byte[ BUFFER_SIZE ];
                int index = is.read( by, 0, BUFFER_SIZE );
                while ( index != -1 )
                {
                  out.write( by, 0, index );
                  index = is.read( by, 0, BUFFER_SIZE );
                }
                out.flush();

            } catch (Exception e) {
                //can redirect this to error log
                System.err.println("Encountered exception: " + e);
                //encountered error - just send nothing back, so
                //processing can continue
                out.writeBytes("");
            }

            //close out all resources
            if (rd != null) {
                rd.close();
            }
            if (out != null) {
                out.close();
            }
            if (in != null) {
                in.close();
            }
            if (socket != null) {
                socket.close();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}