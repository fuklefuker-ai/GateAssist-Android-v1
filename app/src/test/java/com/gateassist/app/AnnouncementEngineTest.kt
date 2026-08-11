package com.gateassist.app

import org.junit.Assert.*
import org.junit.Test

class AnnouncementEngineTest {
    private val f = AnnouncementEngine.flights.first()

    @Test fun finalCallContainsCriticalFacts() {
        val o = AnnouncementEngine.generate(AnnouncementType.FINAL, f, "92", "17:10", "")
        assertTrue(o.english.contains("UO871"))
        assertTrue(o.english.contains("Hong Kong"))
        assertTrue(o.english.contains("Gate 92"))
        assertTrue(o.english.contains("17:10"))
        assertTrue(o.warnings.isEmpty())
    }

    @Test fun pagingRequiresPassenger() {
        val o = AnnouncementEngine.generate(AnnouncementType.PAGING, f, "92", "17:10", "")
        assertTrue(o.warnings.any { it.contains("Passenger name") })
    }

    @Test fun delayRequiresUpdatedTime() {
        val o = AnnouncementEngine.generate(AnnouncementType.DELAY, f, "92", "", "")
        assertTrue(o.warnings.any { it.contains("Updated departure") })
    }

    @Test fun gateChangeRequiresNewGate() {
        val o = AnnouncementEngine.generate(AnnouncementType.GATE_CHANGE, f, "92", "17:10", "")
        assertTrue(o.warnings.any { it.contains("New gate") })
    }

    @Test fun gateChangeContainsBothGates() {
        val o = AnnouncementEngine.generate(AnnouncementType.GATE_CHANGE, f, "92", "17:10", "95")
        assertTrue(o.english.contains("Gate 92"))
        assertTrue(o.english.contains("Gate 95"))
        assertTrue(o.warnings.isEmpty())
    }

    @Test fun volunteerRequiresAlternativeDetails() {
        val o = AnnouncementEngine.generate(AnnouncementType.VOLUNTEER, f, "92", "17:10", "")
        assertTrue(o.warnings.any { it.contains("Alternative flight") })
    }

    @Test fun speechSafeExpandsFlightAndGate() {
        val s = AnnouncementEngine.speechSafeEnglish("Flight UO871 proceed to Gate 92")
        assertTrue(s.contains("U O eight seven one"))
        assertTrue(s.contains("Gate ninety two"))
    }
}
