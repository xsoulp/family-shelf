package com.xsoulp.livingroom;

import android.content.Context;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Optional local settings do not disclose the household's chosen location in version control. */
final class OutdoorSceneConfig {
    final String label,zone;
    final double latitude,longitude;
    OutdoorSceneConfig(String label,String zone,double latitude,double longitude) {
        this.label=label;this.zone=zone;this.latitude=latitude;this.longitude=longitude;
    }
    static OutdoorSceneConfig load(Context context) {
        try(InputStream input=context.getAssets().open("outdoor-scene.json")) {
            JSONObject j=new JSONObject(new String(JellyfinApi.readBounded(input,8192),StandardCharsets.UTF_8));
            double lat=j.getDouble("latitude"),lon=j.getDouble("longitude");OutdoorWeatherClient.url(lat,lon);
            return new OutdoorSceneConfig(j.getString("label"),j.getString("timezone"),lat,lon);
        } catch(Exception unavailable) { return null; }
    }
}
