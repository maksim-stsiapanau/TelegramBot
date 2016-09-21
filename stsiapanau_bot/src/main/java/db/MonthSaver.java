package db;

import rent.RentMonthHolder;

/**
 * Async saver for month rent stat
 * 
 * @author Maksim Stepanov
 * @email maksim.stsiapanau@gmail.com
 */
public class MonthSaver implements Runnable {

	private final RentMonthHolder total;

	public MonthSaver(RentMonthHolder rh) {
		this.total = rh;
	}

	@Override
	public void run() {

		DataBaseHelper.getInstance().insertMonthStat(this.total);
	}
}
