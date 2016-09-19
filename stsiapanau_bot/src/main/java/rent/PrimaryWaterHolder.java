package rent;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * 
 * this.objectMapper.configure(
 * DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
@JsonIgnoreProperties({ "typeOfWater", "typeOfRates", "waterSet", "ratesSet",
		"waitValue", "setWaterIndications", "setRates" })
public class PrimaryWaterHolder {

	private Integer countHotWaterCounter;
	private Integer countColdWaterCounter;
	private String typeOfWater;
	private String typeOfRates;
	private boolean waterSet;
	private boolean ratesSet;
	private boolean waitValue;
	private Double hotWaterRate;
	private Double coldWaterRate;
	private Double outfallRate;
	private Map<Integer, WaterHolder> hotWater;
	private Map<Integer, WaterHolder> coldWater;

	public PrimaryWaterHolder() {
	}

	public String getTypeOfWater() {
		return typeOfWater;
	}

	public void setTypeOfWater(String typeOfWater) {
		this.typeOfWater = typeOfWater;
	}

	public boolean isWaterSet() {
		return waterSet;
	}

	public void setWaterSet(boolean setWater) {
		this.waterSet = setWater;
	}

	public Integer getCountHotWaterCounter() {
		return countHotWaterCounter;
	}

	public void setCountHotWaterCounter(Integer countHotWaterCounter) {
		this.countHotWaterCounter = countHotWaterCounter;
	}

	public Integer getCountColdWaterCounter() {
		return countColdWaterCounter;
	}

	public void setCountColdWaterCounter(Integer countColdWaterCounter) {
		this.countColdWaterCounter = countColdWaterCounter;
	}

	public Double getHotWaterRate() {
		return hotWaterRate;
	}

	public void setHotWaterRate(Double hotWaterRate) {
		this.hotWaterRate = hotWaterRate;
	}

	public Double getColdWaterRate() {
		return coldWaterRate;
	}

	public void setColdWaterRate(Double coldWaterRate) {
		this.coldWaterRate = coldWaterRate;
	}

	public Double getOutfallRate() {
		return outfallRate;
	}

	public void setOutfallRate(Double outfallRate) {
		this.outfallRate = outfallRate;
	}

	public Map<Integer, WaterHolder> getHotWater() {
		return hotWater;
	}

	public void setHotWater() {
		if (null == this.hotWater) {
			this.hotWater = new TreeMap<>();
		}
	}

	public Map<Integer, WaterHolder> getColdWater() {
		return coldWater;
	}

	public void setColdWater() {
		if (null == this.coldWater) {
			this.coldWater = new TreeMap<>();
		}
	}

	public boolean isWaitValue() {
		return waitValue;
	}

	public void setWaitValue(boolean waitValue) {
		this.waitValue = waitValue;
	}

	public boolean isSetRates() {
		return (this.hotWaterRate != null && this.coldWaterRate != null && this.outfallRate != null) ? true
				: false;
	}

	public boolean isSetWaterIndications() {
		return (this.coldWater != null && this.hotWater != null
				&& this.coldWater.size() == this.countColdWaterCounter && this.hotWater
					.size() == this.countHotWaterCounter) ? true : false;
	}

	public boolean isRatesSet() {
		return ratesSet;
	}

	public void setRatesSet(boolean ratesSet) {
		this.ratesSet = ratesSet;
	}

	public String getTypeOfRates() {
		return typeOfRates;
	}

	public void setTypeOfRates(String typeOfRates) {
		this.typeOfRates = typeOfRates;
	}
}
