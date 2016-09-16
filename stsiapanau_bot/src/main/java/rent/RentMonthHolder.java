package rent;

import java.util.LinkedHashMap;
import java.util.Map;

public class RentMonthHolder {

	private Map<String, Counter> light;
	private Map<String, Counter> coldWater;
	private Map<String, Counter> hotWater;
	private Map<String, Counter> outfall;
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

	public Map<String, Counter> getColdWater() {
		if (null == this.coldWater) {
			this.coldWater = new LinkedHashMap<>();
		}
		return this.coldWater;
	}

	public Map<String, Counter> getHotWater() {
		if (null == this.hotWater) {
			this.hotWater = new LinkedHashMap<>();
		}
		return this.hotWater;
	}

	public Map<String, Counter> getOutfall() {
		if (null == this.outfall) {
			this.outfall = new LinkedHashMap<>();
		}
		return this.outfall;
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

		this.outfall.entrySet().stream().forEach(e -> {
			this.totalAmount += e.getValue().getPrice();
		});

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

	// public String getStatAddedMonth() {
	//
	// StringBuilder sb = new StringBuilder();
	//
	// sb.append("Added by: ").append(this.owner).append("\nMonth: ")
	// .append(this.monthRent).append("\nLight\n")
	// .append("T1 indication: ").append(this.countT1)
	// .append("; used: ").append(this.usedT1).append("; price: ")
	// .append(String.format("%.2f", this.priceT1)).append(" rub")
	// .append("\nT2 indication: ").append(this.countT2)
	// .append("; used: ").append(this.usedT2).append("; price: ")
	// .append(String.format("%.2f", this.priceT2)).append(" rub")
	// .append("\nT3 indication: ").append(this.countT3)
	// .append("; used: ").append(this.usedT3).append("; price: ")
	// .append(String.format("%.2f", this.priceT3)).append(" rub")
	// .append("\n\nWater\n").append("Hot water indication: ")
	// .append(this.countHotWater).append("; used: ")
	// .append(this.usedHotWater).append("; price: ")
	// .append(String.format("%.2f", this.priceHotWater))
	// .append(" rub").append("\nCold water indication: ")
	// .append(this.countColdWater).append("; used: ")
	// .append(this.usedColdWater).append("; price: ")
	// .append(String.format("%.2f", this.priceColdWater))
	// .append(" rub").append("\nOutfall water indication: ")
	// .append(this.countOutFall).append("; price: ")
	// .append(String.format("%.2f", this.priceOutFall))
	// .append(" rub").append("\n\nRent Amount: ")
	// .append(this.rentAmount).append(" rub").append("\nTotal: ")
	// .append(String.format("%.2f", this.total)).append(" rub");
	//
	// if (this.takeout > 0) {
	// sb.append("\nTakeout: ")
	// .append(String.format("%.2f", this.takeout)).append(" rub")
	// .append("\nTakeout desc: ").append(this.takeoutDesc);
	// }
	// return sb.toString();
	// }

}
