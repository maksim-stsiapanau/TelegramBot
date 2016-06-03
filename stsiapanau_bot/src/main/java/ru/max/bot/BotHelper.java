package ru.max.bot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rent.RentHolder;

/**
 * Useful methods
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class BotHelper {

	private static final Logger logger = LogManager.getLogger(BotHelper.class
			.getName());

	public static ConcurrentHashMap<String, Optional<RentHolder>> rentData = new ConcurrentHashMap<>();

	public static ConcurrentHashMap<String, Boolean> adaMode = new ConcurrentHashMap<>();

	public static ConcurrentHashMap<String, ConcurrentLinkedQueue<String>> adaEvents = new ConcurrentHashMap<>();

	private BotHelper() {

	}

	public static Optional<String> callApiGet(String method, String url) {

		Optional<String> result = Optional.ofNullable(null);

		BufferedReader in = null;

		URL obj;

		try {

			obj = new URL(url + method);

			HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();

			// optional default is GET
			con.setRequestMethod("GET");

			// add request header
			int responseCode = con.getResponseCode();

			if (responseCode == 200) {

				in = new BufferedReader(new InputStreamReader(
						con.getInputStream()));

				String inputLine;

				StringBuffer response = new StringBuffer();

				while ((inputLine = in.readLine()) != null) {

					response.append(inputLine);

				}

				result = Optional.of(response.toString());

			}

		} catch (Exception e) {

			logger.error(e.getMessage(), e);

		} finally {

			if (null != in) {

				try {

					in.close();

				} catch (IOException e) {

					logger.error(e.getMessage(), e);
				}
			}
		}

		return result;

	}

	/**
	 * Finds value field from xml by regexp no close tag
	 * 
	 * @param name
	 *            the value for search in xml
	 * @return value for requested field
	 * 
	 */
	public static boolean checkStrByRegexp(String text, String reqExp) {

		boolean result = false;

		Pattern p = Pattern.compile(reqExp);

		Matcher m = p.matcher(text);

		if (m.find()) {

			result = true;

		}

		return result;
	}

}
