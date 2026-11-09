package com.tom.accounting.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

// 新增、修改账单的请求参数
@Data
public class BillDTO {

    // 分类 id，必填
    @NotNull(message = "分类不能为空")
    private Long categoryId;

    // 金额，必填且必须大于 0，最多两位小数
    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0.01", message = "金额必须大于0")
    @Digits(integer = 8, fraction = 2, message = "金额格式不正确，最多两位小数")
    private BigDecimal amount;

    // 类型，1 支出 2 收入
    @NotNull(message = "账单类型不能为空")
    @Min(value = 1, message = "账单类型只能为1(支出)或2(收入)")
    @Max(value = 2, message = "账单类型只能为1(支出)或2(收入)")
    private Integer type;

    // 账单发生日期，必填
    @NotNull(message = "账单日期不能为空")
    private LocalDate billDate;

    // 备注，可选，最长 200 字符
    @Size(max = 200, message = "备注长度不能超过200个字符")
    private String remark;
}
