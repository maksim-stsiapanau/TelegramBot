package ru.max.bot.rent;

import static ru.max.bot.tools.BotHelper.objectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import lombok.Data;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import com.mongodb.client.model.Filters;

import ru.max.bot.database.DataBaseHelper;

@Data
public class RentHolder {

    private PrimaryLightHolder lightPrimary;
    private PrimaryWaterHolder waterPrimary;
    private LastIndicationsHolder lastIndications;
    private final String chatId;
    private final String owner;
    private Map<String, Double> currentLightIndications;
    private Map<Integer, WaterHolder> currentHotWaterIndications;
    private Map<Integer, WaterHolder> currentColdWaterIndications;
    private Double takeout;
    private String takeoutDescription;
    private String monthOfRent;
    private String lightTypeActive;
    private String waterTypeActive;
    private List<List<String>> waterButtons;
    private String buttonWaterCounterActive;
    private Double rentAmount;

    // need if user decides change indicators
    private Map<String, Integer> addedWater;

    public RentHolder(String chatId, String owner) {
        this.chatId = chatId;
        this.owner = owner;
    }

    public void initIndications() throws
            IOException {

        this.lightPrimary = objectMapper.readValue(
                (String) DataBaseHelper.getInstance().getFirstValue(
                        "rent_const", "light",
                        Filters.eq("id_chat", this.chatId)),
                PrimaryLightHolder.class);
        this.waterPrimary = objectMapper.readValue(
                (String) DataBaseHelper.getInstance().getFirstValue(
                        "rent_const", "water",
                        Filters.eq("id_chat", this.chatId)),
                PrimaryWaterHolder.class);
        this.lastIndications = objectMapper.readValue(
                (String) DataBaseHelper.getInstance().getFirstValue(
                        "rent_const", "last_indications",
                        Filters.eq("id_chat", this.chatId)),
                LastIndicationsHolder.class);
        this.rentAmount = DataBaseHelper.getInstance()
                .getFirstValue("rent_const", "rent_amount",
                        Filters.eq("id_chat", this.chatId));
        this.currentLightIndications = new TreeMap<>();
        this.currentColdWaterIndications = new TreeMap<>();
        this.currentHotWaterIndications = new TreeMap<>();
        this.addedWater = new LinkedHashMap<>();

    }

    /**
     * Calculate total amount for rent month and save to database
     *
     * @param needOutfall - flag for include ootfall to total amount or not
     * @return - total amount for month
     */
    public Double getTotalAmount(Boolean needOutfall) {

        RentMonthHolder totalObj = new RentMonthHolder(this.chatId, this.owner);

        if (isLightSet() && isWaterSet() && this.monthOfRent != null) {

            LastIndicationsHolder lastInds = new LastIndicationsHolder();
            lastInds.setColdWater(this.currentColdWaterIndications);

            this.currentColdWaterIndications
                    .entrySet()
                    .stream()
                    .forEach(
                            e -> {
                                Integer key = e.getKey();
                                String alias = e.getValue().getAlias();
                                Double rate = this.waterPrimary
                                        .getColdWaterRate();

                                Integer lastKey = null;

                                if (null != alias) {
                                    for (Entry<Integer, WaterHolder> entry : this.lastIndications
                                            .getColdWater().entrySet()) {
                                        Integer keyEntry = entry.getKey();
                                        if (entry.getValue().getAlias()
                                                .equalsIgnoreCase(alias)) {
                                            lastKey = keyEntry;
                                        }
                                    }
                                }

                                Double used = e.getValue()
                                        .getPrimaryIndication()
                                        - this.lastIndications
                                        .getColdWater()
                                        .get((lastKey == null) ? key
                                                : lastKey)
                                        .getPrimaryIndication();
                                Double price = used * rate;

                                Counter counter = new Counter(rate, used, price);
                                counter.setAlias(alias);
                                totalObj.getColdWater().put(key, counter);
                            });

            lastInds.setHotWater(this.currentHotWaterIndications);

            this.currentHotWaterIndications
                    .entrySet()
                    .stream()
                    .forEach(
                            e -> {
                                Integer key = e.getKey();
                                String alias = e.getValue().getAlias();
                                Double rate = this.waterPrimary
                                        .getHotWaterRate();

                                Integer lastKey = null;

                                if (null != alias) {
                                    for (Entry<Integer, WaterHolder> entry : this.lastIndications
                                            .getHotWater().entrySet()) {
                                        Integer keyEntry = entry.getKey();
                                        if (entry.getValue().getAlias()
                                                .equalsIgnoreCase(alias)) {
                                            lastKey = keyEntry;
                                        }
                                    }
                                }

                                Double used = e.getValue()
                                        .getPrimaryIndication()
                                        - this.lastIndications
                                        .getHotWater()
                                        .get((lastKey == null) ? key
                                                : lastKey)
                                        .getPrimaryIndication();
                                Double price = used * rate;

                                Counter counter = new Counter(rate, used, price);
                                counter.setAlias(alias);
                                totalObj.getHotWater().put(key, counter);
                            });

            if (needOutfall) {
                Double outfallCount = 0.0;

                for (Entry<Integer, Counter> entry : totalObj.getColdWater()
                        .entrySet()) {
                    outfallCount += entry.getValue().getUsed();
                }

                for (Entry<Integer, Counter> entry : totalObj.getHotWater()
                        .entrySet()) {
                    outfallCount += entry.getValue().getUsed();
                }

                Double outfallRate = this.waterPrimary.getOutfallRate();

                totalObj.setOutfall(new Counter(outfallRate, outfallCount,
                        outfallCount * outfallRate));
            }

            lastInds.setLight(this.currentLightIndications);

            this.currentLightIndications
                    .entrySet()
                    .stream()
                    .forEach(
                            e -> {
                                String key = e.getKey();
                                Double rate = this.lightPrimary.getRates().get(
                                        key);
                                Double used = e.getValue()
                                        - this.lastIndications.getLight().get(
                                        key);
                                Double price = used * rate;
                                totalObj.getLight().put(key,
                                        new Counter(rate, used, price));
                            });

            if (this.takeout != null) {
                totalObj.setTakeout(this.takeout);
                totalObj.setTakeoutDesc(this.takeoutDescription);
            }

            totalObj.setMonth(this.monthOfRent);
            totalObj.setRentAmount(this.rentAmount);
            totalObj.setLastIndications(lastInds);

            // save month statistic to database
            ExecutorService es = Executors.newSingleThreadExecutor();
            es.execute(new Runnable() {

                @Override
                public void run() {
                    DataBaseHelper.getInstance().insertMonthStat(totalObj);

                }
            });
            es.shutdown();

        }

        return totalObj.getTotalAmount();
    }

