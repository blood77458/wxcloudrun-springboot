package com.tencent.wxcloudrun.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import okhttp3.*;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;

@Component
public class YingshiUtils {

    private static final HashMap<Long, String> cacheMap = new HashMap<>();


    public String getToken() {
        for (Long aLong : cacheMap.keySet()) {
            if (System.currentTimeMillis() < aLong) {
                return cacheMap.get(aLong);
            }
        }
        cacheMap.clear();
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "appKey=e9b66befc7b446e398494494958cd006&appSecret=7861052d0df534d958ca2aaa0ef73e41");
        Request request = new Request.Builder()
                .url("https://open.ys7.com/api/lapp/token/get")
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        try {
            Response response = client.newCall(request).execute();
            assert response.body() != null;
            JSONObject object = JSON.parseObject(response.body().string());
            String token = object.getJSONObject("data").getString("accessToken");
            Long expireTime = object.getJSONObject("data").getLong("expireTime");
            cacheMap.put(expireTime, token);
            return token;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }


    public boolean addDevice(String deviceSerial, String validateCode) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "accessToken=" + getToken() + "&deviceSerial=" + deviceSerial + "&validateCode=" + validateCode);
        Request request = new Request.Builder()
                .url("https://open.ys7.com/api/lapp/device/add")
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        try {
            Response response = client.newCall(request).execute();
            assert response.body() != null;
            JSONObject object = JSON.parseObject(response.body().string());
            String code = object.getString("code");
            return "200".equals(code);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getAddress(String deviceSerial, Integer type) {
        OkHttpClient client = new OkHttpClient().newBuilder()
                .build();
        MediaType mediaType = MediaType.parse("application/x-www-form-urlencoded");
        RequestBody body = RequestBody.create(mediaType, "accessToken=" + getToken() + "&deviceSerial=" + deviceSerial + "&protocol=" + type+"&expireTime=61000000");
        Request request = new Request.Builder()
                .url("https://open.ys7.com/api/lapp/v2/live/address/get")
                .method("POST", body)
                .addHeader("Content-Type", "application/x-www-form-urlencoded")
                .build();
        try {
            Response response = client.newCall(request).execute();
            assert response.body() != null;
            JSONObject object = JSON.parseObject(response.body().string());
            return object.getJSONObject("data").getString("url");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
