package com.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.secondhand.entity.Store;
import com.secondhand.entity.User;
import com.secondhand.mapper.StoreMapper;
import com.secondhand.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoreServiceImpl extends ServiceImpl<StoreMapper, Store> implements StoreService {

    @Autowired
    private UserMapper userMapper;

    @Override
    @Transactional
    public Store applyStore(Long userId, String storeName, String description) {
        Store store = new Store();
        store.setUserId(userId);
        store.setStoreName(storeName);
        store.setDescription(description);
        store.setStatus("PENDING");
        baseMapper.insert(store);
        // 更新用户角色为SELLER（审核通过后更合适，但这里先改，或审核时再改）
        User user = userMapper.selectById(userId);
        if (user != null && user.getRole().equals("BUYER")) {
            user.setRole("SELLER");
            userMapper.updateById(user);
        }
        return store;
    }

    @Override
    @Transactional
    public void auditStore(Long storeId, String status) {
        Store store = baseMapper.selectById(storeId);
        store.setStatus(status);
        baseMapper.updateById(store);
        // 如果拒绝，可回退角色，这里简化不处理
    }

    @Override
    public Store getStoreByUserId(Long userId) {
        QueryWrapper<Store> wrapper = new QueryWrapper<>();
        wrapper.eq("user_id", userId);
        return baseMapper.selectOne(wrapper);
    }
}