package rent;

import java.util.HashMap;

public class PrimaryLightHolder {

	/**
	 * key - name value - indicator
	 */
	private HashMap<String, String> indications;

	/**
	 * key - name value - tariff price
	 */
	private HashMap<String, String> rates;

	public PrimaryLightHolder() {

	}

	public HashMap<String, String> getIndications() {
		return this.indications;
	}

	public HashMap<String, String> getRates() {
		return this.rates;
	}

	public void setIndications(HashMap<String, String> indications) {
		this.indications = indications;
	}

	public void setRates(HashMap<String, String> rates) {
		this.rates = rates;
	}
}
