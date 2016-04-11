package servelet;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import session.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import rpc.Response;
import rpc.RpcClient;
import rpc.RpcServer;
import rpc.Utils;

public class SessionServelet extends HttpServlet{

	/**
	 * @author Yuzhuo Sun(ys684)
	 * 
	 * This is session management servlet for CS5300 project1a
	 * This is class implements the POST and GET request from jsp file
	 * doGet() method handles session initialization and session refresh
	 * doPost() method handles logout and replace message
	 */
	private static final long serialVersionUID = 1L;
	private static ConcurrentHashMap<String, Session> sessionTable;
	public static final String COOKIE_NAME = "CS5300PROJECT1";
	public static final String LOG_OUT = "/CS5300Project1/logout.jsp";
	public static final String SPLITTER = "/";
	public static final String SESSIONID_SPLITTER = "-";
	public static final String INVALID_INSTRUCTION = "Invalid input!";
	public static final long THREAD_SLEEP_TIME = 1000 * 10;//1000 * 60 * 5;
	public static final int COOKIE_AGE = 10;
	
	private static long sessNum = 0; // sessin number
	
	private static long servID = 0; // TODO: change it to read from local file
	private static long rebootNum = 0; // TODO: change it to read from local file
	
	//-------------
	
	public static final boolean TEST = true;
	
	public static final String SERVER_0 = "10.148.3.224";
	public static final String SERVER_1 = "10.132.3.234";
	
	public static InetAddress addr0;
	public static InetAddress addr1;
	
	public static InetAddress[] addrs = new InetAddress[1];
	
	
	
	@Override
	public void init() throws ServletException {
		// TODO Auto-generated method stub
		super.init();
		initializeRpcServer();
	}

