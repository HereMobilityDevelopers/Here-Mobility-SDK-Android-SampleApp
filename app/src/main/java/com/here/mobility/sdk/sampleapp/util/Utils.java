package com.here.mobility.sdk.sampleapp.util;

import android.graphics.drawable.Drawable;

import com.here.mobility.sdk.demand.DemandDateTime;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.net.URL;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.SimpleTimeZone;
import java.util.TimeZone;

/**********************************************************
 * Copyright © 2018 HERE Global B.V. All rights reserved. *
 **********************************************************/
public class Utils {


    /**
     * Load image from URL
     * @param url the image URL
     * @return The drawable if the image load succeeded; otherwise null.
     */
    @Nullable
    public static Drawable loadImageFromURL(@NonNull String url) {
        try {
            InputStream is = (InputStream) new URL(url).getContent();
            return Drawable.createFromStream(is, null);
        } catch (Exception e) {
            return null;
        }
    }


    /**
     * Converting the {@link DemandDateTime} Object to epoch time in UTC timezone.
     * for future DST calculation.
     * @param demandDateTime the SDK representation of specific date
     * @return UTC time in millis since epoch
     */
    public static long demandDateTimeToUTCEpoch(@NonNull DemandDateTime demandDateTime) {
        GregorianCalendar gregorianCalendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        gregorianCalendar.set(Calendar.YEAR, demandDateTime.getYear());
        gregorianCalendar.set(Calendar.MONTH, demandDateTime.getMonthOfYear());
        gregorianCalendar.set(Calendar.DAY_OF_MONTH, demandDateTime.getDayOfMonth());
        gregorianCalendar.set(Calendar.HOUR_OF_DAY, demandDateTime.getHourOfDay());
        gregorianCalendar.set(Calendar.MINUTE, demandDateTime.getMinuteOfHour());
        return gregorianCalendar.getTimeInMillis();
    }
    
}
