package ru.max.bot;

import static ru.max.bot.BotHelper.activeCommand;
import static ru.max.bot.BotHelper.adaEvents;
import static ru.max.bot.BotHelper.adaMode;
import static ru.max.bot.BotHelper.callApiGet;
import static ru.max.bot.BotHelper.checkStrByRegexp;
import static ru.max.bot.BotHelper.rentData;
import jackson.bot.message.Chat;
import jackson.bot.message.Entity;
import jackson.bot.message.IncomeMessage;
import jackson.bot.message.Message;
import jackson.bot.message.Result;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rent.PrimaryRatesHolder;
import rent.RentHolder;
import telegram.api.KeyboardButton;
import telegram.api.ReplyKeyboardMarkup;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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
	private ObjectMapper objectMapper = new ObjectMapper();

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
								message = Optional.ofNullable(this.objectMapper
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

						String text = null;
						text = messageObj.get().getText();
						text = text.trim().toLowerCase();

						String answer = "I can't answer at this question(";

						if (typeCommand.equalsIgnoreCase("bot_command")) {

							// remove old command
							activeCommand.remove(id);

							// set active command
							activeCommand.put(id, text);

							switch (text) {
							case "/start": {
								List<List<String>> buttons = new ArrayList<>();

								List<String> buttonNames = new ArrayList<>();
								buttons.add(buttonNames);
								buttonNames.add("/rent");

								StringBuilder sb = new StringBuilder()
										.append("Hi. I am bot. I can process next operations:\n")
										.append("/rent - calculating rent per month\n");

								if (owner.equalsIgnoreCase("Maksim Stepanov")
										|| owner.equalsIgnoreCase("Yuliya Stepanova")) {
									sb.append("/ada -  events from Ada's life");
									buttonNames.add("/ada");
								}

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(this.objectMapper
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
								List<String> buttonNames = null;

								for (int i = 0; i < 3; i++) {
									buttonNames = new ArrayList<>();
									buttons.add(buttonNames);
									switch (i) {
									case 0: {
										buttonNames.add("/setevents");
										buttonNames.add("/getevents");
									}
										break;
									case 1: {
										buttonNames.add("/delone");
										buttonNames.add("/delall");
									}
										break;
									case 2: {
										buttonNames.add("/start");
									}
										break;
									default:
										break;
									}
								}

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(this.objectMapper
										.writeValueAsString(getButtons(buttons)));

								answer = new StringBuilder()
										.append("You can get or set events related with our Ada by sending these commands:\n")
										.append("/setevents - activate adding event mode. After you must send two simple messages: 1- description; 2- date of event and etc.\n")
										.append("/save - saved all added events\n")
										.append("/delone - remove event by date\n")
										.append("/delall - removed all events related with our Ada\n")
										.append("/getevents - get all events related with our Ada")
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
								buttonNames.add("/ada");
								buttonNames.add("/start");

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(this.objectMapper
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
								buttonNames.add("/save");

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(this.objectMapper
										.writeValueAsString(getButtons(buttons)));

								adaMode.put(id, true);
								adaEvents
										.put(id, new ConcurrentLinkedQueue<>());
								answer = "Add mode activated successfully! I'm ready to get events)";
							}
								break;
							case "/delone":
								answer = "OK. Send simple message with event date for deleting.\n Before using this command recommend check events via /getevents command.\nPlease use this format:\n\ndd.mm.yyyy\nExample:17.08.2016";
								break;
							case "/rent": {
								List<List<String>> buttons = new ArrayList<>();
								List<String> buttonNames = null;

								for (int i = 0; i < 5; i++) {
									buttonNames = new ArrayList<>();
									buttons.add(buttonNames);
									switch (i) {
									case 0: {
										buttonNames.add("/rent_add");
									}
										break;
									case 1: {
										buttonNames.add("/gethistory");
										buttonNames.add("/getstatbymonth");
									}
										break;
									case 2: {
										buttonNames.add("/getrates");
										buttonNames.add("/changerates");
									}
										break;
									case 3: {
										buttonNames.add("/setprimarycounters");
									}
										break;
									case 4: {
										buttonNames.add("/purge");
										buttonNames.add("/start");
									}
										break;
									default:
										break;
									}
								}

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(this.objectMapper
										.writeValueAsString(getButtons(buttons)));

								answer = new StringBuilder()
										.append("You can control rent by sending these commands:\n")
										.append("/setmonth - set rent month\n")
										.append("/setlight - set indications for light\n")
										.append("/setwater - set indications for water, outfall calculating automatically\n")
										.append("/settakeout - set takeout from rent\n")
										.append("/rent_add - add month of rent\n")
										.append("/calc - return total amount for month\n")
										.append("/getrates - return all rates for rent\n")
										.append("/changerates - change rates for rent\n")
										.append("/gethistory - return total amount by months\n")
										.append("/purge - remove statistics for all months of rent and primary values\n")
										.append("/delmonthstat - remove statistics by month\n")
										.append("/getstat- return rent statistics for adding month\n")
										.append("/getstatbymonth - getting rent statistics by month\n")
										.append("/setprimarycounters- set starting indications")
										.toString();
								if (rentData.containsKey(id)) {
									rentData.remove(id);
								}
							}
								break;
							case "/rent_add": {
								List<List<String>> buttons = new ArrayList<>();
								List<String> buttonNames = null;

								for (int i = 0; i < 4; i++) {
									buttonNames = new ArrayList<>();
									buttons.add(buttonNames);
									switch (i) {
									case 0: {
										buttonNames.add("/setmonth");
										buttonNames.add("/setlight");
									}
										break;
									case 1: {
										buttonNames.add("/setwater");
										buttonNames.add("/settakeout");
									}
										break;
									case 2: {
										buttonNames.add("/getstat");
										buttonNames.add("/delmonthstat");
									}
										break;
									case 3: {
										buttonNames.add("/calc");
										buttonNames.add("/rent");
									}
										break;
									default:
										break;
									}
								}

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(this.objectMapper
										.writeValueAsString(getButtons(buttons)));

								rentData.put(id,
										Optional.of(new RentHolder(id, owner)));
								answer = "I'm ready for set indications";
							}
								break;
							case "/getrates": {
								answer = DataBaseHelper.getInstance().getRates(
										id);
								if (answer.length() == 0) {
									answer = "Rates are empty!";
								}
							}
								break;
							case "/changerates": {
								List<List<String>> buttons = new ArrayList<>();
								List<String> buttonNames = null;

								for (int i = 0; i < 6; i++) {
									buttonNames = new ArrayList<>();
									buttons.add(buttonNames);
									switch (i) {
									case 0: {
										buttonNames.add("/changehotwaterrate");
									}
										break;
									case 1: {
										buttonNames.add("/changecoldwaterrate");
									}
										break;
									case 2: {
										buttonNames.add("/changeoutfallrate");
									}
										break;
									case 3: {
										buttonNames.add("/changelightrate");
									}
										break;
									case 4: {
										buttonNames.add("/changerentamount");
									}
										break;
									case 5: {
										buttonNames.add("/rent");
									}
										break;
									default:
										break;
									}
								}

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(this.objectMapper
										.writeValueAsString(getButtons(buttons)));

								answer = new StringBuilder()
										.append("You can change next rates via follows command:\n\n")
										.append("/changehotwaterrate - set new hot water rate\n")
										.append("/changecoldwaterrate - set new cold water rate\n")
										.append("/changeoutfallrate - set new outfall rate\n")
										.append("/changelightrate - set new light rate\n")
										.append("/changerentamount - set new rent amount")
										.toString();
							}
								break;
							case "/changehotwaterrate":
								answer = "For change hot water rate send simple message with new hot water rate";
								break;
							case "/changecoldwaterrate":
								answer = "For change cold water rate send simple message with new cold water rate";
								break;
							case "/changelightrate":
								answer = "For change light rate please use this format:\n\nt1 - value\nt2 - value\nt3 - value";
								break;
							case "/changeoutfallrate":
								answer = "For change outfall rate send simple message with new outfall rate";
								break;
							case "/changerentamount":
								answer = "For change rent amount send simple message with new rent amount";
								break;
							case "/delmonthstat":
								answer = "Ok. Send simple message with month for deleting. Please use this format:\n\nshortmonthname_year\nExample:may_2016";
								break;

							case "/gethistory": {
								answer = DataBaseHelper.getInstance()
										.getHistory(id);
								if (!answer.contains("Month")) {
									answer = "History is empty!";
								}
							}
								break;
							case "/purge":
								if (DataBaseHelper.getInstance().purgeAll(id)) {
									answer = "All information about rent removed";
								} else {
									answer = "Oops error! Can't delete information!";
								}
								break;
							case "/getstat": {
								Optional<RentHolder> rentHolder = rentData
										.get(id);
								answer = (null != rentHolder) ? rentHolder
										.get().getStatAddedMonth()
										: "Rent mode is not active. For activate rent mode use /rent_add command";
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

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(this.objectMapper
										.writeValueAsString(getButtons(buttons)));

								answer = "OK. Send simple message with month. Please use this format:\n\nshortmonthname_year\nExample:may_2016";
							}
								break;
							case "/calc": {
								List<List<String>> buttons = new ArrayList<>();
								List<String> buttonNames = new ArrayList<>();
								buttons.add(buttonNames);
								buttonNames.add("yes");
								buttonNames.add("no");

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(this.objectMapper
										.writeValueAsString(getButtons(buttons)));

								answer = "OK. Send simple message with outfall status. Please use this format:\n\nif need outfall send yes or not no";
							}
								break;
							case "/settakeout":
								answer = "For set takeout information please use this format:\n\namount description\n\ndescription restriction - use underscore instead of space";
								break;
							case "/setmonth":
								answer = "For set rent month please use this format:\n\nshortmonthname_year\nExample:may_2016";
								break;
							case "/setlight":
								answer = "For set light indications please use this format:\n\nt1=value t2=value t3=value";
								break;
							case "/setwater":
								answer = "For set water indications please use this format:\n\nhot=value cold=value";
								break;
							case "/setprimarycounters":
								answer = "For set primary indications please use this format:\n\nt1=value t2=value t3=value hw=value cw=value t1rate=value t2rate=value t3rate=value hwrate=value cwrate=value outfallrate=value rentamount=value";
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

		String answer = "";

		switch (command) {
		case "/delone": {
			answer = (DataBaseHelper.getInstance().deleteAdaEvent(text)) ? "Event "
					+ text + " deleted successfully!"
					: "Can't delete event " + text;
		}
			break;
		case "/changehotwaterrate":
			setDefaultRentButtons(rh);
			if (DataBaseHelper.getInstance().updateRate(idChat, "hw_rate",
					Double.parseDouble(text))) {
				answer = "Hot water rate updated successfully!";
			} else {
				answer = "Can't update hot water rate! Ask my father to see logs(";
			}
			break;
		case "/changecoldwaterrate":
			setDefaultRentButtons(rh);
			if (DataBaseHelper.getInstance().updateRate(idChat, "cw_rate",
					Double.parseDouble(text))) {
				answer = "Cold water rate updated successfully!";
			} else {
				answer = "Can't update cold water rate! Ask my father to see logs(";
			}
			break;
		case "/changeoutfallrate":
			setDefaultRentButtons(rh);
			if (DataBaseHelper.getInstance().updateRate(idChat, "outfall_rate",
					Double.parseDouble(text))) {
				answer = "Outfall rate updated successfully!";
			} else {
				answer = "Can't update outfall rate! Ask my father to see logs(";
			}
			break;
		case "/changelightrate": {
			setDefaultRentButtons(rh);
			String[] data = text.split("-");
			String typeRate = data[0].trim().toLowerCase();

			switch (typeRate) {
			case "t1":
				if (DataBaseHelper.getInstance().updateRate(idChat, "t1_rate",
						Double.parseDouble(data[1].trim()))) {
					answer = "T1 rate updated successfully!";
				} else {
					answer = "Can't update T1 rate! Ask my father to see logs(";
				}
				break;
			case "t2":
				if (DataBaseHelper.getInstance().updateRate(idChat, "t2_rate",
						Double.parseDouble(data[1].trim()))) {
					answer = "T2 rate updated successfully!";
				} else {
					answer = "Can't update T2 rate! Ask my father to see logs(";
				}
				break;
			case "t3":
				if (DataBaseHelper.getInstance().updateRate(idChat, "t3_rate",
						Double.parseDouble(data[1].trim()))) {
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
		case "/changerentamount":
			setDefaultRentButtons(rh);
			if (DataBaseHelper.getInstance().updateRate(idChat, "rent_amount",
					Long.parseLong(text))) {
				answer = "Rent amount rate updated successfully!";
			} else {
				answer = "Can't update rent amount! Ask my father to see logs(";
			}
			break;
		case "/delmonthstat":
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
						.of(String.valueOf(rentHolder.get().getTotal(outfall)))
						: Optional.ofNullable(null);

				if (answerTemp.isPresent()) {
					answer = answerTemp.get() + " rub";
					rentData.remove(idChat);
				} else {
					answer = "Water and light didn't set. You need set water and light indications";
				}
			} else {
				answer = "Wrong format. For check format use command - /rent";
			}
		}
			break;
		case "/settakeout": {
			String[] data = text.split(" ");
			Optional<RentHolder> rentHolder = rentData.get(idChat);

			if (null != rentHolder) {
				if (data.length == 2) {
					rentHolder.get().setTakeout(Double.parseDouble(data[0]));
					rentHolder.get().setTakeoutDesc(data[1]);
					answer = "Takeout set successfully!";
				} else {
					answer = "Wrong format. For check format use command - /rent";
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
					rentHolder.get().setMonthRent(text);
					answer = "Month " + rentHolder.get().getMonthRent()
							+ " set successfully!";
				} else {
					answer = "Rent mode is not active. For activate rent mode use /rent_add command";
				}
			} else {
				answer = "Wrong format. For check format use command - /rent";
			}
		}
			break;
		case "/setlight": {
			String[] data = text.split(" ");
			Optional<RentHolder> rentHolder = rentData.get(String
					.valueOf(idChat));

			if (null != rentHolder) {
				if (data.length == 3) {
					rentHolder.get().setCountT1(
							Long.parseLong(data[0].split("=")[1]));
					rentHolder.get().setCountT2(
							Long.parseLong(data[1].split("=")[1]));
					rentHolder.get().setCountT3(
							Long.parseLong(data[2].split("=")[1]));
					answer = "Light set successfully! t1="
							+ rentHolder.get().getCountT1() + "; t2="
							+ rentHolder.get().getCountT2() + "; t3="
							+ rentHolder.get().getCountT3();
					rentHolder.get().setSetLight(true);
				} else {
					answer = "Wrong format. For check format use command - /rent";
				}
			} else {
				answer = "Rent mode is not active. For activate rent mode use /rent_add command";
			}
		}
			break;
		case "/setwater": {
			String[] data = text.split(" ");
			Optional<RentHolder> rentHolder = rentData.get(idChat);

			if (null != rentHolder) {
				if (data.length == 2) {
					rentHolder.get().setCountColdWater(
							Long.parseLong(data[1].split("=")[1]));
					rentHolder.get().setCountHotWater(
							Long.parseLong(data[0].split("=")[1]));
					answer = "Water set successfully! Hot="
							+ rentHolder.get().getCountHotWater() + "; Cold: "
							+ rentHolder.get().getCountColdWater();
					rentHolder.get().setSetWater(true);
				} else {
					answer = "Wrong format. For check format use command - /rent";
				}
			} else {
				answer = "Rent mode is not active. For activate rent mode use /rent_add command";
			}
		}
			break;
		case "/setprimarycounters": {

			String[] data = text.split(" ");

			int len = data.length;

			double t1Rate = 0.0, t2Rate = 0.0, t3Rate = 0.0, hwRate = 0.0, cwRate = 0.0, outFallRate = 0.0;
			long t1 = 0, t2 = 0, t3 = 0, hw = 0, cw = 0, rentAmount = 0;

			boolean flag = true;

			for (int j = 1; j < len; j++) {
				String temp = data[j].trim();

				if (temp.length() > 0 && temp.contains("=")) {

					String[] counter = temp.split("=");

					switch (counter[0]) {
					case "t1":
						t1 = Long.parseLong(counter[1]);
						break;
					case "t2":
						t2 = Long.parseLong(counter[1]);
						break;
					case "t3":
						t3 = Long.parseLong(counter[1]);
						break;
					case "hw":
						hw = Long.parseLong(counter[1]);
						break;
					case "cw":
						cw = Long.parseLong(counter[1]);
						break;
					case "t1rate":
						t1Rate = Double.parseDouble(counter[1]);
						break;
					case "t2rate":
						t2Rate = Double.parseDouble(counter[1]);
						break;
					case "t3rate":
						t3Rate = Double.parseDouble(counter[1]);
						break;
					case "cwrate":
						cwRate = Double.parseDouble(counter[1]);
						break;
					case "hwrate":
						hwRate = Double.parseDouble(counter[1]);
						break;
					case "outfallrate":
						outFallRate = Double.parseDouble(counter[1]);
						break;
					case "rentamount":
						rentAmount = Long.parseLong(counter[1]);
						break;
					default:
						break;
					}
				} else {
					answer = "Wrong input  primary values! For check format use command -  /rent";
					flag = false;
					break;
				}
			}

			if (flag) {

				PrimaryRatesHolder rent = new PrimaryRatesHolder.Builder()
						.setT1Start(t1).setT2Start(t2).setT3Start(t3)
						.setT3Rate(t3Rate).setT2Rate(t2Rate).setT1Rate(t1Rate)
						.setHwRate(hwRate).setCwRate(cwRate).setCwStart(cw)
						.setHwStart(hw).setOutFallRate(outFallRate)
						.setRentAmount(rentAmount).setIdChat(idChat)
						.setOwner(owner).build();

				logger.debug(rent.toString());

				boolean stat = DataBaseHelper.getInstance()
						.insertPrimaryCounters(rent);

				if (stat) {
					answer = "Primary indications set successfully!";
				} else {
					answer = "Can't set primary indications!";
				}
			}

		}
			break;
		default:
			answer = "Sorry, i can't answer to this message(";
			break;
		}

		activeCommand.remove(idChat);

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
		List<String> buttonNames = new ArrayList<>();
		buttons.add(buttonNames);

		buttonNames.add("/rent");
		buttonNames.add("/start");

		try {
			rh.setNeedReplyMarkup(true);
			rh.setReplyMarkup(this.objectMapper
					.writeValueAsString(getButtons(buttons)));
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
