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

        try {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());
            
            // Getting incoming data 
            Header headerReq = readHeader(in);
            Content contentReq = readContent(in, headerReq);
            
            // Send data to new port
            int portDestiny = Integer.parseInt(prop.getProperty(ProxyBase.DEFAULT_PORT_OUT));
            Socket socketDestiny = new Socket();
            String ipAddress =  prop.getProperty(ProxyBase.DEFAULT_HOST);
            socketDestiny.connect(new InetSocketAddress(ipAddress, portDestiny), 5000);
            DataOutputStream os = new DataOutputStream(socketDestiny.getOutputStream());
            
            writeResponse(headerReq, contentReq, os);
            os.flush();
            
            // Read the response
            DataInputStream ins=new DataInputStream(new BufferedInputStream(socketDestiny.getInputStream()));
            Header header = readHeader(ins);
            Content content = readContent(ins, header);
            
            // Write the response
            writeResponse(header, content, out);
            out.flush();
            
            // Close channels and sockets
            ins.close();
            socketDestiny.close();
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
            	//String str = new String(arr,"UTF-8");
            	//System.out.println(str);
                out.write(arr);
            }
            //out.writeBytes("\r\n");
            for(byte[] arr:contents) {
                //String aa = new String(arr,"UTF-8");
                //System.out.println(aa);
                out.write(arr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private Content readContent(DataInputStream ins, Header header) {
        Content out = new Content();
        if (header.getContentLength()!=null) {
            // Read data by length
            long len = header.getContentLength();
            readBufferedContent(ins,len,out);
        } else if (header.isChunked()) {
            // read data by chunks
            out = readContentByChunk(ins);
        }
        // If no length and no chunk, then no data to process
        return out;
    }
    
    private void readBufferedContent(DataInputStream ins, long len, Content content) {
        long pointer = 0;
        try {
            while (len-pointer >=BUFFER_SIZE) {
                byte[] chunk = new byte[BUFFER_SIZE];
                chunk = readBytes(ins, BUFFER_SIZE);
                content.addArray(chunk);
                pointer = pointer + BUFFER_SIZE;
            }
            if (len-pointer>0) {
                byte[] chunk = new byte[(int)(len-pointer)];
                chunk = readBytes(ins, (int)(len-pointer));
                content.addArray(chunk);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Wrapper to DataInputStream.read, to make sure that the number of read bytes 
     * is exactly what is passed by parameter
     * @param ins
     * @param len
     * @return
     */
    private byte [] readBytes(DataInputStream ins, int len) {
    	int readSoFar = 0;
    	byte[] out = new byte[len];
    	boolean done = false;
    	try {
    		int readNow = ins.read(out, readSoFar, len-readSoFar);
	    	while(readSoFar<len || done) {
	    		if (readNow==-1) {
	    			done=true;
	    		} else {
	    			readSoFar += readNow;
	    			Thread.sleep(1); // Sleep for one millisecond
	    			readNow = ins.read(out, readSoFar, len-readSoFar);
	    		}
	    	}
    	} catch (Exception e) {
    		e.printStackTrace();
    	}
    	return out;
    }
    
    private Content readContentByChunk(DataInputStream ins) {
        Content out = new Content();
        byte[] endLine = new byte[2];
        endLine[0] = (byte) '\r';
        endLine[1] = (byte) '\n';
        byte[] input = readBytesUntil(ins, endLine);
        out.addArray(input);
        while(input.length>2) {
            long lenChunk = getLenChunk(input);
            if (lenChunk>0) {
                readBufferedContent(ins,lenChunk, out);
                input = readBytesUntil(ins, endLine); // We read the \r\n of the end of the chunk
                if(input.length!=2) {
                	try {
                	} catch (Exception e){ e.printStackTrace();};
                }
                out.addArray(input);
            }
            input = readBytesUntil(ins, endLine); // Again, we read the next number line
            out.addArray(input);
        }
        return out;
    }
    
    private long getLenChunk(byte[] input) {
        byte[] hexNum = new byte[input.length-2];
        System.arraycopy(input, 0, hexNum, 0, input.length-2);
        long hexNumLong=0;
        try {
            String hexNumStr = new String(hexNum,"UTF-8");
            hexNumLong = Long.parseLong(hexNumStr, 16);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hexNumLong;
    }
    
    // For short stuff
    private byte[] readBytesUntil(DataInputStream ins, byte[] endLine) {
        byte oldInput = 0;
        byte [] buffer = new byte[BUFFER_SIZE];
        byte [] out = null;
        int len= 0;
        try {
            byte input = ins.readByte();
            while (oldInput!=endLine[0] || input!=endLine[1]) {
            	if (len==0) System.out.println(input);
                buffer[len] = input;
                len++;
                oldInput=input;
                input = ins.readByte();
            }
            buffer[len] = input;
            len++;
            System.out.println("LEN in readBytesUntil:" + len + " Last char:" + input);
            out = new byte[len];
            System.arraycopy(buffer, 0, out, 0, len);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }
    
    private Header readHeader(DataInputStream ins) {
        Header out = new Header();
        try {
            byte[] oldInput = new byte[3];
            oldInput[0]=0;
            oldInput[1]=0;
            oldInput[2]=0;
            byte input = 0;
            input = ins.readByte();
            int bufferSize = 0;
            byte[] buffer = new byte[BUFFER_SIZE];
            StringBuffer line = new StringBuffer();
            while(!isBreakEnd(oldInput, input)) {
                // Save the read data
                buffer[bufferSize] = input;
                bufferSize = (bufferSize +1) % BUFFER_SIZE;
                if (bufferSize==0) {
                    out.addRawArray(buffer);
                }
                // Parse the content                
                if (isBreak(oldInput, input)) {
                	String lineStr = line.toString();
                    if (lineStr.contains(":")) {
                        // We are not in first line
                        String [] tokens = lineStr.split(":");
                        if (tokens[0].toLowerCase().trim().equals("content-length")) {
                            Long len = Long.parseLong(tokens[1].trim());
                            out.setContentLength(len);
                        } else if (tokens[0].toLowerCase().trim().equals("host")) {
                            out.setHost(tokens[1] + ":" +tokens[2]);
                        } else if (tokens[0].toLowerCase().equals("transfer-encoding")) {
                            out.setChunked(tokens[1].toLowerCase().trim().equals("chunked"));
                        }
                    } else {
                        // we are in the first line of HTTP protocol
                        String [] tokens = lineStr.split(" ");
                        out.setOperation(tokens[0]);
                        out.setUrl(tokens[1]);
                    }
                    line = new StringBuffer();
                } else {
                	byte[] aux = new byte[1];
                	aux[0] = input;
                    line.append(new String(aux, "UTF-8"));
                }
                oldInput[0] = oldInput[1];
                oldInput[1] = oldInput[2];
                oldInput[2] = input;
                input = ins.readByte();
            }
            buffer[bufferSize] = input;
            bufferSize = (bufferSize +1) % BUFFER_SIZE;
            if (bufferSize==0) {
                out.addRawArray(buffer);
            } else {
            	byte []last = new byte[bufferSize];
                System.arraycopy(buffer, 0, last, 0, bufferSize);
                out.addRawArray(last);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }
    
    private boolean isBreakEnd(byte[] oldInput, byte input) {
    	boolean b1 = (oldInput[0]==(byte)'\r' && oldInput[1]==(byte)'\n');
    	boolean b2 = (oldInput[2]==(byte)'\r' && input==(byte)'\n');
        return b1 && b2;
    }
    
    private boolean isBreak(byte[] oldInput, byte input) {
    	boolean b1 = (oldInput[2]==(byte)'\r' && input==(byte)'\n');
        return b1;
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
        private boolean isChunked=false;
        
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
        public boolean isChunked() {
            return isChunked;
        }

        public void setChunked(boolean isChunked) {
            this.isChunked = isChunked;
        }

        public String getUrl() {
            return url;
        }
        public void setUrl(String url) {
            this.url = url;
        }
    }
}