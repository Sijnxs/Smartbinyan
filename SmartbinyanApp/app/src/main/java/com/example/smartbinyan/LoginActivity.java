package com.example.smartbinyan;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private MaterialButton btnLogin, btnGoogle;
    private TextView btnGoRegister;

    private GoogleSignInClient googleSignInClient;
    private ActivityResultLauncher<Intent> googleSignInLauncher;
    private FirebaseAuth mAuth;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // ✅ Initialize Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // ✅ Initialize Views
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnGoRegister = findViewById(R.id.btnGoRegister);
        btnGoogle = findViewById(R.id.btnGoogle);

        // ✅ Configure Google Sign-In for Firebase
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Must match google-services.json
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        // ✅ Always sign out first to avoid token reuse
        googleSignInClient.signOut();

        // ✅ Setup Google Sign-In launcher
        googleSignInLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                (ActivityResult result) -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(result.getData());
                        handleGoogleSignInResult(task);
                    } else {
                        Toast.makeText(this, "Google Sign-In canceled or failed.", Toast.LENGTH_SHORT).show();
                    }
                });

        // ✅ Button click listeners
        btnLogin.setOnClickListener(v -> handleLogin());
        btnGoogle.setOnClickListener(v -> signInWithGoogle());
        btnGoRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            finish();
        });
    }

    // ✅ Handle manual username/password login
    private void handleLogin() {
        String username = etUsername.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (TextUtils.isEmpty(username) || TextUtils.isEmpty(password)) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
        String savedUsername = sharedPref.getString("username", null);
        String savedPassword = sharedPref.getString("password", null);
        String savedEmail = sharedPref.getString("email", "user@example.com");

        if (username.equals(savedUsername) && password.equals(savedPassword)) {
            // ✅ Email MFA step
            Intent intent = new Intent(LoginActivity.this, VerifyCodeActivity.class);
            intent.putExtra("email", savedEmail);
            intent.putExtra("username", username);
            // Pass the final destination activity name to VerifyCodeActivity
            intent.putExtra("nextActivity", HomePageActivity.class.getName());
            startActivity(intent);
        } else {
            Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Launch Google Sign-In
    private void signInWithGoogle() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    // ✅ Handle Google Sign-In result
    private void handleGoogleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null) {
                firebaseAuthWithGoogle(account);
            } else {
                Toast.makeText(this, "Google Sign-In failed: Null account", Toast.LENGTH_SHORT).show();
            }
        } catch (ApiException e) {
            Toast.makeText(this, "Google Sign-In error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    // ✅ Authenticate Google account with Firebase
    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        String name = account.getDisplayName() != null ? account.getDisplayName() : "User";
                        String email = account.getEmail() != null ? account.getEmail() : "No email";

                        // ✅ Save session
                        SharedPreferences sharedPref = getSharedPreferences("UserPrefs", MODE_PRIVATE);
                        SharedPreferences.Editor editor = sharedPref.edit();
                        editor.putString("loggedInUsername", name);
                        editor.putString("loggedInEmail", email);
                        editor.apply();

                        // ✅ MFA step: go to verify code screen
                        Intent intent = new Intent(LoginActivity.this, VerifyCodeActivity.class);
                        intent.putExtra("email", email);
                        intent.putExtra("username", name);
                        // Pass the final destination activity name to VerifyCodeActivity
                        intent.putExtra("nextActivity", HomePageActivity.class.getName());
                        startActivity(intent);
                        finish();

                    } else {
                        Toast.makeText(this, "Firebase Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // ❌ Navigate to HomePageActivity (renamed from goToDashboard)
    // NOTE: This method is currently not used, as navigation goes through VerifyCodeActivity.
    private void goToHomePage(String username, String email) {
        Intent intent = new Intent(LoginActivity.this, HomePageActivity.class);
        intent.putExtra("username", username);
        intent.putExtra("email", email);
        startActivity(intent);
        finish();
    }
}