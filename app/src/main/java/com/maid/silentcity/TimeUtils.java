package com.maid.silentcity;

import java.util.Calendar;
import java.util.TimeZone;

/**
 * Утилітарний клас для визначення активного часового проміжку
 * для фільтрації даних на карті згідно зі схемою:
 * 5:00-9:00, 9:00-17:00, 17:00-20:00, 20:00-24:00, 0:00-5:00.
 */
public class TimeUtils {

    // Структура для зберігання годин початку та кінця проміжку
    public static class TimeRange {
        public int startHour; // Включно
        public int endHour;   // Виключно

        public TimeRange(int startHour, int endHour) {
            this.startHour = startHour;
            this.endHour = endHour;
        }
    }

    /**
     * Повертає TimeRange для поточного часу.
     */
    public static TimeRange getCurrentTimeRange() {
        Calendar currentCal = Calendar.getInstance();
        currentCal.setTimeZone(TimeZone.getDefault()); // Використовуємо локальний часовий пояс
        int currentHour = currentCal.get(Calendar.HOUR_OF_DAY);

        if (currentHour >= 5 && currentHour < 9) {
            return new TimeRange(5, 9);
        } else if (currentHour >= 9 && currentHour < 17) {
            return new TimeRange(9, 17);
        } else if (currentHour >= 17 && currentHour < 20) {
            return new TimeRange(17, 20);
        } else if (currentHour >= 20) {
            return new TimeRange(20, 24); // 20:00 - 23:59:59
        } else {
            return new TimeRange(0, 5); // 0:00 - 04:59:59
        }
    }

    /**
     * Повертає TimeRange за індексом, обраним у Spinner.
     * Індекси Spinner:
     * 0: Поточний час
     * 1: 00:00 - 05:00
     * 2: 05:00 - 09:00
     * 3: 09:00 - 17:00
     * 4: 17:00 - 20:00
     * 5: 20:00 - 24:00
     */
    public static TimeRange getTimeRangeByIndex(int index) {
        switch (index) {
            case 1: return new TimeRange(0, 5);
            case 2: return new TimeRange(5, 9);
            case 3: return new TimeRange(9, 17);
            case 4: return new TimeRange(17, 20);
            case 5: return new TimeRange(20, 24);
            case 0: // Fall-through: "Поточний час"
            default: return getCurrentTimeRange();
        }
    }

    /**
     * Перевіряє, чи належить час даних до заданого проміжку.
     */
    public static boolean isTimestampInSelectedRange(long dataTimestamp, TimeRange range) {
        Calendar dataCal = Calendar.getInstance();
        dataCal.setTimeInMillis(dataTimestamp);
        dataCal.setTimeZone(TimeZone.getDefault());
        int dataHour = dataCal.get(Calendar.HOUR_OF_DAY);

        if (range.startHour == 20 && range.endHour == 24) {
            // Проміжок 20:00 до 23:59:59
            return dataHour >= 20 && dataHour <= 23;
        } else if (range.startHour == 0 && range.endHour == 5) {
            // Проміжок 0:00 до 4:59:59
            return dataHour >= 0 && dataHour < 5;
        } else {
            // Стандартні проміжки
            return dataHour >= range.startHour && dataHour < range.endHour;
        }
    }
}