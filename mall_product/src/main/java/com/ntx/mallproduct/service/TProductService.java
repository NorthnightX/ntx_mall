package com.ntx.mallproduct.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TProduct;
import com.ntx.mallproduct.DTO.ProductDTO;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
* @author NorthnightX
* @description 针对表【t_product】的数据库操作Service
* @createDate 2023-08-21 21:54:43
*/
public interface TProductService extends IService<TProduct> {

    Result queryProductPage(Integer pageNum, Integer pageSize, String name, Integer categoryId);

    Result updateProductStatus(TProduct product) throws IOException;

    Result deleteProduct(Integer id);

    Result updateProduct(ProductDTO productDTO) throws IOException;

    Result addProduct(TProduct product) throws IOException;

    Result queryProductByCategoryId(Integer id);

    Result queryByKeyWord(Integer pageNum, Integer pageSize,String keyword) throws IOException;

    Result queryProductRecommend();

    Result queryProductMessage(Integer id);

    List<TProduct> getProductList(List<Long> productIdList);

    Boolean updateProductStock(Map<Long, Integer> updateMap) throws Exception;

    Result promotion() throws IOException;


    Result queryAllProductByCategoryId(Integer id);

//    Result queryInitialProduct(Integer pageNum, Integer pageSize, int categoryId);
}
