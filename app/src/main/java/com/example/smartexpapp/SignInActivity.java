package com.example.smartexpapp;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.example.smartexpapp.data.SettingsRepository;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.concurrent.Executors;

public class SignInActivity extends BaseActivity {
    private static final String TAG = "SignInActivity";

    private TextView welcomeTitle;
    private TextView subtitleText;
    private View displayNameLabel;
    private EditText displayNameInput;
    private EditText emailInput;
    private EditText passwordInput;
    private View signInButton;
    private View orSeparator;
    private View googleSignInButton;
    private View guestButton;
    private TextView registerTextLink;
    private View forgotPasswordLink;

    private boolean isRegisterMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        // Bind views
        welcomeTitle = findViewById(R.id.welcomeTitle);
        subtitleText = findViewById(R.id.subtitleText);
        displayNameLabel = findViewById(R.id.displayNameLabel);
        displayNameInput = findViewById(R.id.displayNameInput);
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        signInButton = findViewById(R.id.signInButton);
        orSeparator = findViewById(R.id.orSeparator);
        googleSignInButton = findViewById(R.id.googleSignInButton);
        guestButton = findViewById(R.id.guestButton);
        registerTextLink = findViewById(R.id.registerTextLink);
        forgotPasswordLink = findViewById(R.id.forgotPasswordLink);

        // Listeners
        signInButton.setOnClickListener(v -> handleSubmit());
        googleSignInButton.setOnClickListener(v -> handleGoogleSignIn());
        guestButton.setOnClickListener(v -> handleGuestSignIn());
        registerTextLink.setOnClickListener(v -> toggleMode());
        if (forgotPasswordLink != null) {
            forgotPasswordLink.setOnClickListener(v -> handleForgotPassword());
        }

