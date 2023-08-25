package com.ntx.mallorder.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.mallcommon.domain.Province;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallorder.mapper.ProvinceMapper;
import com.ntx.mallorder.service.ProvinceService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author NorthnightX
* @description 针对表【province】的数据库操作Service实现
* @createDate 2023-08-24 21:19:26
*/
@Service
public class ProvinceServiceImpl extends ServiceImpl<ProvinceMapper, Province>
    implements ProvinceService{

    @Autowired
    private MongoTemplate mongoTemplate;

    @Override
    public Result getProvinces() {
        Query query = new Query();
        query.addCriteria(Criteria.where("city").is("0"));
        List<Province> provinces = mongoTemplate.find(query, Province.class);
        return Result.success(provinces);
    }

    @Override
    public Result getCities(Integer id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("province").
                is(id.toString())).addCriteria(Criteria.where("area").is("0"))
                .addCriteria(Criteria.where("city").ne("0"));
        List<Province> provinces = mongoTemplate.find(query, Province.class);
        return Result.success(provinces);
    }

    @Override
    public Result getAreas(String cityId, String provinceId) {
        Query query = new Query();
        query.addCriteria(
                Criteria.where("city").is(cityId)).
                addCriteria(Criteria.where("province").is(provinceId)).
                addCriteria(Criteria.where("town").is("0"))
                .addCriteria(Criteria.where("area").ne("0"));
        List<Province> provinces = mongoTemplate.find(query, Province.class);
        return Result.success(provinces);
    }
}




