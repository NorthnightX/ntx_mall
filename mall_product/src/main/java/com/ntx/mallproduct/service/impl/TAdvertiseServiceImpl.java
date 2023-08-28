package com.ntx.mallproduct.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;

import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TAdvertise;
import com.ntx.mallcommon.domain.TProduct;
import com.ntx.mallproduct.DTO.AdvertiseDTO;
import com.ntx.mallproduct.DTO.ProductDTO;
import com.ntx.mallproduct.mapper.TAdvertiseMapper;
import com.ntx.mallproduct.service.TAdvertiseService;

import com.ntx.mallproduct.service.TProductService;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
* @author NorthnightX
* @description 针对表【t_advertise】的数据库操作Service实现
* @createDate 2023-08-27 22:43:20
*/
@Service
public class TAdvertiseServiceImpl extends ServiceImpl<TAdvertiseMapper, TAdvertise>
    implements TAdvertiseService{

    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TProductService productService;

    /**
     * 获取广告
     * @param pageNum
     * @param pageSize
     * @param status
     * @return
     */
    @Override
    public Result getAdvertise(Integer pageNum, Integer pageSize, Integer status) {
        Page<AdvertiseDTO> page = new Page<>(pageNum, pageSize);
        Query query = new Query();
        if(status != null){
            query.addCriteria(Criteria.where("status").is(status));
        }
        long count = mongoTemplate.count(query, AdvertiseDTO.class);
        page.setTotal(count);
        query.skip((long) (pageNum - 1) * pageSize).limit(pageSize);
        query.with(Sort.by(Sort.Direction.DESC,"gmtCreate"));
        List<AdvertiseDTO> advertiseDTOS =
                mongoTemplate.find(query, AdvertiseDTO.class);
        page.setRecords(advertiseDTOS);
        return Result.success(page);
    }

    @Override
    public Result addAdvertise(TAdvertise tAdvertise) {
        Long productId = tAdvertise.getProductId();
        LambdaQueryWrapper<TAdvertise> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TAdvertise::getProductId, productId);
        TAdvertise isAbsent = this.getOne(queryWrapper);
        if(isAbsent != null){
            return Result.error("该商品已经存在");
        }
        tAdvertise.setGmtCreate(LocalDateTime.now());
        tAdvertise.setStatus(0);
        this.save(tAdvertise);
        AdvertiseDTO advertiseDTO = new AdvertiseDTO();
        BeanUtil.copyProperties(tAdvertise, advertiseDTO);
        Result result = productService.queryProductMessage(Math.toIntExact(tAdvertise.getProductId()));
        ProductDTO data = (ProductDTO)result.getData();
        advertiseDTO.setProductDTO(data);
        mongoTemplate.save(advertiseDTO);
        return Result.success("添加成功");
    }

    /**
     * 修改广告状态
     * @param tAdvertise
     * @return
     */
    @Override
    public Result updateStatus(TAdvertise tAdvertise) {
        Query queryCount = new Query();
        queryCount.addCriteria(Criteria.where("status").is(1));
        long count = mongoTemplate.count(queryCount, AdvertiseDTO.class);
        if(count >= 4){
            return Result.error("最多课同时上架4个广告");
        }
        Integer status = tAdvertise.getStatus();
        Long id = Long.valueOf(tAdvertise.getId());
        boolean update = this.update()
                .eq("id", id).set("status", status).update();
        Update updateMongo = new Update();
        updateMongo.set("status", status);
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(tAdvertise.getId()));
        mongoTemplate.updateFirst(query, updateMongo, AdvertiseDTO.class);
        return update ? Result.success("修改成功") : Result.error("网络异常");
    }

    @Override
    public Result recommend() {
        Query query = new Query();
        query.addCriteria(Criteria.where("status").is(1));
        query.with(Sort.by(Sort.Direction.DESC, "gmtCreate"));
        List<AdvertiseDTO> advertiseDTOS = mongoTemplate.find(query, AdvertiseDTO.class);
        return Result.success(advertiseDTOS);
    }
}




