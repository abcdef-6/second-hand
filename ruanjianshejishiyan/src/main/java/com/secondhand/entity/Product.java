package com.secondhand.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("product")

public class Product {

    @TableField(exist = false)
    private String sellerNickname;
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long storeId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String imageUrl;
    private String status;     // PENDING, ON_SALE, OFF_SALE, REJECTED
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}