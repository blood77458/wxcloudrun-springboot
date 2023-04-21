package com.tencent.wxcloudrun.dao;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.tencent.wxcloudrun.model.UserData;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author cong.liu@mthreads.com
 * @version 1.0
 * @since 1.0
 */
@Mapper
public interface UserDataMapper extends BaseMapper<UserData> {
}
