package com.knowledge.agent.tool;

import cn.hutool.http.HttpUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class WeatherTool implements Tool {

    private final ObjectMapper objectMapper;

    private static final String WEATHER_API = "https://api.open-meteo.com/v1/forecast";

    @Override
    public String name() {
        return "weather_query";
    }

    @Override
    public String description() {
        return "查询指定城市的天气情况，返回温度、天气状况、湿度、风速等信息。";
    }

    @Override
    public List<ToolParameter> parameters() {
        return List.of(
                ToolParameter.builder()
                        .name("city")
                        .description("城市名称，如：北京、上海、广州")
                        .type(ParamType.STRING)
                        .required(true)
                        .build(),
                ToolParameter.builder()
                        .name("date")
                        .description("日期，格式为YYYY-MM-DD，默认为今天")
                        .type(ParamType.STRING)
                        .required(false)
                        .build()
        );
    }

    @Override
    public ToolResult execute(Map<String, Object> args) {
        try {
            String city = (String) args.get("city");
            double[] coords = getCoordinates(city);
            if (coords == null) {
                return ToolResult.fail("未找到城市: " + city);
            }

            String url = WEATHER_API + "?latitude=" + coords[0]
                    + "&longitude=" + coords[1]
                    + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
                    + "&daily=temperature_2m_max,temperature_2m_min,weather_code"
                    + "&timezone=Asia/Shanghai";

            String response = HttpUtil.get(url, 5000);
            JsonNode root = objectMapper.readTree(response);
            JsonNode current = root.get("current");

            String result = String.format(
                    "城市: %s\n当前温度: %s°C\n湿度: %s%%\n风速: %s km/h\n天气代码: %s",
                    city,
                    current.get("temperature_2m").asText(),
                    current.get("relative_humidity_2m").asText(),
                    current.get("wind_speed_10m").asText(),
                    current.get("weather_code").asText()
            );

            return ToolResult.ok(result);
        } catch (Exception e) {
            log.error("Weather query failed", e);
            return ToolResult.fail("天气查询失败: " + e.getMessage());
        }
    }

    private double[] getCoordinates(String city) {
        Map<String, double[]> cityMap = Map.ofEntries(
                Map.entry("北京", new double[]{39.9042, 116.4074}),
                Map.entry("上海", new double[]{31.2304, 121.4737}),
                Map.entry("广州", new double[]{23.1291, 113.2644}),
                Map.entry("深圳", new double[]{22.5431, 114.0579}),
                Map.entry("杭州", new double[]{30.2741, 120.1551}),
                Map.entry("成都", new double[]{30.5728, 104.0668}),
                Map.entry("武汉", new double[]{30.5928, 114.3055}),
                Map.entry("南京", new double[]{32.0603, 118.7969}),
                Map.entry("西安", new double[]{34.3416, 108.9398}),
                Map.entry("重庆", new double[]{29.4316, 106.9123})
        );
        double[] coords = cityMap.get(city);
        if (coords != null) return coords;
        for (Map.Entry<String, double[]> entry : cityMap.entrySet()) {
            if (city.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return null;
    }
}
