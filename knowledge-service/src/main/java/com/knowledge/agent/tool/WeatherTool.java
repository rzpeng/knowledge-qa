package com.knowledge.agent.tool;

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
    private static final String GEO_API = "https://geocoding-api.open-meteo.com/v1/search";

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

            java.net.URL url = new java.net.URL(WEATHER_API + "?latitude=" + coords[0]
                    + "&longitude=" + coords[1]
                    + "&current=temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m"
                    + "&daily=temperature_2m_max,temperature_2m_min,weather_code"
                    + "&timezone=Asia/Shanghai");
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept-Encoding", "identity");
            String response;
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = br.readLine()) != null) sb.append(line);
                response = sb.toString();
            }
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
        try {
            String encodedCity = java.net.URLEncoder.encode(city, "UTF-8");
            java.net.URL geoUrl = new java.net.URL(GEO_API + "?name=" + encodedCity
                    + "&count=5&language=zh&format=json");
            log.info("Geocoding URL: {}", geoUrl);
            java.net.HttpURLConnection conn = (java.net.HttpURLConnection) geoUrl.openConnection();
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setRequestMethod("GET");
            String geoResponse;
            try (java.io.BufferedReader br = new java.io.BufferedReader(
                    new java.io.InputStreamReader(conn.getInputStream(), "UTF-8"))) {
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = br.readLine()) != null) sb.append(line);
                geoResponse = sb.toString();
            }
            JsonNode geoRoot = objectMapper.readTree(geoResponse);
            JsonNode results = geoRoot.get("results");
            if (results != null && results.isArray() && results.size() > 0) {
                // Prefer Chinese result (country=CN) if multiple matches
                for (JsonNode node : results) {
                    JsonNode country = node.get("country_code");
                    if (country != null && "CN".equalsIgnoreCase(country.asText())) {
                        return new double[]{
                                node.get("latitude").asDouble(),
                                node.get("longitude").asDouble()
                        };
                    }
                }
                // Fall back to first result
                JsonNode first = results.get(0);
                return new double[]{
                        first.get("latitude").asDouble(),
                        first.get("longitude").asDouble()
                };
            }
        } catch (Exception e) {
            log.warn("Geocoding API failed for city '{}', falling back to static map", city, e);
        }
        return null;
    }
}
