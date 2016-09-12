package db;

import static ru.max.bot.BotHelper.adaEvents;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;
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

import rent.PrimaryLightHolder;
import rent.PrimaryWaterHolder;
import rent.RentHolder;

import com.fasterxml.jackson.databind.ObjectMapper;
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
	 * Return rent rates
	 * 
	 * @return message with rates
	 */
	public String getRates(String chatId, ObjectMapper mapper) {

		StringBuilder sb = new StringBuilder();

		try {
			Optional<Document> document = Optional.of(this.db
					.getCollection("rent_const")
					.find(Filters.eq("id_chat", chatId)).first());
			document.ifPresent(doc -> {

				sb.append("Rent amount: ").append(doc.get("rent_amount"))
						.append(" rub.").append("\n\nLight");

				try {
					PrimaryLightHolder plh = mapper.readValue(
							(String) doc.get("light"), PrimaryLightHolder.class);

					for (Entry<String, Double> entry : plh.getRates()
							.entrySet()) {
						sb.append("\n").append(entry.getKey()).append(": ")
								.append(entry.getValue()).append(" rub");
					}

					sb.append("\n\nWater\n");

					PrimaryWaterHolder pwh = mapper.readValue(
							(String) doc.get("water"), PrimaryWaterHolder.class);

					sb.append("Hot water: ").append(pwh.getHotWaterRate())
							.append(" rub.").append("\nCold water: ")
							.append(pwh.getColdWaterRate()).append(" rub.")
							.append("\nOutfall: ").append(pwh.getOutfallRate())
							.append(" rub.");

				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}

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

				// prices
				double t1Price = (double) doc.get("t1_price");
				double t2Price = (double) doc.get("t2_price");
				double t3Price = (double) doc.get("t3_price");
				double totalLightPrice = t1Price + t2Price + t3Price;
				double hotWaterPrice = (double) doc.get("hot_water_price");
				double coldWaterPrice = (double) doc.get("cold_water_price");
				double outfallPrice = (double) doc.get("outfall_price");
				double totalWater = hotWaterPrice + coldWaterPrice
						+ outfallPrice;

				sb.append("Added by: ")
						.append(doc.get("who_set"))
						.append("\nMonth: ")
						.append(doc.get("month"))
						.append("\nLight\n")
						.append("T1 indication: ")
						.append(doc.get("t1_indication"))
						.append("; used: ")
						.append(String.format("%.2f", doc.get("t1_used")))
						.append("; price: ")
						.append(String.format("%.2f", t1Price))
						.append(" rub; rate: ")
						.append(doc.get("t1_rate"))
						.append(" rub.")
						.append("\nT2 indication: ")
						.append(doc.get("t2_indication"))
						.append("; used: ")
						.append(String.format("%.2f", doc.get("t2_used")))
						.append("; price: ")
						.append(String.format("%.2f", t2Price))
						.append(" rub; rate: ")
						.append(doc.get("t2_rate"))
						.append(" rub.")
						.append("\nT3 indication: ")
						.append(doc.get("t3_indication"))
						.append("; used: ")
						.append(String.format("%.2f", doc.get("t3_used")))
						.append("; price: ")
						.append(String.format("%.2f", t3Price))
						.append(" rub; rate: ")
						.append(doc.get("t3_rate"))
						.append(" rub.")
						.append("\n\nTotal price for light: ")
						.append(String.format("%.2f", totalLightPrice))
						.append(" rub.")
						.append("\n\nWater\n")
						.append("Hot water indication: ")
						.append(doc.get("hot_water_indication"))
						.append("; used: ")
						.append(String.format("%.2f", doc.get("hot_water_used")))
						.append("; price: ")
						.append(String.format("%.2f", hotWaterPrice))
						.append(" rub; rate: ")
						.append(doc.get("hot_water_rate"))
						.append(" rub.")
						.append("\nCold water indication: ")
						.append(doc.get("cold_water_indication"))
						.append("; used: ")
						.append(String.format("%.2f",
								doc.get("cold_water_used")))
						.append("; price: ")
						.append(String.format("%.2f", coldWaterPrice))
						.append(" rub; rate: ")
						.append(doc.get("cold_water_rate"))
						.append(" rub.")
						.append("\nOutfall water indication: ")
						.append(String.format("%.2f",
								doc.get("outfall_indication")))
						.append("; price: ")
						.append(String.format("%.2f", outfallPrice))
						.append(" rub; rate: ").append(doc.get("outfall_rate"))
						.append(" rub.").append("\n\nTotal price for water: ")
						.append(String.format("%.2f", totalWater))
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
		boolean isLastRecord = false;

		// check on last added record
		try {
			Optional<Document> lastRecord = Optional.ofNullable(this.db
					.getCollection("rent_stat").find().limit(1)
					.sort(Sorts.descending("$natural", "-1")).first());
			if (lastRecord.isPresent()) {
				String lastMonthAdded = lastRecord.get().getString("month");

				if (lastMonthAdded.toLowerCase().equalsIgnoreCase(month)) {
					isLastRecord = true;
				}
			}
		} catch (Exception e) {
			status = false;
			logger.error(e.getMessage(), e);
		}

		try {
			DeleteResult dr = this.db.getCollection("rent_stat").deleteOne(
					Filters.and(Filters.eq("id_chat", idChat),
							Filters.eq("month", month)));
			logger.debug("Month %s deleted successfully! Row deleted %s",
					month, dr.getDeletedCount());

			if (isLastRecord) {
				Optional<Document> lastRecord = Optional.ofNullable(this.db
						.getCollection("rent_stat").find().limit(1)
						.sort(Sorts.descending("$natural", "-1")).first());

				if (lastRecord.isPresent()) {
					logger.debug("Last record detected!");
					try {
						this.db.getCollection("rent_const")
								.updateOne(
										this.db.getCollection("rent_const")
												.find(Filters.eq("id_chat",
														idChat)).first(),
										new Document(
												"$set",
												new Document(
														"t1_last",
														lastRecord
																.get()
																.get("t1_indication"))
														.append("t2_last",
																lastRecord
																		.get()
																		.get("t2_indication"))
														.append("t3_last",
																lastRecord
																		.get()
																		.get("t3_indication"))
														.append("hw_last",
																lastRecord
																		.get()
																		.get("hot_water_indication"))
														.append("cw_last",
																lastRecord
																		.get()
																		.get("cold_water_indication"))));
					} catch (Exception e) {
						logger.error(
								"Can't update last indications! Error: %s",
								e.getMessage(), e);
					}
				}

			}
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
	 * 
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
	 * @param <T>
	 * 
	 * @param r
	 *            - Rent instance
	 * @return boolean status execute
	 */
	public <T> boolean insertPrimaryCounters(T obj, String field,
			String idChat, String owner) {

		boolean status = true;

		try {
			Optional<Document> rates = Optional.ofNullable(this.db
					.getCollection("rent_const")
					.find(Filters.eq("id_chat", idChat)).first());
			if (rates.isPresent()) {
				this.db.getCollection("rent_const").updateOne(
						this.db.getCollection("rent_const")
								.find(Filters.eq("id_chat", idChat)).first(),
						new Document("$set", new Document(field, obj)));

			} else {
				this.db.getCollection("rent_const").insertOne(
						new Document(field, obj).append("id_chat", idChat)
								.append("owner", owner));

			}
		} catch (Exception e) {
			status = false;
			logger.error("Can't insert primary values for %s! Error: %s",
					e.getMessage(), field, e);
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

	/**
	 * Return paid months for the rent
	 * 
	 * @param chatId
	 *            - chat id
	 * @return List<String> with paid months
	 */
	public List<String> getPaidRentMonths(String chatId) {

		List<String> months = new ArrayList<>();
		MongoCursor<Document> iterator = null;

		try {
			FindIterable<Document> iter = this.db.getCollection("rent_stat")
					.find(Filters.eq("id_chat", chatId));
			iterator = iter.iterator();
			while (iterator.hasNext()) {
				Document doc = iterator.next();
				months.add(doc.getString("month"));
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

		return months;
	}

	/**
	 * Check exist rent user
	 * 
	 * @param chatId
	 *            - unique chat id
	 * @return existing flag
	 */
	public boolean existRentUser(String chatId) {

		Optional<Document> rentConsts = Optional.empty();

		try {
			rentConsts = Optional.ofNullable(this.db
					.getCollection("rent_const")
					.find(Filters.eq("id_chat", chatId)).first());

			if (rentConsts.isPresent()) {

				return (rentConsts.get().get("light") != null
						&& rentConsts.get().get("rent_amount") != null && rentConsts
						.get().get("water") != null) ? true : false;
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return false;
	}

	private static class LazyDbHolder {

		public static DataBaseHelper instance = new DataBaseHelper();

	}

}
