package ru.max.bot;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.HttpsURLConnection;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rent.RentHolder;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Useful methods
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class BotHelper {

	private static final Logger logger = LogManager.getLogger(BotHelper.class
			.getName());
	public static final ObjectMapper objectMapper = new ObjectMapper();
	public static ConcurrentMap<String, Optional<RentHolder>> rentData = new ConcurrentHashMap<>();
	public static ConcurrentMap<String, CommandHolder> activeCommand = new ConcurrentHashMap<>();
	public static ConcurrentMap<String, String> commandMapper = new ConcurrentHashMap<>();
	public static ConcurrentMap<String, String> commandMapperRus = new ConcurrentHashMap<>();
	public static ConcurrentMap<String, Object> chatObjectMapper = new ConcurrentHashMap<>();
	public static ConcurrentMap<String, List<List<String>>> cacheButtons = new ConcurrentHashMap<>();

	static {
		commandMapper.put("set light", "/setprimarylight");
		commandMapper.put("set water", "/setprimarywater");
		commandMapper.put("set rent amount", "/setprimaryrentamount");
		commandMapper.put("rent", "/rent");
		commandMapper.put("back to rent menu", "/rent");
		commandMapper.put("calc", "/calc");
		commandMapper.put("home", "/start");
		commandMapper.put("payments", "/gethistory");
		commandMapper.put("rates", "/getrates");
		commandMapper.put("change rates", "/changerates");
		commandMapper.put("add month", "/rent_add");
		commandMapper.put("new primary", "/setprimarycounters");
		commandMapper.put("name of month", "/setmonth");
		commandMapper.put("light", "/setlight");
		commandMapper.put("water", "/setwater");
		commandMapper.put("takeout", "/settakeout");
		commandMapper.put("details", "/getstatbymonth");
		commandMapper.put("remove rent", "/purge");
		commandMapper.put("remove payment", "/delmonthstat");
		commandMapper.put("current statistic", "/getstat");
		commandMapper.put("hot water", "/changehotwaterrate");
		commandMapper.put("cold water", "/changecoldwaterrate");
		commandMapper.put("outfall", "/changeoutfallrate");
		commandMapper.put("light rate", "/changelightrate");
		commandMapper.put("rent amount", "/changerentamount");

		// russian language
		commandMapperRus.put("свет", "/setprimarylight");
		commandMapperRus.put("вода", "/setprimarywater");
		commandMapperRus.put("сумма аренда", "/setprimaryrentamount");
		commandMapperRus.put("аренда", "/rent");
		commandMapperRus.put("назад в основное меню", "/rent");
		commandMapperRus.put("рассчитать", "/calc");
		commandMapperRus.put("домой", "/start");
		commandMapperRus.put("платежи", "/gethistory");
		commandMapperRus.put("тарифы", "/getrates");
		commandMapperRus.put("изменить тарифы", "/changerates");
		commandMapperRus.put("добавить месяц", "/rent_add");
		commandMapperRus.put("начальные показания", "/setprimarycounters");
		commandMapperRus.put("месяц", "/setmonth");
		commandMapperRus.put("показания света", "/setlight");
		commandMapperRus.put("показания воды", "/setwater");
		commandMapperRus.put("вычет", "/settakeout");
		commandMapperRus.put("статистика", "/getstatbymonth");
		commandMapperRus.put("удалить все", "/purge");
		commandMapperRus.put("удалить платеж", "/delmonthstat");
		commandMapperRus.put("инфо", "/getstat");
		commandMapperRus.put("горячая вода", "/changehotwaterrate");
		commandMapperRus.put("холодная вода", "/changecoldwaterrate");
		commandMapperRus.put("водоотвод", "/changeoutfallrate");
		commandMapperRus.put("тариф на свет", "/changelightrate");
		commandMapperRus.put("сумма аренды", "/changerentamount");

	}

	public BotHelper() {

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

	public static String getEmoji(String hexCode) {
		String done = null;
		try {
			done = new String(Hex.decodeHex(hexCode.toCharArray()), "UTF-8");
		} catch (UnsupportedEncodingException | DecoderException e) {
			logger.error(e.getMessage(), e);
		}
		return done;
	}

}
