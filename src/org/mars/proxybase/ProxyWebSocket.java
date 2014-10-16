package org.mars.proxybase;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;

public class ProxyWebSocket {
    DataInputStream in = null;
    DataOutputStream out = null;
    DataInputStream inMid = null;
    DataOutputStream outMid = null;
    
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
        boolean moving=true;
        try {
            while (moving) {
                // We read the first two bytes that for sure will be there
                try {
                    Frame inFrame = new Frame();
                    inFrame.readFrame(this.in);
                    inFrame.printInfo();
                    
                    inFrame.writeFrame(outMid);
                    Frame outFrame = new Frame();
                    outFrame.readFrame(inMid);
                    outFrame.printInfo();
                    outFrame.writeFrame(out);
                    moving=(inFrame.getOpcode()==ProxyWebSocket.OP_CLOSE);
                } catch (Exception e) {
                    e.printStackTrace();
                    moving = false;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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
            System.out.println("Byte0: " + this.headBytes[0]);
            System.out.println("Byte1: " + this.headBytes[1]);
            System.out.println("Opcode: " + this.opcode);
            System.out.println("len: " + payloadLength);
        }
        public void readFrame(DataInputStream in) throws Exception {
            System.out.println("In readFrame");
            this.headBytes[0] =in.readByte();
            this.headBytes[1] = in.readByte();
            System.out.println("read " + this.headBytes[0] + "  " + this.headBytes[1]);
            this.opcode = getOpCode(this.headBytes[0]);
            System.out.println("Opcode:" + opcode);
            
            this.hasMask = getHasMask(this.headBytes[1]);
            long payload = getPayLoadSmall(this.headBytes[1]);
            System.out.println("payLoad:" + payload);
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
    
        public void writeFrame(DataOutputStream out) throws Exception {
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
                out.write(arr);
            }
        }
        
        private static long toLong(byte[] b) {
            ByteBuffer bb = ByteBuffer.allocate(b.length);
            bb.put(b);
            return bb.getLong();
        }
        private int getPayLoadSmall(byte b) {
            byte mask = (byte)0x3F; //01111111b
            return mask & b;
        }
        
        private boolean getHasMask(byte b) {
            byte mask = (byte)0x40; //10000000b
            int val = mask & b;
            return (val!=0);
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
