package com.xsoulp.livingroom;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.json.*;

/** Bounded app-private startup cache. Filenames are hashes; credentials are never serialized. */
final class ShelfCache {
    private static final int MAX_BYTES=1024*1024, MAX_FILES=128;
    private final File directory;
    ShelfCache(File directory) { this.directory=directory; }
    static String hash(String value) {
        try {
            byte[] digest=MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder out=new StringBuilder();
            for(byte b:digest)out.append(String.format(Locale.ROOT,"%02x",b&255));
            return out.toString();
        } catch(Exception e) { throw new IllegalStateException(e); }
    }
    synchronized byte[] read(String key) {
        File file=new File(directory,hash(key));
        if(!file.isFile() || file.length()>MAX_BYTES)return null;
        try(InputStream in=new FileInputStream(file);ByteArrayOutputStream out=new ByteArrayOutputStream()) {
            byte[] buffer=new byte[4096];int n;
            while((n=in.read(buffer))!=-1) {
                if(out.size()+n>MAX_BYTES)return null;
                out.write(buffer,0,n);
            }
            file.setLastModified(System.currentTimeMillis());
            return out.toByteArray();
        } catch(IOException e) { return null; }
    }
    synchronized void write(String key,byte[] bytes) {
        if(bytes==null || bytes.length>MAX_BYTES)return;
        if(!directory.isDirectory() && !directory.mkdirs())return;
        File target=new File(directory,hash(key)),temp=new File(directory,hash(key)+".tmp");
        try {
            try(OutputStream out=new FileOutputStream(temp)) { out.write(bytes); }
            if(!temp.renameTo(target))temp.delete();
            File[] files=directory.listFiles();
            if(files!=null && files.length>MAX_FILES) {
                Arrays.sort(files,(a,b) -> Long.compare(a.lastModified(),b.lastModified()));
                for(int i=0;i<files.length-MAX_FILES;i++)files[i].delete();
            }
        } catch(IOException e) { temp.delete(); }
    }
    synchronized void clear() {
        File[] files=directory.listFiles();
        if(files!=null)for(File file:files)file.delete();
    }
    List<ShelfItem> shelves(String scope,int count) {
        byte[] bytes=read("shelves:"+scope);
        if(bytes==null)return Collections.emptyList();
        try {
            JSONArray rows=new JSONArray(new String(bytes,StandardCharsets.UTF_8));
            if(rows.length()!=count)return Collections.emptyList();
            List<ShelfItem> result=new ArrayList<>();
            for(int i=0;i<rows.length();i++) {
                JSONObject r=rows.getJSONObject(i);
                ShelfItem item=new ShelfItem(r.getInt("kind"),r.getString("key"),r.getString("title"),
                    r.getString("subtitle"),r.getString("package"),r.getString("id"),r.getString("image"),
                    r.getString("intent"),r.getLong("position"),r.getLong("duration"),r.getString("overview"));
                item.sortDate=r.optString("date");result.add(item);
            }
            return result;
        } catch(Exception e) { return Collections.emptyList(); }
    }
    void saveShelves(String scope,List<ShelfItem> items) {
        try {
            JSONArray rows=new JSONArray();
            for(ShelfItem i:items)rows.put(new JSONObject().put("kind",i.kind).put("key",i.key)
                .put("title",i.title).put("subtitle",i.subtitle).put("package",i.packageName)
                .put("id",i.itemId).put("image",i.imagePath).put("intent",i.intentUri)
                .put("position",i.positionMs).put("duration",i.durationMs).put("overview",i.overview).put("date",i.sortDate));
            write("shelves:"+scope,rows.toString().getBytes(StandardCharsets.UTF_8));
        } catch(JSONException ignored) { /* Cache failures must not hide live content. */ }
    }
}
