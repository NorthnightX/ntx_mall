package com.ntx.mallorder.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ntx.mallcommon.domain.Province;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TShipping;
import com.ntx.mallcommon.domain.TUser;
import com.ntx.mallorder.DTO.ShippingDTO;
import com.ntx.mallorder.DTO.UserHolder;
import com.ntx.mallorder.mapper.TShippingMapper;
import com.ntx.mallorder.service.TShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @author NorthnightX
 * @description 针对表【t_shipping】的数据库操作Service实现
 * @createDate 2023-08-21 21:55:15
 */
@Service
public class TShippingServiceImpl extends ServiceImpl<TShippingMapper, TShipping>
        implements TShippingService {

    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * 新增收货地址
     *
     * @param shipping
     * @return
     */
    @Override
    public Result addShipping(TShipping shipping) {
        try {
            TUser user = UserHolder.getUser();
            //因为前台传过来的省市为编号，需要进一步处理
            Query query = new Query(Criteria.where("userId").is(user.getId()));
            List<ShippingDTO> shippingDTOS = mongoTemplate.find(query, ShippingDTO.class);
            if (shippingDTOS.size() == 4) {
                return Result.error("最多可以设置4个收货地址");
            }
            processAddress(shipping);
            shipping.setUserId(user.getId());
            shipping.setStatus(1);
            shipping.setDeleted(1);
            shipping.setGmtCreate(LocalDateTime.now());
            shipping.setGmtModified(LocalDateTime.now());
            this.save(shipping);
            ShippingDTO shippingDTO = new ShippingDTO();
            BeanUtil.copyProperties(shipping, shippingDTO);
            mongoTemplate.save(shippingDTO);
            return Result.success("添加成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        } finally {
            UserHolder.removeUser();
        }
    }

    /**
     * 更新收货地址
     *
     * @param shipping
     * @return
     */
    @Override
    public Result updateShipping(TShipping shipping) {
        try {
            Long id = shipping.getId();
            TShipping older = this.getById(id);
            if(!shipping.getReceiverProvince().equals(older.getReceiverProvince())){
                //地址变了
                processAddress(shipping);
            }
            shipping.setStatus(older.getStatus());
            shipping.setDeleted(older.getDeleted());
            shipping.setGmtModified(LocalDateTime.now());
            shipping.setGmtCreate(older.getGmtCreate());
            ShippingDTO shippingDTO = new ShippingDTO();
            BeanUtil.copyProperties(shipping, shippingDTO);
            this.updateById(shipping);
            mongoTemplate.save(shippingDTO);
            return Result.success("修改成功");
        } catch (IllegalArgumentException e) {
            return Result.error(e.getMessage());
        }
    }

    /**
     * 填充地址信息
     * @param shipping
     */
    private void processAddress(TShipping shipping) {
        String receiverCity = shipping.getReceiverCity();
        String receiverDistrict = shipping.getReceiverDistrict();
        String receiverProvince = shipping.getReceiverProvince();
        Query queryProvince = new Query(Criteria.where("province").is(receiverProvince)).
                addCriteria(Criteria.where("city").is("0"));
        Province province = mongoTemplate.findOne(queryProvince, Province.class);

        if (province != null) {
            String provinceName = province.getName();
            shipping.setReceiverProvince(provinceName);

            if (provinceName.equals("北京市") || provinceName.equals("上海市")
                    || provinceName.equals("天津市") ||
                    provinceName.equals("重庆市") || provinceName.equals("香港特别行政区") ||
                    provinceName.equals("澳门特别行政区")) {
                shipping.setReceiverCity(provinceName);
                shipping.setReceiverDistrict(provinceName);
            } else {
                Query queryCity = new Query(Criteria.where("province").is(receiverProvince)).
                        addCriteria(Criteria.where("city").is(receiverCity)).
                        addCriteria(Criteria.where("area").is("0"));
                Province city = mongoTemplate.findOne(queryCity, Province.class);

                if (city == null) {
                    throw new IllegalArgumentException("请选择城市");
                }
                shipping.setReceiverCity(city.getName());

                Query queryArea = new Query(Criteria.where("province").is(receiverProvince)).
                        addCriteria(Criteria.where("city").is(receiverCity)).
                        addCriteria(Criteria.where("area").is(receiverDistrict));
                Province area = mongoTemplate.findOne(queryArea, Province.class);

                if (area == null) {
                    throw new IllegalArgumentException("请选择区域");
                }
                shipping.setReceiverDistrict(area.getName());
            }
        } else {
            throw new IllegalArgumentException("无效的省份信息");
        }
    }

    /**
     * 获取用户的收货地址
     *
     * @return
     */
    @Override
    public Result getShipping() {
        Long id = UserHolder.getUser().getId();
        try {
            Query query = new Query();
            query.addCriteria(Criteria.where("userId").is(id));
            List<ShippingDTO> shippingDTOS = mongoTemplate.find(query, ShippingDTO.class);
            return Result.success(shippingDTOS);
        } finally {
            UserHolder.removeUser();
        }
    }

    /**
     * 删除用户地址信息
     *
     * @param id
     * @return
     */
    @Override
    public Result deleteShipping(Integer id) {
        boolean update = this.update().eq("id", id).set("deleted", 0).
                set("gmt_modified", LocalDateTime.now()).update();
        Query query = new Query(Criteria.where("_id").is(id));
        mongoTemplate.remove(query, ShippingDTO.class);
        return Result.success("删除成功");
    }
}




