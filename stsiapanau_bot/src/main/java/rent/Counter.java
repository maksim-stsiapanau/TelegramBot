package rent;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

public class Counter {

	private Double rate;
	private Double used;
	private Double price;

	public Counter(Double rate, Double used, Double price) {
		this.rate = rate;
		this.used = used;
		this.price = price;
	}

	public Counter() {
	}

	public Double getRate() {
		return this.rate;
	}

	public Double getUsed() {
		return this.used;
	}

	public Double getPrice() {
		return this.price;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
