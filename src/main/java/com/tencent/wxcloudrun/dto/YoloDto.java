package com.tencent.wxcloudrun.dto;

import lombok.Data;

import java.util.Map;

@Data
public class YoloDto {

    private Map<String,Double> xmin;
    private Map<String,Double> ymin;
    private Map<String,Double> xmax;
    private Map<String,Double> ymax;
    private Map<String,Double> confidence;

}
