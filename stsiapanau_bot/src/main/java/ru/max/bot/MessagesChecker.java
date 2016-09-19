package ru.max.bot;

import static ru.max.bot.BotHelper.activeCommand;
import static ru.max.bot.BotHelper.adaEvents;
import static ru.max.bot.BotHelper.adaMode;
import static ru.max.bot.BotHelper.cacheButtons;
import static ru.max.bot.BotHelper.callApiGet;
import static ru.max.bot.BotHelper.chatObjectMapper;
import static ru.max.bot.BotHelper.checkStrByRegexp;
import static ru.max.bot.BotHelper.commandMapper;
import static ru.max.bot.BotHelper.getEmoji;
import static ru.max.bot.BotHelper.objectMapper;
import static ru.max.bot.BotHelper.rentData;
import jackson.bot.message.Chat;
import jackson.bot.message.Entity;
import jackson.bot.message.IncomeMessage;
import jackson.bot.message.Message;
import jackson.bot.message.Result;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bson.Document;
import org.joda.time.DateTime;

import rent.LastIndicationsHolder;
import rent.PrimaryLightHolder;
import rent.PrimaryLightHolder.Periods;
import rent.PrimaryWaterHolder;
import rent.RentHolder;
import rent.WaterHolder;
import telegram.api.KeyboardButton;
import telegram.api.ReplyKeyboardHide;
import telegram.api.ReplyKeyboardMarkup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mongodb.client.model.Filters;

import db.DataBaseHelper;

