package communication.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import communication.XMLHelper;
import core.GameState;
import core.Player;

/**
 * ServerProtocol handles receiving client-initiated XML requests and sending
 * server-initiated XML replies or notifications for the Gobang game.
 */
public class ServerProtocol {

	/**
	 * Handler to process decoded requests on the server-side.
	 */
	private final ServerProtocolHandler serverHandler;

	/**
	 * InputStream for reading XML requests from the client.
	 */
	private final InputStream inputStream;

	/**
	 * OutputStream for writing XML replies and notifications to the client.
	 */
	private final OutputStream outputStream;

	/**
	 * Constructs a ProtocolServer bound to the given streams and handler.
	 *
	 * @param serverHandler the callback interface for processing incoming requests
	 * @param inputStream   the InputStream for reading XML requests
	 * @param outputStream  the OutputStream for writing XML replies/notifications
	 */
	public ServerProtocol(ServerProtocolHandler serverHandler, InputStream inputStream, OutputStream outputStream) {
		this.serverHandler = serverHandler;
		this.inputStream = inputStream;
		this.outputStream = outputStream;
	}

	/**
	 * Receives the next XML request, validates it against the protocol XSD, parses
	 * its payload, and dispatches to the server handler.
	 *
	 * @throws Exception if an error occurs during reading, validation, or
	 *                   dispatching
	 */
	public void receiveRequests() throws Exception {
		Document requestDocument = XMLHelper.readDocumentFromStream(inputStream);
		if (!XMLHelper.validateDocumentAgainstXsd(requestDocument)) {
			throw new IllegalArgumentException("Invalid XML against protocolo XSD");
		}

		Element payloadElement = (Element) requestDocument.getDocumentElement().getFirstChild();
		String requestType = payloadElement.getNodeName();

		switch (requestType) {
		case "LoginRequest":
			String loginUsername = XMLHelper.getElementTextContent(payloadElement, "Username");
			String loginPassword = XMLHelper.getElementTextContent(payloadElement, "Password");
			serverHandler.handleLoginRequest(loginUsername, loginPassword);
			break;

		case "LogoutRequest":
			serverHandler.handleLogoutRequest();
			break;

		case "RegisterRequest":
			String registerUsername = XMLHelper.getElementTextContent(payloadElement, "Username");
			String registerPassword = XMLHelper.getElementTextContent(payloadElement, "Password");
			String registerNationality = XMLHelper.getElementTextContent(payloadElement, "Nationality");
			LocalDate registerDob = LocalDate.parse(XMLHelper.getElementTextContent(payloadElement, "DateOfBirth"));
			serverHandler.handleRegisterRequest(registerUsername, registerPassword, registerNationality, registerDob);
			break;

		case "UpdateProfileRequest":
			String newPassword = XMLHelper.getElementTextContent(payloadElement, "Password");
			String newNationality = XMLHelper.getElementTextContent(payloadElement, "Nationality");
			String dobText = XMLHelper.getElementTextContent(payloadElement, "DateOfBirth");
			LocalDate newDob = (dobText != null) ? LocalDate.parse(dobText) : null;
			String newTheme = XMLHelper.getElementTextContent(payloadElement, "Theme");
			serverHandler.handleUpdateProfileRequest(newPassword, newNationality, newDob, newTheme);
			break;

		case "UpdatePhotoRequest":
			String photoBase64 = XMLHelper.getElementTextContent(payloadElement, "Photo");
			serverHandler.handleUpdatePhotoRequest(photoBase64);
			break;

		case "LeaderboardRequest":
			serverHandler.handleLeaderboardRequest();
			break;

		case "ListPlayersRequest":
			serverHandler.handleListPlayersRequest();
			break;

		case "ReadyRequest":
			boolean isReady = Boolean.parseBoolean(payloadElement.getTextContent());
			serverHandler.handleReadyRequest(isReady);
			break;

		case "ChallengeRequest":
			String opponent = XMLHelper.getElementTextContent(payloadElement, "Opponent");
			serverHandler.handleChallengeRequest(opponent);
			break;

		case "ChallengeReply":
			String challengeStatus = payloadElement.getTextContent();
			serverHandler.handleChallengeReply(challengeStatus);
			break;

		case "MoveRequest":
			String gameId = XMLHelper.getElementTextContent(payloadElement, "GameId");
			int row = Integer.parseInt(XMLHelper.getElementTextContent(payloadElement, "Row"));
			int col = Integer.parseInt(XMLHelper.getElementTextContent(payloadElement, "Col"));
			serverHandler.handleMoveRequest(gameId, row, col);
			break;

		case "ForfeitMatchRequest":
			String forfeitGameId = XMLHelper.getElementTextContent(payloadElement, "GameId");
			serverHandler.handleForfeitMatchRequest(forfeitGameId);
			break;

		default:
			throw new UnsupportedOperationException("Unexpected request type: " + requestType);
		}
	}

