package com.example.smartexpapp;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
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

import com.example.smartexpapp.data.AuthStateRepository;
import com.example.smartexpapp.data.firestore.FirestoreProvider;
import com.example.smartexpapp.data.firestore.ProductSyncRepository;
import com.example.smartexpapp.data.firestore.UserDataSyncRepository;
import com.example.smartexpapp.util.EmailValidator;
import com.example.smartexpapp.data.local.AppDatabase;
import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.example.smartexpapp.data.SettingsRepository;
import com.example.smartexpapp.model.Product;
import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.auth.UserProfileChangeRequest;

import java.util.concurrent.Executors;
import android.os.CountDownTimer;
import android.graphics.drawable.ColorDrawable;
import android.graphics.Color;

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
    private AlertDialog verificationDialog;
    private CountDownTimer verificationTimer;

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
            showWarningNotification(getString(R.string.login_error_empty));
            return;
        }

        if (!EmailValidator.isValid(email)) {
            showWarningNotification(getString(R.string.login_error_invalid_email));
            return;
        }

        if (password.length() < 6) {
            showWarningNotification(getString(R.string.login_error_short_password));
            return;
        }

        FirebaseAuth auth = getAuth();
        if (auth == null) {
            showErrorNotification(getString(R.string.firebase_config_error));
            return;
        }

        setLoading(true);

        if (isRegisterMode) {
            String name = displayNameInput.getText().toString().trim();
            if (name.isEmpty()) {
                showWarningNotification(getString(R.string.display_name_empty_error));
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
                                            user.sendEmailVerification()
                                                    .addOnCompleteListener(verificationSendTask -> {
                                                        if (verificationSendTask.isSuccessful()) {
                                                            showEmailVerificationDialog(user, name);
                                                        } else {
                                                            setLoading(false);
                                                            String errorMsg = verificationSendTask.getException() != null ? verificationSendTask.getException().getLocalizedMessage() : "Failed to send verification email.";
                                                            showErrorNotification(errorMsg);
                                                        }
                                                    });
                                        });
                            } else {
                                setLoading(false);
                                showErrorNotification("Failed to get current authenticated user.");
                            }
                        } else {
                            setLoading(false);
                            String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Unknown error";
                            showErrorNotification(getString(R.string.registration_failed_format, errorMsg));
                        }
                    });
        } else {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this, task -> {
                        if (task.isSuccessful()) {
                            AuthStateRepository.markGuestMode(this, false);
                            FirebaseUser user = auth.getCurrentUser();
                            String name = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : user.getEmail();
                            SettingsRepository.setDisplayNameAsync(this, name,
                                    snapshot -> {
                                        continueAfterSignedIn();
                                    },
                                    error -> {
                                        continueAfterSignedIn();
                                    });
                        } else {
                            setLoading(false);
                            String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Unknown error";
                            showErrorNotification(getString(R.string.login_failed_format, errorMsg));
                        }
                    });
        }
    }

    private void handleGoogleSignIn() {
        FirebaseAuth auth = getAuth();
        if (auth == null) {
            showErrorNotification(getString(R.string.firebase_config_error));
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
            showErrorNotification(getString(R.string.firebase_config_error));
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
                                    showErrorNotification(getString(R.string.google_sign_in_failed, e.getLocalizedMessage()));
                                });
                            }
                        } else {
                            runOnUiThread(() -> {
                                setLoading(false);
                                showErrorNotification("Google sign in failed: Unexpected credential type");
                            });
                        }
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        runOnUiThread(() -> {
                            setLoading(false);
                            showErrorNotification(getString(R.string.google_sign_in_failed, e.getLocalizedMessage()));
                        });
                    }
                }
        );
    }

    private void firebaseAuthWithGoogle(FirebaseAuth auth, String idToken) {
        runOnUiThread(() -> auth.signInWithCredential(GoogleAuthProvider.getCredential(idToken, null))
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        AuthStateRepository.markGuestMode(this, false);
                        FirebaseUser user = auth.getCurrentUser();
                        String name = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "Google User";
                        SettingsRepository.setDisplayNameAsync(this, name,
                                snapshot -> {
                                    continueAfterSignedIn();
                                },
                                error -> {
                                    continueAfterSignedIn();
                                });
                    } else {
                        setLoading(false);
                        String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Unknown error";
                        showErrorNotification(getString(R.string.google_sign_in_failed, errorMsg));
                    }
                }));
    }

    private void handleGuestSignIn() {
        AuthStateRepository.markGuestMode(this, true);
        FirebaseAuth auth = getAuth();
        if (auth != null && FirestoreProvider.isConfigured(this)) {
            // Attempt anonymous sign-in in the background to get a Firebase UID if needed,
            // but do not block the user or wait for it.
            auth.signInAnonymously();
        }
        navigateToDashboard();
    }

    private void handleForgotPassword() {
        String email = emailInput.getText().toString().trim();
        if (email.isEmpty()) {
            showWarningNotification(getString(R.string.forgot_password_email_empty));
            return;
        }

        if (!EmailValidator.isValid(email)) {
            showWarningNotification(getString(R.string.login_error_invalid_email));
            return;
        }

        FirebaseAuth auth = getAuth();
        if (auth == null) {
            showErrorNotification(getString(R.string.firebase_config_error));
            return;
        }

        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(R.layout.dialog_forgot_password_confirm)
                .create();
        android.view.Window window = dialog.getWindow();
        if (window != null) {
            window.setDimAmount(0.6f);
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(
                    android.graphics.Color.TRANSPARENT));
        }
        dialog.show();

        ((TextView) dialog.findViewById(R.id.dialogMessage)).setText(
                getString(R.string.forgot_password_dialog_message, email));

        dialog.findViewById(R.id.dialogConfirm).setOnClickListener(v -> {
            dialog.dismiss();
            setLoading(true);
            auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        setLoading(false);
                        if (task.isSuccessful()) {
                            showSuccessNotification(getString(R.string.forgot_password_email_sent));
                        } else {
                            String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Unknown error";
                            showErrorNotification(getString(R.string.forgot_password_email_failed, errorMsg));
                        }
                    });
        });

        dialog.findViewById(R.id.dialogCancel).setOnClickListener(v -> dialog.dismiss());
    }

    private void navigateToDashboard() {
        Intent intent = new Intent(this, StatsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
        overridePendingTransition(0, 0);
    }

    private void continueAfterSignedIn() {
        Context appContext = getApplicationContext();
        ProductSyncRepository.stopProductListener();
        Executors.newSingleThreadExecutor().execute(() -> {
            AppDatabase database = AppDatabase.getInstance(appContext);
            int ownerlessCount = database.productDao().countOwnerlessProducts();
            runOnUiThread(() -> {
                if (ownerlessCount > 0) {
                    setLoading(false);
                    promptImportLocalInventory(database);
                } else {
                    runInitialProductSync(database);
                }
            });
        });
    }

    private void promptImportLocalInventory(AppDatabase database) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.import_local_inventory_title)
                .setMessage(R.string.import_local_inventory_message)
                .setPositiveButton(R.string.import_local_inventory_action, (dialog, which) -> importOwnerlessInventory(database))
                .setNegativeButton(R.string.keep_local_inventory_action, (dialog, which) -> {
                    setLoading(true);
                    runInitialProductSync(database);
                })
                .setOnCancelListener(dialog -> {
                    setLoading(true);
                    runInitialProductSync(database);
                })
                .show();
    }

    private void importOwnerlessInventory(AppDatabase database) {
        setLoading(true);
        Context appContext = getApplicationContext();
        Executors.newSingleThreadExecutor().execute(() -> {
            AuthStateRepository.AuthState authState = AuthStateRepository.getAuthState(appContext);
            if (authState.isSignedIn() && authState.getUserId() != null && !authState.getUserId().trim().isEmpty()) {
                database.productDao().assignOwnerToOwnerlessProducts(
                        authState.getUserId(),
                        Product.SYNC_STATUS_PENDING_UPLOAD,
                        System.currentTimeMillis()
                );
            }
            runOnUiThread(() -> runInitialProductSync(database));
        });
    }

    private void runInitialProductSync(AppDatabase database) {
        try {
            ProductSyncRepository.initialSyncAsync(this, database,
                    ignored -> {
                        UserDataSyncRepository.syncUserDataAsync(this, database);
                        setLoading(false);
                        navigateToDashboard();
                    },
                    error -> {
                        Log.w(TAG, "Initial product sync failed; continuing with scoped cache.", error);
                        UserDataSyncRepository.syncUserDataAsync(this, database);
                        setLoading(false);
                        navigateToDashboard();
                    });
        } catch (RuntimeException error) {
            Log.w(TAG, "Initial product sync skipped.", error);
            setLoading(false);
            navigateToDashboard();
        }
    }

    private void showEmailVerificationDialog(FirebaseUser user, String name) {
        if (verificationTimer != null) {
            verificationTimer.cancel();
            verificationTimer = null;
        }
        if (verificationDialog != null && verificationDialog.isShowing()) {
            verificationDialog.dismiss();
        }

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_verify_email, null);
        
        TextView verifySubtitle = dialogView.findViewById(R.id.verifySubtitle);
        verifySubtitle.setText(getString(R.string.verify_subtitle, user.getEmail()));

        View btnCheckStatus = dialogView.findViewById(R.id.verifyBtnCheckStatus);
        TextView timerText = dialogView.findViewById(R.id.verifyTimerText);
        TextView resendBtn = dialogView.findViewById(R.id.verifyResendBtn);
        View btnCancel = dialogView.findViewById(R.id.verifyBtnCancel);

        verificationDialog = new AlertDialog.Builder(this)
                .setView(dialogView)
                .setCancelable(false)
                .create();

        if (verificationDialog.getWindow() != null) {
            verificationDialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        startVerificationResendTimer(timerText, resendBtn);

        btnCheckStatus.setOnClickListener(v -> {
            btnCheckStatus.setEnabled(false);
            user.reload().addOnCompleteListener(task -> {
                btnCheckStatus.setEnabled(true);
                if (task.isSuccessful()) {
                    if (user.isEmailVerified()) {
                        if (verificationTimer != null) {
                            verificationTimer.cancel();
                            verificationTimer = null;
                        }
                        verificationDialog.dismiss();
                        
                        AuthStateRepository.markGuestMode(this, false);
                        SettingsRepository.setDisplayNameAsync(this, name,
                                snapshot -> continueAfterSignedIn(),
                                error -> continueAfterSignedIn());
                    } else {
                        showWarningNotification(getString(R.string.verify_error_not_verified));
                    }
                } else {
                    String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Unknown error";
                    showErrorNotification(errorMsg);
                }
            });
        });

        resendBtn.setOnClickListener(v -> {
            resendBtn.setEnabled(false);
            user.sendEmailVerification().addOnCompleteListener(task -> {
                resendBtn.setEnabled(true);
                if (task.isSuccessful()) {
                    showSuccessNotification(getString(R.string.verify_code_resent));
                    startVerificationResendTimer(timerText, resendBtn);
                } else {
                    String errorMsg = task.getException() != null ? task.getException().getLocalizedMessage() : "Unknown error";
                    showErrorNotification(errorMsg);
                }
            });
        });

        btnCancel.setOnClickListener(v -> {
            if (verificationTimer != null) {
                verificationTimer.cancel();
                verificationTimer = null;
            }
            FirebaseAuth.getInstance().signOut();
            verificationDialog.dismiss();
            setLoading(false);
        });

        verificationDialog.setOnDismissListener(dialogInterface -> {
            if (verificationTimer != null) {
                verificationTimer.cancel();
                verificationTimer = null;
            }
        });

        verificationDialog.show();
    }

    private void startVerificationResendTimer(TextView timerText, TextView resendBtn) {
        if (verificationTimer != null) {
            verificationTimer.cancel();
        }
        resendBtn.setVisibility(View.GONE);
        timerText.setVisibility(View.VISIBLE);

        verificationTimer = new CountDownTimer(60000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timerText.setText(getString(R.string.verify_timer_format, (int) (millisUntilFinished / 1000)));
            }

            @Override
            public void onFinish() {
                timerText.setVisibility(View.GONE);
                resendBtn.setVisibility(View.VISIBLE);
            }
        }.start();
    }

    @Override
    protected void onDestroy() {
        if (verificationTimer != null) {
            verificationTimer.cancel();
            verificationTimer = null;
        }
        if (verificationDialog != null && verificationDialog.isShowing()) {
            verificationDialog.dismiss();
        }
        super.onDestroy();
    }
}
