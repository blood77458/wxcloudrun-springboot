package com.tencent.wxcloudrun.model;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import lombok.Data;

import java.util.Date;

@Data
public class UserData {

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private String openId;

    private String yoloData;

    private Date createTime;
}
