package rent;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class LastIndicationsHolder {

	private Map<String, Double> light;
	private Map<Integer, WaterHolder> hotWater;
	private Map<Integer, WaterHolder> coldWater;

	public LastIndicationsHolder() {

	}

	public Map<String, Double> getLight() {
		return light;
	}

	public void setLight(Map<String, Double> light) {
		this.light = copyMap(light);
	}

	public Map<Integer, WaterHolder> getHotWater() {
		return hotWater;
	}

	public void setHotWater(Map<Integer, WaterHolder> hotWater) {
		this.hotWater = copyMap(hotWater);
	}

	public Map<Integer, WaterHolder> getColdWater() {
		return coldWater;
	}

	public void setColdWater(Map<Integer, WaterHolder> coldWater) {
		this.coldWater = copyMap(coldWater);
	}

	public <T, V> Map<T, V> copyMap(Map<T, V> map) {

		return map
				.entrySet()
				.stream()
				.collect(
						Collectors.toMap(Entry::getKey, Entry::getValue, (e1,
								e2) -> e1, TreeMap::new));

	}
	
	public <T> List<T> copyList(List<T> list) {
		return list.stream().collect(Collectors.toList());
	}

}
