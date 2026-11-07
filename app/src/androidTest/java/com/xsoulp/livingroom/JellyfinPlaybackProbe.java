package com.xsoulp.livingroom;

import android.app.Instrumentation;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Explicit live acceptance test: starts and briefly resumes each real unfinished item. */
final class JellyfinPlaybackProbe {
    private final Instrumentation instrumentation;
    private final String itemFilter;
    JellyfinPlaybackProbe(Instrumentation instrumentation,String itemFilter) {
        this.instrumentation=instrumentation;
        this.itemFilter=itemFilter;
    }
    private void status(String value) {
        Bundle state=new Bundle(); state.putString("stream",value+"\n"); instrumentation.sendStatus(1,state);
    }
    String run() throws Exception {
        JellyfinStore store=new JellyfinStore(instrumentation.getTargetContext());
        if(store.token().isEmpty()) throw new AssertionError("Pair Jellyfin first");
        JellyfinApi api=new JellyfinApi(store.server(),store.token(),store.userId(),store.deviceId());
        List<ShelfItem> items=api.resume();
        if(!itemFilter.isEmpty()) items.removeIf(item -> !sameId(item.itemId,itemFilter));
        if(items.isEmpty()) throw new AssertionError("No real unfinished items to test");
        // Check this user's normal Sessions API access before starting any video.
        sessions(api);
        JSONArray report=new JSONArray();
        int passed=0;
        try {
            for(int index=0;index<items.size();index++) {
                ShelfItem item=items.get(index);
                status("Testing "+(index+1)+"/"+items.size()+": "+item.title);
                JSONObject row=new JSONObject().put("id",item.itemId).put("title",item.title)
                    .put("savedPositionMs",item.positionMs);
                report.put(row);
                try {
                    // Exercise a real launcher-to-client handoff, allowing Jellyfin's
                    // onStop session teardown before starting the next item.
                    key(KeyEvent.KEYCODE_HOME);
                    SystemClock.sleep(1800);
                    Intent intent=new Intent(Intent.ACTION_VIEW)
                        .setComponent(new ComponentName("org.jellyfin.androidtv","org.jellyfin.androidtv.ui.startup.StartupActivity"))
                        .putExtra("ItemId",ShelfItem.canonicalId(item.itemId)).putExtra("HideSplash",true)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    instrumentation.getTargetContext().startActivity(intent);
                    // The previous details window can remain accessible briefly while StartupActivity routes.
                    SystemClock.sleep(1400);
                    AccessibilityNodeInfo resume=waitForResume(item,20000);
                    if(resume==null) throw new AssertionError("No Resume control exposed by Jellyfin");
                    row.put("resumeControl",String.valueOf(resume.getText()!=null?resume.getText():resume.getContentDescription()));
                    boolean clicked=false;
                    for(int parent=0;resume!=null && parent<5;parent++,resume=resume.getParent()) {
                        if(resume.isClickable() && resume.performAction(AccessibilityNodeInfo.ACTION_CLICK)) { clicked=true; break; }
                    }
                    if(!clicked) throw new AssertionError("Could not activate Jellyfin's Resume control");
                    long deadline=SystemClock.elapsedRealtime()+30000;
                    long first=-1,last=-1;
                    boolean moving=false;
                    while(SystemClock.elapsedRealtime()<deadline) {
                        JSONArray active=sessions(api);
                        for(int i=0;i<active.length();i++) {
                            JSONObject session=active.getJSONObject(i),playing=session.optJSONObject("NowPlayingItem");
                            if(playing==null || !sameId(session.optString("UserId"),store.userId())) continue;
                            String device=session.optString("DeviceName"),client=session.optString("Client");
                            if(!device.equals(android.os.Build.MODEL) && !client.toLowerCase(java.util.Locale.ROOT).contains("android tv")) continue;
                            row.put("activeItemId",playing.optString("Id"));
                            if(!sameId(playing.optString("Id"),item.itemId)) continue;
                            JSONObject state=session.optJSONObject("PlayState"); if(state==null) continue;
                            long position=state.optLong("PositionTicks")/10000;
                            if(first<0 && position>=Math.max(1,item.positionMs-15000)) first=position;
                            last=position;
                            if(first>=0 && position>first+1000 && !state.optBoolean("IsPaused")) moving=true;
                        }
                        if(moving) break;
                        SystemClock.sleep(1200);
                    }
                    row.put("firstObservedPositionMs",first).put("lastObservedPositionMs",last).put("advancing",moving);
                    if(first<Math.max(0,item.positionMs-15000) || first>item.positionMs+120000)
                        throw new AssertionError("Playback did not start near the saved position");
                    if(!moving) throw new AssertionError("Playback position did not advance");
                    row.put("passed",true); passed++;
                    status("PASS: correct item resumed near "+item.positionMs/1000+" seconds; position advanced");
                } catch(Throwable e) {
                    row.put("passed",false).put("error",e.getClass().getSimpleName()+": "+e.getMessage());
                    status("FAIL: "+e.getMessage());
                } finally {
                    key(KeyEvent.KEYCODE_MEDIA_PAUSE);
                    key(KeyEvent.KEYCODE_BACK);
                    SystemClock.sleep(500);
                    writeReport(report);
                }
            }
        } finally { writeReport(report); }
        if(passed!=items.size()) throw new AssertionError(passed+"/"+items.size()+" items passed; see playback-validation.json");
        return "OK ("+passed+" real items opened and resumed with advancing server playback position)\n";
    }
    private void writeReport(JSONArray report) throws Exception {
        String filename=itemFilter.isEmpty()?"playback-validation.json":"playback-retry-validation.json";
        try(java.io.FileOutputStream output=instrumentation.getTargetContext().openFileOutput(filename,0)) {
            output.write(report.toString(2).getBytes(StandardCharsets.UTF_8));
        }
    }
    private static boolean sameId(String a,String b) { return a.replace("-","").equalsIgnoreCase(b.replace("-","")); }
    private JSONArray sessions(JellyfinApi api) throws Exception {
        return new JSONArray(new String(api.request("GET","Sessions",null,2*1024*1024),StandardCharsets.UTF_8));
    }
    private AccessibilityNodeInfo waitForResume(ShelfItem item,long timeout) {
        long end=SystemClock.elapsedRealtime()+timeout;
        while(SystemClock.elapsedRealtime()<end) {
            AccessibilityNodeInfo root=instrumentation.getUiAutomation().getRootInActiveWindow();
            if(root!=null && root.getPackageName()!=null && "org.jellyfin.androidtv".contentEquals(root.getPackageName())) {
                String title=item.subtitle.startsWith("Movie")?item.title:item.subtitle.split(" · ")[0];
                AccessibilityNodeInfo result=containsTitle(root,title)?findResume(root):null;
                if(result!=null) return result;
            }
            SystemClock.sleep(300);
        }
        return null;
    }
    private boolean containsTitle(AccessibilityNodeInfo node,String title) {
        if(node.getText()!=null && node.getText().toString().contains(title)) return true;
        if(node.getContentDescription()!=null && node.getContentDescription().toString().contains(title)) return true;
        for(int i=0;i<node.getChildCount();i++) {
            AccessibilityNodeInfo child=node.getChild(i);
            if(child!=null && containsTitle(child,title)) return true;
        }
        return false;
    }
    private AccessibilityNodeInfo findResume(AccessibilityNodeInfo node) {
        String text=node.getText()==null?"":node.getText().toString();
        String description=node.getContentDescription()==null?"":node.getContentDescription().toString();
        if(text.toLowerCase(java.util.Locale.ROOT).startsWith("resume")
            || text.toLowerCase(java.util.Locale.ROOT).startsWith("retomar")
            || description.toLowerCase(java.util.Locale.ROOT).startsWith("resume")) return node;
        for(int i=0;i<node.getChildCount();i++) {
            AccessibilityNodeInfo child=node.getChild(i); if(child==null) continue;
            AccessibilityNodeInfo result=findResume(child); if(result!=null) return result;
        }
        return null;
    }
    private void key(int code) {
        long now=SystemClock.uptimeMillis();
        instrumentation.getUiAutomation().injectInputEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,code,0),true);
        instrumentation.getUiAutomation().injectInputEvent(new KeyEvent(now,now+30,KeyEvent.ACTION_UP,code,0),true);
    }
}
