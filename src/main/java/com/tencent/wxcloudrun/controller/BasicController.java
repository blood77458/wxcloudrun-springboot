
package com.tencent.wxcloudrun.controller;

import com.alibaba.fastjson.JSON;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;

import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.tencent.wxcloudrun.dto.IRequestBody;
import com.tencent.wxcloudrun.dto.YoloDto;
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
                String flvAddress = yingshiUtils.getAddress(one.getDeviceSerial(), 2);
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
        path = URLEncoder.encode(path);
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
            String flvAddress = yingshiUtils.getAddress(one.getDeviceSerial(), 2);
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
    @PostMapping("/getDataNum")
    @ResponseBody
    public String getDataNum(HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("X-WX-OPENID");
        List<UserData> list = userDataService.list(new LambdaQueryWrapper<UserData>()
                .eq(UserData::getOpenId, header)
                .between(UserData::getCreateTime, getMinute(-10), new Date())
                .orderByAsc(UserData::getCreateTime));
        List<String> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");
        for (int i = 0; i < list.size() - 1; i++) {
            UserData userData = list.get(i);
            String yoloData = userData.getYoloData();
            try {
                YoloDto yoloDto = JSON.parseObject(yoloData, YoloDto.class);
                Double value = getD(yoloDto.getYmax());
                Date createTime = userData.getCreateTime();
                createTime.setTime(createTime.getTime() +8*60*60*1000);
                xData.add(dateFormat.format(createTime));
                yData.add(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return JSON.toJSONString(new CharValue(xData, yData));
    }

    @PostMapping("/getDataHight")
    @ResponseBody
    public String getDataHight(@RequestBody String data, HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("X-WX-OPENID");
        IRequestBody req_data = JSON.parseObject(data, IRequestBody.class);
        int time = req_data.getTime();
        List<UserData> list = userDataService.list(new LambdaQueryWrapper<UserData>()
                .eq(UserData::getOpenId, header)
                .between(UserData::getCreateTime, getMinute(-time), new Date())
                .orderByAsc(UserData::getCreateTime));
        List<String> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");
        int merge_size = time / 10;
        if (merge_size == 0) merge_size = 1;
        int i = 0;
        Double avg = 0.0;
        long avg_time = 0;
        while (i < list.size())
        {
            int merge_index = 0;
            avg = 0.0;
            avg_time = 0;
            while (merge_index < merge_size && i+merge_index < list.size())
            {
                UserData userData = list.get(i+merge_index);
                String yoloData = userData.getYoloData();
                try {
                    YoloDto yoloDto = JSON.parseObject(yoloData, YoloDto.class);
                    Double value = getD(yoloDto.getYmax());
                    avg = avg / (merge_index + 1) * merge_index + value / (merge_index + 1);
                    long time_value = userData.getCreateTime().getTime();
                    avg_time = avg_time / (merge_index + 1) * merge_index + time_value / (merge_index + 1);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                merge_index++;
            }
            Date createTime = new Date();
            createTime.setTime(avg_time + 8*60*60*1000);
            xData.add(dateFormat.format(createTime));
            yData.add(avg);

            i += merge_size;
        }
        return JSON.toJSONString(new CharValue(xData, yData));
    }
    @PostMapping("/getDataHasFish")
    @ResponseBody
    public String getDataHasFish(HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("X-WX-OPENID");
        List<UserData> list = userDataService.list(new LambdaQueryWrapper<UserData>()
                .eq(UserData::getOpenId, header)
                .between(UserData::getCreateTime, getMinute(-10), new Date())
                .orderByAsc(UserData::getCreateTime));
        List<String> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");
        for (int i = 0; i < list.size() - 1; i++) {
            UserData userData = list.get(i);
            String yoloData = userData.getYoloData();
            try {
                YoloDto yoloDto = JSON.parseObject(yoloData, YoloDto.class);
                Double value = getHasFish(getD(yoloDto.getXmin()), getD(yoloDto.getXmax()), getD(yoloDto.getYmin()), getD(yoloDto.getYmax()));
                Date createTime = userData.getCreateTime();
                createTime.setTime(createTime.getTime() +8*60*60*1000);
                xData.add(dateFormat.format(createTime));
                yData.add(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return JSON.toJSONString(new CharValue(xData, yData));
    }
    @PostMapping("/getData")
    @ResponseBody
    public String getData(HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("X-WX-OPENID");
        List<UserData> list = userDataService.list(new LambdaQueryWrapper<UserData>()
                .eq(UserData::getOpenId, header)
                .between(UserData::getCreateTime, getMinute(-30), new Date())
                .orderByAsc(UserData::getCreateTime));
        List<String> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");
        for (int i = 0; i < list.size() - 1; i++) {
            UserData userData = list.get(i);
            UserData userData1 = list.get(i + 1);
            String yoloData = userData.getYoloData();
            String yoloData1 = userData1.getYoloData();
            try {
                YoloDto yoloDto = JSON.parseObject(yoloData, YoloDto.class);
                YoloDto yoloDto1 = JSON.parseObject(yoloData1, YoloDto.class);
                Double value = getPosition(getD(yoloDto.getXmin()), getD(yoloDto.getYmin()), getD(yoloDto1.getXmin()), getD(yoloDto1.getYmin()));
                if (value == null) {
                    continue;
                }
                Date createTime = userData1.getCreateTime();
                createTime.setTime(createTime.getTime() +8*60*60*1000);

                xData.add(dateFormat.format(createTime));
                yData.add(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return JSON.toJSONString(new CharValue(xData, yData));
    }
    @PostMapping("/getDataSpeed")
    @ResponseBody
    public String getDataSpeed(HttpServletRequest httpServletRequest) {
        String header = httpServletRequest.getHeader("X-WX-OPENID");
        List<UserData> list = userDataService.list(new LambdaQueryWrapper<UserData>()
                .eq(UserData::getOpenId, header)
                .between(UserData::getCreateTime, getMinute(-30), new Date())
                .orderByAsc(UserData::getCreateTime));
        List<String> xData = new ArrayList<>();
        List<Double> yData = new ArrayList<>();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm:ss");
        for (int i = 0; i < list.size() - 1; i++) {
            UserData userData = list.get(i);
            UserData userData1 = list.get(i + 1);
            String yoloData = userData.getYoloData();
            String yoloData1 = userData1.getYoloData();
            try {
                YoloDto yoloDto = JSON.parseObject(yoloData, YoloDto.class);
                YoloDto yoloDto1 = JSON.parseObject(yoloData1, YoloDto.class);
                Double value = getPosition(getD(yoloDto.getXmin()), getD(yoloDto.getYmin()), getD(yoloDto1.getXmin()), getD(yoloDto1.getYmin()));
                if (value == null) {
                    continue;
                }
                Date createTime = userData1.getCreateTime();
                createTime.setTime(createTime.getTime() +8*60*60*1000);

                xData.add(dateFormat.format(createTime));
                yData.add(value);
            } catch (Exception e) {
                e.printStackTrace();
            }
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

    private static Double getD(Map<String, Double> map) {
        if (map == null) {
            return 0D;
        }
        return map.get("0");
    }

    private static Double getPosition(Double x1, Double y1, Double x2, Double y2) {
        if (x1 == null || y1 == null || x2 == null || y2 == null) {
            return null;
        }
        return Math.sqrt(Math.pow((x1 - x2), 2) + Math.pow((y1 - y2), 2));
    }

    private static Double getHasFish(Double xmin, Double xmax, Double ymin, Double ymax) {
        if (xmin == null && xmax == null && ymin == null && ymax == null)
        {
            return 0.0;
        }
        return 1.0;
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

    private static Date getHour(int amount) {
        Date date = new Date();//取时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        //把日期往后增加一天.整数往后推,负数往前移动(1:表示明天、-1：表示昨天，0：表示今天)
        calendar.add(Calendar.HOUR, amount);
        //这个时间就是日期往后推一天的结果
        return calendar.getTime();
    }
    private static Date getMinute(int amount) {
        Date date = new Date();//取时间
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        //把日期往后增加一天.整数往后推,负数往前移动(1:表示明天、-1：表示昨天，0：表示今天)
        calendar.add(Calendar.MINUTE, amount);
        //这个时间就是日期往后推一天的结果
        return calendar.getTime();
    }

}
