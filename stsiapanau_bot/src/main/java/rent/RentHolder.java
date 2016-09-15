package rent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.client.model.Filters;

import db.DataBaseHelper;

public class RentHolder {

	private PrimaryLightHolder lightPrimary;
	private PrimaryWaterHolder waterPrimary;
	private LastIndicationsHolder lastIndications;
	private final String chatId;
	private final String owner;
	private Map<String, Double> currentLightIndications;
	private Map<Integer, WaterHolder> currentHotWaterIndications;
	private Map<Integer, WaterHolder> currentColdWaterIndications;
	private Double takeout;
	private String takeoutDescription;
	private String monthOfRent;
	private String lightTypeActive;
	private String waterTypeActive;
	private List<List<String>> waterButtons;
	private String buttonWaterCounterActive;

	public RentHolder(String chatId, String owner, ObjectMapper mapper) {
		this.chatId = chatId;
		this.owner = owner;
	}

	public void initIndications(ObjectMapper mapper) throws JsonParseException,
			JsonMappingException, IOException {

		this.lightPrimary = mapper.readValue(
				(String) DataBaseHelper.getInstance().getFirstValue(
						"rent_const", "light",
						Filters.eq("id_chat", this.chatId)),
				PrimaryLightHolder.class);
		this.waterPrimary = mapper.readValue(
				(String) DataBaseHelper.getInstance().getFirstValue(
						"rent_const", "water",
						Filters.eq("id_chat", this.chatId)),
				PrimaryWaterHolder.class);
		this.lastIndications = mapper.readValue(
				(String) DataBaseHelper.getInstance().getFirstValue(
						"rent_const", "last_indications",
						Filters.eq("id_chat", this.chatId)),
				LastIndicationsHolder.class);
		this.currentLightIndications = new TreeMap<>();
		this.currentColdWaterIndications = new TreeMap<>();
		this.currentHotWaterIndications = new TreeMap<>();

	}

	public Double getTotal(Boolean needOutfall) {

		Double total = null;
		
		

		//if (isLightSet() && isWaterSet() && this.monthOfRent != null) {

			this.currentColdWaterIndications
					.entrySet()
					.stream()
					.forEach(
							e -> {
								Integer key = e.getKey();

								Double used = e.getValue()
										.getPrimaryIndication()
										- this.lastIndications.getColdWater()
												.get(key)
												.getPrimaryIndication();
								System.out.println("Used for "
										+ e.getValue().getAlias() + " "
										+ (used));

								System.out.println("Price for "
										+ e.getValue().getAlias()
										+ " "
										+ (used * this.waterPrimary
												.getColdWaterRate()));

							});

			this.currentHotWaterIndications
					.entrySet()
					.stream()
					.forEach(
							e -> {
								Integer key = e.getKey();

								Double used = e.getValue()
										.getPrimaryIndication()
										- this.lastIndications.getHotWater()
												.get(key)
												.getPrimaryIndication();
								System.out.println("Used for "
										+ e.getValue().getAlias() + " " + used);

								System.out.println("Price for "
										+ e.getValue().getAlias()
										+ " "
										+ (used * this.waterPrimary
												.getHotWaterRate()));

							});

//			this.usedT1 = this.countT1 - this.lastT1Count;
//			this.priceT1 = this.usedT1 * this.T1Rate;
//			this.usedT2 = this.countT2 - this.lastT2Count;
//			this.priceT2 = this.usedT2 * this.T2Rate;
//			this.usedT3 = this.countT3 - this.lastT3Count;
//			this.priceT3 = this.usedT3 * this.T3Rate;
//
//			
//
//			
//			this.countOutFall = this.usedColdWater + this.usedHotWater;
//			this.priceOutFall = (null != needOutfall && needOutfall) ? (this.outFallRate * this.countOutFall)
//					: 0.0;
//			this.total = this.priceColdWater + this.priceHotWater
//					+ this.priceOutFall + this.priceT1 + this.priceT2
//					+ this.priceT3 + this.rentAmount - this.takeout;

			// save month statistic to database
			// ExecutorService exs = Executors.newSingleThreadExecutor();
			// exs.execute(new MonthSaver(this));
			// exs.shutdown();
		//}
		return total;
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

	
	public Double getTakeout() {
		return this.takeout;
	}

	public void setTakeout(Double takeout) {
		this.takeout = takeout;
	}

	public String getTakeoutDescription() {
		return this.takeoutDescription;
	}

	public void setTakeoutDescription(String takeoutDescription) {
		this.takeoutDescription = takeoutDescription;
	}

	public String getMonthOfRent() {
		return this.monthOfRent;
	}

	public void setMonthOfRent(String monthOfRent) {
		this.monthOfRent = monthOfRent;
	}

	public String getOwner() {
		return this.owner;
	}

	public PrimaryLightHolder getLightPrimary() {
		return this.lightPrimary;
	}

	public PrimaryWaterHolder getWaterPrimary() {
		return this.waterPrimary;
	}

	public Map<String, Double> getCurrentLightIndications() {
		return this.currentLightIndications;
	}

	public Map<Integer, WaterHolder> getCurrentHotWaterIndications() {
		return this.currentHotWaterIndications;
	}

	public Map<Integer, WaterHolder> getCurrentColdWaterIndications() {
		return this.currentColdWaterIndications;
	}

	public String getLightTypeActive() {
		return this.lightTypeActive;
	}

	public void setLightTypeActive(String lightTypeActive) {
		this.lightTypeActive = lightTypeActive;
	}

	public boolean isLightSet() {
		return (this.lightPrimary.getIndications().size() == this.currentLightIndications
				.size()) ? true : false;
	}

	public boolean isWaterSet() {
		return (this.waterPrimary.getColdWater().size() == this.currentColdWaterIndications
				.size() && this.waterPrimary.getHotWater().size() == this.currentHotWaterIndications
				.size()) ? true : false;
	}

	public String getWaterTypeActive() {
		return waterTypeActive;
	}

	public void setWaterTypeActive(String waterTypeActive) {
		this.waterTypeActive = waterTypeActive;
	}

	public List<List<String>> getWaterButtons() {
		if (null == this.waterButtons) {
			this.waterButtons = new ArrayList<List<String>>();
		}
		return this.lastIndications.copyList(this.waterButtons);
	}

	public void setWaterButtons(List<List<String>> waterButtons) {
		this.waterButtons = this.lastIndications.copyList(waterButtons);
	}

	public String getButtonWaterCounterActive() {
		return buttonWaterCounterActive;
	}

	public void setButtonWaterCounterActive(String buttonWaterCounterActive) {
		this.buttonWaterCounterActive = buttonWaterCounterActive;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
