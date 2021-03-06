package ru.max.bot;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import ru.max.bot.database.DataBaseHelper;

/**
 * Telegram bot
 *
 */
public class Main {

	private static final Logger logger = LogManager.getLogger(Main.class
			.getName());

	public static void main(String[] args) {

		StringBuilder sb = new StringBuilder();

		DataBaseHelper.getInstance().getToken().ifPresent(e -> {
			sb.append("https://api.telegram.org/bot").append(e).append("/");
		});

		String telegramApiUrl = sb.toString();

		if (telegramApiUrl.length() == 0) {
			logger.error("Telegram api url doesn't set! Start is failed!");
			System.exit(0);

		}

		ScheduledExecutorService scheduleEx = Executors
				.newSingleThreadScheduledExecutor();
		scheduleEx.scheduleAtFixedRate(new MessagesChecker(telegramApiUrl), 0,
				1000, TimeUnit.MILLISECONDS);
	}
}
