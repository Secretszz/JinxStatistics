package com.jinx.statistics.utility;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateUtility {
    public static String format(Date date, String format) {
        return (new SimpleDateFormat(format)).format(date);
    }

    public static String format(Date date) {
        return format(date, "yyyy-MM-dd HH:mm:ss");
    }

    public static long now(){
        return System.currentTimeMillis();
    }
}
