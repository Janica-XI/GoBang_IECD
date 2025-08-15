package web.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Base64;

import core.Player;
import core.client.ClientUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;
import web.utils.WebClientConnection;
import web.utils.WebConnectionManager;
import web.utils.WebLogger;

/**
 * Handles user profile management and updates for the GoBang web application.
 * 
 * This servlet manages both the display of the user profile form and the processing
 * of profile update requests including personal information and photo uploads.
 * It provides comprehensive profile management functionality with proper validation
 * and server synchronization.
 * 
 * GET requests:
 * - Displays the profile management form (profile.jsp)
 * - Requires user authentication
 * 
 * POST requests:
 * - Processes profile updates (password, nationality, date of birth, theme)
 * - Handles photo uploads with validation and Base64 conversion
 * - Synchronizes changes with the game server
 * - Updates local session state with new profile data
 * - Provides success/error feedback to users
 * 
 * Features:
 * - Secure password updates with length validation
 * - Date of birth validation and parsing
 * - Photo upload with file type and size validation
 * - Base64 encoding for server transmission
 * - Real-time server communication for updates
 * - Local session synchronization
 * - Comprehensive error handling and user feedback
 * 
 * File upload constraints:
 * - Maximum file size: 5MB
 * - Supported formats: Image files only
 * - Automatic Base64 conversion for server storage
 * 
 * Security:
 * - Requires authenticated user (loggedInPlayer in session)
 * - Input validation for all form fields
 * - File type and size validation for uploads
 * - Server-side validation of all changes
 */
