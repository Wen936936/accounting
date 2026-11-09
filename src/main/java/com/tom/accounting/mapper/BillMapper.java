package com.tom.accounting.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tom.accounting.entity.Bill;
import org.apache.ibatis.annotations.Mapper;

// 账单表数据访问接口，继承 BaseMapper 获得基础 CRUD 方法
@Mapper
public interface BillMapper extends BaseMapper<Bill> {
}
