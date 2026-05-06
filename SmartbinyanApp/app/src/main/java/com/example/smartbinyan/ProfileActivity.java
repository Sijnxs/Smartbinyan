package com.example.smartbinyan;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

public class ProfileActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone;
    private MaterialButton btnSave, btnBack, btnLogout;
    private ImageView ivProfile;

    private static final int PICK_IMAGE_REQUEST = 101;
    private Uri selectedImageUri;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        btnSave = findViewById(R.id.btnSave);
        btnBack = findViewById(R.id.btnBack);
        btnLogout = findViewById(R.id.btnLogout);
        ivProfile = findViewById(R.id.ivProfile);

        SharedPreferences prefs = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        etName.setText(prefs.getString("username", ""));
        etEmail.setText(prefs.getString("email", ""));
        etPhone.setText(prefs.getString("phone", ""));

        // ✅ Load saved profile image
        String savedImageUri = prefs.getString("profile_image_uri", null);
        if (savedImageUri != null) {
            ivProfile.setImageURI(Uri.parse(savedImageUri));
        }

        // ✅ Open gallery to choose a new image
        ivProfile.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK);
            intent.setType("image/*");
            startActivityForResult(intent, PICK_IMAGE_REQUEST);
        });

        // ✅ Save profile data
        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();

            if (newName.isEmpty() || newEmail.isEmpty() || newPhone.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putString("username", newName);
            editor.putString("email", newEmail);
            editor.putString("phone", newPhone);

            FirebaseAuth auth = FirebaseAuth.getInstance();
            FirebaseUser user = auth.getCurrentUser();

            if (selectedImageUri != null && user != null) {
                // ✅ Upload image to Firebase Storage
                StorageReference storageRef = FirebaseStorage.getInstance()
                        .getReference("profile_images/" + user.getUid() + ".jpg");

                storageRef.putFile(selectedImageUri)
                        .addOnSuccessListener(taskSnapshot -> storageRef.getDownloadUrl()
                                .addOnSuccessListener(uri -> {
                                    String imageUrl = uri.toString();
                                    editor.putString("profile_image_uri", imageUrl);
                                    editor.apply();

                                    // ✅ Update Firebase Realtime Database
                                    DatabaseReference userRef = FirebaseDatabase.getInstance()
                                            .getReference("users")
                                            .child(user.getUid());

                                    userRef.child("username").setValue(newName);
                                    userRef.child("email").setValue(newEmail);
                                    userRef.child("phone").setValue(newPhone);
                                    userRef.child("profileImage").setValue(imageUrl)
                                            .addOnSuccessListener(unused ->
                                                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show())
                                            .addOnFailureListener(e ->
                                                    Toast.makeText(this, "Firebase update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());

                                    goToDashboard();
                                }))
                        .addOnFailureListener(e ->
                                Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            } else {
                editor.apply();

                if (user != null) {
                    DatabaseReference userRef = FirebaseDatabase.getInstance()
                            .getReference("users")
                            .child(user.getUid());

                    userRef.child("username").setValue(newName);
                    userRef.child("email").setValue(newEmail);
                    userRef.child("phone").setValue(newPhone)
                            .addOnSuccessListener(unused ->
                                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Firebase update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }

                goToDashboard();
            }
        });

        // ✅ Go back
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(ProfileActivity.this, DashboardActivity.class));
            finish();
        });

        // ✅ Logout
        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            SharedPreferences.Editor editor = prefs.edit();
            editor.clear();
            editor.apply();
            Toast.makeText(this, "Logged out successfully!", Toast.LENGTH_SHORT).show();

            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    // ✅ Handle image picker result
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            ivProfile.setImageURI(selectedImageUri);
        }
    }

    private void goToDashboard() {
        Intent intent = new Intent(ProfileActivity.this, DashboardActivity.class);
        startActivity(intent);
        finish();
    }
}
