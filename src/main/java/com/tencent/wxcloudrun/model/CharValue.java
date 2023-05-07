package com.tencent.wxcloudrun.model;

import java.util.Date;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * @author cong.liu@mthreads.com
 * @version 1.0
 * @since 1.0
 */
@Data
@AllArgsConstructor
public class CharValue {
  private List<String> xData;

  private List<Double> yData;
}
