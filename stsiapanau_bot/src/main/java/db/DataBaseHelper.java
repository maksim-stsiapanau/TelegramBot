package db;

import static ru.max.bot.BotHelper.adaEvents;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

import rent.PrimaryRatesHolder;
import rent.RentHolder;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.DeleteResult;

/**
 * Database operations
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class DataBaseHelper {

	private final SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy");
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
	 * Return rent rates
	 * 
	 * @return message with rates
	 */
	public String getRates(String chatId) {

		StringBuilder sb = new StringBuilder();

		try {
			Optional<Document> document = Optional.of(this.db
					.getCollection("rent_const")
					.find(Filters.eq("id_chat", chatId)).first());
			document.ifPresent(doc -> {
				sb.append("Rates:\n").append("Rent amount: ")
						.append(doc.get("rent_amount")).append(" rub.")
						.append("\nT1: ").append(doc.get("t1_rate"))
						.append(" rub.").append("\nT2: ")
						.append(doc.get("t2_rate")).append(" rub.")
						.append("\nT3: ").append(doc.get("t3_rate"))
						.append(" rub.").append("\nHot water: ")
						.append(doc.get("hw_rate")).append(" rub.")
						.append("\nCold water: ").append(doc.get("cw_rate"))
						.append(" rub.").append("\nOutfall: ")
						.append(doc.get("outfall_rate")).append(" rub.");
			});
		} catch (Exception e) {
			logger.error("Can't get rates month! Error: %s", e.getMessage(), e);
		}
		return sb.toString();
	}

	/**
	 * 
	 * Return statistic for require month
	 * 
	 * @param month
	 *            - rent month
	 * @return - String message
	 */
	public String getStatByMonth(String month, String idChat) {

		StringBuilder sb = new StringBuilder();

		try {
			Optional<Document> document = Optional.ofNullable(DataBaseHelper
					.getInstance().getFirstDocByFilter(
							"rent_stat",
							Filters.and(Filters.eq("month", month),
									Filters.eq("id_chat", idChat))));
			document.ifPresent(doc -> {
				sb.append("Added by: ")
						.append(doc.get("who_set"))
						.append("\nMonth: ")
						.append(doc.get("month"))
						.append("\nLight\n")
						.append("T1 indication: ")
						.append(doc.get("t1_indication"))
						.append("; used: ")
						.append(doc.get("t1_used"))
						.append("; price: ")
						.append(String.format("%.2f", doc.get("t1_price")))
						.append(" rub; rate: ")
						.append(doc.get("t1_rate"))
						.append(" rub.")
						.append("\nT2 indication: ")
						.append(doc.get("t2_indication"))
						.append("; used: ")
						.append(doc.get("t2_used"))
						.append("; price: ")
						.append(String.format("%.2f", doc.get("t2_price")))
						.append(" rub; rate: ")
						.append(doc.get("t2_rate"))
						.append(" rub.")
						.append("\nT3 indication: ")
						.append(doc.get("t3_indication"))
						.append("; used: ")
						.append(doc.get("t3_used"))
						.append("; price: ")
						.append(String.format("%.2f", doc.get("t3_price")))
						.append(" rub; rate: ")
						.append(doc.get("t3_rate"))
						.append(" rub.")
						.append("\n\nWater\n")
						.append("Hot water indication: ")
						.append(doc.get("hot_water_indication"))
						.append("; used: ")
						.append(doc.get("hot_water_used"))
						.append("; price: ")
						.append(String.format("%.2f",
								doc.get("hot_water_price")))
						.append(" rub; rate: ")
						.append(doc.get("hot_water_rate"))
						.append(" rub.")
						.append("\nCold water indication: ")
						.append(doc.get("cold_water_indication"))
						.append("; used: ")
						.append(doc.get("cold_water_used"))
						.append("; price: ")
						.append(String.format("%.2f",
								doc.get("cold_water_price")))
						.append(" rub; rate: ")
						.append(doc.get("cold_water_rate"))
						.append(" rub.")
						.append("\nOutfall water indication: ")
						.append(doc.get("outfall_indication"))
						.append("; price: ")
						.append(String.format("%.2f", doc.get("outfall_price")))
						.append(" rub; rate: ").append(doc.get("outfall_rate"))
						.append(" rub.").append("\n\nRent Amount: ")
						.append(doc.get("rent_amount")).append(" rub")
						.append("\nTotal: ")
						.append(String.format("%.2f", doc.get("total_amount")))
						.append(" rub");

				try {
					double takeout = Double.parseDouble(String.valueOf(doc
							.get("takeout")));
					if (takeout > 0) {
						sb.append("\nTakeout: ").append(takeout).append(" rub")
								.append("\nTakeout desc: ")
								.append(doc.get("takeout_desc"));
					}
				} catch (Exception e) {
					logger.error("Can't get takeout! Error: %s",
							e.getMessage(), e);
				}
			});
		} catch (Exception e) {
			logger.error("Can't get stat by month! Error: %s", e.getMessage(),
					e);
			logger.error("Month: %s;", month);
		}
		return sb.toString();
	}

	/**
	 * Get totals amount for rent by months
	 * 
	 * @return String message with history by months
	 */
	public String getHistory(String chatId) {

		StringBuilder sb = new StringBuilder().append("History:\n");

		MongoCursor<Document> iter = null;

		try {
			FindIterable<Document> docs = this.db.getCollection("rent_stat")
					.find(Filters.eq("id_chat", chatId));
			iter = docs.iterator();
			while (iter.hasNext()) {
				Document document = iter.next();
				sb.append("\nMonth: ").append(document.get("month"))
						.append("; Total: ")
						.append(document.get("total_amount")).append(" rub.");
			}
		} catch (Exception e) {
			logger.error("Can't get history! Error: %s", e.getMessage(), e);
		} finally {
			try {
				if (null != iter)
					iter.close();
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
		return sb.toString();
	}

	/**
	 * Delete statistics by month
	 * 
	 * @param idChat
	 *            - unique chat id
	 * @param month
	 *            - month for deleting
	 * @return statuse execute true or false
	 */
	public boolean deleteMothStat(String idChat, String month) {

		boolean status = true;

		try {
			DeleteResult dr = this.db.getCollection("rent_stat").deleteOne(
					Filters.and(Filters.eq("id_chat", idChat),
							Filters.eq("month", month)));
			logger.debug("Month %s deleted successfully! Row deleted %s",
					month, dr.getDeletedCount());
		} catch (Exception e) {
			status = false;
			logger.error("Can't purge statistics! Error: %s", e.getMessage(), e);
		}
		return status;
	}

	/**
	 * Erase all information about rent
	 * 
	 * @param chatId
	 *            - chat it for which execute erase
	 * @return
	 */
	public boolean purgeAll(String chatId) {

		boolean status = true;

		try {
			this.db.getCollection("rent_const").deleteMany(
					Filters.eq("id_chat", chatId));
			logger.debug(
					"Primary values of chat with id %s deleted successfully!",
					chatId);
			DeleteResult dr = this.db.getCollection("rent_stat").deleteMany(
					Filters.eq("id_chat", chatId));
			logger.debug("Months deleted: %s", dr.getDeletedCount());
		} catch (Exception e) {
			status = false;
			logger.error("Can't purge statistics! Error: %s", e.getMessage(), e);
		}
		return status;
	}

	/**
	 * 
	 * Update rates
	 * @param <T>
	 * 
	 * @param idChat
	 *            - unique chat id
	 * @param field
	 *            - field for update
	 * @param value
	 *            - new value
	 * @return status execute true or false
	 */
	public <T> boolean updateRate(String idChat, String field, T value) {

		boolean status = true;

		try {
			this.db.getCollection("rent_const").updateOne(
					this.db.getCollection("rent_const")
							.find(Filters.eq("id_chat", idChat)).first(),
					new Document("$set", new Document(field, value)));
		} catch (Exception e) {
			status = false;
			logger.error("Can't update %s rate! Error: %s", field,
					e.getMessage(), e);
		}

		return status;
	}

	/**
	 * Initialization primary indications like light,water and rates for it
	 * 
	 * @param r
	 *            - Rent instance
	 * @return boolean status execute
	 */
	public boolean insertPrimaryCounters(PrimaryRatesHolder r) {

		boolean status = true;

		try {
			Optional<Document> rates = Optional.ofNullable(this.db
					.getCollection("rent_const")
					.find(Filters.eq("id_chat", r.getIdChat())).first());
			if (rates.isPresent()) {
				DeleteResult dr = this.db.getCollection("rent_const")
						.deleteMany(Filters.eq("id_chat", r.getIdChat()));
				logger.debug("Count deleted rows for chat with id %s: %s",
						r.getIdChat(), dr.getDeletedCount());
			} else {
				logger.debug("New client!");
			}
		} catch (Exception e) {
			status = false;
			logger.error("Can't delete primaty indications! Error: %s",
					e.getMessage(), e);
		}

		if (status) {
			try {
				this.db.getCollection("rent_const").insertOne(
						new Document("rent_amount", r.getRentAmount())
								.append("t1_start", r.getT1Start())
								.append("t2_start", r.getT2Start())
								.append("t3_start", r.getT3Start())
								.append("t1_last", r.getT1Start())
								.append("t2_last", r.getT2Start())
								.append("t3_last", r.getT3Start())
								.append("t1_rate", r.getT1Rate())
								.append("t2_rate", r.getT2Rate())
								.append("t3_rate", r.getT3Rate())
								.append("hw_start", r.getHwStart())
								.append("cw_start", r.getCwStart())
								.append("hw_last", r.getHwStart())
								.append("cw_last", r.getCwStart())
								.append("hw_rate", r.getHwRate())
								.append("cw_rate", r.getCwRate())
								.append("outfall_rate", r.getOutFallRate())
								.append("id_chat", r.getIdChat())
								.append("owner", r.getOwner()));
			} catch (Exception e) {
				status = false;
				logger.error("Can't insert primary values! Error: %s",
						e.getMessage(), e);
			}
		}
		return status;
	}

	/**
	 * 
	 * Save statistic of month
	 * 
	 * @param rh
	 *            RentHolder instance
	 * @return boolean status execute
	 */
	public boolean insertMonthStat(RentHolder rh) {

		boolean status = true;

		try {
			this.db.getCollection("rent_stat").insertOne(
					new Document("month", rh.getMonthRent())
							.append("t1_indication", rh.getCountT1())
							.append("t1_rate", rh.getT1Rate())
							.append("t2_indication", rh.getCountT2())
							.append("t2_rate", rh.getT2Rate())
							.append("t3_indication", rh.getCountT3())
							.append("t3_rate", rh.getT3Rate())
							.append("t1_used", rh.getUsedT1())
							.append("t2_used", rh.getUsedT2())
							.append("t3_used", rh.getUsedT3())
							.append("t1_price", rh.getPriceT1())
							.append("t2_price", rh.getPriceT2())
							.append("t3_price", rh.getPriceT3())
							.append("hot_water_indication",
									rh.getCountHotWater())
							.append("hot_water_rate", rh.getHotWaterRate())
							.append("hot_water_used", rh.getUsedHotWater())
							.append("hot_water_price", rh.getPriceHotWater())
							.append("cold_water_indication",
									rh.getCountColdWater())
							.append("cold_water_rate", rh.getColdWaterRate())
							.append("cold_water_used", rh.getUsedColdWater())
							.append("cold_water_price", rh.getPriceColdWater())
							.append("outfall_indication", rh.getCountOutFall())
							.append("outfall_rate", rh.getOutFallRate())
							.append("outfall_price", rh.getPriceOutFall())
							.append("total_amount", rh.getTotal(null))
							.append("rent_amount", rh.getRentAmount())
							.append("takeout", rh.getTakeout())
							.append("takeout_desc", rh.getTakeoutDesc())
							.append("id_chat", rh.getIdChat())
							.append("who_set", rh.getOwner()));
		} catch (Exception e) {
			status = false;
			logger.error("Can't insert month stat! Error: %s", e.getMessage(),
					e);
		}

		try {
			this.db.getCollection("rent_const").updateOne(
					this.db.getCollection("rent_const")
							.find(Filters.eq("id_chat", rh.getIdChat()))
							.first(),
					new Document("$set", new Document("t1_last", rh
							.getCountT1()).append("t2_last", rh.getCountT2())
							.append("t3_last", rh.getCountT3())
							.append("hw_last", rh.getCountHotWater())
							.append("cw_last", rh.getCountColdWater())));
		} catch (Exception e) {
			status = false;
			logger.error("Can't update last indications! Error: %s",
					e.getMessage(), e);
		}
		return status;
	}

	/**
	 * 
	 * Get all Ada's event
	 * 
	 * @return String message with all Ada's event
	 */
	public String getAllEvents() {

		// date of bithday
		Calendar cal = Calendar.getInstance();

		cal.set(2015, 8, 14);

		StringBuilder result = new StringBuilder();

		MongoCursor<Document> iterator = null;

		try {
			FindIterable<Document> iter = this.db.getCollection("ada_events")
					.find().sort(Filters.eq("event_date", 1));
			iterator = iter.iterator();
			while (iterator.hasNext()) {
				Document doc = iterator.next();
				Date event = new Date(doc.getLong("event_date"));
				long timeEvent = event.getTime();
				long diff = timeEvent - cal.getTime().getTime();
				long days = diff / (24 * 60 * 60 * 1000);
				String eventDate = this.sdf.format(event);
				result.append("\nEvent desc: ").append(doc.get("event_name"))
						.append("\nDate: ").append(eventDate).append("\nAge: ")
						.append(days / 365).append(" year ").append(days / 30)
						.append(" month  ").append(days % 30).append(" days")
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