	/*
	 * Constructor
	 * Create cleanup daemon thread
	 */
	public SessionServelet() {
		sessionTable = new ConcurrentHashMap<>();
		createCleanupThread();
		
		// ========
		
		try {
			addr0 = InetAddress.getByName(SERVER_0);
//			addr1 = InetAddress.getByName(SERVER_1);
			addrs[0] = addr0;
//			addrs[1] = addr1;
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// ==========
	}
	
	/*
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 * Handles GET event from the jsp file, including refresh and first connect to the index.jsp
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		
		// initialization
		Session session;
		Cookie currCookie = findCookie(request.getCookies());
		if(TEST) {
			if(currCookie == null) {
				System.out.println("No cookie");
			} else {
				System.out.println("Have cookie, cookie data: " + currCookie.getValue());
			}
		}
		String sessionID = getSessionIDFromCookie(currCookie);
		Response writeResponse = null;
		
		// check if there is an existing session
		if(sessionID == null) {
			// no existing session, render a new session
			
			session = genSession();
//			addSessionToTable(session);
			
			if(TEST) {
				System.out.println("First Time ping the web, rendering a new session......");
			}
			writeResponse = write(session.getSessionID(), 0, session.getMessage(), session.getExpireTime(), addrs);
			
			if(TEST) {
				System.out.println("Finish render new session.");
			}
		} else {
			
			// there is an existing session, create a new session with higher versionNumber
			
//			session = sessionTable.get(sessionID);
//			session.refresh();
			
			//==========================
//			currCookie.getValue();
			
			if(TEST) {
				System.out.println("Have cookie, try to read old session......");
			}
			
			Response readResponse = read(sessionID, getVersionNumberFromCookie(currCookie), addrs); // TODO: replace INETADDRESS
			
			session = genSession();
			session.setSessionID(sessionID);
			session.setServerID(servID);
			System.out.println("Read response status is: " + readResponse.resStatus);
			if(readResponse != null && readResponse.resStatus.equals(Utils.RESPONSE_FLAGS_READING[0])) {
				System.out.println("====Servelet initializing write call......");
				Long updatedVersionNumber = getVersionNumberFromCookie(currCookie)+1;
				writeResponse = write(sessionID, updatedVersionNumber, readResponse.resMessage.trim(), session.getExpireTime(), addrs); // TODO: replace INETADDRESS
				System.out.println("Hash map size is: " + sessionTable.size());
				System.out.println("Expire time is: " + session.getExpireTime());
				session.setVersionNumber(updatedVersionNumber);
				System.out.println("Version Number updated is: " + updatedVersionNumber);
				if(!writeResponse.resStatus.equals(Utils.RESPONSE_FLAGS_WRITING[0])) {
					//TODO: handle the case when write operation fails
				}
			}
		}
		
		// update coockie
		currCookie = new Cookie(COOKIE_NAME, genCookieIDFromSession(session, writeResponse.locationData));
		System.out.println("Version Number saved to Cookie is: " + session.getVersionNumber());
		currCookie.setMaxAge(COOKIE_AGE);
		
		
		
		// pass session to jsp file
		request.setAttribute("session", session);
		request.setAttribute("currTime", Calendar.getInstance().getTime());
		request.setAttribute("cookieID", currCookie.getValue());
		response.addCookie(currCookie);
		response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
		RequestDispatcher dispacher = request.getRequestDispatcher("/");
		dispacher.forward(request, response);
	}
	
	/*
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 * Handles POST event from jsp page including message replace and logout
	 */
	@SuppressWarnings("null")
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		// initialization
		String param = request.getParameter("req");
		String message = request.getParameter("message");
		Cookie currCookie = findCookie(request.getCookies());
		String sessionID = getSessionIDFromCookie(currCookie);
		long versionNumber = getVersionNumberFromCookie(currCookie);
		String locationData = getLocationDataFromCookie(currCookie);
		Session session;
//		boolean isNewSession = false;
		
		// render a new session if the old session is already expired
//		if(sessionID == null || !sessionTable.containsKey(sessionID)) {
//			session = genSession();
//			sessionTable.put(session.getSessionID(), session);
//			isNewSession = true;
//			currCookie = new Cookie(COOKIE_NAME, genCookieIDFromSession(session));
//		} else {
//			session = sessionTable.get(sessionID);
//		}
		
		Response readResponse = read(sessionID, versionNumber, addrs); //TODO: replace address
		session = new Session(sessionID);
		
		//TODO: need to check the result of read!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		
		// check the parameter from jsp button
		if(param.equals("Replace")) {
			
//			if(!isNewSession) {
//				// handle message replace button
//				session.refresh();
//				// generate cookie
//				currCookie.setValue(genCookieIDFromSession(session));
//			}
			
			
			// update message if input is valid
			if(message != null || !message.equals("")) {
				session.setMessage(message);
			}
			
			Response writeResponse = write(sessionID, versionNumber+1, message, session.getExpireTime(), addrs); //TODO: replace address
			
			if(writeResponse.resStatus.equals(Utils.RESPONSE_FLAGS_WRITING[0])) {
				session.setVersionNumber(versionNumber + 1);
			}
			
			// forward response and request to jsp 
			request.setAttribute("session", session);
			request.setAttribute("currTime", Calendar.getInstance().getTime());
			request.setAttribute("cookieID", currCookie.getValue());	
			response.addCookie(currCookie);
			response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
			RequestDispatcher dispatcher = request.getRequestDispatcher("/");
			dispatcher.forward(request, response);
		} else if (param.equals("Logout")) {
			// handle logout button
			synchronized (this) {
				
				//TODO: check this
				
				// remove session from the session table
				//sessionTable.remove(session.getSessionID());
			}
			
			//TODO: check this, commented because it not necessary, and currently not supported by RPC server write
			//Response writeResponse = write(sessionID, versionNumber, message, Calendar.getInstance().getTime(), new InetAddress[3]); //TODO: replace address
			
			//if(writeResponse.resStatus.equals(Utils.RESPONSE_FLAGS_WRITING[0])) 
			currCookie.setMaxAge(0); 
			
			// redirect to logout page
			response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
			response.sendRedirect(LOG_OUT);
		} else {
			throw new IOException(INVALID_INSTRUCTION);
		}
		
	}
	
