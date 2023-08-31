package com.ntx.mallproduct.DTO;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Document
public class CommentDTO {
    @MongoId
    private Long id;

    private String content;

    private Long userId;
    @Transient
    private String userName;
    @Transient
    private String userImage;

    private Long productId;
    private List<CommentDTO> commentDTOList;
    private Long commentParentId;

    private Integer deleted;

    private LocalDateTime gmtCreate;

    private LocalDateTime gmtModified;
}
