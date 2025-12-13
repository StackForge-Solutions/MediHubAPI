package com.MediHubAPI.util;

import java.time.LocalDate;
import java.time.Period;

public final class AgeUtil {
    private AgeUtil(){}
    public static String ageYMD(LocalDate dob, LocalDate asOf) {
        if (dob == null || asOf == null) return "";
        Period p = Period.between(dob, asOf);
        return String.format("%d YRS %d MTHS %d DAYS", p.getYears(), p.getMonths(), p.getDays());
    }
}
