package servelet;

import java.io.IOException;
import java.text.ParseException;

import javax.servlet.ServletContextEvent;

import rpc.RpcServer;

public class BackgroundThread {
    private Thread t = null;
    public void contextInitialized(ServletContextEvent sce) {
        if ((t == null) || (!t.isAlive())) {
            t = new Thread() {
                public void run() {
                	RpcServer server = new RpcServer();
    				try {
    					server.rpcCallRequestProcessor();
    				} catch (NumberFormatException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				} catch (IOException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				} catch (ParseException e) {
    					// TODO Auto-generated catch block
    					e.printStackTrace();
    				}
                }
            };
            t.start();
        }
    }
    public void contextDestroyed(ServletContextEvent sce) {
        try {
            t.interrupt();
        } catch (Exception ex) {
        }
    }
}
