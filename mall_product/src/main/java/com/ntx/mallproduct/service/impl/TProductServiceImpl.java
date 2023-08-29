package com.ntx.mallproduct.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TCategory;
import com.ntx.mallcommon.domain.TProduct;
import com.ntx.mallcommon.domain.UserActive;
import com.ntx.mallproduct.DTO.CategoryDTO;
import com.ntx.mallproduct.DTO.CustomException;
import com.ntx.mallproduct.DTO.ProductDTO;
import com.ntx.mallproduct.DTO.UserHolder;
import com.ntx.mallproduct.config.CuratorConfig;
import com.ntx.mallproduct.mapper.TProductMapper;
import com.ntx.mallproduct.service.TCategoryService;
import com.ntx.mallproduct.service.TProductService;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.locks.InterProcessMutex;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import static com.ntx.mallproduct.common.SystemConstant.*;

/**
 * @author NorthnightX
 * @description 针对表【t_product】的数据库操作Service实现
 * @createDate 2023-08-21 21:54:43
 */
@Service
public class TProductServiceImpl extends ServiceImpl<TProductMapper, TProduct>
        implements TProductService {
    @Autowired
    private TCategoryService categoryService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    @Autowired
    private CuratorFramework curatorClient;

    /**
     * 商品列表查询
     *
     * @param pageNum
     * @param pageSize
     * @param productName
     * @param categoryId
     * @return
     */
    @Override
    public Result queryProductPage(Integer pageNum, Integer pageSize, String productName, Integer categoryId) {
        Page<ProductDTO> pageInfo = new Page<>(pageNum, pageSize);
        Query query = new Query();
        long countMongo = mongoTemplate.count(query, ProductDTO.class);
        //如果mongoDB中有数据
        if (countMongo > 0) {
            if (productName != null && productName.length() > 0) {
                query.addCriteria(Criteria.where("name").regex(productName, "i"));
            }
            if (categoryId != null) {
                query.addCriteria(Criteria.where("categoryId").is(categoryId));
            }
            long count = mongoTemplate.count(query, ProductDTO.class);
            query.skip((long) (pageNum - 1) * pageSize).limit(pageSize);
            List<ProductDTO> productDTOS = mongoTemplate.find(query, ProductDTO.class);
            pageInfo.setRecords(productDTOS);
            pageInfo.setTotal(count);
            return Result.success(pageInfo);
        }
        LambdaQueryWrapper<TProduct> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.like(productName != null && productName.length() > 0,
                TProduct::getName, productName);
        queryWrapper.eq(categoryId != null, TProduct::getCategoryId, categoryId);
        int count = this.count(queryWrapper);
        queryWrapper.last("LIMIT " + (pageNum - 1) * pageSize + ", " + pageSize);
        List<TProduct> list = this.list(queryWrapper);
        List<ProductDTO> productDTOList = list.stream().map(tProduct -> {
            ProductDTO productDTO = new ProductDTO();
            BeanUtil.copyProperties(tProduct, productDTO);
            TCategory category = categoryService.getById(productDTO.getCategoryId());
            productDTO.setCategoryName(category.getName());
            //保存到MongoDB
            mongoTemplate.save(productDTO);
            return productDTO;
        }).collect(Collectors.toList());
        pageInfo.setTotal(count);
        pageInfo.setRecords(productDTOList);
        return Result.success(pageInfo);
    }

    /**
     * 修改状态
     *
     * @param product
     * @return
     */
    @Override
    public Result updateProductStatus(TProduct product) throws IOException {
        //牵扯较多
        if (true) {
            return Result.error("权限不够");
        }

        Integer status = product.getStatus();
        Long id = product.getId();
        boolean update = this.update()
                .eq("id", id).set("status", status)
                .set("gmt_modified", LocalDateTime.now()).update();
        Update updateMongo = new Update();
        updateMongo.set("status", status);
        updateMongo.set("gmtModified", LocalDateTime.now());
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(product.getId()));
        mongoTemplate.updateFirst(query, updateMongo, ProductDTO.class);
        UpdateRequest request = new UpdateRequest("product", String.valueOf(product.getId()));
        request.doc("status", status, "gmtModified", LocalDateTime.now());
        client.update(request, RequestOptions.DEFAULT);
        return update ? Result.success("修改成功") : Result.error("网络异常");
    }

    /**
     * 删除 !!!!!!!
     *
     * @param id
     * @return
     */
    @Override
    public Result deleteProduct(Integer id) {
        //暂停使用
        if (true) {
            return Result.error("权限不够");
        }

        boolean update = this.update().
                eq("id", id).set("deleted", DELETED_PRODUCT)
                .set("gmt_modified", LocalDateTime.now()).
                update();
        return update ? Result.success("删除成功") : Result.error("网络异常");
    }

    /**
     * 更新商品
     *
     * @param productDTO
     * @return
     */
    @Override
    public Result updateProduct(ProductDTO productDTO) throws IOException {
        //牵扯较多
        if (true) {
            return Result.error("权限不够");
        }
        TProduct product = new TProduct();
        productDTO.setGmtModified(LocalDateTime.now());
        BeanUtil.copyProperties(productDTO, product);
        String subImages = product.getSubImages();
        List list = JSON.parseObject(subImages, List.class);
        if (list.size() == 0) {
            return Result.error("至少要设置一张商品图片");
        }
        product.setMainImage((String) list.get(0));
        boolean update = this.updateById(product);
        //用户可能会更新分类
        Long categoryId = productDTO.getCategoryId();
        TCategory category = categoryService.getById(categoryId);
        productDTO.setCategoryName(category.getName());
        mongoTemplate.save(productDTO);
        UpdateRequest updateRequest = new UpdateRequest("product", String.valueOf(productDTO.getId()));
        updateRequest.doc(
                "name", productDTO.getName(),
                "categoryName", productDTO.getCategoryName(),
                "subtitle", productDTO.getSubtitle(),
                "mainImage", productDTO.getMainImage(),
                "subImages", productDTO.getSubImages(),
                "detail", productDTO.getDetail(),
                "price", productDTO.getPrice(),
                "stock", productDTO.getStock(),
                "status", productDTO.getStatus(),
                "deleted", productDTO.getDeleted(),
                "gmtModified", productDTO.getGmtModified());
        client.update(updateRequest, RequestOptions.DEFAULT);
        return update ? Result.success("修改成功") : Result.error("网络异常");
    }

    /**
     * 新增商品,有时连接不上ES的话可能会多次添加!!!!
     *
     * @param product
     * @return
     */
    @Override
    public Result addProduct(TProduct product) throws IOException {
        product.setGmtCreate(LocalDateTime.now());
        product.setGmtModified(LocalDateTime.now());
        product.setDeleted(ADD_PRODUCT_DELETED);
        product.setStatus(ADD_PRODUCT_STATUS);
        String productName = product.getName();
        if (productName == null || productName.length() == 0) {
            return Result.error("请输入商品名");
        }
        BigDecimal price = product.getPrice();
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            return Result.error("请设置正常的商品价格");
        }
        Long categoryId = product.getCategoryId();
        if (categoryId == null) {
            return Result.error("请选择分类");
        }
        TCategory category = categoryService.getById(categoryId);
        if (category == null) {
            return Result.error("请选择存在的分类");
        }
        if(category.getParentId() == 0){
            return Result.error("不能选择基分类");
        }
        String subImages = product.getSubImages();
        List list = JSON.parseObject(subImages, List.class);
        if (list.size() == 0) {
            return Result.error("至少要设置一张商品图片");
        }
        product.setMainImage((String) list.get(0));
        boolean save = this.save(product);
        ProductDTO productDTO = new ProductDTO();
        BeanUtil.copyProperties(product, productDTO);
        productDTO.setCategoryName(category.getName());
        mongoTemplate.save(productDTO);
        IndexRequest indexRequest = new IndexRequest("product").id(productDTO.getId().toString());
        indexRequest.source(JSON.toJSONString(productDTO), XContentType.JSON);
        client.index(indexRequest, RequestOptions.DEFAULT);
        return save ? Result.success("新增成功") : Result.error("网络异常");
    }

    /**
     * 根据分类id推荐商品
     *
     * @param id
     * @return
     */
    @Override
    public Result queryProductByCategoryId(Integer id) {
        Query queryCategory = new Query();
        queryCategory.addCriteria(Criteria.where("parentId").is(id));
        List<CategoryDTO> categoryDTOS = mongoTemplate.find(queryCategory, CategoryDTO.class);
        List<Long> collect = categoryDTOS.stream().map(CategoryDTO::getId).collect(Collectors.toList());
        Query query = new Query();
        query.addCriteria(Criteria.where("categoryId").in(collect));
        //根据销量倒序
        query.with(Sort.by(Sort.Direction.DESC, "saleCount")).limit(20);
        List<ProductDTO> productDTOS = mongoTemplate.find(query, ProductDTO.class);
        if (productDTOS.size() > 0) {
            return Result.success(productDTOS);
        }
        //分页？？？
        LambdaQueryWrapper<TProduct> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.eq(TProduct::getCategoryId, id);
        List<TProduct> list = this.list(queryWrapper);
        List<ProductDTO> productDTOList = list.stream().map(tProduct -> {
            ProductDTO productDTO = new ProductDTO();
            BeanUtil.copyProperties(tProduct, productDTO);
            Long categoryId = tProduct.getCategoryId();
            TCategory category = categoryService.getById(categoryId);
            productDTO.setCategoryName(category.getName());
            mongoTemplate.save(productDTO);
            return productDTO;
        }).collect(Collectors.toList());
        return Result.success(productDTOList);
    }

    /**
     * 根据关键字查询
     *
     * @param keyword
     * @return
     */
    @Override
    public Result queryByKeyWord(Integer pageNum, Integer pageSize, String keyword) throws IOException {
        SearchRequest request = new SearchRequest("product");
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        if (keyword.length() == 0) {
            boolQueryBuilder.
                    must(QueryBuilders.termQuery("status", 1)).
                    must(QueryBuilders.termQuery("deleted", 1));
        } else {
            boolQueryBuilder.must(QueryBuilders.matchQuery("text", keyword)).
                    must(QueryBuilders.termQuery("status", 1)).
                    must(QueryBuilders.termQuery("deleted", 1));
        }
        request.source().query(boolQueryBuilder);
        request.source().sort("saleCount", SortOrder.DESC);
        request.source().size(pageSize).from((pageNum - 1) * pageSize);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        TotalHits totalHits = response.getHits().getTotalHits();
        long value = totalHits.value;
        SearchHit[] hits = response.getHits().getHits();
        List<ProductDTO> list = new ArrayList<>();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            ProductDTO productDTO = JSON.parseObject(sourceAsString, ProductDTO.class);
            list.add(productDTO);
        }
        Page<ProductDTO> page = new Page<>(pageNum, pageSize);
        page.setTotal(value);
        page.setRecords(list);
        return Result.success(page);
    }

    /**
     * 默认推荐产品,销量前20的商品
     *
     * @return
     */
    @Override
    public Result queryProductRecommend() {
        Query query = new Query();
        query.limit(20).with(Sort.by(Sort.Direction.DESC, "saleCount"));
        List<ProductDTO> productDTOS = mongoTemplate.find(query, ProductDTO.class);
        if (productDTOS.size() != 0) {
            return Result.success(productDTOS);
        }
        LambdaQueryWrapper<TProduct> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.orderByDesc(TProduct::getSaleCount);
        queryWrapper.last("LIMIT 20");
        List<TProduct> list = this.list(queryWrapper);
        List<ProductDTO> collect = list.stream().map(product -> {
            Long categoryId = product.getCategoryId();
            TCategory category = categoryService.getById(categoryId);
            ProductDTO productDTO = new ProductDTO();
            BeanUtil.copyProperties(product, productDTO);
            productDTO.setCategoryName(category.getName());
            mongoTemplate.save(productDTO);
            return productDTO;
        }).collect(Collectors.toList());
        return Result.success(collect);
    }

    /**
     * 查询指定商品
     *
     * @param id
     * @return
     */
    @Override
    public Result queryProductMessage(Integer id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(id));
        ProductDTO productDTO = mongoTemplate.findOne(query, ProductDTO.class);
        if (productDTO == null || productDTO.getStatus() == 0 || productDTO.getDeleted() == 0) {
            return Result.error("该商品已下架");
        }
        UserActive userActive = new UserActive();
        userActive.setProductId(id);
        userActive.setUserId(Math.toIntExact(UserHolder.getUser().getId()));
        userActive.setCategoryId(productDTO.getCategoryId());
        userActive.setGmtCreate(LocalDateTime.now());
        kafkaTemplate.send("userBehavior", "", JSON.toJSONString(userActive));
        return Result.success(productDTO);
    }

    /**
     * 获取用户购物车的商品信息
     *
     * @param productIdList
     * @return
     */
    @Override
    public List<TProduct> getProductList(List<Long> productIdList) {
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").in(productIdList));
        return mongoTemplate.find(query, ProductDTO.class).stream().map(productDTO -> {
            TProduct product = new TProduct();
            BeanUtil.copyProperties(productDTO, product);
            return product;
        }).collect(Collectors.toList());
    }

    /**
     * 更新库存,增加销量
     *
     * @param updateMap
     * @return
     */
    @Override
    @Transactional(rollbackFor = CustomException.class)
    public Boolean updateProductStock(Map<Long, Integer> updateMap) throws Exception {
        AtomicReference<Boolean> success = new AtomicReference<>(true);
        //使用zookeeper的分布式锁
        InterProcessMutex lock = new InterProcessMutex(curatorClient, "/lock");
        try {
            lock.acquire();
            updateMap.forEach((productId, quantity) -> {
                TProduct product = this.getById(productId);
                int stock = product.getStock();
                int saleCount = product.getSaleCount();
                int newStock = stock - quantity;
                if (stock - quantity >= 0) {
                    boolean update = this.update()
                            .set("stock", newStock)
                            .eq("stock", stock)
                            .eq("sale_count", saleCount)
                            .eq("id", productId)
                            .gt("stock", 0)
                            .update();
                    if (update) {
                        //如果更新成功，更新mongoDB的数据和es中的数据
                        Query query = new Query();
                        query.addCriteria(Criteria.where("_id").is(productId));
                        Update updateMongoD = new Update();
                        updateMongoD.set("stock", newStock).set("saleCount", saleCount);
                        mongoTemplate.updateFirst(query, updateMongoD, ProductDTO.class);
                        //更新ES
                        UpdateRequest updateRequest = new UpdateRequest("product", productId.toString());
                        updateRequest.doc("stock", newStock, "saleCount", saleCount);
                        try {
                            client.update(updateRequest, RequestOptions.DEFAULT);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        success.set(false);
                        throw new CustomException("更新失败");
                    }
                }
                else {
                    success.set(false);
                }
            });
            return success.get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            lock.release();
        }
    }

    @Override
    public Result promotion() throws IOException {
        SearchRequest request = new SearchRequest("product");
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        boolQueryBuilder.
                must(QueryBuilders.termQuery("status", 1)).
                must(QueryBuilders.termQuery("deleted", 1));
        request.source().query(boolQueryBuilder);
        request.source().sort("gmtCreate", SortOrder.DESC);
        request.source().size(4);
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);
        SearchHit[] hits = response.getHits().getHits();
        List<ProductDTO> list = new ArrayList<>();
        for (SearchHit hit : hits) {
            String sourceAsString = hit.getSourceAsString();
            ProductDTO productDTO = JSON.parseObject(sourceAsString, ProductDTO.class);
            list.add(productDTO);
        }
        return Result.success(list);
    }

    /**
     * 查询分类下的所有产品
     * @param id
     * @return
     */
    @Override
    public Result queryAllProductByCategoryId(Integer id) {
        Query query = new Query();
        query.addCriteria(Criteria.where("categoryId").is(id));
        //根据销量倒序
        query.with(Sort.by(Sort.Direction.DESC, "saleCount"));
        List<ProductDTO> productDTOS = mongoTemplate.find(query, ProductDTO.class);
        return Result.success(productDTOS);
    }

