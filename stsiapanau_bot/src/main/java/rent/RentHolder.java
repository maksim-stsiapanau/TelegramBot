package rent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import db.MonthSaver;

/**
 * 
 * Information about added month
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class RentHolder {

	private final String chatId;
	private final String owner;
	private Long rentAmount;
	private double T1Rate;
	private double T2Rate;
	private double T3Rate;
	private double coldWaterRate;
	private double hotWaterRate;
	private double outFallRate;
	private String monthRent;
	private double countT1;
	private double usedT1;
	private double priceT1;
	private double countT2;
	private double usedT2;
	private double priceT2;
	private double countT3;
	private double usedT3;
	private double priceT3;
	private double countColdWater;
	private double usedColdWater;
	private double priceColdWater;
	private double countHotWater;
	private double usedHotWater;
	private double priceHotWater;
	private double countOutFall;
	private double priceOutFall;
	private double lastT1Count;
	private double lastT2Count;
	private double lastT3Count;
	private double lastColdWaterCount;
	private double lastHotWaterCount;
	private Double total;
	private boolean setLight;
	private boolean setWater;
	private double takeout = 0.0;
	private String takeoutDesc;
	private RatesHolder rentConst;

	public RentHolder(String chatId, String owner) {
		this.chatId = chatId;
		this.owner = owner;
		this.setLight = false;
		this.setWater = false;
		this.rentConst = RatesHolder.getInstance(this.chatId);
		this.rentAmount = this.rentConst.getRentAmount();
		this.T1Rate = this.rentConst.getT1Rate();
		this.T2Rate = this.rentConst.getT2Rate();
		this.T3Rate = this.rentConst.getT3Rate();
		this.coldWaterRate = this.rentConst.getColdWaterRate();
		this.hotWaterRate = this.rentConst.getHotWaterRate();
		this.outFallRate = this.rentConst.getOutfallRate();
		this.lastColdWaterCount = this.rentConst.getLastColdWaterCount();
		this.lastHotWaterCount = this.rentConst.getLastHotWaterCount();
		this.lastT1Count = this.rentConst.getLastT1Count();
		this.lastT2Count = this.rentConst.getLastT2Count();
		this.lastT3Count = this.rentConst.getLastT3Count();
	}

	public Double getTotal(Boolean needOutfall) {

		if (this.setLight && this.setWater && this.total == null) {
			this.usedT1 = this.countT1 - this.lastT1Count;
			this.priceT1 = this.usedT1 * this.T1Rate;
			this.usedT2 = this.countT2 - this.lastT2Count;
			this.priceT2 = this.usedT2 * this.T2Rate;
			this.usedT3 = this.countT3 - this.lastT3Count;
			this.priceT3 = this.usedT3 * this.T3Rate;
			this.usedColdWater = this.countColdWater - this.lastColdWaterCount;
			this.priceColdWater = this.usedColdWater * this.coldWaterRate;
			this.usedHotWater = this.countHotWater - this.lastHotWaterCount;
			this.priceHotWater = this.usedHotWater * this.hotWaterRate;
			this.countOutFall = this.usedColdWater + this.usedHotWater;
			this.priceOutFall = (null != needOutfall && needOutfall) ? (this.outFallRate * this.countOutFall)
					: 0.0;
			this.total = this.priceColdWater + this.priceHotWater
					+ this.priceOutFall + this.priceT1 + this.priceT2
					+ this.priceT3 + this.rentAmount - this.takeout;

			// save month statistic to database
			ExecutorService exs = Executors.newSingleThreadExecutor();
			exs.execute(new MonthSaver(this));
			exs.shutdown();
		}
		return this.total;
	}

	public String getStatAddedMonth() {

		StringBuilder sb = new StringBuilder();

		sb.append("Added by: ").append(this.owner).append("\nMonth: ")
				.append(this.monthRent).append("\nLight\n")
				.append("T1 indication: ").append(this.countT1)
				.append("; used: ").append(this.usedT1).append("; price: ")
				.append(String.format("%.2f", this.priceT1)).append(" rub")
				.append("\nT2 indication: ").append(this.countT2)
				.append("; used: ").append(this.usedT2).append("; price: ")
				.append(String.format("%.2f", this.priceT2)).append(" rub")
				.append("\nT3 indication: ").append(this.countT3)
				.append("; used: ").append(this.usedT3).append("; price: ")
				.append(String.format("%.2f", this.priceT3)).append(" rub")
				.append("\n\nWater\n").append("Hot water indication: ")
				.append(this.countHotWater).append("; used: ")
				.append(this.usedHotWater).append("; price: ")
				.append(String.format("%.2f", this.priceHotWater))
				.append(" rub").append("\nCold water indication: ")
				.append(this.countColdWater).append("; used: ")
				.append(this.usedColdWater).append("; price: ")
				.append(String.format("%.2f", this.priceColdWater))
				.append(" rub").append("\nOutfall water indication: ")
				.append(this.countOutFall).append("; price: ")
				.append(String.format("%.2f", this.priceOutFall))
				.append(" rub").append("\n\nRent Amount: ")
				.append(this.rentAmount).append(" rub").append("\nTotal: ")
				.append(String.format("%.2f", this.total)).append(" rub");

		if (this.takeout > 0) {
			sb.append("\nTakeout: ")
					.append(String.format("%.2f", this.takeout)).append(" rub")
					.append("\nTakeout desc: ").append(this.takeoutDesc);
		}
		return sb.toString();
	}

	public long getRentAmount() {
		return this.rentAmount;
	}

	public void setRentAmount(long rentAmount) {
		this.rentAmount = rentAmount;
	}

	public double getT1Rate() {
		return this.T1Rate;
	}

	public void setT1Rate(double t1Rate) {
		T1Rate = t1Rate;
	}

	public double getT2Rate() {
		return this.T2Rate;
	}

	public void setT2Rate(int t2Rate) {
		T2Rate = t2Rate;
	}

	public double getT3Rate() {
		return this.T3Rate;
	}

	public void setT3Rate(double t3Rate) {
		T3Rate = t3Rate;
	}

	public double getColdWaterRate() {
		return this.coldWaterRate;
	}

	public void setColdWaterRate(double coldWaterRate) {
		this.coldWaterRate = coldWaterRate;
	}

	public double getHotWaterRate() {
		return this.hotWaterRate;
	}

	public void setHotWaterRate(double hotWaterRate) {
		this.hotWaterRate = hotWaterRate;
	}

	public double getOutFallRate() {
		return this.outFallRate;
	}

	public void setOutFallRate(double outFallRate) {
		this.outFallRate = outFallRate;
	}

	public String getMonthRent() {
		return this.monthRent;
	}

	public void setMonthRent(String monthRent) {
		this.monthRent = monthRent;
	}

	public double getCountT1() {
		return this.countT1;
	}

	public void setCountT1(long countT1) {
		this.countT1 = countT1;
	}

	public double getCountT2() {
		return this.countT2;
	}

	public void setCountT2(long countT2) {
		this.countT2 = countT2;
	}

	public double getCountT3() {
		return this.countT3;
	}

	public void setCountT3(long countT3) {
		this.countT3 = countT3;
	}

	public double getCountColdWater() {
		return this.countColdWater;
	}

	public void setCountColdWater(long countColdWater) {
		this.countColdWater = countColdWater;
	}

	public double getCountHotWater() {
		return this.countHotWater;
	}

	public void setCountHotWater(long countHotWater) {
		this.countHotWater = countHotWater;
	}

	public double getLastT1Count() {
		return lastT1Count;
	}

	public double getLastT2Count() {
		return this.lastT2Count;
	}

	public double getLastT3Count() {
		return this.lastT3Count;
	}

	public double getLastColdWaterCount() {
		return this.lastColdWaterCount;
	}

	public double getLastHotWaterCount() {
		return this.lastHotWaterCount;
	}

	public void setSetLight(boolean setLight) {
		this.setLight = setLight;
	}

	public void setSetWater(boolean setWater) {
		this.setWater = setWater;
	}

	public double getUsedT1() {
		return this.usedT1;
	}

	public double getPriceT1() {
		return priceT1;
	}

	public double getUsedT2() {
		return this.usedT2;
	}

	public double getPriceT2() {
		return this.priceT2;
	}

	public double getUsedT3() {
		return this.usedT3;
	}

	public double getPriceT3() {
		return this.priceT3;
	}

	public double getUsedColdWater() {
		return this.usedColdWater;
	}

	public double getPriceColdWater() {
		return this.priceColdWater;
	}

	public double getUsedHotWater() {
		return this.usedHotWater;
	}

	public double getPriceHotWater() {
		return this.priceHotWater;
	}

	public double getCountOutFall() {
		return this.countOutFall;
	}

	public double getPriceOutFall() {
		return this.priceOutFall;
	}

	public void setLastT1Count(long lastT1Count) {
		this.lastT1Count = lastT1Count;
	}

	public void setLastT2Count(long lastT2Count) {
		this.lastT2Count = lastT2Count;
	}

	public void setLastT3Count(long lastT3Count) {
		this.lastT3Count = lastT3Count;
	}

	public void setLastColdWaterCount(long lastColdWaterCount) {
		this.lastColdWaterCount = lastColdWaterCount;
	}

	public void setLastHotWaterCount(long lastHotWaterCount) {
		this.lastHotWaterCount = lastHotWaterCount;
	}

	public double getTakeout() {
		return this.takeout;
	}

	public void setTakeout(double takeout) {
		this.takeout = takeout;
	}

	public String getTakeoutDesc() {
		return this.takeoutDesc;
	}

	public void setTakeoutDesc(String takeoutDesc) {
		this.takeoutDesc = takeoutDesc;
	}

	public String getOwner() {
		return this.owner;
	}

	public String getIdChat() {
		return this.chatId;
	}

	public RatesHolder getRentConst() {
		return this.rentConst;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}
}
