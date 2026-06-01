package com.secondhand.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.secondhand.entity.Store;

public interface StoreService extends IService<Store> {
    Store applyStore(Long userId, String storeName, String description);
    void auditStore(Long storeId, String status);
    Store getStoreByUserId(Long userId);
}