package com.example.smartexpapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

public class ExampleUnitTest {
    @Test
    public void buildConfigUsesExpectedApplicationId() {
        assertEquals("com.example.smartexpapp", BuildConfig.APPLICATION_ID);
        assertFalse(BuildConfig.VERSION_NAME.trim().isEmpty());
    }
}
