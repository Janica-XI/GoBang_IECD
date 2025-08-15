package communication.client;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import communication.XMLHelper;
import core.GameState;
import core.Player;

/**
 * ClientProtocolFactory provides static methods to create XML request documents
 * and parse certain replies for client-side operations in the Gobang game.
 */
public class ClientProtocolFactory {

	/**
	 * Creates a ChallengeReply document indicating acceptance or rejection.
	 *
	 * @param status the challenge status (e.g. "Accepted", "Rejected")
	 * @return a Document representing <ChallengeReply>
	 */
	public static Document createChallengeReply(String status) {
		Document requestDoc = XMLHelper.createMessageDocument("ChallengeReply");
		Element payload = (Element) requestDoc.getDocumentElement().getFirstChild();
		payload.setTextContent(status);
		return requestDoc;
	}

	/**
	 * Creates a ChallengeRequest document to challenge another player.
	 *
	 * @param challengerUsername the user issuing the challenge
	 * @param opponentUsername   the user being challenged
	 * @return a Document representing <ChallengeRequest>
	 */
	public static Document createChallengeRequest(String opponentUsername) {
		Document requestDoc = XMLHelper.createMessageDocument("ChallengeRequest");
		Element payload = (Element) requestDoc.getDocumentElement().getFirstChild();
		XMLHelper.appendTextElement(requestDoc, payload, "Opponent", opponentUsername);
		return requestDoc;
	}

	/**
	 * Creates a ForfeitMatchRequest document to forfeit an ongoing game.
	 *
	 * @param gameId the unique identifier for the game to forfeit
	 * @return a Document representing <ForfeitMatchRequest>
	 */
	public static Document createForfeitMatchRequest(String gameId) {
		Document requestDoc = XMLHelper.createMessageDocument("ForfeitMatchRequest");
		Element payload = (Element) requestDoc.getDocumentElement().getFirstChild();
		XMLHelper.appendTextElement(requestDoc, payload, "GameId", gameId);
		return requestDoc;
	}

	/**
	 * Creates a LeaderboardRequest document with no payload.
	 * 
	 * @return a Document representing <LeaderboardRequest>
	 */
	public static Document createLeaderboardRequest() {
		return XMLHelper.createMessageDocument("LeaderboardRequest");
	}

	/**
	 * Creates a ListPlayersRequest document with no payload.
	 *
	 * @return a Document representing <ListPlayersRequest>
	 */
	public static Document createListPlayersRequest() {
		return XMLHelper.createMessageDocument("ListPlayersRequest");
	}

	/**
	 * Creates a LoginRequest document with username and password.
	 *
	 * @param username the user name for login
	 * @param password the password for login
	 * @return a Document representing <LoginRequest>
	 */
	public static Document createLoginRequest(String username, String password) {
		Document requestDoc = XMLHelper.createMessageDocument("LoginRequest");
		Element payload = (Element) requestDoc.getDocumentElement().getFirstChild();
		XMLHelper.appendTextElement(requestDoc, payload, "Username", username);
		XMLHelper.appendTextElement(requestDoc, payload, "Password", password);
		return requestDoc;
	}

	/**
	 * Creates a LogoutRequest document.
	 *
	 * @return a Document representing <LogoutRequest/>
	 */
	public static Document createLogoutRequest() {
		Document requestDoc = XMLHelper.createMessageDocument("LogoutRequest");
		return requestDoc;
	}

	/**
	 * Creates a MoveRequest document with game ID, coordinates, and color.
	 *
	 * @param gameId the unique identifier for the game
	 * @param row    the row coordinate of the move
	 * @param col    the column coordinate of the move
	 * @return a Document representing <MoveRequest>
	 */
	public static Document createMoveRequest(String gameId, int row, int col) {
		Document requestDoc = XMLHelper.createMessageDocument("MoveRequest");
		Element payload = (Element) requestDoc.getDocumentElement().getFirstChild();
		XMLHelper.appendTextElement(requestDoc, payload, "GameId", gameId);
		XMLHelper.appendTextElement(requestDoc, payload, "Row", Integer.toString(row));
		XMLHelper.appendTextElement(requestDoc, payload, "Col", Integer.toString(col));
		return requestDoc;
	}

	/**
	 * Creates a ReadyRequest document indicating availability.
	 *
	 * @param isReady true if the user is ready to play, false otherwise
	 * @return a Document representing <ReadyRequest>true|false</ReadyRequest>
	 */
	public static Document createReadyRequest(boolean isReady) {
		Document requestDoc = XMLHelper.createMessageDocument("ReadyRequest");
		Element payload = (Element) requestDoc.getDocumentElement().getFirstChild();
		payload.setTextContent(Boolean.toString(isReady));
		return requestDoc;
	}

	/**
	 * Creates a RegisterRequest document with user details.
	 *
	 * @param username    the desired user name
	 * @param password    the desired password
	 * @param nationality the user's nationality code
	 * @param dateOfBirth the user's date of birth
	 * @return a Document representing <RegisterRequest>
	 */
	public static Document createRegisterRequest(String username, String password, String nationality,
			String dateOfBirth) {
		Document requestDoc = XMLHelper.createMessageDocument("RegisterRequest");
		Element payload = (Element) requestDoc.getDocumentElement().getFirstChild();
		XMLHelper.appendTextElement(requestDoc, payload, "Username", username);
		XMLHelper.appendTextElement(requestDoc, payload, "Password", password);
		XMLHelper.appendTextElement(requestDoc, payload, "Nationality", nationality);
		XMLHelper.appendTextElement(requestDoc, payload, "DateOfBirth", dateOfBirth.toString());
		return requestDoc;
	}

