package rent;

public class WaterHolder {

	private String typeOfWater;
	private String alias;
	private Double primaryIndication;

	public WaterHolder(String typeOfWater) {
		this.typeOfWater = typeOfWater;
		this.alias = typeOfWater;

	}

	public WaterHolder() {
	}

	public String getTypeOfWater() {
		return this.typeOfWater;
	}

	public String getAlias() {
		return this.alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	public Double getPrimaryIndication() {
		return this.primaryIndication;
	}

	public void setPrimaryIndication(Double primaryIndication) {
		this.primaryIndication = primaryIndication;
	}

}
