package com.example.smartbinyan.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

public final class NotificationUtils {
    public static final String CH_GAS = "gas_alerts";

    public static void ensureGasChannel(Context ctx) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CH_GAS, "Gas Alerts", NotificationManager.IMPORTANCE_HIGH
            );
            ch.setDescription("Alerts for gas leaks");
            ch.enableVibration(true);
            NotificationManager nm = ctx.getSystemService(NotificationManager.class);
            nm.createNotificationChannel(ch);
        }
    }
}
