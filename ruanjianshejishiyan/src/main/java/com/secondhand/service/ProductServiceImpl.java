package com.secondhand.service;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.secondhand.entity.Product;
import com.secondhand.mapper.ProductMapper;
import org.springframework.stereotype.Service;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    @Override
    public Product publish(Long storeId, Product product) {
        product.setStoreId(storeId);
        product.setStatus("PENDING");  // 待审核
        baseMapper.insert(product);
        return product;
    }

    @Override
    public void auditProduct(Long productId, String status) {
        Product product = baseMapper.selectById(productId);
        product.setStatus(status); //此处直接用传入的status
        baseMapper.updateById(product);
    }

    @Override
    public void updateStatus(Long productId, String status) {
        Product product = baseMapper.selectById(productId);
        product.setStatus(status);
        baseMapper.updateById(product);
    }
}