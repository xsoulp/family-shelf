package com.xsoulp.livingroom;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/** Small bounded HTTP client. Requests run only on worker threads; credentials stay in headers. */
final class JellyfinApi {
    final String server, token, userId;
    private final String deviceId;
    static final class ApiException extends IOException {
        final int status;
        ApiException(int status) { super("Jellyfin returned HTTP "+status); this.status=status; }
    }
    JellyfinApi(String server,String token,String userId,String deviceId) {
        this.server=normalize(server); this.token=token; this.userId=userId; this.deviceId=deviceId;
    }
    static String normalize(String address) {
        try {
            URI uri=new URI(address.trim());
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost()==null || uri.getUserInfo()!=null || uri.getQuery()!=null || uri.getFragment()!=null)
                throw new IllegalArgumentException();
            String clean=uri.toASCIIString();
            while(clean.endsWith("/")) clean=clean.substring(0,clean.length()-1);
            return clean+"/";
        } catch (Exception e) { throw new IllegalArgumentException("Enter an http:// or https:// server URL"); }
    }
    static String encode(String value) { try { return URLEncoder.encode(value,"UTF-8"); } catch(Exception e) { throw new AssertionError(e); } }
    private String authorization() {
        return "MediaBrowser Client=\"Family Shelf\", Device=\"Android TV room\", DeviceId=\""
            +headerValue(deviceId)+"\", Version=\"0.15.2\""
            +(token.isEmpty()?"":", Token=\""+headerValue(token)+"\"");
    }
    private static String headerValue(String value) {
        if(value.indexOf('\r')>=0 || value.indexOf('\n')>=0)
            throw new IllegalArgumentException("Invalid authentication header value");
        return value.replace("\\","\\\\").replace("\"","\\\"");
    }
    private JSONObject userResult(String current,String legacy,String query) throws Exception {
        try { return json("GET",current+query,null); }
        catch(ApiException e) {
            // Authentication and permission failures must not masquerade as missing routes.
            if(e.status!=404) throw e;
            return json("GET",legacy+query,null);
        }
    }
    byte[] request(String method,String path,JSONObject data,int maxBytes) throws Exception {
        if (path.startsWith("/") || path.contains("://")) throw new IOException("Invalid server-relative path");
        HttpURLConnection connection=(HttpURLConnection)new URL(server+path).openConnection();
        connection.setConnectTimeout(7000); connection.setReadTimeout(9000);
        connection.setInstanceFollowRedirects(false);
        connection.setRequestMethod(method);
        connection.setRequestProperty("Authorization",authorization());
        if (!token.isEmpty()) connection.setRequestProperty("X-Emby-Token",token);
        connection.setRequestProperty("Accept","application/json");
        try {
            if (data!=null) {
                byte[] bytes=data.toString().getBytes(StandardCharsets.UTF_8);
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type","application/json; charset=utf-8");
                connection.setFixedLengthStreamingMode(bytes.length);
                try(java.io.OutputStream output=connection.getOutputStream()) { output.write(bytes); }
            }
            int status=connection.getResponseCode();
            if (status<200 || status>=300) throw new ApiException(status);
            try(InputStream input=connection.getInputStream()) { return readBounded(input,maxBytes); }
        } finally { connection.disconnect(); }
    }
    static byte[] readBounded(InputStream input,int limit) throws IOException {
        ByteArrayOutputStream output=new ByteArrayOutputStream();
        byte[] buffer=new byte[4096]; int read;
        while((read=input.read(buffer))!=-1) {
            if(output.size()+read>limit) throw new IOException("Response exceeded size limit");
            output.write(buffer,0,read);
        }
        return output.toByteArray();
    }
    JSONObject json(String method,String path,JSONObject body) throws Exception {
        return new JSONObject(new String(request(method,path,body,2*1024*1024),StandardCharsets.UTF_8));
    }
    JSONObject initiate() throws Exception { return json("POST","QuickConnect/Initiate",new JSONObject()); }
    boolean authorized(String secret) throws Exception {
        return json("GET","QuickConnect/Connect?Secret="+encode(secret),null).optBoolean("Authenticated");
    }
    JSONObject authenticate(String secret) throws Exception {
        return json("POST","Users/AuthenticateWithQuickConnect",new JSONObject().put("Secret",secret));
    }
    List<ShelfItem> resume() throws Exception { return resume("Movie,Episode"); }
    List<ShelfItem> resumeMovies() throws Exception { return resume("Movie"); }
    private List<ShelfItem> resume(String types) throws Exception {
        if (token.isEmpty() || !ShelfItem.validId(userId)) throw new IOException("Connect Jellyfin first");
        String query="?userId="+encode(userId)+"&limit=12&mediaTypes=Video&includeItemTypes="+types
            +"&enableUserData=true&enableImages=true&imageTypeLimit=1&fields=PrimaryImageAspectRatio,Overview";
        JSONObject result=userResult("UserItems/Resume","Users/"+userId+"/Items/Resume",query);
        JSONArray rows=result.optJSONArray("Items");
        if(rows==null) throw new IOException("Invalid resume response");
        List<ShelfItem> items=new ArrayList<>();
        java.util.HashSet<String> ids=new java.util.HashSet<>();
        for(int i=0;i<rows.length() && items.size()<12;i++) {
            JSONObject row=rows.optJSONObject(i); if(row==null) continue;
            ShelfItem item=ShelfItem.fromJellyfin(row);
            if(item!=null && ids.add(item.key)) items.add(item);
        }
        return items;
    }
    JSONArray libraries() throws Exception {
        JSONArray rows=userResult("UserViews","Users/"+userId+"/Views",
            "?userId="+encode(userId)).optJSONArray("Items");
        if(rows==null) throw new IOException("Invalid libraries response");
        return rows;
    }
    List<ShelfItem> shelf(String parentId,boolean ongoing,boolean series) throws Exception {
        if(!ShelfItem.validId(parentId)) throw new IOException("Choose a library first");
        String query="?userId="+encode(userId)+"&parentId="+encode(parentId)
            +"&recursive=true&limit=40&enableUserData=true&enableImages=true&imageTypeLimit=1&fields=Overview,DateCreated"
            +"&includeItemTypes="+(series?(ongoing?"Episode":"Series"):"Movie")
            +"&sortBy="+(ongoing?"DatePlayed":"DateCreated")+"&sortOrder=Descending"
            +(ongoing?"&filters=IsResumable":"");
        JSONObject response=ongoing?
            userResult("UserItems/Resume","Users/"+userId+"/Items/Resume",query):
            json("GET","Items"+query,null);
        JSONArray rows=response.optJSONArray("Items");
        if(rows==null) throw new IOException("Invalid shelf response");
        List<ShelfItem> result=new ArrayList<>();
        java.util.HashSet<String> identities=new java.util.HashSet<>();
        for(int i=0;i<rows.length();i++) {
            JSONObject row=rows.optJSONObject(i); if(row==null) continue;
            ShelfItem item;
            if(ongoing) item=ShelfItem.fromJellyfin(row);
            else {
                LatestItem latest=LatestItem.fromJson(row);
                item=latest==null?null:new ShelfItem(ShelfItem.JELLYFIN,"latest:"+latest.id,latest.title,
                    series?"Series · Recently added":"Movie · Recently added",
                    "org.jellyfin.androidtv",latest.id,latest.imagePath,"",0,0);
            }
            if(item!=null) {
                if(!ongoing) item.sortDate=row.optString("DateCreated");
                else if(item.sortDate.isEmpty()) item.sortDate=row.optString("DateLastSaved");
            }
            String identity=series && ongoing?row.optString("SeriesId",row.optString("Id")):row.optString("Id");
            if(item!=null && identities.add(identity)) result.add(item);
        }
        return result;
    }
    List<ShelfItem> randomBooks(String parentId) throws Exception {
        if(!ShelfItem.validId(parentId)) throw new IOException("Invalid books library");
        JSONArray rows=json("GET","Items?userId="+encode(userId)+"&parentId="+encode(parentId)
            +"&recursive=true&filters=IsNotFolder&sortBy=Random&limit=8&enableUserData=true"
            +"&enableImages=true&imageTypeLimit=1&fields=Overview",null).optJSONArray("Items");
        if(rows==null) throw new IOException("Invalid books response");
        List<ShelfItem> result=new ArrayList<>();
        java.util.HashSet<String> ids=new java.util.HashSet<>();
        for(int i=0;i<rows.length() && result.size()<8;i++) {
            JSONObject row=rows.optJSONObject(i);
            if(row==null || row.optBoolean("IsFolder") || !ShelfItem.validId(row.optString("Id"))
                || row.optString("Name").trim().isEmpty() || !ids.add(row.optString("Id"))) continue;
            String id=row.optString("Id"),imageId=id;
            JSONObject tags=row.optJSONObject("ImageTags");
            if((tags==null || tags.optString("Primary").isEmpty()) && ShelfItem.validId(row.optString("ParentPrimaryImageItemId")))
                imageId=row.optString("ParentPrimaryImageItemId");
            JSONObject userData=row.optJSONObject("UserData");
            long duration=Math.max(0,row.optLong("RunTimeTicks"))/10000;
            long position=userData==null?0:Math.max(0,userData.optLong("PlaybackPositionTicks"))/10000;
            if(userData!=null && userData.optBoolean("Played")) position=duration;
            position=Math.min(position,duration);
            result.add(new ShelfItem(ShelfItem.JELLYFIN,"book:"+id,row.optString("Name"),
                "Books & Stories · Jellyfin", "org.jellyfin.androidtv",id,
                "Items/"+imageId+"/Images/Primary?maxWidth=480&quality=85","",position,duration,
                row.optString("Overview").replaceAll("<[^>]*>"," ").trim()));
        }
        return result;
    }
    List<ShelfItem> disneyMovies(java.util.Set<String> parents) throws Exception {
        ShelfItem[] selected=new ShelfItem[DisneyCatalog.TITLES.length];
        for(String parent:parents) {
            if(!ShelfItem.validId(parent)) continue;
            for(int offset=0;offset<10000;offset+=100) {
                JSONArray rows=json("GET","Items?userId="+encode(userId)+"&parentId="+encode(parent)
                    +"&recursive=true&includeItemTypes=Movie&limit=100&startIndex="+offset
                    +"&sortBy=SortName&sortOrder=Ascending&enableUserData=true&enableImages=true&imageTypeLimit=1"
                    +"&fields=Overview,OriginalTitle",null).optJSONArray("Items");
                if(rows==null) throw new IOException("Invalid Disney movies response");
                for(int i=0;i<rows.length();i++) {
                    JSONObject row=rows.optJSONObject(i);
                    if(row==null || !"Movie".equals(row.optString("Type")) || !ShelfItem.validId(row.optString("Id"))) continue;
                    int slot=DisneyCatalog.slot(row.optString("Name"),row.optString("OriginalTitle"),row.optInt("ProductionYear"));
                    if(slot<0 || selected[slot]!=null) continue;
                    String id=row.optString("Id");
                    JSONObject userData=row.optJSONObject("UserData");
                    long duration=Math.max(0,row.optLong("RunTimeTicks"))/10000;
                    long position=userData==null?0:Math.max(0,userData.optLong("PlaybackPositionTicks"))/10000;
                    if(userData!=null && userData.optBoolean("Played")) position=duration;
                    selected[slot]=new ShelfItem(ShelfItem.JELLYFIN,"disney:"+id,row.optString("Name"),
                        "Disney Library · Jellyfin","org.jellyfin.androidtv",id,
                        "Items/"+id+"/Images/Primary?maxWidth=480&quality=85","",Math.min(position,duration),duration,
                        row.optString("Overview").replaceAll("<[^>]*>"," ").trim());
                }
                if(rows.length()<100) break;
                if(offset==9900) throw new IOException("Kids library exceeds scan limit");
            }
        }
        return java.util.Arrays.asList(selected);
    }
    Bitmap poster(ShelfItem item) throws Exception {
        byte[] bytes=request("GET",item.imagePath,null,1024*1024);
        return decodePoster(bytes);
    }
    List<LatestItem> latest() throws Exception {
        if(token.isEmpty() || !ShelfItem.validId(userId)) throw new IOException("Connect Jellyfin first");
        String query="?userId="+encode(userId)+"&limit=2&includeItemTypes=Movie,Episode,Series"
            +"&groupItems=true&enableImages=true&imageTypeLimit=1";
        byte[] bytes;
        try { bytes=request("GET","Items/Latest"+query,null,2*1024*1024); }
        catch(ApiException e) {
            if(e.status!=404) throw e;
            bytes=request("GET","Users/"+userId+"/Items/Latest"+query,null,2*1024*1024);
        }
        JSONArray rows=new JSONArray(new String(bytes,StandardCharsets.UTF_8));
        List<LatestItem> result=new ArrayList<>();
        java.util.HashSet<String> ids=new java.util.HashSet<>();
        for(int i=0;i<rows.length() && result.size()<2;i++) {
            JSONObject row=rows.optJSONObject(i);
            LatestItem item=row==null?null:LatestItem.fromJson(row);
            if(item!=null && ids.add(item.id)) result.add(item);
        }
        return result;
    }
    Bitmap latestPoster(LatestItem item) throws Exception {
        return decodePoster(request("GET",item.imagePath,null,1024*1024));
    }
    static Bitmap decodePoster(byte[] bytes) {
        BitmapFactory.Options options=new BitmapFactory.Options(); options.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(options.outWidth<=0 || options.outHeight<=0) return null;
        options.inSampleSize=1;
        while(options.outWidth/options.inSampleSize>512 || options.outHeight/options.inSampleSize>768)
            options.inSampleSize*=2;
        options.inJustDecodeBounds=false;
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
    }
    void logout() throws Exception { request("POST","Sessions/Logout",new JSONObject(),4096); }
}