        // Initial setup
        updateUI();
    }

    private FirebaseAuth getAuth() {
        if (FirebaseApp.getApps(this).isEmpty()) {
            return null;
        }
        return FirebaseAuth.getInstance();
    }

    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        updateUI();
    }

    private void updateUI() {
        if (isRegisterMode) {
            welcomeTitle.setText(R.string.register_title);
            subtitleText.setText(R.string.register_subtitle);
            displayNameLabel.setVisibility(View.VISIBLE);
            displayNameInput.setVisibility(View.VISIBLE);
            ((TextView) signInButton).setText(R.string.register_btn_label);
            orSeparator.setVisibility(View.GONE);
            googleSignInButton.setVisibility(View.GONE);
            guestButton.setVisibility(View.GONE);
            registerTextLink.setText(R.string.register_toggle_label);
            if (forgotPasswordLink != null) {
                forgotPasswordLink.setVisibility(View.GONE);
            }
        } else {
            welcomeTitle.setText("Welcome back!");
            subtitleText.setText("Please enter your details to sign in.");
            displayNameLabel.setVisibility(View.GONE);
            displayNameInput.setVisibility(View.GONE);
            ((TextView) signInButton).setText(R.string.signIn_btn_label);
            orSeparator.setVisibility(View.VISIBLE);
            googleSignInButton.setVisibility(View.VISIBLE);
            guestButton.setVisibility(View.VISIBLE);
            registerTextLink.setText("Don't have an account? Register");
            if (forgotPasswordLink != null) {
                forgotPasswordLink.setVisibility(View.VISIBLE);
            }
        }
    }

    private void setLoading(boolean loading) {
        View progressBar = findViewById(R.id.authProgressBar);
        if (progressBar != null) {
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        }

        emailInput.setEnabled(!loading);
        passwordInput.setEnabled(!loading);
        displayNameInput.setEnabled(!loading);

        signInButton.setEnabled(!loading);
        googleSignInButton.setEnabled(!loading);
        guestButton.setEnabled(!loading);
        registerTextLink.setEnabled(!loading);
        if (forgotPasswordLink != null) {
            forgotPasswordLink.setEnabled(!loading);
        }

        if (loading) {
            ((TextView) signInButton).setText(isRegisterMode ? R.string.registering_in_progress : R.string.signing_in_progress);
        } else {
            ((TextView) signInButton).setText(isRegisterMode ? R.string.register_btn_label : R.string.signIn_btn_label);
        }
    }

    private void handleSubmit() {
        String email = emailInput.getText().toString().trim();
        String password = passwordInput.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, R.string.login_error_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.login_error_invalid_email, Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, R.string.login_error_short_password, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth auth = getAuth();
        if (auth == null) {
            Toast.makeText(this, R.string.firebase_config_error, Toast.LENGTH_LONG).show();
            return;
        }

        setLoading(true);

        if (isRegisterMode) {
            String name = displayNameInput.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, R.string.display_name_empty_error, Toast.LENGTH_SHORT).show();
                setLoading(false);
                return;
            }

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            if (user != null) {
                                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                        .setDisplayName(name)
                                        .build();
                                user.updateProfile(profileUpdates)
                                        .addOnCompleteListener(profileTask -> {
                                            SettingsRepository.setDisplayNameAsync(this, name,
                                                    snapshot -> {
                                                        setLoading(false);
                                                        navigateHome();
                                                    },
                                                    error -> {
                                                        setLoading(false);
                                                        navigateHome();
                                                    });
                                        });
                            } else {
                                setLoading(false);
                                navigateHome();
                            }
                        } else {
                            setLoading(false);
                            String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Unknown error";
                            Toast.makeText(this, getString(R.string.registration_failed_format, errorMsg), Toast.LENGTH_LONG).show();
                        }
                    });
        } else {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            FirebaseUser user = auth.getCurrentUser();
                            String name = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : user.getEmail();
                            SettingsRepository.setDisplayNameAsync(this, name,
                                    snapshot -> {
                                        setLoading(false);
                                        navigateHome();
                                    },
                                    error -> {
                                        setLoading(false);
                                        navigateHome();
                                    });
                        } else {
                            setLoading(false);
                            String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Unknown error";
                            Toast.makeText(this, getString(R.string.login_failed_format, errorMsg), Toast.LENGTH_LONG).show();
                        }
                    });
        }
    }

    private void handleGoogleSignIn() {
        FirebaseAuth auth = getAuth();
        if (auth == null) {
            Toast.makeText(this, R.string.firebase_config_error, Toast.LENGTH_LONG).show();
            return;
        }

        // Web Client ID is auto-injected from google-services.json by GMS Plugin.
        // If the resource is not defined (dummy json build), we catch compiler/resource lookup exceptions.
        String webClientId = "";
        try {
            int resId = getResources().getIdentifier("default_web_client_id", "string", getPackageName());
            if (resId != 0) {
                webClientId = getString(resId);
            }
        } catch (Exception e) {
            Log.w(TAG, "Web Client ID resource default_web_client_id not found.", e);
        }

        if (webClientId.isEmpty() || webClientId.contains("placeholder")) {
            Toast.makeText(this, R.string.firebase_config_error, Toast.LENGTH_LONG).show();
            return;
        }

        CredentialManager credentialManager = CredentialManager.create(this);

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setServerClientId(webClientId)
                .setFilterByAuthorizedAccounts(false)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        setLoading(true);

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        Credential credential = result.getCredential();
                        if (credential instanceof CustomCredential &&
                                credential.getType().equals(GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
                            try {
                                GoogleIdTokenCredential googleIdTokenCredential =
                                        GoogleIdTokenCredential.createFrom(credential.getData());
                                String idToken = googleIdTokenCredential.getIdToken();
                                firebaseAuthWithGoogle(auth, idToken);
                            } catch (Exception e) {
                                runOnUiThread(() -> {
                                    setLoading(false);
                                    Toast.makeText(SignInActivity.this,
                                            getString(R.string.google_sign_in_failed, e.getLocalizedMessage()),
                                            Toast.LENGTH_LONG).show();
                                });
                            }
                        } else {
                            runOnUiThread(() -> {
                                setLoading(false);
                                Toast.makeText(SignInActivity.this,
                                        "Google sign in failed: Unexpected credential type",
                                        Toast.LENGTH_LONG).show();
                            });
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            Toast.makeText(SignInActivity.this,
                                    getString(R.string.google_sign_in_failed, e.getLocalizedMessage()),
                                    Toast.LENGTH_LONG).show();
                        });
                    }
                }
        );
    }

    private void firebaseAuthWithGoogle(FirebaseAuth auth, String idToken) {
        auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = auth.getCurrentUser();
                        String name = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "Google User";
                        SettingsRepository.setDisplayNameAsync(this, name,
                                snapshot -> {
                                    setLoading(false);
                                    navigateHome();
                                },
                                error -> {
                                    setLoading(false);
                                    navigateHome();
                                });
                    } else {
                        setLoading(false);
                        String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Unknown error";
                        Toast.makeText(this, getString(R.string.google_sign_in_failed, errorMsg), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void handleGuestSignIn() {
        FirebaseAuth auth = getAuth();
        setLoading(true);
        if (auth != null) {
            auth.signInAnonymously().addOnCompleteListener(this, task -> {
                getSharedPreferences("auth_prefs", MODE_PRIVATE).edit().putBoolean("guest_mode_enabled", true).apply();
                setLoading(false);
                navigateHome();
            });
        } else {
            getSharedPreferences("auth_prefs", MODE_PRIVATE).edit().putBoolean("guest_mode_enabled", true).apply();
            setLoading(false);
            navigateHome();
        }
    }

    private void handleForgotPassword() {
        String email = emailInput.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, R.string.forgot_password_email_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, R.string.login_error_invalid_email, Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseAuth auth = getAuth();
        if (auth == null) {
            Toast.makeText(this, R.string.firebase_config_error, Toast.LENGTH_LONG).show();
            return;
        }

        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.forgot_password_dialog_title)
                .setMessage(getString(R.string.forgot_password_dialog_message, email))
                .setPositiveButton(R.string.save_label, (dialog, which) -> {
                    setLoading(true);
                    auth.sendPasswordResetEmail(email)
                            .addOnCompleteListener(task -> {
                                setLoading(false);
                                if (task.isSuccessful()) {
                                    Toast.makeText(this, R.string.forgot_password_email_sent, Toast.LENGTH_LONG).show();
                                } else {
                                    String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Unknown error";
                                    Toast.makeText(this, getString(R.string.forgot_password_email_failed, errorMsg), Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void navigateHome() {
        Intent intent = new Intent(this, InventoryActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }
}
