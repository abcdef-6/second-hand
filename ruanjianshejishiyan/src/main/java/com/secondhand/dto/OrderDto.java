package com.secondhand.dto;
import lombok.Data;

@Data
public class OrderDto {
    private Long productId;
    private Integer quantity;
}