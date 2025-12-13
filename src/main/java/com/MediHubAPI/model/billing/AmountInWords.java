package com.MediHubAPI.model.billing;

import java.text.DecimalFormat;

public final class AmountInWords {
    private AmountInWords() {}
    private static final String[] tens = {"", " Ten", " Twenty", " Thirty", " Forty", " Fifty", " Sixty", " Seventy", " Eighty", " Ninety"};
    private static final String[] ones = {"", " One", " Two", " Three", " Four", " Five", " Six", " Seven", " Eight", " Nine", " Ten", " Eleven", " Twelve", " Thirteen", " Fourteen", " Fifteen", " Sixteen", " Seventeen", " Eighteen", " Nineteen"};

    private static String lt1000(int n) {
        String cur;
        if (n % 100 < 20) { cur = ones[n % 100]; n /= 100; }
        else { cur = ones[n % 10]; n /= 10; cur = tens[n % 10] + cur; n /= 10; }
        if (n == 0) return cur;
        return ones[n] + " Hundred" + cur;
    }
    public static String rupees(long n) {
        if (n == 0) return "Zero";
        String s = new DecimalFormat("000000000").format(n);
        int crore = Integer.parseInt(s.substring(0,2));
        int lakh  = Integer.parseInt(s.substring(2,4));
        int thou  = Integer.parseInt(s.substring(4,6));
        int hund  = Integer.parseInt(s.substring(6,9));
        String res = "";
        if (crore != 0) res += lt1000(crore) + " Crore";
        if (lakh  != 0) res += lt1000(lakh)  + " Lakh";
        if (thou  != 0) res += lt1000(thou)  + " Thousand";
        if (hund  != 0) res += lt1000(hund);
        return res.trim();
    }
}
