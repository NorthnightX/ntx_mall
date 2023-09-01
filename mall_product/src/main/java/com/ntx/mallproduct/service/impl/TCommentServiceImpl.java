package com.ntx.mallproduct.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TComment;
import com.ntx.mallcommon.domain.TUser;
import com.ntx.mallcommon.feign.OrderClient;
import com.ntx.mallcommon.feign.UserClient;
import com.ntx.mallproduct.DTO.CommentDTO;
import com.ntx.mallproduct.DTO.UserHolder;
import com.ntx.mallproduct.mapper.TCommentMapper;
import com.ntx.mallproduct.service.TCommentService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
* @author NorthnightX
* @description 针对表【t_comment】的数据库操作Service实现
* @createDate 2023-08-21 21:54:40
*/
@Service
public class TCommentServiceImpl extends ServiceImpl<TCommentMapper, TComment>
    implements TCommentService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private UserClient userClient;
    @Autowired
    private OrderClient orderClient;

    @Override
    public Result addComment(TComment comment) {
        TUser user = UserHolder.getUser();
        Boolean buyProduct = orderClient.isBuyProduct(Math.toIntExact(user.getId()), Math.toIntExact(comment.getProductId()));
        if(!buyProduct){
            return Result.error("你还没有购买该商品");
        }
        //主评论人需要购买商品
        try {
            comment.setUserId(user.getId());
            comment.setCommentParentId(0L);
            comment.setGmtCreate(LocalDateTime.now());
            comment.setGmtModified(LocalDateTime.now());
            comment.setDeleted(1);
            this.save(comment);
            CommentDTO commentDTO = new CommentDTO();
            BeanUtil.copyProperties(comment, commentDTO);
            mongoTemplate.save(commentDTO);
            return Result.success("添加成功");
        } finally {
            UserHolder.removeUser();
        }
    }

    @Override
    public Result queryByProduct(int id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("productId").is(id));
        List<CommentDTO> commentDTOS = mongoTemplate.find(query, CommentDTO.class);
        for (CommentDTO commentDTO : commentDTOS) {
            List<CommentDTO> commentDTOList = commentDTO.getCommentDTOList();
            if(commentDTOList != null){
                for (CommentDTO dto : commentDTOList) {
                    Long userId = dto.getUserId();
                    TUser user = userClient.getUser(Math.toIntExact(userId));
                    dto.setUserImage(user.getAvatar());
                    dto.setUserName(user.getNickName());
                }
            }
            Long userId = commentDTO.getUserId();
            TUser user = userClient.getUser(Math.toIntExact(userId));
            commentDTO.setUserName(user.getNickName());
            commentDTO.setUserImage(user.getAvatar());
        }
        return Result.success(commentDTOS);
    }

    @Override
    public Result addFollowComment(TComment comment) {
        Long commentParentId = comment.getCommentParentId();
        TComment parent = this.getById(commentParentId);
        if(parent.getCommentParentId() != 0){
            return Result.error("不能多级评论");
        }
        try {
            TUser user = UserHolder.getUser();
            //填充评论数据
            comment.setUserId(user.getId());
            comment.setGmtCreate(LocalDateTime.now());
            comment.setGmtModified(LocalDateTime.now());
            comment.setDeleted(1);
            //保存评论
            this.save(comment);
            //查询父评论
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(comment.getCommentParentId()));
            CommentDTO parentDTO = mongoTemplate.findOne(query, CommentDTO.class);
            //如果存在
            if (parentDTO != null) {
                CommentDTO childDTO = new CommentDTO();
                BeanUtil.copyProperties(comment, childDTO);
                List<CommentDTO> commentDTOList;
                commentDTOList = parentDTO.getCommentDTOList();
                if(commentDTOList == null){
                    //如果评论列表为空
                    commentDTOList= new ArrayList<>();
                }
                commentDTOList.add(childDTO);
                Update update = new Update();
                update.set("commentDTOList", commentDTOList);
                mongoTemplate.updateFirst(query, update, CommentDTO.class);
                return Result.success("添加成功");
            }
            return Result.error("该评论已删除");
        }
        finally {
            UserHolder.removeUser();
        }
    }

    @Override
    public Result deleteChildComment(int id) {
        Long userId = UserHolder.getUser().getId();
        try {
            TComment tComment = this.getById(id);
            TComment commentParent = this.getById(tComment.getCommentParentId());
            if(!Objects.equals(tComment.getUserId(), userId) || !Objects.equals(commentParent.getUserId(), userId)){
                return Result.error("权限不足");
            }
            //删除子评论
            this.update().eq("id", id).set("deleted", 0).set("gmt_modified", LocalDateTime.now()).update();
            TComment comment = this.getById(id);
            Long commentParentId = comment.getCommentParentId();
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(commentParentId));
            CommentDTO commentDTO = mongoTemplate.findOne(query, CommentDTO.class);
            List<CommentDTO> commentDTOList = null;
            if (commentDTO != null) {
                commentDTOList = commentDTO.getCommentDTOList();
                commentDTOList.removeIf(dto -> dto.getId() == id);
                Update update = new Update();
                update.set("commentDTOList", commentDTOList);
                mongoTemplate.updateFirst(query, update, CommentDTO.class);
                return Result.success("删除成功");
            }
            return Result.error("评论不存在");
        } finally {
            UserHolder.removeUser();
        }
    }

    @Override
    public Result deleteComment(int id) {
        Long userId = UserHolder.getUser().getId();
        try {
            TComment tComment = this.getById(id);
            if(!Objects.equals(tComment.getUserId(), userId)){
                return Result.error("权限不足");
            }
            //删除父评论
            this.update().eq("id", id).set("deleted", 0).set("gmt_modified", LocalDateTime.now()).update();
            //删除子评论
            this.update().eq("comment_parent_id", id).set("deleted", 0).set("gmt_modified", LocalDateTime.now()).update();
            Query query = new Query();
            query.addCriteria(Criteria.where("_id").is(id));
            mongoTemplate.remove(query, CommentDTO.class);
            return Result.success("删除成功");
        } finally {
            UserHolder.removeUser();
        }
    }
}




