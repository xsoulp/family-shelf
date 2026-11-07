package com.xsoulp.livingroom;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class OutdoorWeatherTest {
    private JSONObject fixture() throws Exception {
        return new JSONObject("{\"timezone\":\"Europe/Lisbon\",\"current\":{\"time\":1793959200,\"temperature_2m\":16.5,\"wind_speed_10m\":12.2,\"cloud_cover\":80,\"weather_code\":61,\"is_day\":1},\"daily\":{\"sunrise\":[1793948400],\"sunset\":[1793984400]}}");
    }
    @Test public void retainsTemperatureCloudsWindAndSourceTimestamp() throws Exception {
        OutdoorWeather w=new OutdoorWeather(fixture());
        assertEquals(16.5,w.temperature,.01);assertEquals(80,w.clouds);assertEquals(12.2,w.wind,.01);
        assertEquals(1793959200000L,w.observedAt);assertTrue(w.rain());assertEquals("Rain",w.condition());
    }
    @Test public void switchesAtActualSunriseAndSunsetEvenWhenObservationIsOld() throws Exception {
        OutdoorWeather w=new OutdoorWeather(fixture());
        assertFalse(w.day(w.sunrise[0]-1));assertTrue(w.day(w.sunrise[0]));
        assertTrue(w.day(w.sunset[0]-1));assertFalse(w.day(w.sunset[0]));
    }
    @Test public void staleAndFutureReadingsAreNotPresentedAsFresh() throws Exception {
        OutdoorWeather w=new OutdoorWeather(fixture());
        assertTrue(w.fresh(w.observedAt));assertFalse(w.fresh(w.observedAt+OutdoorWeather.FRESH_MS+1));
        assertFalse(w.fresh(w.observedAt-10*60_000));
    }
    @Test public void distinguishesSnowFogAndClearSky() throws Exception {
        JSONObject j=fixture();j.getJSONObject("current").put("weather_code",73);
        assertTrue(new OutdoorWeather(j).snow());assertFalse(new OutdoorWeather(j).rain());
        j.getJSONObject("current").put("weather_code",45);assertEquals("Fog",new OutdoorWeather(j).condition());
        j.getJSONObject("current").put("weather_code",0).put("cloud_cover",0);assertEquals("Clear sky",new OutdoorWeather(j).condition());
    }
    @Test public void rejectsMissingOrInvalidWeatherInsteadOfInventingAReading() throws Exception {
        try { new OutdoorWeather(new JSONObject());fail(); }catch(JSONException expected){}
        JSONObject j=fixture();j.getJSONObject("current").put("temperature_2m",999);
        try { new OutdoorWeather(j);fail(); }catch(JSONException expected){}
    }
    @Test public void requestsCurrentConditionsWithUnixTimesAndSolarData() {
        String url=OutdoorWeatherClient.url(40,-8);
        assertTrue(url.startsWith("https://api.open-meteo.com/"));
        assertTrue(url.contains("cloud_cover"));assertTrue(url.contains("daily=sunrise,sunset"));
        assertTrue(url.contains("timeformat=unixtime"));assertTrue(url.contains("timezone=auto"));
    }
    @Test public void rejectsInvalidLocationCoordinates() {
        for(double lat:new double[]{Double.NaN,91,-91})try {OutdoorWeatherClient.url(lat,0);fail();}catch(IllegalArgumentException expected){}
    }
}
