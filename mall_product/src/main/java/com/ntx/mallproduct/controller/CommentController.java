package com.ntx.mallproduct.controller;

import com.ntx.mallcommon.domain.Result;
import com.ntx.mallcommon.domain.TComment;
import com.ntx.mallproduct.service.TCommentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/comment")
public class CommentController {
    @Autowired
    private TCommentService commentService;

    @PostMapping("/addComment")
    public Result addComment(@RequestBody TComment comment){
        return commentService.addComment(comment);
    }

    @PostMapping("/addFollowComment")
    public Result addFollowComment(@RequestBody TComment comment){
        return commentService.addFollowComment(comment);
    }


    @GetMapping("/queryByProduct")
    public Result queryByProduct(@RequestParam int id){
        return commentService.queryByProduct(id);
    }

    @DeleteMapping("/deleteChildComment")
    public Result deleteChildComment(@RequestParam int id){
        return commentService.deleteChildComment(id);
    }

    @DeleteMapping("/deleteComment")
    public Result deleteComment(@RequestParam int id){
        return commentService.deleteComment(id);
    }
}
