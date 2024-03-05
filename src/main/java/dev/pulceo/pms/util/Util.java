package dev.pulceo.pms.util;

import java.util.Calendar;
import java.util.UUID;

public class Util {
    public static String createRandomName(String namePrefix) {
        String root = UUID.randomUUID().toString().replace("-", "");
        long millis = Calendar.getInstance().getTimeInMillis();
        long datePart = millis % 10000000L;
        return namePrefix + root.toLowerCase().substring(0, 3) + datePart;
    }
}
