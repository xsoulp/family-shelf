package com.xsoulp.livingroom;

import org.json.JSONObject;
import java.util.Locale;

/** Only server/provider records become media cards. Shortcuts are explicitly labelled. */
final class ShelfItem {
    static final int JELLYFIN = 0, WATCH_NEXT = 1, SHORTCUT = 2, CONNECT = 3;
    final int kind;
    final String key, title, subtitle, packageName, itemId, imagePath, intentUri;
    final long positionMs, durationMs;
    final String overview;
    String sortDate="";

    ShelfItem(int kind, String key, String title, String subtitle, String packageName,
              String itemId, String imagePath, String intentUri, long positionMs, long durationMs) {
        this(kind,key,title,subtitle,packageName,itemId,imagePath,intentUri,positionMs,durationMs,"");
    }
    ShelfItem(int kind, String key, String title, String subtitle, String packageName,
              String itemId, String imagePath, String intentUri, long positionMs, long durationMs,String overview) {
        this.kind=kind; this.key=key; this.title=title; this.subtitle=subtitle;
        this.packageName=packageName; this.itemId=itemId; this.imagePath=imagePath;
        this.intentUri=intentUri; this.positionMs=positionMs; this.durationMs=durationMs;
        this.overview=overview;
    }

    static ShelfItem shortcut(String title, String packageName, String reason) {
        return new ShelfItem(SHORTCUT, "app:"+packageName, "Open "+title, reason,
            packageName, "", "", "", 0, 0);
    }

    static ShelfItem connect() {
        return new ShelfItem(CONNECT, "connect", "Connect Jellyfin", "Sign in to see unfinished films and episodes",
            "org.jellyfin.androidtv", "", "", "", 0, 0);
    }

    static ShelfItem fromJellyfin(JSONObject item) {
        String type = item.optString("Type");
        String id = item.optString("Id");
        JSONObject data = item.optJSONObject("UserData");
        if (!("Movie".equals(type) || "Episode".equals(type)) || !validId(id) || data == null
                || data.optBoolean("Played")) return null;
        long position = Math.max(0, data.optLong("PlaybackPositionTicks"))/10000;
        long duration = Math.max(0, item.optLong("RunTimeTicks"))/10000;
        if (position <= 0 || duration <= position) return null;
        String title = item.optString("Name", "Untitled");
        String detail = "Movie · Jellyfin";
        if ("Episode".equals(type)) {
            String episode = (item.has("ParentIndexNumber") ? "S"+item.optInt("ParentIndexNumber")+" " : "")
                + (item.has("IndexNumber") ? "E"+item.optInt("IndexNumber") : "");
            detail = title+" · "+episode+" · Jellyfin";
            title = item.optString("SeriesName", title);
        }
        String imageId=id, tag="";
        JSONObject tags = item.optJSONObject("ImageTags");
        if (tags != null) tag=tags.optString("Primary");
        if ("Episode".equals(type) && validId(item.optString("SeriesId"))) {
            imageId=item.optString("SeriesId");
            tag=item.optString("SeriesPrimaryImageTag");
        } else if (tag.isEmpty() && validId(item.optString("ParentPrimaryImageItemId"))) {
            imageId=item.optString("ParentPrimaryImageItemId");
            tag=item.optString("ParentPrimaryImageTag");
        }
        String image="Items/"+imageId+"/Images/Primary?maxWidth=480&quality=85";
        if (tag.matches("[A-Za-z0-9-]+")) image+="&tag="+tag;
        ShelfItem result=new ShelfItem(JELLYFIN,"jf:"+id,title,detail,"org.jellyfin.androidtv",id,image,"",position,duration,
            item.optString("Overview","").replaceAll("<[^>]*>"," ").trim());
        result.sortDate=data.optString("LastPlayedDate");
        return result;
    }

    static boolean validId(String value) { return value != null && value.replace("-", "").matches("[a-fA-F0-9]{32}"); }
    static String canonicalId(String value) {
        String compact=value.replace("-", "");
        return compact.substring(0,8)+"-"+compact.substring(8,12)+"-"+compact.substring(12,16)
            +"-"+compact.substring(16,20)+"-"+compact.substring(20);
    }
    float progress() { return durationMs > 0 ? Math.max(0,Math.min(1,positionMs/(float)durationMs)) : 0; }
    String progressLabel() {
        if (durationMs <= 0) return "";
        return String.format(Locale.ROOT,"%d%% · %d min left",Math.round(progress()*100),
            Math.max(1,(durationMs-positionMs+59999)/60000));
    }
}
