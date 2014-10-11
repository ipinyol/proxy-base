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
                
                // begin send response headers to client
                
                //out.writeBytes(conn.getContentEncoding());
                writeHeaders(conn, out);
                //begin send response content to client
                writeOutput(is, out);
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
    
    private static void writeHeaders(URLConnection conn, DataOutputStream out) {
    	Map<String, List<String>> map = conn.getHeaderFields();
    	try {
	    	for (Map.Entry<String, List<String>> entry : map.entrySet()) {
	    		String points="";
	    		if (entry.getKey()!=null) {
	    			out.writeBytes(entry.getKey());
	    			points=":";
	    			System.out.print(entry.getKey());
	    		}
	    		if(entry.getValue().size()>0) {
	    			out.writeBytes(points + entry.getValue().get(0)+ "\r\n");
	    			System.out.print(points + entry.getValue().get(0)+"\r\n");
	    		}
	    	}
	    	out.writeBytes("\r\n");
	    	System.out.print("\r\n");
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    private static void writeOutput(InputStream is, DataOutputStream out) {
    	byte by[] = new byte[ BUFFER_SIZE ];
    	try {
	        int index = is.read( by, 0, BUFFER_SIZE );
	        while ( index != -1 )
	        {
	          out.write( by, 0, index );
	          index = is.read( by, 0, BUFFER_SIZE );
	        }
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    }
}