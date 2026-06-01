package com.secondhand.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.secondhand.dto.ProductDto;
import com.secondhand.dto.Result;
import com.secondhand.entity.Product;
import com.secondhand.entity.Store;
import com.secondhand.entity.User;
import com.secondhand.service.ProductService;
import com.secondhand.service.StoreService;
import com.secondhand.service.UserService;
import com.secondhand.util.AliyunOSSOperator;
import com.secondhand.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/product")
public class ProductController {

    @Autowired private UserService userService;

    @Autowired
    private ProductService productService;
    @Autowired
    private StoreService storeService;
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AliyunOSSOperator aliyunOSSOperator;

    @PostMapping("/publish")
    public Result publish(
            @RequestParam("name") String name,
            @RequestParam("price") BigDecimal price,
            @RequestParam("stock") Integer stock,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "image", required = false) MultipartFile image,
            HttpServletRequest request) throws Exception {

        Long userId = jwtUtil.getUserIdFromToken(request.getHeader("token"));
        Store store = storeService.getStoreByUserId(userId);
        if (store == null || !store.getStatus().equals("APPROVED")) {
            return Result.error("店铺未通过审核，无法上架商品");
        }

        Product product = new Product();
        product.setName(name);
        product.setPrice(price);
        product.setStock(stock);
        product.setDescription(description);
        product.setStoreId(store.getId());
        product.setStatus("PENDING");

        if (image != null && !image.isEmpty()) {
            String imageUrl = aliyunOSSOperator.upload(image.getBytes(), image.getOriginalFilename());
            product.setImageUrl(imageUrl);
        }

        productService.save(product);
        return Result.success(product);
    }

    // 买家浏览商品：查看所有ON_SALE的商品（可加条件和分页）
    @GetMapping("/list")
    public Result list(@RequestParam(required = false) String keyword) {
        QueryWrapper<Product> wrapper = new QueryWrapper<>();
        wrapper.eq("status", "ON_SALE")
                .gt("stock", 0);   // 只显示库存大于0的商品
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.like("name", keyword);
        }
        List<Product> products = productService.list(wrapper);

        // 批量查询卖家昵称
        if (products != null && !products.isEmpty()) {
            // 收集所有 storeId
            List<Long> storeIds = products.stream().map(Product::getStoreId).collect(Collectors.toList());
            // 查询店铺信息
            List<Store> stores = storeService.listByIds(storeIds);
            Map<Long, Long> storeUserMap = stores.stream().collect(Collectors.toMap(Store::getId, Store::getUserId));
            // 查询所有卖家用户
            List<Long> userIds = new ArrayList<>(storeUserMap.values());
            List<User> sellers = userService.listByIds(userIds);
            Map<Long, String> userNicknameMap = sellers.stream().collect(Collectors.toMap(User::getId, User::getNickname));

            // 填充 sellerNickname
            for (Product p : products) {
                Long storeId = p.getStoreId();
                Long userId = storeUserMap.get(storeId);
                if (userId != null) {
                    p.setSellerNickname(userNicknameMap.getOrDefault(userId, "未知"));
                } else {
                    p.setSellerNickname("未知");
                }
            }
        }
        return Result.success(products);
    }

    // 商品详情
    @GetMapping("/detail/{id}")
    public Result detail(@PathVariable Long id) {
        Product product = productService.getById(id);
        if (product != null && product.getStoreId() != null) {
            Store store = storeService.getById(product.getStoreId());
            if (store != null) {
                User seller = userService.getById(store.getUserId());
                if (seller != null) {
                    product.setSellerNickname(seller.getNickname());
                }
            }
        }
        return Result.success(product);
    }

    // 卖家下架商品
    @PutMapping("/off/{id}")
    public Result off(@PathVariable Long id, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request.getHeader("token"));
        Product product = productService.getById(id);
        Store store = storeService.getStoreByUserId(userId);
        if (store == null || !product.getStoreId().equals(store.getId())) {
            return Result.error("无权限");
        }
        productService.updateStatus(id, "OFF_SALE");
        return Result.success();
    }

    // 卖家上架商品（从下架恢复）
    @PutMapping("/on/{id}")
    public Result on(@PathVariable Long id, HttpServletRequest request) {
        Long userId = jwtUtil.getUserIdFromToken(request.getHeader("token"));
        Product product = productService.getById(id);
        Store store = storeService.getStoreByUserId(userId);
        if (store == null || !product.getStoreId().equals(store.getId())) {
            return Result.error("无权限");
        }
        productService.updateStatus(id, "ON_SALE");
        return Result.success();
    }

    // 管理员审核商品
    @PutMapping("/audit/{id}")
    public Result audit(@PathVariable Long id, @RequestParam String status, HttpServletRequest request) {
        String role = jwtUtil.getRoleFromToken(request.getHeader("token"));
        if (!"ADMIN".equals(role)) return Result.error("无权限");
        productService.auditProduct(id, status);
        return Result.success();
    }

    // 管理员查看待审核商品
    @GetMapping("/pending")
    public Result pendingList(HttpServletRequest request) {
        return Result.success(productService.list(
                new QueryWrapper<Product>().eq("status", "PENDING")
        ));
    }
}