package com.tom.accounting.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

// 账单返回数据
@Data
public class BillVO {

    // 账单 id
    private Long id;

    // 分类 id
    private Long categoryId;

    // 金额
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