//    /**
//     * 查询基分类下的所有商品
//     * @param pageNum
//     * @param pageSize
//     * @param categoryId
//     * @return
//     */
//    @Override
//    public Result queryInitialProduct(Integer pageNum, Integer pageSize, int categoryId) {
//        Page<ProductDTO> page = new Page<>(pageNum, pageSize);
//        Query query = new Query();
//        query.addCriteria(Criteria.where("parentId").is(categoryId));
//        List<CategoryDTO> categoryDTOS = mongoTemplate.find(query, CategoryDTO.class);
//        List<Long> categoryChildIdList = categoryDTOS.stream().map(CategoryDTO::getId).collect(Collectors.toList());
//        Query queryProduct = new Query();
//        queryProduct.addCriteria(Criteria.where("categoryId").in(categoryChildIdList));
//        long count = mongoTemplate.count(queryProduct, ProductDTO.class);
//        page.setTotal(count);
//        queryProduct.limit(pageSize).skip((long) (pageNum - 1) * pageSize);
//        queryProduct.with(Sort.by(Sort.Direction.DESC, "saleCount"));
//        List<ProductDTO> productDTOS = mongoTemplate.find(queryProduct, ProductDTO.class);
//        page.setRecords(productDTOS);
//        return Result.success(page);
//    }
}




