package com.ntx.mallorder.component;

import com.ntx.mallcommon.domain.Province;
import com.ntx.mallorder.service.ProvinceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

//@Component
public class ScheduleJobES {
//
//    @Autowired
//    private MongoTemplate mongoTemplate;
//    @Autowired
//    private ProvinceService provinceService;
//    /**
//     * 定时任务，两小时一次,定期向es中写入数据
//     */
//    @Scheduled(fixedDelay = 1000 * 60 * 60 * 2)
//    public void fixedDelayTask() throws IOException {
//        List<Province> list = provinceService.list();
//        mongoTemplate.insertAll(list);
//    }

}
