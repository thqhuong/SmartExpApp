package com.example.smartexpapp.util;

import org.junit.Test;
import static org.junit.Assert.*;

public class EmailValidatorTest {

    @Test
    public void testValidEmails() {
        assertTrue(EmailValidator.isValid("chef@smartexp.comi"));
        assertTrue(EmailValidator.isValid("chef@smartexp.com"));
        assertTrue(EmailValidator.isValid("chef.one+two-three_four@sub.smartexp.comi"));
        assertTrue(EmailValidator.isValid("user@mail.co.uk"));
    }

    @Test
    public void testInvalidEmails() {
        assertFalse(EmailValidator.isValid(null));
        assertFalse(EmailValidator.isValid(""));
        assertFalse(EmailValidator.isValid("   "));
        assertFalse(EmailValidator.isValid("chef"));
        assertFalse(EmailValidator.isValid("chef@smartexp"));
        assertFalse(EmailValidator.isValid("@smartexp.com"));
        assertFalse(EmailValidator.isValid("chef@.com"));
        assertFalse(EmailValidator.isValid("chef@smartexp."));
        assertFalse(EmailValidator.isValid("chef@smartexp.c"));
    }
}
