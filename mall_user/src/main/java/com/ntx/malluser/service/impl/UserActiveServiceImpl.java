package com.ntx.malluser.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.UserActive;
import com.ntx.malluser.pojo.DTO.ActiveDTO;
import com.ntx.malluser.pojo.DTO.UserHolder;
import com.ntx.malluser.mapper.UserActiveMapper;
import com.ntx.malluser.service.UserActiveService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author NorthnightX
* @description 针对表【user_active】的数据库操作Service实现
* @createDate 2023-08-26 09:51:47
*/
@Service
public class UserActiveServiceImpl extends ServiceImpl<UserActiveMapper, UserActive>
    implements UserActiveService {

    @Autowired
    private MongoTemplate mongoTemplate;
    @Override
    public Result history(Integer pageNum, Integer pageSize) {
        try {
            Page<ActiveDTO> page = new Page<>(pageNum, pageSize);
            Long userId = UserHolder.getUser().getId();
            Query query = new Query();
            long count = mongoTemplate.count(query, ActiveDTO.class);
            query.addCriteria(Criteria.where("userId").is(userId));
            query.with(Sort.by(Sort.Direction.DESC, "gmtCreate"));
            query.limit(pageSize).skip((long) (pageNum - 1) * pageSize);
            List<ActiveDTO> activeDTOS = mongoTemplate.find(query, ActiveDTO.class);
            page.setRecords(activeDTOS);
            page.setTotal(count);
            return Result.success(page);
        } finally {
            UserHolder.removeUser();
        }
    }

    @Override
    public Result deleteFoot(int id) {
        this.removeById(id);
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, ActiveDTO.class);
        return Result.success("删除成功");
    }
}




