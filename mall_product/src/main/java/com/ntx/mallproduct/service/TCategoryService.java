package com.ntx.mallproduct.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TCategory;
import com.ntx.mallproduct.DTO.CategoryDTO;

/**
* @author NorthnightX
* @description 针对表【t_category】的数据库操作Service
* @createDate 2023-08-21 21:54:37
*/
public interface TCategoryService extends IService<TCategory> {

    Result queryCategoryPage(Integer pageNum, Integer pageSize, String name, Integer parentId);

    Result queryInitialCategory();

    Result updateCategory(CategoryDTO categoryDTO);

    Result updateCategoryStatus(TCategory category);

    Result deleteCategory(Integer id);

    Result addCategory(TCategory category);

    Result getAllCategory();
}
