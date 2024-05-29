package com.asuala.mock.m3u8.utils;

import java.math.BigDecimal;

/**
 * @author liyaling
 * @email ts_liyaling@qq.com
 * @date 2019/12/14 16:27
 */

public class StringUtils {

    public static boolean isBlank(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean isEmpty(String str) {
        return str == null || str.trim().length() == 0;
    }

    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    public static final BigDecimal unit = new BigDecimal(1);
    public static final BigDecimal kb = new BigDecimal(1 << 10);
    public static final BigDecimal mb = new BigDecimal(1 << 10).multiply(kb);
    public static final BigDecimal gb = new BigDecimal(1 << 10).multiply(mb);
    public static final BigDecimal tb = new BigDecimal(1 << 10).multiply(gb);
    public static final BigDecimal pb = new BigDecimal(1 << 10).multiply(tb);
    public static final BigDecimal eb = new BigDecimal(1 << 10).multiply(pb);

    public static boolean isUrl(String str) {
        if (isEmpty(str))
            return false;
        str = str.trim();
        return str.matches("^(http|https)://.+");
    }

    public static String convertToDownloadSpeed(BigDecimal bigDecimal, int scale) {
//        bigDecimal = bigDecimal.divide(new BigDecimal("5"));

        if (bigDecimal.divide(kb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0)
            return bigDecimal.divide(unit, scale, BigDecimal.ROUND_HALF_UP).toString() + " B";
        else if (bigDecimal.divide(mb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0)
            return bigDecimal.divide(kb, scale, BigDecimal.ROUND_HALF_UP).toString() + " KB";
        else if (bigDecimal.divide(gb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0)
            return bigDecimal.divide(mb, scale, BigDecimal.ROUND_HALF_UP).toString() + " MB";
        else if (bigDecimal.divide(tb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0)
            return bigDecimal.divide(gb, scale, BigDecimal.ROUND_HALF_UP).toString() + " GB";
        else if (bigDecimal.divide(pb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0)
            return bigDecimal.divide(tb, scale, BigDecimal.ROUND_HALF_UP).toString() + " TB";
        else if (bigDecimal.divide(eb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0)
            return bigDecimal.divide(pb, scale, BigDecimal.ROUND_HALF_UP).toString() + " PB";
        return bigDecimal.divide(eb, scale, BigDecimal.ROUND_HALF_UP).toString() + " EB";
    }

    public static String convertToDownloadSpeedNomal(BigDecimal bigDecimal, int scale) {
        if (bigDecimal.divide(kb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0)
            return bigDecimal.divide(unit, scale, BigDecimal.ROUND_HALF_UP).toString() + " B";
        else if (bigDecimal.divide(mb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0)
            return bigDecimal.divide(kb, scale, BigDecimal.ROUND_HALF_UP).toString() + " KB";
        else if (bigDecimal.divide(gb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0)
            return bigDecimal.divide(mb, scale, BigDecimal.ROUND_HALF_UP).toString() + " MB";
        else if (bigDecimal.divide(tb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0)
            return bigDecimal.divide(gb, scale, BigDecimal.ROUND_HALF_UP).toString() + " GB";
        else if (bigDecimal.divide(pb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0)
            return bigDecimal.divide(tb, scale, BigDecimal.ROUND_HALF_UP).toString() + " TB";
        else if (bigDecimal.divide(eb, scale, BigDecimal.ROUND_HALF_UP).compareTo(unit) < 0)
            return bigDecimal.divide(pb, scale, BigDecimal.ROUND_HALF_UP).toString() + " PB";
        return bigDecimal.divide(eb, scale, BigDecimal.ROUND_HALF_UP).toString() + " EB";
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        if ((len & 1) == 1) {
            s = "0" + s;
            len++;
        }
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

}
