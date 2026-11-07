package com.xsoulp.livingroom;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Log;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

final class ShelfRepository {
    static final class Snapshot {
        final List<ShelfItem> items=new ArrayList<>();
        final List<LatestItem> latest=new ArrayList<>();
        JellyfinApi api;
        String status;
        boolean kidsBookcase, retryNeeded;
        String cacheScope="";
    }
    private final Context context;
    private final JellyfinStore store;
    private final ShelfCache cache;
    ShelfRepository(Context context) { this.context=context.getApplicationContext(); store=new JellyfinStore(context);
        cache=new ShelfCache(new java.io.File(context.getCacheDir(),"shelf-startup")); }
    private String cacheScope(boolean kids) {
        String token=store.token();
        if(token.isEmpty())return "";
        android.content.SharedPreferences prefs=context.getSharedPreferences("room",Context.MODE_PRIVATE);
        String libraryKey=kids?"kidsLibraries":"adultLibraries";
        java.util.List<String> libraries=new ArrayList<>(prefs.getStringSet(libraryKey,java.util.Collections.emptySet()));
        java.util.Collections.sort(libraries);
        StringBuilder scope=new StringBuilder(store.server()).append('\n').append(store.userId()).append('\n')
            .append(token).append('\n').append(kids).append('\n').append(prefs.contains(libraryKey)).append(libraries);
        for(int slot:new int[]{1,3})scope.append('\n').append(prefs.getString(netflixKey(kids,slot),""));
        return ShelfCache.hash(scope.toString());
    }
    Snapshot cached(boolean kids) {
        String scope=cacheScope(kids);
        Snapshot result=new Snapshot();result.kidsBookcase=kids;result.cacheScope=scope;
        if(!scope.isEmpty())result.items.addAll(cache.shelves(scope,kids?KidsBookcase.COUNT:8));
        if(!result.items.isEmpty()) {
            result.api=new JellyfinApi(store.server(),store.token(),store.userId(),store.deviceId());
            result.status="Showing saved shelves · refreshing…";
        }
        return result;
    }
    Snapshot loadRoom(boolean kids) {
        String scope=cacheScope(kids);
        Snapshot result=kids?loadKidsBookcase():load(false);
        result.cacheScope=scope.equals(cacheScope(kids))?scope:"";
        if(result.cacheScope.isEmpty())return result;
        if(result.retryNeeded) {
            List<ShelfItem> saved=cache.shelves(result.cacheScope,kids?KidsBookcase.COUNT:8);
            if(!saved.isEmpty()) {
                result.items.clear();result.items.addAll(saved);
                result.status="Showing saved shelves · waiting for Jellyfin; retrying automatically…";
            }
        } else cache.saveShelves(result.cacheScope,result.items);
        return result;
    }
    void clearCache() { cache.clear(); }
    Snapshot load() { return load(false); }
    Snapshot load(boolean kids) {
        Snapshot result=new Snapshot();
        String token=store.token();
        android.content.SharedPreferences prefs=context.getSharedPreferences("room",Context.MODE_PRIVATE);
        String preferenceKey=kids?"kidsLibraries":"adultLibraries";
        java.util.Set<String> parents=new java.util.HashSet<>(prefs.getStringSet(preferenceKey,java.util.Collections.emptySet()));
        result.status=token.isEmpty()?"Connect your Jellyfin account in Options":parents.isEmpty()?"Choose libraries for this view in Options":"";
        if(!token.isEmpty()) result.api=new JellyfinApi(store.server(),token,store.userId(),store.deviceId());
        if(result.api!=null && !prefs.contains(preferenceKey)) {
            try {
                org.json.JSONArray libraries=result.api.libraries();
                for(int i=0;i<libraries.length();i++) {
                    org.json.JSONObject library=libraries.getJSONObject(i);
                    String name=library.optString("Name").trim().toLowerCase(java.util.Locale.ROOT).replaceAll("[()]", "").replaceAll("\\s+"," ");
                    if(kids?(name.equals("tvshows kids")||name.equals("shows kids")||name.equals("movies kids")):
                        (name.equals("movies +18")||name.equals("tvshows +18")||name.equals("movies 18+")||name.equals("shows 18+"))) parents.add(library.optString("Id"));
                }
                if(!parents.isEmpty()) { prefs.edit().putStringSet(preferenceKey,parents).apply(); result.status=""; }
            } catch(Exception e) { result.status=jellyfinFailure(e); result.retryNeeded|=ShelfRecovery.retryable(e); }
        }
        List<ShelfItem> ongoingMovies=new ArrayList<>(), recentMovies=new ArrayList<>();
        for(int group=0;group<4;group++) {
            boolean ongoing=group<2, series=group%2==1;
            List<ShelfItem> candidates=new ArrayList<>();
            if(result.api!=null && !result.retryNeeded) for(String parent:parents) {
                try { candidates.addAll(result.api.shelf(parent,ongoing,series)); }
                catch(Exception e) { result.status=jellyfinFailure(e); result.retryNeeded|=ShelfRecovery.retryable(e); if(result.retryNeeded)break; }
            }
            java.util.Collections.sort(candidates,(a,b) -> b.sortDate.compareTo(a.sortDate));
            if(group==0) ongoingMovies.addAll(candidates);
            if(group==2) recentMovies.addAll(candidates);
            java.util.HashSet<String> keys=new java.util.HashSet<>();
            int count=0;
            for(ShelfItem item:candidates) if(keys.add(series && ongoing?item.imagePath:item.key) && count<2) { result.items.add(item); count++; }
            while(count<2) {
                String title=(series?"Series":"Movie")+" · "+(count+1);
                String reason=result.status.isEmpty()?(ongoing?"Nothing to continue":"No recent additions"):result.status;
                result.items.add(new ShelfItem(token.isEmpty()?ShelfItem.CONNECT:ShelfItem.SHORTCUT,
                    "empty:"+kids+":"+group+":"+count,title,reason,"org.jellyfin.androidtv","","","",0,0));
                count++;
            }
        }
        WatchNextReader.Result shared=WatchNextReader.read(context);
        NetflixShelfStore netflix=new NetflixShelfStore(context);
        netflix.apply(result,shared.items,kids,1);
        netflix.apply(result,shared.items,kids,3);
        // Netflix can replace the second ongoing movie. Apply the final exclusion after that
        // replacement and continue through the recent candidates to fill both positions.
        List<ShelfItem> recent=MovieShelfSelector.recent(recentMovies,ongoingMovies,result.items.subList(0,2));
        for(int i=0;i<2;i++) {
            ShelfItem item=i<recent.size()?recent.get(i):new ShelfItem(token.isEmpty()?ShelfItem.CONNECT:ShelfItem.SHORTCUT,
                "empty:"+kids+":2:"+i,"Movie · "+(i+1),
                result.status.isEmpty()?"No other recent additions":result.status,"org.jellyfin.androidtv","","","",0,0);
            result.items.set(4+i,item);
        }
        return result;
    }
    Snapshot loadKidsBookcase() {
        Snapshot result=new Snapshot();
        result.kidsBookcase=true; result.status="";
        for(Snapshot part:new Snapshot[]{loadStories(),load(true),loadDisney()}) {
            result.items.addAll(part.items);
            result.retryNeeded|=part.retryNeeded;
            if(part.api!=null) result.api=part.api;
            if(!part.status.isEmpty() && !result.status.contains(part.status))
                result.status+=(result.status.isEmpty()?"":" · ")+part.status;
        }
        return result;
    }
    static String jellyfinFailure(Exception error) {
        if(error instanceof JellyfinApi.ApiException) {
            int status=((JellyfinApi.ApiException)error).status;
            if(status==401) return "Jellyfin session expired. Reconnect your account in Options.";
            if(status==403) return "Jellyfin denied access. Check this account's library permissions.";
        }
        return "Could not refresh Jellyfin. Try again in Options.";
    }
    Snapshot loadStories() {
        Snapshot result=new Snapshot();
        result.status="";
        String token=store.token();
        if(token.isEmpty()) result.status="Connect your Jellyfin account in Options";
        else try {
            result.api=new JellyfinApi(store.server(),token,store.userId(),store.deviceId());
            org.json.JSONArray libraries=result.api.libraries();
            String parent="";
            for(int i=0;i<libraries.length();i++) {
                org.json.JSONObject library=libraries.getJSONObject(i);
                String name=java.text.Normalizer.normalize(library.optString("Name"),java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}","").trim().replaceAll("\\s+"," ");
                if(name.equalsIgnoreCase("Books & Stories") || name.equalsIgnoreCase("Books and Stories")
                    || name.equalsIgnoreCase("Livros e historias")) { parent=library.optString("Id"); break; }
            }
            if(parent.isEmpty()) result.status="Books & Stories library not found in this account";
            else {
                result.items.addAll(result.api.randomBooks(parent));
                result.status=result.items.isEmpty()?"This library has no books available yet":"";
            }
        } catch(Exception e) { result.retryNeeded=ShelfRecovery.retryable(e); result.status=result.retryNeeded?"Could not load books. Retrying when available.":jellyfinFailure(e); }
        while(result.items.size()<8) {
            int slot=result.items.size();
            result.items.add(new ShelfItem(token.isEmpty()?ShelfItem.CONNECT:ShelfItem.SHORTCUT,
                "story-empty:"+slot,"Book · "+(slot+1),result.status.isEmpty()?"No more books in this library":result.status,
                "org.jellyfin.androidtv","","","",0,0));
        }
        return result;
    }
    Snapshot loadDisney() {
        Snapshot result=new Snapshot();
        result.status="";
        String token=store.token();
        ShelfItem[] movies=new ShelfItem[DisneyCatalog.TITLES.length];
        if(token.isEmpty()) result.status="Connect your Jellyfin account in Options";
        else try {
            result.api=new JellyfinApi(store.server(),token,store.userId(),store.deviceId());
            android.content.SharedPreferences prefs=context.getSharedPreferences("room",Context.MODE_PRIVATE);
            java.util.Set<String> parents=new java.util.HashSet<>(prefs.getStringSet("kidsLibraries",java.util.Collections.emptySet()));
            if(!prefs.contains("kidsLibraries")) {
                org.json.JSONArray libraries=result.api.libraries();
                for(int i=0;i<libraries.length();i++) {
                    org.json.JSONObject library=libraries.getJSONObject(i);
                    String name=DisneyCatalog.normalize(library.optString("Name"));
                    if(name.equals("movieskids")||name.equals("showskids")||name.equals("tvshowskids")) parents.add(library.optString("Id"));
                }
                if(!parents.isEmpty()) prefs.edit().putStringSet("kidsLibraries",parents).apply();
            }
            if(parents.isEmpty()) result.status="Choose kids libraries in Options";
            else movies=result.api.disneyMovies(parents).toArray(movies);
        } catch(Exception e) { result.retryNeeded=ShelfRecovery.retryable(e); result.status=result.retryNeeded?"Could not load Disney Library. Retrying when available.":jellyfinFailure(e); }
        int missing=0;
        for(int i=0;i<movies.length;i++) {
            ShelfItem item=movies[i];
            if(item==null) {
                missing++;
                item=new ShelfItem(token.isEmpty()?ShelfItem.CONNECT:ShelfItem.SHORTCUT,"disney-empty:"+i,
                    DisneyCatalog.TITLES[i],result.status.isEmpty()?"Not found in kids libraries · "+DisneyCatalog.YEARS[i]:result.status,
                    "org.jellyfin.androidtv","","","",0,0);
            }
            result.items.add(item);
        }
        if(result.status.isEmpty() && missing>0) result.status=missing+" titles not found in kids libraries";
        return result;
    }
    static String netflixKey(boolean kids,int slot) {
        return (kids?"kids":"adult")+"Netflix"+(slot==1?"Movie":"Series");
    }
    Bitmap cachedPoster(Snapshot snapshot,ShelfItem item) {
        if(item.kind!=ShelfItem.JELLYFIN || snapshot.cacheScope.isEmpty())return null;
        byte[] bytes=cache.read("poster:"+snapshot.cacheScope+":"+item.imagePath);
        return bytes==null?null:JellyfinApi.decodePoster(bytes);
    }
    Bitmap poster(Snapshot snapshot,ShelfItem item) throws Exception {
        if(item.kind==ShelfItem.JELLYFIN && !snapshot.cacheScope.isEmpty()) {
            String key="poster:"+snapshot.cacheScope+":"+item.imagePath;
            Bitmap image=cachedPoster(snapshot,item);
            if(image!=null)return image;
            image=snapshot.api.poster(item);
            if(image!=null) {
                java.io.ByteArrayOutputStream output=new java.io.ByteArrayOutputStream();
                if(image.compress(Bitmap.CompressFormat.PNG,100,output))cache.write(key,output.toByteArray());
            }
            return image;
        }
        return fetchPoster(snapshot,item);
    }
    private Bitmap fetchPoster(Snapshot snapshot,ShelfItem item) throws Exception {
        if(item.kind==ShelfItem.WATCH_NEXT && item.packageName.contains("netflix")) {
            int slot=snapshot.items.indexOf(item);
            if(snapshot.kidsBookcase) slot-=8;
            if(slot==1 || slot==3) try {
                Bitmap portrait=new PublicPosterReader(context).poster(item.title,slot==1?"movie":"series");
                if(portrait!=null) return portrait;
            } catch(Exception ignored) { /* Retain the real shared artwork when public lookup is unavailable. */ }
        }
        if(item.imagePath.isEmpty()) return null;
        if(item.kind==ShelfItem.JELLYFIN) return snapshot.api.poster(item);
        if(item.kind!=ShelfItem.WATCH_NEXT) return null;
        Uri uri=Uri.parse(item.imagePath);
        if("content".equals(uri.getScheme())) {
            try(InputStream input=context.getContentResolver().openInputStream(uri)) {
                return input==null?null:JellyfinApi.decodePoster(JellyfinApi.readBounded(input,1024*1024));
            }
        }
        HttpURLConnection connection=(HttpURLConnection)new URL(item.imagePath).openConnection();
        connection.setConnectTimeout(5000); connection.setReadTimeout(5000); connection.setInstanceFollowRedirects(false);
        try {
            if(connection.getResponseCode()!=200) return null;
            try(InputStream input=connection.getInputStream()) { return JellyfinApi.decodePoster(JellyfinApi.readBounded(input,1024*1024)); }
        } finally { connection.disconnect(); }
    }
}
