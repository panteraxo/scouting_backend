package com.scouting.scouting.player;

import com.scouting.scouting.player.service.PlayerTime;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerTimeTest {

    private final LocalDate today = LocalDate.of(2026, 10, 20);

    @Test
    void calculatesMinorityOnTheEighteenthBirthday() {
        assertTrue(PlayerTime.isMinor(today.minusYears(17).minusDays(1), today));
        assertFalse(PlayerTime.isMinor(today.minusYears(18), today));
        assertEquals(17, PlayerTime.age(today.minusYears(17), today));
    }

    @Test
    void handlesLeapDayBirthDates() {
        assertEquals(17, PlayerTime.age(LocalDate.of(2008, 2, 29), LocalDate.of(2026, 2, 28)));
        assertEquals(18, PlayerTime.age(LocalDate.of(2008, 2, 29), LocalDate.of(2026, 3, 1)));
        assertEquals(17, PlayerTime.age(LocalDate.of(2008, 2, 29), LocalDate.of(2026, 2, 27)));
    }

    @Test
    void ageRangeBirthDateBoundariesAreConsistent() {
        assertEquals(today.minusYears(15), PlayerTime.minBirthDateForAge(15, today));
        assertEquals(today.minusYears(16), PlayerTime.maxBirthDateExclusiveForAge(15, today));
    }
}
