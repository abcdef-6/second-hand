package com.secondhand.controller;

import com.secondhand.dto.*;
import com.secondhand.entity.User;
import com.secondhand.service.UserService;
import com.secondhand.util.JwtUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/user")
public class UserController {

    @Autowired
    private UserService userService;
    @Autowired
    private JwtUtil jwtUtil;

    @PostMapping("/register")
    public Result register(@RequestBody RegisterRequest req) {
        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(req.getPassword());
        user.setNickname(req.getNickname());
        userService.register(user);
        log.info("用户注册");
        return Result.success();
    }

    @PostMapping("/login")
    public Result login(@RequestBody LoginRequest req) {
        User user = userService.login(req.getUsername(), req.getPassword());
        if (user != null) {
            String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
            return Result.success(token);
        }
        return Result.error("用户名或密码错误");
    }

    @GetMapping("/info")
    public Result getUserInfo(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (token == null) return Result.error("未登录");
        Long userId = jwtUtil.getUserIdFromToken(token);
        User user = userService.getById(userId);
        if (user != null) {
            user.setPassword(null);
            return Result.success(user);
        }
        return Result.error("用户不存在");
    }
    @GetMapping("/findByUsername")
    public Result findByUsername(@RequestParam String username) {
        User user = userService.getByUsername(username);
        if (user != null) {
            user.setPassword(null);  // 不返回密码
            return Result.success(user);
        }
        return Result.error("用户不存在");
    }
    // 修改用户名
    @PutMapping("/update/username")
    public Result updateUsername(@RequestParam String newUsername, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request.getHeader("token"));
        if (newUsername == null || newUsername.trim().isEmpty()) {
            return Result.error("用户名不能为空");
        }
        // 检查新用户名是否已存在
        User exist = userService.getByUsername(newUsername);
        if (exist != null && !exist.getId().equals(userId)) {
            return Result.error("用户名已存在");
        }
        userService.updateUsername(userId, newUsername);
        return Result.success();
    }

    // 修改密码
    @PutMapping("/update/password")
    public Result updatePassword(@RequestBody Map<String, String> params, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request.getHeader("token"));
        String oldPassword = params.get("oldPassword");
        String newPassword = params.get("newPassword");
        if (oldPassword == null || newPassword == null || newPassword.length() < 6) {
            return Result.error("密码至少6位");
        }
        boolean success = userService.updatePassword(userId, oldPassword, newPassword);
        if (success) {
            return Result.success();
        } else {
            return Result.error("原密码错误");
        }
    }

    @PostMapping("/recharge")
    public Result recharge(@RequestParam(required = false) Long userId,
                           @RequestParam double amount,
                           HttpServletRequest request) {
        String token = request.getHeader("token");
        Long operatorId = jwtUtil.getUserIdFromToken(token);
        String role = jwtUtil.getRoleFromToken(token);

        // 如果是管理员，且传入了 userId，则为该用户充值；否则为自己充值
        if ("ADMIN".equals(role) && userId != null) {
            userService.recharge(userId, amount);
        } else {
            userService.recharge(operatorId, amount);
        }
        return Result.success();
    }
}