@WebServlet("/profile")
@MultipartConfig(maxFileSize = 5 * 1024 * 1024) // 5MB max file size
public class ProfileServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    
    private static final String PROFILE_VIEW = "profile.jsp";
    private static final String LOGIN_REDIRECT_PATH = "login";
    private static final String LOBBY_REDIRECT_PATH = "lobby";
    
    private static final String PARAM_NEW_PASSWORD = "newPassword";
    private static final String PARAM_NATIONALITY = "nationality";
    private static final String PARAM_DATE_OF_BIRTH = "dateOfBirth";
    private static final String PARAM_THEME = "theme";
    private static final String PARAM_PROFILE_PHOTO = "profilePhoto";
    
    private static final String ATTR_LOGGED_IN_PLAYER = "loggedInPlayer";
    private static final String ATTR_ERROR = "error";
    private static final String ATTR_LAST_PROFILE_UPDATE_STATUS = "lastProfileUpdateStatus";
    private static final String ATTR_LAST_PHOTO_UPDATE_STATUS = "lastPhotoUpdateStatus";
    
    private static final String STATUS_ACCEPTED = "Accepted";
    private static final int PASSWORD_MIN_LENGTH = 8;
    private static final int PASSWORD_MAX_LENGTH = 16;
    private static final long MAX_PHOTO_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
    private static final String IMAGE_CONTENT_TYPE_PREFIX = "image/";
    private static final int SERVER_RESPONSE_WAIT_MS = 500;
    
    private static final String ERROR_NO_CONNECTION = "No server connection";
    private static final String ERROR_PASSWORD_LENGTH = "Password must be between 8 and 16 characters";
    private static final String ERROR_INVALID_DATE = "Invalid date of birth";
    private static final String ERROR_INVALID_IMAGE = "Please select a valid image file";
    private static final String ERROR_IMAGE_TOO_LARGE = "Image must be maximum 5MB";
    private static final String ERROR_PROCESSING_IMAGE = "Error processing image";
    private static final String ERROR_INTERNAL_SERVER = "Internal server error";

    @Override
    protected void doGet(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        String sessionIdentifier = getSessionIdentifier(httpRequest);
        WebLogger.info("ProfileServlet", sessionIdentifier, "Profile form requested");

        if (!isUserAuthenticated(httpRequest)) {
            redirectToLogin(httpResponse, sessionIdentifier);
            return;
        }

        forwardToProfileView(httpRequest, httpResponse);
    }

    @Override
    protected void doPost(HttpServletRequest httpRequest, HttpServletResponse httpResponse)
            throws ServletException, IOException {

        String sessionIdentifier = getSessionIdentifier(httpRequest);
        
        if (!isUserAuthenticated(httpRequest)) {
            redirectToLogin(httpResponse, sessionIdentifier);
            return;
        }

        Player loggedInPlayer = getLoggedInPlayer(httpRequest);
        WebLogger.info("ProfileServlet", sessionIdentifier, "Profile update request for user: " + loggedInPlayer.getUsername());

        try {
            processProfileUpdateRequest(httpRequest, httpResponse, loggedInPlayer, sessionIdentifier);
        } catch (Exception exception) {
            handleProfileUpdateException(sessionIdentifier, exception, httpRequest, httpResponse);
        }
    }

    /**
     * Processes a complete profile update request including validation and server communication.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param loggedInPlayer the current user's profile
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if any step of the update process fails
     */
    private void processProfileUpdateRequest(HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                           Player loggedInPlayer, String sessionIdentifier) throws Exception {
        
        WebClientConnection serverConnection = validateConnectionForUpdate(httpRequest, sessionIdentifier);
        ProfileUpdateData updateData = extractAndValidateUpdateData(httpRequest, httpResponse, sessionIdentifier);
        
        if (updateData == null) {
            return; // Validation failed, response already sent
        }
        
        clearPreviousUpdateStatus(httpRequest.getSession());
        
        if (updateData.hasProfileChanges()) {
            processProfileDataUpdate(serverConnection, updateData, httpRequest, httpResponse, sessionIdentifier);
            if (httpResponse.isCommitted()) {
                return; // Error occurred, response already sent
            }
        }
        
        if (updateData.hasPhotoUpload()) {
            processPhotoUpdate(serverConnection, updateData, loggedInPlayer, httpRequest, httpResponse, sessionIdentifier);
            if (httpResponse.isCommitted()) {
                return; // Error occurred, response already sent
            }
        }
        
        updateLocalPlayerProfile(loggedInPlayer, updateData);
        updateSessionWithNewProfile(httpRequest.getSession(), loggedInPlayer);
        redirectToLobbyWithSuccess(httpResponse, loggedInPlayer.getUsername(), sessionIdentifier);
    }

    /**
     * Validates that a connection exists and is active for processing updates.
     * 
     * @param httpRequest the HTTP request object
     * @param sessionIdentifier the session ID for logging
     * @return the active WebClientConnection
     * @throws Exception if no valid connection exists
     */
    private WebClientConnection validateConnectionForUpdate(HttpServletRequest httpRequest, String sessionIdentifier) 
            throws Exception {
        
        WebClientConnection connection = WebConnectionManager.getExistingConnection(httpRequest.getSession());
        
        if (connection == null || !connection.isConnectionActive()) {
            WebLogger.error("ProfileServlet", sessionIdentifier, "No active connection for profile update");
            throw new Exception(ERROR_NO_CONNECTION);
        }
        
        return connection;
    }

    /**
     * Extracts and validates profile update data from the request.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @return ProfileUpdateData object if validation succeeds, null if validation fails
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private ProfileUpdateData extractAndValidateUpdateData(HttpServletRequest httpRequest, 
                                                          HttpServletResponse httpResponse, 
                                                          String sessionIdentifier) 
                                                          throws ServletException, IOException {
        
        ProfileUpdateData updateData = new ProfileUpdateData();
        
        // Extract and validate password
        String newPassword = httpRequest.getParameter(PARAM_NEW_PASSWORD);
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            if (!isPasswordValid(newPassword)) {
                sendValidationError(ERROR_PASSWORD_LENGTH, httpRequest, httpResponse, sessionIdentifier);
                return null;
            }
            updateData.setPassword(newPassword.trim());
        }
        
        // Extract nationality
        String nationality = httpRequest.getParameter(PARAM_NATIONALITY);
        if (nationality != null && !nationality.trim().isEmpty()) {
            updateData.setNationality(nationality.trim());
        }
        
        // Extract and validate date of birth
        String dateOfBirthStr = httpRequest.getParameter(PARAM_DATE_OF_BIRTH);
        if (dateOfBirthStr != null && !dateOfBirthStr.trim().isEmpty()) {
            try {
                LocalDate dateOfBirth = LocalDate.parse(dateOfBirthStr);
                updateData.setDateOfBirth(dateOfBirth);
            } catch (Exception exception) {
                sendValidationError(ERROR_INVALID_DATE, httpRequest, httpResponse, sessionIdentifier);
                return null;
            }
        }
        
        // Extract theme
        String theme = httpRequest.getParameter(PARAM_THEME);
        if (theme != null && !theme.trim().isEmpty()) {
            updateData.setTheme(theme.trim());
        }
        
        // Extract and validate photo
        Part photoPart = httpRequest.getPart(PARAM_PROFILE_PHOTO);
        if (photoPart != null && photoPart.getSize() > 0) {
            if (!validatePhotoUpload(photoPart, httpRequest, httpResponse, sessionIdentifier)) {
                return null;
            }
            updateData.setPhotoPart(photoPart);
        }
        
        return updateData;
    }

    /**
     * Validates password according to length requirements.
     * 
     * @param password the password to validate
     * @return true if password is valid, false otherwise
     */
    private boolean isPasswordValid(String password) {
        return password.length() >= PASSWORD_MIN_LENGTH && password.length() <= PASSWORD_MAX_LENGTH;
    }

    /**
     * Validates a photo upload for file type and size constraints.
     * 
     * @param photoPart the uploaded photo part
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @return true if photo is valid, false otherwise
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private boolean validatePhotoUpload(Part photoPart, HttpServletRequest httpRequest, 
                                       HttpServletResponse httpResponse, String sessionIdentifier) 
                                       throws ServletException, IOException {
        
        WebLogger.info("ProfileServlet", sessionIdentifier, "Processing photo upload, size: " + photoPart.getSize());
        
        // Validate file type
        String contentType = photoPart.getContentType();
        if (contentType == null || !contentType.startsWith(IMAGE_CONTENT_TYPE_PREFIX)) {
            sendValidationError(ERROR_INVALID_IMAGE, httpRequest, httpResponse, sessionIdentifier);
            return false;
        }
        
        // Validate file size
        if (photoPart.getSize() > MAX_PHOTO_SIZE_BYTES) {
            sendValidationError(ERROR_IMAGE_TOO_LARGE, httpRequest, httpResponse, sessionIdentifier);
            return false;
        }
        
        return true;
    }

    /**
     * Clears any previous update status from the session.
     * 
     * @param userSession the user's HTTP session
     */
    private void clearPreviousUpdateStatus(HttpSession userSession) {
        userSession.removeAttribute(ATTR_LAST_PROFILE_UPDATE_STATUS);
        userSession.removeAttribute(ATTR_LAST_PHOTO_UPDATE_STATUS);
    }

    /**
     * Processes profile data updates (password, nationality, date of birth, theme).
     * 
     * @param serverConnection the server connection
     * @param updateData the profile update data
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if profile update fails
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void processProfileDataUpdate(WebClientConnection serverConnection, ProfileUpdateData updateData,
                                        HttpServletRequest httpRequest, HttpServletResponse httpResponse,
                                        String sessionIdentifier) throws Exception, ServletException, IOException {
        
        WebLogger.info("ProfileServlet", sessionIdentifier, "Sending profile data update - theme: " + updateData.getTheme());
        
        serverConnection.getCommunicationProtocol().sendUpdateProfileRequest(
            updateData.getPassword(),
            updateData.getNationality(),
            updateData.getDateOfBirth() != null ? updateData.getDateOfBirth().toString() : null,
            updateData.getTheme()
        );
        
        WebLogger.info("ProfileServlet", sessionIdentifier, "Profile update request sent");
        
        waitForServerResponse();
        
        String updateStatus = (String) httpRequest.getSession().getAttribute(ATTR_LAST_PROFILE_UPDATE_STATUS);
        if (!STATUS_ACCEPTED.equals(updateStatus)) {
            String errorMessage = createUserFriendlyErrorMessage(updateStatus, "Error updating profile");
            sendUpdateError(errorMessage, httpRequest, httpResponse, sessionIdentifier);
        }
    }

    /**
     * Processes photo upload and update.
     * 
     * @param serverConnection the server connection
     * @param updateData the profile update data containing photo
     * @param loggedInPlayer the current user's profile
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws Exception if photo update fails
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void processPhotoUpdate(WebClientConnection serverConnection, ProfileUpdateData updateData,
                                  Player loggedInPlayer, HttpServletRequest httpRequest, 
                                  HttpServletResponse httpResponse, String sessionIdentifier) 
                                  throws Exception, ServletException, IOException {
        
        try {
            String photoBase64 = convertPhotoToBase64(updateData.getPhotoPart(), sessionIdentifier);
            
            serverConnection.getCommunicationProtocol().sendUpdatePhotoRequest(photoBase64);
            WebLogger.info("ProfileServlet", sessionIdentifier, "Photo update request sent");
            
            waitForServerResponse();
            
            String photoStatus = (String) httpRequest.getSession().getAttribute(ATTR_LAST_PHOTO_UPDATE_STATUS);
            if (!STATUS_ACCEPTED.equals(photoStatus)) {
                String errorMessage = createUserFriendlyErrorMessage(photoStatus, "Error updating photo");
                sendUpdateError(errorMessage, httpRequest, httpResponse, sessionIdentifier);
                return;
            }
            
            // Update local player object with new photo
            loggedInPlayer.setPhotoBase64(photoBase64);
            
        } catch (Exception exception) {
            WebLogger.error("ProfileServlet", sessionIdentifier, "Error processing photo", exception);
            sendUpdateError(ERROR_PROCESSING_IMAGE, httpRequest, httpResponse, sessionIdentifier);
        }
    }

    /**
     * Converts an uploaded photo to Base64 format for server transmission.
     * 
     * @param photoPart the uploaded photo part
     * @param sessionIdentifier the session ID for logging
     * @return Base64 encoded photo string
     * @throws IOException if reading the photo fails
     */
    private String convertPhotoToBase64(Part photoPart, String sessionIdentifier) throws IOException {
        try (InputStream inputStream = photoPart.getInputStream()) {
            byte[] photoBytes = inputStream.readAllBytes();
            String photoBase64 = Base64.getEncoder().encodeToString(photoBytes);
            
            WebLogger.info("ProfileServlet", sessionIdentifier, "Photo converted to Base64, length: " + photoBase64.length());
            return photoBase64;
        }
    }

    /**
     * Updates the local player profile object with new data.
     * 
     * @param loggedInPlayer the player profile to update
     * @param updateData the new profile data
     */
    private void updateLocalPlayerProfile(Player loggedInPlayer, ProfileUpdateData updateData) {
        if (updateData.getPassword() != null) {
            loggedInPlayer.setPassword(updateData.getPassword());
        }
        if (updateData.getNationality() != null) {
            loggedInPlayer.setNationality(updateData.getNationality());
        }
        if (updateData.getDateOfBirth() != null) {
            loggedInPlayer.setDateOfBirth(updateData.getDateOfBirth());
        }
        if (updateData.getTheme() != null) {
            loggedInPlayer.setTheme(updateData.getTheme());
        }
    }

    /**
     * Updates the session with the modified player profile.
     * 
     * @param userSession the user's HTTP session
     * @param updatedPlayer the updated player profile
     */
    private void updateSessionWithNewProfile(HttpSession userSession, Player updatedPlayer) {
        userSession.setAttribute(ATTR_LOGGED_IN_PLAYER, updatedPlayer);
    }

    /**
     * Redirects to lobby with success message after successful profile update.
     * 
     * @param httpResponse the HTTP response object
     * @param username the username of the updated user
     * @param sessionIdentifier the session ID for logging
     * @throws IOException if redirect fails
     */
    private void redirectToLobbyWithSuccess(HttpServletResponse httpResponse, String username, String sessionIdentifier) 
            throws IOException {
        
        WebLogger.info("ProfileServlet", sessionIdentifier, "Profile update completed successfully for: " + username);
        httpResponse.sendRedirect(LOBBY_REDIRECT_PATH);
    }

    /**
     * Waits briefly for server response to arrive.
     */
    private void waitForServerResponse() {
        try {
            Thread.sleep(SERVER_RESPONSE_WAIT_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Creates a user-friendly error message from server status.
     * 
     * @param status the status from the server
     * @param defaultMessage the default message if status is null
     * @return user-friendly error message
     */
    private String createUserFriendlyErrorMessage(String status, String defaultMessage) {
        if (status != null) {
            return ClientUtils.getFriendlyStatusMessage(status);
        }
        return defaultMessage;
    }

    /**
     * Sends a validation error response to the user.
     * 
     * @param errorMessage the error message to display
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void sendValidationError(String errorMessage, HttpServletRequest httpRequest, 
                                   HttpServletResponse httpResponse, String sessionIdentifier) 
                                   throws ServletException, IOException {
        
        WebLogger.warning("ProfileServlet", sessionIdentifier, "Validation error: " + errorMessage);
        httpRequest.setAttribute(ATTR_ERROR, errorMessage);
        forwardToProfileView(httpRequest, httpResponse);
    }

    /**
     * Sends an update error response to the user.
     * 
     * @param errorMessage the error message to display
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void sendUpdateError(String errorMessage, HttpServletRequest httpRequest, 
                               HttpServletResponse httpResponse, String sessionIdentifier) 
                               throws ServletException, IOException {
        
        WebLogger.warning("ProfileServlet", sessionIdentifier, "Update error: " + errorMessage);
        httpRequest.setAttribute(ATTR_ERROR, errorMessage);
        forwardToProfileView(httpRequest, httpResponse);
    }

    /**
     * Checks if the current user is authenticated.
     * 
     * @param httpRequest the HTTP request to check
     * @return true if user is authenticated, false otherwise
     */
    private boolean isUserAuthenticated(HttpServletRequest httpRequest) {
        return getLoggedInPlayer(httpRequest) != null;
    }

    /**
     * Gets the logged-in player from the session.
     * 
     * @param httpRequest the HTTP request object
     * @return the logged-in Player object, or null if not authenticated
     */
    private Player getLoggedInPlayer(HttpServletRequest httpRequest) {
        return (Player) httpRequest.getSession().getAttribute(ATTR_LOGGED_IN_PLAYER);
    }

    /**
     * Redirects unauthenticated users to the login page.
     * 
     * @param httpResponse the HTTP response object
     * @param sessionIdentifier the session ID for logging
     * @throws IOException if redirect fails
     */
    private void redirectToLogin(HttpServletResponse httpResponse, String sessionIdentifier) throws IOException {
        WebLogger.info("ProfileServlet", sessionIdentifier, "User not authenticated - redirecting to login");
        httpResponse.sendRedirect(LOGIN_REDIRECT_PATH);
    }

    /**
     * Forwards the request to the profile view.
     * 
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void forwardToProfileView(HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
            throws ServletException, IOException {
        
        httpRequest.getRequestDispatcher(PROFILE_VIEW).forward(httpRequest, httpResponse);
    }

    /**
     * Handles exceptions that occur during profile update processing.
     * 
     * @param sessionIdentifier the session ID for logging
     * @param exception the exception that occurred
     * @param httpRequest the HTTP request object
     * @param httpResponse the HTTP response object
     * @throws ServletException if forwarding fails
     * @throws IOException if forwarding fails
     */
    private void handleProfileUpdateException(String sessionIdentifier, Exception exception, 
                                            HttpServletRequest httpRequest, HttpServletResponse httpResponse) 
                                            throws ServletException, IOException {
        
        WebLogger.error("ProfileServlet", sessionIdentifier, "Error updating profile", exception);
        httpRequest.setAttribute(ATTR_ERROR, ERROR_INTERNAL_SERVER + ": " + exception.getMessage());
        forwardToProfileView(httpRequest, httpResponse);
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

    /**
     * Data class to hold profile update information during processing.
     */
    private static class ProfileUpdateData {
        private String password;
        private String nationality;
        private LocalDate dateOfBirth;
        private String theme;
        private Part photoPart;

        public boolean hasProfileChanges() {
            return password != null || nationality != null || dateOfBirth != null || theme != null;
        }

        public boolean hasPhotoUpload() {
            return photoPart != null;
        }

        // Getters and setters
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }

        public String getNationality() { return nationality; }
        public void setNationality(String nationality) { this.nationality = nationality; }

        public LocalDate getDateOfBirth() { return dateOfBirth; }
        public void setDateOfBirth(LocalDate dateOfBirth) { this.dateOfBirth = dateOfBirth; }

        public String getTheme() { return theme; }
        public void setTheme(String theme) { this.theme = theme; }

        public Part getPhotoPart() { return photoPart; }
        public void setPhotoPart(Part photoPart) { this.photoPart = photoPart; }
    }
}