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
            /*
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
            */
            
            DataInputStream ins=new DataInputStream(new BufferedInputStream(socketDestiny.getInputStream()));
            Header header = readHeader(ins);
            Content content = readContent(ins, header);
            
            // Write the response
            writeResponse(header, content, out);
            out.flush();
            ins.close();
            socketDestiny.close();
            //System.out.println("Writing headers");
            //out.writeBytes(dataToReturn.toString()+ "\r\n");
            /*
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
    
    private void writeResponse(Header header, Content content, DataOutputStream out) {
        List<byte[]> headers = header.getRawHeaderList();
        List<byte[]> contents = content.getRawContentList();
        try {
            for(byte[] arr:headers) {
                out.write(arr);
            }
            out.writeBytes("\r\n");
            for(byte[] arr:contents) {
                out.write(arr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Content readContent(DataInputStream ins, Header header) {
        Content out = new Content();
        long len = header.getContentLength();
        long pointer = 0;
        try {
            while (len-pointer >=BUFFER_SIZE) {
                byte[] chunk = new byte[BUFFER_SIZE];
                ins.read(chunk, 0, BUFFER_SIZE);
                out.addArray(chunk);
                len = len + BUFFER_SIZE;
            }
            if (len-pointer>0) {
                byte[] chunk = new byte[(int)(len-pointer)];
                out.addArray(chunk);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }
    
    private Header readHeader(DataInputStream ins) {
        Header out = new Header();
        try {
            byte[] oldInput = new byte[2];
            oldInput[0] = 0;
            oldInput[1] = 0;
            byte[] input = new byte[2];
            ins.read(input, 0, 2);
            int bufferSize = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            StringBuffer line = new StringBuffer();
            while(!isBreak(oldInput) || !isBreak(input)) {
                // Save the read data
                buffer[bufferSize] = input[0];
                buffer[bufferSize+1] = input[1];
                bufferSize = (bufferSize +2) % BUFFER_SIZE;
                if (bufferSize==0) {
                    out.addRawArray(buffer);
                }
                
                // Parse the content
                if (isBreak(input)) {
                    System.out.println("isBreak!!!");
                    String lineStr = line.toString();
                    System.out.println(lineStr);
                    if (lineStr.contains(":")) {
                        // We are not in first line
                        String [] tokens = lineStr.split(":");
                        if (tokens[0].equals("Content-Length")) {
                            Long len = Long.parseLong(tokens[1]);
                            System.out.println("Detected: " + len);
                            out.setContentLength(len);
                        } else if (tokens[0].equals("Host")) {
                            System.out.println("Detected: " + tokens[1] + ":" +tokens[2]);
                            out.setHost(tokens[1] + ":" +tokens[2]);
                        }
                    } else {
                        // we are in the first line of HTTP protocol
                        String [] tokens = lineStr.split(" ");
                        out.setOperation(tokens[0]);
                        out.setUrl(tokens[1]);
                        System.out.println("Detected: " + tokens[1]);
                    }

                    line = new StringBuffer();
                } else {
                    line.append(new String(input, "UTF-8"));
                }

                oldInput[0] = input[0];
                oldInput[1] = input[1];
                ins.read(input, 0, 2);
            }
            System.out.println("out");
            if (bufferSize!=0) {
                byte []last = new byte[bufferSize];
                System.arraycopy(buffer, 0, last, 0, bufferSize);
                out.addRawArray(last);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }
    
    /**
     * Return true of val[0]=='\r' and val[1]=='\n' (0x13 and 0x10 respectively)
     * @param val
     * @return
     */
    private boolean isBreak(byte[] val) {
        return (val[0]==13 && val[1]==10);
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
    
    private class Content {
        private List<byte[]> rawContentList = new ArrayList<byte[]>();
        
        public void setRawContentList(List<byte[]> rawContentList) {
            this.rawContentList = rawContentList;
        }
        
        public List<byte[]> getRawContentList() {
            return this.rawContentList;
        }
        
        public void addArray(byte[] array) {
            if (rawContentList==null) {
                this.rawContentList = new ArrayList<byte[]>();
            }
            this.rawContentList.add(array);
        }
    }
    
    private class Header {
        private Content rawHeaderList = new Content();
        private String host = null;
        private Long contentLength = null;      
        private String operation= null;         // GET, POST
        private String url = null;
        
        public void setRawHeaderList(List<byte[]> rawHeaderList) {
            this.rawHeaderList.setRawContentList(rawHeaderList);
        }
        
        public List<byte[]> getRawHeaderList() {
            return this.rawHeaderList.getRawContentList();
        }
        
        public void addRawArray(byte[] array) {
            this.rawHeaderList.addArray(array);
        }
        
        public String getHost() {
            return host;
        }
        public void setHost(String host) {
            this.host = host;
        }
        public Long getContentLength() {
            return contentLength;
        }
        public void setContentLength(Long contentLength) {
            this.contentLength = contentLength;
        }
        public String getOperation() {
            return operation;
        }
        public void setOperation(String operation) {
            this.operation = operation;
        }
        public String getUrl() {
            return url;
        }
        public void setUrl(String url) {
            this.url = url;
        }
    }
}