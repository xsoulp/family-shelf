package com.xsoulp.livingroom;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Instrumentation;
import android.content.Intent;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.KeyEvent;
import java.io.ByteArrayInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import org.json.JSONObject;
import org.json.JSONArray;

/** Dependency-free device smoke tests. Fixture records never enter the production shelf. */
public final class RoomSmokeInstrumentation extends Instrumentation {
    private int checks;
    private volatile MainActivity resumedMain;
    @Override public void callActivityOnResume(Activity activity) {
        super.callActivityOnResume(activity);
        if(activity instanceof MainActivity)resumedMain=(MainActivity)activity;
    }
    private String mode;
    private String itemFilter;
    private void check(boolean condition,String description) {
        if(!condition) throw new AssertionError(description);
        checks++;
    }
    @Override public void onCreate(Bundle arguments) {
        super.onCreate(arguments);
        mode=arguments==null?"smoke":arguments.getString("mode","smoke");
        itemFilter=arguments==null?"":arguments.getString("itemId","");
        start();
    }
    @Override public void onStart() {
        Bundle result=new Bundle();
        try {
            if("aircraft-art".equals(mode)) {
                aircraftArt();result.putString("stream","OK ("+checks+" checks): off-screen manga aircraft styles\n");
                finish(Activity.RESULT_OK,result);return;
            }
            if("outdoor".equals(mode)) {
                outdoorChecks();result.putString("stream","OK ("+checks+" checks): outdoor screensaver, wake input, day/night and foreground lifecycle\n");
                finish(Activity.RESULT_OK,result);return;
            }
            if("movies-kids".equals(mode)) {
                JellyfinStore store=new JellyfinStore(getTargetContext());
                if(store.token().isEmpty()) throw new IllegalStateException("Connect Jellyfin first");
                JellyfinApi api=new JellyfinApi(store.server(),store.token(),store.userId(),store.deviceId());
                JSONArray libraries=api.libraries(), movies=new JSONArray();
                String parent="", libraryName="";
                for(int i=0;i<libraries.length();i++) {
                    JSONObject library=libraries.getJSONObject(i);
                    if(DisneyCatalog.normalize(library.optString("Name")).equals("movieskids")) {
                        parent=library.getString("Id"); libraryName=library.getString("Name"); break;
                    }
                }
                if(parent.isEmpty()) throw new IllegalStateException("Movies (kids) library not found");
                int total=0;
                for(int offset=0;offset<100000;offset+=100) {
                    JSONObject response=api.json("GET","Items?userId="+JellyfinApi.encode(store.userId())
                        +"&parentId="+JellyfinApi.encode(parent)+"&recursive=true&includeItemTypes=Movie"
                        +"&sortBy=SortName&sortOrder=Ascending&limit=100&startIndex="+offset
                        +"&fields=OriginalTitle&enableImages=false&enableUserData=false",null);
                    JSONArray rows=response.getJSONArray("Items");
                    total=response.optInt("TotalRecordCount",total);
                    for(int i=0;i<rows.length();i++) {
                        JSONObject row=rows.getJSONObject(i);
                        movies.put(new JSONObject().put("title",row.optString("Name"))
                            .put("originalTitle",row.optString("OriginalTitle")).put("year",row.optInt("ProductionYear")));
                    }
                    if(rows.length()<100) break;
                }
                JSONObject report=new JSONObject().put("library",libraryName).put("total",total).put("movies",movies);
                try(java.io.FileOutputStream file=getTargetContext().openFileOutput("movies-kids-inventory.json",0)) {
                    file.write(report.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                result.putString("stream",libraryName+": "+movies.length()+" movies, server total "+total+"\n");
                finish(Activity.RESULT_OK,result); return;
            }
            if("playback".equals(mode)) {
                result.putString("stream",new JellyfinPlaybackProbe(this,itemFilter).run());
                finish(Activity.RESULT_OK,result); return;
            }
            if("netflix".equals(mode)) {
                StringBuilder dump=new StringBuilder();
                for(String table:new String[]{"channel","preview_program","watch_next_program"}) {
                    try(android.database.Cursor cursor=getTargetContext().getContentResolver().query(
                        android.net.Uri.parse("content://android.media.tv/"+table),null,null,null,null)) {
                        if(cursor==null) continue;
                        int owner=cursor.getColumnIndex("package_name");
                        while(cursor.moveToNext()) {
                            if(owner<0 || !cursor.getString(owner).contains("netflix")) continue;
                            dump.append(table).append(": ");
                            for(String column:new String[]{"_id","display_name","title","type","watch_next_type","channel_id","episode_title","season_display_number","episode_display_number","poster_art_uri","thumbnail_uri","poster_art_aspect_ratio","thumbnail_aspect_ratio"}) {
                                int col=cursor.getColumnIndex(column);
                                if(col>=0) dump.append(column).append("=").append(cursor.getString(col)).append("; ");
                            }
                            dump.append("\n");
                        }
                    } catch(Exception e) { dump.append(table).append(": ").append(e.getClass().getSimpleName()).append("\n"); }
                }
                try(java.io.FileOutputStream file=getTargetContext().openFileOutput("netflix-shared.txt",0)) { file.write(dump.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
                result.putString("stream",dump.toString()); finish(Activity.RESULT_OK,result); return;
            }
            if("posters".equals(mode)) {
                modelChecks();
                PublicPosterReader reader=new PublicPosterReader(getTargetContext());
                for(String[] entry:new String[][]{{"Eat Pray Bark","movie"},{"Everwood","series"},{"Running Point","series"}}) {
                    android.graphics.Bitmap poster=reader.poster(entry[0],entry[1]);
                    check(poster!=null && poster.getHeight()>poster.getWidth() && poster.getWidth()>=192,"Public portrait poster decodes for "+entry[0]);
                    if(poster!=null) poster.recycle();
                }
                result.putString("stream","OK ("+checks+" checks): public posters for all three adult titles\n");
                finish(Activity.RESULT_OK,result); return;
            }
            if("remote-buttons".equals(mode)) {
                check(RemoteButtons.target(KeyEvent.KEYCODE_BUTTON_6)==2,"Netflix remote key maps to Netflix");
                check(RemoteButtons.target(KeyEvent.KEYCODE_BUTTON_7)==1,"Prime Video remote key maps to Jellyfin");
                check(RemoteButtons.target(KeyEvent.KEYCODE_BUTTON_3)==0,"YouTube remote key maps to SmartTube");
                for(int key:new int[]{KeyEvent.KEYCODE_HOME,KeyEvent.KEYCODE_BACK,KeyEvent.KEYCODE_DPAD_CENTER,
                    KeyEvent.KEYCODE_DPAD_LEFT,KeyEvent.KEYCODE_DPAD_RIGHT,KeyEvent.KEYCODE_VOLUME_UP,KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE})
                    check(RemoteButtons.target(key)==-1,"Other remote keys are not remapped");
                Intent netflix=RemoteButtons.launchIntent(getTargetContext(),2);
                Intent jellyfin=RemoteButtons.launchIntent(getTargetContext(),1);
                Intent smarttube=RemoteButtons.launchIntent(getTargetContext(),0);
                check(netflix!=null && netflix.getComponent()!=null && netflix.getComponent().getPackageName().contains("netflix"),"Installed Netflix launch intent resolves");
                check(jellyfin!=null && jellyfin.getComponent()!=null && jellyfin.getComponent().getPackageName().contains("jellyfin"),"Installed Jellyfin launch intent resolves");
                check(smarttube!=null && smarttube.getComponent()!=null && WatchNextReader.supported(smarttube.getComponent().getPackageName())
                    && !smarttube.getComponent().getPackageName().contains("netflix"),"Installed SmartTube launch intent resolves");
                android.view.accessibility.AccessibilityManager manager=(android.view.accessibility.AccessibilityManager)getTargetContext().getSystemService(android.content.Context.ACCESSIBILITY_SERVICE);
                boolean found=false;
                for(android.accessibilityservice.AccessibilityServiceInfo service:manager.getInstalledAccessibilityServiceList()) {
                    if(!service.getId().contains("com.xsoulp.livingroom")) continue;
                    found=true;
                    check((service.flags&android.accessibilityservice.AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS)!=0,"Remote service requests key filtering");
                    check((service.getCapabilities()&android.accessibilityservice.AccessibilityServiceInfo.CAPABILITY_CAN_RETRIEVE_WINDOW_CONTENT)==0,"Remote service cannot retrieve screen contents");
                }
                check(found,"Global remote-button service is installed");
                result.putString("stream","OK ("+checks+" checks): Netflix, Prime Video and YouTube remote mapping\n");
                finish(Activity.RESULT_OK,result); return;
            }
            modelChecks();
            ShelfRepository.Snapshot stories=new ShelfRepository(getTargetContext()).loadStories();
            check(stories.items.size()==8,"Stories room reserves eight books");
            java.util.HashSet<String> bookIds=new java.util.HashSet<>();
            StringBuilder booksReport=new StringBuilder(stories.status+"\n");
            for(ShelfItem book:stories.items) {
                if(book.kind==ShelfItem.JELLYFIN) {
                    check(book.key.startsWith("book:") && bookIds.add(book.itemId),"Random books are unique Jellyfin items");
                    if("stories".equals(mode)) {
                        android.graphics.Bitmap poster=stories.api.poster(book);
                        check(poster!=null,"Real book artwork decodes");
                        if(poster!=null) poster.recycle();
                    }
                }
                booksReport.append(book.title).append(" | ").append(book.subtitle).append("\n");
            }
            try(java.io.FileOutputStream file=getTargetContext().openFileOutput("stories-validation.txt",0)) { file.write(booksReport.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8)); }
            if("stories".equals(mode)) check(bookIds.size()==8,"Live stories library supplies eight real books");
            ShelfRepository.Snapshot disney=new ShelfRepository(getTargetContext()).loadDisney();
            check(disney.items.size()==12,"Disney Media reserves all twelve photo positions");
            if("disney-corrections".equals(mode)) {
                check(disney.items.get(1).kind==ShelfItem.JELLYFIN,"Confirmed Sleeping Beauty filename loads from live Jellyfin");
                check(disney.items.get(6).kind==ShelfItem.JELLYFIN,"Snow White with 1938 metadata loads from live Jellyfin");
            }
            java.util.HashSet<String> disneyIds=new java.util.HashSet<>();
            StringBuilder disneyReport=new StringBuilder(disney.status+"\n");
            for(ShelfItem item:disney.items) {
                check(item.kind!=ShelfItem.WATCH_NEXT && item.packageName.equals("org.jellyfin.androidtv"),"Disney positions use Jellyfin exclusively");
                if(item.kind==ShelfItem.JELLYFIN) {
                    check(disneyIds.add(item.itemId),"Disney movie identities are unique");
                    if("disney".equals(mode)) {
                        android.graphics.Bitmap poster=disney.api.poster(item);
                        check(poster!=null,"Live Disney movie artwork decodes");
                        if(poster!=null) poster.recycle();
                    }
                }
                disneyReport.append(item.title).append(" | ").append(item.subtitle).append(" | ").append(item.itemId).append("\n");
            }
            try(java.io.FileOutputStream file=getTargetContext().openFileOutput("disney-validation.txt",0)) {
                file.write(disneyReport.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            if("disney".equals(mode)) check(disneyIds.size()==12,"All twelve photo titles exist in the live kids libraries");
            ShelfRepository.Snapshot grouped=new ShelfRepository(getTargetContext()).load();
            check(grouped.items.size()==8,"Four books per shelf");
            ShelfRepository.Snapshot kids=new ShelfRepository(getTargetContext()).load(true);
            check(kids.items.size()==8,"Children also have eight reserved positions");
            for(ShelfRepository.Snapshot snapshot:java.util.Arrays.asList(grouped,kids)) {
                for(int i=0;i<8;i++) {
                    ShelfItem item=snapshot.items.get(i);
                    if(item.kind==ShelfItem.JELLYFIN) {
                        check(i<4?item.positionMs>0:item.positionMs==0,"Ongoing and latest use separate data");
                        check(i%4<2?item.subtitle.startsWith("Movie")||item.subtitle.startsWith("Filme"):
                            !item.subtitle.startsWith("Movie")&&!item.subtitle.startsWith("Filme"),"Two films and two series per shelf");
                    }
                }
            }
            for(ShelfRepository.Snapshot snapshot:java.util.Arrays.asList(grouped,kids)) {
                NetflixShelfStore netflix=new NetflixShelfStore(getTargetContext());
                for(int slot:new int[]{1,3}) {
                    ShelfItem item=snapshot.items.get(slot);
                    check(item.kind==ShelfItem.WATCH_NEXT?netflix.belongsTo(item.title,snapshot==kids):
                        item.packageName.contains("jellyfin"),"Optional Netflix positions use assigned view records or Jellyfin");
                    if(item.kind==ShelfItem.WATCH_NEXT) {
                        ShelfRepository.Snapshot offline=fallbackFixture();
                        netflix.apply(offline,java.util.Collections.emptyList(),snapshot==kids,slot);
                        check(offline.items.get(slot).title.equals(item.title)
                            && offline.items.get(slot).positionMs==item.positionMs
                            && offline.items.get(slot).intentUri.equals(item.intentUri),"Live view metadata survives an empty provider feed");
                    } else if(item.kind==ShelfItem.JELLYFIN) {
                        check(!item.key.equals(snapshot.items.get(slot-1).key),"Jellyfin fallback does not duplicate its neighbouring title");
                    }
                }
                check(snapshot.items.get(0).packageName.contains("jellyfin") && snapshot.items.get(2).packageName.contains("jellyfin"),"First film and series remain Jellyfin");
            }
            StringBuilder shelves=new StringBuilder();
            for(ShelfRepository.Snapshot snapshot:java.util.Arrays.asList(grouped,kids)) {
                shelves.append(snapshot==kids?"Kids: ":"Adults: ").append(snapshot.status).append("\n");
                for(ShelfItem item:snapshot.items) shelves.append(item.title).append(" | ").append(item.subtitle).append("\n");
            }
            result.putString("shelves",shelves.toString());
            WatchNextReader.Result watch=WatchNextReader.read(getTargetContext());
            for(ShelfItem item:watch.items) check(item.kind==ShelfItem.WATCH_NEXT && WatchNextReader.supported(item.packageName),"Watch Next owner allowlist");
            result.putString("watch_next","Visible rows="+watch.visibleRows+", usable="+watch.items.size()+"; "+watch.status);
            JSONArray shared=new JSONArray();
            for(ShelfItem item:watch.items) shared.put(new JSONObject().put("package",item.packageName)
                .put("title",item.title).put("positionMs",item.positionMs).put("durationMs",item.durationMs));
            try(java.io.FileOutputStream file=getTargetContext().openFileOutput("watch-next-validation.json",0)) {
                file.write(shared.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            if("live".equals(mode)) {
                JellyfinStore store=new JellyfinStore(getTargetContext());
                check(!store.token().isEmpty(),"Jellyfin must be paired before live checks");
                JellyfinApi api=new JellyfinApi(store.server(),store.token(),store.userId(),store.deviceId());
                List<ShelfItem> rows=api.resume();
                JSONArray inventory=new JSONArray();
                int images=0;
                for(ShelfItem item:rows) {
                    inventory.put(new JSONObject().put("id",item.itemId).put("title",item.title)
                        .put("positionMs",item.positionMs).put("durationMs",item.durationMs));
                    check(item.positionMs>0 && item.positionMs<item.durationMs,"Real item is unfinished");
                    android.graphics.Bitmap bitmap=api.poster(item);
                    if(bitmap!=null) { images++; bitmap.recycle(); }
                }
                result.putString("jellyfin","Real unfinished items="+rows.size()+", decoded posters="+images);
                List<LatestItem> latest=api.latest();
                check(latest.size()<=2,"Latest additions bounded to two items");
                JSONArray additions=new JSONArray();
                for(LatestItem item:latest) {
                    check(ShelfItem.validId(item.id) && !item.title.isEmpty(),"Latest addition has a real ID and title");
                    android.graphics.Bitmap image=api.latestPoster(item);
                    additions.put(new JSONObject().put("id",item.id).put("title",item.title).put("posterDecoded",image!=null));
                    if(image!=null) image.recycle();
                }
                try(java.io.FileOutputStream file=getTargetContext().openFileOutput("latest-validation.json",0)) {
                    file.write(additions.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
                result.putString("latest","Real Jellyfin latest additions="+latest.size());
                try(java.io.FileOutputStream file=getTargetContext().openFileOutput("live-items.json",0)) {
                    file.write(inventory.toString(2).getBytes(java.nio.charset.StandardCharsets.UTF_8));
                }
            }
            try(java.io.FileOutputStream file=getTargetContext().openFileOutput("shelves-validation.txt",0)) {
                file.write(shelves.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                JellyfinStore store=new JellyfinStore(getTargetContext());
                org.json.JSONArray libraries=new JellyfinApi(store.server(),store.token(),store.userId(),store.deviceId()).libraries();
                for(int i=0;i<libraries.length();i++) file.write(("Library: "+libraries.getJSONObject(i).optString("Name")+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            uiChecks();
            result.putString("stream","OK ("+checks+" checks)\n");
            finish(Activity.RESULT_OK,result);
        } catch(Throwable error) {
            result.putString("stream","FAIL: "+error.getClass().getSimpleName()+": "+error.getMessage()+"\n");
            finish(Activity.RESULT_CANCELED,result);
        }
    }
    private void modelChecks() throws Exception {
        JSONArray covers=new JSONArray("[{\"id\":\"tt1\",\"name\":\"Everwood\",\"type\":\"series\",\"poster\":\"https://example.com/cover.jpg\"}]");
        check(PublicPosterReader.match(covers,"Everwood","series").endsWith("cover.jpg"),"Exact public artwork identity matches");
        check(PublicPosterReader.match(covers,"Everwood","movie").isEmpty(),"Public artwork cannot cross film and series types");
        check(PublicPosterReader.match(covers,"Other title","series").isEmpty(),"Fuzzy public search results cannot replace the confirmed title");
        covers.put(new JSONObject("{\"id\":\"tt2\",\"name\":\"Everwood\",\"type\":\"series\",\"poster\":\"https://example.com/remake.jpg\"}"));
        check(PublicPosterReader.match(covers,"Everwood","series").isEmpty(),"Ambiguous remake artwork is rejected");
        check(!ShelfRepository.netflixKey(false,1).equals(ShelfRepository.netflixKey(true,1)),"Netflix choices stay separate for adults and children");
        check(!ShelfRepository.netflixKey(false,1).equals(ShelfRepository.netflixKey(false,3)),"Netflix film and series choices stay separate");
        netflixFallbackChecks();
        check(DisneyCatalog.slot("A Bela e o Monstro","",1991)==2,"Portuguese photo titles match their slots");
        check(DisneyCatalog.slot("Other localized name","Tangled",2010)==11,"Original titles match localized movies");
        check(DisneyCatalog.slot("A Bela e o Monstro","Beauty and the Beast",2017)==-1,"Live action remakes cannot replace animated originals");
        check(DisneyCatalog.slot("Moana 2","",2024)==-1,"Sequels cannot replace photo titles");
        check(DisneyCatalog.slot("Snow White and the Seven Dwarfs","",1938)==6,"1938 Snow White uses the same animation slot");
        check(DisneyCatalog.slot("A Bela Adormecida.ia","",0)==1,"Confirmed Sleeping Beauty filename needs no year");
        check(DisneyCatalog.slot("A Bela Adormecida.ia","",2016)==-1,"Filename exception does not accept a different release year");
        check(DisneyCatalog.slot("A Maldição da Bela Adormecida","The Curse of Sleeping Beauty",2016)==-1,"Different Sleeping Beauty movie is excluded");
        check(DisneyCatalog.slot("Snow White","",2025)==-1,"Snow White remake is still excluded");
        check(DisneyCatalog.slot("BRANCA DE NEVE E OS SETE ANOES","",1937)==6,"Accents and case do not affect title matching");

        JSONObject movie=new JSONObject("{\"Id\":\"0123456789abcdef0123456789abcdef\",\"Type\":\"Movie\","
            +"\"Name\":\"Test fixture only\",\"RunTimeTicks\":6000000000,\"UserData\":{\"PlaybackPositionTicks\":3000000000,\"Played\":false}}");
        ShelfItem row=ShelfItem.fromJellyfin(movie);
        check(row!=null && row.positionMs==300000 && row.durationMs==600000,"100ns ticks convert to milliseconds");
        check(Math.abs(row.progress()-.5f)<.001f,"Progress ratio");
        movie.getJSONObject("UserData").put("Played",true);
        check(ShelfItem.fromJellyfin(movie)==null,"Finished movies excluded");
        movie.getJSONObject("UserData").put("Played",false).put("PlaybackPositionTicks",0);
        check(ShelfItem.fromJellyfin(movie)==null,"Unstarted movies excluded");
        movie.getJSONObject("UserData").put("PlaybackPositionTicks",6000000000L);
        check(ShelfItem.fromJellyfin(movie)==null,"Completed positions excluded");
        movie.getJSONObject("UserData").put("PlaybackPositionTicks",3000000000L);
        movie.put("Type","Episode").put("SeriesName","Test series").put("ParentIndexNumber",2).put("IndexNumber",3);
        row=ShelfItem.fromJellyfin(movie);
        check(row!=null && row.title.equals("Test series") && row.subtitle.contains("S2 E3"),"Episode identity preserved");
        check(JellyfinApi.normalize("http://jellyfin.example:8096/jellyfin/").endsWith("/jellyfin/"),"Server base paths preserved");
        boolean rejected=false;
        try { JellyfinApi.normalize("https://name:secret@example.com/"); } catch(IllegalArgumentException e) { rejected=true; }
        check(rejected,"Credentials in server URL rejected");
        rejected=false;
        try { JellyfinApi.readBounded(new ByteArrayInputStream(new byte[12]),8); } catch(java.io.IOException e) { rejected=true; }
        check(rejected,"Oversized response rejected");
        movie.put("Overview","Actual description <b>from the server</b>");
        check(ShelfItem.fromJellyfin(movie).overview.contains("Actual description")
            && !ShelfItem.fromJellyfin(movie).overview.contains("<b>"),"Server descriptions become plain TV text");
        JSONObject addition=new JSONObject("{\"Id\":\"0123456789abcdef0123456789abcdef\",\"Type\":\"Series\",\"Name\":\"Fixture series\",\"ProductionYear\":2024}");
        check(LatestItem.fromJson(addition)!=null,"Latest series need no unfinished playback position");
        addition.put("Type","Audio");
        check(LatestItem.fromJson(addition)==null,"Non-video additions excluded");
    }
    private ShelfRepository.Snapshot fallbackFixture() {
        ShelfRepository.Snapshot snapshot=new ShelfRepository.Snapshot();
        for(int i=0;i<4;i++) snapshot.items.add(ShelfItem.shortcut("Jellyfin","org.jellyfin.androidtv",""));
        return snapshot;
    }
    private ShelfItem netflixFixture(String title,long position) {
        return new ShelfItem(ShelfItem.WATCH_NEXT,"wn:fixture:"+title,title,"Netflix · Watch Next",
            "com.netflix.ninja","","https://example.com/poster.jpg","intent://fixture#Intent;package=com.netflix.ninja;end",position,200);
    }
    private void netflixFallbackChecks() throws Exception {
        android.content.SharedPreferences room=getTargetContext().getSharedPreferences("netflix-test-room",0);
        android.content.SharedPreferences cache=getTargetContext().getSharedPreferences("netflix-test-cache",0);
        room.edit().clear().commit(); cache.edit().clear().commit();
        try {
            NetflixShelfStore store=new NetflixShelfStore(room,cache);
            check(!store.canAssign("Goat",false) && !store.canAssign("Everwood",true),"Confirmed kids and adult titles cannot be assigned across views");
            check(store.canAssign("Unassigned show",true) && !store.belongsTo("Unassigned show",true),"Unknown titles require an explicit assignment");
            ShelfItem movie=netflixFixture("Eat Pray Bark",100),series=netflixFixture("Everwood",70),goat=netflixFixture("Goat",50);
            ShelfRepository.Snapshot snapshot=fallbackFixture();
            ShelfItem fallback=snapshot.items.get(1);
            store.apply(snapshot,java.util.Collections.singletonList(goat),false,1);
            check(snapshot.items.get(1)==fallback,"Kids-only feed with no adult cache retains the original Jellyfin movie");
            store.apply(snapshot,java.util.Arrays.asList(goat,movie),false,1);
            store.apply(snapshot,java.util.Arrays.asList(goat,series),false,3);
            check(snapshot.items.get(1)==movie && snapshot.items.get(3)==series,"Mixed profile feed selects only confirmed adult movie and series");
            store=new NetflixShelfStore(room,cache);
            snapshot=fallbackFixture();
            store.apply(snapshot,java.util.Collections.singletonList(goat),false,1);
            store.apply(snapshot,java.util.Collections.emptyList(),false,3);
            check(snapshot.items.get(1).title.equals(movie.title) && snapshot.items.get(1).positionMs==100
                && snapshot.items.get(1).intentUri.equals(movie.intentUri),"Adult movie cache survives store recreation and a kids-only feed");
            check(snapshot.items.get(3).title.equals(series.title) && snapshot.items.get(3).subtitle.contains("Guardado"),"Empty feed restores cached series with its saved timestamp");
            snapshot=fallbackFixture();
            store.apply(snapshot,java.util.Collections.singletonList(movie),true,1);
            check(snapshot.items.get(1).packageName.contains("jellyfin"),"Adult cache is never reused in kids view");
            store.apply(snapshot,java.util.Arrays.asList(movie,goat),true,1);
            check(snapshot.items.get(1)==goat,"Confirmed Goat appears only in kids movie position");
            check(!store.select(true,3,series),"Selecting a confirmed adult show for children is rejected");
            check(store.select(false,3,netflixFixture("New adult show",80)),"Explicit selection assigns and caches an unknown adult show");
            check(!store.canAssign("New adult show",true),"Newly assigned titles disappear from the opposite view chooser");
            room.edit().putString(ShelfRepository.netflixKey(false,3),"Different show").commit();
            snapshot=fallbackFixture();
            store.apply(snapshot,java.util.Collections.emptyList(),false,3);
            check(snapshot.items.get(3).packageName.contains("jellyfin"),"Changing the selected title cannot restore the previous title cache");
            room.edit().putString(ShelfRepository.netflixKey(false,1),"Goat").commit();
            snapshot=fallbackFixture();
            store.apply(snapshot,java.util.Collections.singletonList(goat),false,1);
            check(snapshot.items.get(1).packageName.contains("jellyfin"),"Legacy conflicting assignments cannot leak kids titles into adults");
            room.edit().remove(ShelfRepository.netflixKey(false,1)).commit();
            snapshot=fallbackFixture();
            ShelfItem updated=netflixFixture("Eat Pray Bark",150);
            store.apply(snapshot,java.util.Collections.singletonList(updated),false,1);
            check(snapshot.items.get(1)==updated,"Live progress supersedes older cached progress");
            cache.edit().putString("record:"+ShelfRepository.netflixKey(false,1),"invalid json").commit();
            snapshot=fallbackFixture();
            store.apply(snapshot,java.util.Collections.emptyList(),false,1);
            check(snapshot.items.get(1).packageName.contains("jellyfin"),"Corrupt cache falls back to Jellyfin");
        } finally { room.edit().clear().commit(); cache.edit().clear().commit(); }
    }

    private void aircraftArt() throws Exception {
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(1672,941,android.graphics.Bitmap.Config.ARGB_8888);
        android.graphics.Canvas canvas=new android.graphics.Canvas(bitmap);
        android.graphics.Paint paint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setShader(new android.graphics.LinearGradient(0,0,0,941,0xffb9d7ea,0xffeef2e9,android.graphics.Shader.TileMode.CLAMP));
        canvas.drawRect(0,0,1672,941,paint);paint.setShader(null);paint.setColor(0xff21394d);paint.setTextSize(35);
        canvas.drawText("Manga aircraft · airline colour examples",62,75,paint);paint.setTextSize(21);
        canvas.drawText("Design examples only — not live flight detections",64,112,paint);
        String[] ids={"EJU123","RYR123","VLG123","TAP123"};
        AircraftPainter painter=new AircraftPainter(getTargetContext());
        for(int i=0;i<4;i++) {
            int save=canvas.save();canvas.translate(i%2==0?425:1245,i<2?320:715);canvas.scale(2.5f,2.5f);
            painter.draw(canvas,new BridgeFlightTracker.Event("example-"+i,ids[i],90,System.currentTimeMillis()),0,0,400,true,false);
            canvas.restoreToCount(save);check(!AirlineStyle.forCallsign(ids[i]).name.isEmpty(),"Example operator palette resolves");
        }
        try(java.io.FileOutputStream out=getTargetContext().openFileOutput("aircraft-style-examples.png",0)) {
            check(bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out),"Aircraft style preview saved");
        } finally {bitmap.recycle();}
    }

    private void outdoorChecks() throws Exception {
        MainActivity launched=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        waitForIdleSync();SystemClock.sleep(4500);
        MainActivity activity=resumedMain==null?launched:resumedMain;
        awaitFocus(activity);
        Field field=MainActivity.class.getDeclaredField("screensaver");field.setAccessible(true);
        OutdoorScreensaver saver=(OutdoorScreensaver)field.get(activity);
        check(saver.configured(),"Private local weather location is configured");
        runOnMainSync(() -> saver.preview(1));SystemClock.sleep(2500);
        check(saver.showing(),"Day preview covers the room");saveOutdoorCapture("screensaver-day.png");
        checkOutdoorAudio(activity);
        Field index=MainActivity.class.getDeclaredField("shelfIndex");index.setAccessible(true);
        Field room=MainActivity.class.getDeclaredField("roomIndex");room.setAccessible(true);
        Field focus=MainActivity.class.getDeclaredField("shelfFocused");focus.setAccessible(true);
        int beforeIndex=index.getInt(activity),beforeRoom=room.getInt(activity);boolean beforeFocus=focus.getBoolean(activity);
        runOnMainSync(() -> {
            long now=SystemClock.uptimeMillis();
            activity.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_CENTER,0));
            activity.dispatchKeyEvent(new KeyEvent(now,now+50,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_CENTER,0));
        });
        check(!saver.showing(),"First OK closes the screensaver");
        check(index.getInt(activity)==beforeIndex && room.getInt(activity)==beforeRoom && focus.getBoolean(activity)==beforeFocus,
            "Wake key down and up do not move or open the shelf");
        runOnMainSync(() -> saver.preview(0));SystemClock.sleep(2500);
        check(saver.showing(),"Night preview opens");saveOutdoorCapture("screensaver-night.png");
        Intent original=activity.getIntent();
        runOnMainSync(() -> activity.onNewIntent(new Intent(original).addCategory(Intent.CATEGORY_HOME)));
        check(!saver.showing(),"Home returns to the room instead of leaving the outdoor scene visible");
        runOnMainSync(() -> activity.setIntent(original));
        runOnMainSync(() -> saver.preview(0));
        runOnMainSync(saver::pause);check(!saver.showing(),"Pause removes the screensaver");
        runOnMainSync(saver::resume);check(!saver.showing(),"Resume waits for idle instead of covering the room immediately");
        runOnMainSync(() -> saver.preview(-1));SystemClock.sleep(16000);saveOutdoorCapture("screensaver-current.png");
        check(saver.showing(),"Current-weather scene stays visible while foreground");
        runOnMainSync(() -> saver.screen(false));check(!saver.showing(),"Screen-off removes the screensaver");
        runOnMainSync(() -> saver.screen(true));check(!saver.showing(),"Screen-on starts a fresh idle timer");
    }
    private void checkOutdoorAudio(MainActivity activity) throws Exception {
        Field field=MainActivity.class.getDeclaredField("ambientMusic");field.setAccessible(true);
        AmbientMusic music=(AmbientMusic)field.get(activity);
        Field mode=AmbientMusic.class.getDeclaredField("currentMode");mode.setAccessible(true);
        Field player=AmbientMusic.class.getDeclaredField("player");player.setAccessible(true);
        Field loaded=AmbientMusic.class.getDeclaredField("whooshLoaded");loaded.setAccessible(true);
        java.util.concurrent.atomic.AtomicReference<Throwable> failure=new java.util.concurrent.atomic.AtomicReference<>();
        runOnMainSync(() -> {
            try {
                check(mode.getInt(music)==2,"Screensaver selects its private ambient music track");
                android.media.MediaPlayer p=(android.media.MediaPlayer)player.get(music);
                check(p!=null && p.isPlaying(),"Private screensaver music is actually playing");
                check(p.isLooping(),"Screensaver music loops");
                check(p.getDuration()>60000,"The complete music track is decoded");
                check(loaded.getBoolean(music),"Downloaded whoosh loaded successfully in SoundPool");
                music.setEnabled(false);check(player.get(music)==null,"Mute releases music playback immediately");
                music.setEnabled(true);
            } catch(Throwable e) {failure.set(e);}
        });
        if(failure.get()!=null)throw new AssertionError("Audio check: "+failure.get().getMessage()+"; windowFocus="+activity.hasWindowFocus()+"; mode="+mode.getInt(music));
        SystemClock.sleep(1700);
        runOnMainSync(() -> {
            try {android.media.MediaPlayer p=(android.media.MediaPlayer)player.get(music);
                check(p!=null && p.isPlaying(),"Unmute restores the outdoor track");
            } catch(Throwable e) {failure.set(e);}
        });
        if(failure.get()!=null)throw new AssertionError("Audio resume: "+failure.get().getMessage()+"; windowFocus="+activity.hasWindowFocus()+"; mode="+mode.getInt(music));
    }

    private void saveOutdoorCapture(String name) throws Exception {
        android.graphics.Bitmap bitmap=getUiAutomation().takeScreenshot();check(bitmap!=null,"Native outdoor screenshot captured");
        try(java.io.FileOutputStream out=getTargetContext().openFileOutput(name,0)) {
            bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG,100,out);
        } finally {bitmap.recycle();}
    }

    private void uiChecks() throws Exception {
        MainActivity activity=(MainActivity)startActivitySync(new Intent(getTargetContext(),MainActivity.class)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_CLEAR_TASK));
        waitForIdleSync();awaitFocus(activity);
        Field focused=MainActivity.class.getDeclaredField("shelfFocused");focused.setAccessible(true);
        Field index=MainActivity.class.getDeclaredField("shelfIndex");index.setAccessible(true);
        Field roomIndex=MainActivity.class.getDeclaredField("roomIndex");roomIndex.setAccessible(true);
        Field cardsField=MainActivity.class.getDeclaredField("cards");cardsField.setAccessible(true);
        Field availableField=MainActivity.class.getDeclaredField("available");availableField.setAccessible(true);
        Field viewField=MainActivity.class.getDeclaredField("roomView");viewField.setAccessible(true);
        Method focusShelf=MainActivity.class.getDeclaredMethod("focusShelf",boolean.class);focusShelf.setAccessible(true);
        Method switchView=MainActivity.class.getDeclaredMethod("switchView",int.class);switchView.setAccessible(true);
        Method select=MainActivity.class.getDeclaredMethod("select",int.class);select.setAccessible(true);
        Field selectedField=MainActivity.class.getDeclaredField("selected");selectedField.setAccessible(true);
        int initial=roomIndex.getInt(activity);
        for(int visit=0;visit<2;visit++) {
            boolean kidsRoom=roomIndex.getInt(activity)==1;
            AppIconView[] cards=(AppIconView[])cardsField.get(activity);
            Intent target=((Intent[])availableField.get(activity))[0];
            check(target!=null && target.getComponent()!=null && (kidsRoom?
                target.getComponent().getPackageName().equals("pt.reddescent.game"):
                target.getComponent().getPackageName().contains("smarttube")),"Room app target stays specific to Kids or Adults");
            runOnMainSync(() -> { try { index.setInt(activity,0);focusShelf.invoke(activity,true); }
                catch(Exception e) {throw new RuntimeException(e);} });
            if(kidsRoom) {
                for(int row=1;row<6;row++) {
                    roomKey(activity,KeyEvent.KEYCODE_DPAD_DOWN);waitForIdleSync();
                    check(KidsBookcase.row(index.getInt(activity))==row,"Down visits each successive row in the same Kids room");
                    check(roomIndex.getInt(activity)==1 && focused.getBoolean(activity),"Kids section transition retains its room and cover focus");
                }
                roomKey(activity,KeyEvent.KEYCODE_DPAD_DOWN);waitForIdleSync();
                check(!focused.getBoolean(activity),"Down after the last Kids shelf enters app shortcuts");
                for(int i=0;i<3;i++)check(cards[i].getVisibility()==android.view.View.VISIBLE,"Kids app shortcuts remain reachable");
                check(cards[3].getVisibility()==android.view.View.GONE,"Stremio remains Adults-only");
                roomKey(activity,KeyEvent.KEYCODE_DPAD_UP);waitForIdleSync();
                check(focused.getBoolean(activity) && KidsBookcase.row(index.getInt(activity))==5,"Up restores the last Kids shelf");
                runOnMainSync(() -> { try { index.setInt(activity,15);focusShelf.invoke(activity,true); }
                    catch(Exception e) {throw new RuntimeException(e);} });
                roomKey(activity,KeyEvent.KEYCODE_DPAD_DOWN);waitForIdleSync();
                check(index.getInt(activity)==21,"Four-to-six-column move preserves the right edge");
                roomKey(activity,KeyEvent.KEYCODE_DPAD_UP);waitForIdleSync();
                check(index.getInt(activity)==15,"Six-to-four-column move returns to the same side");
                runOnMainSync(() -> { try { index.setInt(activity,0);focusShelf.invoke(activity,true); }
                    catch(Exception e) {throw new RuntimeException(e);} });
                roomKey(activity,KeyEvent.KEYCODE_DPAD_LEFT);waitForIdleSync();
                check(index.getInt(activity)==3 && roomIndex.getInt(activity)==2,"Left past the first Kids cover enters the right edge of Adults");
                roomKey(activity,KeyEvent.KEYCODE_DPAD_RIGHT);waitForIdleSync();
                check(index.getInt(activity)==0 && roomIndex.getInt(activity)==1,"Right from the Adults edge returns to the remembered Kids shelf");
            } else {
                roomKey(activity,KeyEvent.KEYCODE_DPAD_DOWN);waitForIdleSync();
                check(index.getInt(activity)==4,"Adults Down enters the lower shelf");
                roomKey(activity,KeyEvent.KEYCODE_DPAD_UP);waitForIdleSync();
                check(index.getInt(activity)==0,"Adults Up returns to the upper shelf");
            }
            // Both edges of every shelf toggle room and reverse to the same boundary.
            for(int row=0;row<(kidsRoom?6:2);row++) for(int direction:new int[]{-1,1}) {
                int columns=kidsRoom?KidsBookcase.columns(row):4;
                int edge=(kidsRoom?KidsBookcase.start(row):row*4)+(direction<0?0:columns-1);
                runOnMainSync(() -> {try{index.setInt(activity,edge);focusShelf.invoke(activity,true);}
                    catch(Exception e){throw new RuntimeException(e);}});
                roomKey(activity,direction<0?KeyEvent.KEYCODE_DPAD_LEFT:KeyEvent.KEYCODE_DPAD_RIGHT);waitForIdleSync();
                check(roomIndex.getInt(activity)==(kidsRoom?2:1),"Shelf edge switches to the other room in either direction");
                roomKey(activity,direction<0?KeyEvent.KEYCODE_DPAD_RIGHT:KeyEvent.KEYCODE_DPAD_LEFT);waitForIdleSync();
                check(roomIndex.getInt(activity)==(kidsRoom?1:2) && index.getInt(activity)==edge,
                    "Reverse crossing restores the same shelf row and boundary cover");
            }
            for(int direction:new int[]{-1,1}) {
                int appEdge=direction<0?1:(kidsRoom?2:3);
                runOnMainSync(() -> {try{select.invoke(activity,appEdge);}catch(Exception e){throw new RuntimeException(e);}});
                roomKey(activity,direction<0?KeyEvent.KEYCODE_DPAD_LEFT:KeyEvent.KEYCODE_DPAD_RIGHT);waitForIdleSync();
                check(roomIndex.getInt(activity)==(kidsRoom?2:1) && !focused.getBoolean(activity),
                    "Both app-row edges switch rooms and keep app focus");
                check(selectedField.getInt(activity)==(direction>0?1:(kidsRoom?3:2)),
                    "App row enters at its opposite end with the correct app count");
                roomKey(activity,direction<0?KeyEvent.KEYCODE_DPAD_RIGHT:KeyEvent.KEYCODE_DPAD_LEFT);waitForIdleSync();
                check(roomIndex.getInt(activity)==(kidsRoom?1:2) && selectedField.getInt(activity)==appEdge,
                    "Reverse app-edge crossing restores the original app");
            }
            runOnMainSync(() -> { try {switchView.invoke(activity,1);}catch(Exception e){throw new RuntimeException(e);} });
            check(roomIndex.getInt(activity)==(kidsRoom?2:1),"Room control switches between Kids and Adults");
        }
        check(roomIndex.getInt(activity)==initial,"Two room transitions return to the initial room");
        if("disney".equals(mode) || "kids-game".equals(mode)) {
            int previewIndex="disney".equals(mode)?16:8;
            runOnMainSync(() -> {
                try {
                    if(roomIndex.getInt(activity)!=1)switchView.invoke(activity,1);
                    index.setInt(activity,previewIndex);focusShelf.invoke(activity,true);
                }catch(Exception e){throw new RuntimeException(e);}
            });
            // Allow the camera animation to settle before saving an app-rendered preview.
            SystemClock.sleep(600);waitForIdleSync();
            runOnMainSync(() -> {
                try {
                    android.view.View view=(android.view.View)viewField.get(activity);
                    android.graphics.Bitmap preview=android.graphics.Bitmap.createBitmap(view.getWidth(),view.getHeight(),android.graphics.Bitmap.Config.ARGB_8888);
                    view.draw(new android.graphics.Canvas(preview));
                    try(java.io.FileOutputStream file=getTargetContext().openFileOutput(previewIndex==16?"disney-room-preview.png":"kids-game-preview.png",0)) {
                        preview.compress(android.graphics.Bitmap.CompressFormat.PNG,100,file);
                    }
                    preview.recycle();
                }catch(Exception e){throw new RuntimeException(e);}
            });
        }
        runOnMainSync(activity::finish);
    }

    private void roomKey(MainActivity activity,int keyCode) {
        // Dispatch to the activity under test, avoiding Home-alias focus races.
        runOnMainSync(() -> {
            long now=SystemClock.uptimeMillis();
            activity.dispatchKeyEvent(new KeyEvent(now,now,KeyEvent.ACTION_DOWN,keyCode,0));
            activity.dispatchKeyEvent(new KeyEvent(now,now+1,KeyEvent.ACTION_UP,keyCode,0));
        });
    }

    private void awaitFocus(Activity activity) {
        long end=SystemClock.uptimeMillis()+5000;
        boolean[] focused={false};
        while(SystemClock.uptimeMillis()<end) {
            runOnMainSync(() -> focused[0]=activity.getWindow().getDecorView().hasWindowFocus());
            if(focused[0]) { waitForIdleSync(); return; }
            SystemClock.sleep(50);
        }
        throw new AssertionError("Room window did not regain input focus after dialog dismissal");
    }
}
