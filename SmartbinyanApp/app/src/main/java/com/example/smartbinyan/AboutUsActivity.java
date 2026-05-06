package com.example.smartbinyan;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AboutUsActivity extends AppCompatActivity {

    private TextView tvHeader, tvAboutDescription, tvMission, tvVision;
    private ImageView btnBack;

    private LinearLayout bottomHome, bottomProfile, bottomStatus, bottomNotification;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_us);

        // 🟢 Link XML Views Safely (Content)
        tvHeader = findViewById(R.id.tvHeader);
        tvAboutDescription = findViewById(R.id.tvAboutDescription);
        tvMission = findViewById(R.id.tvMission);
        tvVision = findViewById(R.id.tvVision);
        btnBack = findViewById(R.id.btnBack);

        // 🟢 Link XML Views Safely (Bottom Nav)
        bottomHome = findViewById(R.id.bottomHome);
        bottomProfile = findViewById(R.id.bottomProfile);
        bottomStatus = findViewById(R.id.bottomStatus);
        bottomNotification = findViewById(R.id.bottomNotification);

        // 🟢 Check for nulls to prevent crashes (Content logic remains)
        if (tvHeader != null)
            tvHeader.setText("About Us");

        if (tvAboutDescription != null)
            tvAboutDescription.setText("SmartBinyan is designed to monitor and manage waste bins efficiently. " +
                    "Our system provides real-time updates on bin status, ensuring cleaner and safer environments. " +
                    "We aim to help hospitals, public areas, and communities track waste levels, reduce overflow, and promote sustainability.");

        if (tvMission != null)
            tvMission.setText("To provide smart waste management solutions that keep spaces clean, efficient, and eco-friendly.");

        if (tvVision != null)
            tvVision.setText("To be a leading platform for sustainable waste tracking and management, creating cleaner and healthier communities.");

        // 🟢 Handle Back Button Click (Safe)
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                Toast.makeText(AboutUsActivity.this, "Returning to Dashboard...", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(AboutUsActivity.this, DashboardActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            });
        }

        // 🟢 Implement Bottom Navigation Click Listeners 🚀
        if (bottomHome != null) {
            // ✅ Home links to HomePageActivity
            bottomHome.setOnClickListener(v -> navigateToActivity(HomePageActivity.class));
        }

        if (bottomProfile != null) {
            // ✅ Profile links to ProfileActivity
            bottomProfile.setOnClickListener(v -> navigateToActivity(ProfileActivity.class));
        }

        if (bottomStatus != null) {
            // ✅ Status links to DashboardActivity
            bottomStatus.setOnClickListener(v -> navigateToActivity(DashboardActivity.class));
        }

        if (bottomNotification != null) {
            // ✅ Notification links to NotificationActivity
            bottomNotification.setOnClickListener(v -> navigateToActivity(NotificationActivity.class));
        }
    }

    /**
     * Helper method to handle navigation to a new activity.
     * Uses optimized flags for main navigation points.
     * @param targetActivity The Class of the Activity to start.
     */
    private void navigateToActivity(Class<?> targetActivity) {
        // Prevent navigating to the current activity
        if (targetActivity.equals(AboutUsActivity.class)) {
            Toast.makeText(this, "You are already on the About Us screen.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(AboutUsActivity.this, targetActivity);

        if (targetActivity.equals(HomePageActivity.class)) {
            // 🔥 This is the core fix: Guarantees HomePageActivity is brought to the front
            // by clearing anything above it and keeping it as the single top activity.
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        } else {
            // Use REORDER_TO_FRONT for other main activities
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        }

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);

        // Always finish the current activity when navigating away via bottom nav
        finish();
    }


    // 🟢 Handle physical back press properly (Still navigating to Dashboard/Status)
    @Override
    public void onBackPressed() {
        // ⚠️ CRITICAL ADJUSTMENT: Added FLAG_ACTIVITY_CLEAR_TOP here as well
        // to ensure the Dashboard is treated as a high-priority navigation target,
        // which helps resolve conflicts with the bottom nav logic.
        Intent intent = new Intent(AboutUsActivity.this, DashboardActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}