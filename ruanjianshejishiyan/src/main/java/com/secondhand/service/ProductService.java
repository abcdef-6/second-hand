package com.secondhand.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.secondhand.entity.Product;

public interface ProductService extends IService<Product> {
    Product publish(Long storeId, Product product);
    void auditProduct(Long productId, String status);
    void updateStatus(Long productId, String status);
}