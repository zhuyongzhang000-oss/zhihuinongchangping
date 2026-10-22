package com.agritrace.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 生产批次 */
@Data
@TableName("batch")
public class Batch {
    @TableId(type = IdType.AUTO)
    private Long id;
    /** 为空时后端自动生成 */
    private String batchNo;
    @NotNull(message = "请选择产品")
    private Long productId;
    @NotNull(message = "请选择基地")
    private Long baseId;
    private LocalDate plantDate;
    private LocalDate harvestDate;
    private BigDecimal quantity;
    private String status;
    private String remark;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long createdBy;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;

    // ↓ 以下字段不在表里，查询时填充给前端展示
    @TableField(exist = false)
    private String productName;
    @TableField(exist = false)
    private String category;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private String baseName;
    @TableField(exist = false)
    private String traceCode;
}
