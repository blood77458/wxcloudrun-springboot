package com.tencent.wxcloudrun.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.tencent.wxcloudrun.dao.UserMapper;
import com.tencent.wxcloudrun.model.User;
import com.tencent.wxcloudrun.service.UserService;
import org.springframework.stereotype.Service;

/**
 * @author cong.liu@mthreads.com
 * @version 1.0
 * @since 1.0
 */
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {



}
