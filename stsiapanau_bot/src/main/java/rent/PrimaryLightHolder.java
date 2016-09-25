package rent;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Hold information about primary counters of light
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
@JsonIgnoreProperties({ "setRates", "setIndications" })
public class PrimaryLightHolder {

	private Integer tariffType;
	private String period;

	/**
	 * key - name value - indicator
	 */
	private final Map<String, Double> indications = new TreeMap<>();

	/**
	 * key - name value - tariff price
	 */
	private Map<String, Double> rates;

	public PrimaryLightHolder() {

	}

	public Map<String, Double> getIndications() {
		return this.indications;
	}

	public Map<String, Double> getRates() {
		return this.rates;
	}

	public Integer getTariffType() {
		return this.tariffType;
	}

	public void setTariffType(Integer tariffType) {
		this.tariffType = tariffType;
	}

	public boolean isSetIndications() {
		return (this.indications.size() == this.tariffType) ? true : false;
	}

	public boolean isSetRates() {
		return (this.rates.size() == this.tariffType) ? true : false;
	}

	public String getPeriod() {
		return this.period;
	}

	public void setPeriod(String period) {
		this.period = period;
	}

	public void initRates() {
		if (null == this.rates) {
			this.rates = new HashMap<>();
		}
	}

	public static enum Periods {
		DAY, NIGHT, PEAK, HALF_PEAK
	}

	public static enum PeriodsRus {
		ДЕНЬ, НОЧЬ, ПИК, ПОЛУПИК
	}
}
