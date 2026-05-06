package com.example.smartbinyan;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.google.android.material.bottomnavigation.BottomNavigationView;
// New imports added for navigation destinations
import com.example.smartbinyan.AboutUsActivity;
import com.example.smartbinyan.adapters.BinStatusActivity;

// *** ASSUMED IMPORTS ADDED/FIXED HERE ***
import com.example.smartbinyan.ProfileActivity;
import com.example.smartbinyan.NotificationActivity;
import com.example.smartbinyan.DashboardActivity;
// ***************************************

// Assuming you will use a BottomNavigationView for navigation
// If not, you can remove the BottomNavigationView related code.

public class HomePageActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_homepage);

        // Setup Toolbar (Assuming you use the common toolbar.xml layout)
        @SuppressLint({"MissingInflatedId", "LocalSuppress"}) Toolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            if (getSupportActionBar() != null) {
                getSupportActionBar().setTitle("SmartBinyan Home");
            }
        }

        // Setup Bottom Navigation (if you use it)
        setupBottomNavigation();

        // Setup new click listeners for dashboard items
        setupDashboardListeners();
    }

    private void setupDashboardListeners() {
        // Listener for the "View Current Bin Status" card
        View cardBinStatus = findViewById(R.id.cardBinStatus);
        if (cardBinStatus != null) {
            cardBinStatus.setOnClickListener(v -> {
                Intent intent = new Intent(HomePageActivity.this, BinStatusActivity.class);
                startActivity(intent);
            });
        }

        // Click listeners for the custom bottom navigation bar (from XML)

        // Home Icon (This is the current activity, clicking it does nothing but refresh/toast)
        View bottomHome = findViewById(R.id.bottomHome);
        if (bottomHome != null) {
            bottomHome.setOnClickListener(v -> {
                Toast.makeText(HomePageActivity.this, "You are already on the Home screen.", Toast.LENGTH_SHORT).show();
            });
        }

        // Profile Icon
        View bottomProfile = findViewById(R.id.bottomProfile);
        if (bottomProfile != null) {
            bottomProfile.setOnClickListener(v -> {
                navigateToActivity(ProfileActivity.class);
            });
        }

        // Status Icon (NEW LOGIC)
        View bottomStatus = findViewById(R.id.bottomStatus);
        if (bottomStatus != null) {
            bottomStatus.setOnClickListener(v -> {
                // Navigates to DashboardActivity when Status is clicked
                navigateToActivity(DashboardActivity.class);
            });
        }

        // About Us Icon
        View bottomAbout = findViewById(R.id.bottomAbout);
        if (bottomAbout != null) {
            bottomAbout.setOnClickListener(v -> {
                navigateToActivity(AboutUsActivity.class);
            });
        }

        // Notification Icon
        View bottomNotification = findViewById(R.id.bottomNotification);
        if (bottomNotification != null) {
            bottomNotification.setOnClickListener(v -> {
                navigateToActivity(NotificationActivity.class);
            });
        }
    }

    /**
     * Helper method to handle navigation to a new activity.
     * Uses optimized flags for main navigation points.
     * This replaces the repetitive intent creation logic.
     * @param targetActivity The Class of the Activity to start.
     */
    private void navigateToActivity(Class<?> targetActivity) {
        // Prevent navigating to the current activity (though it's Home, we check anyway)
        if (targetActivity.equals(HomePageActivity.class)) {
            Toast.makeText(this, "You are already on the Home screen.", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(HomePageActivity.this, targetActivity);

        // For non-Home navigation points, we typically use REORDER_TO_FRONT
        // to switch activities without restarting them if they are in the stack.
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }


    // NOTE: This method contains the original code which is now redundant
    // due to the custom bottom navigation bar used in activity_homepage.xml,
    // but it is kept as per the instruction "dont remove anything".
    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);

        // If the home icon ID is known, select it here:
        // bottomNavigationView.setSelectedItemId(R.id.navigation_home);

        if (bottomNavigationView != null) {
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.navigation_dashboard) {
                    // Navigate to Dashboard or keep it as the home base
                    return true;
                } else if (itemId == R.id.navigation_notification) {
                    startActivity(new Intent(HomePageActivity.this, NotificationActivity.class));
                    return true;
                } else if (itemId == R.id.navigation_profile) {
                    startActivity(new Intent(HomePageActivity.this, ProfileActivity.class));
                    return true;
                }
                return false;
            });
        }
    }

    // You can remove this if you are using BottomNavigationView instead of a menu
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // IMPORTANT: Add proper back press handling for the root activity.
    // When the user is on the Home screen and presses back, the app should close.
    @Override
    public void onBackPressed() {
        // Only call finish() if the user is on the root screen and presses back.
        // Calling super.onBackPressed() will close the activity if it's the root.
        super.onBackPressed();
    }
}