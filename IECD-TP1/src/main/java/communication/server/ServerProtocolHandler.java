package communication.server;

import java.time.LocalDate;

/**
 * ServerProtocolHandler defines callbacks for handling all client-initiated
 * protocol messages on the server side in the Gobang game.
 */
public interface ServerProtocolHandler {

	/**
	 * Called when ChallengeReply is received, indicating if the challenge was
	 * accepted or denied.
	 * 
	 * @param challengeReply
	 */
	void handleChallengeReply(String challengeReply);

	/**
	 * Called when a ChallengeRequest is received to start a match offer.
	 *
	 * @param challengerUsername the user issuing the challenge
	 * @param opponentUsername   the user being challenged
	 */
	void handleChallengeRequest(String opponentUsername);

	/**
	 * Called when a ForfeitMatchRequest is received to forfeit an ongoing game.
	 *
	 * @param gameId the unique identifier for the game to forfeit
	 */
	void handleForfeitMatchRequest(String gameId);

	/**
	 * Called when a LeaderboardRequest is received.
	 */
	void handleLeaderboardRequest();

	/**
	 * Called when a ListPlayersRequest is received (lobby pull).
	 */
	void handleListPlayersRequest();

	/**
	 * Called when a LoginRequest is received.
	 *
	 * @param username the login username
	 * @param password the login password
	 */
	void handleLoginRequest(String username, String password);

	/**
	 * Called when a LogoutRequest is received.
	 */
	void handleLogoutRequest();

	/**
	 * Called when a MoveRequest is received representing a player's move.
	 *
	 * @param gameId the unique identifier for the game
	 * @param row    the row coordinate of the move
	 * @param col    the column coordinate of the move
	 */
	void handleMoveRequest(String gameId, int row, int col);

	/**
	 * Called when a ReadyRequest is received.
	 *
	 * @param isReady true if the client wants to be marked ready, false to unready
	 */
	void handleReadyRequest(boolean isReady);

	/**
	 * Called when a RegisterRequest is received.
	 *
	 * @param username    the desired username
	 * @param password    the desired password
	 * @param nationality the user's nationality code (3-letter ISO)
	 * @param dateOfBirth the user's date of birth
	 */
	void handleRegisterRequest(String username, String password, String nationality, LocalDate dateOfBirth);

	/**
	 * Called when an UpdatePhotoRequest is received.
	 *
	 * @param photoBase64 the new profile photo encoded in Base64
	 */
	void handleUpdatePhotoRequest(String photoBase64);

	/**
	 * Called when an UpdateProfileRequest is received to change profile fields.
	 *
	 * @param newPassword    the new password, or null if unchanged
	 * @param newNationality the new nationality code, or null if unchanged
	 * @param newDateOfBirth the new date of birth, or null if unchanged
	 * @param newTheme       the new theme, or null if unchanged
	 */
	void handleUpdateProfileRequest(String newPassword, String newNationality, LocalDate newDateOfBirth,
			String newTheme);
}