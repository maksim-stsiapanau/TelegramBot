package ru.max.bot.rent;

import java.util.Map;
import java.util.TreeMap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Hold information about primary counters of light
 *
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
@Data
@JsonIgnoreProperties({"setRates", "setIndications"})
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


    public boolean isSetIndications() {
        return (this.indications.size() == this.tariffType) ? true : false;
    }

    public boolean isSetRates() {
        return (this.rates.size() == this.tariffType) ? true : false;
    }


    public void initRates() {
        if (null == this.rates) {
            this.rates = new TreeMap<>();
        }
    }

    public enum Periods {
        DAY, NIGHT, PEAK, HALF_PEAK
    }

    public static enum PeriodsRus {
        ДЕНЬ, НОЧЬ, ПИК, ПОЛУПИК
    }
}
