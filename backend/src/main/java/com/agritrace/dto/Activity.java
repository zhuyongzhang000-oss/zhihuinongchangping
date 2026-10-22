package com.agritrace.dto;

import java.time.LocalDateTime;

/** 首页「最新动态」的一条 */
public record Activity(String kind, String title, String batchNo, LocalDateTime time) {
}
