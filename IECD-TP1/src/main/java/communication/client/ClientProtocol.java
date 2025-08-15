package communication.client;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import communication.XMLHelper;
import core.GameState;
import core.Player;

/**
 * ClientProtocol handles sending client-initiated XML requests and receiving
 * server-initiated XML replies or notifications for the Gobang game.
 */
public class ClientProtocol {

	/**
	 * Handler to process decoded replies and notifications on the client-side.
	 */
	private final ClientProtocolHandler clientHandler;

	/**
	 * InputStream for reading XML replies and notifications.
	 */
	private final InputStream inputStream;

	/**
	 * OutputStream for writing XML requests.
	 */
	private final OutputStream outputStream;

	/**
	 * Constructs a ProtocolClient with the given handler and streams.
	 *
	 * @param clientHandler the callback interface for processing incoming messages
	 * @param inputStream   the InputStream for reading XML replies/notifications
	 * @param outputStream  the OutputStream for sending XML requests
	 */
	public ClientProtocol(ClientProtocolHandler clientHandler, InputStream inputStream, OutputStream outputStream) {
		this.clientHandler = clientHandler;
		this.inputStream = inputStream;
		this.outputStream = outputStream;
	}

	/**
	 * Receives the next XML message, validates it against the protocolo XSD, parses
	 * its payload, and dispatches to the client handler.
	 *
	 * @throws Exception if an error occurs during read, validate or dispatch
	 */
	public void receiveReplies() throws Exception {
		Document responseDocument = XMLHelper.readDocumentFromStream(inputStream);
		if (!XMLHelper.validateDocumentAgainstXsd(responseDocument)) {
			throw new IllegalArgumentException("Invalid XML against protocolo XSD");
		}

		Element payloadElement = (Element) responseDocument.getDocumentElement().getFirstChild();
		String messageType = payloadElement.getNodeName();

		switch (messageType) {
		case "LoginReply":
			String loginStatus = XMLHelper.getElementTextContent(payloadElement, "Status");
			Player playerProfile = null;
			if (loginStatus.equals("Accepted"))
				playerProfile = ClientProtocolFactory.parsePlayer(payloadElement);
			clientHandler.handleLoginReply(loginStatus, playerProfile);
			break;

		case "LogoutReply":
			String logoutStatus = payloadElement.getTextContent();
			clientHandler.handleLogoutReply(logoutStatus);
			break;

		case "RegisterReply":
			String registerStatus = payloadElement.getTextContent();
			clientHandler.handleRegisterReply(registerStatus);
			break;

		case "UpdateProfileReply":
			String profileStatus = payloadElement.getTextContent();
			clientHandler.handleUpdateProfileReply(profileStatus);
			break;

		case "UpdatePhotoReply":
			String photoStatus = payloadElement.getTextContent();
			clientHandler.handleUpdatePhotoReply(photoStatus);
			break;

		case "ProfileUpdateNotification":
			Player updatedProfile = ClientProtocolFactory.parsePlayer(payloadElement);
			clientHandler.handleProfileUpdateNotification(updatedProfile);
			break;

		case "LeaderboardReply":
			List<Player> leaderboard = ClientProtocolFactory.parsePlayers(payloadElement);
			clientHandler.handleLeaderboardReply(leaderboard);
			break;

		case "ListPlayersReply":
			List<Player> players = ClientProtocolFactory.parsePlayers(payloadElement);
			clientHandler.handleListPlayersReply(players);
			break;

		case "ReadyReply":
			String readyStatus = payloadElement.getTextContent();
			clientHandler.handleReadyReply(readyStatus);
			break;

		case "ChallengeInvitation":
			Player challengerProfile = ClientProtocolFactory.parsePlayer(payloadElement);
			clientHandler.handleChallengeInvitation(challengerProfile);
			break;

		case "ChallengeReply":
			String challengeStatus = payloadElement.getTextContent();
			clientHandler.handleChallengeReply(challengeStatus);
			break;

		case "GameStarted":
			String gameId = XMLHelper.getElementTextContent(payloadElement, "GameId");

			// Parse full player profiles
			Element blackProfileElement = (Element) payloadElement.getElementsByTagName("BlackPlayerProfile").item(0);
			Element whiteProfileElement = (Element) payloadElement.getElementsByTagName("WhitePlayerProfile").item(0);

			Player blackPlayer = ClientProtocolFactory.parsePlayer(blackProfileElement);
			Player whitePlayer = ClientProtocolFactory.parsePlayer(whiteProfileElement);

			clientHandler.handleGameStarted(gameId, blackPlayer, whitePlayer);
			break;

		case "MoveReply":
			String moveGameId = XMLHelper.getElementTextContent(payloadElement, "GameId");
			String moveStatus = XMLHelper.getElementTextContent(payloadElement, "Status");
			clientHandler.handleMoveReply(moveGameId, moveStatus);
			break;

		case "GameState":
			GameState gameState = ClientProtocolFactory.parseGameState(payloadElement);
			clientHandler.handleGameState(gameState);
			break;

		case "EndGameNotification":
			String finishedGameId = XMLHelper.getElementTextContent(payloadElement, "GameId");
			String winnerUsername = XMLHelper.getElementTextContent(payloadElement, "Winner");
			Duration totalTime = java.time.Duration.parse(XMLHelper.getElementTextContent(payloadElement, "TotalTime"));
			clientHandler.handleEndGameNotification(finishedGameId, winnerUsername, totalTime);
			break;

		case "OpponentDisconnected":
			String disconnectGameId = XMLHelper.getElementTextContent(payloadElement, "GameId");
			String disconnectDesc = XMLHelper.getElementTextContent(payloadElement, "Description");
			clientHandler.handleOpponentDisconnected(disconnectGameId, disconnectDesc);
			break;

		case "ErrorNotification":
			String errorCode = XMLHelper.getElementTextContent(payloadElement, "Code");
			String description = XMLHelper.getElementTextContent(payloadElement, "Description");
			clientHandler.handleErrorNotification(errorCode, description);
			break;

		default:
			throw new UnsupportedOperationException("Unexpected message type: " + messageType);
		}
	}

