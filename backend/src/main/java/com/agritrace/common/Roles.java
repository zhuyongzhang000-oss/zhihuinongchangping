package com.agritrace.common;

/** 角色常量 */
public final class Roles {
    public static final String ADMIN = "ADMIN";
    public static final String FARMER = "FARMER";
    public static final String INSPECTOR = "INSPECTOR";
    public static final String LOGISTICS = "LOGISTICS";

    public static boolean valid(String role) {
        return ADMIN.equals(role) || FARMER.equals(role) || INSPECTOR.equals(role) || LOGISTICS.equals(role);
    }

    private Roles() {
    }
}
