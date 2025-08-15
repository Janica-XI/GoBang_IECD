package core.server;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;

import core.Player;

/**
 * Utility class for loading and saving player data from/to a file.
 */
public class PlayerManager {
	public final static String PLAYERS_FILE = "players.dat";

	/**
	 * Loads the list of players from a serialized file. If the file does not exist
	 * or cannot be read, returns an empty list.
	 *
	 * @param filename the name of the file to read
	 * @return the list of players, or empty list if file is missing or invalid
	 */
	@SuppressWarnings("unchecked")
	public static List<Player> loadPlayers(String filename) {
		try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename))) {
			return (List<Player>) ois.readObject();
		} catch (Exception e) {
			System.out.println("No existing player database found. Creating a new database.");
			return new ArrayList<>();
		}
	}

	/**
	 * Saves the given list of players to a serialized file.
	 *
	 * @param players  the list of players to save
	 * @param filename the name of the file to write
	 */
	public static void savePlayers(List<Player> players, String filename) {
		try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename))) {
			oos.writeObject(players);
			if (ServerConfig.DEBUG_MODE) {
				System.out.println("Player database saved.");
			}
		} catch (IOException e) {
			System.err.println("Error saving player database: " + e.getMessage());
		}
	}
}
