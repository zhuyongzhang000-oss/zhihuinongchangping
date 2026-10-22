package com.agritrace.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 扫码日志 */
@Data
@TableName("trace_scan_log")
public class TraceScanLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private Long batchId;
    private String ip;
    private String userAgent;
    private LocalDateTime scannedAt;
}
