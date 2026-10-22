package com.agritrace.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 农事记录 */
@Data
@TableName("farming_record")
public class FarmingRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotNull(message = "请选择批次")
    private Long batchId;
    /** SOW/FERTILIZE/IRRIGATE/PEST/WEED/HARVEST/OTHER */
    @NotBlank(message = "请选择农事类型")
    private String type;
    @NotNull(message = "请选择作业日期")
    private LocalDate opDate;
    private String operator;
    private String material;
    private String dosage;
    private String description;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long createdBy;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String batchNo;
    @TableField(exist = false)
    private String productName;
}
