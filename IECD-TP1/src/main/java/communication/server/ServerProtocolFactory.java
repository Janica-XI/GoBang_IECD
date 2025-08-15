package communication.server;

import java.time.Duration;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import communication.XMLHelper;
import core.GameState;
import core.Player;

/**
 * ServerProtocolFactory provides static methods to create XML documents for
 * each protocol message type that the server sends in the Gobang game. It
 * covers authentication replies, profile updates, lobby notifications, match
 * handling, game flow events, and error notifications.
 */
public class ServerProtocolFactory {

	/**
	 * Helper function to attach a Player object into the document.
	 * 
	 * @param doc
	 * @param parent
	 * @param player
	 */
	private static void appendPlayerProfile(Document doc, Element parent, Player player) {
		XMLHelper.appendTextElement(doc, parent, "Username", player.getUsername());
		XMLHelper.appendTextElement(doc, parent, "Nationality", player.getNationality());
		XMLHelper.appendTextElement(doc, parent, "DateOfBirth", player.getDateOfBirth().toString());
		String base64Photo = player.getPhotoBase64();
		if (base64Photo != null) {
			XMLHelper.appendTextElement(doc, parent, "Photo", base64Photo);
		}
		XMLHelper.appendTextElement(doc, parent, "Victories", Integer.toString(player.getVictories()));
		XMLHelper.appendTextElement(doc, parent, "Defeats", Integer.toString(player.getDefeats()));
		XMLHelper.appendTextElement(doc, parent, "TotalTime", player.getTotalTime().toString());
		if (player.getTheme() != null) {
			XMLHelper.appendTextElement(doc, parent, "Theme", player.getTheme());
		}
	}

	/**
	 * Creates a ChallengeInvitation document carrying the full challenger profile.
	 *
	 * @param challenger the Player who issued the challenge
	 * @return a Document representing <ChallengeInvitation>
	 */
	public static Document createChallengeInvitation(Player challenger) {
		Document responseDoc = XMLHelper.createMessageDocument("ChallengeInvitation");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();
		// wrap challenger as ChallengerProfile
		Element profileElement = responseDoc.createElement("Challenger");
		// now append all PlayerType fields

		appendPlayerProfile(responseDoc, profileElement, challenger);

		payload.appendChild(profileElement);
		return responseDoc;
	}

	/**
	 * Creates a ChallengeReply document indicating acceptance or rejection.
	 *
	 * @param status the challenge result status ("Accepted", "Rejected")
	 * @return a Document representing <ChallengeReply>
	 */
	public static Document createChallengeReply(String status) {
		Document responseDoc = XMLHelper.createMessageDocument("ChallengeReply");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();
		payload.setTextContent(status);
		return responseDoc;
	}

	/**
	 * Creates an EndGameNotification document announcing the winner.
	 *
	 * @param gameId         the unique identifier for the finished game
	 * @param winnerUsername the user name of the winning player
	 * @param totalTime      the total duration of the game
	 * @return a Document representing <EndGameNotification>
	 */
	public static Document createEndGameNotification(String gameId, String winnerUsername, Duration totalTime) {
		Document responseDoc = XMLHelper.createMessageDocument("EndGameNotification");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();
		XMLHelper.appendTextElement(responseDoc, payload, "GameId", gameId);
		XMLHelper.appendTextElement(responseDoc, payload, "Winner", winnerUsername);
		XMLHelper.appendTextElement(responseDoc, payload, "TotalTime", totalTime.toString());
		return responseDoc;
	}

	/**
	 * Creates an ErrorNotification document for timeouts or disconnections.
	 *
	 * @param errorCode   the error code ("Timeout", "OpponentDisconnected")
	 * @param description a human-readable description
	 * @return a Document representing <ErrorNotification>
	 */
	public static Document createErrorNotification(String errorCode, String description) {
		Document responseDoc = XMLHelper.createMessageDocument("ErrorNotification");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();
		XMLHelper.appendTextElement(responseDoc, payload, "Code", errorCode);
		XMLHelper.appendTextElement(responseDoc, payload, "Description", description);
		return responseDoc;
	}