	/**
	 * Sends a ChallengeInvitation to notify an opponent of a challenge.
	 */
	public void sendChallengeInvitation(Player challenge) {
		Document responseDoc = ServerProtocolFactory.createChallengeInvitation(challenge);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends a ChallengeReply indicating acceptance or rejection.
	 */
	public void sendChallengeReply(String status) {
		Document responseDoc = ServerProtocolFactory.createChallengeReply(status);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends an EndGameNotification announcing the winner.
	 */
	public void sendEndGameNotification(String gameId, String winnerUsername, Duration totalTime) {
		Document responseDoc = ServerProtocolFactory.createEndGameNotification(gameId, winnerUsername, totalTime);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends an ErrorNotification for timeouts or disconnections.
	 */
	public void sendErrorNotification(String errorCode, String description) {
		Document responseDoc = ServerProtocolFactory.createErrorNotification(errorCode, description);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends a GameStarted notification to both clients at game start.
	 */
	public void sendGameStarted(String gameId, Player blackPlayer, Player whitePlayer) {
		Document responseDoc = ServerProtocolFactory.createGameStarted(gameId, blackPlayer, whitePlayer);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends a GameState snapshot containing board rows, timing info and next
	 * player.
	 */
	public void sendGameState(GameState gameState) {
		Document responseDoc = ServerProtocolFactory.createGameState(gameState);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends a LeaderboardReply message with registered players.
	 */
	public void sendLeaderboardReply(List<Player> players) {
		Document responseDoc = ServerProtocolFactory.createLeaderboardReply(players);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends a ListPlayersReply message containing the current players.
	 */
	public void sendListPlayersReply(List<Player> players) {
		Document responseDoc = ServerProtocolFactory.createListPlayersReply(players);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends a LoginReply message with status and optional profile.
	 */
	public void sendLoginReply(String status, Player profile) {
		Document responseDoc = ServerProtocolFactory.createLoginReply(status, profile);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends a LogoutRequest to log out but keep the connection open.
	 */
	public void sendLogoutReply(String status) {
		Document responseDoc = ServerProtocolFactory.createLogoutReply(status);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends a MoveReply message with gameId and status.
	 */
	public void sendMoveReply(String gameId, String status) {
		Document responseDoc = ServerProtocolFactory.createMoveReply(gameId, status);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends an OpponentDisconnected notification for a specific game.
	 */
	public void sendOpponentDisconnected(String gameId, String description) {
		Document responseDoc = ServerProtocolFactory.createOpponentDisconnected(gameId, description);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends a ProfileUpdateNotification with updated stats.
	 */
	public void sendProfileUpdateNotification(Player player) {
		Document responseDoc = ServerProtocolFactory.createProfileUpdateNotification(player);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends a ReadyReply acknowledging readiness status.
	 */
	public void sendReadyReply(String status) {
		Document responseDoc = ServerProtocolFactory.createReadyReply(status);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends a RegisterReply message with the given status.
	 */
	public void sendRegisterReply(String status) {
		Document responseDoc = ServerProtocolFactory.createRegisterReply(status);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends an UpdatePhotoReply message with the given status.
	 */
	public void sendUpdatePhotoReply(String status) {
		Document responseDoc = ServerProtocolFactory.createUpdatePhotoReply(status);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}

	/**
	 * Sends an UpdateProfileReply message with the given status.
	 */
	public void sendUpdateProfileReply(String status) {
		Document responseDoc = ServerProtocolFactory.createUpdateProfileReply(status);
		XMLHelper.writeDocumentToStream(responseDoc, outputStream);
	}
}