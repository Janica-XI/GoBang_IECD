package core.client;

import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import core.MyBase64;

/**
 * Provides utility methods for client-side operations including: - Image
 * handling (loading, encoding/decoding Base64, scaling) - Default resource
 * management - Status code to user-friendly message conversion
 * 
 * This class contains static helper methods for common client operations,
 * particularly focused on image processing and user feedback presentation.
 * 
 * Key features: - Base64 image encoding/decoding - Image scaling with aspect
 * ratio preservation - Fallback to default images when needed - Localized
 * status message conversion (Portuguese)
 */

public class ClientUtils {
	/**
	 * Decodes a Base64 string into an image and scales it to the specified size.
	 * 
	 * @param b64  The Base64 encoded image string.
	 * @param size The desired height of the scaled image.
	 * @return An ImageIcon of the scaled image.
	 * @throws UncheckedIOException If an IOException occurs during decoding or
	 *                              reading the image.
	 */
	public static ImageIcon decodeAndScaleBase64(String b64, int targetHeight) {
		try {
			byte[] bytes = MyBase64.decode(b64);
			BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));

			int originalWidth = img.getWidth();
			int originalHeight = img.getHeight();

			// calculate new width to maintain aspect ratio
			int newWidth = (targetHeight * originalWidth) / originalHeight;

			Image scaled = img.getScaledInstance(newWidth, targetHeight, Image.SCALE_SMOOTH);
			return new ImageIcon(scaled);
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Formats a Duration into a human-readable time string (MM:SS).
	 * 
	 * @param duration the Duration to format
	 * @return formatted string like "04:35" or "00:00" if null/negative
	 */
	public static String formatTimeRemaining(Duration duration) {
		if (duration == null || duration.isNegative() || duration.isZero()) {
			return "00:00";
		}

		long totalSeconds = duration.getSeconds();
		long minutes = totalSeconds / 60;
		long seconds = totalSeconds % 60;

		return String.format("%02d:%02d", minutes, seconds);
	}

	/**
	 * Converts a status code into a user-friendly message in Portuguese.
	 * 
	 * @param statusCode The status code received from the server or system
	 * @return A friendly Portuguese message corresponding to the status code, or
	 *         the original status code if no mapping exists
	 * 
	 * @see <List of related classes or methods if applicable>
	 * 
	 *      Status code mappings: - "Accepted" → "Aceitado" - "UsernameUnknown" →
	 *      "Nome de utilizador desconhecido" - "WrongPassword" →
	 *      "Palavra-passe incorreta" - "UsernameDuplicated" →
	 *      "Nome de utilizador já existe" - "UsernameInvalid" →
	 *      "Nome de utilizador inválido" - "PasswordInvalid" →
	 *      "Palavra-passe inválida" - "NationalityInvalid" →
	 *      "Nacionalidade inválida" - "DateInvalid" → "Data inválida" - "Rejected"
	 *      → "Rejeitado" - "InvalidMove" → "Jogada inválida" - "NotYourTurn" →
	 *      "Não é a sua vez de jogar" - "GameNotFound" → "Jogo não encontrado" -
	 *      "Timeout" → "Tempo esgotado"
	 */
	public static String getFriendlyStatusMessage(String statusCode) {
		switch (statusCode) {
		case "Accepted":
			return "Aceitado";
		case "UsernameUnknown":
			return "Nome de utilizador desconhecido";
		case "WrongPassword":
			return "Palavra-passe incorreta";
		case "UsernameDuplicated":
			return "Nome de utilizador já existe";
		case "UsernameInvalid":
			return "Nome de utilizador inválido";
		case "PasswordInvalid":
			return "Palavra-passe inválida";
		case "NationalityInvalid":
			return "Nacionalidade inválida";
		case "DateInvalid":
			return "Data inválida";
		case "Rejected":
			return "Rejeitado";
		case "InvalidMove":
			return "Jogada inválida";
		case "NotYourTurn":
			return "Não é a sua vez de jogar";
		case "GameNotFound":
			return "Jogo não encontrado";
		case "Timeout":
			return "Tempo esgotado";
		default:
			return statusCode; // Return the original if no mapping is found
		}
	}

	/**
	 * Tries to decode & scale from Base-64. If that fails or is empty, falls back
	 * to the default resource.
	 * 
	 * @param base64 The Base64 encoded image string.
	 * @param size   The desired width and height of the scaled image.
	 * @return An ImageIcon of the scaled image or the default image if decoding
	 *         fails or the Base64 string is empty.
	 */
	public static ImageIcon loadAndScaleIconOrDefault(String base64, int size) {
		if (base64 != null) {
			return decodeAndScaleBase64(base64, size);
		}
		return loadDefaultScaledIcon(size);
	}

	/** Loads & scales /assets/default.jpg to the specified size. */
	private static ImageIcon loadDefaultScaledIcon(int size) {
		URL def = ClientUtils.class.getResource("/assets/default.jpg");
		if (def == null) {
			// last-resort: blank
			BufferedImage blank = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
			return new ImageIcon(blank);
		}
		ImageIcon raw = new ImageIcon(def);
		Image img = raw.getImage().getScaledInstance(size, size, Image.SCALE_SMOOTH);
		return new ImageIcon(img);
	}

	/**
	 * Loads an image from the specified file path and encodes it to Base64.
	 * 
	 * @param filepath The path to the image file.
	 * @return The Base64 encoded string of the image, or an empty string if an
	 *         error occurs.
	 */
	public static String loadImageAsBase64(String filepath) {
		try {
			byte[] imageBytes = Files.readAllBytes(Paths.get(filepath));
			return MyBase64.encode(imageBytes);
		} catch (IOException e) {
			System.err.println("Error loading image: " + e.getMessage());
			return "";
		}
	}
}