package ru.max.bot;

import java.util.ArrayList;
import java.util.List;

/**
 * Hold all response message with maxId for update queue
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class Response {

	private List<ResponseHolder> responses;
	private Integer maxUpdateId;

	public Response() {
	}

	public List<ResponseHolder> getResponses() {
		if (null == responses) {
			this.responses = new ArrayList<>();
		}
		return this.responses;
	}

	public void setResponses(List<ResponseHolder> responses) {
		this.responses = responses;
	}

	public Integer getMaxUpdateId() {
		return this.maxUpdateId;
	}

	public void setMaxUpdateId(Integer maxUpdateId) {
		this.maxUpdateId = maxUpdateId;
	}

}
