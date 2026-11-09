package com.tom.accounting.controller;

import com.tom.accounting.common.Result;
import com.tom.accounting.dto.BillDTO;
import com.tom.accounting.interceptor.JwtInterceptor;
import com.tom.accounting.service.BillService;
import com.tom.accounting.vo.BillVO;
import com.tom.accounting.vo.PageVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// 账单接口控制器，路径前缀 /api/v1/bills，所有接口均需登录
@RestController
@RequestMapping("/api/v1/bills")
@RequiredArgsConstructor
public class BillController {

    private final BillService billService;

    // 新增账单
    @PostMapping
    public Result<Void> createBill(@RequestAttribute(JwtInterceptor.USER_ID_ATTRIBUTE) Long userId,
                                   @Valid @RequestBody BillDTO billDTO) {
        billService.createBill(userId, billDTO);
        return Result.success();
    }

    // 分页查询当前用户账单，month 格式 yyyy-MM，可选
    @GetMapping
    public Result<PageVO<BillVO>> pageBills(@RequestAttribute(JwtInterceptor.USER_ID_ATTRIBUTE) Long userId,
                                            @RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "10") long size,
                                            @RequestParam(required = false) String month) {
        return Result.success(billService.pageBills(userId, page, size, month));
    }

    // 修改账单
    @PutMapping("/{id}")
    public Result<Void> updateBill(@RequestAttribute(JwtInterceptor.USER_ID_ATTRIBUTE) Long userId,
                                   @PathVariable Long id,
                                   @Valid @RequestBody BillDTO billDTO) {
        billService.updateBill(userId, id, billDTO);
        return Result.success();
    }

    // 删除账单
    @DeleteMapping("/{id}")
    public Result<Void> deleteBill(@RequestAttribute(JwtInterceptor.USER_ID_ATTRIBUTE) Long userId,
                                   @PathVariable Long id) {
        billService.deleteBill(userId, id);
        return Result.success();
    }
}
