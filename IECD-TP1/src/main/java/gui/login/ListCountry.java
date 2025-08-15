package gui.login;

import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * Provides country code and name mapping functionality for
 * internationalization.
 * 
 * <p>
 * This class maintains two-way mappings between country names and their ISO
 * codes, primarily used for locale-sensitive operations like country selection
 * in UI components.
 * 
 * <p>
 * Key features:
 * <ul>
 * <li>Auto-populates country data from Java's Locale system
 * <li>Provides sorted maps (TreeMap) for consistent ordering
 * <li>Supports both country name to code and code to name lookups
 * <li>Includes legacy language mapping capability
 * </ul>
 * 
 * <p>
 * Note: Originally adapted from mkyong.com's country list example.
 */

public class ListCountry {

	/*
	 * Class obtained and modified from the website
	 * https://mkyong.com/java/display-a-list-of-countries-in-java/
	 */

	/**
	 * Main method for testing - prints all country name/code pairs.
	 * 
	 * @param args Command line arguments (unused)
	 */
	public static void main(String[] args) {
		ListCountry obj = new ListCountry();
		Map<String, String> countryCodes = obj.getCountryCodeMap();
		for (Map.Entry<String, String> entry : countryCodes.entrySet()) {
			System.out.println("Country Name = " + entry.getKey() + ", Code = " + entry.getValue());
		}
	}
	private Map<String, String> countryCodeMap = new TreeMap<>(); // Mapa para armazenar nome -> código

	private Map<String, String> countryNameMap = new TreeMap<>(); // Mapa para armazenar código -> nome (mantendo para o
																	// ComboBox)

	// create Map with country code and languages (keeping for reference)
	private Map<String, String> languagesMap = new TreeMap<>();

	/**
	 * Constructs a new ListCountry instance and initializes country mappings.
	 */
	public ListCountry() {
		initCountryMaps();
	}

	/**
	 * Gets the country name to ISO code mapping.
	 * 
	 * @return Map where keys are country names and values are ISO codes
	 */
	public Map<String, String> getCountryCodeMap() {
		return countryCodeMap;
	}

	/**
	 * Gets the ISO code to country name mapping.
	 * 
	 * @return Map where keys are ISO codes and values are country names
	 */
	public Map<String, String> getCountryNameMap() {
		return countryNameMap;
	}

	/**
	 * Legacy method that prints detailed country/language information. Maintained
	 * for reference purposes only.
	 */

	@SuppressWarnings("deprecation")
	public void getListOfCountries() { // keeping for reference
		String[] countries = Locale.getISOCountries();
		int supportedLocale = 0, nonSupportedLocale = 0;
		for (String countryCode : countries) {
			Locale obj = null;
			if (countryNameMap.get(countryCode) == null) {
				obj = new Locale("", countryCode);
				nonSupportedLocale++;
			} else {
				obj = new Locale(languagesMap.get(countryCode), countryCode);
				supportedLocale++;
			}
			System.out.println("Country Code = " + obj.getCountry() + ", Country Name = " + obj.getDisplayCountry(obj)
					+ ", Languages = " + obj.getDisplayLanguage());
		}
		System.out.println("nonSupportedLocale : " + nonSupportedLocale);
		System.out.println("supportedLocale : " + supportedLocale);
	}

	/**
	 * Initializes country code and name mappings using Java's Locale system.
	 * Populates both countryCodeMap (name->code) and countryNameMap (code->name).
	 * Only includes countries with non-empty display names.
	 */
	public void initCountryMaps() {
		String[] countryCodes = Locale.getISOCountries();
		for (String countryCode : countryCodes) {
			@SuppressWarnings("deprecation")
			Locale locale = new Locale("", countryCode);
			String countryName = locale.getDisplayCountry(locale);
			if (!countryName.isEmpty()) {
				countryCodeMap.put(countryName, countryCode);
				countryNameMap.put(countryCode, countryName); // Keeping code map -> ComboBox name
			}
		}
	}

	/**
	 * Initializes language mapping by country code. Legacy method maintained for
	 * reference purposes.
	 */

	public void initLanguageMap() {
		Locale[] locales = Locale.getAvailableLocales();
		for (Locale obj : locales) {
			if ((obj.getDisplayCountry() != null) && (!"".equals(obj.getDisplayCountry()))) {
				languagesMap.put(obj.getCountry(), obj.getLanguage());
			}
		}
	}
}