	/**
	 * Creates an UpdatePhotoRequest document containing Base64 photo data.
	 *
	 * @param photoBase64 the new photo encoded in Base64
	 * @return a Document representing <UpdatePhotoRequest>
	 */
	public static Document createUpdatePhotoRequest(String photoBase64) {
		Document requestDoc = XMLHelper.createMessageDocument("UpdatePhotoRequest");
		Element payload = (Element) requestDoc.getDocumentElement().getFirstChild();
		XMLHelper.appendTextElement(requestDoc, payload, "Photo", photoBase64);
		return requestDoc;
	}

	/**
	 * Creates an UpdateProfileRequest document with optional profile fields.
	 *
	 * @param newPassword    the new password, or null if unchanged
	 * @param newNationality the new nationality code, or null if unchanged
	 * @param newDateOfBirth the new date of birth, or null if unchanged
	 * @return a Document representing <UpdateProfileRequest>
	 */
	public static Document createUpdateProfileRequest(String newPassword, String newNationality, String newDateOfBirth,
			String newTheme) {
		Document requestDoc = XMLHelper.createMessageDocument("UpdateProfileRequest");
		Element payload = (Element) requestDoc.getDocumentElement().getFirstChild();
		if (newPassword != null) {
			XMLHelper.appendTextElement(requestDoc, payload, "Password", newPassword);
		}
		if (newNationality != null) {
			XMLHelper.appendTextElement(requestDoc, payload, "Nationality", newNationality);
		}
		if (newDateOfBirth != null) {
			XMLHelper.appendTextElement(requestDoc, payload, "DateOfBirth", newDateOfBirth.toString());
		}
		if (newTheme != null) {
			XMLHelper.appendTextElement(requestDoc, payload, "Theme", newTheme);
		}
		return requestDoc;
	}

	/**
	 * Parses a GameState Element into a GameState object.
	 *
	 * @param payload the <GameState> element
	 * @return a GameState POJO containing boardRows and nextPlayerColor
	 */
	public static GameState parseGameState(Element payload) {
		String gameId = XMLHelper.getElementTextContent(payload, "GameId");
		NodeList rowNodes = payload.getElementsByTagName("Row");
		List<String> boardRows = new ArrayList<>();
		for (int i = 0; i < rowNodes.getLength(); i++) {
			Element rowElem = (Element) rowNodes.item(i);
			boardRows.add(rowElem.getTextContent());
		}
		String nextPlayerColor = XMLHelper.getElementTextContent(payload, "NextPlayer");
		String blackTimeText = XMLHelper.getElementTextContent(payload, "BlackTimeRemaining");
		Duration blackTimeRemaining = Duration.parse(blackTimeText);

		String whiteTimeText = XMLHelper.getElementTextContent(payload, "WhiteTimeRemaining");
		Duration whiteTimeRemaining = Duration.parse(whiteTimeText);

		String timestampText = XMLHelper.getElementTextContent(payload, "Timestamp");
		Instant timestamp = Instant.parse(timestampText);
		return new GameState(gameId, boardRows, nextPlayerColor, blackTimeRemaining, whiteTimeRemaining, timestamp);
	}

	/**
	 * Parses a Player object from its XML representation.
	 *
	 * @param payloadElement the XML element containing the player's data
	 * @return a Player populated with username, nationality, date of birth, photo
	 *         data (if present), victories, defeats, and total time
	 */
	public static Player parsePlayer(Element payloadElement) {
		Player player = new Player();
		player.setUsername(XMLHelper.getElementTextContent(payloadElement, "Username"));
		player.setNationality(XMLHelper.getElementTextContent(payloadElement, "Nationality"));
		LocalDate dateOfBirth = LocalDate.parse(XMLHelper.getElementTextContent(payloadElement, "DateOfBirth"));
		player.setDateOfBirth(dateOfBirth);
		String photoData = XMLHelper.getElementTextContent(payloadElement, "Photo");
		player.setPhotoBase64(photoData);
		int victories = Integer.parseInt(XMLHelper.getElementTextContent(payloadElement, "Victories"));
		player.setVictories(victories);
		int defeats = Integer.parseInt(XMLHelper.getElementTextContent(payloadElement, "Defeats"));
		player.setDefeats(defeats);
		Duration totalTime = Duration.parse(XMLHelper.getElementTextContent(payloadElement, "TotalTime"));
		player.setTotalTime(totalTime);
		String theme = XMLHelper.getElementTextContent(payloadElement, "Theme");
		if (theme != null) {
			player.setTheme(theme);
		} else {
			player.setTheme("default"); // Valor padrão para compatibilidade
		}
		return player;
	}

	/**
	 * Parses a list of Player objects from a <ListPlayersReply> payload element.
	 *
	 * @param payloadElement the <ListPlayersReply> element
	 * @return a List of Player instances extracted from the payload
	 */
	public static List<Player> parsePlayers(Element payloadElement) {
		List<Player> players = new ArrayList<>();
		NodeList nodeList = payloadElement.getElementsByTagName("Player");
		for (int i = 0; i < nodeList.getLength(); i++) {
			Element playerElem = (Element) nodeList.item(i);
			Player player = parsePlayer(playerElem);
			players.add(player);
		}
		return players;
	}
}