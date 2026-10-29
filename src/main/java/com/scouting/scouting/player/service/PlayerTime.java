package com.scouting.scouting.player.service;

import java.time.LocalDate;
import java.time.Period;
import java.time.ZoneId;

/** Reglas de edad y de fechas de corte usadas por SCP-005. */
public final class PlayerTime {

    public static final ZoneId LIMA = ZoneId.of("America/Lima");
    public static final int MAJORITY_AGE = 18;

    private PlayerTime() {
    }

    public static LocalDate today() {
        return LocalDate.now(LIMA);
    }

    public static int age(LocalDate birthDate, LocalDate reference) {
        return Period.between(birthDate, reference).getYears();
    }

    public static boolean isMinor(LocalDate birthDate, LocalDate reference) {
        return age(birthDate, reference) < MAJORITY_AGE;
    }

    public static LocalDate minBirthDateForAge(int age, LocalDate reference) {
        return reference.minusYears(age);
    }

    public static LocalDate maxBirthDateExclusiveForAge(int age, LocalDate reference) {
        return reference.minusYears((long) age + 1);
    }
}
