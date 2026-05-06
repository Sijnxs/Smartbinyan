package com.example.smartbinyan;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast; // Added Toast for helper function

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.example.smartbinyan.adapters.BinStatusActivity;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.messaging.FirebaseMessaging;

public class DashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener, SensorEventListener {

    private DrawerLayout drawerLayout;
    private TextView tvWelcome, tvProximityStatus;
    private TextView tvToxicStatus, tvNonToxicStatus, tvFoodStatus;
    private ImageView imgProfile;
    // 1. ADDED bottomHome to the existing declarations
    private LinearLayout bottomHome, bottomProfile, bottomAbout, bottomNotification;
    private SensorManager sensorManager;
    private Sensor proximitySensor;

    private static final String CHANNEL_ID = "smartbin_alerts";
    private static final long PROXIMITY_DEBOUNCE_MS = 1500;
    private long lastProximityEventTime = 0;

    private boolean toxicGasNotified = false;
    private boolean nonToxicGasNotified = false;
    private boolean foodGasNotified = false;

    // -------------------- SAFE UI HELPER --------------------
    private void safeSetText(TextView tv, String text) {
        if (tv != null) tv.setText(text);
    }

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 101);
        }

        createNotificationChannel();
        subscribeToBinTopics();
        setupToolbarAndDrawer();
        bindViews();

        // Load user data and show personalized welcome
        new Handler().post(this::initializeUserData);

        setupBottomNavigation();
        new Handler().postDelayed(this::loadBinStatus, 300);
        setupFirebaseGasListener();
        setupFirebaseProximityListener();
        setupProximitySensor();
    }
    // ... (createNotificationChannel, setupToolbarAndDrawer methods remain unchanged)

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "SmartBin Alerts", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private void setupToolbarAndDrawer() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(""); // Remove top title
        }

        drawerLayout = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            try {
                View headerView = navigationView.getHeaderView(0);
                if (headerView != null) {
                    imgProfile = headerView.findViewById(R.id.imgUser);
                }
            } catch (Exception ignored) {}
        }
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawerLayout, toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();
    }

    private void bindViews() {
        tvProximityStatus = findViewById(R.id.tvProximityStatus);
        // 2. BIND bottomHome
        bottomHome = findViewById(R.id.bottomHome);
        bottomProfile = findViewById(R.id.bottomProfile);
        bottomAbout = findViewById(R.id.bottomAbout);
        bottomNotification = findViewById(R.id.bottomNotification);
        tvToxicStatus = findViewById(R.id.tvToxicStatus);
        tvNonToxicStatus = findViewById(R.id.tvNonToxicStatus);
        tvFoodStatus = findViewById(R.id.tvFoodStatus);
    }

    private void setupBottomNavigation() {
        // 3. ADD CLICK LISTENER FOR bottomHome
        if (bottomHome != null) {
            bottomHome.setOnClickListener(v -> navigateToActivity(HomePageActivity.class));
        }

        // Existing listeners (now using the helper function for consistency)
        if (bottomProfile != null)
            bottomProfile.setOnClickListener(v -> navigateToActivity(ProfileActivity.class));
        if (bottomAbout != null)
            bottomAbout.setOnClickListener(v -> navigateToActivity(AboutUsActivity.class));
        if (bottomNotification != null)
            bottomNotification.setOnClickListener(v -> navigateToActivity(NotificationActivity.class));
    }

    /**
     * Helper method to handle navigation to a new activity from the bottom bar.
     * Uses optimized flags for main navigation points to ensure correct back stack behavior.
     */
    private void navigateToActivity(Class<?> targetActivity) {
        if (targetActivity.equals(DashboardActivity.class)) {
            Toast.makeText(this, "You are already on the Status screen.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 🚨 CRITICAL FIX: Ensure the Intent is created correctly.
        Intent intent = new Intent(DashboardActivity.this, targetActivity);

        if (targetActivity.equals(HomePageActivity.class)) {
            // Use CLEAR_TOP | SINGLE_TOP for the home/root navigation
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            // Use REORDER_TO_FRONT for switching between main screens
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Ensure the current activity finishes so it's not kept in the stack unnecessarily
        finish();
    }

    // 5. ADDED onBackPressed() for controlled exit.
    // This is vital to prevent unexpected behavior when mixing navigation methods.
    @Override
    public void onBackPressed() {
        super.onBackPressed();
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            // Instead of finish(), navigate back to the true Home/Root activity.
            // This prevents the app from exiting to the device home screen directly
            // unless the Home Activity is the only thing left.
            Intent intent = new Intent(this, HomePageActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        }
    }


    // ... (initializeUserData, subscribeToBinTopics, loadBinStatus methods remain unchanged)

    private void initializeUserData() {
        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String userName = prefs.getString("userName", "");

        // Get name from intent if passed from Register/Login
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("userName")) {
            userName = intent.getStringExtra("userName");
            // Save it for next time
            prefs.edit().putString("userName", userName).apply();
        }

        // Display personalized welcome text
        if (!userName.isEmpty()) {
            // Assuming tvWelcome is linked correctly in the original code
            tvWelcome = findViewById(R.id.tvWelcome);
            safeSetText(tvWelcome, "Welcome, " + userName + "!");
        } else {
            // Assuming tvWelcome is linked correctly in the original code
            tvWelcome = findViewById(R.id.tvWelcome);
            safeSetText(tvWelcome, "Welcome!");
        }
    }

    private void subscribeToBinTopics() {
        String binId = "CMU123";
        FirebaseMessaging.getInstance().subscribeToTopic("gas_" + binId + "_toxic");
        FirebaseMessaging.getInstance().subscribeToTopic("gas_" + binId + "_non_toxic");
        FirebaseMessaging.getInstance().subscribeToTopic("gas_" + binId + "_food");
    }

    private void loadBinStatus() {
        safeSetText(tvToxicStatus, "Toxic Compartment: Clear");
        safeSetText(tvNonToxicStatus, "Non-Toxic Compartment: Clear");
        safeSetText(tvFoodStatus, "Food Compartment: Clear");
        safeSetText(tvProximityStatus, "Proximity: Monitoring...");
    }

    // ... (setupFirebaseGasListener, setupFirebaseProximityListener, handleGasUpdate,
    // handleProximityUpdate, setupProximitySensor, onSensorChanged, onAccuracyChanged,
    // sendNotification, onDestroy, onNavigationItemSelected methods remain unchanged)

    private void setupFirebaseGasListener() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("gas_events");
        final boolean[] initialDataLoaded = {false};

        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String prevKey) {
                if (!initialDataLoaded[0]) return;
                processGasSnapshot(snapshot);
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, String prevKey) {
                processGasSnapshot(snapshot);
            }

            private void processGasSnapshot(DataSnapshot snapshot) {
                String compartment = snapshot.child("compartment").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);
                Long timestamp = snapshot.child("timestamp").getValue(Long.class);

                if (compartment == null || status == null) return;
                long now = System.currentTimeMillis();
                if (timestamp != null && now - timestamp > 10000) return;

                String cleanStatus = status.trim().toLowerCase();
                boolean gasDetected = cleanStatus.equals("gas") ||
                        cleanStatus.equals("detected") ||
                        cleanStatus.equals("gas_detected") ||
                        cleanStatus.contains("leak");

                handleGasUpdate(compartment.toLowerCase(), gasDetected ? "detected" : "clear");
            }

            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String prevKey) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}

            { new Handler().postDelayed(() -> initialDataLoaded[0] = true, 2000); }
        });
    }

    // ------------------ PROXIMITY LISTENER ------------------
    private void setupFirebaseProximityListener() {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("proximity_events");
        ref.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, String prevKey) {
                String compartment = snapshot.child("compartment").getValue(String.class);
                String status = snapshot.child("status").getValue(String.class);
                if (compartment != null && status != null)
                    handleProximityUpdate(compartment.toLowerCase(), status.toLowerCase());
            }

            @Override public void onChildChanged(@NonNull DataSnapshot snapshot, String prevKey) {}
            @Override public void onChildRemoved(@NonNull DataSnapshot snapshot) {}
            @Override public void onChildMoved(@NonNull DataSnapshot snapshot, String prevKey) {}
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ------------------ GAS + PROXIMITY HANDLERS ------------------
    private void handleGasUpdate(String bin, String status) {
        boolean detected = status.equals("detected");
        if (!detected) {
            switch (bin) {
                case "toxic":
                    toxicGasNotified = false;
                    safeSetText(tvToxicStatus, "Toxic Compartment: Clear ✅");
                    break;
                case "non_toxic":
                case "nontoxic":
                    nonToxicGasNotified = false;
                    safeSetText(tvNonToxicStatus, "Non-Toxic Compartment: Clear ✅");
                    break;
                case "food":
                    foodGasNotified = false;
                    safeSetText(tvFoodStatus, "Food Compartment: Clear ✅");
                    break;
            }
            return;
        }

        switch (bin) {
            case "toxic":
                if (!toxicGasNotified) {
                    toxicGasNotified = true;
                    safeSetText(tvToxicStatus, "Toxic Compartment");
                    sendNotification("Gas Alert", "Gas detected in Toxic Compartment!");
                }
                break;
            case "non_toxic":
            case "nontoxic":
                if (!nonToxicGasNotified) {
                    nonToxicGasNotified = true;
                    safeSetText(tvNonToxicStatus, "Non-Toxic Compartment");
                    sendNotification("Gas Alert", "Gas detected in Non-Toxic Compartment!");
                }
                break;
            case "food":
                if (!foodGasNotified) {
                    foodGasNotified = true;
                    safeSetText(tvFoodStatus, "Food Compartment");
                    sendNotification("Gas Alert", "Gas detected in Food Compartment!");
                }
                break;
        }
    }

    private void handleProximityUpdate(String bin, String status) {
        boolean isFull = status.contains("full") || status.contains("1") || status.contains("near");
        boolean isClear = !isFull;

        long now = System.currentTimeMillis();
        if (now - lastProximityEventTime < PROXIMITY_DEBOUNCE_MS && !isClear) return;
        lastProximityEventTime = now;

        switch (bin) {
            case "toxic":
                safeSetText(tvToxicStatus, isFull ? "Toxic Compartment: FULL 🚮" : "Toxic Compartment: Clear ✅");
                if (isFull) sendNotification("Bin Full", "Toxic compartment is full.");
                break;
            case "non_toxic":
            case "nontoxic":
                safeSetText(tvNonToxicStatus, isFull ? "Non-Toxic Compartment: FULL 🚮" : "Non-Toxic Compartment: Clear ✅");
                if (isFull) sendNotification("Bin Full", "Non-Toxic compartment is full.");
                break;
            case "food":
                safeSetText(tvFoodStatus, isFull ? "Food Compartment: FULL 🚮" : "Food Compartment: Clear ✅");
                if (isFull) sendNotification("Bin Full", "Food compartment is full.");
                break;
        }

        safeSetText(tvProximityStatus, isFull ? "Proximity: Object Detected" : "Proximity: Monitoring...");
    }

    // ------------------ SENSOR ------------------
    private void setupProximitySensor() {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            if (proximitySensor != null) {
                sensorManager.registerListener(this, proximitySensor, SensorManager.SENSOR_DELAY_NORMAL);
            } else {
                safeSetText(tvProximityStatus, "Proximity Sensor: Not Available ❌");
            }
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_PROXIMITY) return;
        float distance = event.values[0];
        if (proximitySensor != null) {
            boolean objectNear = distance < proximitySensor.getMaximumRange();
            safeSetText(tvToxicStatus, objectNear ? "Toxic Compartment: FULL 🚮" : "Toxic Compartment: Clear ✅");
        } else {
            safeSetText(tvToxicStatus, "Toxic Compartment: Clear ✅");
        }
    }

    @Override public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void sendNotification(String title, String message) {
        Intent intent = new Intent(this, NotificationActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(title)
                .setContentText(message)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat manager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
            manager.notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (sensorManager != null) {
            try { sensorManager.unregisterListener(this); } catch (Exception ignored) {}
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_bin_status)
            startActivity(new Intent(this, BinStatusActivity.class));
        else if (id == R.id.nav_profile)
            startActivity(new Intent(this, ProfileActivity.class));
        else if (id == R.id.nav_about)
            startActivity(new Intent(this, AboutUsActivity.class));
        else if (id == R.id.nav_notifications)
            startActivity(new Intent(this, NotificationActivity.class));
        drawerLayout.closeDrawer(GravityCompat.START);
        return true;
    }
}