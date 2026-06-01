package com.secondhand.service;
import com.baomidou.mybatisplus.extension.service.IService;
import com.secondhand.entity.User;

public interface UserService extends IService<User> {
    User login(String username, String password);
    void register(User user);
    User getByUsername(String username);
    void recharge(Long userId, double amount);
    void updateUsername(Long userId, String newUsername);
    boolean updatePassword(Long userId, String oldPassword, String newPassword);
}