package com.xsoulp.livingroom;

import android.accessibilityservice.AccessibilityService;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;

/** Filters branded keys and observes foreground package changes, without reading screen contents. */
public final class RemoteButtonService extends AccessibilityService {
    static boolean connected;
    @Override protected void onServiceConnected() {connected=true;NetflixAircraftService.foreground(null);}
    @Override protected boolean onKeyEvent(KeyEvent event) { return RemoteButtons.handle(this,event); }
    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if(event.getEventType()!=AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED || event.getPackageName()==null)return;
        String name=event.getPackageName().toString(),type=String.valueOf(event.getClassName());
        if(name.equals("android") || name.equals("com.android.systemui"))return;
        if(name.equals(getPackageName()) && !type.endsWith("MainActivity") && !type.endsWith(".Home")
            && !type.endsWith("JellyfinConnectActivity"))return;
        NetflixAircraftService.foreground(name);
    }
    @Override public void onDestroy() {connected=false;NetflixAircraftService.foreground(null);super.onDestroy();}
    @Override public void onInterrupt() { }
}
