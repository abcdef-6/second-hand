package com.secondhand.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.secondhand.entity.Order;


public interface OrderService extends IService<Order> {
    Order createOrder(Long buyerId, Long productId, Integer quantity);
    void payOrder(Long orderId, Long buyerId);
    void shipOrder(Long orderId);
    void confirmReceive(Long orderId);
    void applyReturn(Long orderId, Long buyerId, String reason);
    void approveReturn(Long returnId);
    void rejectReturn(Long returnId);
    void completeReturn(Long returnId); // 卖家确认收到退货，退款
}