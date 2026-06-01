package com.secondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.secondhand.dto.OrderDto;
import com.secondhand.dto.Result;
import com.secondhand.entity.Order;
import com.secondhand.entity.ReturnRequest;
import com.secondhand.mapper.ReturnRequestMapper;
import com.secondhand.service.OrderService;
import com.secondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/order")
public class OrderController {

    @Autowired
    private OrderService orderService;
    @Autowired
    private JwtUtil jwtUtil;
    @Autowired
    private ReturnRequestMapper returnRequestMapper;   // 新增

    // 买家下单
    @PostMapping("/create")
    public Result create(@RequestBody OrderDto dto, HttpServletRequest request) {
        Long buyerId = jwtUtil.getUserIdFromToken(request.getHeader("token"));
        try {
            Order order = orderService.createOrder(buyerId, dto.getProductId(), dto.getQuantity() == null ? 1 : dto.getQuantity());
            return Result.success(order);
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 支付
    @PutMapping("/pay/{orderId}")
    public Result pay(@PathVariable Long orderId, HttpServletRequest request) {
        Long buyerId = jwtUtil.getUserIdFromToken(request.getHeader("token"));
        try {
            orderService.payOrder(orderId, buyerId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 卖家发货
    @PutMapping("/ship/{orderId}")
    public Result ship(@PathVariable Long orderId, HttpServletRequest request) {
        orderService.shipOrder(orderId);
        return Result.success();
    }

    // 买家确认收货
    @PutMapping("/receive/{orderId}")
    public Result receive(@PathVariable Long orderId, HttpServletRequest request) {
        orderService.confirmReceive(orderId);
        return Result.success();
    }

    // 买家申请退货
    @PostMapping("/return/apply")
    public Result applyReturn(@RequestParam Long orderId, @RequestParam String reason, HttpServletRequest request) {
        Long buyerId = jwtUtil.getUserIdFromToken(request.getHeader("token"));
        try {
            orderService.applyReturn(orderId, buyerId, reason);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 卖家同意退货
    @PutMapping("/return/approve/{returnId}")
    public Result approveReturn(@PathVariable Long returnId) {
        try {
            orderService.approveReturn(returnId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 卖家拒绝退货
    @PutMapping("/return/reject/{returnId}")
    public Result rejectReturn(@PathVariable Long returnId) {
        try {
            orderService.rejectReturn(returnId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 卖家确认收到退货，执行退款
    @PutMapping("/return/complete/{returnId}")
    public Result completeReturn(@PathVariable Long returnId) {
        try {
            orderService.completeReturn(returnId);
            return Result.success();
        } catch (RuntimeException e) {
            return Result.error(e.getMessage());
        }
    }

    // 获取订单的退货申请（新增）
    @GetMapping("/return/byOrder/{orderId}")
    public Result getReturnByOrder(@PathVariable Long orderId, HttpServletRequest request) {
        QueryWrapper<ReturnRequest> wrapper = new QueryWrapper<>();
        wrapper.eq("order_id", orderId).orderByDesc("apply_time").last("limit 1");
        ReturnRequest rr = returnRequestMapper.selectOne(wrapper);
        if (rr == null) {
            return Result.error("未找到退货申请");
        }
        return Result.success(rr);
    }

    // 买家/卖家查看自己的订单列表
    @GetMapping("/list")
    public Result list(HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request.getHeader("token"));
        String role = jwtUtil.getRoleFromToken(request.getHeader("token"));
        QueryWrapper<Order> wrapper = new QueryWrapper<>();
        if ("BUYER".equals(role)) {
            wrapper.eq("buyer_id", userId);
        } else if ("SELLER".equals(role) || "ADMIN".equals(role)) {
            wrapper.eq("seller_id", userId);
        }
        wrapper.orderByDesc("create_time");
        List<Order> orders = orderService.list(wrapper);
        return Result.success(orders);
    }
}