	public static String genTableKey(String sessionID, String versionNum){
		return sessionID + SPLITTER + versionNum;
	}
	
	/*
	 * This method is to search for cookie with the correct cookie name and the session id is contained in the table
	 * @param Cookie[] cookies 
	 * @return Cookie
	 */
	private Cookie findCookie(Cookie[] cookies){
		if (cookies == null) return null;
		for(Cookie cookie : cookies) {
//			String sessionID = getSessionIDFromCookie(cookie);
			if(cookie.getName().equals(COOKIE_NAME)) {
				return cookie;
			}
		}
		return null;
	}
	
	/*
	 * This method is used to render a new session
	 * @return new session
	 */
	public static Session genSession() {
			String newSessionID = null;
//			do {
//				newSessionID = UUID.randomUUID().toString();
//				newSessionID.replace(SPLITTER, "-");
//			} while (sessionTable.containsKey(newSessionID));
			newSessionID = servID + SESSIONID_SPLITTER + rebootNum + SESSIONID_SPLITTER + sessNum;
			sessNum++;
			return new Session(newSessionID);
	}
	
	/*
	 * Get the session ID information from cookie
	 * @param Cookie cookie
	 * @return String session ID
	 */
	private String getSessionIDFromCookie(Cookie cookie) {
		return cookie == null ? null : cookie.getValue().split(SPLITTER)[0];
	}
	
	private Long getVersionNumberFromCookie(Cookie cookie) {
		return cookie == null ? null : Long.parseLong(cookie.getValue().split(SPLITTER)[1]);
	}
	
	private String getLocationDataFromCookie(Cookie cookie) {
		return cookie == null ? null : cookie.getValue().split(SPLITTER)[2];
	}
	
	/*
	 * Get cookie ID information from input session
	 * @param Session session
	 * @return String output cookie ID
	 */
	private String genCookieIDFromSession(Session session, String locationData) {
		return session.getSessionID() + SPLITTER + session.getVersionNumber() + SPLITTER + locationData;
	}

	/*
	 * Create a daemon clean up thread that clean up the session table every 5 minutes
	 * 
	 */
	private void createCleanupThread() {
		Thread cleanupThread = new Thread(new Runnable() {
			
			@Override
			public void run() {
				while(true) {
					cleanup();
					try {
						Thread.sleep(THREAD_SLEEP_TIME);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
			}
		});
		cleanupThread.setDaemon(true);
		cleanupThread.start();
	}
	
	/*
	 * Perform session table clean up operation 
	 */
	private synchronized void cleanup() {
		System.out.println("Cleanning expired session......");
		for(String sessionID : sessionTable.keySet()) {
			Calendar cal = Calendar.getInstance();
			if(sessionTable.get(sessionID).getExpireTime().before(cal.getTime())) {
				System.out.println("Cleaning session with SessionID: " + sessionID);
				System.out.println("Its expire time is: " + sessionTable.get(sessionID).getExpireTime());
				sessionTable.remove(sessionID);
			}
		}
	}
	
	private void initializeRpcServer() {
		Thread t = new Thread(new Runnable(){

			@Override
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
			
		});
		t.start();
	}
	
	public static Session getSessionByIDVersion(String sessionID, String versionNumber){
		return sessionTable.get(genTableKey(sessionID, versionNumber));
	}
	
	public static void addSessionToTable(Session session) {
		sessionTable.put(genTableKey(session.getSessionID(), String.valueOf(session.getVersionNumber())), session);
	}
	
	private Response read(String sessionID, long versionNumber, InetAddress[] destAdds) throws IOException {
		return RpcClient.sessionReadClient(sessionID, versionNumber, destAdds);
	}
	
	private Response write(String sessionID, long versionNumber, String message, Date date, InetAddress[] destAddrs) throws IOException {
		return RpcClient.sessionWriteClient(sessionID, versionNumber, message, date, destAddrs);
	}
	
	
	
	// =======
	
	public static long getServID(){
		return servID;
	}
	
	
	
	
}
