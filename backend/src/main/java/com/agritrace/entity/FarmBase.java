package com.agritrace.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 种植基地 */
@Data
@TableName("farm_base")
public class FarmBase {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotBlank(message = "基地编号不能为空")
    private String code;
    @NotBlank(message = "基地名称不能为空")
    private String name;
    private String address;
    private BigDecimal area;
    private String manager;
    private String phone;
    private String environment;
    private String description;
    private Integer status;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updatedAt;
}
