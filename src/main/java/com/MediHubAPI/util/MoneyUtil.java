// src/main/java/com/MediHubAPI/billing/service/MoneyUtil.java
package com.MediHubAPI.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtil {
    private MoneyUtil(){}

    public static BigDecimal bd(double v){ return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP); }

    public static BigDecimal round(BigDecimal v){ return v.setScale(2, RoundingMode.HALF_UP); }
}
