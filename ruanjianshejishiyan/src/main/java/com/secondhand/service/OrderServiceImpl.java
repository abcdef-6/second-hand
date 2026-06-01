package com.secondhand.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.secondhand.entity.*;
import com.secondhand.mapper.*;
import com.secondhand.util.OrderNoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class OrderServiceImpl extends ServiceImpl<OrderMapper, Order> implements OrderService {

    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private StoreMapper storeMapper;
    @Autowired
    private ReturnRequestMapper returnRequestMapper;

    @Override
    @Transactional
    public Order createOrder(Long buyerId, Long productId, Integer quantity) {
        Product product = productMapper.selectById(productId);
        if (product == null || !product.getStatus().equals("ON_SALE")) {
            throw new RuntimeException("商品不可购买");
        }
        if (product.getStock() < quantity) {
            throw new RuntimeException("库存不足");
        }

        // 买家信息
        User buyer = userMapper.selectById(buyerId);
        if (buyer == null) throw new RuntimeException("买家不存在");

        BigDecimal totalAmount = product.getPrice().multiply(BigDecimal.valueOf(quantity));
        // 检查余额
        if (buyer.getBalance().compareTo(totalAmount) < 0) {
            throw new RuntimeException("余额不足，请充值");
        }

        // 获取卖家ID (通过storeId)
        Store store = storeMapper.selectById(product.getStoreId());
        if (store == null) throw new RuntimeException("商店不存在");

        // 扣款
        buyer.setBalance(buyer.getBalance().subtract(totalAmount));
        userMapper.updateById(buyer);

        // 减库存
        product.setStock(product.getStock() - quantity);
        productMapper.updateById(product);

        // 生成订单
        Order order = new Order();
        order.setOrderNo(OrderNoUtil.generate());
        order.setBuyerId(buyerId);
        order.setSellerId(store.getUserId());
        order.setProductId(productId);
        order.setProductName(product.getName());
        order.setQuantity(quantity);
        order.setUnitPrice(product.getPrice());
        order.setTotalAmount(totalAmount);
        order.setStatus("PAID");    // 立即支付成功
        order.setPayTime(LocalDateTime.now());
        baseMapper.insert(order);

        return order;
    }

    // 支付（如果订单状态为UNPAID）
    @Override
    @Transactional
    public void payOrder(Long orderId, Long buyerId) {
        Order order = baseMapper.selectById(orderId);
        if (!order.getStatus().equals("UNPAID")) {
            throw new RuntimeException("订单状态不正确");
        }
        User buyer = userMapper.selectById(buyerId);
        if (buyer.getBalance().compareTo(order.getTotalAmount()) < 0) {
            throw new RuntimeException("余额不足");
        }
        buyer.setBalance(buyer.getBalance().subtract(order.getTotalAmount()));
        userMapper.updateById(buyer);
        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        baseMapper.updateById(order);
    }

    @Override
    @Transactional
    public void shipOrder(Long orderId) {
        Order order = baseMapper.selectById(orderId);
        if (!order.getStatus().equals("PAID")) {
            throw new RuntimeException("订单未支付，不能发货");
        }
        order.setStatus("SHIPPED");
        order.setShipTime(LocalDateTime.now());
        baseMapper.updateById(order);
    }

    @Override
    @Transactional
    public void confirmReceive(Long orderId) {
        Order order = baseMapper.selectById(orderId);
        if (!order.getStatus().equals("SHIPPED")) {
            throw new RuntimeException("订单未发货，不能确认收货");
        }
        // 将钱转给卖家
        User seller = userMapper.selectById(order.getSellerId());
        seller.setBalance(seller.getBalance().add(order.getTotalAmount()));
        userMapper.updateById(seller);

        order.setStatus("COMPLETED");
        order.setReceiveTime(LocalDateTime.now());
        baseMapper.updateById(order);
    }

    @Override
    @Transactional
    public void applyReturn(Long orderId, Long buyerId, String reason) {
        Order order = baseMapper.selectById(orderId);
        if (!order.getStatus().equals("SHIPPED") && !order.getStatus().equals("COMPLETED")) {
            throw new RuntimeException("当前订单状态不可退货");
        }
        // 创建退货申请
        ReturnRequest rr = new ReturnRequest();
        rr.setOrderId(orderId);
        rr.setBuyerId(buyerId);
        rr.setReason(reason);
        rr.setStatus("APPLYING");
        returnRequestMapper.insert(rr);
        // 更新订单状态
        order.setStatus("RETURNING");
        baseMapper.updateById(order);
    }

    @Override
    @Transactional
    public void approveReturn(Long returnId) {
        ReturnRequest rr = returnRequestMapper.selectById(returnId);
        if (!rr.getStatus().equals("APPLYING")) throw new RuntimeException("申请状态不正确");
        rr.setStatus("APPROVED");
        rr.setDealTime(LocalDateTime.now());
        returnRequestMapper.updateById(rr);
        // 更新订单状态
        Order order = baseMapper.selectById(rr.getOrderId());
        order.setStatus("RETURNED");  // 等待卖家确认退货
        baseMapper.updateById(order);
    }

    @Override
    @Transactional
    public void rejectReturn(Long returnId) {
        ReturnRequest rr = returnRequestMapper.selectById(returnId);
        if (!rr.getStatus().equals("APPLYING")) throw new RuntimeException("申请状态不正确");
        rr.setStatus("REJECTED");
        rr.setDealTime(LocalDateTime.now());
        returnRequestMapper.updateById(rr);
        // 订单状态恢复为之前状态？简单处理：恢复为COMPLETED或SHIPPED，根据订单记录恢复
        Order order = baseMapper.selectById(rr.getOrderId());
        // 假设退回之前的正常状态，简单设为COMPLETED
        order.setStatus("COMPLETED");
        baseMapper.updateById(order);
    }

    @Override
    @Transactional
    public void completeReturn(Long returnId) {
        ReturnRequest rr = returnRequestMapper.selectById(returnId);
        if (!rr.getStatus().equals("APPROVED")) throw new RuntimeException("退货未同意");
        // 退款：从卖家扣回给买家
        Order order = baseMapper.selectById(rr.getOrderId());
        User seller = userMapper.selectById(order.getSellerId());
        User buyer = userMapper.selectById(order.getBuyerId());
        if (seller.getBalance().compareTo(order.getTotalAmount()) < 0) {
            throw new RuntimeException("卖家余额不足，无法退款");
        }
        seller.setBalance(seller.getBalance().subtract(order.getTotalAmount()));
        buyer.setBalance(buyer.getBalance().add(order.getTotalAmount()));
        userMapper.updateById(seller);
        userMapper.updateById(buyer);
        rr.setStatus("REFUNDED");
        rr.setDealTime(LocalDateTime.now());
        returnRequestMapper.updateById(rr);
        order.setStatus("REFUNDED");
        baseMapper.updateById(order);
    }
}