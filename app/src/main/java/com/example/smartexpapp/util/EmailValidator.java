package com.example.smartexpapp.util;

import java.util.regex.Pattern;

public final class EmailValidator {
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[\\w.+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
    );

    private EmailValidator() {
    }

    public static boolean isValid(String email) {
        if (email == null) {
            return false;
        }
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }
}
