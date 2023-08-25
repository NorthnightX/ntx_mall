package com.ntx.mallproduct.controller;

import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TCategory;
import com.ntx.mallcommon.domain.TUser;
import com.ntx.mallproduct.DTO.CategoryDTO;
import com.ntx.mallproduct.DTO.UserHolder;
import com.ntx.mallproduct.service.TCategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/category")
public class TCategoryController {
    @Autowired
    private TCategoryService categoryService;

    /**
     * 商品分类分页查询
     * @param pageNum
     * @param pageSize
     * @param name
     * @param parentId
     * @return
     */
    @GetMapping("/queryAll")
    public Result getAllCategory(@RequestParam(required = false, defaultValue = "1") Integer pageNum,
                                 @RequestParam(required = false, defaultValue = "10") Integer pageSize,
                                 @RequestParam(required = false) String name,
                                 @RequestParam(required = false) Integer parentId) {
        return categoryService.queryCategoryPage(pageNum, pageSize, name, parentId);
    }

    /**
     * 搜索基分类
     * @return
     */
    @GetMapping("/queryInitialCategory")
    public Result queryInitialCategory(){
        return categoryService.queryInitialCategory();
    }

    /**
     * 搜索全部分类
     * @return
     */
    @GetMapping("/queryAllCategory")
    public Result queryAllCategory(){
        return categoryService.getAllCategory();
    }
    /**
     * 修改分类
     * @param categoryDTO
     * @return
     */
    @PutMapping("/updateCategory")
    public Result updateCategory(@RequestBody CategoryDTO categoryDTO){
        return categoryService.updateCategory(categoryDTO);
    }

    /**
     * 修改分类状态
     * @param category
     * @return
     */
    @PutMapping("/updateCategoryStatus")
    public Result updateCategoryStatus(@RequestBody TCategory category){
        return categoryService.updateCategoryStatus(category);
    }

    /**
     * 删除分类
     * @param id
     * @return
     */
    @DeleteMapping("/deleteCategory")
    public Result deleteCategory(@RequestParam Integer id){
        return categoryService.deleteCategory(id);
    }

    /**
     * 新增分类
     * @param category
     * @return
     */
    @PostMapping("/addCategory")
    public Result addCategory(@RequestBody TCategory category){
        return categoryService.addCategory(category);
    }
}
