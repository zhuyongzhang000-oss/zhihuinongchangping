package com.agritrace.common;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Predicate;

/** 生成「前缀 + 日期 + 流水号」形式的业务编号，例如 B20261022-001、JC2026102201 */
public final class NoGenerator {
    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    public static String next(String prefix, String sep, LocalDate date, int digits, Predicate<String> exists) {
        String head = prefix + (date == null ? LocalDate.now() : date).format(DAY) + sep;
        for (int i = 1; i < Math.pow(10, digits); i++) {
            String no = head + String.format("%0" + digits + "d", i);
            if (!exists.test(no)) {
                return no;
            }
        }
        throw new BusinessException("当天编号已用完");
    }

    private NoGenerator() {
    }
}
