package com.example.calmsea;

import java.util.Calendar;

public class GreetingHelper {
    public static String getGreeting() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);

        if (hour >= 5 && hour < 12) {
            return "Доброе утро";
        } else if (hour >= 12 && hour < 17) {
            return "Добрый день";
        } else if (hour >= 17 && hour < 22) {
            return "Добрый вечер";
        } else {
            return "Доброй ночи";
        }
    }
}
