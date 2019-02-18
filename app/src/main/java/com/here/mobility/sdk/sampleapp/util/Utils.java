package com.here.mobility.sdk.sampleapp.util;

import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.InputStream;
import java.net.URL;

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
    
}