	/**
	 * Sends a ChallengeReply indicating acceptance or rejection.
	 *
	 * @param status the challenge result status ("Accepted", "Rejected")
	 */
	public void sendChallengeReply(String status) {
		Document requestDocument = ClientProtocolFactory.createChallengeReply(status);
		XMLHelper.writeDocumentToStream(requestDocument, outputStream);
	}

	/**
	 * Sends a ChallengeRequest to initiate a match with another player.
	 *
	 * @param challengerUsername the user issuing the challenge
	 * @param opponentUsername   the user being challenged
	 */
	public void sendChallengeRequest(String opponentUsername) {
		Document requestDocument = ClientProtocolFactory.createChallengeRequest(opponentUsername);
		XMLHelper.writeDocumentToStream(requestDocument, outputStream);
	}

	/**
	 * Sends a ForfeitMatchRequest to forfeit an ongoing game.
	 * 
	 * @param gameId the unique identifier for the game to forfeit
	 */
	public void sendForfeitMatchRequest(String gameId) {
		Document doc = ClientProtocolFactory.createForfeitMatchRequest(gameId);
		XMLHelper.writeDocumentToStream(doc, outputStream);
	}

	/**
	 * Sends a LeaderboardRequest to retrieve the current leaderboard.
	 */
	public void sendLeaderboardRequest() {
		Document requestDocument = ClientProtocolFactory.createLeaderboardRequest();
		XMLHelper.writeDocumentToStream(requestDocument, outputStream);
	}

	/**
	 * Sends a ListPlayersRequest to retrieve the current lobby players.
	 */
	public void sendListPlayersRequest() {
		Document requestDocument = ClientProtocolFactory.createListPlayersRequest();
		XMLHelper.writeDocumentToStream(requestDocument, outputStream);
	}

	/**
	 * Sends a LoginRequest with the provided credentials.
	 *
	 * @param username the user's login name
	 * @param password the user's login password
	 */
	public void sendLoginRequest(String username, String password) {
		Document requestDocument = ClientProtocolFactory.createLoginRequest(username, password);
		XMLHelper.writeDocumentToStream(requestDocument, outputStream);
	}

	/**
	 * Sends a LogoutRequest to log out but keep the connection open.
	 */
	public void sendLogoutRequest() {
		Document requestDocument = ClientProtocolFactory.createLogoutRequest();
		XMLHelper.writeDocumentToStream(requestDocument, outputStream);
	}

	/**
	 * Sends a MoveRequest with the specified move coordinates and piece color.
	 *
	 * @param gameId the unique identifier for the game
	 * @param row    the row coordinate of the move
	 * @param col    the column coordinate of the move
	 */
	public void sendMoveRequest(String gameId, int row, int col) {
		Document requestDocument = ClientProtocolFactory.createMoveRequest(gameId, row, col);
		XMLHelper.writeDocumentToStream(requestDocument, outputStream);
	}

	/**
	 * Sends a ReadyRequest indicating availability for play.
	 *
	 * @param isReady true if this client is ready, false to cancel readiness
	 */
	public void sendReadyRequest(boolean isReady) {
		Document requestDocument = ClientProtocolFactory.createReadyRequest(isReady);
		XMLHelper.writeDocumentToStream(requestDocument, outputStream);
	}

	/**
	 * Sends a RegisterRequest with the provided user details.
	 *
	 * @param username    the desired user name
	 * @param password    the desired password
	 * @param nationality the user's nationality code
	 * @param dateOfBirth the user's date of birth
	 */
	public void sendRegisterRequest(String username, String password, String nationality, String dateOfBirth) {
		Document requestDocument = ClientProtocolFactory.createRegisterRequest(username, password, nationality,
				dateOfBirth);
		XMLHelper.writeDocumentToStream(requestDocument, outputStream);
	}

	/**
	 * Sends an UpdatePhotoRequest containing new photo data in Base64.
	 *
	 * @param photoBase64 the new photo encoded in Base64
	 */
	public void sendUpdatePhotoRequest(String photoBase64) {
		Document requestDocument = ClientProtocolFactory.createUpdatePhotoRequest(photoBase64);
		XMLHelper.writeDocumentToStream(requestDocument, outputStream);
	}

	/**
	 * Sends an UpdateProfileRequest to change user profile fields.
	 *
	 * @param newPassword    the new password, or null if unchanged
	 * @param newNationality the new nationality code, or null if unchanged
	 * @param newDateOfBirth the new date of birth, or null if unchanged
	 * @param newTheme       the new theme, or null if unchanged
	 */
	public void sendUpdateProfileRequest(String newPassword, String newNationality, String newDateOfBirth,
			String newTheme) {
		Document requestDocument = ClientProtocolFactory.createUpdateProfileRequest(newPassword, newNationality,
				newDateOfBirth, newTheme);
		XMLHelper.writeDocumentToStream(requestDocument, outputStream);
	}
}