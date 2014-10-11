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
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			//InputStream in = socket.getInputStream();
            String inputLine;
            int cnt = 0;
            String postfix = "";
            String operation="";
            ///////////////////////////////////
            //begin get request from client
            /*
            byte[] b = new byte[BUFFER_SIZE];
            StringBuilder sb = new StringBuilder();
            System.out.println("Before while");
            while ((in.read(b)) >= 0) {
            	System.out.println("In while");	
            	sb.append(b);
            	System.out.println(sb.toString());
            }
            String input = sb.toString();
            System.out.println(input);
            String[] lines = input.split("\r\n");
            
            postfix = lines[0].split(" ")[1];	// The end point
            operation = lines[0].split(" ")[0];	// POST | GET | PUT | DELETE...
            */
            /*
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
                    postfix = tokens[1];	// The end point
                    operation = tokens[0];	// POST | GET | PUT | DELETE...
                    //can redirect this to output log
                    //System.out.println("Request for : " + postfix);
                    System.out.println(tokens[0]);
                }
                System.out.println("DD:" + inputLine);
                cnt++;
            }*/
            int postDataI = -1;
            Map<String,String> headerMap = new HashMap<String,String>();
            while ((inputLine = in.readLine()) != null && (inputLine.length() != 0)) {
                System.out.println("HTTP-HEADER: " + inputLine);
                if (inputLine.indexOf("Content-Length:") > -1) {
                    postDataI = new Integer(
                    		inputLine.substring(
                    				inputLine.indexOf("Content-Length:") + 16,
                    				inputLine.length())).intValue();
                }
                if (cnt == 0) {
                	String[] tokens = inputLine.split(" ");
                    postfix = tokens[1];	// The end point
                    operation = tokens[0];	// POST | GET | PUT 
                    System.out.println(postfix);
                    System.out.println(operation);
                } else {
                	String headers[] = inputLine.split(":");
                	if(headers.length==2) {
                		headerMap.put(headers[0], headers[1]);
                	}
                }
                cnt++;
            }
            String postData = "";
            // read the post data, only if postDataI > 0 
            if (postDataI > 0) {
                char[] charArray = new char[postDataI];
                in.read(charArray, 0, postDataI);
                postData = new String(charArray);
            }
            System.out.println("POST DATA:" + postData);
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
                
                /*
                if (operation.equals(ProxyBase.GET)) {
                	conn.setDoOutput(false);	
                } else {
                	conn.setDoOutput(true);	
                }*/
                
                //write the headers
                for(String key:headerMap.keySet()) {
                	String value = headerMap.get(key);
                	conn.setRequestProperty(key, value);
                }
               
                //Write the output data if necessary
                if(postDataI>0) {
                	conn.setDoOutput(true);
                	OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
                	wr.write(postData);
                	wr.flush();
                } else {
                	conn.setDoOutput(false);
                }
                
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
    	if (is!=null) {
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
}