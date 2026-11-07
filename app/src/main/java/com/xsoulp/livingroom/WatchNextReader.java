package com.xsoulp.livingroom;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import java.util.ArrayList;
import java.util.List;

/** Read only what this app's normal identity may see. No privileged grants or scraping. */
final class WatchNextReader {
    static final class Result {
        final List<ShelfItem> items=new ArrayList<>();
        String status="No shared Watch Next data available";
        int visibleRows;
        boolean permissionGranted;
    }
    static boolean supported(String name) {
        return "com.netflix.ninja".equals(name) || "com.netflix.mediaclient".equals(name)
            || "org.smarttube.stable".equals(name) || "org.smarttube.beta".equals(name)
            || "com.liskovsoft.smartyoutubetv2.tv".equals(name)
            || "com.liskovsoft.smartyoutubetv2.tv.beta".equals(name)
            || "com.liskovsoft.smarttubetv".equals(name);
    }
    static Result read(Context context) {
        Result result=new Result();
        result.permissionGranted=context.checkSelfPermission("android.permission.READ_TV_LISTINGS")
            ==android.content.pm.PackageManager.PERMISSION_GRANTED;
        if(!result.permissionGranted) result.status="Shared TV listings access is off (Options → Allow shared TV data)";
        if(Build.VERSION.SDK_INT<26) { result.status="Watch Next requires Android 8 or newer"; return result; }
        String[] columns={"_id","package_name","title","intent_uri","poster_art_uri",
            "last_playback_position_millis","duration_millis","watch_next_type","browsable"};
        try(Cursor cursor=context.getContentResolver().query(
                Uri.parse("content://android.media.tv/watch_next_program"),columns,null,null,null)) {
            if(cursor==null) { result.status="Watch Next provider unavailable"; return result; }
            while(result.visibleRows<500 && cursor.moveToNext()) {
                result.visibleRows++;
                String pkg=cursor.getString(1),title=cursor.getString(2),intent=cursor.getString(3);
                if(!supported(pkg) || title==null || title.trim().isEmpty() || intent==null || intent.isEmpty()
                    || cursor.getInt(7)!=0 || cursor.getInt(8)!=1) continue;
                long pos=cursor.isNull(5)?0:Math.max(0,cursor.getLong(5));
                long duration=cursor.isNull(6)?0:Math.max(0,cursor.getLong(6));
                if(duration>0 && pos>=duration) continue;
                String image=cursor.getString(4);
                if(image==null) image="";
                Uri uri=Uri.parse(image);
                if(!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()) || "content".equals(uri.getScheme()))) image="";
                result.items.add(new ShelfItem(ShelfItem.WATCH_NEXT,"wn:"+cursor.getLong(0),title,
                    (pkg.contains("netflix")?"Netflix":"SmartTube")+" · Watch Next",pkg,"",image,intent,pos,duration));
            }
            if(!result.items.isEmpty()) result.status=result.items.size()+" shared Watch Next items";
        } catch(SecurityException e) { result.status="Watch Next data is not shared with this launcher"; }
        catch(RuntimeException e) { result.status="Watch Next unavailable on this firmware"; }
        return result;
    }
}
