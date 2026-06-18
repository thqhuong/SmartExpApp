package com.example.smartexpapp.data;

import android.content.Context;
import android.os.CancellationSignal;

import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.Executors;

public final class AuthStateRepository {
    private static final String AUTH_PREFS = "auth_prefs";
    private static final String KEY_GUEST_MODE_ENABLED = "guest_mode_enabled";
    private static volatile AuthState testAuthStateOverride;

    public static final class AuthState {
        private final boolean firebaseConfigured;
        private final boolean signedIn;
        private final boolean guest;
        private final String userId;
        private final String displayName;
        private final String email;

        private AuthState(
                boolean firebaseConfigured,
                boolean signedIn,
                boolean guest,
                String userId,
                String displayName,
                String email
        ) {
            this.firebaseConfigured = firebaseConfigured;
            this.signedIn = signedIn;
            this.guest = guest;
            this.userId = userId;
            this.displayName = displayName;
            this.email = email;
        }

        public static AuthState signedIn(String userId, String displayName, String email) {
            return new AuthState(true, true, false, userId, displayName, email);
        }

        public static AuthState guest(boolean firebaseConfigured) {
            return new AuthState(firebaseConfigured, false, true, null, null, null);
        }

        public static AuthState localOnly(boolean firebaseConfigured) {
            return new AuthState(firebaseConfigured, false, false, null, null, null);
        }

        public boolean isFirebaseConfigured() {
            return firebaseConfigured;
        }

        public boolean isSignedIn() {
            return signedIn;
        }

        public boolean isGuest() {
            return guest;
        }

        public String getUserId() {
            return userId;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getEmail() {
            return email;
        }

        public String getBestDisplayName(String fallback) {
            if (displayName != null && !displayName.trim().isEmpty()) {
                return displayName.trim();
            }
            if (email != null && !email.trim().isEmpty()) {
                return email.trim();
            }
            return fallback;
        }
    }

    private AuthStateRepository() {
    }

    public static AuthState getAuthState(Context context) {
        if (testAuthStateOverride != null) {
            return testAuthStateOverride;
        }

        boolean firebaseConfigured = false;
        FirebaseUser user = null;
        try {
            firebaseConfigured = !FirebaseApp.getApps(context).isEmpty();
            if (firebaseConfigured) {
                user = FirebaseAuth.getInstance().getCurrentUser();
            }
        } catch (Exception ignored) {
            firebaseConfigured = false;
        }

        if (user != null && !user.isAnonymous()) {
            markGuestMode(context, false);
            return AuthState.signedIn(user.getUid(), user.getDisplayName(), user.getEmail());
        }

        boolean guestMode = user != null && user.isAnonymous();
        guestMode = guestMode || context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_GUEST_MODE_ENABLED, false);
        return guestMode ? AuthState.guest(firebaseConfigured) : AuthState.localOnly(firebaseConfigured);
    }

    public static String getSignedInOwnerUserId(Context context) {
        AuthState state = getAuthState(context);
        return state.isSignedIn() ? state.getUserId() : null;
    }

    public static void markGuestMode(Context context, boolean enabled) {
        context.getSharedPreferences(AUTH_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_GUEST_MODE_ENABLED, enabled)
                .apply();
    }

    public static void signOut(Context context) {
        try {
            if (!FirebaseApp.getApps(context).isEmpty()) {
                FirebaseAuth.getInstance().signOut();
            }
        } catch (Exception ignored) {
        }

        markGuestMode(context, false);
        clearCredentialState(context);
    }

    public static void setTestAuthStateOverride(AuthState authState) {
        testAuthStateOverride = authState;
    }

    public static void clearTestAuthStateOverride() {
        testAuthStateOverride = null;
    }

    private static void clearCredentialState(Context context) {
        try {
            CredentialManager.create(context).clearCredentialStateAsync(
                    new ClearCredentialStateRequest(),
                    new CancellationSignal(),
                    Executors.newSingleThreadExecutor(),
                    new CredentialManagerCallback<Void, ClearCredentialException>() {
                        @Override
                        public void onResult(Void result) {
                        }

                        @Override
                        public void onError(ClearCredentialException e) {
                        }
                    }
            );
        } catch (Exception ignored) {
        }
    }
}
