package com.xsoulp.livingroom;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.List;
import java.util.Locale;
import org.json.JSONObject;

/** Explicit room assignments: Watch Next itself does not identify Netflix profiles. */
final class NetflixShelfStore {
    private final SharedPreferences room,cache;
    NetflixShelfStore(Context context) {
        this(context.getSharedPreferences("room",Context.MODE_PRIVATE),
            context.getSharedPreferences("netflix-shelf",Context.MODE_PRIVATE));
    }
    NetflixShelfStore(SharedPreferences room,SharedPreferences cache) { this.room=room; this.cache=cache; }

    private static String normalize(String title) {
        return title.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");
    }
    private static String view(boolean kids) { return kids?"kids":"adult"; }
    private String owner(String title) {
        String name=normalize(title);
        String assigned=cache.getString("owner:"+name,"");
        if(!assigned.isEmpty()) return assigned;
        // Titles explicitly confirmed by the owner, never inferred from whichever profile is active.
        if(name.equals("goat") || name.equals("the loud house movie") || name.equals("the loud house")) return "kids";
        if(name.equals("eat pray bark") || name.equals("everwood") || name.equals("running point")) return "adult";
        boolean adult=false,kids=false;
        for(int slot:new int[]{1,3}) {
            adult|=name.equals(normalize(room.getString(ShelfRepository.netflixKey(false,slot),"")));
            kids|=name.equals(normalize(room.getString(ShelfRepository.netflixKey(true,slot),"")));
        }
        return adult==kids?"":kids?"kids":"adult";
    }
    boolean belongsTo(String title,boolean kids) { return view(kids).equals(owner(title)); }
    boolean canAssign(String title,boolean kids) { String owner=owner(title); return owner.isEmpty() || view(kids).equals(owner); }

    String selected(boolean kids,int slot) {
        return room.getString(ShelfRepository.netflixKey(kids,slot),kids?(slot==1?"Goat":"The Loud House"):
            slot==1?"Eat Pray Bark":"Everwood");
    }
    boolean select(boolean kids,int slot,ShelfItem item) {
        if(!netflix(item) || !canAssign(item.title,kids)) return false;
        cache.edit().putString("owner:"+normalize(item.title),view(kids)).apply();
        room.edit().putString(ShelfRepository.netflixKey(kids,slot),item.title).apply();
        remember(kids,slot,item);
        return true;
    }
    static boolean netflix(ShelfItem item) {
        return item.kind==ShelfItem.WATCH_NEXT && ("com.netflix.ninja".equals(item.packageName)
            || "com.netflix.mediaclient".equals(item.packageName));
    }
    void apply(ShelfRepository.Snapshot snapshot,List<ShelfItem> live,boolean kids,int slot) {
        String title=selected(kids,slot);
        if(!belongsTo(title,kids)) return;
        for(ShelfItem item:live) if(netflix(item) && normalize(item.title).equals(normalize(title))) {
            remember(kids,slot,item);
            snapshot.items.set(slot,item);
            return;
        }
        ShelfItem saved=restore(kids,slot,title);
        if(saved!=null) snapshot.items.set(slot,saved);
        // With no confirmed live or cached record, retain the existing second Jellyfin film/series.
    }
    private void remember(boolean kids,int slot,ShelfItem item) {
        try {
            JSONObject json=new JSONObject().put("title",item.title).put("key",item.key)
                .put("subtitle",item.subtitle).put("package",item.packageName).put("image",item.imagePath)
                .put("intent",item.intentUri).put("position",item.positionMs).put("duration",item.durationMs)
                .put("savedAt",System.currentTimeMillis());
            cache.edit().putString("record:"+ShelfRepository.netflixKey(kids,slot),json.toString()).apply();
        } catch(org.json.JSONException ignored) { /* Live data can still be displayed. */ }
    }
    private ShelfItem restore(boolean kids,int slot,String title) {
        try {
            JSONObject json=new JSONObject(cache.getString("record:"+ShelfRepository.netflixKey(kids,slot),""));
            if(!normalize(title).equals(normalize(json.getString("title"))) || !belongsTo(json.getString("title"),kids)) return null;
            long position=json.getLong("position"),duration=json.getLong("duration");
            if(position<0 || duration<0 || (duration>0 && position>=duration) || json.getString("intent").isEmpty()) return null;
            String when=new java.text.SimpleDateFormat("dd/MM HH:mm",Locale.getDefault()).format(new java.util.Date(json.getLong("savedAt")));
            ShelfItem item=new ShelfItem(ShelfItem.WATCH_NEXT,json.getString("key"),json.getString("title"),
                "Netflix · Guardado "+when,json.getString("package"),"",json.getString("image"),json.getString("intent"),position,duration);
            return netflix(item)?item:null;
        } catch(org.json.JSONException ignored) { return null; }
    }
}
