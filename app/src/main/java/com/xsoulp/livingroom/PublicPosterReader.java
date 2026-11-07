package com.xsoulp.livingroom;

import android.content.Context;
import android.graphics.Bitmap;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.File;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

/** Public artwork only. Netflix remains the source of history and playback links. */
final class PublicPosterReader {
    private final Context context;
    PublicPosterReader(Context context) { this.context=context; }

    static String match(JSONArray metas,String title,String type) {
        String selected="",id="";
        // Verified identities for the owner's confirmed adult titles, including the homonymous series.
        String expected=normalize(title).equals("running point") && type.equals("series")?"tt14879018":"";
        for(int i=0;i<metas.length();i++) {
            JSONObject meta=metas.optJSONObject(i);
            if(meta==null || !type.equals(meta.optString("type"))
                    || !normalize(title).equals(normalize(meta.optString("name")))) continue;
            if(!expected.isEmpty() && !expected.equals(meta.optString("id"))) continue;
            // A title alone cannot distinguish remakes; skip ambiguous matches.
            if(!id.isEmpty() && !id.equals(meta.optString("id"))) return "";
            String poster=meta.optString("poster");
            if(!poster.startsWith("https://")) continue;
            id=meta.optString("id"); selected=poster;
        }
        return selected;
    }

    private static String normalize(String value) {
        return value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+"," ");
    }

    Bitmap poster(String title,String type) throws Exception {
        byte[] digest=java.security.MessageDigest.getInstance("SHA-256")
            .digest((type+":"+normalize(title)).getBytes(StandardCharsets.UTF_8));
        StringBuilder key=new StringBuilder();
        for(byte b:digest) key.append(String.format(Locale.ROOT,"%02x",b&255));
        File directory=new File(context.getCacheDir(),"public-posters");
        if(!directory.isDirectory() && !directory.mkdirs()) return null;
        File cached=new File(directory,key+".img");
        if(cached.isFile()) {
            try(InputStream input=new java.io.FileInputStream(cached)) {
                Bitmap image=JellyfinApi.decodePoster(JellyfinApi.readBounded(input,2*1024*1024));
                if(image!=null) return image;
            } catch(java.io.IOException ignored) { /* Retry corrupt or unavailable cache entries. */ }
        }
        android.content.SharedPreferences prefs=context.getSharedPreferences("poster-lookups",Context.MODE_PRIVATE);
        long now=System.currentTimeMillis();
        String url=prefs.getString(key.toString(),"");
        if(now>=prefs.getLong(key+":expires",0)) {
            String query="https://v3-cinemeta.strem.io/catalog/"+type+"/top/search="+JellyfinApi.encode(title).replace("+","%20")+".json";
            JSONArray metas=new JSONObject(new String(fetch(query,1024*1024),StandardCharsets.UTF_8)).optJSONArray("metas");
            url=metas==null?"":match(metas,title,type);
            prefs.edit().putString(key.toString(),url)
                .putLong(key+":expires",now+(url.isEmpty()?15*60*1000L:7*24*60*60*1000L)).apply();
        }
        if(url.isEmpty()) return null;
        byte[] bytes=fetch(url,2*1024*1024);
        Bitmap image=JellyfinApi.decodePoster(bytes);
        if(image==null) return null;
        if(image.getWidth()>=image.getHeight()) { image.recycle(); return null; }
        try(java.io.FileOutputStream output=new java.io.FileOutputStream(cached)) { output.write(bytes); }
        catch(java.io.IOException ignored) { /* Display even if cache is full. */ }
        File[] files=directory.listFiles();
        if(files!=null && files.length>24) {
            java.util.Arrays.sort(files,(a,b) -> Long.compare(a.lastModified(),b.lastModified()));
            for(int i=0;i<files.length-24;i++) if(!files[i].equals(cached)) files[i].delete();
        }
        return image;
    }

    private static byte[] fetch(String url,int limit) throws Exception {
        HttpURLConnection connection=(HttpURLConnection)new URL(url).openConnection();
        connection.setConnectTimeout(5000); connection.setReadTimeout(5000);
        connection.setRequestProperty("User-Agent","LivingRoomTV/0.9.2");
        try {
            if(connection.getResponseCode()!=200 || !"https".equals(connection.getURL().getProtocol()))
                throw new java.io.IOException("Poster source unavailable");
            try(InputStream input=connection.getInputStream()) { return JellyfinApi.readBounded(input,limit); }
        } finally { connection.disconnect(); }
    }
}
