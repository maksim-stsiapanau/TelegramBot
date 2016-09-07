package ru.max.bot;

import java.util.ArrayList;
import java.util.List;

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
