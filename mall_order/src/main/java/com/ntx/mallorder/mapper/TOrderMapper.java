package com.ntx.mallorder.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.ntx.mallcommon.domain.TOrder;
import com.ntx.mallorder.DTO.RateDTO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
* @author NorthnightX
* @description 针对表【t_order】的数据库操作Mapper
* @createDate 2023-08-21 21:55:07
* @Entity generator.domain.TOrder
*/
@Mapper
public interface TOrderMapper extends BaseMapper<TOrder> {
    List<RateDTO> getPayMethodRate();

    List<RateDTO> orderStatusRate();
}




