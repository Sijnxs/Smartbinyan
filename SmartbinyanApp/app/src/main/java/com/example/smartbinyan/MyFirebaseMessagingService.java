package com.example.smartbinyan;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONObject;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "smartbinyan_notifications";
    private static final String PREFS_NAME = "mqtt_notifications";
    private static final String KEY_NOTIFICATIONS = "notifications";

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String title = "SmartBinyan Notification";
        String body = "You have a new message";

        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle();
            body = remoteMessage.getNotification().getBody();
        } else if (remoteMessage.getData().size() > 0) {
            title = remoteMessage.getData().get("title");
            body = remoteMessage.getData().get("body");
        }

        if (body == null) return;
        String lower = body.toLowerCase();
        if (lower.contains("unknown") || lower.contains("normal") || lower.contains("startup") || lower.contains("system on")) {
            return;
        }

        //  Handle gas detection alerts
        if (lower.contains("gas")) {
            if (lower.contains("toxic")) {
                body = "Gas detected in the Toxic compartment.";
            } else if (lower.contains("non-toxic") || lower.contains("nontoxic")) {
                body = "Gas detected in the Non-Toxic compartment.";
            } else if (lower.contains("food")) {
                body = "Gas detected in the Food compartment.";
            } else {
                body = "Gas detected in a designated compartment.";
            }
            title = "Gas Detected";
        }

        saveNotification(title, body);
        showAppNotification(title, body);
    }

    private void saveNotification(String title, String body) {
        try {
            SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
            String jsonStr = prefs.getString(KEY_NOTIFICATIONS, "[]");
            JSONArray jsonArray = new JSONArray(jsonStr);

            JSONObject newMsg = new JSONObject();
            newMsg.put("title", title);
            newMsg.put("body", body);
            jsonArray.put(newMsg);

            prefs.edit().putString(KEY_NOTIFICATIONS, jsonArray.toString()).apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showAppNotification(String title, String message) {
        Intent intent = new Intent(this, NotificationActivity.class);
        intent.putExtra("notification_title", title);
        intent.putExtra("notification_message", message);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                message.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setColor(Color.parseColor("#004909"))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (notificationManager != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "SmartBinyan Alerts",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("Channel for bin notifications");
                channel.enableLights(true);
                channel.setLightColor(Color.GREEN);
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }

            notificationManager.notify(message.hashCode(), builder.build());
        }
    }
}
