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
		String result = "Hours\tMon.\tTue.\tWed.\tThu.\tFri.\tSat.\tSun.\n";

		for (int hour = 0; hour < NUMBER_OF_HOURS; ++hour) {
			result += hour + "H00\t";
			
			for (int day = 0; day < NUMBER_OF_WEEKDAYS; ++day) {
				if (isAvailable(day, hour)) result += "OK\t";
				else result += "NOK\t";
			}

			result += "\n";
		}

		return result;
	}

	public ArrayList<int[]> listAllAvailableSlots() {
		ArrayList<int[]> result = new ArrayList<int[]>();

		for (int day = 0; day < NUMBER_OF_WEEKDAYS; ++day) {
			for (int hour = 0; hour < NUMBER_OF_HOURS; ++hour) {
				int[] availableSlot = {day, hour};
				if (isAvailable(day, hour)) result.add(availableSlot);
			}
		}

		return result;
	}

	public boolean isAvailable(int day, int hour) {
		return availability[day * NUMBER_OF_HOURS + hour];
	}
}

	