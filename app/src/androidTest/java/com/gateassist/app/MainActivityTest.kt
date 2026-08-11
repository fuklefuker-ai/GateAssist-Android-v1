package com.gateassist.app

import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityTest {
    @get:Rule
    val rule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun appLaunchesAndGeneratesAnnouncement() {
        onView(withText("GateAssist")).check(matches(isDisplayed()))
        onView(withText("Generate & Safety Check")).perform(click())
        onView(withText("English")).check(matches(isDisplayed()))
        onView(withText("I checked the airline, flight, destination, gate and times.")).check(matches(isDisplayed()))
    }
}
