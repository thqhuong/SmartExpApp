package com.example.smartexpapp.data;

import android.content.Context;

final class AccountInventoryScope {
    enum Kind {
        OWNER,
        LOCAL,
        BLOCKED
    }

    final Kind kind;
    final String ownerUserId;

    private AccountInventoryScope(Kind kind, String ownerUserId) {
        this.kind = kind;
        this.ownerUserId = ownerUserId;
    }

    static AccountInventoryScope resolve(Context context) {
        AuthStateRepository.AuthState authState = AuthStateRepository.getAuthState(context);
        if (authState.isSignedIn() && hasText(authState.getUserId())) {
            return new AccountInventoryScope(Kind.OWNER, authState.getUserId());
        }
        if (authState.isGuest()) {
            return new AccountInventoryScope(Kind.LOCAL, null);
        }
        return new AccountInventoryScope(Kind.BLOCKED, null);
    }

    boolean isOwner() {
        return kind == Kind.OWNER;
    }

    boolean isLocal() {
        return kind == Kind.LOCAL;
    }

    boolean isBlocked() {
        return kind == Kind.BLOCKED;
    }

    boolean canSeeOwner(String productOwnerUserId) {
        if (isOwner()) {
            return ownerUserId.equals(productOwnerUserId);
        }
        return isLocal() && !hasText(productOwnerUserId);
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