	/**
	 * Creates a GameStarted notification with full player profiles.
	 *
	 * @param gameId      the unique identifier for the new game
	 * @param blackPlayer the Player object assigned Black pieces
	 * @param whitePlayer the Player object assigned White pieces
	 * @return a Document representing <GameStarted>
	 */
	public static Document createGameStarted(String gameId, Player blackPlayer, Player whitePlayer) {
		Document responseDoc = XMLHelper.createMessageDocument("GameStarted");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();

		XMLHelper.appendTextElement(responseDoc, payload, "GameId", gameId);

		// Black player profile completo
		Element blackElement = responseDoc.createElement("BlackPlayerProfile");
		appendPlayerProfile(responseDoc, blackElement, blackPlayer);
		payload.appendChild(blackElement);

		// White player profile completo
		Element whiteElement = responseDoc.createElement("WhitePlayerProfile");
		appendPlayerProfile(responseDoc, whiteElement, whitePlayer);
		payload.appendChild(whiteElement);

		return responseDoc;
	}

	/**
	 * Creates a GameState document capturing the entire board, timing info and next
	 * player.
	 *
	 * @param gameState the GameState object to serialize
	 * @return a Document representing <GameState>
	 */
	public static Document createGameState(GameState gameState) {
		Document responseDoc = XMLHelper.createMessageDocument("GameState");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();

		XMLHelper.appendTextElement(responseDoc, payload, "GameId", gameState.getGameId());

		for (int rowIndex = 0; rowIndex < gameState.getBoardRows().size(); rowIndex++) {
			Element rowElement = responseDoc.createElement("Row");
			rowElement.setAttribute("index", Integer.toString(rowIndex));
			rowElement.setTextContent(gameState.getBoardRows().get(rowIndex));
			payload.appendChild(rowElement);
		}

		XMLHelper.appendTextElement(responseDoc, payload, "NextPlayer", gameState.getNextPlayerColor());

		// NOVO: Adicionar informação de timing
		XMLHelper.appendTextElement(responseDoc, payload, "BlackTimeRemaining",
				gameState.getBlackPlayerTimeRemaining().toString());
		XMLHelper.appendTextElement(responseDoc, payload, "WhiteTimeRemaining",
				gameState.getWhitePlayerTimeRemaining().toString());
		XMLHelper.appendTextElement(responseDoc, payload, "Timestamp", gameState.getTimestamp().toString());

		return responseDoc;
	}

	/**
	 * Creates a LeaderbordReply with a list of registered players.
	 * 
	 * @param players the list of registered players.
	 * @return a Document representing <LeaderboardReply>
	 */
	public static Document createLeaderboardReply(List<Player> players) {
		Document responseDoc = XMLHelper.createMessageDocument("LeaderboardReply");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();

		for (Player player : players) {
			Element playerElement = responseDoc.createElement("Player");
			appendPlayerProfile(responseDoc, playerElement, player);
			payload.appendChild(playerElement);
		}
		return responseDoc;
	}

	/**
	 * Creates a ListPlayersReply document containing a list of Player entries.
	 *
	 * @param players the list of Player objects to include
	 * @return a Document representing <ListPlayersReply>
	 */
	public static Document createListPlayersReply(List<Player> players) {
		Document responseDoc = XMLHelper.createMessageDocument("ListPlayersReply");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();
		for (Player player : players) {
			Element playerElement = responseDoc.createElement("Player");

			appendPlayerProfile(responseDoc, playerElement, player);

			payload.appendChild(playerElement);
		}
		return responseDoc;
	}

	/**
	 * Creates a LoginReply document with status and optional profile.
	 *
	 * @param status  the login result status ("Accepted", "UsernameUnknown",
	 *                "WrongPassword")
	 * @param profile the authenticated Player profile to include on success, or
	 *                null if login failed
	 * @return a Document representing <LoginReply>
	 */
	public static Document createLoginReply(String status, Player profile) {
		Document responseDoc = XMLHelper.createMessageDocument("LoginReply");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();
		XMLHelper.appendTextElement(responseDoc, payload, "Status", status);
		if (profile != null) {
			Element profileElement = responseDoc.createElement("Profile");

			appendPlayerProfile(responseDoc, profileElement, profile);

			payload.appendChild(profileElement);
		}
		return responseDoc;
	}

