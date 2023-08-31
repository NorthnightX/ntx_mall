package com.ntx.mallproduct.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TComment;

/**
* @author NorthnightX
* @description 针对表【t_comment】的数据库操作Service
* @createDate 2023-08-21 21:54:40
*/
public interface TCommentService extends IService<TComment> {

    Result addComment(TComment comment);

    Result queryByProduct(int id);

    Result addFollowComment(TComment comment);

    Result deleteChildComment(int id);

    Result deleteComment(int id);
}
