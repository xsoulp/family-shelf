package com.xsoulp.livingroom;

import java.util.Calendar;
import java.util.TimeZone;
import org.json.*;

/** Provider observations retain their own timestamp; cached readings are never labelled live. */
final class OutdoorWeather {
    static final long FRESH_MS=45*60_000L;
    final double temperature,wind;
    final int clouds,code;
    final long observedAt;
    final boolean observedDay;
    final String zone;
    final long[] sunrise,sunset;
    OutdoorWeather(JSONObject json) throws JSONException {
        JSONObject current=json.getJSONObject("current");
        temperature=current.getDouble("temperature_2m");wind=current.getDouble("wind_speed_10m");
        clouds=current.getInt("cloud_cover");code=current.getInt("weather_code");
        observedAt=current.getLong("time")*1000L;observedDay=current.getInt("is_day")==1;
        zone=json.getString("timezone");
        if(!finite(temperature) || temperature< -100 || temperature>70
            || !finite(wind) || wind<0 || wind>500 || clouds<0 || clouds>100
            || observedAt<=0 || !java.util.Arrays.asList(TimeZone.getAvailableIDs()).contains(zone))
            throw new JSONException("Invalid weather observation");
        JSONObject daily=json.getJSONObject("daily");
        JSONArray rises=daily.getJSONArray("sunrise"),sets=daily.getJSONArray("sunset");
        if(rises.length()==0 || rises.length()!=sets.length())throw new JSONException("Missing solar times");
        sunrise=new long[rises.length()];sunset=new long[rises.length()];
        for(int i=0;i<sunrise.length;i++) {
            sunrise[i]=rises.getLong(i)*1000L;sunset[i]=sets.getLong(i)*1000L;
            if(sunset[i]<=sunrise[i])throw new JSONException("Invalid solar times");
        }
    }
    static boolean finite(double value) { return !Double.isNaN(value) && !Double.isInfinite(value); }
    boolean fresh(long now) { return now>=observedAt-5*60_000L && now-observedAt<=FRESH_MS; }
    boolean day(long now) {
        Calendar current=Calendar.getInstance(TimeZone.getTimeZone(zone));current.setTimeInMillis(now);
        Calendar solar=Calendar.getInstance(TimeZone.getTimeZone(zone));
        for(int i=0;i<sunrise.length;i++) {
            solar.setTimeInMillis(sunrise[i]);
            if(current.get(Calendar.YEAR)==solar.get(Calendar.YEAR) && current.get(Calendar.DAY_OF_YEAR)==solar.get(Calendar.DAY_OF_YEAR))
                return now>=sunrise[i] && now<sunset[i];
        }
        if(fresh(now))return observedDay;
        return fallbackDay(now,TimeZone.getTimeZone(zone));
    }
    static boolean fallbackDay(long now,TimeZone zone) {
        Calendar c=Calendar.getInstance(zone);c.setTimeInMillis(now);
        return c.get(Calendar.HOUR_OF_DAY)>=7 && c.get(Calendar.HOUR_OF_DAY)<19;
    }
    boolean rain() { return (code>=51 && code<=67) || (code>=80 && code<=82) || code>=95; }
    boolean snow() { return (code>=71 && code<=77) || code==85 || code==86; }
    String condition() {
        if(code>=95)return "Thunderstorms";
        if(snow())return "Snow";
        if(rain())return code<60?"Drizzle":"Rain";
        if(code==45 || code==48)return "Fog";
        return clouds<15?"Clear sky":clouds<65?"Partly cloudy":"Cloudy";
    }
}
