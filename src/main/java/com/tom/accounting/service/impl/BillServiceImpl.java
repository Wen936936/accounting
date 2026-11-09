package com.tom.accounting.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tom.accounting.dto.BillDTO;
import com.tom.accounting.entity.Bill;
import com.tom.accounting.exception.BusinessException;
import com.tom.accounting.mapper.BillMapper;
import com.tom.accounting.service.BillService;
import com.tom.accounting.vo.BillVO;
import com.tom.accounting.vo.PageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.util.List;

// 账单业务实现类
@Service
@RequiredArgsConstructor
public class BillServiceImpl implements BillService {

    private static final String NO_PERMISSION_MSG = "无权操作该账单";
    private static final String BILL_NOT_FOUND_MSG = "账单不存在";

    private final BillMapper billMapper;

    @Override
    public void createBill(Long userId, BillDTO billDTO) {
        Bill bill = new Bill();
        bill.setUserId(userId);
        copyToEntity(billDTO, bill);
        LocalDateTime now = LocalDateTime.now();
        bill.setCreatedAt(now);
        bill.setUpdatedAt(now);
        billMapper.insert(bill);
    }

    @Override
    public void updateBill(Long userId, Long id, BillDTO billDTO) {
        Bill bill = getOwnedBill(userId, id);
        copyToEntity(billDTO, bill);
        bill.setUpdatedAt(LocalDateTime.now());
        billMapper.updateById(bill);
    }

    @Override
    public void deleteBill(Long userId, Long id) {
        Bill bill = getOwnedBill(userId, id);
        billMapper.deleteById(bill.getId());
    }

    @Override
    public PageVO<BillVO> pageBills(Long userId, long page, long size, String month) {
        LambdaQueryWrapper<Bill> wrapper = new LambdaQueryWrapper<Bill>()
                .eq(Bill::getUserId, userId)
                .orderByDesc(Bill::getBillDate)
                .orderByDesc(Bill::getId);
        // 按月份过滤，month 格式为 yyyy-MM，为空则查询全部
        if (StringUtils.hasText(month)) {
            YearMonth yearMonth = parseMonth(month);
            wrapper.ge(Bill::getBillDate, yearMonth.atDay(1))
                    .le(Bill::getBillDate, yearMonth.atEndOfMonth());
        }

        Page<Bill> pageParam = new Page<>(page, size);
        Page<Bill> result = billMapper.selectPage(pageParam, wrapper);
        List<BillVO> records = result.getRecords().stream().map(this::toVO).toList();
        return new PageVO<>(result.getTotal(), records);
    }

    // 查询账单并校验数据归属权，不存在或不属于当前用户时阻止操作
    private Bill getOwnedBill(Long userId, Long id) {
        Bill bill = billMapper.selectById(id);
        if (bill == null) {
            throw new BusinessException(BILL_NOT_FOUND_MSG);
        }
        if (!bill.getUserId().equals(userId)) {
            throw new BusinessException(NO_PERMISSION_MSG);
        }
        return bill;
    }

    // 解析月份参数，格式非法时抛出 BusinessException
    private YearMonth parseMonth(String month) {
        try {
            return YearMonth.parse(month);
        } catch (DateTimeParseException e) {
            throw new BusinessException("月份格式不正确，应为 yyyy-MM");
        }
    }

    private void copyToEntity(BillDTO billDTO, Bill bill) {
        bill.setCategoryId(billDTO.getCategoryId());
        bill.setAmount(billDTO.getAmount());
        bill.setType(billDTO.getType());
        bill.setBillDate(billDTO.getBillDate());
        bill.setRemark(billDTO.getRemark());
    }

    private BillVO toVO(Bill bill) {
        BillVO vo = new BillVO();
        vo.setId(bill.getId());
        vo.setCategoryId(bill.getCategoryId());
        vo.setAmount(bill.getAmount());
        vo.setType(bill.getType());
        vo.setBillDate(bill.getBillDate());
        vo.setRemark(bill.getRemark());
        vo.setCreatedAt(bill.getCreatedAt());
        vo.setUpdatedAt(bill.getUpdatedAt());
        return vo;
    }
}
