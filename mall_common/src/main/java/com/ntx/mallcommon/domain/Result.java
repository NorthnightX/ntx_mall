package com.ntx.mallcommon.domain;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import static com.ntx.mallcommon.commons.SystemContent.*;

/**
 * 结果类
 * @param <T>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Result<T> {
    private Integer code;
    private String message;
    private T data;

    public Result(Integer errorCode, String errorMsg) {
        this.code = errorCode;
        this.message = errorMsg;
    }


    public static<T> Result<T> error(String msg){
        return new Result<>(ERROR_CODE, msg);
    }
    public static<T> Result<T> success(T data){
        return new Result<>(SUCCESS_CODE, SUCCESS_MSG, data);
    }
}
