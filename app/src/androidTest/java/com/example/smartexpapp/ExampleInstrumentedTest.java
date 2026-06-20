package com.example.smartexpapp;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.smartexpapp.data.AuthStateRepository;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    @After
    public void tearDown() {
        AuthStateRepository.clearTestAuthStateOverride();
    }

    @Test
    public void inventoryLaunchesForGuestMode() {
        AuthStateRepository.setTestAuthStateOverride(AuthStateRepository.AuthState.guest(true));

        try (ActivityScenario<InventoryActivity> ignored = ActivityScenario.launch(InventoryActivity.class)) {
            onView(withId(R.id.searchInput)).check(matches(isDisplayed()));
            onView(withId(R.id.expirySpinner)).check(matches(isDisplayed()));
            onView(withId(R.id.storageSpinner)).check(matches(isDisplayed()));
            onView(withId(R.id.sortSpinner)).check(matches(isDisplayed()));
        }
    }
}
