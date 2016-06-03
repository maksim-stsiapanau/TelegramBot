package ada;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Ada's event holder
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class EventHolder {

	public final String eventName;

	public final String description;

	public final int chatId;

	public final String author;

	public EventHolder(String eventName, String description, int chatId,
			String author) {

		this.eventName = eventName;

		this.description = description;

		this.chatId = chatId;

		this.author = author;

	}

	public String getEventName() {
		return this.eventName;
	}

	public String getDescription() {
		return this.description;
	}

	public int getChatId() {
		return this.chatId;
	}

	public String getAuthor() {
		return this.author;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
