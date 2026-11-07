package com.xsoulp.livingroom;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Open-Meteo current model conditions and local sunrise/sunset; no location permissions or API key. */
final class OutdoorWeatherClient {
    static String url(double latitude,double longitude) {
        if(!OutdoorWeather.finite(latitude) || !OutdoorWeather.finite(longitude) || latitude< -90 || latitude>90 || longitude< -180 || longitude>180)
            throw new IllegalArgumentException("Invalid weather coordinates");
        return "https://api.open-meteo.com/v1/forecast?latitude="+latitude+"&longitude="+longitude
            +"&current=temperature_2m,is_day,weather_code,cloud_cover,wind_speed_10m"
            +"&daily=sunrise,sunset&temperature_unit=celsius&wind_speed_unit=kmh"
            +"&timezone=auto&timeformat=unixtime&forecast_days=3";
    }
    static JSONObject fetch(double latitude,double longitude) throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(url(latitude,longitude)).openConnection();
        connection.setConnectTimeout(6000);connection.setReadTimeout(8000);connection.setInstanceFollowRedirects(false);
        connection.setRequestProperty("User-Agent","FamilyShelf/OutdoorScreensaver");
        try {
            if(connection.getResponseCode()!=200)throw new java.io.IOException("Weather temporarily unavailable");
            try(InputStream input=connection.getInputStream()) {
                JSONObject json=new JSONObject(new String(JellyfinApi.readBounded(input,128*1024),StandardCharsets.UTF_8));
                new OutdoorWeather(json);return json;
            }
        } finally { connection.disconnect(); }
    }
}
