package jadeproject;

import java.util.ArrayList;
import java.util.Random;

public class WeekCalendar {
	private boolean[] availability;

	private final int NUMBER_OF_WEEKDAYS = 7;
	private final int NUMBER_OF_HOURS    = 24;
	
	WeekCalendar() {
		Random random = new Random();

		availability = new boolean[NUMBER_OF_HOURS * NUMBER_OF_WEEKDAYS];

		for (int i = 0; i < availability.length; ++i) {
			availability[i] = random.nextBoolean();
		}
	}

	@Override
	public String toString() {
		String result = "";
		for (int day = 0; day < NUMBER_OF_WEEKDAYS; ++day) {
			result += "DAY: " + day + "\n";

			for (int hour = 0; hour < NUMBER_OF_HOURS; ++hour) {
				result += availability[day * NUMBER_OF_HOURS + hour] + " ";
			}
		}
		return result;
	}
}

	