package com.agritrace.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/** 物流记录 */
@Data
@TableName("logistics")
public class Logistics {
    @TableId(type = IdType.AUTO)
    private Long id;
    @NotNull(message = "请选择批次")
    private Long batchId;
    /** 为空时后端自动生成 */
    private String waybillNo;
    private String carrier;
    private String vehicleNo;
    private String driver;
    private String driverPhone;
    @NotBlank(message = "起运地不能为空")
    private String fromAddr;
    @NotBlank(message = "目的地不能为空")
    private String toAddr;
    @NotNull(message = "请选择发车时间")
    private LocalDateTime departTime;
    private LocalDateTime arriveTime;
    private String temperature;
    /** IN_TRANSIT / ARRIVED */
    private String status;
    private String remark;
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private Long createdBy;
    @TableField(insertStrategy = FieldStrategy.NEVER, updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private String batchNo;
    @TableField(exist = false)
    private String productName;
}
