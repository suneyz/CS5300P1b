package servelet;

import java.io.IOException;
import java.util.Calendar;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import session.Session;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
	private ConcurrentHashMap<String, Session> sessionTable;
	public static final String COOKIE_NAME = "cs5300project1";
	public static final String LOG_OUT = "/CS5300Project1/logout.jsp";
	public static final String SPLITTER = "/";
	public static final String INVALID_INSTRUCTION = "Invalid input!";
	public static final long THREAD_SLEEP_TIME = 1000 * 60 * 5;
	
	/*
	 * Constructor
	 * Create cleanup daemon thread
	 */
	public SessionServelet() {
		sessionTable = new ConcurrentHashMap<>();
		createCleanupThread();
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
		String sessionID = getSessionIDFromCookie(currCookie);
		
		// check if there is an existing session
		if(sessionID == null) {
			// no existing session, render a new session
			session = genSession();
			sessionTable.put(session.getSessionID(), session);
		} else {
			// there is an existing session, refresh current session
			session = sessionTable.get(sessionID);
			session.refresh();
		}
		
		// update coockie
		currCookie = new Cookie(COOKIE_NAME, genCookieIDFromSession(session));
		
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
		Session session;
		boolean isNewSession = false;
		
		// render a new session if the old session is already expired
		if(sessionID == null || !sessionTable.containsKey(sessionID)) {
			session = genSession();
			sessionTable.put(session.getSessionID(), session);
			isNewSession = true;
			currCookie = new Cookie(COOKIE_NAME, genCookieIDFromSession(session));
		} else {
			session = sessionTable.get(sessionID);
		}
		
		// check the parameter from jsp button
		if(param.equals("Replace")) {
			
			if(!isNewSession) {
				// handle message replace button
				session.refresh();
				// generate cookie
				currCookie.setValue(genCookieIDFromSession(session));
			}
			
			// update message if input is valid
			if(message != null || !message.equals("")) {
				session.setMessage(message);
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
				// remove session from the session table
				sessionTable.remove(session.getSessionID());
			}
			currCookie.setMaxAge(0); 
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
		for(Cookie cookie : cookies) {
			String sessionID = getSessionIDFromCookie(cookie);
			if(cookie.getName().equals(COOKIE_NAME) && sessionTable.containsKey(sessionID)) {
				return cookie;
			}
		}
		return null;
	}
	
	/*
	 * This method is used to render a new session
	 * @return new session
	 */
	private Session genSession() {
			String newSessionID = null;
			do {
				newSessionID = UUID.randomUUID().toString();
				newSessionID.replace(SPLITTER, "-");
			} while (sessionTable.containsKey(newSessionID));
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
	 * Get cookie ID information from input session
	 * @param Session session
	 * @return String output cookie ID
	 */
	private String genCookieIDFromSession(Session session) {
		return session.getSessionID() + SPLITTER + session.getVersionNumber();
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
}
