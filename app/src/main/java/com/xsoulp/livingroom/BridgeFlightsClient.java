package com.xsoulp.livingroom;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Free community ADS-B data; shared by the outdoor screensaver and active-video aircraft overlay. */
final class BridgeFlightsClient {
    static final String URL="https://api.adsb.lol/v2/point/38.68917/-9.17694/8";
    static final class Unavailable extends java.io.IOException {
        final long retryMs;
        Unavailable(int status,long retryMs) {super("Flight service unavailable ("+status+")");this.retryMs=retryMs;}
    }
    static JSONObject fetch() throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(URL).openConnection();
        c.setConnectTimeout(6000);c.setReadTimeout(8000);c.setInstanceFollowRedirects(false);
        c.setRequestProperty("User-Agent","FamilyShelf/OutdoorScreensaver (personal use)");
        try {
            int code=c.getResponseCode();
            if(code!=200) {
                long retry=code==401 || code==403?3600000:30000;
                try {retry=Math.max(retry,Long.parseLong(c.getHeaderField("Retry-After"))*1000);}catch(Exception ignored){}
                throw new Unavailable(code,Math.min(86400000,retry));
            }
            try(InputStream in=c.getInputStream()) {
                return new JSONObject(new String(JellyfinApi.readBounded(in,512*1024),StandardCharsets.UTF_8));
            }
        } finally {c.disconnect();}
    }
}
