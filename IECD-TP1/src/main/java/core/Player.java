package core;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a player in the Gobang protocol, corresponding to PlayerType in
 * the XSD. Stores credentials, profile information and game statistics.
 */
public class Player implements Serializable {

	private static final long serialVersionUID = 1L;

	/** The player's unique username. */
	private String username;

	/** The player's account password. */
	private String password;

	/** The player's nationality code (3-letter ISO). */
	private String nationality;

	/** The player's date of birth. */
	private LocalDate dateOfBirth;

	/** The player's profile photo encoded in Base64 (optional). */
	private String photoBase64;

	/** The total number of victories. */
	private int victories;

	/** The total number of defeats. */
	private int defeats;

	/** The total accumulated game time. */
	private Duration totalTime;

	/**
	 * Game theme for each player
	 */
	private String theme = "default"; // Default theme

	/**
	 * Default constructor for Player. Initializes statistics to zero and leaves
	 * optional fields null.
	 */
	public Player() {
		this.username = null;
		this.password = null;
		this.nationality = null;
		this.dateOfBirth = null;
		this.photoBase64 = null;
		this.victories = 0;
		this.defeats = 0;
		this.totalTime = Duration.ZERO;
	}

	/**
	 * @return the player's date of birth
	 */
	public LocalDate getDateOfBirth() {
		return dateOfBirth;
	}

	/**
	 * @return the total number of defeats
	 */
	public int getDefeats() {
		return defeats;
	}

	/**
	 * @return the player's nationality code
	 */
	public String getNationality() {
		return nationality;
	}

	/**
	 * @return the player's password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * @return the player's profile photo encoded in Base64, or null if none
	 */
	public String getPhotoBase64() {
		return photoBase64;
	}

	public String getTheme() {
		return theme;
	}

	/**
	 * @return the total accumulated game time
	 */
	public Duration getTotalTime() {
		return totalTime;
	}

	/**
	 * @return the player's username
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @return the total number of victories
	 */
	public int getVictories() {
		return victories;
	}

	/**
	 * Sets the player's date of birth.
	 *
	 * @param dateOfBirth the new date of birth
	 */
	public void setDateOfBirth(LocalDate dateOfBirth) {
		this.dateOfBirth = dateOfBirth;
	}

	/**
	 * Sets the total number of defeats.
	 *
	 * @param defeats the new defeat count
	 */
	public void setDefeats(int defeats) {
		this.defeats = defeats;
	}

	/**
	 * Sets the player's nationality code.
	 *
	 * @param nationality the new nationality code
	 */
	public void setNationality(String nationality) {
		this.nationality = nationality;
	}

	/**
	 * Sets the player's account password.
	 *
	 * @param password the new password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * Sets the player's profile photo in Base64 format.
	 *
	 * @param photoBase64 the Base64-encoded photo string
	 */
	public void setPhotoBase64(String photoBase64) {
		this.photoBase64 = photoBase64;
	}

	public void setTheme(String theme) {
		this.theme = theme;
	}

	/**
	 * Sets the total accumulated game time.
	 *
	 * @param totalTime the total Duration of play
	 */
	public void setTotalTime(Duration totalTime) {
		this.totalTime = totalTime;
	}

	/**
	 * Sets the player's username.
	 *
	 * @param username the new username
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Sets the total number of victories.
	 *
	 * @param victories the new victory count
	 */
	public void setVictories(int victories) {
		this.victories = victories;
	}

	@Override
	public String toString() {
		return "Player{" + "username='" + username + '\'' + ", nationality='" + nationality + '\'' + ", dateOfBirth="
				+ dateOfBirth + ", victories=" + victories + ", defeats=" + defeats + ", totalTime=" + totalTime + '}';
	}

	/**
	 * Increments the total number of defeats by a one.
	 *
	 */
	public void updateDefeats() {
		this.defeats++;
	}

	/**
	 * Increments the total accumulated game time by a given duration.
	 *
	 * @param additionalTime the Duration to add to totalTime; must be non-null and
	 *                       non-negative
	 * @throws NullPointerException     if additionalTime is null
	 * @throws IllegalArgumentException if additionalTime is negative
	 */
	public void updateTotalTime(Duration additionalTime) {
		Objects.requireNonNull(additionalTime, "additionalTime must not be null");
		if (additionalTime.isNegative()) {
			throw new IllegalArgumentException("additionalTime must be non-negative");
		}
		this.totalTime = this.totalTime.plus(additionalTime);
	}

	/**
	 * Increments the total number of victories by one.
	 *
	 */
	public void updateVictories() {
		this.victories++;
	}
}
