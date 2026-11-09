package com.tom.accounting.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 账单实体，对应数据库表 bill
@Data
@TableName("bill")
public class Bill {

    // 主键，自增 BIGINT
    @TableId(type = IdType.AUTO)
    private Long id;

    // 账单归属用户 id，用于数据归属权校验
    private Long userId;

    // 分类 id
    private Long categoryId;

    // 金额，必须大于 0
    private BigDecimal amount;

    // 类型，1 支出 2 收入
    private Integer type;

    // 账单发生日期
    private LocalDate billDate;

    // 备注
    private String remark;

    // 创建时间
    private LocalDateTime createdAt;

    // 更新时间
    private LocalDateTime updatedAt;
}