    public String getStatAddedMonth(boolean isRus) {

        StringBuilder sb = new StringBuilder();

        sb.append((isRus) ? "<b>Автор:</b> " : "<b>Added by:</b> ")
                .append(this.owner)
                .append((isRus) ? "\n<b>Месяц:</b> " : "\n<b>Month:</b> ")
                .append(this.monthOfRent)
                .append((isRus) ? "\n\n<b>Электричество</b>\n"
                        : "\n<b>Light</b>\n");

        this.currentLightIndications
                .entrySet()
                .stream()
                .forEach(
                        e -> {
                            sb.append(e.getKey()).append(": ")
                                    .append(e.getValue()).append("\n");
                        });

        sb.append((isRus) ? "\n<b>Холодная вода:</b>\n"
                : "\n<b>Cold water:</b>\n");

        this.currentColdWaterIndications
                .entrySet()
                .stream()
                .forEach(
                        e -> {
                            sb.append(
                                    (e.getValue().getAlias() == null) ? "" : e
                                            .getValue().getAlias() + ": ")
                                    .append(e.getValue().getPrimaryIndication())
                                    .append("\n");
                        });

        sb.append((isRus) ? "\n<b>Горячая вода:</b>\n"
                : "\n<b>Hot water:</b>\n");
        this.currentHotWaterIndications
                .entrySet()
                .stream()
                .forEach(
                        e -> {
                            sb.append(
                                    (e.getValue().getAlias() == null) ? "" : e
                                            .getValue().getAlias() + ": ")
                                    .append(e.getValue().getPrimaryIndication())
                                    .append("\n");
                        });

        sb.append(
                (isRus) ? "\n\n<b>Стоимость аренды:</b> "
                        : "\n\n<b>Rent Amount:</b> ").append(this.rentAmount)
                .append((isRus) ? " руб" : " rub");

        if (this.takeout != null) {
            sb.append((isRus) ? "\n<b>Вычет:</b> " : "\n<b>Takeout:</b> ")
                    .append(String.format("%.2f", this.takeout))
                    .append((isRus) ? " руб" : " rub").append(" - ")
                    .append(this.takeoutDescription);
        }
        return sb.toString();
    }


    public boolean isLightSet() {
        return (this.lightPrimary.getIndications().size() == this.currentLightIndications
                .size()) ? true : false;
    }

    public boolean isWaterSet() {
        return (this.waterPrimary.getColdWater().size() == this.currentColdWaterIndications
                .size() && this.waterPrimary.getHotWater().size() == this.currentHotWaterIndications
                .size()) ? true : false;
    }


    public List<List<String>> getWaterButtons() {
        if (null == this.waterButtons) {
            this.waterButtons = new ArrayList<List<String>>();
        }
        return this.lastIndications.copyList(this.waterButtons);
    }


    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this,
                ToStringStyle.SHORT_PREFIX_STYLE);
    }
}
