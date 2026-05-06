package com.example.smartbinyan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.HashMap;
import java.util.Map;

import info.mqtt.android.service.MqttAndroidClient;

public class NotificationActivity extends AppCompatActivity {

    private LinearLayout messageContainer;
    private TextView tvNoNotification, tvNotificationStatus, btnClear;
    private MqttAndroidClient mqttClient;

    private static final String MQTT_BROKER_URI = "tcp://broker.hivemq.com:1883";
    private static final String CHANNEL_ID = "smartbinyan_notifications";
    private static String MQTT_TOPIC = "smartbinyan/bin/status";

    private String userId;
    private DatabaseReference gasRef, proximityRef;

    private final Map<String, String> lastGasStatus = new HashMap<>();
    private final Map<String, String> lastProximityStatus = new HashMap<>();

    @SuppressLint({"MissingInflatedId", "WrongViewCast"})
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        userId = prefs.getString("userId", "defaultUser");
        MQTT_TOPIC = "smartbinyan/bin/status_" + userId;

        messageContainer = findViewById(R.id.messageContainer);
        tvNoNotification = findViewById(R.id.tvNoNotification);
        tvNotificationStatus = findViewById(R.id.tvNotificationStatus);
        btnClear = findViewById(R.id.btnClear);

        if (messageContainer == null) {
            Toast.makeText(this, "Layout not found", Toast.LENGTH_SHORT).show();
            return;
        }

        createNotificationChannel();
        setupMQTT();
        setupFirebaseRealtimeListeners();

        btnClear.setOnClickListener(v -> clearNotification());

