package communication.client;

import java.time.Duration;
import java.util.List;

import core.GameState;
import core.Player;

/**
 * ClientProtocolHandler defines callbacks for handling all server-initiated
 * protocol messages for the Gobang game from the client's perspective.
 */
public interface ClientProtocolHandler {

	/**
	 * Called when a ChallengeInvitation arrives with the challenger’s full profile.
	 *
	 * @param challengerProfile the Player object of who challenged you
	 */
	void handleChallengeInvitation(Player challengerProfile);

	/**
	 * Called when a ChallengeReply is received.
	 *
	 * @param challengeStatus the challenge result status (e.g. "Accepted",
	 *                        "Rejected")
	 * @param message         an optional explanatory message
	 */
	void handleChallengeReply(String challengeStatus);

	/**
	 * Called when an EndGameNotification is received.
	 *
	 * @param gameId         the unique identifier of the finished game
	 * @param winnerUsername the user name of the winning player
	 * @param totalTime      the total duration of the game until forfeit
	 */
	void handleEndGameNotification(String gameId, String winnerUsername, Duration totalTime);

	/**
	 * Called when an ErrorNotification is received.
	 *
	 * @param errorCode   the error code (e.g. "Timeout", "OpponentDisconnected")
	 * @param description a human-readable error description
	 */
	void handleErrorNotification(String errorCode, String description);

	/**
	 * Called when a GameStarted message is received.
	 *
	 * @param gameId      the unique identifier of the game
	 * @param blackPlayer the Player object assigned Black pieces
	 * @param whitePlayer the Player object assigned White pieces
	 */
	void handleGameStarted(String gameId, Player blackPlayer, Player whitePlayer);

	/**
	 * Called when a GameState snapshot is received.
	 *
	 * @param gameState the current game state object containing board rows and next
	 *                  player
	 */
	void handleGameState(GameState gameState);

	/**
	 * Called when a LeaderboardReply is received.
	 * 
	 * @param players the list of players in a leaderboard.
	 */
	void handleLeaderboardReply(List<Player> players);

	/**
	 * Called when a ListPlayersReply is received.
	 *
	 * @param players the list of ready players as Player objects
	 */
	void handleListPlayersReply(List<Player> players);

	/**
	 * Called when a LoginReply is received.
	 *
	 * @param loginStatus   the login result status (e.g. "Accepted",
	 *                      "WrongPassword")
	 * @param playerProfile
	 */
	void handleLoginReply(String loginStatus, Player playerProfile);

	/**
	 * Called when a LogoutReply is received.
	 *
	 * @param status the logout status ("Accepted","Rejected")
	 */
	void handleLogoutReply(String status);

	/**
	 * Called when a MoveReply is received.
	 *
	 * @param gameId     the game identifier
	 * @param moveStatus the result status of the move
	 */
	void handleMoveReply(String gameId, String moveStatus);

	/**
	 * Called when an OpponentDisconnected notification is received.
	 *
	 * @param gameId      the game identifier
	 * @param description description of the disconnection
	 */
	void handleOpponentDisconnected(String gameId, String description);

	/**
	 * Called when a ProfileUpdateNotification is received with updated stats.
	 *
	 * @param updatedProfile the updated player profile
	 */
	void handleProfileUpdateNotification(Player updatedProfile);

	/**
	 * Called when a ReadyReply is received.
	 *
	 * @param readyStatus the readiness status (e.g. "Accepted", "Rejected")
	 */
	void handleReadyReply(String readyStatus);

	/**
	 * Called when a RegisterReply is received.
	 *
	 * @param registerStatus the registration result status (e.g. "Accepted",
	 *                       "UsernameDuplicated")
	 */
	void handleRegisterReply(String registerStatus);

	/**
	 * Called when an UpdatePhotoReply is received.
	 *
	 * @param updatePhotoStatus the photo update result status (e.g. "Accepted",
	 *                          "PhotoInvalid")
	 */
	void handleUpdatePhotoReply(String updatePhotoStatus);

	/**
	 * Called when an UpdateProfileReply is received.
	 *
	 * @param updateProfileStatus the profile update result status (e.g. "Accepted",
	 *                            "Rejected")
	 */
	void handleUpdateProfileReply(String updateProfileStatus);
}
