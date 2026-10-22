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

/** 质检记录 */
@Data
@TableName("inspection")
public class Inspection {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotNull(message = "请选择批次")
    private Long batchId;
    /** 为空时后端自动生成 */
    private String reportNo;
    @NotNull(message = "请选择检测日期")
    private LocalDate inspectDate;
    private String agency;
    private String inspector;
    private String pesticideResidue;
    private String heavyMetal;
    private String otherItems;
    /** PASS / FAIL */
    @NotBlank(message = "请选择检测结论")
    private String result;
    private String conclusion;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long createdBy;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String batchNo;
    @TableField(exist = false)
    private String productName;
}
