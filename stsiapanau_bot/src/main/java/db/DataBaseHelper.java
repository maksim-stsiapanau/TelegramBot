package db;

import static ru.max.bot.BotHelper.objectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.bson.conversions.Bson;

import rent.Counter;
import rent.LastIndicationsHolder;
import rent.PrimaryLightHolder;
import rent.PrimaryWaterHolder;
import rent.RentMonthHolder;
import rent.WaterHolder;

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

	private static final Logger logger = LogManager
			.getFormatterLogger(DataBaseHelper.class.getName());
	private MongoClient mongoCl;
	private MongoDatabase db;

	private DataBaseHelper() {
		this.mongoCl = new MongoClient();
		this.db = this.mongoCl.getDatabase("apartment_rent_bot");
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
	public String getRates(String chatId, ObjectMapper mapper, boolean isRus) {

		StringBuilder sb = new StringBuilder();

		try {
			Optional<Document> document = Optional.of(this.db
					.getCollection("rent_const")
					.find(Filters.eq("id_chat", chatId)).first());
			document.ifPresent(doc -> {

				sb.append(
						(isRus) ? "<b>Сумма аренды:</b> "
								: "<b>Rent amount:</b> ")
						.append(doc.get("rent_amount"))
						.append(" rub.")
						.append((isRus) ? "\n\n<b>Электричество</b>"
								: "\n\n<b>Light</b>");

				try {
					PrimaryLightHolder plh = mapper.readValue(
							(String) doc.get("light"), PrimaryLightHolder.class);

					for (Entry<String, Double> entry : plh.getRates()
							.entrySet()) {
						sb.append("\n").append(entry.getKey()).append(": ")
								.append(entry.getValue())
								.append((isRus) ? " руб" : " rub");
					}

					sb.append((isRus) ? "\n\n<b>Вода</b>\n"
							: "\n\n<b>Water</b>\n");

					PrimaryWaterHolder pwh = mapper.readValue(
							(String) doc.get("water"), PrimaryWaterHolder.class);

					sb.append(
							(isRus) ? "Горячая: "
									: "Hot: ")
							.append(pwh.getHotWaterRate())
							.append((isRus) ? " руб" : " rub")
							.append((isRus) ? "\nХолодная: "
									: "\nCold: ")
							.append(pwh.getColdWaterRate())
							.append(" rub.")
							.append((isRus) ? "\nВодоотвод: "
									: "\nOutfall: ")
							.append(pwh.getOutfallRate())
							.append((isRus) ? " руб" : " rub");

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
	public String getStatByMonth(String month, String idChat, boolean isRus) {

		StringBuilder sb = new StringBuilder();

		try {
			Optional<Document> document = Optional.ofNullable(DataBaseHelper
					.getInstance().getFirstDocByFilter(
							"rent_stat",
							Filters.and(Filters.eq("month", month),
									Filters.eq("id_chat", idChat))));
			document.ifPresent(doc -> {

				try {
					RentMonthHolder rentHolder = objectMapper.readValue(
							(String) doc.get("stat"), RentMonthHolder.class);

					LastIndicationsHolder lastData = rentHolder
							.getLastIndications();

					Map<String, Double> lightLast = lastData.getLight();

					sb.append((isRus) ? "<b>Автор:</b> " : "<b>Added by:</b> ")
							.append(rentHolder.getOwner())
							.append((isRus) ? "\n<b>Месяц:</b> "
									: "\n<b>Month:</b> ")
							.append(rentHolder.getMonth())
							.append((isRus) ? "\n<b>Конечная стоимость аренды:</b> "
									: "\n<b>Final amount:</b> ")
							.append(String.format("%.2f",
									rentHolder.getTotalAmount()))
							.append((isRus) ? " руб" : " rub")
							.append((isRus) ? "\n\n<b>Электричество</b>\n\n"
									: "\n\n<b>Light</b>\n\n");

					rentHolder
							.getLight()
							.entrySet()
							.stream()
							.forEach(
									e -> {
										sb.append(e.getKey())
												.append((isRus) ? " - показание: "
														: " - indication: ")
												.append(String.format("%.2f",
														lightLast.get(e
																.getKey())))
												.append((isRus) ? "; использовано: "
														: "; used: ")
												.append(String.format("%.2f", e
														.getValue().getUsed()))
												.append((isRus) ? "; стоимость: "
														: "; price: ")
												.append(String.format("%.2f", e
														.getValue().getPrice()))
												.append((isRus) ? " руб; тариф: "
														: " rub; rate: ")
												.append(String.format("%.2f", e
														.getValue().getRate()))
												.append((isRus) ? " руб"
														: " rub")
												.append("\n\n");
									});

					int sizeColdWater = rentHolder.getColdWater().size();
					int sizeHotWater = rentHolder.getHotWater().size();

					if (sizeColdWater > 0) {
						sb.append((isRus) ? "\n<b>Холодная вода</b>"
								: "\n<b>Cold water</b>");
						rentHolder
								.getColdWater()
								.entrySet()
								.stream()
								.forEach(
										e -> {

											sb.append("\n\n");
											Optional<String> alias = Optional
													.ofNullable(e.getValue()
															.getAlias());

											Double lastIndication = null;
											int size = lastData.getColdWater()
													.size();
											if (size == 1) {
												lastIndication = lastData
														.getColdWater().get(1)
														.getPrimaryIndication();
											} else {
												for (Entry<Integer, WaterHolder> entry : lastData
														.getColdWater()
														.entrySet()) {

													WaterHolder wh = entry
															.getValue();

													if (alias.get().equals(
															wh.getAlias())) {
														lastIndication = wh
																.getPrimaryIndication();
													}
												}
											}

											if (alias.isPresent()) {
												sb.append(alias.get()).append(
														" - ");

											}

											sb.append(
													(isRus) ? "показание: "
															: "indication: ")
													.append(String.format(
															"%.2f",
															lastIndication))
													.append((isRus) ? "; использовано: "
															: "; used: ")
													.append(String.format(
															"%.2f", e
																	.getValue()
																	.getUsed()))
													.append((isRus) ? "; стоимость: "
															: "; price: ")
													.append(String
															.format("%.2f", e
																	.getValue()
																	.getPrice()))
													.append((isRus) ? " руб; тариф: "
															: " rub; rate: ")
													.append(String.format(
															"%.2f", e
																	.getValue()
																	.getRate()))
													.append((isRus) ? " руб"
															: " rub");
										});
					}

					if (sizeHotWater > 0) {
						sb.append((isRus) ? "\n\n<b>Горячая вода</b>"
								: "\n\n<b>Hot water</b>");
						rentHolder
								.getHotWater()
								.entrySet()
								.stream()
								.forEach(
										e -> {
											sb.append("\n\n");
											Optional<String> alias = Optional
													.ofNullable(e.getValue()
															.getAlias());

											if (alias.isPresent()) {
												sb.append(alias.get()).append(
														" - ");
											}

											Double lastIndication = null;
											int size = lastData.getHotWater()
													.size();
											if (size == 1) {
												lastIndication = lastData
														.getHotWater().get(1)
														.getPrimaryIndication();
											} else {
												for (Entry<Integer, WaterHolder> entry : lastData
														.getHotWater()
														.entrySet()) {

													WaterHolder wh = entry
															.getValue();

													if (alias.get().equals(
															wh.getAlias())) {
														lastIndication = wh
																.getPrimaryIndication();
													}
												}
											}
											sb.append(
													(isRus) ? "показание: "
															: "indication: ")
													.append(String.format(
															"%.2f",
															lastIndication))
													.append((isRus) ? "; использовано: "
															: "; used: ")
													.append(String.format(
															"%.2f", e
																	.getValue()
																	.getUsed()))
													.append((isRus) ? "; стоимость: "
															: "; price: ")
													.append(String
															.format("%.2f", e
																	.getValue()
																	.getPrice()))
													.append((isRus) ? " руб; тариф: "
															: " rub; rate: ")
													.append(String.format(
															"%.2f", e
																	.getValue()
																	.getRate()))
													.append((isRus) ? " руб"
															: " rub");
										});

					}

					Optional<Counter> outfall = Optional.ofNullable(rentHolder
							.getOutfall());

					if (outfall.isPresent()) {

						sb.append(
								(isRus) ? "\n\n<b>Водоотвод</b> - количество: "
										: "\n\n<b>Outfall</b> - count: ")
								.append(String.format("%.2f", outfall.get()
										.getUsed()))
								.append((isRus) ? "; стоимость: " : "; price: ")
								.append(String.format("%.2f", outfall.get()
										.getPrice()))
								.append((isRus) ? " руб; тариф: "
										: " rub; rate: ")
								.append(String.format("%.2f", outfall.get()
										.getRate()))
								.append((isRus) ? " руб" : " rub");

					}

					sb.append(
							(isRus) ? "\n\n<b>Стоимость аренды:</b> "
									: "\n\n<b>Rent Amount:</b> ")
							.append(String.format("%.2f",
									rentHolder.getRentAmount()))
							.append((isRus) ? " руб" : " rub");

					if (null != rentHolder.getTakeout()) {
						sb.append(
								(isRus) ? "\n<b>Вычет:</b> "
										: "\n<b>Takeout:</b> ")
								.append(String.format("%.2f",
										rentHolder.getTakeout()))
								.append((isRus) ? " руб" : " rub")
								.append(" - ")
								.append(rentHolder.getTakeoutDesc());
					}

				} catch (Exception e1) {
					logger.error(e1.getMessage(), e1);
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
	 * Get total amount for rent by months
	 * 
	 * @return String message with history by months
	 */
	public String getPaymentsHistory(String chatId, boolean isRus) {

		StringBuilder sb = new StringBuilder();

		MongoCursor<Document> iter = null;

		try {
			FindIterable<Document> docs = this.db.getCollection("rent_stat")
					.find(Filters.eq("id_chat", chatId))
					.sort(Sorts.descending("add_date", "-1"));
			iter = docs.iterator();
			sb.append((isRus) ? "История:\n" : "History:\n");
			while (iter.hasNext()) {
				Document document = iter.next();
				sb.append("\n")
						.append(document.get("month"))
						.append(": ")
						.append(String
								.format("%.2f",
										objectMapper.readValue(
												(String) document.get("stat"),
												RentMonthHolder.class)
												.getTotalAmount()))
						.append((isRus) ? " руб" : " rub");
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

		// check count document -> if == 1 remove all (purge)
		if (getDocsCount("rent_stat", idChat) == 1) {
			status = purgeAll(idChat);
		} else {
			// check on last added record
			try {
				Optional<Document> lastRecord = Optional.ofNullable(this.db
						.getCollection("rent_stat")
						.find(Filters.eq("id_chat", idChat)).limit(1)
						.sort(Sorts.descending("add_date", "-1")).first());
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
					Document lastRecord = this.db.getCollection("rent_stat")
							.find(Filters.eq("id_chat", idChat)).limit(1)
							.sort(Sorts.descending("add_date", "-1")).first();

					logger.debug("Last record detected!");

					try {
						RentMonthHolder rent = objectMapper.readValue(
								(String) lastRecord.get("stat"),
								RentMonthHolder.class);

						this.db.getCollection("rent_const")
								.updateOne(
										this.db.getCollection("rent_const")
												.find(Filters.eq("id_chat",
														idChat)).first(),
										new Document(
												"$set",
												new Document(
														"last_indications",
														objectMapper
																.writeValueAsString(rent
																		.getLastIndications()))));
					} catch (Exception e) {
						logger.error(
								"Can't update last indications! Error: %s",
								e.getMessage(), e);
					}

				}
			} catch (Exception e) {
				status = false;
				logger.error("Can't purge statistics! Error: %s",
						e.getMessage(), e);
			}
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
	public <T> boolean updateField(String collection, String idChat,
			String field, T value) {

		boolean status = true;

		try {
			this.db.getCollection(collection).updateOne(
					this.db.getCollection(collection)
							.find(Filters.eq("id_chat", idChat)).first(),
					new Document("$set", new Document(field, value)));
		} catch (Exception e) {
			status = false;
			logger.error("Can't update %s at %s! Error: %s", field, collection,
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
		boolean primarySet = false;

		try {
			Optional<Document> rates = Optional.ofNullable(this.db
					.getCollection("rent_const")
					.find(Filters.eq("id_chat", idChat)).first());
			if (rates.isPresent()) {
				this.db.getCollection("rent_const").updateOne(
						this.db.getCollection("rent_const")
								.find(Filters.eq("id_chat", idChat)).first(),
						new Document("$set", new Document(field, obj)));

				primarySet = (rates.get().get("light") != null
						&& rates.get().get("rent_amount") != null && rates
						.get().get("water") != null) ? true : false;

			} else {
				this.db.getCollection("rent_const").insertOne(
						new Document(field, obj).append("id_chat", idChat)
								.append("owner", owner));

			}

			if (primarySet) {
				LastIndicationsHolder lastIndications = new LastIndicationsHolder();
				try {
					lastIndications.setLight(objectMapper.readValue(
							(String) rates.get().get("light"),
							PrimaryLightHolder.class).getIndications());

					lastIndications.setColdWater(objectMapper.readValue(
							(String) rates.get().get("water"),
							PrimaryWaterHolder.class).getColdWater());
					lastIndications.setHotWater(objectMapper.readValue(
							(String) rates.get().get("water"),
							PrimaryWaterHolder.class).getHotWater());

					this.db.getCollection("rent_const")
							.updateOne(
									this.db.getCollection("rent_const")
											.find(Filters.eq("id_chat", idChat))
											.first(),
									new Document(
											"$set",
											new Document(
													"last_indications",
													objectMapper
															.writeValueAsString(lastIndications))));

				} catch (IOException e) {
					logger.error(e.getMessage(), e);
				}
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
	public boolean insertMonthStat(RentMonthHolder total) {

		boolean status = true;

		if (null != getFirstDocByFilter(
				"rent_stat",
				Filters.and(Filters.eq("month", total.getMonth()),
						Filters.eq("id_chat", total.getChatId())))) {

			this.db.getCollection("rent_stat").deleteOne(
					Filters.and(Filters.eq("id_chat", total.getChatId()),
							Filters.eq("month", total.getMonth())));
			logger.debug("Month exist! Month will be replace");
		}

		try {
			this.db.getCollection("rent_stat").insertOne(
					new Document("month", total.getMonth())
							.append("stat",
									objectMapper.writeValueAsString(total))
							.append("id_chat", total.getChatId())
							.append("who_set", total.getOwner())
							.append("add_date", new Date().getTime()));
		} catch (Exception e) {
			status = false;
			logger.error("Can't insert month stat! Error: %s", e.getMessage(),
					e);
		}

		// update last indications
		try {
			this.db.getCollection("rent_const").updateOne(
					this.db.getCollection("rent_const")
							.find(Filters.eq("id_chat", total.getChatId()))
							.first(),
					new Document("$set", new Document("last_indications",
							objectMapper.writeValueAsString(total
									.getLastIndications()))));
		} catch (Exception e) {
			status = false;
			logger.error("Can't update last indications! Error: %s",
					e.getMessage(), e);
		}
		return status;
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
					.find(Filters.eq("id_chat", chatId))
					.sort(Sorts.descending("add_date", "-1"));
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
				boolean status = (rentConsts.get().get("light") != null
						&& rentConsts.get().get("rent_amount") != null && rentConsts
						.get().get("water") != null) ? true : false;

				if (status && rentConsts.get().get("last_indications") == null) {
					LastIndicationsHolder lastIndications = new LastIndicationsHolder();
					try {
						lastIndications.setLight(objectMapper.readValue(
								(String) rentConsts.get().get("light"),
								PrimaryLightHolder.class).getIndications());

						lastIndications.setColdWater(objectMapper.readValue(
								(String) rentConsts.get().get("water"),
								PrimaryWaterHolder.class).getColdWater());
						lastIndications.setHotWater(objectMapper.readValue(
								(String) rentConsts.get().get("water"),
								PrimaryWaterHolder.class).getHotWater());

						this.db.getCollection("rent_const")
								.updateOne(
										this.db.getCollection("rent_const")
												.find(Filters.eq("id_chat",
														chatId)).first(),
										new Document(
												"$set",
												new Document(
														"last_indications",
														objectMapper
																.writeValueAsString(lastIndications))));

					} catch (IOException e) {
						logger.error(e.getMessage(), e);
					}
				}
				return status;
			}

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return false;
	}

	/**
	 * Check exist rent statistic by user
	 * 
	 * @param chatId
	 *            - unique chat id
	 * @return existing flag
	 */
	public boolean existPayment(String chatId) {

		Optional<Document> rentStat = Optional.empty();

		try {
			rentStat = Optional.ofNullable(this.db.getCollection("rent_stat")
					.find(Filters.eq("id_chat", chatId)).first());

		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}

		return rentStat.isPresent();
	}

	public long getDocsCount(String collection, String idChat) {
		return this.db.getCollection(collection).count(
				Filters.eq("id_chat", idChat));
	}

	private static class LazyDbHolder {

		public static DataBaseHelper instance = new DataBaseHelper();

	}

}
