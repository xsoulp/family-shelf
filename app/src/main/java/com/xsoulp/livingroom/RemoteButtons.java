package com.xsoulp.livingroom;

import android.content.Context;
import android.content.Intent;
import android.view.KeyEvent;
import android.widget.Toast;

/** Branded keys from the Xiaomi remote's installed key layout. */
final class RemoteButtons {
    static int target(int keyCode) {
        if(keyCode==KeyEvent.KEYCODE_BUTTON_6) return 2; // Netflix
        if(keyCode==KeyEvent.KEYCODE_BUTTON_7) return 1; // Prime Video -> Jellyfin
        if(keyCode==KeyEvent.KEYCODE_BUTTON_3) return 0; // YouTube -> SmartTube
        return -1;
    }
    static Intent launchIntent(Context context,int target) {
        String[] packages=target==2?new String[]{"com.netflix.ninja","com.netflix.mediaclient"}:
            target==1?new String[]{"org.jellyfin.androidtv","dev.jdtech.jellyfin","org.jellyfin.mobile"}:
            target==0?new String[]{"org.smarttube.stable","org.smarttube.beta","com.liskovsoft.smartyoutubetv2.tv",
                "com.liskovsoft.smartyoutubetv2.tv.beta","com.liskovsoft.smarttubetv"}:new String[0];
        for(String name:packages) {
            Intent intent=context.getPackageManager().getLeanbackLaunchIntentForPackage(name);
            if(intent==null) intent=context.getPackageManager().getLaunchIntentForPackage(name);
            if(intent!=null) return intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        }
        return null;
    }
    static boolean handle(Context context,KeyEvent event) {
        int target=target(event.getKeyCode());
        if(target<0) return false;
        String name=target==2?"Netflix":target==1?"Jellyfin":"SmartTube";
        if(event.getAction()==KeyEvent.ACTION_UP && !event.isCanceled()) {
            Intent intent=launchIntent(context,target);
            try {
                if(intent==null) throw new android.content.ActivityNotFoundException();
                context.startActivity(intent);
                android.util.Log.i("LivingRoomRemote","Opened "+name);
            } catch(android.content.ActivityNotFoundException|SecurityException error) {
                Toast.makeText(context,name+" unavailable",Toast.LENGTH_LONG).show();
            }
        }
        return true;
    }
}
