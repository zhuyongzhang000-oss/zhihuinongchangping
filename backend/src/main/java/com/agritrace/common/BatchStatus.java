package com.agritrace.common;

import java.util.Set;

/**
 * 批次状态。流转：
 * GROWING 种植中 → HARVESTED 已采收 → INSPECTED 质检合格 / REJECTED 质检不合格 → SHIPPING 运输中 → ON_SALE 已上市
 */
public final class BatchStatus {
    public static final String GROWING = "GROWING";
    public static final String HARVESTED = "HARVESTED";
    public static final String INSPECTED = "INSPECTED";
    public static final String REJECTED = "REJECTED";
    public static final String SHIPPING = "SHIPPING";
    public static final String ON_SALE = "ON_SALE";

    /** 质检合格之后的状态：可以发运、可以生成溯源码 */
    public static final Set<String> PASSED = Set.of(INSPECTED, SHIPPING, ON_SALE);

    public static final Set<String> ALL = Set.of(GROWING, HARVESTED, INSPECTED, REJECTED, SHIPPING, ON_SALE);

    private BatchStatus() {
    }
}
