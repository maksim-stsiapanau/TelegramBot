package db;

import static ru.max.bot.BotHelper.adaEvents;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormat;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.result.DeleteResult;

/**
 * Database operations
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class DataBaseHelper {

	private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
	private final DateTimeFormatter fmt = DateTimeFormat
			.forPattern("dd.MM.yyyy");
	private static final Logger logger = LogManager
			.getFormatterLogger(DataBaseHelper.class.getName());
	private MongoClient mongoCl;
	private MongoDatabase db;

	private DataBaseHelper() {
		this.mongoCl = new MongoClient();
		this.db = this.mongoCl.getDatabase("telegram_bot");
	}

	public static DataBaseHelper getInstance() {
		return LazyDbHolder.instance;
	}

	public void closeMongoClient() {
		this.mongoCl.close();
	}

	@SuppressWarnings("unchecked")
	public <T> T getFirstValue(String collection, String field, Bson bson) {

		T result = null;

		Optional<Document> doc = Optional.ofNullable(this.db
				.getCollection(collection).find(bson).first());

		if (doc.isPresent()) {
			result = (T) doc.get().get(field);
		}
		return result;
	}

	/**
	 * 
	 * Return first object from collection by filter
	 * 
	 * @param collection
	 * @param field
	 * @param value
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public <T> T getFirstDocByFilter(String collection, Bson bson) {

		T result = null;

		Optional<T> doc = (Optional<T>) Optional.ofNullable(this.db
				.getCollection(collection).find(bson).first());
		if (doc.isPresent()) {
			result = (T) doc.get();
		}
		return result;
	}

	/**
	 * 
	 * Get all Ada's events
	 * 
	 * @return String message with all Ada's events
	 */
	public String getAllEvents() {

		// date of bithday
		DateTime dob = new DateTime(2015, 9, 14, 0, 0, 0, 0);
		StringBuilder result = new StringBuilder();
		MongoCursor<Document> iterator = null;

		try {
			FindIterable<Document> iter = this.db.getCollection("ada_events")
					.find().sort(Filters.eq("event_date", 1));
			iterator = iter.iterator();
			while (iterator.hasNext()) {
				Document doc = iterator.next();
				DateTime event = new DateTime(doc.getLong("event_date"));
				Period period = new Period(dob, event);
				result.append("\nEvent desc: ").append(doc.get("event_name"))
						.append("\nDate: ").append(this.fmt.print(event))
						.append("\nAge: ")
						.append(PeriodFormat.getDefault().print(period))
						.append("\n");
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			try {
				if (null != iterator)
					iterator.close();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return result.toString();
	}

	/**
	 * 
	 * Get all dates of Ada's events
	 * 
	 * @return String message with all dates of Ada's events
	 */
	public List<String> getAllEventsDates(String chatId) {

		List<String> dates = new ArrayList<>();
		MongoCursor<Document> iterator = null;

		try {
			FindIterable<Document> iter = this.db.getCollection("ada_events")
					.find(Filters.eq("chat_id", chatId));
			iterator = iter.iterator();
			while (iterator.hasNext()) {
				dates.add(this.fmt.print(new DateTime(iterator.next().getLong(
						"event_date"))));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			try {
				if (null != iterator)
					iterator.close();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return dates;
	}

	/**
	 * 
	 * Remove all events related with Ada
	 * 
	 * 
	 * @return execute status
	 */
	public long purgeAdaEvents() {

		long countDeleted = 0;

		try {
			countDeleted = this.db.getCollection("ada_events")
					.deleteMany(new Document()).getDeletedCount();
		} catch (Exception e) {
			countDeleted = -1;
			logger.error("Can't delete all events! Error: %s", e.getMessage(),
					e);
		}
		return countDeleted;
	}

	/**
	 * 
	 * Remove event by name
	 * 
	 * @param eventDate
	 *            Ada's event
	 * @return
	 */
	public boolean deleteAdaEvent(String eventDate) {

		boolean status = true;

		try {
			this.db.getCollection("ada_events").deleteOne(
					Filters.eq("event_date", eventDate));
		} catch (Exception e) {
			status = false;
			logger.error("Can't delete event for date %s! Error: %s",
					eventDate, e.getMessage(), e);
		}
		return status;
	}

	/**
	 * Saving Ada's event
	 * 
	 * @param eh
	 *            EventHolder instance
	 * @return
	 */
	public Optional<String> saveAdaEvents(String chatId, String author) {

		StringBuilder sb = new StringBuilder();
		Optional<String> eventsMessage;
		ConcurrentLinkedQueue<String> events = adaEvents.get(chatId);
		List<Document> eventList = new ArrayList<>();

		while (!events.isEmpty()) {
			String eventDesc = events.poll();
			String eventDate = events.poll();
			Date d = null;
			long time = 0;

			try {
				d = this.sdf.parse(eventDate);
				time = d.getTime();
			} catch (ParseException e) {
				logger.error(e.getMessage(), e);
			}

			sb.append("\nDesc: ").append(eventDesc).append("\nDate: ")
					.append(eventDate);
			eventList.add(new Document("chat_id", chatId)
					.append("author", author).append("event_name", eventDesc)
					.append("event_date", time));
		}

		try {
			this.db.getCollection("ada_events").insertMany(eventList);
			eventsMessage = Optional.of(sb.toString());
		} catch (Exception e) {
			eventsMessage = Optional.ofNullable(null);
			logger.error("Can't insert Ada's events! Error: %s",
					e.getMessage(), e);
		}
		return eventsMessage;
	}

	/**
	 * Return telegram api token
	 * 
	 * @return token for calling api
	 */
	public Optional<String> getToken() {

		Optional<String> result = Optional.empty();

		try {
			result = Optional.ofNullable(this.db.getCollection("bot_data")
					.find(Filters.eq("type", "telegram_api")).first()
					.getString("token"));
		} catch (Exception e) {
			logger.error("Can't get token! Error: %s", e.getMessage(), e);
		}
		return result;

	}

	private static class LazyDbHolder {

		public static DataBaseHelper instance = new DataBaseHelper();

	}

}