        if (messageContainer.getChildCount() == 0 && tvNoNotification != null) {
            tvNoNotification.setVisibility(TextView.VISIBLE);
            tvNoNotification.setText("✅ No notifications yet");
        }
    }

    private void setupFirebaseRealtimeListeners() {
        FirebaseDatabase db = FirebaseDatabase.getInstance();
        gasRef = db.getReference("gas_events");
        proximityRef = db.getReference("proximity_events");

        attachListenersWithInitialLoad(gasRef, "gas");
        attachListenersWithInitialLoad(proximityRef, "proximity");
    }

    private void attachListenersWithInitialLoad(DatabaseReference ref, String type) {
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot child : snapshot.getChildren()) {
                    String key = child.getKey();
                    String status = child.child("status").getValue(String.class);
                    if (key == null) continue;
                    if (status == null) status = "";
                    status = status.toLowerCase().trim();

                    if (type.equals("gas")) {
                        lastGasStatus.put(key, status);
                    } else {
                        lastProximityStatus.put(key, status);
                    }
                }

                ref.addChildEventListener(new ChildEventListener() {
                    @Override
                    public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        handleAlertChange(snapshot, type);
                    }

                    @Override
                    public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                        handleAlertChange(snapshot, type);
                    }

                    private void handleAlertChange(@NonNull DataSnapshot snapshot, String type) {
                        String key = snapshot.getKey();
                        String status = snapshot.child("status").getValue(String.class);
                        String compartment = snapshot.child("compartment").getValue(String.class);

                        if (key == null || status == null) return;
                        status = status.toLowerCase().trim();

                        if (type.equals("gas")) {
                            String prev = lastGasStatus.getOrDefault(key, "normal");

                            if (!isAlertGas(prev) && isAlertGas(status)) {
                                lastGasStatus.put(key, status);
                                String body = generateGasAlertBody(compartment, status);
                                runOnUiThread(() -> showNewNotification("Gas Alert", body));
                            } else if (isAlertGas(prev) && !isAlertGas(status)) {
                                lastGasStatus.put(key, status);
                                String body = generateGasClearBody(compartment);
                                runOnUiThread(() -> showNewNotification("Gas Cleared", body));
                            } else {
                                lastGasStatus.put(key, status);
                            }
                        } else {
                            String prev = lastProximityStatus.getOrDefault(key, "normal");

                            if (!isAlertProximity(prev) && isAlertProximity(status)) {
                                lastProximityStatus.put(key, status);
                                String body = generateProximityAlertBody(compartment, status);
                                runOnUiThread(() -> showNewNotification("Bin Alert", body));
                            } else {
                                lastProximityStatus.put(key, status);
                            }
                        }
                    }

                    @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                        String key = snapshot.getKey();
                        if (key == null) return;
                        if (type.equals("gas")) lastGasStatus.remove(key);
                        else lastProximityStatus.remove(key);
                    }
                    @Override public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {}
                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private String generateGasAlertBody(@Nullable String compartment, String status) {
        String base = "Gas detected! Please check the area.";
        if (compartment != null && !compartment.isEmpty()) {
            base = "Gas detected in the " + compartment + " compartment!";
        }
        if (status.contains("leak")) {
            return base + " (Leak suspected)";
        }
        return base;
    }

    private String generateGasClearBody(@Nullable String compartment) {
        if (compartment != null && !compartment.isEmpty()) {
            return "Gas levels in the " + compartment + " compartment have returned to normal.";
        }
        return "Gas alert has cleared. Levels are normal.";
    }

    private String generateProximityAlertBody(@Nullable String compartment, String status) {
        String binName = (compartment != null && !compartment.isEmpty()) ? compartment + " Bin" : "A Bin";

        if (status.equals("full")) {
            return binName + " is FULL.";
        } else if (status.equals("obstructed") || status.equals("blocked")) {
            return binName + " is obstructed or blocked.";
        }
        return binName + " requires attention.";
    }

    private boolean isAlertGas(String status) {
        if (status == null) return false;
        status = status.toLowerCase().trim();
        return status.equals("detected") || status.equals("leak") ||
                status.contains("gas detected") || status.contains("gas leak");
    }

    private boolean isAlertProximity(String status) {
        if (status == null) return false;
        status = status.toLowerCase().trim();
        return status.equals("full") || status.equals("obstructed") || status.equals("blocked");
    }

    private void clearNotification() {
        messageContainer.removeAllViews();
        if (tvNoNotification != null) {
            tvNoNotification.setVisibility(TextView.VISIBLE);
            tvNoNotification.setText("✅ No notifications yet");
        }
    }

    private void setupMQTT() {
        String clientId = MqttClient.generateClientId();
        mqttClient = new MqttAndroidClient(getApplicationContext(), MQTT_BROKER_URI, clientId);

        mqttClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (tvNotificationStatus != null) tvNotificationStatus.setText("Connected to server");
                subscribeToTopic();
            }

            @Override
            public void connectionLost(Throwable cause) {
                if (tvNotificationStatus != null) tvNotificationStatus.setText("Connection lost. Retrying...");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                handleMQTTMessage(new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {}
        });

        connectMQTT();
    }

    private void connectMQTT() {
        try {
            MqttConnectOptions options = new MqttConnectOptions();
            options.setCleanSession(false); // MORE STABLE
            options.setAutomaticReconnect(true);
            options.setKeepAliveInterval(20);

            if (tvNotificationStatus != null)
                tvNotificationStatus.setText("Connecting to SmartBinyan...");

            mqttClient.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (tvNotificationStatus != null)
                        tvNotificationStatus.setText("SmartBinyan Connected");

                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                    // ❗ REMOVE FALSE ERROR
                    if (tvNotificationStatus != null)
                        tvNotificationStatus.setText("Reconnecting...");

                    // 🔁 RETRY EVERY 2 SECONDS
                    messageContainer.postDelayed(NotificationActivity.this::connectMQTT, 2000);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (tvNotificationStatus != null)
                tvNotificationStatus.setText("Connection error");
        }
    }


    private void subscribeToTopic() {
        try {
            mqttClient.subscribe(MQTT_TOPIC, 1, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    if (tvNotificationStatus != null) tvNotificationStatus.setText("Smart Bin online");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    if (tvNotificationStatus != null) tvNotificationStatus.setText("Subscription failed");
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            if (tvNotificationStatus != null) tvNotificationStatus.setText("Subscription error");
        }
    }

    private void handleMQTTMessage(String message) {
        if (message == null || messageContainer == null || tvNoNotification == null) return;
        String msg = message.toLowerCase().trim();

        if (msg.contains("gas detected") || msg.contains("gas leak")) {
            runOnUiThread(() -> showNewNotification("Gas Alert (MQTT)", "Gas detected! Please check the area."));
        } else if (msg.contains("bin full")) {
            String compartment;
            if (msg.contains("toxic")) compartment = "Toxic Bin";
            else if (msg.contains("food")) compartment = "Food Bin";
            else compartment = "A Bin";
            runOnUiThread(() -> showNewNotification("Bin Full (MQTT)", compartment + " is FULL."));
        } else if (msg.contains("obstructed") || msg.contains("blocked")) {
            String compartment;
            if (msg.contains("toxic")) compartment = "Toxic Bin";
            else if (msg.contains("food")) compartment = "Food Bin";
            else compartment = "A Bin";
            runOnUiThread(() -> showNewNotification("Bin Alert (MQTT)", compartment + " is obstructed or blocked."));
        }
    }

    private void showNewNotification(String title, String body) {
        if (messageContainer == null || tvNoNotification == null) return;

        if (tvNoNotification.getVisibility() == View.VISIBLE) {
            tvNoNotification.setVisibility(TextView.GONE);
        }

        TextView tv = new TextView(this);
        tv.setText("• " + title + ": " + body);
        tv.setTextColor(Color.BLACK);
        tv.setTextSize(16);
        tv.setTypeface(null, android.graphics.Typeface.BOLD);
        int pad = (int) (16 * getResources().getDisplayMetrics().density);
        tv.setPadding(pad, pad, pad, pad);

        messageContainer.addView(tv, 0);

        showSystemNotification(title, body);
    }

    private void showSystemNotification(String title, String body) {
        NotificationManager manager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent intent = new Intent(this, NotificationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT
        );

        int NOTIFICATION_ID = 1001;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setColor(Color.parseColor("#00A36C"))
                .setContentIntent(pendingIntent);

        manager.notify(NOTIFICATION_ID, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SmartBinyan Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Channel for SmartBinyan proximity and gas alerts");
            channel.enableLights(true);
            channel.setLightColor(Color.GREEN);
            channel.enableVibration(true);

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mqttClient != null) {
            try {
                mqttClient.disconnect();
            } catch (Exception ignored) {}
        }
    }
}
