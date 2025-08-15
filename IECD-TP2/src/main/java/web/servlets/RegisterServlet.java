package web.servlets;

import java.io.IOException;

import core.client.ClientUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import web.utils.WebClientConnection;
import web.utils.WebConnectionManager;
import web.utils.WebLogger;

/**
 * Handles user registration for the GoBang web application.
 * 
 * This servlet manages both the display of the registration form and the processing
 * of new user registration requests. It communicates with the game server to create
 * new user accounts and maintains proper connection state for subsequent login attempts.
 * 
 * GET requests:
 * - Displays the registration form (register.jsp)
 * 
 * POST requests:
 * - Processes registration data against the game server
 * - Establishes and maintains WebClientConnection for proper server communication
 * - Starts background message reader to maintain connection state
 * - Redirects to login on success or back to registration form with errors
 * 
 * The servlet follows a secure registration pattern:
 * 1. Validate input parameters
 * 2. Establish server connection
 * 3. Send registration request to game server
 * 4. Process server response
 * 5. Start background communication to maintain connection health
 * 6. Direct user to login page on success
 * 7. Handle errors gracefully with user-friendly messages
 * 
 * Important: After successful registration, the background reader thread is started
 * to maintain proper connection state, even though the user isn't logged in yet.
 * This prevents connection issues during the subsequent login attempt.
 */
