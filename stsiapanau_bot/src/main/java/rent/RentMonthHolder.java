package rent;

import java.util.LinkedHashMap;
import java.util.Map;

public class RentMonthHolder {

	private Map<String, Counter> light;
	private Map<Integer, Counter> coldWater;
	private Map<Integer, Counter> hotWater;
	private Counter outfall;
	private Double takeout;
	private String takeoutDesc;
	private String month;
	private Double totalAmount;
	private Double rentAmount;
	private LastIndicationsHolder lastIndications;
	private String chatId;
	private String owner;

	public RentMonthHolder(String chatId, String owner) {
		this.chatId = chatId;
		this.owner = owner;
	}

	public RentMonthHolder() {
	}

	public Map<String, Counter> getLight() {
		if (null == this.light) {
			this.light = new LinkedHashMap<>();
		}
		return this.light;
	}

	public Map<Integer, Counter> getColdWater() {
		if (null == this.coldWater) {
			this.coldWater = new LinkedHashMap<>();
		}
		return this.coldWater;
	}

	public Map<Integer, Counter> getHotWater() {
		if (null == this.hotWater) {
			this.hotWater = new LinkedHashMap<>();
		}
		return this.hotWater;
	}

	public Counter getOutfall() {
		return this.outfall;
	}

	public void setOutfall(Counter outfall) {
		this.outfall = outfall;
	}

	public Double getTakeout() {
		return this.takeout;
	}

	public void setTakeout(Double takeout) {
		this.takeout = takeout;
	}

	public String getTakeoutDesc() {
		return this.takeoutDesc;
	}

	public void setTakeoutDesc(String takeoutDesc) {
		this.takeoutDesc = takeoutDesc;
	}

	public String getMonth() {
		return this.month;
	}

	public void setMonth(String month) {
		this.month = month;
	}

	public Double getTotalAmount() {
		this.totalAmount = new Double(0.0);

		this.light.entrySet().stream().forEach(e -> {
			this.totalAmount += e.getValue().getPrice();
		});

		this.coldWater.entrySet().stream().forEach(e -> {
			this.totalAmount += e.getValue().getPrice();
		});

		this.hotWater.entrySet().stream().forEach(e -> {
			this.totalAmount += e.getValue().getPrice();
		});

		if (null != this.outfall) {
			this.totalAmount += this.outfall.getPrice();
		}

		this.totalAmount += this.rentAmount;

		if (null != this.takeout) {
			this.totalAmount -= this.takeout;
		}

		return this.totalAmount;
	}

	public Double getRentAmount() {
		return this.rentAmount;
	}

	public void setRentAmount(Double rentAmount) {
		this.rentAmount = rentAmount;
	}

	public LastIndicationsHolder getLastIndications() {
		return this.lastIndications;
	}

	public void setLastIndications(LastIndicationsHolder lastIndications) {
		this.lastIndications = lastIndications;
	}

	public String getChatId() {
		return this.chatId;
	}

	public String getOwner() {
		return this.owner;
	}

}
