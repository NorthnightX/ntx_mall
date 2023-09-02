package com.ntx.mallproduct.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TCategory;
import com.ntx.mallcommon.domain.TProduct;
import com.ntx.mallproduct.DTO.CategoryDTO;
import com.ntx.mallproduct.DTO.ProductDTO;
import com.ntx.mallproduct.mapper.TCategoryMapper;
import com.ntx.mallproduct.service.TCategoryService;

import com.ntx.mallproduct.service.TProductService;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.reindex.UpdateByQueryRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
    @Autowired
    private TProductService productService;
    @Autowired
    private RestHighLevelClient client;

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
            long count = mongoTemplate.count(query, CategoryDTO.class);
            query.skip((long) (pageNum - 1) * pageSize).limit(pageSize);
            List<CategoryDTO> categoryDTOS = mongoTemplate.find(query, CategoryDTO.class);
            if(categoryDTOS.size() > 0){
                pageInfo.setRecords(categoryDTOS);
                pageInfo.setTotal(count);
                return Result.success(pageInfo);
            }
        }
        return Result.success(new ArrayList<>());
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
        return Result.success(categoryDTOS);
    }

    /**
     * 修改分类
     * @param categoryDTO
     * @return
     */
    @Override
    public Result updateCategory(CategoryDTO categoryDTO) throws IOException {
        if(categoryDTO.getStatus() == 0){
            Long id = categoryDTO.getId();
            Query queryCategory = new Query();
            queryCategory.addCriteria(Criteria.where("parentId").is(id));
            queryCategory.addCriteria(Criteria.where("status").is(1));
            queryCategory.addCriteria(Criteria.where("deleted").is(1));
            CategoryDTO one = mongoTemplate.findOne(queryCategory, CategoryDTO.class);
            if(one != null){
                return Result.error("该分类下还有分类未处理");
            }
            Query queryProduct = new Query();
            queryProduct.addCriteria(Criteria.where("categoryId").is(id));
            queryProduct.addCriteria(Criteria.where("status").is(1));
            queryProduct.addCriteria(Criteria.where("deleted").is(1));
            ProductDTO productDTO = mongoTemplate.findOne(queryProduct, ProductDTO.class);
            if(productDTO != null){
                return Result.error("该分类下还有产品未处理");
            }
        }
        //基分类处理
        TCategory tCategory = this.getById(categoryDTO.getId());
        if(tCategory.getParentId() == 0){
            Long parentId = categoryDTO.getParentId();
            //如果改动了基分类的分类
            if(parentId != 0){
                //搜索该基分类下有没有分类
                Query queryCategory = new Query();
                queryCategory.addCriteria(Criteria.where("parentId").is(tCategory.getId()));
                queryCategory.addCriteria(Criteria.where("status").is(1));
                queryCategory.addCriteria(Criteria.where("deleted").is(1));
                CategoryDTO one = mongoTemplate.findOne(queryCategory, CategoryDTO.class);
                if(one != null){
                    return Result.error("该基分类下还有分类没有处理");
                }
            }
        }
        //如果要将该分类设为基分类
        if(categoryDTO.getParentId() == 0){
            Query queryProduct= new Query();
            queryProduct.addCriteria(Criteria.where("categoryId").is(tCategory.getId()));
            queryProduct.addCriteria(Criteria.where("status").is(1));
            queryProduct.addCriteria(Criteria.where("deleted").is(1));
            ProductDTO one = mongoTemplate.findOne(queryProduct, ProductDTO.class);
            if(one != null){
                return Result.error("该分类下还有商品，不能设置为基分类");
            }
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
        //修改mongoDB的商品信息
        Query query = new Query();
        query.addCriteria(Criteria.where("categoryId").is(categoryDTO.getId()));
        List<ProductDTO> productDTOS = mongoTemplate.find(query, ProductDTO.class);
        Update updateProductCategoryName = new Update();
        updateProductCategoryName.set("categoryName", category.getName());
        mongoTemplate.updateMulti(query, updateProductCategoryName, ProductDTO.class);
        //修改ES的商品
        List<Long> collect = productDTOS.stream().map(ProductDTO::getId).collect(Collectors.toList());
        collect.forEach(id -> {
            UpdateRequest updateRequest = new UpdateRequest("product", String.valueOf(id));
            updateRequest.doc("categoryName", category.getName());
            try {
                client.update(updateRequest, RequestOptions.DEFAULT);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return update ? Result.success("修改成功") : Result.error("网络异常");
    }

    /**
     * 修改分类状态
     * @param category
     * @return
     */
    @Override
    @Transactional
    public Result updateCategoryStatus(TCategory category) {
        //如果该分类下还有其他分类
        Long id = category.getId();
        Query queryCategory = new Query();
        queryCategory.addCriteria(Criteria.where("parentId").is(id));
        queryCategory.addCriteria(Criteria.where("status").is(1));
        queryCategory.addCriteria(Criteria.where("deleted").is(1));
        CategoryDTO one = mongoTemplate.findOne(queryCategory, CategoryDTO.class);
        if(one != null){
            return Result.error("该分类下还有分类未处理");
        }
        Query queryProduct = new Query();
        queryProduct.addCriteria(Criteria.where("categoryId").is(id));
        queryProduct.addCriteria(Criteria.where("status").is(1));
        queryProduct.addCriteria(Criteria.where("deleted").is(1));
        ProductDTO productDTO = mongoTemplate.findOne(queryProduct, ProductDTO.class);
        if(productDTO != null){
            return Result.error("该分类下还有产品未处理");
        }
        Integer status = category.getStatus();
        this.update()
                .eq("id", id).set("status", status)
                .set("gmt_modified", LocalDateTime.now()).update();
        Update updateMongo = new Update();
        updateMongo.set("status", status);
        updateMongo.set("gmtModified", LocalDateTime.now());
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(category.getId()));
        mongoTemplate.updateFirst(query, updateMongo, CategoryDTO.class);
        return Result.success("修改成功");
    }

    /**
     * 删除分类
     * @param id
     * @return
     */
    @Override
    @Transactional
    public Result deleteCategory(Integer id) {
        LambdaQueryWrapper<TProduct> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TProduct::getCategoryId, id);
        queryWrapper.eq(TProduct::getDeleted, 1);
        queryWrapper.last("LIMIT 1");
        TProduct isAbsent = productService.getOne(queryWrapper);
        if(isAbsent != null){
            return Result.error("该分类下还有商品");
        }
        LambdaQueryWrapper<TCategory> queryWrapperCategory = new LambdaQueryWrapper<>();
        queryWrapperCategory.eq(TCategory::getParentId, id);
        queryWrapperCategory.eq(TCategory::getDeleted, 1);
        queryWrapperCategory.last("LIMIT 1");
        TCategory category = this.getOne(queryWrapperCategory);
        if(category != null){
            return Result.error("该分类还有子分类");
        }
        this.update().
                eq("id", id).set("deleted", DELETED_CATEGORY)
                .set("gmt_modified", LocalDateTime.now()).
                update();
        //删除MongoDB的分类
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, CategoryDTO.class);
        return Result.success("删除成功");
    }

    /**
     * 新增分类
     * @param category
     * @return
     */
    @Override
    @Transactional
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
        this.save(category);
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
        return Result.success("新增成功");
    }

    /**
     * 查询全部分类
     * @return
     */
    @Override
    public Result getAllCategory() {
        //先查mongoDB
        Query query = new Query();
        query.addCriteria(Criteria.where("parentId").ne(0));
        query.addCriteria(Criteria.where("status").is(1));
        List<CategoryDTO> categoryDTOS = mongoTemplate.find(query, CategoryDTO.class);
        return Result.success(categoryDTOS);

    }

    /**
     * 查询分类下的子分类
     * @param id
     * @return
     */
    @Override
    public Result queryChildCategory(Integer id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("parentId").is(id));
        List<CategoryDTO> categoryDTOS = mongoTemplate.find(query, CategoryDTO.class);
        return Result.success(categoryDTOS);
    }
}




