package ru.max.bot.rent;

import lombok.Data;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

/**
 * Hold info about counter
 *
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
@Data
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

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
    }

}
