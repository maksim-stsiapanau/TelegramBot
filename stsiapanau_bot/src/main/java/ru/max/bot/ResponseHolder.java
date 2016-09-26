package ru.max.bot;

/**
 * Contains information about response message
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class ResponseHolder {

	private String responseMessage;
	private boolean needReplyMarkup;
	private String replyMarkup;
	private String chatId;

	public ResponseHolder() {
	}

	public String getResponseMessage() {
		return this.responseMessage;
	}

	public void setResponseMessage(String responseMessage) {
		this.responseMessage = responseMessage;
	}

	public boolean isNeedReplyMarkup() {
		return this.needReplyMarkup;
	}

	public void setNeedReplyMarkup(boolean needReplyMarkup) {
		this.needReplyMarkup = needReplyMarkup;
	}

	public String getReplyMarkup() {
		return this.replyMarkup;
	}

	public void setReplyMarkup(String replyMarkup) {
		this.replyMarkup = replyMarkup;
	}

	public String getChatId() {
		return this.chatId;
	}

	public void setChatId(String chatId) {
		this.chatId = chatId;
	}

}
