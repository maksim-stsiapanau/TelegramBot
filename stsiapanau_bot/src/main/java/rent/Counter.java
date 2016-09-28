package rent;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Hold info about counter
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class Counter {

	private Double rate;
	private Double used;
	private Double price;
	private String alias;

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

	public String getAlias() {
		return this.alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