/**
 * 
 * Messages checker
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class MessagesChecker implements Runnable {

	private static final Logger logger = LogManager
			.getFormatterLogger(MessagesChecker.class.getName());
	private String telegramApiUrl;

	public MessagesChecker(String telegramApiUrl) {
		this.telegramApiUrl = telegramApiUrl;
	}

	@Override
	public void run() {

		callApiGet("getUpdates", this.telegramApiUrl)
				.ifPresent(
						inputData -> {
							Optional<IncomeMessage> message = Optional.empty();

							try {
								message = Optional.ofNullable(objectMapper
										.readValue(inputData,
												IncomeMessage.class));
							} catch (Exception e1) {
								logger.error(e1.getMessage(), e1);
							}

							boolean status = (message.isPresent()) ? message
									.get().getOk() : false;

							if (status) {
								Response response = parseMessage(message.get());

								response.getResponses()
										.stream()
										.forEach(
												e -> {

													try {
														StringBuilder sb = new StringBuilder();

														sb.append(
																"sendMessage?chat_id=")
																.append(e
																		.getChatId())
																.append("&text=")
																.append(URLEncoder
																		.encode(e
																				.getResponseMessage(),
																				"UTF-8"));
														if (e.isNeedReplyMarkup()) {
															sb.append(
																	"&reply_markup=")
																	.append(e
																			.getReplyMarkup());
														}

														callApiGet(
																sb.toString(),
																this.telegramApiUrl);
													} catch (Exception e1) {

														logger.error(
																e1.getMessage(),
																e1);
													}

												});

								if (null != response.getMaxUpdateId()) {
									callApiGet(
											"getUpdates?offset="
													+ (response
															.getMaxUpdateId() + 1)
													+ "", this.telegramApiUrl);
								}

							}
						});

	}

	/**
	 * 
	 * @param message
	 *            -
	 * @return
	 */
	private Response parseMessage(IncomeMessage message) {

		List<Result> result = message.getResult();
		Response resp = new Response();
		Integer updateId = null;
		String id = null;
		String owner = "";

		if (result.size() > 0) {
			for (Result res : result) {
				ResponseHolder rh = new ResponseHolder();

				try {
					updateId = res.getUpdateId();
					Optional<Message> messageObj = Optional.ofNullable(res
							.getMessage());

					if (messageObj.isPresent()) {
						String typeCommand = new String("simple_message");
						List<Entity> entiies = messageObj.get().getEntities();

						if (entiies.size() > 0) {
							typeCommand = entiies.get(0).getType();
						}
						Chat chat = messageObj.get().getChat();
						id = String.valueOf(chat.getId());
						rh.setChatId(id);

						owner = chat.getFirstName() + " " + chat.getLastName();

						logger.debug("ChatId: %s; Owner: %s", id, owner);

						String text = messageObj.get().getText();

						if (text.contains(getEmoji("E29C85"))) {
							text = text.replace(getEmoji("E29C85"), "");
						}

						text = text.trim().toLowerCase();
						boolean isMapper = commandMapper.containsKey(text);
						String answer = "I can't answer at this question(";

						if (typeCommand.equalsIgnoreCase("bot_command")
								|| isMapper) {

							if (isMapper) {
								text = commandMapper.get(text);
							}

							// remove old command and object
							activeCommand.remove(id);
							chatObjectMapper.remove(id);

							// set active command
							activeCommand.put(id, text);

							switch (text) {
							case "/start": {
								List<List<String>> buttons = new ArrayList<>();

								List<String> buttonNames = new ArrayList<>();
								buttons.add(buttonNames);
								buttonNames.add("Rent");

								StringBuilder sb = new StringBuilder()
										.append("Hi. I am bot. I can process next operations:\n")
										.append("Rent (/rent) - calculating rent per month\n");

								if (owner.equalsIgnoreCase("Maksim Stepanov")
										|| owner.equalsIgnoreCase("Yuliya Stepanova")) {
									sb.append("/ada (Ada) -  events from Ada's life");
									buttonNames.add("Ada");
								}

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(objectMapper
										.writeValueAsString(getButtons(buttons)));

								answer = sb.toString();
							}
								break;
							case "/getevents":
								answer = DataBaseHelper.getInstance()
										.getAllEvents();
								if (answer.length() == 0) {
									answer = "Events not found";
								} else {
									answer = "Ada's events:" + answer;
								}
								break;
							case "/ada": {
								List<List<String>> buttons = new ArrayList<>();
								buttons.add(getButtonsList("add events",
										"see events"));
								buttons.add(getButtonsList("remove event",
										"remove all events"));

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(objectMapper
										.writeValueAsString(getButtons(buttons)));

								answer = new StringBuilder()
										.append("You can get or set events related with our Ada by sending these commands:\n")
										.append("/setevents (Add events) - activate adding event mode. After you must send two simple messages: 1- description; 2- date of event and etc.\n")
										.append("/save (Save) - saved all added events\n")
										.append("/getevents (See events) - get all events related with our Ada")
										.append("/delone (Remove event) - remove event by date\n")
										.append("/delall (Remove all events) - removed all events related with our Ada\n")
										.toString();
							}
								break;
							case "/delall": {
								long countDel = DataBaseHelper.getInstance()
										.purgeAdaEvents();
								answer = (countDel != -1) ? countDel
										+ " Ada's events removed successfully!"
										: "Oops error! Can't delete Ada's events!";
							}
								break;
							case "/save": {
								List<List<String>> buttons = new ArrayList<>();
								List<String> buttonNames = new ArrayList<>();
								buttonNames.add("Back to Ada menu");
								buttonNames.add("Home");

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(objectMapper
										.writeValueAsString(getButtons(buttons)));

								adaMode.remove(id);
								StringBuilder sb = new StringBuilder();
								DataBaseHelper
										.getInstance()
										.saveAdaEvents(id, owner)
										.ifPresent(
												e -> {
													sb.append("Events: ")
															.append(e)
															.append("\n added successfully!");
												});
								answer = sb.toString();
								if (answer.length() == 0) {
									answer = "Oops error! Can't save Ada's event!";
								}
								adaEvents.remove(id);
							}
								break;
							case "/setevents": {
								List<List<String>> buttons = new ArrayList<>();
								List<String> buttonNames = new ArrayList<>();
								buttons.add(buttonNames);
								buttonNames.add("Save");

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(objectMapper
										.writeValueAsString(getButtons(buttons)));

								adaMode.put(id, true);
								adaEvents
										.put(id, new ConcurrentLinkedQueue<>());
								answer = "Add mode activated successfully! I'm ready to get events)";
							}
								break;
							case "/delone": {
								List<List<String>> buttons = new ArrayList<>();

								DataBaseHelper
										.getInstance()
										.getAllEventsDates(id)
										.stream()
										.forEach(
												e -> {
													List<String> buttonNames = new ArrayList<>();
													buttons.add(buttonNames);
													buttonNames.add(e);
												});

								if (buttons.size() > 0) {
									List<String> buttonNames = new ArrayList<>();
									buttons.add(buttonNames);
									buttonNames.add("Back to Ada menu");
									rh.setNeedReplyMarkup(true);
									rh.setReplyMarkup(objectMapper
											.writeValueAsString(getButtons(buttons)));
									answer = "Ok. Before using this command recommend check events via 'See events' command.\nChoose event date for deleting";
								} else {
									answer = "Not found events for deleting";
									activeCommand.remove(id);
								}

							}
								break;
							case "/rent": {
								List<List<String>> buttons = new ArrayList<>();

								if (DataBaseHelper.getInstance().existRentUser(
										id)) {

									if (DataBaseHelper.getInstance()
											.existPayment(id)) {
										buttons.add(getButtonsList("add month"));
										buttons.add(getButtonsList("details",
												"payments"));
										buttons.add(getButtonsList("rates",
												"change rates"));
										buttons.add(getButtonsList("new primary"));
										buttons.add(getButtonsList(
												"remove payment", "remove rent"));
										answer = new StringBuilder()
												.append("You can control your rent by sending these commands (Use buttons below):\n")
												.append("add month (/rent_add) - add month of rent\n")
												.append("details (/getstatbymonth) - getting rent statistics by month\n")
												.append("payments (/gethistory) - return total amount by months\n")
												.append("rates (/getrates) - return all rates for rent\n")
												.append("change rates (/changerates) - change rates for rent\n")
												.append("remove payment (/delmonthstat) - remove statistics by month\n")
												.append("remove rent (/purge) - remove statistics for all months of rent and primary values\n")
												.toString();
									} else {
										buttons.add(getButtonsList("add month"));
										buttons.add(getButtonsList("rates",
												"change rates"));
										buttons.add(getButtonsList("new primary"));
										answer = new StringBuilder()
												.append("You can control your rent by sending these commands (Use buttons below):\n")
												.append("add month (/rent_add) - add month of rent\n")
												.append("rates (/getrates) - return all rates for rent\n")
												.append("change rates (/changerates) - change rates for rent\n")
												.append("new primary (/setprimarycounters)- set starting indications")
												.toString();
									}

								} else {
									buttons.add(getButtonsList("new primary"));

									answer = new StringBuilder()
											.append("Hi new user! For access to all functions for control your rent you must set primary counters. Please use this command:\n")
											.append("new primary (/setprimarycounters)- set starting indications")
											.toString();
								}

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(objectMapper
										.writeValueAsString(getButtons(buttons)));

								if (rentData.containsKey(id)) {
									rentData.remove(id);
								}
							}
								break;
							case "/rent_add": {
								RentHolder rent = new RentHolder(id, owner);
								rent.initIndications();
								rentData.put(id, Optional.of(rent));

								defaultAddMonthButtons(rent, rh);

								answer = new StringBuilder(
										"I'm ready for set indications.\nYou can use next commands (Use buttons below):\n")
										.append("name of month (/setmonth) - set the name of the rental month\n")
										.append("light (/setlight) - set indications for light\n")
										.append("water (/setwater) - set indications for water, outfall calculating automatically\n")
										.append("takeout (/settakeout) - set takeout from rent\n")
										.append("calc (/calc) - return total amount for month\n")
										.append("current statistic (/getstat)- return rent statistics for adding month\n")
										.toString();
							}
								break;
							case "/getrates": {
								answer = DataBaseHelper.getInstance().getRates(
										id, objectMapper);
								if (answer.length() == 0) {
									answer = "Rates are empty!";
								}
							}
								break;
							case "/changerates": {
								List<List<String>> buttons = new ArrayList<>();
								buttons.add(getButtonsList("hot water",
										"cold water"));
								buttons.add(getButtonsList("outfall",
										"light rate"));
								buttons.add(getButtonsList("rent amount",
										"back to rent menu"));

								cacheButtons.put(id, buttons);

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(objectMapper
										.writeValueAsString(getButtons(buttons)));

								answer = new StringBuilder()
										.append("You can change next rates via follows commands (Use buttons below):\n\n")
										.append("/changehotwaterrate (hot water) - set new hot water rate\n")
										.append("/changecoldwaterrate (cold water) - set new cold water rate\n")
										.append("/changeoutfallrate (outfall) - set new outfall rate\n")
										.append("/changelightrate (light rate)- set new light rate\n")
										.append("/changerentamount (rent amount) - set new rent amount")
										.toString();
							}
								break;
							case "/changehotwaterrate": {
								hideKeybord(rh);
								answer = "For change hot water rate send simple message with new hot water rate";
							}
								break;
							case "/changecoldwaterrate": {
								hideKeybord(rh);
								answer = "For change cold water rate send simple message with new cold water rate";
							}
								break;
							case "/changelightrate": {
								PrimaryLightHolder light = objectMapper
										.readValue(
												(String) DataBaseHelper
														.getInstance()
														.getFirstValue(
																"rent_const",
																"light",
																Filters.eq(
																		"id_chat",
																		id)),
												PrimaryLightHolder.class);

								Map<String, Double> lightRates = light
										.getRates();

								int sizeRates = lightRates.size();

								switch (sizeRates) {
								case 1:
									answer = "You have one-tariff counter. Please set new value for it";
									break;
								case 2:
									answer = "You have two-tariff counter. Please set new value for it";
									break;
								case 3:
									answer = "You have three-tariff counter. Please set new value for it";
									break;
								default:
									break;
								}

								if (sizeRates > 1) {
									answer = "You have one-tariff counter. Please set new value for it";
									List<List<String>> buttons = new ArrayList<>();
									List<String> names = new ArrayList<>();
									buttons.add(names);

									lightRates.entrySet().stream()
											.forEach(e -> {
												names.add(e.getKey());
											});

									rh.setNeedReplyMarkup(true);
									rh.setReplyMarkup(objectMapper
											.writeValueAsString(getButtons(buttons)));
								}

							}
								break;
							case "/changeoutfallrate": {
								hideKeybord(rh);
								answer = "For change outfall rate send simple message with new outfall rate";
							}
								break;
							case "/changerentamount": {
								hideKeybord(rh);
								answer = "For change rent amount send simple message with new rent amount";
							}
								break;
							case "/delmonthstat": {
								List<List<String>> buttons = new ArrayList<>();

								DataBaseHelper
										.getInstance()
										.getPaidRentMonths(id)
										.stream()
										.forEach(
												e -> {
													List<String> buttonNames = new ArrayList<>();
													buttons.add(buttonNames);
													buttonNames.add(e);
												});

								if (buttons.size() > 0) {
									buttons.add(getButtonsList("back to rent menu"));
									rh.setNeedReplyMarkup(true);
									rh.setReplyMarkup(objectMapper
											.writeValueAsString(getButtons(buttons)));
									answer = "Ok. Follow months can be deleted";
								} else {
									answer = "Not found months for deleting";
									activeCommand.remove(id);
								}
							}
								break;

							case "/gethistory": {
								answer = DataBaseHelper.getInstance()
										.getPaymentsHistory(id);
								if (answer.length() == 0) {
									answer = "History is empty!";
								}
							}
								break;
							case "/purge": {
								List<List<String>> buttons = new ArrayList<>();
								if (DataBaseHelper.getInstance().purgeAll(id)) {
									buttons.add(getButtonsList("back to rent menu"));
									rh.setNeedReplyMarkup(true);
									rh.setReplyMarkup(objectMapper
											.writeValueAsString(getButtons(buttons)));
									answer = "All information about rent removed";
								} else {
									answer = "Oops error! Can't delete information!";
								}
							}
								break;
							case "/getstat": {
								Optional<RentHolder> rentHolder = rentData
										.get(id);
								answer = (null != rentHolder) ? rentHolder
										.get().getStatAddedMonth()
										: "Rent mode is not active. For activate rent mode use 'Add month' command";
							}
								break;
							case "/getstatbymonth": {
								List<List<String>> buttons = new ArrayList<>();

								DataBaseHelper
										.getInstance()
										.getPaidRentMonths(id)
										.stream()
										.forEach(
												e -> {
													List<String> buttonNames = new ArrayList<>();
													buttons.add(buttonNames);
													buttonNames.add(e);
												});

								if (buttons.size() > 0) {
									buttons.add(getButtonsList("back to rent menu"));
									rh.setNeedReplyMarkup(true);
									rh.setReplyMarkup(objectMapper
											.writeValueAsString(getButtons(buttons)));
									answer = "Ok. Follow months can be detailed";
								} else {
									answer = "Not found months for detailed";
									activeCommand.remove(id);
								}
							}
								break;
							case "/calc": {
								List<List<String>> buttons = new ArrayList<>();
								List<String> buttonNames = new ArrayList<>();
								buttons.add(buttonNames);
								buttonNames.add("yes");
								buttonNames.add("no");

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(objectMapper
										.writeValueAsString(getButtons(buttons)));

								answer = "Ok. Do you need include outfall to final amount?";
							}
								break;
							case "/settakeout": {
								hideKeybord(rh);
								answer = "For set takeout information please use this format:\n\namount description\n\ndescription restriction - use underscore instead of space";
							}
								break;
							case "/setmonth": {
								List<List<String>> buttons = new ArrayList<>();
								DateTime date = new DateTime();
								buttons.add(getButtonsList(date.toString("MMM",
										Locale.US) + "_" + date.getYear()));

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(objectMapper
										.writeValueAsString(getButtons(buttons)));
								answer = "For set rent for current month - tap on button below.\n\nFor set rent for another month please use this format:\n\nshortmonthname_year\nExample:may_2016";
							}
								break;
							case "/setlight": {
								RentHolder rent = rentData.get(id).get();
								Map<String, Double> lightPrimary = rent
										.getLightPrimary().getIndications();

								if (lightPrimary.size() > 1) {
									List<List<String>> buttons = new ArrayList<>();
									List<String> buttonNames = new ArrayList<>();
									buttons.add(buttonNames);

									lightPrimary.entrySet().stream()
											.forEach(e -> {
												buttonNames.add(e.getKey());
											});

									buttons.add(getButtonsList("back to rent menu"));

									cacheButtons.put(id, buttons);

									rh.setNeedReplyMarkup(true);
									rh.setReplyMarkup(objectMapper
											.writeValueAsString(getButtons(buttons)));

									answer = "You have "
											+ lightPrimary.size()
											+ " counter of light. Set the value for it use buttons below";
								} else {
									hideKeybord(rh);
									answer = "You have one-tariff counter of light. Set the value for it";
								}

							}
								break;
							case "/setwater": {
								Optional<RentHolder> rentHolder = rentData
										.get(id);
								PrimaryWaterHolder water = rentHolder.get()
										.getWaterPrimary();

								List<List<String>> buttons = new ArrayList<>();
								if (water.getHotWater().size() > 1) {
									water.getHotWater()
											.entrySet()
											.stream()
											.forEach(
													e -> {
														buttons.add(getButtonsList("hot-"
																+ e.getValue()
																		.getAlias()));
													});
								} else {
									buttons.add(getButtonsList("hot"));
								}

								if (water.getColdWater().size() > 1) {
									water.getColdWater()
											.entrySet()
											.stream()
											.forEach(
													e -> {
														buttons.add(getButtonsList("cold-"
																+ e.getValue()
																		.getAlias()));
													});

								} else {
									buttons.add(getButtonsList("cold"));
								}
								buttons.add(getButtonsList("back to rent menu"));

								rentHolder.get().setWaterButtons(buttons);

								rh.setNeedReplyMarkup(true);
								try {
									rh.setReplyMarkup(objectMapper
											.writeValueAsString(getButtons(buttons)));
								} catch (JsonProcessingException e) {
									logger.error(e.getMessage(), e);
								}
								answer = "Ok. Set indications for it";
							}
								break;
							case "/setprimarycounters": {
								defaultPrimaryButtons(rh);
								answer = new StringBuilder()
										.append("You can set next primary counters via follows commands:\n\n")
										.append("/setprimarywater (set water) - set primary counters for water\n")
										.append("/setprimaryrentamount (set rent amount) - set rent amount per month\n")
										.append("/setprimarylight (set light) - set primary counters for light\n")
										.toString();
							}

								break;
							case "/setprimarywater": {
								chatObjectMapper.put(id,
										new PrimaryWaterHolder());
								List<List<String>> buttons = new ArrayList<>();
								buttons.add(getButtonsList("hot", "cold"));
								buttons.add(getButtonsList("back to rent menu"));

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(objectMapper
										.writeValueAsString(getButtons(buttons)));
								answer = "Choose type of water for setting primary indications?";
							}

								break;
							case "/setprimaryrentamount": {
								hideKeybord(rh);
								answer = "Ok. Send simple message with rent amount per month";
							}
								break;
							case "/setprimarylight": {
								chatObjectMapper.put(id,
										new PrimaryLightHolder());
								List<List<String>> buttons = new ArrayList<>();
								buttons.add(getButtonsList("1", "2", "3"));
								buttons.add(getButtonsList("back to rent menu"));

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(objectMapper
										.writeValueAsString(getButtons(buttons)));
								answer = "Which type of tariff is right for you?";
							}
								break;
							default: {
							}
								break;
							}

						} else {

							// simple message. check Ada
							// mode
							Boolean flag = adaMode.get(id);

							if (null != flag && adaMode.get(id)) {
								adaEvents.get(id).add(text);
								answer = "";
							} else {
								if (null != activeCommand.get(id)) {
									answer = processSimpleMessages(id, owner,
											activeCommand.get(id), text, rh);
								} else {
									answer = "Sorry, i can't answer to this message(";
								}
							}

						}

						rh.setResponseMessage(answer);
						resp.getResponses().add(rh);

					}
				} catch (Exception e) {
					callApiGet(
							"sendMessage?chat_id="
									+ id
									+ "&text=Oops error! Wrong operation! Say my daddy to see log. For check format use command /start.)",
							this.telegramApiUrl);
					if (rentData.containsKey(id)) {
						rentData.remove(id);
					}
					logger.error("Can't parse message! Error: %s",
							e.getMessage(), e);
				}

			}

			resp.setMaxUpdateId(updateId);

		}

		return resp;

	}

	private String processSimpleMessages(String idChat, String owner,
			String command, String text, ResponseHolder rh) {

		boolean isRemoveCommand = true;

		String answer = "";

		switch (command) {
		case "/delone": {
			answer = (DataBaseHelper.getInstance().deleteAdaEvent(text)) ? "Event "
					+ text + " deleted successfully!"
					: "Can't delete event " + text;
		}
			break;
		case "/changehotwaterrate": {
			Double value = null;
			try {
				value = Double.parseDouble(text);
			} catch (NumberFormatException e) {
				isRemoveCommand = false;
				logger.error(e.getMessage(), e);
				return "Wrong format! Value must be a number! Try again";
			}

			try {
				PrimaryWaterHolder waterHolder = objectMapper.readValue(
						(String) DataBaseHelper.getInstance().getFirstValue(
								"rent_const", "water",
								Filters.eq("id_chat", idChat)),
						PrimaryWaterHolder.class);
				waterHolder.setHotWaterRate(value);

				if (DataBaseHelper.getInstance().updateField("rent_const",
						idChat, "water",
						objectMapper.writeValueAsString(waterHolder))) {
					rh.setNeedReplyMarkup(true);
					rh.setReplyMarkup(objectMapper
							.writeValueAsString(getButtons(cacheButtons.get(idChat))));
					cacheButtons.remove(idChat);
					answer = "Hot water rate updated successfully!";
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}

		}

			break;
		case "/changecoldwaterrate": {
			Double value = null;
			try {
				value = Double.parseDouble(text);
			} catch (NumberFormatException e) {
				isRemoveCommand = false;
				logger.error(e.getMessage(), e);
				return "Wrong format! Value must be a number! Try again";
			}

			try {
				PrimaryWaterHolder waterHolder = objectMapper.readValue(
						(String) DataBaseHelper.getInstance().getFirstValue(
								"rent_const", "water",
								Filters.eq("id_chat", idChat)),
						PrimaryWaterHolder.class);
				waterHolder.setColdWaterRate(value);

				if (DataBaseHelper.getInstance().updateField("rent_const",
						idChat, "water",
						objectMapper.writeValueAsString(waterHolder))) {
					rh.setNeedReplyMarkup(true);
					rh.setReplyMarkup(objectMapper
							.writeValueAsString(getButtons(cacheButtons.get(idChat))));
					cacheButtons.remove(idChat);
					answer = "Cold water rate updated successfully!";
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}

		}

			break;
		case "/changeoutfallrate": {
			Double value = null;
			try {
				value = Double.parseDouble(text);
			} catch (NumberFormatException e) {
				isRemoveCommand = false;
				logger.error(e.getMessage(), e);
				return "Wrong format! Value must be a number! Try again";
			}

			try {
				PrimaryWaterHolder waterHolder = objectMapper.readValue(
						(String) DataBaseHelper.getInstance().getFirstValue(
								"rent_const", "water",
								Filters.eq("id_chat", idChat)),
						PrimaryWaterHolder.class);
				waterHolder.setOutfallRate(value);

				if (DataBaseHelper.getInstance().updateField("rent_const",
						idChat, "water",
						objectMapper.writeValueAsString(waterHolder))) {
					rh.setNeedReplyMarkup(true);
					rh.setReplyMarkup(objectMapper
							.writeValueAsString(getButtons(cacheButtons.get(idChat))));
					cacheButtons.remove(idChat);
					answer = "Outfall rate updated successfully!";
				}
			} catch (IOException e) {
				logger.error(e.getMessage(), e);
			}

		}
			break;
		case "/changelightrate": {
			try {
				PrimaryLightHolder light = objectMapper.readValue(
						(String) DataBaseHelper.getInstance().getFirstValue(
								"rent_const", "light",
								Filters.eq("id_chat", idChat)),
						PrimaryLightHolder.class);

				// Map<>

			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			setDefaultRentButtons(rh);
			String[] data = text.split("-");
			String typeRate = data[0].trim().toLowerCase();

			switch (typeRate) {
			case "t1":
				if (DataBaseHelper.getInstance().updateField("rent_const",
						idChat, "t1_rate", Double.parseDouble(data[1].trim()))) {
					answer = "T1 rate updated successfully!";
				} else {
					answer = "Can't update T1 rate! Ask my father to see logs(";
				}
				break;
			case "t2":
				if (DataBaseHelper.getInstance().updateField("rent_const",
						idChat, "t2_rate", Double.parseDouble(data[1].trim()))) {
					answer = "T2 rate updated successfully!";
				} else {
					answer = "Can't update T2 rate! Ask my father to see logs(";
				}
				break;
			case "t3":
				if (DataBaseHelper.getInstance().updateField("rent_const",
						idChat, "t3_rate", Double.parseDouble(data[1].trim()))) {
					answer = "T3 rate updated successfully!";
				} else {
					answer = "Can't update T3 rate! Ask my father to see logs(";
				}
				break;
			default:
				answer = "Wrong command! You have 3 rates of light. Please use this format:\n\nt1 - value\nt2 - value\nt3 - value";
				break;
			}
		}
			break;
		case "/changerentamount": {
			Double value = null;
			try {
				value = Double.parseDouble(text);
			} catch (NumberFormatException e) {
				isRemoveCommand = false;
				logger.error(e.getMessage(), e);
				return "Wrong format! Value must be a number! Try again";
			}
			
			if (DataBaseHelper.getInstance().updateField("rent_const", idChat,
					"rent_amount", value)) {
				answer = "Rent amount rate updated successfully!";
				rh.setNeedReplyMarkup(true);
				try {
					rh.setReplyMarkup(objectMapper
							.writeValueAsString(getButtons(cacheButtons.get(idChat))));
				} catch (JsonProcessingException e) {
					logger.error(e.getMessage(),e);
				}
				cacheButtons.remove(idChat);
			}
		}
			break;
		case "/delmonthstat":
			setDefaultRentButtons(rh);
			if (DataBaseHelper.getInstance().deleteMothStat(idChat,
					text.toLowerCase())) {
				answer = "Statistics of " + text + " deleted successfully!";
			} else {
				answer = "Can't delete statistics of " + text
						+ "! Ask my father to see logs(";
			}
			break;
		case "/getstatbymonth": {
			setDefaultRentButtons(rh);
			if (checkStrByRegexp(text, "[a-z]{3,4}_{1}[0-9]{4}$")) {
				answer = DataBaseHelper.getInstance().getStatByMonth(text,
						idChat);
				if (answer.length() == 0) {
					answer = "Asked month not found! Try another month!)";
				}
			} else {
				answer = "Wrong format. For check format use command - /rent";
			}
		}

			break;
		case "/calc": {
			setDefaultRentButtons(rh);
			if (checkStrByRegexp(text, "[a-z]{2,3}$")) {
				Boolean outfall = (text.equalsIgnoreCase("yes")) ? true : false;
				Optional<RentHolder> rentHolder = rentData.get(idChat);
				Optional<String> answerTemp = (rentHolder != null) ? Optional
						.of(String.valueOf(rentHolder.get().getTotalAmount(
								outfall))) : Optional.ofNullable(null);

				if (answerTemp.isPresent()) {
					answer = answerTemp.get() + " rub";
					rentData.remove(idChat);
				} else {
					answer = "Water and light didn't set. You need set water and light indications";
				}
			} else {
				isRemoveCommand = false;
				return "Wrong format. Use buttons below";
			}
		}
			break;
		case "/settakeout": {
			String[] data = text.split(" ");
			Optional<RentHolder> rentHolder = rentData.get(idChat);

			if (null != rentHolder) {
				if (data.length == 2) {
					Double takeout = null;
					try {
						takeout = Double.parseDouble(data[0]);
					} catch (NumberFormatException e) {
						logger.error(e.getMessage(), e);
						return "Takeout is not a number! Takeout must be a number.";
					}
					rentHolder.get().setTakeout(takeout);
					rentHolder.get().setTakeoutDescription(data[1]);
					answer = "Takeout set successfully!";
					defaultAddMonthButtons(rentHolder.get(), rh);
				} else {
					isRemoveCommand = false;
					return "Wrong format! Please use this format: amount description";
				}
			} else {
				answer = "Rent mode is not active. For activate rent mode use /rent_add command";
			}
		}
			break;
		case "/setmonth": {
			if (checkStrByRegexp(text, "[a-z]{3,4}_{1}[0-9]{4}$")) {
				Optional<RentHolder> rentHolder = rentData.get(idChat);

				if (null != rentHolder) {
					rentHolder.get().setMonthOfRent(text);
					answer = "Month " + rentHolder.get().getMonthOfRent()
							+ " set successfully!";
					defaultAddMonthButtons(rentHolder.get(), rh);
				} else {
					answer = "Rent mode is not active. For activate rent mode use /rent_add command";
				}
			} else {
				isRemoveCommand = false;
				return "Wrong format. Please use this format: shortmonthname_year (may_2016)";
			}
		}
			break;
		case "/setlight": {
			isRemoveCommand = false;
			RentHolder rentHolder = rentData.get(String.valueOf(idChat)).get();

			Map<String, Double> lightPrimary = rentHolder.getLightPrimary()
					.getIndications();

			int sizeLightPrimary = lightPrimary.size();

			if (sizeLightPrimary == 1) {
				try {
					rentHolder.getCurrentLightIndications().put("t1",
							Double.parseDouble(text));
				} catch (NumberFormatException e) {
					logger.error(e.getMessage(), e);
					return "Wrong format indication must be a number! Try again";
				}

				if (rentHolder.isLightSet()) {
					isRemoveCommand = true;
					answer = "Light set successfully";
					defaultAddMonthButtons(rentHolder, rh);
				}

			} else if (rentHolder.getLightPrimary().getIndications()
					.containsKey(text)) {
				rentHolder.getCurrentLightIndications().put(text, 0.0);
				rentHolder.setLightTypeActive(text);

				hideKeybord(rh);
				answer = "Set light indication for " + text;

			} else {
				String lightActiveType = rentHolder.getLightTypeActive();

				if (null != lightActiveType) {
					try {
						rentHolder.getCurrentLightIndications().put(
								lightActiveType, Double.parseDouble(text));
						answer = "Indication for " + lightActiveType
								+ " set successfully!";
						markDoneButton(idChat, lightActiveType, rh);
					} catch (NumberFormatException e) {
						logger.error(e.getMessage(), e);
						return "Wrong format indication must be a number! Try again";
					}
				} else {
					return "Choose type of light (buttons below)";
				}

				if (rentHolder.isLightSet()) {
					isRemoveCommand = true;
					answer = "Light set successfully";
					defaultAddMonthButtons(rentHolder, rh);
				}
			}
		}
			break;
		case "/setwater": {
			isRemoveCommand = false;
			RentHolder rentHolder = rentData.get(idChat).get();
			if (null != rentHolder.getWaterTypeActive()) {

				Double counterValue = null;

				try {
					counterValue = Double.parseDouble(text);
				} catch (NumberFormatException e) {
					return "Wrong format! Indicator must be a number!";
				}

				// wait value for indication
				switch (rentHolder.getWaterTypeActive()) {
				case "hot":
					((NavigableMap<Integer, WaterHolder>) rentHolder
							.getCurrentHotWaterIndications()).lastEntry()
							.getValue().setPrimaryIndication(counterValue);
					break;
				case "cold":
					((NavigableMap<Integer, WaterHolder>) rentHolder
							.getCurrentColdWaterIndications()).lastEntry()
							.getValue().setPrimaryIndication(counterValue);
					break;
				default:
					break;
				}

				answer = rentHolder.getWaterTypeActive() + " set successfully";
				defaultWaterButtons(rentHolder, rh,
						rentHolder.getButtonWaterCounterActive());

				rentHolder.setWaterTypeActive(null);

				if (rentHolder.isWaterSet()) {
					isRemoveCommand = true;
					answer = "Water set successfully";
					defaultAddMonthButtons(rentHolder, rh);
				}

			} else {
				String[] temp = text.split("-");
				int tempSize = temp.length;

				switch (temp[0]) {
				case "hot": {
					Map<Integer, WaterHolder> hotWater = rentHolder
							.getCurrentHotWaterIndications();
					if (tempSize > 1) {
						// with alias
						if (hotWater.isEmpty()) {
							hotWater.put(1, new WaterHolder(temp[1]));
							rentHolder.getAddedWater().put(text, 1);
						} else {
							if (rentHolder.getAddedWater().containsKey(text)) {
								Integer indexExist = rentHolder.getAddedWater()
										.get(text);

								hotWater.put(indexExist, new WaterHolder(
										temp[1]));

							} else {
								Integer next = ((NavigableMap<Integer, WaterHolder>) hotWater)
										.lastKey() + 1;

								hotWater.put(next, new WaterHolder(temp[1]));
								rentHolder.getAddedWater().put(text, next);
							}
						}
					} else {
						hotWater.put(1, new WaterHolder());
					}
					rentHolder.setWaterTypeActive("hot");

				}
					break;
				case "cold": {
					Map<Integer, WaterHolder> coldWater = rentHolder
							.getCurrentColdWaterIndications();
					if (tempSize > 1) {

						// with alias
						if (coldWater.isEmpty()) {
							coldWater.put(1, new WaterHolder(temp[1]));
							rentHolder.getAddedWater().put(text, 1);

						} else {
							if (rentHolder.getAddedWater().containsKey(text)) {
								Integer indexExist = rentHolder.getAddedWater()
										.get(text);

								coldWater.put(indexExist, new WaterHolder(
										temp[1]));

							} else {
								Integer next = ((NavigableMap<Integer, WaterHolder>) coldWater)
										.lastKey() + 1;
								coldWater.put(next, new WaterHolder(temp[1]));
								rentHolder.getAddedWater().put(text, next);
							}
						}

					} else {
						coldWater.put(1, new WaterHolder());
					}
					rentHolder.setWaterTypeActive("cold");
				}
					break;
				default:
					return "Wrong format! Use button below";
				}

				answer = "Set value for " + text;
				rentHolder.setButtonWaterCounterActive(text);
				hideKeybord(rh);
			}
		}
			break;
		case "/setprimarywater": {
			isRemoveCommand = false;
			PrimaryWaterHolder pwh = (PrimaryWaterHolder) chatObjectMapper
					.get(idChat);

			if (text.equalsIgnoreCase("back")) {
				List<List<String>> buttons = new ArrayList<>();

				buttons.add(getButtonsList("hot", "cold"));
				buttons.add(getButtonsList("back to rent menu"));

				rh.setNeedReplyMarkup(true);
				try {
					rh.setReplyMarkup(objectMapper
							.writeValueAsString(getButtons(buttons)));
				} catch (JsonProcessingException e) {
					logger.error(e.getMessage(), e);
				}

				pwh.setWaitValue(false);
				pwh.setWaterSet(false);
				pwh.setTypeOfWater(null);
				return "Choose type of water for setting primary indications?";
			}

			String typeOfWater = pwh.getTypeOfWater();

			if (null == typeOfWater) {

				// check back
				if (pwh.getCountColdWaterCounter() != null
						&& pwh.getCountHotWaterCounter() != null) {
					List<List<String>> buttons = new ArrayList<>();
					Map<Integer, WaterHolder> waterHolder;
					logger.trace("Back button was tap");
					Integer countCounters = 0;
					switch (text) {
					case "hot": {
						countCounters = pwh.getCountHotWaterCounter();
						pwh.setTypeOfWater(text);
						waterHolder = pwh.getHotWater();

					}
						break;
					case "cold": {
						countCounters = pwh.getCountColdWaterCounter();
						pwh.setTypeOfWater(text);
						waterHolder = pwh.getColdWater();
					}
						break;

					default: {
						activeCommand.remove(idChat);
						defaultPrimaryButtons(rh);
						return "Wrong format! Water can be hot or cold. Set primary water indications and rates again";
					}
					}

					if (null != waterHolder) {
						int addCount = 0;
						for (Entry<Integer, WaterHolder> entry : waterHolder
								.entrySet()) {

							if (entry.getValue().getAlias() != null) {
								buttons.add(getButtonsList(getEmoji("E29C85")
										+ " " + entry.getValue().getAlias()));
							} else {
								buttons.add(getButtonsList(entry.getKey()
										.toString().toLowerCase()));
							}
							++addCount;
						}

						if (addCount < countCounters) {
							for (int i = addCount; i < countCounters; i++) {
								buttons.add(getButtonsList(String
										.valueOf(i + 1)));
							}
						}

					} else {
						for (int i = 0; i < countCounters; i++) {
							buttons.add(getButtonsList(String.valueOf(i + 1)));
						}

					}
					buttons.add(getButtonsList("back"));
					buttons.add(getButtonsList("back to rent menu"));

					rh.setNeedReplyMarkup(true);
					try {
						rh.setReplyMarkup(objectMapper
								.writeValueAsString(getButtons(buttons)));
					} catch (JsonProcessingException e) {
						logger.error(e.getMessage(), e);
					}

					answer = new StringBuilder("Ok. You have ")
							.append(countCounters).append(" counter of ")
							.append(pwh.getTypeOfWater())
							.append(" water. Set primary indications for it")
							.toString();
					pwh.setWaterSet(true);

				} else {

					Integer existCounters = 0;

					switch (text) {
					case "hot":
						existCounters = pwh.getCountColdWaterCounter();
						pwh.setTypeOfWater(text);
						break;
					case "cold":
						existCounters = pwh.getCountHotWaterCounter();
						pwh.setTypeOfWater(text);
						break;

					default: {
						activeCommand.remove(idChat);
						defaultPrimaryButtons(rh);
						return "Wrong format! Water can be hot or cold. Set primary water indications and rates again";
					}
					}
					StringBuilder sb = new StringBuilder(
							"What number of counters for ").append(text)
							.append(" water you have? (Maximum: 5)");

					if (null != existCounters) {
						List<List<String>> buttons = new ArrayList<>();
						buttons.add(getButtonsList(existCounters.toString()));
						rh.setNeedReplyMarkup(true);
						try {
							rh.setReplyMarkup(objectMapper
									.writeValueAsString(getButtons(buttons)));
						} catch (JsonProcessingException e) {
							logger.error(e.getMessage(), e);
						}
						answer = sb.append("\nMaybe you have ")
								.append(existCounters).append(" counter of ")
								.append(text).append(" water").toString();
					} else {
						answer = sb.toString();
						hideKeybord(rh);
					}
				}
			} else {
				if (pwh.isWaterSet()) {

					// create water objects
					switch (typeOfWater) {
					case "hot": {
						if ((null == pwh.getHotWater() || pwh.getHotWater()
								.size() < pwh.getCountHotWaterCounter())
								&& !pwh.isWaitValue()) {

							Integer counter = null;

							try {
								counter = Integer.valueOf(text);
								if (pwh.getCountHotWaterCounter() > 1
										&& counter > pwh
												.getCountHotWaterCounter()) {
									return "Wrong format! Try again! Use buttons below to choose counter";
								}
							} catch (NumberFormatException e) {
								return "Wrong format! Try again! Use buttons below to choose counter";
							}

							pwh.setHotWater();

							if (pwh.getCountHotWaterCounter() > 1) {
								pwh.getHotWater().put(counter,
										new WaterHolder(typeOfWater));
								answer = new StringBuilder(
										"Ok. Set primary indication for counter number ")
										.append(counter)
										.append(".\nPlease use this format: value alias")
										.toString();
								hideKeybord(rh);
								pwh.setWaitValue(true);

							} else {

								pwh.getHotWater().put(1,
										new WaterHolder(typeOfWater));
								pwh.getHotWater()
										.get(1)
										.setPrimaryIndication(
												Double.valueOf(counter));

								pwh.setWaterSet(false);
								pwh.setTypeOfWater(null);

								List<List<String>> buttons = new ArrayList<>();
								buttons.add(getButtonsList(getEmoji("E29C85")
										+ " " + "hot", "cold"));
								buttons.add(getButtonsList("back to rent menu"));

								rh.setNeedReplyMarkup(true);
								try {
									rh.setReplyMarkup(objectMapper
											.writeValueAsString(getButtons(buttons)));
								} catch (JsonProcessingException e) {
									logger.error(e.getMessage(), e);
								}

								answer = "Indications for hot water set successfully";

							}

						} else {
							Integer lastKey = ((NavigableMap<Integer, WaterHolder>) pwh
									.getHotWater()).lastKey();

							if (pwh.getCountHotWaterCounter() > 1) {
								String[] temp = text.trim().split(" ");

								pwh.getHotWater()
										.get(lastKey)
										.setPrimaryIndication(
												Double.valueOf(temp[0]));
								pwh.getHotWater().get(lastKey)
										.setAlias(temp[1]);

								ListIterator<String> iter = cacheButtons
										.get(idChat).get(0).listIterator();

								while (iter.hasNext()) {
									String e = iter.next();
									if (e.equalsIgnoreCase(lastKey.toString())) {
										iter.set(e.replace(e,
												getEmoji("E29C85") + " "
														+ temp[1]));
									}
								}

								try {
									rh.setNeedReplyMarkup(true);
									rh.setReplyMarkup(objectMapper
											.writeValueAsString(getButtons(cacheButtons
													.get(idChat))));
								} catch (JsonProcessingException e) {
									logger.error(e.getMessage(), e);
								}

							} else {
								pwh.getHotWater()
										.get(lastKey)
										.setPrimaryIndication(
												Double.valueOf(text));
							}

							answer = new StringBuilder(
									"Ok. Value for counter number ")
									.append(lastKey)
									.append(" is set successful").toString();

							pwh.setWaitValue(false);

							if (lastKey == pwh.getCountHotWaterCounter()) {
								pwh.setWaterSet(false);
								pwh.setTypeOfWater(null);

								List<List<String>> buttons = new ArrayList<>();
								buttons.add(getButtonsList(getEmoji("E29C85")
										+ " " + "hot", "cold"));
								buttons.add(getButtonsList("back to rent menu"));

								rh.setNeedReplyMarkup(true);
								try {
									rh.setReplyMarkup(objectMapper
											.writeValueAsString(getButtons(buttons)));
								} catch (JsonProcessingException e) {
									logger.error(e.getMessage(), e);
								}

								answer = "Indications for hot water set successfully";

							}

						}

					}
						break;
					case "cold": {
						if ((null == pwh.getColdWater() || pwh.getColdWater()
								.size() < pwh.getCountColdWaterCounter())
								&& !pwh.isWaitValue()) {

							Integer counter = null;

							try {
								counter = Integer.valueOf(text);
								if (pwh.getCountColdWaterCounter() > 1
										&& counter > pwh
												.getCountColdWaterCounter()) {
									return "Wrong format! Try again! Use buttons below to choose counter";
								}
							} catch (NumberFormatException e) {
								return "Wrong format! Try again! Use buttons below to choose counter";
							}

							pwh.setColdWater();

							if (pwh.getCountColdWaterCounter() > 1) {
								pwh.getColdWater().put(counter,
										new WaterHolder(typeOfWater));
								answer = new StringBuilder(
										"Ok. Set primary indication for counter number ")
										.append(counter)
										.append(". Please use this format:\n value alias")
										.toString();
								hideKeybord(rh);
								pwh.setWaitValue(true);
							} else {
								pwh.getColdWater().put(1,
										new WaterHolder(typeOfWater));
								pwh.getColdWater()
										.get(1)
										.setPrimaryIndication(
												Double.valueOf(counter));

								pwh.setWaterSet(false);
								pwh.setTypeOfWater(null);

								List<List<String>> buttons = new ArrayList<>();
								buttons.add(getButtonsList("hot",
										getEmoji("E29C85") + " " + "cold"));
								buttons.add(getButtonsList("back to rent menu"));

								rh.setNeedReplyMarkup(true);
								try {
									rh.setReplyMarkup(objectMapper
											.writeValueAsString(getButtons(buttons)));
								} catch (JsonProcessingException e) {
									logger.error(e.getMessage(), e);
								}

								answer = "Indications for cold water set successfully";
							}
						} else {
							Integer lastKey = ((NavigableMap<Integer, WaterHolder>) pwh
									.getColdWater()).lastKey();
							if (pwh.getCountColdWaterCounter() > 1) {
								String[] temp = text.trim().split(" ");

								pwh.getColdWater()
										.get(lastKey)
										.setPrimaryIndication(
												Double.valueOf(temp[0]));
								pwh.getColdWater().get(lastKey)
										.setAlias(temp[1]);

								ListIterator<String> iter = cacheButtons
										.get(idChat).get(0).listIterator();

								while (iter.hasNext()) {
									String e = iter.next();
									if (e.equalsIgnoreCase(lastKey.toString())) {
										iter.set(e.replace(e,
												getEmoji("E29C85") + " "
														+ temp[1]));
									}
								}

								try {
									rh.setNeedReplyMarkup(true);
									rh.setReplyMarkup(objectMapper
											.writeValueAsString(getButtons(cacheButtons
													.get(idChat))));
								} catch (JsonProcessingException e) {
									logger.error(e.getMessage(), e);
								}

							} else {
								pwh.getColdWater()
										.get(lastKey)
										.setPrimaryIndication(
												Double.valueOf(text));
							}

							answer = new StringBuilder("Ok. Counter number ")
									.append(lastKey)
									.append(" is set successful").toString();

							pwh.setWaitValue(false);

							if (lastKey == pwh.getCountColdWaterCounter()) {
								pwh.setWaterSet(false);
								pwh.setTypeOfWater(null);

								List<List<String>> buttons = new ArrayList<>();
								buttons.add(getButtonsList("hot",
										getEmoji("E29C85") + " " + "cold"));
								buttons.add(getButtonsList("back to rent menu"));

								rh.setNeedReplyMarkup(true);
								try {
									rh.setReplyMarkup(objectMapper
											.writeValueAsString(getButtons(buttons)));
								} catch (JsonProcessingException e) {
									logger.error(e.getMessage(), e);
								}

								answer = "Indications for cold water set successfully";

							}
						}

					}
						break;
					default:
						break;
					}

					// check finish set
					if (pwh.isSetWaterIndications() && !pwh.isWaitValue()) {
						List<List<String>> buttons = new ArrayList<>();

						buttons.add(getButtonsList("hot", "cold",
								"outfall rate"));

						buttons.add(getButtonsList("back to rent menu"));

						cacheButtons.put(idChat, buttons);

						rh.setNeedReplyMarkup(true);
						try {
							rh.setReplyMarkup(objectMapper
									.writeValueAsString(getButtons(buttons)));
						} catch (JsonProcessingException e) {

							logger.error(e.getMessage(), e);
						}

						answer = "Indications for cold and hot water set successfully! Set rates for it";
						pwh.setRatesSet(true);
						pwh.setTypeOfWater("rate");
					}

				} else {

					if (pwh.isRatesSet()) {

						if (!pwh.isWaitValue()) {
							switch (text) {
							case "hot":
							case "cold":
							case "outfall rate":
								hideKeybord(rh);
								pwh.setTypeOfRates(text);
								pwh.setWaitValue(true);
								answer = new StringBuilder("Ok. Set rate for ")
										.append(text).append(" water")
										.toString();
								break;

							default:
								break;
							}
						} else {
							Double rate = null;

							try {
								rate = Double.valueOf(text);
							} catch (NumberFormatException e) {
								return "Wrong format! Try again! Rate must be a number!";
							}

							switch (pwh.getTypeOfRates()) {
							case "hot":
								pwh.setHotWaterRate(rate);
								break;
							case "cold":
								pwh.setColdWaterRate(rate);
								break;
							case "outfall rate":
								pwh.setOutfallRate(rate);
								break;
							default:
								break;
							}
							answer = new StringBuilder("Ok. Rate for ")
									.append(pwh.getTypeOfRates())
									.append(" water set successfully")
									.toString();
							pwh.setWaitValue(false);
							markDoneButton(idChat, pwh.getTypeOfRates(), rh);
						}

						if (pwh.isSetRates()) {
							answer = "Primary indications and rate for water set successfully!";

							try {
								DataBaseHelper
										.getInstance()
										.insertPrimaryCounters(
												objectMapper
														.writeValueAsString(pwh),
												"water", idChat, owner);

								String lastIndications = DataBaseHelper
										.getInstance().getFirstValue(
												"rent_const",
												"last_indications",
												Filters.eq("id_chat", idChat));

								if (null != lastIndications) {
									LastIndicationsHolder last = objectMapper
											.readValue(lastIndications,
													LastIndicationsHolder.class);
									last.setColdWater(pwh.getColdWater());
									last.setHotWater(pwh.getHotWater());
									DataBaseHelper.getInstance().updateField(
											"rent_const",
											idChat,
											"last_indications",
											objectMapper
													.writeValueAsString(last));
								}
							} catch (IOException e) {
								logger.error(e.getMessage(), e);
							}
							defaultPrimaryButtons(rh);
							chatObjectMapper.remove(idChat);
							isRemoveCommand = true;
						}
					} else {

						Integer countCounters = null;

						try {
							countCounters = Integer.valueOf(text);

							if (countCounters > 5) {
								countCounters = 5;
							}

							StringBuilder sb = new StringBuilder(
									"Ok. You have ").append(countCounters)
									.append(" counter of ")
									.append(pwh.getTypeOfWater())
									.append(" water. ");

							if (countCounters > 1) {
								List<List<String>> buttons = new ArrayList<>();

								for (int i = 0; i < countCounters; i++) {
									buttons.add(getButtonsList(String
											.valueOf(i + 1)));
								}

								buttons.add(getButtonsList("back"));
								buttons.add(getButtonsList("back to rent menu"));

								cacheButtons.put(idChat, buttons);

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(objectMapper
										.writeValueAsString(getButtons(buttons)));

								answer = sb
										.append("Please use buttons below to set value")
										.toString();
							} else {
								hideKeybord(rh);
								answer = sb.append("Please set value for it")
										.toString();
							}

						} catch (NumberFormatException
								| JsonProcessingException e) {
							logger.error(e.getMessage(), e);
							return "Wrong format! Count of counters must be a number. Try again";
						}

						switch (typeOfWater) {
						case "hot":
							pwh.setCountHotWaterCounter(countCounters);
							break;
						case "cold":
							pwh.setCountColdWaterCounter(countCounters);
							break;
						default:
							break;
						}

						pwh.setWaterSet(true);
					}
				}
			}
		}
			break;
		case "/setprimaryrentamount": {
			try {
				Double rentAmount = Double.valueOf(text);
				if (DataBaseHelper.getInstance().insertPrimaryCounters(
						rentAmount, "rent_amount", idChat, owner)) {
					answer = "Rent amount set successful";
				}
				defaultPrimaryButtons(rh);
			} catch (NumberFormatException e) {
				answer = "Wrong format! Rent amount must be a number";
				logger.error(e.getMessage(), e);
			}
		}
			break;
		case "/setprimarylight": {
			isRemoveCommand = false;

			PrimaryLightHolder lightObj = (PrimaryLightHolder) chatObjectMapper
					.get(idChat);
			if (null == lightObj.getTariffType()) {
				lightObj.setTariffType(Integer.valueOf(text));
				switch (text) {
				case "1":
					hideKeybord(rh);
					answer = "Ok. You have one-tariff counter. Send simple message with value";
					break;
				case "2": {
					List<List<String>> buttons = new ArrayList<>();

					buttons.add(getButtonsList(PrimaryLightHolder.Periods.DAY
							.name().toLowerCase(),
							PrimaryLightHolder.Periods.NIGHT.name()
									.toLowerCase()));
					buttons.add(getButtonsList("back to rent menu"));

					cacheButtons.put(idChat, buttons);

					try {
						rh.setNeedReplyMarkup(true);
						rh.setReplyMarkup(objectMapper
								.writeValueAsString(getButtons(buttons)));
					} catch (JsonProcessingException e) {
						logger.error(e.getMessage(), e);
					}
					answer = "Ok. You have two-tariff counter. Set primary value for it. Please use button below";
				}
					break;
				case "3": {
					List<List<String>> buttons = new ArrayList<>();
					Set<String> periodsName = new TreeSet<>();
					Arrays.asList(PrimaryLightHolder.Periods.values()).stream()
							.forEach(e -> {
								if (!e.equals(PrimaryLightHolder.Periods.DAY)) {
									periodsName.add(e.name().toLowerCase());
								}
							});
					String[] names = {};
					buttons.add(getButtonsList(periodsName.toArray(names)));
					buttons.add(getButtonsList("back to rent menu"));

					cacheButtons.put(idChat, buttons);

					try {
						rh.setNeedReplyMarkup(true);
						rh.setReplyMarkup(objectMapper
								.writeValueAsString(getButtons(buttons)));
					} catch (JsonProcessingException e) {
						logger.error(e.getMessage(), e);
					}
					answer = "Ok. You have three-tariff counter. Set primary value for it. Please use button below";
					break;
				}
				default:
					break;
				}
			} else {

				Integer tariffType = lightObj.getTariffType();
				Map<String, Double> rates = lightObj.getRates();

				Periods[] periods = PrimaryLightHolder.Periods.values();
				List<String> periodsStr = new ArrayList<>();

				for (Periods period : periods) {
					periodsStr.add(period.name().toLowerCase());
				}

				if (periodsStr.contains(text)) {
					lightObj.setPeriod(text);
					hideKeybord(rh);
					return (lightObj.getRates() == null) ? new StringBuilder(
							"Ok. Set start indication value for ").append(text)
							.append(" period").toString() : new StringBuilder(
							"Ok. Set rate value for ").append(text)
							.append(" period").toString();
				}

				if (null != tariffType && rates == null) {

					switch (lightObj.getTariffType()) {
					case 1:
						try {
							hideKeybord(rh);
							lightObj.getIndications().put("t1",
									Double.valueOf(text));
							answer = "Primary start indications set successfully! Please set rate via simple send message with value.";
							lightObj.initRates();
						} catch (NumberFormatException e) {
							logger.error(e.getMessage(), e);
							return "Wrong format! Try again!";
						}
						break;
					case 2:
					case 3:
						try {
							lightObj.getIndications().put(lightObj.getPeriod(),
									Double.valueOf(text));
							answer = new StringBuilder("Start indication for ")
									.append(lightObj.getPeriod())
									.append(" period set succefully")
									.toString();

							markDoneButton(idChat, lightObj.getPeriod(), rh);

						} catch (NumberFormatException e) {
							logger.error(e.getMessage(), e);
							return "Wrong format! Try again!";
						}

						if (lightObj.isSetIndications()) {
							List<List<String>> buttons = new ArrayList<>();

							if (lightObj.getTariffType() == 2) {
								buttons.add(getButtonsList(
										PrimaryLightHolder.Periods.DAY.name()
												.toLowerCase(),
										PrimaryLightHolder.Periods.NIGHT.name()
												.toLowerCase()));
							} else {
								Set<String> periodsName = new TreeSet<>();
								Arrays.asList(
										PrimaryLightHolder.Periods.values())
										.stream()
										.forEach(
												e -> {
													if (!e.equals(PrimaryLightHolder.Periods.DAY)) {
														periodsName.add(e
																.name()
																.toLowerCase());
													}
												});
								String[] names = {};
								buttons.add(getButtonsList(periodsName
										.toArray(names)));
							}

							buttons.add(getButtonsList("back to rent menu"));

							try {
								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(objectMapper
										.writeValueAsString(getButtons(buttons)));
							} catch (JsonProcessingException e) {
								logger.error(e.getMessage(), e);
							}

							answer = "Primary start indications set successfully! Please set rates for it";
							lightObj.initRates();
							cacheButtons.put(idChat, buttons);
						}
						break;

					default:
						break;
					}
				} else if (null != rates) {

					switch (lightObj.getTariffType()) {
					case 1:
						try {
							lightObj.getRates().put("t1", Double.valueOf(text));
							answer = "Primary indications and rate for light set successfully!";
							cacheButtons.remove(idChat);

							try {
								DataBaseHelper
										.getInstance()
										.insertPrimaryCounters(
												objectMapper
														.writeValueAsString(lightObj),
												"light", idChat, owner);
							} catch (JsonProcessingException e) {
								logger.error(e.getMessage(), e);
							}

							defaultPrimaryButtons(rh);
							chatObjectMapper.remove(idChat);
							isRemoveCommand = true;
						} catch (NumberFormatException e) {
							logger.error(e.getMessage(), e);
							return "Wrong format! Try again!";
						}
						break;
					case 2:
					case 3:
						try {
							lightObj.getRates().put(lightObj.getPeriod(),
									Double.valueOf(text));
							answer = new StringBuilder("Rate for ")
									.append(lightObj.getPeriod())
									.append(" period set succefully")
									.toString();

							markDoneButton(idChat, lightObj.getPeriod(), rh);

						} catch (NumberFormatException e) {
							logger.error(e.getMessage(), e);
							return "Wrong format! Try again!";
						}

						if (lightObj.isSetRates()) {
							answer = "Primary indications and rate for light set successfully!";

							try {
								DataBaseHelper
										.getInstance()
										.insertPrimaryCounters(
												objectMapper
														.writeValueAsString(lightObj),
												"light", idChat, owner);

								String lastIndications = DataBaseHelper
										.getInstance().getFirstValue(
												"rent_const",
												"last_indications",
												Filters.eq("id_chat", idChat));

								if (null != lastIndications) {
									LastIndicationsHolder last = objectMapper
											.readValue(lastIndications,
													LastIndicationsHolder.class);
									last.setLight(lightObj.getIndications());
									DataBaseHelper.getInstance().updateField(
											"rent_const",
											idChat,
											"last_indications",
											objectMapper
													.writeValueAsString(last));
								}
							} catch (IOException e) {
								logger.error(e.getMessage(), e);
							}

							defaultPrimaryButtons(rh);
							chatObjectMapper.remove(idChat);
							isRemoveCommand = true;
						}
						break;

					default:
						break;
					}

				}
			}
		}
			break;
		default:
			answer = "Sorry, i can't answer to this message(";
			break;
		}

		if (isRemoveCommand) {
			activeCommand.remove(idChat);
		}

		return answer;
	}

	private ReplyKeyboardMarkup getButtons(List<List<String>> buttonNames) {

		List<List<KeyboardButton>> keyButtons = new ArrayList<>();

		ReplyKeyboardMarkup rkm = new ReplyKeyboardMarkup();
		rkm.setResize_keyboard(true);
		rkm.setOne_time_keyboard(false);
		rkm.setKeyboard(keyButtons);

		buttonNames.stream().forEach(name -> {
			List<KeyboardButton> buttons = new ArrayList<>();
			name.stream().forEach(e -> {
				KeyboardButton key = new KeyboardButton();
				key.setText(e);
				buttons.add(key);
			});

			keyButtons.add(buttons);
		});
		return rkm;
	}

	private void setDefaultRentButtons(ResponseHolder rh) {

		List<List<String>> buttons = new ArrayList<>();
		buttons.add(getButtonsList("back to rent menu"));

		try {
			rh.setNeedReplyMarkup(true);
			rh.setReplyMarkup(objectMapper
					.writeValueAsString(getButtons(buttons)));
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void defaultPrimaryButtons(ResponseHolder rh) {

		List<List<String>> buttons = new ArrayList<>();
		Optional<Document> document = Optional.ofNullable(DataBaseHelper
				.getInstance().getFirstDocByFilter("rent_const",
						Filters.eq("id_chat", rh.getChatId())));
		String light = "set light";
		String water = "set water";
		String rentAmount = "set rent amount";

		if (document.isPresent()) {
			if (null != document.get().get("light")) {
				light = getEmoji("E29C85") + " " + light;
			}

			if (null != document.get().get("water")) {
				water = getEmoji("E29C85") + " " + water;
			}

			if (null != document.get().get("rent_amount")) {
				rentAmount = getEmoji("E29C85") + " " + rentAmount;
			}
		}

		buttons.add(getButtonsList(water, rentAmount, light));

		buttons.add(getButtonsList("back to rent menu"));

		try {
			rh.setNeedReplyMarkup(true);
			rh.setReplyMarkup(objectMapper
					.writeValueAsString(getButtons(buttons)));
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
		}

	}

	private void defaultAddMonthButtons(RentHolder rent, ResponseHolder rh) {
		List<List<String>> buttons = new ArrayList<>();

		buttons.add(getButtonsList(
				(null == rent.getMonthOfRent()) ? "name of month"
						: getEmoji("E29C85") + " name of month", (rent
						.isLightSet()) ? getEmoji("E29C85") + " light"
						: "light"));
		buttons.add(getButtonsList(rent.isWaterSet() ? getEmoji("E29C85")
				+ " water" : "water", (null == rent.getTakeout()) ? "takeout"
				: getEmoji("E29C85") + " takeout"));
		buttons.add(getButtonsList("current statistic", "calc"));
		buttons.add(getButtonsList("back to rent menu"));

		rh.setNeedReplyMarkup(true);
		try {
			rh.setReplyMarkup(objectMapper
					.writeValueAsString(getButtons(buttons)));
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private void hideKeybord(ResponseHolder rh) {
		rh.setNeedReplyMarkup(true);
		try {
			rh.setReplyMarkup(objectMapper
					.writeValueAsString(new ReplyKeyboardHide()));
		} catch (JsonProcessingException e) {

			logger.error(e.getMessage());
		}
	}

	private void defaultWaterButtons(RentHolder rent, ResponseHolder rh,
			String fillCounter) {
		rh.setNeedReplyMarkup(true);
		try {
			List<List<String>> list = rent.getWaterButtons();
			if (null != fillCounter) {

				list.stream().forEach(e -> {
					int index = e.indexOf(fillCounter);
					if (index != -1) {
						String done = e.get(index);
						e.remove(index);
						e.add(index, getEmoji("E29C85") + " " + done);
					}
				});
			}
			rh.setReplyMarkup(objectMapper.writeValueAsString(getButtons(list)));
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
		}
	}

	private List<String> getButtonsList(String... buttonsNames) {

		List<String> buttons = new ArrayList<>();
		int len = buttonsNames.length;

		for (int i = 0; i < len; i++) {
			buttons.add(buttonsNames[i]);
		}
		return buttons;
	}

	private void markDoneButton(String idChat, String buttonName,
			ResponseHolder rh) {
		ListIterator<String> iter = cacheButtons.get(idChat).get(0)
				.listIterator();

		while (iter.hasNext()) {
			String e = iter.next();
			if (e.equalsIgnoreCase(buttonName)) {
				iter.set(e.replace(e, getEmoji("E29C85") + " " + e));
			}
		}

		try {
			rh.setNeedReplyMarkup(true);
			rh.setReplyMarkup(objectMapper
					.writeValueAsString(getButtons(cacheButtons.get(idChat))));
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
