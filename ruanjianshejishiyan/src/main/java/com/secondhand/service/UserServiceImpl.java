package com.secondhand.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.secondhand.entity.User;
import com.secondhand.mapper.UserMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    @Override
    @Transactional
    public void updateUsername(Long userId, String newUsername) {
        User user = baseMapper.selectById(userId);
        if (user == null) throw new RuntimeException("用户不存在");
        user.setUsername(newUsername);
        baseMapper.updateById(user);
    }

    @Override
    public boolean updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = baseMapper.selectById(userId);
        if (user == null) return false;
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        if (!encoder.matches(oldPassword, user.getPassword())) {
            return false;
        }
        user.setPassword(encoder.encode(newPassword));
        baseMapper.updateById(user);
        return true;
    }
    @Override
    public User login(String username, String password) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        User user = baseMapper.selectOne(wrapper);
        if (user != null && encoder.matches(password, user.getPassword())) {
            return user;
        }
        return null;
    }

    @Override
    @Transactional
    public void register(User user) {
        user.setPassword(encoder.encode(user.getPassword()));
        user.setRole("BUYER");
        user.setBalance(BigDecimal.ZERO);
        user.setStatus(1);
        baseMapper.insert(user);
    }

    @Override
    public User getByUsername(String username) {
        QueryWrapper<User> wrapper = new QueryWrapper<>();
        wrapper.eq("username", username);
        return baseMapper.selectOne(wrapper);
    }

    @Override
    @Transactional
    public void recharge(Long userId, double amount) {
        User user = baseMapper.selectById(userId);
        if (user != null) {
            user.setBalance(user.getBalance().add(BigDecimal.valueOf(amount)));
            baseMapper.updateById(user);
        }
    }
}