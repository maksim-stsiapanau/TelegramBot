package ru.max.bot.telegram_api;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * This object represents a custom keyboard with reply options
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class ReplyKeyboardMarkup {

	/**
	 * Array of button rows, each represented by an Array of KeyboardButton
	 * objects
	 */
	private List<List<KeyboardButton>> keyboard;

	/**
	 * Optional. Requests clients to resize the keyboard vertically for optimal
	 * fit (e.g., make the keyboard smaller if there are just two rows of
	 * buttons). Defaults to false, in which case the custom keyboard is always
	 * of the same height as the app's standard keyboard.
	 * 
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Boolean resize_keyboard;

	/**
	 * Optional. Requests clients to hide the keyboard as soon as it's been
	 * used. The keyboard will still be available, but clients will
	 * automatically display the usual letter-keyboard in the chat – the user
	 * can press a special button in the input field to see the custom keyboard
	 * again. Defaults to false.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Boolean one_time_keyboard;

	/**
	 * Optional. Use this parameter if you want to show the keyboard to specific
	 * users only. Targets: 1) users that are @mentioned in the text of the
	 * Message object; 2) if the bot's message is a reply (has
	 * reply_to_message_id), sender of the original message.
	 * 
	 * Example: A user requests to change the bot‘s language, bot replies to the
	 * request with a keyboard to select the new language. Other users in the
	 * group don’t see the keyboard.
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	private Boolean selective;

	public ReplyKeyboardMarkup() {
	}

	public List<List<KeyboardButton>> getKeyboard() {
		return keyboard;
	}

	public void setKeyboard(List<List<KeyboardButton>> keyboard) {
		this.keyboard = keyboard;
	}

	public Boolean getResize_keyboard() {
		return resize_keyboard;
	}

	public void setResize_keyboard(Boolean resize_keyboard) {
		this.resize_keyboard = resize_keyboard;
	}

	public Boolean getOne_time_keyboard() {
		return one_time_keyboard;
	}

	public void setOne_time_keyboard(Boolean one_time_keyboard) {
		this.one_time_keyboard = one_time_keyboard;
	}

	public Boolean getSelective() {
		return selective;
	}

	public void setSelective(Boolean selective) {
		this.selective = selective;
	}

}
