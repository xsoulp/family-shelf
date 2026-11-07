package com.xsoulp.livingroom;

import java.text.SimpleDateFormat;
import java.util.*;
import org.json.*;

/** Reject other accounts/devices, stale sessions, audio-only items and paused playback. */
final class JellyfinPlaybackMatch {
    static boolean playing(JSONArray sessions,String user,Set<String> addresses,String player,long now) {
        if(user==null || user.isEmpty() || addresses.isEmpty() || !VideoAircraftPolicy.jellyfin(player))return false;
        for(int i=0;i<sessions.length();i++) {
            JSONObject session=sessions.optJSONObject(i);if(session==null)continue;
            if(!user.replace("-","").equalsIgnoreCase(session.optString("UserId").replace("-","")))continue;
            String endpoint=session.optString("RemoteEndPoint");
            if(endpoint.startsWith("::ffff:"))endpoint=endpoint.substring(7);
            if(!addresses.contains(endpoint))continue;
            String client=session.optString("Client").toLowerCase(Locale.ROOT);
            boolean matching="org.jellyfin.androidtv".equals(player)?client.contains("android") && client.contains("tv"):
                "dev.jdtech.jellyfin".equals(player)?client.contains("findroid"):
                client.contains("android") && !client.contains("tv");
            if(!matching)continue;
            JSONObject item=session.optJSONObject("NowPlayingItem"),state=session.optJSONObject("PlayState");
            if(item==null || !"Video".equalsIgnoreCase(item.optString("MediaType")) || state==null
                || state.optBoolean("IsPaused",true) || state.optLong("PositionTicks",0)<=0)continue;
            long stamp=timestamp(session.optString("LastPlaybackCheckIn"));
            if(stamp>0 && Math.abs(now-stamp)<=20000)return true;
        }
        return false;
    }
    private static long timestamp(String value) {
        try {
            // Jellyfin emits UTC with up to seven fractional digits; milliseconds suffice here.
            value=value.replaceFirst("\\.([0-9]{3})[0-9]*Z$",".$1Z");
            SimpleDateFormat format=new SimpleDateFormat(value.contains(".")?"yyyy-MM-dd'T'HH:mm:ss.SSS'Z'":"yyyy-MM-dd'T'HH:mm:ss'Z'",Locale.ROOT);
            format.setTimeZone(TimeZone.getTimeZone("UTC"));format.setLenient(false);
            return format.parse(value).getTime();
        } catch(Exception ignored) {return 0;}
    }
}
