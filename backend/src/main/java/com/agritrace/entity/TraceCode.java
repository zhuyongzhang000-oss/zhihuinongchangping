package com.agritrace.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/** 溯源码 */
@Data
@TableName("trace_code")
public class TraceCode {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private Long batchId;
    private Integer scanCount;
    private LocalDateTime lastScanAt;
    private Integer status;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String batchNo;
    @TableField(exist = false)
    private String productName;
    @TableField(exist = false)
    private String baseName;
    @TableField(exist = false)
    private String batchStatus;
}
