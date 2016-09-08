package rent;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;
import org.bson.Document;

import com.mongodb.client.model.Filters;

import db.DataBaseHelper;

/**
 * 
 * Hold rates of indications with and last indications
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class RatesHolder {

	private Long rentAmount;
	private Double t1Rate;
	private Double t2Rate;
	private Double t3Rate;
	private Double coldWaterRate;
	private Double hotWaterRate;
	private Double outfallRate;
	private Double lastT1Count;
	private Double lastT2Count;
	private Double lastT3Count;
	private Double lastColdWaterCount;
	private Double lastHotWaterCount;
	private final String chatId;

	private RatesHolder(String chatId) {
		this.chatId = chatId;

		Document doc = DataBaseHelper.getInstance().getFirstDocByFilter(
				"rent_const", Filters.eq("id_chat", this.chatId));

		this.rentAmount = doc.getLong("rent_amount");
		this.t1Rate = doc.getDouble("t1_rate");
		this.t2Rate = doc.getDouble("t2_rate");
		this.t3Rate = doc.getDouble("t3_rate");
		this.coldWaterRate = doc.getDouble("cw_rate");
		this.hotWaterRate = doc.getDouble("hw_rate");
		this.outfallRate = doc.getDouble("outfall_rate");
		this.lastT1Count = doc.getDouble("t1_last");
		this.lastT2Count = doc.getDouble("t2_last");
		this.lastT3Count = doc.getDouble("t3_last");
		this.lastColdWaterCount = doc.getDouble("cw_last");
		this.lastHotWaterCount = doc.getDouble("hw_last");
	}

	public static RatesHolder getInstance(String chatId) {
		return new RatesHolder(chatId);
	}

	public Long getRentAmount() {
		return rentAmount;
	}

	public Double getT1Rate() {
		return t1Rate;
	}

	public Double getT2Rate() {
		return t2Rate;
	}

	public Double getT3Rate() {
		return t3Rate;
	}

	public Double getColdWaterRate() {
		return coldWaterRate;
	}

	public Double getHotWaterRate() {
		return hotWaterRate;
	}

	public Double getOutfallRate() {
		return outfallRate;
	}

	public Double getLastT1Count() {
		return lastT1Count;
	}

	public Double getLastT2Count() {
		return lastT2Count;
	}

	public Double getLastT3Count() {
		return lastT3Count;
	}

	public Double getLastColdWaterCount() {
		return lastColdWaterCount;
	}

	public Double getLastHotWaterCount() {
		return lastHotWaterCount;
	}

	public String getChatId() {
		return chatId;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