@WebServlet("/register")
public class RegisterServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private static final String REGISTER_VIEW = "register.jsp";
    private static final String LOGIN_VIEW = "login.jsp";
    
    private static final String PARAM_USERNAME = "username";
    private static final String PARAM_PASSWORD = "password";
    private static final String PARAM_NATIONALITY = "nationality";
    private static final String PARAM_DATE_OF_BIRTH = "dateOfBirth";
    
    private static final String ATTR_ERROR = "error";
    private static final String ATTR_SUCCESS = "success";
    private static final String ATTR_LAST_REGISTER_STATUS = "lastRegisterStatus";
    
    private static final String STATUS_ACCEPTED = "Accepted";
    private static final String SUCCESS_MESSAGE = "Registration successful! Please log in.";
    private static final String DEFAULT_ERROR_MESSAGE = "Connection error. Please try again.";

    @Override
    protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        String sessionIdentifier = getSessionIdentifier(httpRequest);
        WebLogger.info("RegisterServlet", sessionIdentifier, "Registration form requested");
        
        forwardToRegistrationView(httpRequest, httpResponse);
    }

    @Override
    protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        String username = httpRequest.getParameter(PARAM_USERNAME);
        String password = httpRequest.getParameter(PARAM_PASSWORD);
        String nationality = httpRequest.getParameter(PARAM_NATIONALITY);
        String dateOfBirth = httpRequest.getParameter(PARAM_DATE_OF_BIRTH);
        String sessionIdentifier = getSessionIdentifier(httpRequest);
        
        WebLogger.info("RegisterServlet", sessionIdentifier, "Registration attempt for username: " + username);

        if (!areRegistrationDataValid(username, password, nationality, dateOfBirth)) {
            handleInvalidRegistrationData(httpRequest, httpResponse, sessionIdentifier);
            return;
        }

        try {
            processRegistrationAttempt(username, password, nationality, dateOfBirth, 
                                     httpRequest, httpResponse, sessionIdentifier);
        } catch (Exception exception) {
            handleRegistrationException(username, sessionIdentifier, exception, httpRequest, httpResponse);
        }
    }

    /**
     * Processes a complete registration attempt including server communication and connection management.
     * 
     * @param username the username to register
     * @param password the password for the new account
     * @param nationality the user's nationality
     * @param dateOfBirth the user's date of birth
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if any step of the registration process fails
     */
    private void processRegistrationAttempt(String username, String password, String nationality, String dateOfBirth,
                                          HttpServletRequest httpRequest, HttpServletResponse httpResponse, 
                                          String sessionIdentifier) throws Exception {
        
        WebClientConnection serverConnection = establishServerConnection(httpRequest, sessionIdentifier);
        clearPreviousRegistrationData(httpRequest.getSession());
        sendRegistrationRequestToServer(serverConnection, username, password, nationality, dateOfBirth, sessionIdentifier);
        receiveServerResponse(serverConnection, username, sessionIdentifier);
        
        String registrationStatus = (String) httpRequest.getSession().getAttribute(ATTR_LAST_REGISTER_STATUS);
        
        if (isRegistrationSuccessful(registrationStatus)) {
            handleSuccessfulRegistration(username, httpRequest, httpResponse, sessionIdentifier);
        } else {
            handleFailedRegistration(username, registrationStatus, httpRequest, httpResponse, sessionIdentifier);
        }
    }

    /**
     * Establishes a connection to the game server for registration.
     * 
     * @param httpRequest the HTTP request containing session information
     * @param sessionIdentifier the session ID for logging
     * @return the established WebClientConnection
     * @throws Exception if connection cannot be established
     */
    private WebClientConnection establishServerConnection(HttpServletRequest httpRequest, String sessionIdentifier) 
            throws Exception {
        
        WebLogger.info("RegisterServlet", sessionIdentifier, "Establishing server connection");
        WebClientConnection connection = WebConnectionManager.getOrCreateConnection(httpRequest.getSession());
        
        if (!connection.isConnectionActive()) {
            throw new Exception("No connection to server");
        }
        
        WebLogger.info("RegisterServlet", sessionIdentifier, "Server connection established");
        return connection;
    }

    /**
     * Clears any previous registration data from the session.
     * 
     * @param userSession the user's HTTP session
     */
    private void clearPreviousRegistrationData(HttpSession userSession) {
        userSession.removeAttribute(ATTR_LAST_REGISTER_STATUS);
    }

    /**
     * Sends the registration request to the game server.
     * 
     * @param connection the server connection
     * @param username the username to register
     * @param password the password for the new account
     * @param nationality the user's nationality
     * @param dateOfBirth the user's date of birth
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if the request cannot be sent
     */
    private void sendRegistrationRequestToServer(WebClientConnection connection, String username, String password,
                                               String nationality, String dateOfBirth, String sessionIdentifier) 
                                               throws Exception {
        
        WebLogger.info("RegisterServlet", sessionIdentifier, "Sending registration request for: " + username);
        connection.getCommunicationProtocol().sendRegisterRequest(username, password, nationality, dateOfBirth);
        WebLogger.info("RegisterServlet", sessionIdentifier, "Registration request sent for: " + username);
    }

    /**
     * Receives and processes the server response to the registration request.
     * 
     * @param connection the server connection
     * @param username the username being registered
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if the response cannot be received
     */
    private void receiveServerResponse(WebClientConnection connection, String username, String sessionIdentifier) 
            throws Exception {
        
        WebLogger.info("RegisterServlet", sessionIdentifier, "Waiting for server response for: " + username);
        connection.getCommunicationProtocol().receiveReplies();
        WebLogger.info("RegisterServlet", sessionIdentifier, "Server response received for: " + username);
    }

    /**
     * Checks if the registration attempt was successful based on server response.
     * 
     * @param registrationStatus the status returned by the server
     * @return true if registration was successful, false otherwise
     */
    private boolean isRegistrationSuccessful(String registrationStatus) {
        return STATUS_ACCEPTED.equals(registrationStatus);
    }

    /**
     * Handles successful registration by starting background communication and directing to login.
     * 
     * IMPORTANT: Starts the background reader thread to maintain connection health
     * for the subsequent login attempt. This prevents connection state issues.
     * 
     * @param username the registered username
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void handleSuccessfulRegistration(String username, HttpServletRequest httpRequest, 
                                            HttpServletResponse httpResponse, String sessionIdentifier) 
                                            throws ServletException, IOException {
        
        WebLogger.info("RegisterServlet", sessionIdentifier, "Registration successful for: " + username);
        
        // CRITICAL: Start background reader to maintain connection health
        // This prevents issues when the user attempts to login next
        startBackgroundCommunication(httpRequest.getSession(), sessionIdentifier);
        
        redirectToLoginWithSuccess(httpRequest, httpResponse, sessionIdentifier);
    }

    /**
     * Starts background communication threads to maintain connection health.
     * This is essential even though the user isn't logged in yet, as it keeps
     * the connection in the proper state for the subsequent login attempt.
     * 
     * @param userSession the user's HTTP session
     * @param sessionIdentifier the session ID for logging
     */
    private void startBackgroundCommunication(HttpSession userSession, String sessionIdentifier) {
        WebLogger.info("RegisterServlet", sessionIdentifier, 
                      "Starting background message reader to maintain connection health");
        WebConnectionManager.startBackgroundMessageReader(userSession);
    }

    /**
     * Redirects to login page with success message after successful registration.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void redirectToLoginWithSuccess(HttpServletRequest httpRequest, HttpServletResponse httpResponse, 
                                          String sessionIdentifier) throws ServletException, IOException {
        
        httpRequest.setAttribute(ATTR_SUCCESS, SUCCESS_MESSAGE);
        WebLogger.info("RegisterServlet", sessionIdentifier, "Redirecting to login with success message");
        forwardToLoginView(httpRequest, httpResponse);
    }

    /**
     * Handles failed registration attempts by displaying appropriate error messages.
     * 
     * @param username the username that failed registration
     * @param registrationStatus the failure status from the server
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void handleFailedRegistration(String username, String registrationStatus, HttpServletRequest httpRequest, 
                                        HttpServletResponse httpResponse, String sessionIdentifier) 
                                        throws ServletException, IOException {
        
        WebLogger.warning("RegisterServlet", sessionIdentifier, 
                         "Registration failed for: " + username + " - " + registrationStatus);
        
        String userFriendlyErrorMessage = createUserFriendlyErrorMessage(registrationStatus);
        httpRequest.setAttribute(ATTR_ERROR, userFriendlyErrorMessage);
        
        forwardToRegistrationView(httpRequest, httpResponse);
    }

    /**
     * Creates a user-friendly error message from the server status.
     * 
     * @param registrationStatus the status returned by the server
     * @return user-friendly error message
     */
    private String createUserFriendlyErrorMessage(String registrationStatus) {
        if (registrationStatus != null) {
            return ClientUtils.getFriendlyStatusMessage(registrationStatus);
        }
        return "Registration error";
    }

    /**
     * Handles exceptions that occur during the registration process.
     * 
     * @param username the username being registered
     * @param sessionIdentifier the session ID for logging
     * @param exception the exception that occurred
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void handleRegistrationException(String username, String sessionIdentifier, Exception exception, 
                                           HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
                                           throws ServletException, IOException {
        
        WebLogger.error("RegisterServlet", sessionIdentifier, "Registration error for " + username, exception);
        
        cleanupFailedConnection(httpRequest.getSession(), sessionIdentifier);
        displayConnectionError(httpRequest, httpResponse);
    }

    /**
     * Cleans up connection resources after a failed registration attempt.
     * 
     * @param userSession the user's HTTP session
     * @param sessionIdentifier the session ID for logging
     */
    private void cleanupFailedConnection(HttpSession userSession, String sessionIdentifier) {
        WebLogger.info("RegisterServlet", sessionIdentifier, "Cleaning up failed connection");
        WebConnectionManager.removeAndTerminateConnection(userSession);
    }

    /**
     * Displays a connection error to the user.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void displayConnectionError(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
            throws ServletException, IOException {
        
        httpRequest.setAttribute(ATTR_ERROR, DEFAULT_ERROR_MESSAGE);
        forwardToRegistrationView(httpRequest, httpResponse);
    }

    /**
     * Validates that the provided registration data is complete and valid.
     * 
     * @param username the username to validate
     * @param password the password to validate
     * @param nationality the nationality to validate
     * @param dateOfBirth the date of birth to validate
     * @return true if registration data is valid, false otherwise
     */
    private boolean areRegistrationDataValid(String username, String password, String nationality, String dateOfBirth) {
        return username != null && !username.trim().isEmpty() && 
               password != null && !password.trim().isEmpty() &&
               nationality != null && !nationality.trim().isEmpty() &&
               dateOfBirth != null && !dateOfBirth.trim().isEmpty();
    }

    /**
     * Handles the case where invalid registration data was provided.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void handleInvalidRegistrationData(HttpServletRequest httpRequest, HttpServletResponse httpResponse, 
                                             String sessionIdentifier) throws ServletException, IOException {
        
        WebLogger.warning("RegisterServlet", sessionIdentifier, "Invalid registration data provided");
        httpRequest.setAttribute(ATTR_ERROR, "All fields are required");
        forwardToRegistrationView(httpRequest, httpResponse);
    }

    /**
     * Forwards the request to the registration view.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void forwardToRegistrationView(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
            throws ServletException, IOException {
        
        httpRequest.getRequestDispatcher(REGISTER_VIEW).forward(httpRequest, httpResponse);
    }

    /**
     * Forwards the request to the login view.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void forwardToLoginView(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
            throws ServletException, IOException {
        
        httpRequest.getRequestDispatcher(LOGIN_VIEW).forward(httpRequest, httpResponse);
    }

    /**
     * Gets the session identifier for logging purposes.
     * 
     * @param httpRequest the HTTP request object
     * @return the session identifier
     */
    private String getSessionIdentifier(HttpServletRequest httpRequest) {
        return httpRequest.getSession().getId();
    }
}