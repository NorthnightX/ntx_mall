package com.ntx.mallproduct.component;

import cn.hutool.core.bean.BeanUtil;
import com.alibaba.fastjson.JSON;
import com.ntx.mallcommon.domain.TCategory;
import com.ntx.mallcommon.domain.TProduct;
import com.ntx.mallproduct.DTO.ProductDTO;
import com.ntx.mallproduct.service.TCategoryService;
import com.ntx.mallproduct.service.TProductService;
import org.elasticsearch.action.bulk.BulkRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
public class ScheduleJobES {
    @Autowired
    private RestHighLevelClient client;
    @Autowired
    private TProductService productService;
    @Autowired
    private MongoTemplate mongoTemplate;
    @Autowired
    private TCategoryService categoryService;

    /**
     * 定时任务，两小时一次,定期向es中写入数据
     */
    @Scheduled(fixedDelay = 1000 * 60 * 60 * 2)
    public void fixedDelayTask() throws IOException {
        //1.创建request
        BulkRequest request = new BulkRequest();
        //2.添加数据
        List<TProduct> list = productService.list();
        //填充数据
        list.forEach((product) -> {
            ProductDTO productDTO = new ProductDTO();
            BeanUtil.copyProperties(product, productDTO);
            TCategory category = categoryService.getById(productDTO.getCategoryId());
            productDTO.setCategoryName(category.getName());
            //更新mongoDB
            mongoTemplate.save(productDTO);
            //index方式会替换掉原本的文档，create如果文档存在会返回错误，update是局部更新
            request.add(new IndexRequest("product").
                    id(String.valueOf(productDTO.getId())).
                    source(JSON.toJSONString(productDTO), XContentType.JSON));

        });
        //3.发送请求
        client.bulk(request, RequestOptions.DEFAULT);
    }

}
