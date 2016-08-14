package rent;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

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
	private Long lastT1Count;
	private Long lastT2Count;
	private Long lastT3Count;
	private Long lastColdWaterCount;
	private Long lastHotWaterCount;
	private final String chatId;

	private RatesHolder(String chatId) {
		this.chatId = chatId;
		this.rentAmount = DataBaseHelper.getInstance()
				.getFirstValue("rent_const", "rent_amount",
						Filters.eq("id_chat", this.chatId));
		this.t1Rate = DataBaseHelper.getInstance().getFirstValue("rent_const",
				"t1_rate", Filters.eq("id_chat", this.chatId));
		this.t2Rate = DataBaseHelper.getInstance().getFirstValue("rent_const",
				"t2_rate", Filters.eq("id_chat", this.chatId));
		this.t3Rate = DataBaseHelper.getInstance().getFirstValue("rent_const",
				"t3_rate", Filters.eq("id_chat", this.chatId));
		this.coldWaterRate = DataBaseHelper.getInstance().getFirstValue(
				"rent_const", "cw_rate", Filters.eq("id_chat", this.chatId));
		this.hotWaterRate = DataBaseHelper.getInstance().getFirstValue(
				"rent_const", "hw_rate", Filters.eq("id_chat", this.chatId));
		this.outfallRate = DataBaseHelper.getInstance().getFirstValue(
				"rent_const", "outfall_rate",
				Filters.eq("id_chat", this.chatId));
		this.lastT1Count = DataBaseHelper.getInstance().getFirstValue(
				"rent_const", "t1_last", Filters.eq("id_chat", this.chatId));
		this.lastT2Count = DataBaseHelper.getInstance().getFirstValue(
				"rent_const", "t2_last", Filters.eq("id_chat", this.chatId));
		this.lastT3Count = DataBaseHelper.getInstance().getFirstValue(
				"rent_const", "t3_last", Filters.eq("id_chat", this.chatId));
		this.lastColdWaterCount = DataBaseHelper.getInstance().getFirstValue(
				"rent_const", "cw_last", Filters.eq("id_chat", this.chatId));
		this.lastHotWaterCount = DataBaseHelper.getInstance().getFirstValue(
				"rent_const", "hw_last", Filters.eq("id_chat", this.chatId));
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

	public Long getLastT1Count() {
		return lastT1Count;
	}

	public Long getLastT2Count() {
		return lastT2Count;
	}

	public Long getLastT3Count() {
		return lastT3Count;
	}

	public Long getLastColdWaterCount() {
		return lastColdWaterCount;
	}

	public Long getLastHotWaterCount() {
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
