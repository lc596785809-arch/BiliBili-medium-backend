package com.xypu.response;

import com.xypu.exception.ErrorCodeEnum;

public class ResponseVO<T> {
    private String status;
    private Integer code;
    private String info;
    private T data;

    public static ResponseVO<Void> ok() {
        ResponseVO<Void> vo = new ResponseVO<>();
        vo.setStatus("success");
        vo.setCode(200);
        vo.setInfo("操作成功");
        return vo;
    }

    public static <T> ResponseVO<T> ok(T data) {
        ResponseVO<T> vo = new ResponseVO<>();
        vo.setStatus("success");
        vo.setCode(200);
        vo.setInfo("操作成功");
        vo.setData(data);
        return vo;
    }

    public static ResponseVO<Void> error(ErrorCodeEnum errorCodeEnum) {
        ResponseVO<Void> vo = new ResponseVO<>();
        vo.setStatus("error");
        vo.setCode(errorCodeEnum.getCode());
        vo.setInfo(errorCodeEnum.getMsg());
        return vo;
    }

    public static ResponseVO<Void> error(Integer code, String info) {
        ResponseVO<Void> vo = new ResponseVO<>();
        vo.setStatus("error");
        vo.setCode(code);
        vo.setInfo(info);
        return vo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Integer getCode() {
        return code;
    }

    public void setCode(Integer code) {
        this.code = code;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }
}
