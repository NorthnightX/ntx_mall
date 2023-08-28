package com.ntx.malluser.pojo.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
@Document
@Data
public class ActiveDTO {
    @MongoId
    private Integer id;

    private Integer userId;

    private Long categoryId;

    private Integer productId;
    private String productName;
    private String productImage;
    private BigDecimal productPrice;
    @DateTimeFormat(pattern="yyyy-MM-dd")
    @JsonFormat(pattern="yyyy-MM-dd", timezone = "GMT+8")
    private LocalDateTime gmtCreate;

}
