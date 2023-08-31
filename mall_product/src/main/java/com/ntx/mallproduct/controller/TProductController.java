package com.ntx.mallproduct.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.api.R;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TCategory;
import com.ntx.mallcommon.domain.TProduct;
import com.ntx.mallproduct.DTO.ProductDTO;
import com.ntx.mallproduct.service.TProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/product")
public class TProductController {

    @Autowired
    private TProductService productService;

    /**
     * 分页查询
     * @param pageNum
     * @param pageSize
     * @param name
     * @param categoryId
     * @return
     */
    @GetMapping("/queryAll")
    public Result getProductPage(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                 @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                 @RequestParam(required = false) String name,
                                 @RequestParam(required = false) Integer categoryId) {
        return productService.queryProductPage(pageNum, pageSize, name, categoryId);
    }

    /**
     * 修改商品状态
     * @param product
     * @return
     */
    @PutMapping("/updateProductStatus")
    public Result updateProductStatus(@RequestBody TProduct product) throws IOException {
        return productService.updateProductStatus(product);
    }

    /**
     * 删除商品
     * @param id
     * @return
     */
    @DeleteMapping("/deleteProduct")
    public Result deleteCategory(@RequestParam Integer id) throws IOException {
        return productService.deleteProduct(id);
    }

    /**
     * 修改商品
     * @return
     */
    @PutMapping("/updateProduct")
    public Result updateProduct(@RequestBody ProductDTO productDTO) throws IOException {
        return productService.updateProduct(productDTO);
    }

    /**
     * 新增商品
     * @param product
     * @return
     */
    @PostMapping("/addProduct")
    public Result addProduct(@RequestBody TProduct product) throws IOException {
        return productService.addProduct(product);
    }

    /**
     * 根据用户的浏览习惯进行推荐商品,暂时不实现
     * @return
     */
    @GetMapping("/guessProductByUser")
    public Result getProductByUser() {
        LambdaQueryWrapper<TProduct> queryWrapper = new LambdaQueryWrapper<>();
        queryWrapper.last("limit 10");
        return Result.success(productService.list(queryWrapper));
    }

    /**
     * 根据产品配别推荐商品
     * @param id
     * @return
     */
    @GetMapping("/queryProductByCategoryId/{id}")
    public Result queryProductByCategoryId(@PathVariable Integer id){
        return productService.queryProductByCategoryId(id);
    }

    /**
     * 关键字查询
     * @param keyword
     * @return
     */
    @GetMapping("/queryProductByKeyword")
    public Result queryProductByKeyword(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                        @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                        @RequestParam String keyword) throws IOException {
        return productService.queryByKeyWord(pageNum, pageSize, keyword);
    }

    /**
     * 默认推荐的产品
     * @return
     */
    @GetMapping("/queryProductRecommend")
    public Result queryProductRecommend(){
        return productService.queryProductRecommend();
    }

    /**
     * 查询商品信息
     * @param id
     * @return
     */
    @GetMapping("/productMessage")
    public Result productMessage(@RequestParam Integer id){
        return productService.queryProductMessage(id);
    }

    /**
     * 调用获取用户购物车的商品信息
     * @return
     */
    @GetMapping("/getCartProduct")
    public List<TProduct> getCartProduct(@RequestParam List<Long> productIdList){
        return productService.getProductList(productIdList);
    }

    /**
     * 更新库存
     * @param updateMap
     * @return
     */
    @PostMapping("/updateProductStock")
    public Boolean updateProductStock(@RequestBody Map<Long, Integer> updateMap) throws Exception {
        return productService.updateProductStock(updateMap);
    }

    /**
     * 主页推广 目前没用
     * @return
     */
    @GetMapping("/promotion")
    public Result promotion() throws IOException {
        return productService.promotion();
    }

    /**
     * 查询分类下的所有
     * @return
     * @throws IOException
     */
    @GetMapping("/queryAllProductByCategoryId/{id}")
    public Result queryAllProductByCategoryId(@PathVariable Integer id){
        return productService.queryAllProductByCategoryId(id);
    }

//    /**
//     * 查询基分类下的所有商品
//     * @return
//     */
//    @GetMapping("/queryInitialProduct")
//    public Result queryInitialProduct(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
//                                      @RequestParam(required = false, defaultValue = "20") Integer pageSize,
//                                      @RequestParam int categoryId){
//        return productService.queryInitialProduct(pageNum, pageSize, categoryId);
//    }


}
