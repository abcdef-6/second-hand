package com.secondhand.controller;

import com.secondhand.dto.Result;
import com.secondhand.dto.StoreDto;
import com.secondhand.entity.Store;
import com.secondhand.service.StoreService;
import com.secondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/store")
public class StoreController {

    @Autowired
    private StoreService storeService;
    @Autowired
    private JwtUtil jwtUtil;

    // 申请开店
    @PostMapping("/apply")
    public Result apply(@RequestBody StoreDto storeDto, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request.getHeader("token"));
        Store store = storeService.applyStore(userId, storeDto.getStoreName(), storeDto.getDescription());
        return Result.success(store);
    }

    // 查看自己的店铺
    @GetMapping("/my")
    public Result myStore(HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request.getHeader("token"));
        Store store = storeService.getStoreByUserId(userId);
        return Result.success(store);
    }

    // 管理员审核店铺
    @PutMapping("/audit/{storeId}")
    public Result audit(@PathVariable Long storeId, @RequestParam String status, HttpServletRequest request) {
        // 简单校验管理员角色
        String role = jwtUtil.getRoleFromToken(request.getHeader("token"));
        if (!"ADMIN".equals(role)) return Result.error("无权限");
        storeService.auditStore(storeId, status);
        return Result.success();
    }

    // 管理员获取待审核店铺列表
    @GetMapping("/pending")
    public Result pendingList(HttpServletRequest request) {
        // 省略角色验证
        return Result.success(storeService.list(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<Store>().eq("status", "PENDING")
        ));
    }
}