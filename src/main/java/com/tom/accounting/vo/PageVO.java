package com.tom.accounting.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

// 分页返回数据，仅暴露总数和当前页记录
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageVO<T> {

    // 满足条件的总记录数
    private Long total;

    // 当前页记录
    private List<T> records;
}
