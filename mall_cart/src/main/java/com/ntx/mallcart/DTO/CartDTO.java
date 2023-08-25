package com.ntx.mallcart.DTO;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.lang.annotation.Documented;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Document
public class CartDTO {
    @MongoId
    private Long id;
    private Long userId;
    private Long productId;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;
    private Integer quantity;
    private Integer checked;

}
