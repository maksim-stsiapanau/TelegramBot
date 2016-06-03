package db;

import rent.RentHolder;

/**
 * Async saver for month rent stat
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class MonthSaver implements Runnable {

	private final RentHolder rh;

	public MonthSaver(RentHolder rh) {

		this.rh = rh;

	}

	@Override
	public void run() {

		DataBaseHelper.getInstance().insertMonthStat(this.rh);

	}

}
