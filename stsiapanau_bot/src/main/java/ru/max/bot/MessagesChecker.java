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
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rent.PrimaryRatesHolder;
import rent.RentHolder;

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
						o -> {

							Optional<IncomeMessage> message = Optional.empty();

							try {
								message = Optional.ofNullable(this.objectMapper
										.readValue(o, IncomeMessage.class));
							} catch (Exception e1) {
								logger.error(e1.getMessage(), e1);
							}

							boolean status = (message.isPresent()) ? message
									.get().getOk() : false;

							if (status) {
								List<Result> result = message.get().getResult();
								int resultSize = result.size();
								Integer updateId = 0;
								String id = null;
								String owner = "";

								if (resultSize > 0) {
									for (Result res : result) {

										try {
											updateId = res.getUpdateId();
											Optional<Message> messageType = Optional
													.ofNullable(res
															.getMessage());

											if (messageType.isPresent()) {
												String typeCommand = new String(
														"simple_message");

												List<Entity> entiies = messageType
														.get().getEntities();

												if (entiies.size() > 0) {
													typeCommand = entiies
															.get(0).getType();
												}

												Chat chat = messageType.get()
														.getChat();
												id = String.valueOf(chat
														.getId());

												owner = chat.getFirstName()
														+ " "
														+ chat.getLastName();

												logger.debug(
														"ChatId: %s; Owner: %s",
														id, owner);

												String text = null;
												text = messageType.get()
														.getText();
												text = text.trim()
														.toLowerCase();
												String answer = "I can't answer at this question(";

												if (typeCommand
														.equalsIgnoreCase("bot_command")) {

													// set active command
													activeCommand.put(id, text);

													switch (text) {
													case "/rent_add": {
														rentData.put(
																id,
																Optional.of(new RentHolder(
																		id,
																		owner)));
														answer = "I'm ready for set indications";
													}
														break;
													case "/getrates":
														answer = DataBaseHelper
																.getInstance()
																.getRates(id);
														if (answer.length() == 0) {
															answer = "Rates are empty!";
														}
														break;
													case "/start": {
														StringBuilder sb = new StringBuilder()
																.append("Hi. I am bot. I can process next operations:\n")
																.append("/rent - calculating rent per month\n");
														if (owner
																.equalsIgnoreCase("Maksim Stepanov")
																|| owner.equalsIgnoreCase("Yuliya Stepanova")) {
															sb.append("/ada -  events from Ada's life");
														}
														answer = sb.toString();
													}
														break;
													case "/getevents":
														answer = DataBaseHelper
																.getInstance()
																.getAllEvents();
														if (answer.length() == 0) {
															answer = "Events not found";
														} else {
															answer = "Ada's events:"
																	+ answer;
														}
														break;
													case "/ada":
														answer = new StringBuilder()
																.append("You can get or set events related with our Ada by sending these commands:\n")
																.append("/setevents - activate adding event mode. After you must send two simple messages: 1- description; 2- date of event and etc.\n")
																.append("/save - saved all added events\n")
																.append("/delone date_event - removed by date of event\n")
																.append("/delall - removed all events related with our Ada\n")
																.append("/getevents - get all events related with our Ada")
																.toString();
														break;
													case "/delall": {
														long countDel = DataBaseHelper
																.getInstance()
																.purgeAdaEvents();
														answer = (countDel != -1) ? countDel
																+ " Ada's events removed successfully!"
																: "Oops error! Can't delete Ada's events!";
													}
														break;
													case "/save": {
														adaMode.remove(id);
														StringBuilder sb = new StringBuilder();
														DataBaseHelper
																.getInstance()
																.saveAdaEvents(
																		id,
																		owner)
																.ifPresent(
																		e -> {
																			sb.append(
																					"Events: ")
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
														adaMode.put(id, true);
														adaEvents
																.put(id,
																		new ConcurrentLinkedQueue<>());
														answer = "Add mode activated successfully! I'm ready to get events)";
													}
														break;
													case "/rent": {
														answer = new StringBuilder()
																.append("You can control rent by sending these commands:\n")
																.append("/setmonth month_year (format: short_month_year - may_2016)\n")
																.append("/setlight t1=value t2=value t3=value - set indications for light\n")
																.append("/setwater hot=value cold=value - set indications for water, outfall calculating automatically\n")
																.append("/settakeout value description - set takeout from rent with description (description restriction - use underscore instead of space)\n")
																.append("/rent_add - add month of rent\n")
																.append("/calc outfall_flag - return total amount for month (outfall_flag - true or false)\n")
																.append("/getrates - return all rates for rent\n")
																.append("/changehotwaterrate - set new hot water rate\n")
																.append("/changecoldwaterrate - set new cold water rate\n")
																.append("/changeoutfallrate - set new outfall rate\n")
																.append("/changelightrate - set new light rate\n")
																.append("/changerentamount - set new rent amount\n")
																.append("/gethistory - return total amount by months\n")
																.append("/purge - remove statistics for all months of rent and primary values\n")
																.append("/delmonthstat - remove statistics for entered month\n")
																.append("/getstat- return rent statistics for adding month\n")
																.append("/getstatbymonth month (month short format - may_2016) - return rent statistics by month\n")
																.append("/setprimarycounters- set starting indications (format: /setprimarycounters t1=value t2=value t3=value hw=value cw=value t1rate=value t2rate=value t3rate=value hwrate=value cwrate=value outfallrate=value rentamount=value")
																.toString();
														if (rentData
																.containsKey(id)) {
															rentData.remove(id);
														}
													}
														break;
													case "/changehotwaterrate":
														answer = "OK. Send simple message with new hot water rate";
														break;
													case "/changecoldwaterrate":
														answer = "OK. Send simple message with new cold water rate";
														break;
													case "/changelightrate":
														answer = "OK. You have 3 rates of light. Please use this format:\n\nt1 - value\nt2 - value\nt3 - value";
														break;
													case "/changeoutfallrate":
														answer = "OK. Send simple message with new outfall rate";
														break;
													case "/changerentamount":
														answer = "OK. Send simple message with new rent amount";
														break;
													case "/delmonthstat":
														answer = "OK. Send simple message with month for deleting. Please use this format:\n\nshortmonthname_year\nExample:may_2016";
														break;

													case "/gethistory": {
														answer = DataBaseHelper
																.getInstance()
																.getHistory(id);
														if (!answer
																.contains("Month")) {
															answer = "History is empty!";
														}
													}
														break;
													case "/purge":
														if (DataBaseHelper
																.getInstance()
																.purgeAll(id)) {
															answer = "All information about rent removed";
														} else {
															answer = "Oops error! Can't delete information!";
														}
														break;
													case "/getstat": {
														Optional<RentHolder> rentHolder = rentData
																.get(id);
														answer = (null != rentHolder) ? rentHolder
																.get()
																.getStatAddedMonth()
																: "Rent mode is not active. For activate rent mode use /rent_add command";
													}
														break;
													default: {
														if (text.contains("delone")) {
															String eventDate = text
																	.split(" ")[1];
															answer = (DataBaseHelper
																	.getInstance()
																	.deleteAdaEvent(eventDate)) ? "Event "
																	+ eventDate
																	+ " deleted successfully!"
																	: "Can't delete event "
																			+ eventDate;
														}

														// check parts of
														// command
														if (text.contains("getstatbymonth")) {
															if (checkStrByRegexp(
																	text,
																	"/getstatbymonth [a-z]{3,4}_{1}[0-9]{4}$")) {
																answer = DataBaseHelper
																		.getInstance()
																		.getStatByMonth(
																				text.split(" ")[1],
																				id);
																if (answer
																		.length() == 0) {
																	answer = "Asked month not found! Try another month!)";
																}
															} else {
																answer = "Wrong format. For check format use command - /rent";
															}
														}

														if (text.contains("calc")) {
															if (checkStrByRegexp(
																	text,
																	"/calc [a-z]{4,5}$")) {
																Boolean outfall = Boolean
																		.valueOf((text
																				.split(" ")[1]));
																Optional<RentHolder> rentHolder = rentData
																		.get(id);
																Optional<String> answerTemp = (rentHolder != null) ? Optional
																		.of(String
																				.valueOf(rentHolder
																						.get()
																						.getTotal(
																								outfall)))
																		: Optional
																				.ofNullable(null);

																if (answerTemp
																		.isPresent()) {
																	answer = answerTemp
																			.get()
																			+ " rub";
																	rentData.remove(id);
																} else {
																	answer = "Water and light didn't set. You need set water and light indications";
																}
															} else {
																answer = "Wrong format. For check format use command - /rent";
															}
														}

														if (text.contains("takeout")) {
															String[] data = text
																	.split(" ");
															Optional<RentHolder> rentHolder = rentData
																	.get(id);

															if (null != rentHolder) {
																if (data.length == 3) {
																	rentHolder
																			.get()
																			.setTakeout(
																					Double.parseDouble(data[1]));
																	rentHolder
																			.get()
																			.setTakeoutDesc(
																					data[2]);
																	answer = "Takeout set successfully!";
																} else {
																	answer = "Wrong format. For check format use command - /rent";
																}
															} else {
																answer = "Rent mode is not active. For activate rent mode use /rent_add command";
															}
														}

														if (text.contains("setprimarycounters")) {
															String[] data = text
																	.split(" ");

															int len = data.length;

															double t1Rate = 0.0, t2Rate = 0.0, t3Rate = 0.0, hwRate = 0.0, cwRate = 0.0, outFallRate = 0.0;
															long t1 = 0, t2 = 0, t3 = 0, hw = 0, cw = 0, rentAmount = 0;

															boolean flag = true;

															for (int j = 1; j < len; j++) {
																String temp = data[j]
																		.trim();

																if (temp.length() > 0
																		&& temp.contains("=")) {

																	String[] counter = temp
																			.split("=");

																	switch (counter[0]) {
																	case "t1":
																		t1 = Long
																				.parseLong(counter[1]);
																		break;
																	case "t2":
																		t2 = Long
																				.parseLong(counter[1]);
																		break;
																	case "t3":
																		t3 = Long
																				.parseLong(counter[1]);
																		break;
																	case "hw":
																		hw = Long
																				.parseLong(counter[1]);
																		break;
																	case "cw":
																		cw = Long
																				.parseLong(counter[1]);
																		break;
																	case "t1rate":
																		t1Rate = Double
																				.parseDouble(counter[1]);
																		break;
																	case "t2rate":
																		t2Rate = Double
																				.parseDouble(counter[1]);
																		break;
																	case "t3rate":
																		t3Rate = Double
																				.parseDouble(counter[1]);
																		break;
																	case "cwrate":
																		cwRate = Double
																				.parseDouble(counter[1]);
																		break;
																	case "hwrate":
																		hwRate = Double
																				.parseDouble(counter[1]);
																		break;
																	case "outfallrate":
																		outFallRate = Double
																				.parseDouble(counter[1]);
																		break;
																	case "rentamount":
																		rentAmount = Long
																				.parseLong(counter[1]);
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
																		.setT1Start(
																				t1)
																		.setT2Start(
																				t2)
																		.setT3Start(
																				t3)
																		.setT3Rate(
																				t3Rate)
																		.setT2Rate(
																				t2Rate)
																		.setT1Rate(
																				t1Rate)
																		.setHwRate(
																				hwRate)
																		.setCwRate(
																				cwRate)
																		.setCwStart(
																				cw)
																		.setHwStart(
																				hw)
																		.setOutFallRate(
																				outFallRate)
																		.setRentAmount(
																				rentAmount)
																		.setIdChat(
																				id)
																		.setOwner(
																				owner)
																		.build();

																logger.debug(rent
																		.toString());

																boolean stat = DataBaseHelper
																		.getInstance()
																		.insertPrimaryCounters(
																				rent);

																if (stat) {
																	answer = "Primary indications set successfully!";
																} else {
																	answer = "Can't set primary indications!";
																}
															}
														}

														if (text.contains("setmonth")) {

															if (checkStrByRegexp(
																	text,
																	"/setmonth [a-z]{3,4}_{1}[0-9]{4}$")) {
																String[] data = text
																		.split(" ");
																Optional<RentHolder> rentHolder = rentData
																		.get(id);

																if (null != rentHolder) {
																	rentHolder
																			.get()
																			.setMonthRent(
																					data[1]);
																	answer = "Month "
																			+ rentHolder
																					.get()
																					.getMonthRent()
																			+ " set successfully!";
																} else {
																	answer = "Rent mode is not active. For activate rent mode use /rent_add command";
																}
															} else {
																answer = "Wrong format. For check format use command - /rent";
															}
														}

														if (text.contains("setlight")) {

															String[] data = text
																	.split(" ");

															Optional<RentHolder> rentHolder = rentData.get(String
																	.valueOf(id));

															if (null != rentHolder) {

																if (data.length == 4) {

																	rentHolder
																			.get()
																			.setCountT1(
																					Long.parseLong(data[1]
																							.split("=")[1]));
																	rentHolder
																			.get()
																			.setCountT2(
																					Long.parseLong(data[2]
																							.split("=")[1]));
																	rentHolder
																			.get()
																			.setCountT3(
																					Long.parseLong(data[3]
																							.split("=")[1]));
																	answer = "Light set successfully! t1="
																			+ rentHolder
																					.get()
																					.getCountT1()
																			+ "; t2="
																			+ rentHolder
																					.get()
																					.getCountT2()
																			+ "; t3="
																			+ rentHolder
																					.get()
																					.getCountT3();
																	rentHolder
																			.get()
																			.setSetLight(
																					true);
																} else {
																	answer = "Wrong format. For check format use command - /rent";
																}
															} else {
																answer = "Rent mode is not active. For activate rent mode use /rent_add command";
															}

														}

														if (text.contains("setwater")) {

															String[] data = text
																	.split(" ");

															Optional<RentHolder> rentHolder = rentData
																	.get(id);

															if (null != rentHolder) {

																if (data.length == 3) {

																	rentHolder
																			.get()
																			.setCountColdWater(
																					Long.parseLong(data[2]
																							.split("=")[1]));

																	rentHolder
																			.get()
																			.setCountHotWater(
																					Long.parseLong(data[1]
																							.split("=")[1]));

																	answer = "Water set successfully! Hot="
																			+ rentHolder
																					.get()
																					.getCountHotWater()
																			+ "; Cold: "
																			+ rentHolder
																					.get()
																					.getCountColdWater();

																	rentHolder
																			.get()
																			.setSetWater(
																					true);

																} else {
																	answer = "Wrong format. For check format use command - /rent";
																}
															} else {
																answer = "Rent mode is not active. For activate rent mode use /rent_add command";
															}
														}
													}
														break;
													}

												} else {

													// simple message. check Ada
													// mode

													Boolean flag = adaMode
															.get(id);

													if (null != flag
															&& adaMode.get(id)) {
														adaEvents.get(id).add(
																text);
														answer = "";
													} else {
														answer = processSimpleMessages(
																id,
																activeCommand
																		.get(id),
																text);
													}

												}

												if (answer.length() > 0) {
													callApiGet(
															"sendMessage?chat_id="
																	+ id
																	+ "&text="
																	+ URLEncoder
																			.encode(answer,
																					"UTF-8")
																	+ "",
															this.telegramApiUrl);
												}

											}
										} catch (Exception e) {
											callApiGet(
													"sendMessage?chat_id="
															+ id
															+ "&text=Oops error! Wrong operation! Say my daddy to see log. For check format use command /rent.)",
													this.telegramApiUrl);
											if (rentData.containsKey(id)) {
												rentData.remove(id);
											}
											logger.error(
													"Can't parse message! Error: %s",
													e.getMessage(), e);
										}

									}

									callApiGet("getUpdates?offset="
											+ (updateId + 1) + "",
											this.telegramApiUrl);
								}
							}
						});

	}

	private String processSimpleMessages(String idChat, String command,
			String text) {

		String answer = "";

		switch (command) {
		case "/changehotwaterrate":
			if (DataBaseHelper.getInstance().updateRate(idChat, "hw_rate",
					Double.parseDouble(text))) {
				answer = "Hot water rate updated successfully!";
			} else {
				answer = "Can't update hot water rate! Ask my father to see logs(";
			}
			break;
		case "/changecoldwaterrate":
			if (DataBaseHelper.getInstance().updateRate(idChat, "cw_rate",
					Double.parseDouble(text))) {
				answer = "Cold water rate updated successfully!";
			} else {
				answer = "Can't update cold water rate! Ask my father to see logs(";
			}
			break;
		case "/changeoutfallrate":
			if (DataBaseHelper.getInstance().updateRate(idChat, "outfall_rate",
					Double.parseDouble(text))) {
				answer = "Outfall rate updated successfully!";
			} else {
				answer = "Can't update outfall rate! Ask my father to see logs(";
			}
			break;
		case "/changelightrate":
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
			break;
		case "/changerentamount":
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

		default:
			break;
		}

		activeCommand.remove(idChat);

		return answer;
	}
}
