package com.secondhand.entity;
import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("return_request")
public class ReturnRequest {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long buyerId;
    private String reason;
    private String status;    // APPLYING, APPROVED, REJECTED, RETURNED, REFUNDED
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime applyTime;
    private LocalDateTime dealTime;
}