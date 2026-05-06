package com.example.smartbinyan;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Random;

public class VerifyCodeActivity extends AppCompatActivity {

    private EditText etCode1, etCode2, etCode3, etCode4, etCode5, etCode6, etVerificationCode;
    private Button btnVerify;
    private String generatedCode;
    private String userEmail, username;

    // Field to store the intended activity after verification (passed from LoginActivity)
    private String nextActivityClassName;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_code);

        // Initialize all possible EditTexts
        etCode1 = findViewById(R.id.etCode1);
        etCode2 = findViewById(R.id.etCode2);
        etCode3 = findViewById(R.id.etCode3);
        etCode4 = findViewById(R.id.etCode4);
        etCode5 = findViewById(R.id.etCode5);
        etCode6 = findViewById(R.id.etCode6);
        etVerificationCode = findViewById(R.id.etVerificationCode); // Single code field

        btnVerify = findViewById(R.id.btnVerify);

        // ✅ Retrieve data from intent
        userEmail = getIntent().getStringExtra("email");
        username = getIntent().getStringExtra("username");
        // Retrieve the name of the next activity (passed from LoginActivity)
        nextActivityClassName = getIntent().getStringExtra("nextActivity");

        // ✅ Generate code
        generatedCode = generateCode();

        // ✅ Send verification email asynchronously
        new Thread(() -> {
            try {
                EmailSender.sendEmail(this, userEmail, "SmartBinyan Verification Code",
                        "Your verification code is: " + generatedCode);
                runOnUiThread(() ->
                        Toast.makeText(this, "Verification code sent to email", Toast.LENGTH_SHORT).show());
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() ->
                        Toast.makeText(this, "Failed to send verification code", Toast.LENGTH_SHORT).show());
            }
        }).start();

        btnVerify.setOnClickListener(v -> {
            String inputCode = getCodeFromBoxes();
            if (TextUtils.isEmpty(inputCode)) {
                Toast.makeText(this, "Enter the code sent to your email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (inputCode.equals(generatedCode)) {
                Toast.makeText(this, "Verification successful!", Toast.LENGTH_SHORT).show();

                // ✅ Save verified session
                SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                SharedPreferences.Editor editor = sharedPref.edit();
                editor.putBoolean("isVerified", true);
                editor.apply();

                // ⬇️ MODIFIED: Dynamic navigation to HomePageActivity ⬇️
                Class<?> nextActivityClass = HomePageActivity.class; // Default to HomePageActivity

                if (nextActivityClassName != null && !nextActivityClassName.isEmpty()) {
                    try {
                        // Use the class name string to load the actual Class object
                        nextActivityClass = Class.forName(nextActivityClassName);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error finding home page activity, navigating to default.", Toast.LENGTH_LONG).show();
                    }
                }

                Intent intent = new Intent(VerifyCodeActivity.this, nextActivityClass);
                intent.putExtra("username", username);
                intent.putExtra("email", userEmail);
                startActivity(intent);
                finish();
                // ⬆️ END MODIFIED ⬆️

            } else {
                Toast.makeText(this, "Invalid code. Try again.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String generateCode() {
        Random random = new Random();
        int code = 100000 + random.nextInt(900000); // 6-digit code
        return String.valueOf(code);
    }

    // ✅ Combine code safely (works with 6 boxes or single box)
    private String getCodeFromBoxes() {
        StringBuilder code = new StringBuilder();

        if (etVerificationCode != null) {
            // Single input field
            code.append(etVerificationCode.getText().toString().trim());
        } else {
            // Six input fields
            if (etCode1 != null) code.append(etCode1.getText().toString().trim());
            if (etCode2 != null) code.append(etCode2.getText().toString().trim());
            if (etCode3 != null) code.append(etCode3.getText().toString().trim());
            if (etCode4 != null) code.append(etCode4.getText().toString().trim());
            if (etCode5 != null) code.append(etCode5.getText().toString().trim());
            if (etCode6 != null) code.append(etCode6.getText().toString().trim());
        }

        return code.toString();
    }
}