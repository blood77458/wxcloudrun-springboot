
package com.tencent.wxcloudrun.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import javax.annotation.Resource;

import com.tencent.wxcloudrun.model.CharValue;
import com.tencent.wxcloudrun.model.User;
import com.tencent.wxcloudrun.model.UserData;
import com.tencent.wxcloudrun.service.UserDataService;
import com.tencent.wxcloudrun.service.UserService;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;


@RestController
public class BasicController {

    @Resource
    UserService userService;
    @Resource
    UserDataService userDataService;

    @Value("${wechat.appId}")
    private String appId;

    @Value("${wechat.appSecret}")
    private String appSecret;

    @RequestMapping("/login")
    public Object userLogin(@RequestParam String code) throws IOException {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("text/plain");
        okhttp3.RequestBody body = okhttp3.RequestBody.create(mediaType, "");
        Request request = new Request.Builder()
                .url("https://api.weixin.qq.com/sns/jscode2session?appid=" + appId + "&secret=" + appSecret + "&js_code=" + code + "&grant_type=authorization_code")
                .method("GET", body)
                .build();
        Response response = client.newCall(request).execute();
        JSONObject jsonObject = JSON.parseObject(String.valueOf(response.body()));
        return jsonObject.get("open_id");
    }


    @RequestMapping("/save")
    @ResponseBody
    public void save(User user) {
        userService.save(user);
    }

    @RequestMapping("/getUser")
    @ResponseBody
    public User getUser(@RequestParam String openId) {
        return userService.getOne(new LambdaQueryWrapper<User>().eq(User::getOpenId, openId));
    }

    @RequestMapping("/saveData")
    @ResponseBody
    public void saveData(@RequestBody UserData userData) {
        userData.setCreateTime(new Date());
        userDataService.save(userData);
    }

    @RequestMapping("/getData")
    @ResponseBody
    public String getData(@RequestParam String code) {
        List<UserData> list = userDataService.list(new LambdaQueryWrapper<UserData>()
                .eq(UserData::getOpenId, code)
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