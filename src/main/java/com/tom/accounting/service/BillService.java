package com.tom.accounting.service;

import com.tom.accounting.dto.BillDTO;
import com.tom.accounting.vo.BillVO;
import com.tom.accounting.vo.PageVO;

// 账单业务接口
public interface BillService {

    // 新增账单，userId 为当前登录用户
    void createBill(Long userId, BillDTO billDTO);

    // 修改账单，账单不属于当前用户时抛出 BusinessException
    void updateBill(Long userId, Long id, BillDTO billDTO);

    // 删除账单，账单不属于当前用户时抛出 BusinessException
    void deleteBill(Long userId, Long id);

    // 分页查询当前用户账单，month 格式 yyyy-MM，为空时查询全部
    PageVO<BillVO> pageBills(Long userId, long page, long size, String month);
}
