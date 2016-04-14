package servelet;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import session.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
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
	private ConcurrentHashMap<Long, InetAddress> serverMap;
	public static final String COOKIE_NAME = "CS5300PROJECT1";
	public static final String LOG_OUT = "/logout.jsp";
	public static final String ERROR_HANDLER = "/errorReprot.jsp";
	public static final String SPLITTER = "/";
	public static final String SESSIONID_SPLITTER = "-";
	public static final String INVALID_INSTRUCTION = "Invalid input!";
	//public static final String LOCAL_INFO_FILE = "D:/Cornell/16spring/CS5300/p1b/CS5300Project1/WebContent/WEB-INF/server_info.txt";
	//public static final String SERVER_MAPPING_FILE = "D:/Cornell/16spring/CS5300/p1b/CS5300Project1/WebContent/WEB-INF/server_mapping.txt";
	public static final String SERVER_MAPPING_FILE = "/server_mapping.txt";
	public static final String LOCAL_INFO_FILE = "/server_info.txt";
	public static final long THREAD_SLEEP_TIME = 1000 * 5 * 60 ;//1000 * 60 * 5;
//	public static final int COOKIE_AGE = 60 * 5;
	public static final int COOKIE_AGE = 20 * 1;
	
	private static long sessNum = 0; // sessin number
	
	private static long servID; // serverID
	private static long rebootNum; // rebootNum
	public static InetAddress[] addrs;
	
	/*
	 * (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init()
	 * Also call initializeRPCServer to start RPC server thread
	 */
	@Override
	public void init() throws ServletException {
		// TODO Auto-generated method stub
		super.init();
		initializeRpcServer();
	}

	/*
	 * Constructor
	 * Create cleanup daemon thread
	 * Read in local files
	 */
	public SessionServelet() {
		sessionTable = new ConcurrentHashMap<>();
		createCleanupThread();
		
		if(restoreServerInfo()) {
			SessionServelet.rebootNum++;
		}
		serverMap = new ConcurrentHashMap();
		saveServerInfo();
		restoreServerMapping();
		System.out.println("reboot number is: " + rebootNum);
	}
	
	/*
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 * Handles GET event from the jsp file, including refresh and first connect to the index.jsp
	 */
	@Override
	public void doGet(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		System.out.println("Refresh goes in here and doGet method is called");
		// initialization
		Session session;
		Cookie currCookie = findCookie(request.getCookies());
		String locationData;
		String sessionID = getSessionIDFromCookie(currCookie);
		Response writeResponse = null;
		String servIDOfLastFoundData = "";
		
		if(sessionID == null) {
			
			// no existing session, simply call write method
			
			session = genSession(true);

			addrs = getNIPAddress(Utils.WQ);
			
			System.out.println("Preparing for PRC write : addrs generated");
			System.out.println(addrs[0].toString());
			writeResponse = write(session.getSessionID(), 0, session.getMessage(), session.getExpireTime(), addrs);
		    
		} else {
			
			// there is an existing session, create a new session with higher versionNumber
            System.out.println("there is existing session according to the cookie");
			locationData = getLocationDataFromCookie(currCookie);
			addrs = getIPAddressByLocationData(locationData);
			
			Response readResponse = read(sessionID, getVersionNumberFromCookie(currCookie), addrs);
			
			session = genSession(false);
			session.setSessionID(sessionID);
			session.setServerID(servID);
			
			if(readResponse != null && readResponse.resStatus.equals(Utils.RESPONSE_FLAGS_READING[0])) {
				
				// reading is successful, call write method
				servIDOfLastFoundData = readResponse.serverID;
				Long updatedVersionNumber = getVersionNumberFromCookie(currCookie)+1;
				
				session.setVersionNumber(updatedVersionNumber);
				session.setMessage(readResponse.resMessage);
				
				addrs = getNIPAddress(Utils.WQ);
				
				writeResponse = write(sessionID, updatedVersionNumber, readResponse.resMessage.trim(), session.getExpireTime(), addrs); 
								
			}
			
			else{
				// reading failed: render the reading fail page for user with specified message
				// Cookies would not be updated, and call return
				
				String errorInfo = "Writing failed for the reason: "+readResponse.resStatus;
				request.setAttribute("error", errorInfo);
				response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
				RequestDispatcher dispacher = request.getRequestDispatcher(ERROR_HANDLER);
				dispacher.forward(request, response);
				return;
			}
		}
		
		
		// check writing response before updating and sending back cookies
		
		if(!writeResponse.resStatus.equals(Utils.RESPONSE_FLAGS_WRITING[0])) {
			
			//failed writing, forward to error page
			String errorInfo = "Error happended when updating information at the server: "+writeResponse.resStatus;
			request.setAttribute("error", errorInfo);
			response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
			RequestDispatcher dispacher = request.getRequestDispatcher(ERROR_HANDLER);
			dispacher.forward(request, response);
		}
		else{
			
			// successful writing: update cookie
			
			currCookie = new Cookie(COOKIE_NAME, genCookieIDFromSession(session, writeResponse.locationData));
			System.out.println("Version Number saved to Cookie is: " + session.getVersionNumber());
			currCookie.setDomain(Utils.DOMAIN_NAME);
			currCookie.setMaxAge(COOKIE_AGE);
			
			// pass session to jsp file
			request.setAttribute("session", session);
			request.setAttribute("currTime", Calendar.getInstance().getTime());
			
			
			request.setAttribute("cookieValue", currCookie.getValue());
			request.setAttribute("cookieDomain", Utils.DOMAIN_NAME);
			request.setAttribute("MetaData",writeResponse.locationData);
			
			request.setAttribute("SvrID", servID);
			request.setAttribute("reboot_num", rebootNum);
			request.setAttribute("srvID_session_data_found", servIDOfLastFoundData);
			response.addCookie(currCookie);
			response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
			RequestDispatcher dispacher = request.getRequestDispatcher("/");
			dispacher.forward(request, response);
		}
		
	}
	
	/*
	 * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 * Handles POST event from jsp page including message replace and logout
	 */
	@SuppressWarnings("null")
	@Override
	public void doPost(HttpServletRequest request, HttpServletResponse response) 
			throws ServletException, IOException {
		System.out.println("doPost method is called");
		
		// initialization
		String param = request.getParameter("req");
		String message = request.getParameter("message");
		Cookie currCookie = findCookie(request.getCookies());
		String sessionID;
		long versionNumber;
		Session session;
		String locationData;
		Response readResponse;
		String servIDOfLastFoundData = "";
		
		// Check if cookie is expired before button is clicked
		if(currCookie != null) {
			sessionID = getSessionIDFromCookie(currCookie);
			locationData = getLocationDataFromCookie(currCookie);
			versionNumber = getVersionNumberFromCookie(currCookie);
			addrs = getIPAddressByLocationData(locationData);
			readResponse = read(sessionID, versionNumber, addrs);
			// render error page if reading fails
			
			if(readResponse == null || !readResponse.resStatus.equals(Utils.RESPONSE_FLAGS_READING[0])){
				
			   String errorInfo = "Retreving message failed for the reason: "+readResponse.resStatus;
			   System.out.println("error info: "+errorInfo);
			   request.setAttribute("error", errorInfo);
			   response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
			   RequestDispatcher dispacher = request.getRequestDispatcher(ERROR_HANDLER);
			   dispacher.forward(request, response);
			   
			   return;
			   
			}
			// reading succeed, generate a new session to be passed into jsp for future communication
			session = new Session(sessionID);
			servIDOfLastFoundData = readResponse.serverID;
			
		} else {
			session = genSession(true);
			sessionID = session.getSessionID();
			versionNumber = -1;
			
		}
		
		
		// check the request type from the button being clicked in jsp file
		if(param.equals("Replace")) {

			// update message if input is valid
			if(message != null || !message.equals("")) {
				session.setMessage(message);
			}
			addrs = getNIPAddress(Utils.WQ);
			Response writeResponse = write(sessionID, versionNumber+1, message, session.getExpireTime(), addrs); 
			
			if(writeResponse.resStatus.equals(Utils.RESPONSE_FLAGS_WRITING[0])) {
				
				//writing succeed
				
				session.setVersionNumber(versionNumber + 1);
				
			}else{
				
				// writing failed, render error display page
				
				String errorInfo = "Updating data failed at the server for the reason: "+writeResponse.resStatus;
				request.setAttribute("error", errorInfo);
				response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
				RequestDispatcher dispacher = request.getRequestDispatcher(ERROR_HANDLER);
				dispacher.forward(request, response);
				return;

			}
			
			currCookie = new Cookie(COOKIE_NAME, genCookieIDFromSession(session, writeResponse.locationData));
			currCookie.setDomain(Utils.DOMAIN_NAME);
			currCookie.setMaxAge(COOKIE_AGE);
			
			// forward response and request to jsp 
			request.setAttribute("session", session);
			request.setAttribute("currTime", Calendar.getInstance().getTime());
			request.setAttribute("cookieValue", currCookie.getValue());
			request.setAttribute("cookieDomain", Utils.DOMAIN_NAME);
			request.setAttribute("MetaData",writeResponse.locationData);
			request.setAttribute("SvrID", servID);
			request.setAttribute("reboot_num", rebootNum);
			request.setAttribute("srvID_session_data_found", servIDOfLastFoundData);
			response.addCookie(currCookie);
			response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
			RequestDispatcher dispatcher = request.getRequestDispatcher("/");
			dispatcher.forward(request, response);
			
		} else if (param.equals("Logout")) {
			// handle logout button
			
			if(currCookie != null) {
				currCookie.setMaxAge(0); 
				response.addCookie(currCookie);
			}
			
			// redirect to logout page
			response.setHeader("Cache-Control", "private, no-store, no-cache, must-revalidate");
			response.sendRedirect(LOG_OUT);
		} else {
			throw new IOException(INVALID_INSTRUCTION);
		}
		
	}
	
	
	
	
	/*
	 * This method is to search for cookie with the correct cookie name and the session id is contained in the table
	 * @param Cookie[] cookies 
	 * @return Cookie
	 */
	private Cookie findCookie(Cookie[] cookies){
		if (cookies == null) return null;
		for(Cookie cookie : cookies) {
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
	public static Session genSession(boolean incr) {
			String newSessionID;
			newSessionID = servID + SESSIONID_SPLITTER + rebootNum + SESSIONID_SPLITTER + sessNum;
			if(incr) sessNum++;
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
	
	/*
	 * Get version number from input cookie
	 * @param: Cookie cookie
	 * @return Long: versionNumber
	 */
	private Long getVersionNumberFromCookie(Cookie cookie) {
		return cookie == null ? null : Long.parseLong(cookie.getValue().split(SPLITTER)[1]);
	}
	
	/*
	 * Get location data from cookie
	 * @param: Cookie cookie
	 * @return: String location data
	 */
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
	 * 
	 */
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

	public static long getServID(){
		return servID;
	}
	
	public static String genTableKey(String sessionID, String versionNum){
		return sessionID + SPLITTER + versionNum;
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
		for(String sessionID : sessionTable.keySet()) {
			Calendar cal = Calendar.getInstance();
			if(sessionTable.get(sessionID).getExpireTime().before(cal.getTime())) {
				sessionTable.remove(sessionID);
			}
		}
	}
	
	/*
	 * Initialize RPC server thread, must be called in constructor
	 */
	private void initializeRpcServer() {
		Thread t = new Thread(new Runnable(){

			@Override
			public void run() {
				RpcServer server = new RpcServer();
				try {
					server.rpcCallRequestProcessor();
				} catch (NumberFormatException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (ParseException e) {
					e.printStackTrace();
				}
				
			}
			
		});
		t.start();
	}
	
	/*
	 * Restore server information from local file
	 */
	private boolean restoreServerInfo() {
		try (BufferedReader br = new BufferedReader(new FileReader(LOCAL_INFO_FILE)))
		{
			String serverID = br.readLine();
			String rebootNum = br.readLine();
			SessionServelet.servID = Long.parseLong(serverID);
			SessionServelet.rebootNum = Long.parseLong(rebootNum);
			System.out.println("server Info already be read"+SessionServelet.servID);
			return true;

		} catch (IOException e) {
			e.printStackTrace();
		}
		return false;
		
	}
	
	/*
	 * Save server information into local file
	 */
	private void saveServerInfo() {
		List<String> lines = Arrays.asList(String.valueOf(servID), String.valueOf(rebootNum));
		Path file = Paths.get(LOCAL_INFO_FILE);
		try {
			Path path = Files.write(file, lines, Charset.forName("UTF-8"));
			System.out.println(path.toString());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * Restore server mapping from local file
	 */
	private boolean restoreServerMapping(){
		
		try (BufferedReader br = new BufferedReader(new FileReader(SERVER_MAPPING_FILE))){
			String currLine;
			while((currLine = br.readLine()) != null) {
				String serverIDStr = currLine.split("\\s+")[1];
				//String serverIDStr = currLine.split(" +")[1];
				String serverID = serverIDStr.split("-")[1];

				currLine = br.readLine();
				String ip = currLine.split("\\s+")[2];
				//String ip = currLine.split(" +")[2];
				serverMap.put(Long.parseLong(serverID), InetAddress.getByName(ip));
			}
			
		}catch (IOException e) {
			e.printStackTrace();
		}
		
		return false;
	}

	/*
	 * Get n ip addresses randomly
	 * @param: int N 
	 * @return InetAddress[]
	 */
	public InetAddress[] getNIPAddress(int N) {
		InetAddress[] rst = new InetAddress[N];
		Random r = new Random();
		Set<Long> set = new HashSet<Long>();
		for(int i = 0; i < N; i++) {
			long rand;
			do {
				rand = r.nextInt(N);
			} while (set.contains(rand));
			System.out.println("random is "+rand);
			set.add(rand);
			rst[i] = serverMap.get(rand);
		}
		return rst;
	}
	
	/*
	 * Get IP address from input server ID
	 * @param: long: server ID
	 * @return: InetAddress
	 */
	public InetAddress getIPAddressByServID(long serverID) {
		return serverMap.get(serverID);
	}
	
	/*
	 * Get IP address by input location data
	 * @param: String: location Data
	 * @return: InetAddress[]
	 */
	public InetAddress[] getIPAddressByLocationData(String locationData) {
		InetAddress[] result;
		if(locationData.length() == 1){
			Long servID = Long.parseLong(locationData);
			result = new InetAddress[1];
			result[0] = serverMap.get(servID);
			return result;
		}
		else{
			String[] servIDArray = locationData.split(Utils.SPLITTER);
			int n = servIDArray.length;
			result = new InetAddress[n];
			for(int i=0; i<n; i++){
				Long servID = Long.parseLong(servIDArray[i]);
				result[i] = serverMap.get(servID);
			}
			return result;
		}
	}
}
