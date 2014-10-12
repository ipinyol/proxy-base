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
            StringBuffer dataToSent = new StringBuffer();
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
                    dataToSent.append(inputLine+"\r\n");
                } else {
                	String headers[] = inputLine.split(":");
                	if(headers.length==2) {
                		headerMap.put(headers[0], headers[1]);
                	}
                	if (headers[0].equals("Host")) {
                		String newLine = "Host:" + prop.getProperty(ProxyBase.DEFAULT_HOST)+":";
                		newLine += prop.getProperty(ProxyBase.DEFAULT_PORT_OUT) + "\r\n";
                		dataToSent.append(newLine);
                	} else {
                		dataToSent.append(inputLine+"\r\n");
                	}
                }
                cnt++;
            }
            dataToSent.append("\r\n");
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

            // Send data to new port
            int portDestiny = Integer.parseInt(prop.getProperty(ProxyBase.DEFAULT_PORT_OUT));
            Socket socketDestiny = new Socket();
            String ipAddress =  prop.getProperty(ProxyBase.DEFAULT_HOST);
            socketDestiny.connect(new InetSocketAddress(ipAddress, portDestiny), 5000);
            
            DataOutputStream os = new DataOutputStream(socketDestiny.getOutputStream());
            //DataInputStream is = new DataInputStream(socketDestiny.getInputStream());
            
            System.out.println("Writing to PORT");
            System.out.println(dataToSent.toString());
            os.writeBytes(dataToSent.toString());
            if (postDataI>0) {
            	os.writeBytes(postData);
            }
            os.flush();
            ////////////////////
            
            // Get the response
            BufferedReader is = new BufferedReader(new InputStreamReader(socketDestiny.getInputStream()));
            StringBuffer dataToReturn = new StringBuffer();
            System.out.println("Waiting for response");
            Integer lenData=null;
            while ((inputLine = is.readLine()) != null && (inputLine.length() != 0)) {
            	dataToReturn.append(inputLine + "\r\n");
            	if (inputLine.indexOf("Content-Length:") > -1) {
                    lenData = new Integer(
                    		inputLine.substring(
                    				inputLine.indexOf("Content-Length:") + 16,
                    				inputLine.length())).intValue();
                }
            	System.out.println(inputLine + "\r\n");
            }
            //System.out.println("Writing headers");
            //out.writeBytes(dataToReturn.toString()+ "\r\n");
            dataToReturn.append("\r\n");
            
            if (lenData!=null) {
            	char by[] = new char[ lenData ];
            	is.read(by, 0, lenData);
            	dataToReturn.append(new String(by));
            } else {
            	//TODO: Chuncked data!
            }
            System.out.println("Writing everything");
	        out.writeBytes(dataToReturn.toString()+"\r\n");
	        out.flush();
            //dataToReturn.append("\r\n");
            ////////////////////
            is.close();
            os.close();
            socketDestiny.close();
            // Return to output
            
            /*
            BufferedReader rd = null;
            try {
                //begin send request to server, get response from server
            	String urlToCall="";
            	urlToCall = prop.getProperty(ProxyBase.DEFAULT_PROTOCOL)+"://";
            	urlToCall += prop.getProperty(ProxyBase.DEFAULT_HOST) + ":";
            	urlToCall += prop.getProperty(ProxyBase.DEFAULT_PORT_OUT);
            	urlToCall += postfix; // postfix already includes "/"
                URL url = new URL(urlToCall);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setDoInput(true);
                conn.setAllowUserInteraction(true);
                /*
                if (operation.equals(ProxyBase.GET)) {
                	conn.setDoOutput(false);	
                } else {
                	conn.setDoOutput(true);	
                }
                boolean isChuncked = false;
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
                conn.connect();
                if (conn.getContentLength() > 0 || true) {
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
                isChuncked = writeHeaders(conn, out);
                //begin send response content to client
                if (isChuncked) {
                	System.out.println("CHUUUUUU");
                	writeOutputChuncked(is, out);
                } else {
                	writeOutput(is, out);
                }
                
                out.flush();

            } catch (Exception e) {
                //can redirect this to error log
                System.err.println("Encountered exception: " + e);
                //encountered error - just send nothing back, so
                //processing can continue
                out.writeBytes("");
            }*/

            //close out all resources
            //if (rd != null) {
            //    rd.close();
            //}
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
    
    private static boolean writeHeaders(HttpURLConnection conn, DataOutputStream out) {
    	Map<String, List<String>> map = conn.getHeaderFields();
    	boolean isChunked=false;
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
	    			isChunked = isChunked || entry.getValue().get(0).equals("chunked");
	    			System.out.print(points + entry.getValue().get(0)+"\r\n");
	    		}
	    	}
	    	out.writeBytes("\r\n");		// The White 
	    	System.out.print("\r\n");
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return isChunked;
    }
    
    private static void writeOutput(InputStream is, DataOutputStream out) {
    	if (is!=null) {
	    	byte by[] = new byte[ BUFFER_SIZE ];
	    	try {
		        int index = is.read( by, 0, BUFFER_SIZE );
		        while ( index != -1 ) {
		          out.write( by, 0, index );
		          index = is.read( by, 0, BUFFER_SIZE );
		        }
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
    	} else {
    		System.out.println("InputStream is null!!!!");
    	}
    }
    
    private static void writeOutputChuncked(InputStream is, DataOutputStream out) {
    	if (is!=null) {
	    	byte by[] = new byte[ BUFFER_SIZE ];
	    	try {
		        int index = is.read(by);
		        while ( index != -1 ) {
		        	String aux = ""+index;
		        	out.writeChars(aux + "\r\n");
		        	out.write( by, 0, index );
		        	index = is.read(by);
		        }
		        out.writeChars("0\r\n");
		        out.writeChars("\r\n");
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
    	} else {
    		System.out.println("InputStream is null!!!!");
    	}
    	
    }
}