	/**
	 * Creates a LogoutReply document with status.
	 *
	 * @param status the logout result status ("Accepted","Rejected")
	 * @return a Document representing <LogoutReply>
	 */
	public static Document createLogoutReply(String status) {
		Document responseDoc = XMLHelper.createMessageDocument("LogoutReply");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();
		payload.setTextContent(status);
		return responseDoc;
	}

	/**
	 * Creates a MoveReply document with gameId and a status text for a move
	 * attempt.
	 *
	 * @param gameId the unique identifier for the game
	 * @param status the move result status ("Accepted", "InvalidMove", "WIN")
	 * @return a Document representing <MoveReply>
	 */
	public static Document createMoveReply(String gameId, String status) {
		Document responseDoc = XMLHelper.createMessageDocument("MoveReply");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();
		XMLHelper.appendTextElement(responseDoc, payload, "GameId", gameId);
		XMLHelper.appendTextElement(responseDoc, payload, "Status", status);
		return responseDoc;
	}

	/**
	 * Creates an OpponentDisconnected notification with gameId.
	 *
	 * @param gameId      the unique identifier for the game
	 * @param description a human-readable description
	 * @return a Document representing <OpponentDisconnected>
	 */
	public static Document createOpponentDisconnected(String gameId, String description) {
		Document responseDoc = XMLHelper.createMessageDocument("OpponentDisconnected");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();
		XMLHelper.appendTextElement(responseDoc, payload, "GameId", gameId);
		XMLHelper.appendTextElement(responseDoc, payload, "Description", description);
		return responseDoc;
	}

	/**
	 * Creates a ProfileUpdateNotification with updated player stats.
	 *
	 * @param player the updated Player object
	 * @return a Document representing <ProfileUpdateNotification>
	 */
	public static Document createProfileUpdateNotification(Player player) {
		Document responseDoc = XMLHelper.createMessageDocument("ProfileUpdateNotification");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();

		appendPlayerProfile(responseDoc, payload, player);

		return responseDoc;
	}

	/**
	 * Creates a ReadyReply document acknowledging a ReadyRequest.
	 *
	 * @param status the readiness result status ("Accepted", "Rejected")
	 * @return a Document representing <ReadyReply>
	 */
	public static Document createReadyReply(String status) {
		Document responseDoc = XMLHelper.createMessageDocument("ReadyReply");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();
		payload.setTextContent(status);
		return responseDoc;
	}

	/**
	 * Creates a RegisterReply document with a status text.
	 *
	 * @param status the registration result status ("Accepted",
	 *               "UsernameDuplicated", etc.)
	 * @return a Document representing <RegisterReply>
	 */
	public static Document createRegisterReply(String status) {
		Document responseDoc = XMLHelper.createMessageDocument("RegisterReply");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();
		payload.setTextContent(status);
		return responseDoc;
	}

	/**
	 * Creates an UpdatePhotoReply document with a status text.
	 *
	 * @param status the photo update result status ("Accepted", "PhotoInvalid")
	 * @return a Document representing <UpdatePhotoReply>
	 */
	public static Document createUpdatePhotoReply(String status) {
		Document responseDoc = XMLHelper.createMessageDocument("UpdatePhotoReply");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();
		payload.setTextContent(status);
		return responseDoc;
	}

	/**
	 * Creates an UpdateProfileReply document with a status text.
	 *
	 * @param status the profile update result status ("Accepted", "Rejected")
	 * @return a Document representing <UpdateProfileReply>
	 */
	public static Document createUpdateProfileReply(String status) {
		Document responseDoc = XMLHelper.createMessageDocument("UpdateProfileReply");
		Element payload = (Element) responseDoc.getDocumentElement().getFirstChild();
		payload.setTextContent(status);
		return responseDoc;
	}
}
