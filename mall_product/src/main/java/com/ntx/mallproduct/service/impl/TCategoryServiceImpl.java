package com.ntx.mallproduct.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TCategory;
import com.ntx.mallproduct.DTO.CategoryDTO;
import com.ntx.mallproduct.mapper.TCategoryMapper;
import com.ntx.mallproduct.service.TCategoryService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.ntx.mallproduct.common.SystemConstant.*;

/**
* @author NorthnightX
* @description 针对表【t_category】的数据库操作Service实现
* @createDate 2023-08-21 21:54:37
*/
@Service
public class TCategoryServiceImpl extends ServiceImpl<TCategoryMapper, TCategory>
    implements TCategoryService {
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 分类分页查询
     * @param pageNum
     * @param pageSize
     * @param name
     * @param parentId
     * @return
     */
    @Override
    public Result queryCategoryPage(Integer pageNum, Integer pageSize, String name, Integer parentId) {
        Page<CategoryDTO> pageInfo = new Page<>(pageNum, pageSize);
        //先查mongoDB
        Query query = new Query();
        long counted = mongoTemplate.count(query, CategoryDTO.class);
        if(counted > 0){
            if(name != null && name.length() > 0){
                query.addCriteria(Criteria.where("name").regex(name, "i"));
            }
            if(parentId != null){
                query.addCriteria(Criteria.where("parentId").is(parentId));
            }
            query.skip((long) (pageNum - 1) * pageSize).limit(pageSize);
            List<CategoryDTO> categoryDTOS = mongoTemplate.find(query, CategoryDTO.class);
            if(categoryDTOS.size() > 0){
                pageInfo.setRecords(categoryDTOS);
                pageInfo.setTotal(counted);
                return Result.success(pageInfo);
            }
        }
        //mongoDB没有，搜索数据库
        LambdaQueryWrapper<TCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(name != null && name.length() > 0, TCategory::getName, name);
        queryWrapper.eq(parentId != null, TCategory::getParentId, parentId);
        int count = this.count(queryWrapper);
        queryWrapper.last("LIMIT " + (pageNum - 1) *  pageSize+ ", " + pageSize);
        List<TCategory> list = this.list(queryWrapper);
        List<CategoryDTO> categoryDTOList = list.stream().map(tCategory -> {
            CategoryDTO categoryDTO = new CategoryDTO();
            BeanUtil.copyProperties(tCategory, categoryDTO);
            Long parentCategoryId = tCategory.getParentId();
            if (parentCategoryId == 0) {
                categoryDTO.setParentName(categoryDTO.getName());
            }
            else{
                TCategory category = this.getById(parentCategoryId);
                categoryDTO.setParentName(category.getName());
            }
            mongoTemplate.save(categoryDTO);
            return categoryDTO;
        }).collect(Collectors.toList());
        pageInfo.setTotal(count);
        pageInfo.setRecords(categoryDTOList);
        return Result.success(pageInfo);
    }

    /**
     * 查找所有的基础分类
     * @return
     */
    @Override
    public Result queryInitialCategory() {
        //基础分类查询次数较多，查询缓存
        Query query = new Query();
        query.addCriteria(Criteria.where("parentId").is(0));
        List<CategoryDTO> categoryDTOS = mongoTemplate.find(query, CategoryDTO.class);
        if(categoryDTOS.size() > 0){
            return Result.success(categoryDTOS);
        }
        LambdaQueryWrapper<TCategory> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TCategory::getParentId, INITIAL_CATEGORY);
        List<TCategory> list = this.list(queryWrapper);
        List<CategoryDTO> categoryDTOList = list.stream().map(category -> {
            CategoryDTO categoryDTO = new CategoryDTO();
            BeanUtil.copyProperties(category, categoryDTO);
            categoryDTO.setParentName(category.getName());
            mongoTemplate.save(categoryDTO);
            return categoryDTO;
        }).collect(Collectors.toList());
        return Result.success(categoryDTOList);
    }

    /**
     * 修改分类
     * @param categoryDTO
     * @return
     */
    @Override
    public Result updateCategory(CategoryDTO categoryDTO) {
        //先禁用分类的修改
        //暂停使用
        if(true){
            return Result.error("权限不够");
        }

        TCategory category = new TCategory();
        categoryDTO.setGmtModified(LocalDateTime.now());
        BeanUtil.copyProperties(categoryDTO, category);
        boolean update = this.updateById(category);
        Long parentId = categoryDTO.getParentId();
        if(parentId == 0){
            categoryDTO.setParentName(categoryDTO.getName());
        }
        else{
            TCategory categoryParent = this.getById(parentId);
            categoryDTO.setParentName(categoryParent.getName());
        }
        mongoTemplate.save(categoryDTO);
        return update ? Result.success("修改成功") : Result.error("网络异常");
    }

    /**
     * 修改分类状态
     * @param category
     * @return
     */
    @Override
    public Result updateCategoryStatus(TCategory category) {
        //先禁用分类的修改
        //暂停使用
        if(true){
            return Result.error("权限不够");
        }

        Integer status = category.getStatus();
        Long id = category.getId();
        boolean update = this.update()
                .eq("id", id).set("status", status)
                .set("gmt_modified", LocalDateTime.now()).update();
        Update updateMongo = new Update();
        updateMongo.set("status", status);
        updateMongo.set("gmtModified", LocalDateTime.now());
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(category.getId()));
        mongoTemplate.updateFirst(query, updateMongo, CategoryDTO.class);
        return update ? Result.success("修改成功") : Result.error("网络异常");
    }

    /**
     * 删除分类 ！！！！！！！！！！！！！
     * @param id
     * @return
     */
    @Override
    public Result deleteCategory(Integer id) {
        //暂停使用
        if(true){
            return Result.error("权限不够");
        }

        boolean update = this.update().
                eq("id", id).set("deleted", DELETED_CATEGORY)
                .set("gmt_modified", LocalDateTime.now()).
                update();
        return update ? Result.success("删除成功") : Result.error("网络异常");
    }

    /**
     * 新增分类
     * @param category
     * @return
     */
    @Override
    public Result addCategory(TCategory category) {
        Long parentId = category.getParentId();
        TCategory tCategory = this.getById(parentId);
        if(tCategory.getParentId() != 0){
            return Result.error("分类层级过深，请重新考虑层级关系");
        }
        category.setGmtCreate(LocalDateTime.now());
        category.setGmtModified(LocalDateTime.now());
        category.setDeleted(ADD_CATEGORY_DELETED);
        category.setStatus(ADD_CATEGORY_STATUS);
        boolean save = this.save(category);
        //保存到mongodb
        CategoryDTO categoryDTO = new CategoryDTO();
        BeanUtil.copyProperties(category, categoryDTO);
        if(parentId == 0){
            categoryDTO.setParentName(category.getName());
        }
        else{
            TCategory category1 = this.getById(parentId);
            categoryDTO.setParentName(category1.getName());
        }
        mongoTemplate.save(categoryDTO);
        return save ? Result.success("新增成功") : Result.error("网络异常");
    }

    /**
     * 查询全部分类
     * @return
     */
    @Override
    public Result getAllCategory() {
        //先查mongoDB
        Query query = new Query();
        List<CategoryDTO> categoryDTOS = mongoTemplate.find(query, CategoryDTO.class);
        //如果mongoDB有
        if(categoryDTOS.size() > 0){
            return Result.success(categoryDTOS);
        }
        //查询数据库
        List<TCategory> list = this.list();
        List<CategoryDTO> categoryDTOList = list.stream().map(category -> {
            CategoryDTO categoryDTO = new CategoryDTO();
            BeanUtil.copyProperties(category, categoryDTO);
            Long parentId = categoryDTO.getParentId();
            if (parentId == 0) {
                categoryDTO.setParentName(category.getName());
            } else {
                TCategory category1 = this.getById(categoryDTO.getParentId());
                categoryDTO.setParentName(category1.getName());
            }
            mongoTemplate.save(categoryDTO);
            return categoryDTO;
        }).collect(Collectors.toList());
        return Result.success(categoryDTOList);
    }

    @Override
    public Result queryChildCategory(Integer id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("parentId").is(id));
        List<CategoryDTO> categoryDTOS = mongoTemplate.find(query, CategoryDTO.class);
        return Result.success(categoryDTOS);
    }
}




