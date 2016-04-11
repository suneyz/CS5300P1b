package rpc;

import java.net.DatagramPacket;

import session.Session;

public class Response {
    public String resStatus;
    //public DatagramPacket resPkt;
    public String resMessage;
    public String locationData;// join by "_"
    
    public Session session;
}
