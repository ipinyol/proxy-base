package org.mars.proxybase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.logging.Logger;

public class ProxyWebSocket {
    DataInputStream in = null;
    DataOutputStream out = null;
    DataInputStream inMid = null;
    DataOutputStream outMid = null;
    
    public List<Object> lock = new ArrayList<Object>();
    
    public static byte OP_CLOSE = 0x8;
    public static byte OP_PING = 0x9;
    public static byte OP_PONG = 0xA;
    /**
     * Class that implements a WebSocket proxy traffic of Frames
     * @param in
     * @param out
     * @param inMid
     * @param outMid
     */
    public ProxyWebSocket(DataInputStream in, DataOutputStream out, DataInputStream inMid, DataOutputStream outMid) {
        this.in = in;
        this.out = out;
        this.inMid = inMid;
        this.outMid = outMid;
        
        
    }
    
    /**
     * It follows RFC 6455: https://tools.ietf.org/html/rfc6455#section-5.5.1
     */
    public void startProtocol() {
        System.out.println("Starting websocket protocol RFC 6455");
        WebSocketThread webSocketExternal = new WebSocketThread(in,outMid, lock, "S1");
        WebSocketThread webSocketInternal = new WebSocketThread(inMid,out, lock, "S2");
        webSocketInternal.start();
        webSocketExternal.start();

        while(this.lock.size() ==0 ) {
            try {
                Thread.sleep(10000);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        webSocketExternal.terminate();
        webSocketInternal.terminate();
    }
    
    private static class WebSocketThread extends Thread {
        private DataInputStream in = null;
        private DataOutputStream out = null;
        private List<Object> lock;
        private boolean moving=true;
        private String name = null;
        Logger log = Logger.getLogger("org.mars.proxybase");
        
        public WebSocketThread(DataInputStream in, DataOutputStream out, List<Object> lock, String name) {
            this.in = in;
            this.out = out;
            this.lock = lock;
            this.name = name;
        }
        public void terminate() {
            System.out.println(this.name + ": Terminate!");
            Thread.interrupted();
        }
        
        public void run() {
            int i=0;
            while (moving) {
                try {
                    Frame inFrame = new Frame();
                    inFrame.readFrame(this.in, this.name, log);
                    inFrame.printInfo();
                    inFrame.writeFrame(this.out, this.name, log);
                    i++;
                    moving=(inFrame.getOpcode()!=ProxyWebSocket.OP_CLOSE);
                } catch (Exception e) {
                    //log.info(this.name + ": Exception!");
                    e.printStackTrace();
                    moving = false;
                }
            }
            lock.add(new Object());
        }
        
    }
    
    private static class Frame {
        byte opcode;
        boolean hasMask;
        long payloadLength;
        ProxyThread.Content content;
        
        byte [] headBytes = new byte[2];
        byte [] payloadLenBytes=null;
        byte [] maskBytes=null;
        
        public void printInfo() {
            /*
            System.out.printf("B0:  0x%02X\n", this.headBytes[0]);
            System.out.printf("B1:  0x%02X\n", this.headBytes[1]);
            System.out.println("HasMask:" + this.hasMask);
            System.out.println("Opcode: " + this.opcode);
            System.out.println("len: " + payloadLength);
            */
        }
        
        public void readFrame(DataInputStream in, String name, Logger log) throws Exception {
            //System.out.println("In readFrame");
            this.headBytes[0] =ProxyThread.readBytes(in,1)[0];
            this.headBytes[1] = ProxyThread.readBytes(in,1)[0];
            this.opcode = getOpCode(this.headBytes[0]);
            //System.out.println("Opcode:" + opcode);
            
            this.hasMask = getHasMask(this.headBytes[1]);
            long payload = getPayLoadSmall(this.headBytes[1]);
            //System.out.println("length payload in: " + payload);
            if (payload==126) {
                payloadLenBytes = ProxyThread.readBytes(in, 2);
                payloadLength = toLong(payloadLenBytes);
            } else if(payload==127) {
                payloadLenBytes = ProxyThread.readBytes(in, 8);
                payloadLength = toLong(payloadLenBytes);
            } else {
                payloadLength = payload;
            }
            
            if (this.hasMask) {
                maskBytes = ProxyThread.readBytes(in, 4);
            }
            content = new ProxyThread.Content();
            ProxyThread.readBufferedContent(in,payloadLength , content);
            
        }
    
        public void writeFrame(DataOutputStream out, String name, Logger log) throws Exception {
            //log.info(name + ": " + this.headBytes);
            out.write(this.headBytes);
            if (payloadLenBytes!=null) {
                out.write(payloadLenBytes);
            }
            if (maskBytes!=null) {
                out.write(maskBytes);
            }
            for(byte[] arr:content.getRawContentList()) {
                //String aa = new String(arr,"UTF-8");
                //System.out.println(aa);
            	//System.out.println("length payload out: " + arr.length);
                out.write(arr);
            }
            out.flush();
            
        }
        
        private static long toLong(byte[] b) {
        	
        	int longSize = Long.SIZE / Byte.SIZE;
        	int missing = longSize-b.length;
        	byte[] num = null;
        	if (missing<=0) {
        		num = b;
        	} else {
        		num = new byte[longSize];
        		for(int i = 0; i<missing; i++) {
        			num[i]=0x00;
        		}
        		System.arraycopy(b, 0, num, missing, b.length);
        	}
        	ByteBuffer bb = ByteBuffer.allocate(num.length);
            bb.put(num);
            bb.position(0);
            return bb.getLong();
        }
        
        private int getPayLoadSmall(byte b) {
            byte mask = (byte)0x7F; //01111111b
            //x%02X\n", mask);
            //System.out.printf("byte:  0x%02X\n", b);
            
            return mask & b;
        }
        
        private boolean getHasMask(byte b) {
            byte mask = (byte)0x80; //10000000b
            //System.out.printf("Mask:  0x%02X\n", mask);
            //System.out.printf("Byte:  0x%02X\n", b);
            byte ret = (byte) (mask & b);
            //System.out.printf("Ret:  0x%02X\n", ret);
            return (ret!=0);
        }
        
        private byte getOpCode(byte b) {
            byte mask = (byte)0x0F; //00001111b
            return (byte) (mask & b);
        }
        
        
        
        public byte getOpcode() {
            return this.opcode;
        }
        
        public ProxyThread.Content getContent() {
            return this.content;
        }
        
        public boolean hasMask() {
            return this.hasMask;
        }
        
        public long getPayLoadLength() {
            return this.payloadLength;
        }
        
        
    }
}
