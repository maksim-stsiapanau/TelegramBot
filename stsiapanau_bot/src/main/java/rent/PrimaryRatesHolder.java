package rent;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * 
 * Hold starting indications of T1,T2,T3,HW(hot water),CW(cold water) ,rates and
 * rent amount
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class PrimaryRatesHolder {

	private final double t1Start;
	private final double t2Start;
	private final double t3Start;
	private final double hwStart;
	private final double cwStart;
	private final double t1Rate;
	private final double t2Rate;
	private final double t3Rate;
	private final double hwRate;
	private final double cwRate;
	private final double outFallRate;
	private final long rentAmount;
	private final String idChat;
	private final String owner;

	public PrimaryRatesHolder(Builder builder) {
		this.t1Start = builder.t1Start;
		this.t2Start = builder.t2Start;
		this.t3Start = builder.t3Start;
		this.t1Rate = builder.t1Rate;
		this.t2Rate = builder.t2Rate;
		this.t3Rate = builder.t3Rate;
		this.hwRate = builder.hwRate;
		this.hwStart = builder.hwStart;
		this.cwRate = builder.cwRate;
		this.cwStart = builder.cwStart;
		this.outFallRate = builder.outFallRate;
		this.rentAmount = builder.rentAmount;
		this.idChat = builder.idChat;
		this.owner = builder.owner;
	}

	public static class Builder {

		public double t1Start;
		public double t2Start;
		public double t3Start;
		public double hwStart;
		public double cwStart;
		public double t1Rate;
		public double t2Rate;
		public double t3Rate;
		public double hwRate;
		public double cwRate;
		public double outFallRate;
		public long rentAmount;
		public String idChat;
		public String owner;

		public Builder() {

		}

		public Builder setT1Start(double t1Start) {
			this.t1Start = t1Start;
			return this;
		}

		public Builder setT2Start(double t2Start) {
			this.t2Start = t2Start;
			return this;
		}

		public Builder setT3Start(double t3Start) {
			this.t3Start = t3Start;
			return this;
		}

		public Builder setHwStart(double hwStart) {
			this.hwStart = hwStart;
			return this;
		}

		public Builder setCwStart(double cwStart) {
			this.cwStart = cwStart;
			return this;
		}

		public Builder setT1Rate(double t1Rate) {
			this.t1Rate = t1Rate;
			return this;
		}

		public Builder setT2Rate(double t2Rate) {
			this.t2Rate = t2Rate;
			return this;
		}

		public Builder setT3Rate(double t3Rate) {
			this.t3Rate = t3Rate;
			return this;
		}

		public Builder setHwRate(double hwRate) {
			this.hwRate = hwRate;
			return this;
		}

		public Builder setCwRate(double cwRate) {
			this.cwRate = cwRate;
			return this;
		}

		public Builder setOutFallRate(double outFallRate) {
			this.outFallRate = outFallRate;
			return this;
		}

		public Builder setRentAmount(long rentAmount) {
			this.rentAmount = rentAmount;
			return this;
		}

		public Builder setIdChat(String idChat) {
			this.idChat = idChat;
			return this;
		}

		public Builder setOwner(String owner) {
			this.owner = owner;
			return this;
		}

		public PrimaryRatesHolder build() {

			return new PrimaryRatesHolder(this);
		}

	}

	public double getT1Start() {
		return t1Start;
	}

	public double getT2Start() {
		return t2Start;
	}

	public double getT3Start() {
		return t3Start;
	}

	public double getHwStart() {
		return hwStart;
	}

	public double getCwStart() {
		return cwStart;
	}

	public double getT1Rate() {
		return t1Rate;
	}

	public double getT2Rate() {
		return t2Rate;
	}

	public double getT3Rate() {
		return t3Rate;
	}

	public double getHwRate() {
		return hwRate;
	}

	public double getCwRate() {
		return cwRate;
	}

	public double getOutFallRate() {
		return outFallRate;
	}

	public long getRentAmount() {
		return rentAmount;
	}

	public String getIdChat() {
		return idChat;
	}

	public String getOwner() {
		return owner;
	}

	@Override
	public String toString() {
		return ToStringBuilder.reflectionToString(this,
				ToStringStyle.SHORT_PREFIX_STYLE);
	}

}
