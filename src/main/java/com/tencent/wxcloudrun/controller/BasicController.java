
package com.tencent.wxcloudrun.controller;

import com.alibaba.fastjson.JSON;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.tencent.wxcloudrun.model.CharValue;
import com.tencent.wxcloudrun.model.User;
import com.tencent.wxcloudrun.model.UserData;
import com.tencent.wxcloudrun.service.UserDataService;
import com.tencent.wxcloudrun.service.UserService;

import com.tencent.wxcloudrun.utils.YingshiUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

//{"xmin":{},"ymin":{},"xmax":{},"ymax":{},"confidence":{},"class":{},"name":{}}
@RestController
public class BasicController {

    @Resource
    private YingshiUtils yingshiUtils;
    @Resource
    UserService userService;
    @Resource
    UserDataService userDataService;

    @Autowired
    RestTemplate restTemplate;


    @PostMapping("/getToken")
    @ResponseBody
    public String getToken() {
        return yingshiUtils.getToken();
    }

    @PostMapping("/save")
    @ResponseBody
    public void save(@RequestBody User user, HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("X-WX-OPENID");
        User one = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getOpenId, header));
        if (one == null) {
            one = user;
            one.setOpenId(header);
            userService.save(one);
        } else {
            one.setDeviceSerial(user.getDeviceSerial());
            one.setValidateCode(user.getValidateCode());
            userService.updateById(one);
        }
        if (StringUtils.isNotBlank(user.getDeviceSerial()) && StringUtils.isNotBlank(user.getValidateCode())) {
            try {
                yingshiUtils.addDevice(user.getDeviceSerial(), user.getValidateCode());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (StringUtils.isNotBlank(user.getDeviceSerial()) && StringUtils.isNotBlank(user.getValidateCode())) {
            try {
                String address = yingshiUtils.getAddress(one.getDeviceSerial(), 3);
                String flvAddress = yingshiUtils.getAddress(one.getDeviceSerial(), 4);
                one.setPath(address);
                one.setFlvPath(flvAddress);
                userService.updateById(one);
                startNewYolo(one.getOpenId(), one.getPath());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void startNewYolo(String openId, String path) {
        ResponseEntity<String> forEntity = restTemplate.getForEntity("http://43.142.14.123:8080/start?openId=" + openId + "&path=" + path, String.class);
        System.out.println(forEntity.getBody());
    }

    @PostMapping("/getUser")
    @ResponseBody
    public User getUser(HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("X-WX-OPENID");
        User one = userService.getOne(new LambdaQueryWrapper<User>().eq(User::getOpenId, header));
        if (StringUtils.isBlank(one.getPath())) {
            String address = yingshiUtils.getAddress(one.getDeviceSerial(), 3);
            String flvAddress = yingshiUtils.getAddress(one.getDeviceSerial(), 4);
            one.setPath(address);
            one.setFlvPath(flvAddress);
            userService.updateById(one);
        }
        return one;
    }

    @PostMapping("/saveData")
    @ResponseBody
    public void saveData(@RequestBody UserData userData) {
        userData.setCreateTime(new Date());
        userDataService.save(userData);
    }

    @PostMapping("/getData")
    @ResponseBody
    public String getData(HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("X-WX-OPENID");
        List<UserData> list = userDataService.list(new LambdaQueryWrapper<UserData>()
                .eq(UserData::getOpenId, header)
                .between(UserData::getCreateTime, getDate(-7), new Date())
                .orderByAsc(UserData::getCreateTime));
        List<Date> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();
        for (int i = 0; i < list.size() - 1; i++) {
            UserData userData = list.get(i);
            UserData userData1 = list.get(i + 1);
            String yoloData = userData.getYoloData();
            String yoloData1 = userData1.getYoloData();
            String replace = yoloData.replace("[", "").replace("]", "");
            String replace1 = yoloData1.replace("[", "").replace("]", "");
            String[] split = replace.split(",");
            String[] split1 = replace1.split(",");
            Double value = getPosition(getD(split[0]), getD(split[1]), getD(split1[0]), getD(split1[1]));
            Date createTime = userData1.getCreateTime();
            xData.add(createTime);
            yData.add(value);
        }
        return JSON.toJSONString(new CharValue(xData, yData));
    }

    @PostMapping("/listData")
    @ResponseBody
    public String listData(HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("X-WX-OPENID");
        List<UserData> list = userDataService.list(new LambdaQueryWrapper<UserData>()
                .eq(UserData::getOpenId, header)
                .between(UserData::getCreateTime, getDate(-7), new Date())
                .orderByAsc(UserData::getCreateTime));
        return JSON.toJSONString(list);
    }

    private static Double getD(String s) {
        return Double.parseDouble(s.trim());
    }

    private static Double getPosition(Double x1, Double y1, Double x2, Double y2) {
        return Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }

    private static Date getDate(int amount) {
        Date date = new Date();//取时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        //把日期往后增加一天.整数往后推,负数往前移动(1:表示明天、-1：表示昨天，0：表示今天)
        calendar.add(Calendar.DATE, amount);
        //这个时间就是日期往后推一天的结果
        return calendar.getTime();
    }
}
