package ru.max.bot;

import static ru.max.bot.BotHelper.activeCommand;
import static ru.max.bot.BotHelper.adaEvents;
import static ru.max.bot.BotHelper.adaMode;
import static ru.max.bot.BotHelper.callApiGet;
import static ru.max.bot.BotHelper.checkStrByRegexp;
import static ru.max.bot.BotHelper.commandMapper;
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

						boolean isMapper = commandMapper.containsKey(text);

						String answer = "I can't answer at this question(";

						if (typeCommand.equalsIgnoreCase("bot_command")
								|| isMapper) {

							if (isMapper) {
								text = commandMapper.get(text);
							}

							// remove old command
							activeCommand.remove(id);

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
										.append("/rent (Rent) - calculating rent per month\n");

								if (owner.equalsIgnoreCase("Maksim Stepanov")
										|| owner.equalsIgnoreCase("Yuliya Stepanova")) {
									sb.append("/ada (Ada) -  events from Ada's life");
									buttonNames.add("Ada");
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
										buttonNames.add("Add events");
										buttonNames.add("See events");
									}
										break;
									case 1: {
										buttonNames.add("Remove event");
										buttonNames.add("Remove all events");
									}
										break;
									case 2: {
										buttonNames.add("Home");
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
								buttonNames.add("Save");

								rh.setNeedReplyMarkup(true);
								rh.setReplyMarkup(this.objectMapper
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
									rh.setReplyMarkup(this.objectMapper
											.writeValueAsString(getButtons(buttons)));
									answer = "Ok. Before using this command recommend check events via 'See events' command.\nChoose event date for deleting";
								} else {
									answer = "Not found events for deleting";
									activeCommand.remove(id);
								}

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

		String answer = "";

		switch (command) {
		case "/delone": {
			answer = (DataBaseHelper.getInstance().deleteAdaEvent(text)) ? "Event "
					+ text + " deleted successfully!"
					: "Can't delete event " + text;
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

		buttonNames.add("Back to rent menu");
		buttonNames.add("Home");

		try {
			rh.setNeedReplyMarkup(true);
			rh.setReplyMarkup(this.objectMapper
					.writeValueAsString(getButtons(buttons)));
		} catch (JsonProcessingException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
