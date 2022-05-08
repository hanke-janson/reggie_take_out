package com.reggie.exception;

/**
 * 自定义业务异常类
 */
public class CustomException extends RuntimeException{
    public CustomException (String msg){
        super(msg);
    }
}
