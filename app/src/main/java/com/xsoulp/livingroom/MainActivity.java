package com.xsoulp.livingroom;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.ComponentName;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Bitmap;
import android.net.Uri;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Native TV controls over a baked cozy room with a live television and shelf. */
public final class MainActivity extends Activity {
    private static final String[][] PACKAGES = {
        {"org.smarttube.stable", "org.smarttube.beta",
         "com.liskovsoft.smartyoutubetv2.tv", "com.liskovsoft.smartyoutubetv2.tv.beta",
         "com.liskovsoft.smarttubetv"},
        {"org.jellyfin.androidtv", "dev.jdtech.jellyfin", "org.jellyfin.mobile"},
        {"com.netflix.ninja", "com.netflix.mediaclient"},
        {"com.stremio.one", "com.stremio.two"}
    };
    private static final String[] NAMES = {"SmartTube", "Jellyfin", "Netflix", "Stremio"};
    private final AppIconView[] cards = new AppIconView[NAMES.length];
    private final Intent[] available = new Intent[NAMES.length];
    private CozyRoomView roomView;
    private SharedPreferences prefs;
    private TextView help,optionsButton,roomButton;
    private AmbientMusic ambientMusic;
    private OutdoorScreensaver screensaver;
    private boolean foreground;
    private android.content.BroadcastReceiver screenReceiver;

    private boolean kids;
    private int roomIndex; // 1 = continuous Kids room, 2 = Adults; older 0/3 IDs migrate below.
    private LinearLayout appRow;
    private final ExecutorService shelfWorker=Executors.newFixedThreadPool(2);
    private final ExecutorService posterWorker=Executors.newFixedThreadPool(3);
    private final android.os.Handler recovery=new android.os.Handler(android.os.Looper.getMainLooper());
    private HourlyBell hourlyBell;
    private int metadataRetryAttempt;
    private final List<ShelfItem> shelfItems=new ArrayList<>();
    private volatile int shelfGeneration;
    private boolean shelfFocused;
    private boolean optionsFocused;
    private int shelfIndex;
    private String shelfStatus="Loading Continue watching…";
    private ShelfRepository shelfRepository;
    private int selected;
    private boolean lowResolution;
    private AlertDialog dialog;
    private long lastNavigation;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        prefs = getSharedPreferences("room", MODE_PRIVATE);
        shelfRepository=new ShelfRepository(this);
        roomIndex=Math.max(0,Math.min(3,prefs.getInt("roomIndex",prefs.getBoolean("kids",false)?1:2)));
        int previousRoom=roomIndex;
        kids=roomIndex!=2;
        roomIndex=kids?1:2;
        shelfIndex=kids?KidsBookcase.clamp(prefs.getInt("kidsShelfIndex",previousRoom==0?0:previousRoom==3?16:8)):0;
        shelfFocused=kids;
        ambientMusic=new AmbientMusic(this);
        hourlyBell=new HourlyBell(this,() -> roomVisible() && ambientMusic.effectsAudible());
        ambientMusic.setEnabled(prefs.getBoolean("backgroundMusic",true));
        ambientMusic.setKids(kids);
        screenReceiver=new android.content.BroadcastReceiver() {
            @Override public void onReceive(android.content.Context context,Intent intent) {
                if(screensaver!=null)screensaver.screen(Intent.ACTION_SCREEN_ON.equals(intent.getAction()));
                updateMusicVisibility();
                if(Intent.ACTION_SCREEN_ON.equals(intent.getAction()) && foreground)refreshShelf();
                if(Intent.ACTION_SCREEN_OFF.equals(intent.getAction()))recovery.removeCallbacksAndMessages(null);
            }
        };
        android.content.IntentFilter screenEvents=new android.content.IntentFilter();
        screenEvents.addAction(Intent.ACTION_SCREEN_OFF);screenEvents.addAction(Intent.ACTION_SCREEN_ON);
        if(android.os.Build.VERSION.SDK_INT>=33) registerReceiver(screenReceiver,screenEvents,android.content.Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(screenReceiver,screenEvents);
        selected = Math.max(0, Math.min(appCount()-1, prefs.getInt("selected", 0)));
        lowResolution = prefs.getBoolean("lowResolution", false);
        immersive();
        roomView=new CozyRoomView(this);
        roomView.setKids(kids);


        roomView.setSectionClick(index -> { shelfIndex=index;focusShelf(true); });
        roomView.setPosterClick(index -> {
            if(shelfFocused && shelfIndex==index) openShelfItem();
            else { shelfIndex=index; focusShelf(true); }
        });
        roomButton=text(kids?"ADULTS →":"← KIDS",12,0xfff2e1be);
        roomButton.setGravity(Gravity.CENTER);roomButton.setFocusable(false);
        roomButton.setBackground(background(0xcc203741,0xff6e807f,1));
        roomButton.setOnClickListener(v -> switchView(1));
        roomView.addView(roomButton,new FrameLayout.LayoutParams(1,1));
        optionsButton=text("OPTIONS  ↓",12,0xfff2e1be);
        optionsButton.setGravity(Gravity.CENTER);
        optionsButton.setFocusable(false);
        optionsButton.setOnClickListener(v -> { focusOptions(); showOptions(); });
        roomView.addView(optionsButton,new FrameLayout.LayoutParams(1,1));
        for(int i=0;i<cards.length;i++) {
            final int index=i;
            cards[i]=new AppIconView(this,i);
            cards[i].setFocusable(false);

            cards[i].setOnClickListener(v -> { focusShelf(false); select(index); openSelected(); });
            roomView.addView(cards[i],new FrameLayout.LayoutParams(1,1));
        }
        roomView.setCameraListener(this::positionToyIcons);
        help=text("",12,0xfff5e8d6);
        help.setGravity(Gravity.CENTER);
        help.setBackground(background(0xb3181515,0x445f5144,1));
        roomView.addView(help,new FrameLayout.LayoutParams(1,1));
        roomView.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob) -> {
            if(r-l==or-ol && b-t==ob-ot) return;
            positionControls();
        });
        FrameLayout stage=new FrameLayout(this);
        stage.addView(roomView,new FrameLayout.LayoutParams(-1,-1));
        screensaver=new OutdoorScreensaver(this,stage,this::updateMusicVisibility,ambientMusic::flightWhoosh,() -> dialog!=null && dialog.isShowing());
        setContentView(stage);
        CreatorCredit.install(getWindow(),false);
        applyResolution();
        refreshAvailability();
        updateShelfInfo();
    }

    private boolean hasApps() { return roomIndex==1 || roomIndex==2; }
    private int appCount() { return kids?3:NAMES.length; }
    private int shelfColumns() { return kids?KidsBookcase.columns(KidsBookcase.row(shelfIndex)):4; }
    private int lastApp() { return appCount()-1; }

    private void positionControls() {
        float[] centers=kids?new float[]{870,580,1170}:new float[]{792,613,973,1157};
        float diameter=kids?177:154;
        for(int i=0;i<cards.length;i++) cards[i].setVisibility(hasApps() && i<appCount()?View.VISIBLE:View.GONE);
        for(int i=0;i<appCount();i++) {
            float size=kids?KidsBookcase.FLOOR_APP_SIZE:diameter;
            float center=kids?KidsBookcase.floorAppX(i):centers[i];
            position(cards[i],(center-size/2)/CozyRoomView.W,
                ((kids?KidsBookcase.FLOOR_APP_Y-KidsBookcase.FLOOR_CAMERA:683)-size/2)/CozyRoomView.H,size/CozyRoomView.W,size/CozyRoomView.H);
        }
        position(optionsButton,.865f,.035f,.115f,.045f);
        position(roomButton,.745f,.035f,.11f,.045f);
        position(help,.17f,.928f,.66f,.044f);
        positionToyIcons();
    }
    private void positionToyIcons() {
        if(roomView==null)return;
        float scale=Math.min(roomView.getWidth()/CozyRoomView.W,roomView.getHeight()/CozyRoomView.H);
        float offset=kids?(KidsBookcase.FLOOR_CAMERA-roomView.cameraY())*scale:0;
        for(AppIconView card:cards) if(card!=null)card.setTranslationY(offset);
    }
    private void switchAtEdge(int direction,boolean fromShelf) {
        int sourceRow=kids?KidsBookcase.row(shelfIndex)%2:shelfIndex/4;
        int remembered=kids?shelfIndex:prefs.getInt("kidsShelfIndex",8);
        switchView(direction);
        if(fromShelf) {
            shelfIndex=RoomEdges.enterShelf(kids,remembered,sourceRow,direction);
            shelfFocused=true;optionsFocused=false;
            if(kids)prefs.edit().putInt("kidsShelfIndex",shelfIndex).apply();
        } else {
            selected=direction>0?1:lastApp();shelfFocused=false;optionsFocused=false;
            prefs.edit().putInt("selected",selected).apply();
        }
        updateCards();updateShelfInfo();
    }
    private void switchView(int direction) {
        if(kids) prefs.edit().putInt("kidsShelfIndex",KidsBookcase.clamp(shelfIndex)).apply();
        kids=!kids;roomIndex=kids?1:2;
        shelfFocused=true;optionsFocused=false;
        shelfIndex=kids?KidsBookcase.clamp(prefs.getInt("kidsShelfIndex",8)):0;
        prefs.edit().putInt("roomIndex",roomIndex).putBoolean("kids",kids).apply();
        shelfGeneration++;shelfItems.clear();
        selected=1;prefs.edit().putInt("selected",selected).apply();
        roomView.setItems(new ArrayList<>(),new ArrayList<>(),"Loading…");
        roomView.setKids(kids);
        ambientMusic.setKids(kids);
        roomButton.setText(kids?"ADULTS →":"← KIDS");
        applyResolution();positionControls();
        refreshShelf();refreshAvailability();updateShelfInfo();
    }
    private void position(View child,float x,float y,float width,float height) {
        float scale=Math.min(roomView.getWidth()/CozyRoomView.W,roomView.getHeight()/CozyRoomView.H);
        int sceneWidth=Math.round(CozyRoomView.W*scale),sceneHeight=Math.round(CozyRoomView.H*scale);
        FrameLayout.LayoutParams p=new FrameLayout.LayoutParams(Math.round(width*sceneWidth),Math.round(height*sceneHeight));
        p.leftMargin=(roomView.getWidth()-sceneWidth)/2+Math.round(x*sceneWidth);
        p.topMargin=(roomView.getHeight()-sceneHeight)/2+Math.round(y*sceneHeight);
        child.setLayoutParams(p);
    }

    private int dp(float value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private GradientDrawable background(int fill, int stroke, int width) {
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(fill);
        bg.setCornerRadius(dp(9));
        bg.setStroke(dp(width), stroke);
        return bg;
    }

    private Intent resolve(int index) {
        String[] candidates=roomIndex==1 && index==0?new String[]{"pt.reddescent.game"}:PACKAGES[index];
        for (String name : candidates) {
            try {
                Intent intent = getPackageManager().getLeanbackLaunchIntentForPackage(name);
                if (intent == null) intent = getPackageManager().getLaunchIntentForPackage(name);
                if (intent != null && getPackageManager().resolveActivity(intent, 0) != null)
                    return intent;
            } catch (RuntimeException ignored) {
                // A package can be removed, disabled, or restricted while we are open.
            }
        }
        return null;
    }

    private void refreshAvailability() {
        for (int i = 0; i < available.length; i++) available[i] = resolve(i);
        updateCards();
    }

    private void updateCards() {
        for (int i = 0; i < cards.length; i++) {
            boolean active = !shelfFocused && !optionsFocused && i == selected;
            android.graphics.drawable.Drawable icon=null;
            if(available[i]!=null && available[i].getComponent()!=null) {
                try { icon=getPackageManager().getApplicationIcon(available[i].getComponent().getPackageName()); }
                catch(android.content.pm.PackageManager.NameNotFoundException ignored) { }
            }
            cards[i].setIcon(icon,active,available[i]!=null);
            cards[i].setReplacement(kids);
            cards[i].setToy(kids);
            cards[i].setLabel(appName(i));
            cards[i].setContentDescription(appName(i) + (active ? ", selected" : "")
                + (available[i] != null ? ", available" : ", unavailable"));
            cards[i].setSelected(active);
        }
        roomButton.setVisibility(kids?View.GONE:View.VISIBLE);
        optionsButton.setVisibility(kids?View.GONE:View.VISIBLE);
        help.setVisibility(kids?View.GONE:View.VISIBLE);
        optionsButton.setText(optionsFocused?"OPTIONS   OK":"OPTIONS   ↓");
        optionsButton.setTextColor(optionsFocused?0xff172d35:0xfff2e1be);
        optionsButton.setBackground(background(optionsFocused?0xffefd19b:0xcc203741,
            optionsFocused?0xfffff0c9:0xff6e807f,optionsFocused?3:1));
        optionsButton.setSelected(optionsFocused);
        optionsButton.setContentDescription(optionsFocused?"Options, selected. Press OK to open settings":"Options. Press Down to select");
    }

    private void select(int index) {
        if(!hasApps()) { focusShelf(true); return; }
        optionsFocused=false; shelfFocused=false;
        selected = Math.floorMod(index, appCount());
        prefs.edit().putInt("selected", selected).apply();
        updateCards();
        updateShelfInfo();
    }

    private void openSelected() {
        if(!hasApps()) { focusShelf(true); return; }
        available[selected] = resolve(selected);
        updateCards();
        if (available[selected] == null) {
            showMissing(appName(selected));
            return;
        }
        try {
            Intent launch = new Intent(available[selected]);
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);
        } catch (ActivityNotFoundException | SecurityException e) {
            showMissing(appName(selected));
        }
    }

    private String appName(int index) { return roomIndex==1 && index==0?"Red Descent":NAMES[index]; }

    private void showMissing(String app) {
        dialog = new CreatorCredit.DialogBuilder(this).setTitle(app + " unavailable")
            .setMessage("This app is missing, disabled, or cannot be opened. "
                + "Install or enable it, then return to the room. Your selection is saved.")
            .setPositiveButton("App settings", (d, w) ->
                safeStart(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)))
            .setNegativeButton("Back to room", null).show();
    }

    private void safeStart(Intent intent) {
        try { startActivity(intent); }
        catch (ActivityNotFoundException | SecurityException e) {
            Toast.makeText(this, "This option is unavailable on this TV", Toast.LENGTH_LONG).show();
        }
    }

    private void applyResolution() {
        roomView.loadRoom(lowResolution);
    }

    private void showOptions() {
        dialog = new CreatorCredit.DialogBuilder(this).setTitle("Family Shelf options")
            .setItems(new String[]{
                "Browse shelves", "Connect / change Jellyfin account", "Sign out of Jellyfin",
                lowResolution ? "Room detail: low — switch to full" : "Room detail: full — switch to low",
                "Refresh apps and Continue watching", "Allow shared TV data", "Android settings", "Help / controls", "Libraries for this view", "Netflix for this view", "Remote buttons: Netflix / Jellyfin / SmartTube",
                prefs.getBoolean("backgroundMusic",true)?"Background music: on — switch off":"Background music: off — switch on",
                kids?"Go to Adults library":"Go to Kids library", "Outdoor screensaver", "Video aircraft overlay", "Hourly church bell", "Exit room"
            }, (d, which) -> {
                if(which==0) focusShelf(true);
                else if(which==1) startActivity(new Intent(this,JellyfinConnectActivity.class));
                else if(which==2) confirmSignOut();
                else if (which == 3) {
                    lowResolution = !lowResolution;
                    prefs.edit().putBoolean("lowResolution", lowResolution).apply();
                    applyResolution();
                } else if (which == 4) {
                    refreshAvailability();
                    refreshShelf();
                } else if (which == 5) {
                    requestPermissions(new String[]{"android.permission.READ_TV_LISTINGS"},72);
                } else if (which == 6) {
                    safeStart(new Intent(Settings.ACTION_SETTINGS));
                } else if (which == 7) {
                    dialog = new CreatorCredit.DialogBuilder(this).setTitle("Welcome home")
                        .setMessage("In Kids, Up/Down moves the camera through all six shelves in one bookcase. "
                            + "Left/Right selects a cover; beyond either end changes between Kids and Adults. Back moves to the floor app toys. "
                            + "Use Go to Adults/Kids library in Options to change rooms. "
                            + "OK opens an item in Jellyfin; select Resume there. "
                            + "Down in the room selects Options; press OK to open settings. "
                            + "Menu or long-press OK also opens options. Back returns to the room.\n\n"
                            + "Home selection is managed separately in Android settings.\n\n"
                            + "Netflix cover artwork: Cinemeta (https://cinemeta.strem.io). Viewing history and playback links: Netflix Watch Next.\n\n"
                            + "Music: Music Box Theme (Kids) and Darkest Child (Adults), by Kevin MacLeod (incompetech.com). "
                            + "Licensed under Creative Commons Attribution 4.0: https://creativecommons.org/licenses/by/4.0/ . "
                            + "Original MP3 recordings, unchanged; playback loops at reduced volume.")
                        .setPositiveButton("Return to room", null).show();
                } else if(which==8) chooseLibraries();
                else if(which==9) chooseNetflix();
                else if(which==10) safeStart(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
                else if(which==11) {
                    boolean enabled=!prefs.getBoolean("backgroundMusic",true);
                    prefs.edit().putBoolean("backgroundMusic",enabled).apply();
                    ambientMusic.setEnabled(enabled);
                } else if(which==12) switchView(1);
                else if(which==13) showScreensaverOptions();
                else if(which==14) showVideoAircraftOptions();
                else if(which==15) showHourlyBellOptions();
                else finish();
            }).show();
    }

    private void showHourlyBellOptions() {
        dialog=new CreatorCredit.DialogBuilder(this).setTitle("Hourly church bell")
            .setItems(new String[]{prefs.getBoolean("hourlyBell",true)?"Enabled · switch off":"Disabled · switch on",
                "Preview one bell", "About hourly bells"},(d,which) -> {
                if(which==0)prefs.edit().putBoolean("hourlyBell",!prefs.getBoolean("hourlyBell",true)).apply();
                else if(which==1) {
                    if(screensaver==null || !screensaver.configured())Toast.makeText(this,"Configure the outdoor screensaver first",Toast.LENGTH_LONG).show();
                    else {
                        screensaver.preview(-1);
                        roomView.postDelayed(() -> {
                            if(!hourlyBell.preview())Toast.makeText(this,"Enable background audio and hourly bells to preview",Toast.LENGTH_LONG).show();
                        },900);
                    }
                }
                else if(which==2)dialog=new CreatorCredit.DialogBuilder(this).setTitle("Hourly church bell")
                    .setMessage("At the exact hour, the church bell strikes 1–12 times using the screensaver's time zone. "
                        +"It plays quietly only while the outdoor screensaver is visible. Leaving it, muting or sleeping stops the sequence; missed hours are not replayed. "
                        +"Bell 1 O'clock by Joseph SARDIN & Axeline T., BigSoundBank, CC0: https://bigsoundbank.com/bell-1-o-clock-s3446.html")
                    .setPositiveButton("OK",null).show();
            }).show();
    }

    private void showVideoAircraftOptions() {
        dialog=new CreatorCredit.DialogBuilder(this).setTitle("Aircraft over Netflix & Jellyfin")
            .setItems(new String[]{prefs.getBoolean("netflixAircraftOverlay",true)?"Enabled · switch off":"Disabled · switch on",
                Settings.canDrawOverlays(this)?"Display over other apps: allowed":"Allow display over other apps",
                NetflixAircraftService.playbackAccess(this)?"Video playback access: allowed":"Allow Video playback access",
                "About video aircraft"},(d,which) -> {
                if(which==0) {prefs.edit().putBoolean("netflixAircraftOverlay",!prefs.getBoolean("netflixAircraftOverlay",true)).apply();NetflixAircraftService.refresh();}
                else if(which==1)safeStart(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,Uri.parse("package:"+getPackageName())));
                else if(which==2)safeStart(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
                else dialog=new CreatorCredit.DialogBuilder(this).setTitle("Aircraft over Netflix & Jellyfin")
                    .setMessage("Real bridge-area flight detections appear as small aircraft over Netflix or Jellyfin while a video is playing. "
                        +"They do not take focus or handle remote buttons. Android overlay and notification access are needed to observe playback state. "
                        +"Notification messages and screen contents are not read. Bonito music remains in the outdoor screensaver. Flight data: ADSB.lol (https://www.adsb.lol/), Open Database License (ODbL) 1.0 (https://opendatacommons.org/licenses/odbl/1-0/).")
                    .setPositiveButton("OK",null).show();
            }).show();
    }

    private void showScreensaverOptions() {
        dialog=new CreatorCredit.DialogBuilder(this).setTitle("Outdoor screensaver · "+screensaver.place())
            .setItems(new String[]{screensaver.enabled()?"After 2 minutes · switch off":"Screensaver off · enable after 2 minutes",
                "Preview current weather", "Preview daytime", "Preview night-time"},(d,which) -> {
                if(which==0)screensaver.setEnabled(!screensaver.enabled());
                else if(!screensaver.configured())new CreatorCredit.DialogBuilder(this).setTitle("Landscape not configured")
                    .setMessage("A weather location has not been configured for this TV.").setPositiveButton("OK",null).show();
                else screensaver.preview(which==1?-1:which==2?1:0);
            }).show();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if(screensaver!=null && screensaver.key(event))return true;
        if(RemoteButtons.handle(this,event)) return true;
        if (dialog != null && dialog.isShowing()) return super.dispatchKeyEvent(event);
        int key = event.getKeyCode();
        boolean direction = key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT
            || key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN;
        boolean confirm = key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER
            || key == KeyEvent.KEYCODE_NUMPAD_ENTER || key == KeyEvent.KEYCODE_BUTTON_A;
        if (direction) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                long now = event.getEventTime();
                if (event.getRepeatCount() == 0 || now - lastNavigation >= 180) {
                    if(kids) {
                        if(key==KeyEvent.KEYCODE_DPAD_UP) {
                            if(shelfFocused) shelfIndex=KidsBookcase.vertical(shelfIndex,-1);
                            else { shelfFocused=true;optionsFocused=false; }
                        } else if(key==KeyEvent.KEYCODE_DPAD_DOWN) {
                            if(shelfFocused) {
                                if(KidsBookcase.row(shelfIndex)<KidsBookcase.ROWS-1)
                                    shelfIndex=KidsBookcase.vertical(shelfIndex,1);
                                else { shelfFocused=false;optionsFocused=false; }
                            } else if(optionsFocused) { shelfFocused=true;optionsFocused=false; }
                            else optionsFocused=true;
                        } else {
                            int step=key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1;
                            if(shelfFocused) {
                                if(KidsBookcase.atHorizontalEdge(shelfIndex,step)) { switchAtEdge(step,true);return true; }
                                shelfIndex=KidsBookcase.horizontal(shelfIndex,step);
                            }
                            else if(optionsFocused) { switchView(step);return true; }
                            else {
                                int col=selected==1?0:selected==0?1:selected,next=col+step;
                                if(next<0 || next>=appCount()) { switchAtEdge(step,false);return true; }
                                select(next==0?1:next==1?0:next);
                            }
                        }
                        prefs.edit().putInt("kidsShelfIndex",KidsBookcase.clamp(shelfIndex)).apply();
                        updateShelfInfo();updateCards();
                    } else if(key==KeyEvent.KEYCODE_DPAD_UP) {
                        if(shelfFocused) { if(shelfIndex>=4) { shelfIndex-=4;updateShelfInfo(); } }
                        else { shelfIndex=4+(selected==1?0:selected==0?1:selected);focusShelf(true); }
                    } else if(key==KeyEvent.KEYCODE_DPAD_DOWN) {
                        if(shelfFocused && shelfIndex<4) { shelfIndex+=4;updateShelfInfo(); }
                        else if(shelfFocused || optionsFocused) focusShelf(false);
                        else focusOptions();
                    } else {
                        int step=key==KeyEvent.KEYCODE_DPAD_LEFT?-1:1;
                        if(shelfFocused) {
                            int col=shelfIndex%4;
                            if(col+step<0 || col+step>=4) switchAtEdge(step,true);
                            else { shelfIndex+=step;updateShelfInfo(); }
                        } else if(optionsFocused) select(step>0?1:lastApp());
                        else {
                            int col=selected==1?0:selected==0?1:selected,next=col+step;
                            if(next<0 || next>=appCount()) switchAtEdge(step,false);
                            else select(next==0?1:next==1?0:next);
                        }
                    }
                    lastNavigation = now;
                }
            }
            return true;
        }
        if (confirm) {
            if (event.getAction() == KeyEvent.ACTION_UP && !event.isCanceled()) {
                if (event.getEventTime() - event.getDownTime() >= 650) showOptions();
                else if(optionsFocused) showOptions();
                else if(shelfFocused) openShelfItem();
                else openSelected();
            }
            return true;
        }
        if (key == KeyEvent.KEYCODE_MENU) {
            if (event.getAction() == KeyEvent.ACTION_UP) showOptions();
            return true;
        }
        if(key==KeyEvent.KEYCODE_BACK && (shelfFocused || optionsFocused)) {
            if(event.getAction()==KeyEvent.ACTION_UP) focusShelf(false);
            return true;
        }
        if(key==KeyEvent.KEYCODE_BACK && getIntent().hasCategory(Intent.CATEGORY_HOME)) return true;
        return super.dispatchKeyEvent(event);
    }

    @Override public void onUserInteraction() {
        super.onUserInteraction();if(screensaver!=null)screensaver.activity();
    }
    @Override public boolean dispatchTouchEvent(android.view.MotionEvent event) {
        if(screensaver!=null && screensaver.touch(event))return true;
        return super.dispatchTouchEvent(event);
    }

    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }

    @Override public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) immersive();
    }

    @Override protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if(intent.hasCategory(Intent.CATEGORY_HOME)) {
            if(screensaver!=null)screensaver.returnToRoom();
            focusShelf(false);
        }
    }

    @Override protected void onResume() {
        super.onResume();
        roomView.setActive(true);
        foreground=true;NetflixAircraftService.launcher(true);if(screensaver!=null)screensaver.resume();updateMusicVisibility();
        refreshAvailability();
        refreshShelf();
    }

    @Override protected void onPause() {
        if(hourlyBell!=null)hourlyBell.foreground(false);
        foreground=false;NetflixAircraftService.launcher(false);if(screensaver!=null)screensaver.pause();ambientMusic.setForeground(false);
        recovery.removeCallbacksAndMessages(null);
        shelfGeneration++;
        roomView.setActive(false);
        super.onPause();
    }

    private boolean roomVisible() {
        android.os.PowerManager power=(android.os.PowerManager)getSystemService(POWER_SERVICE);
        return foreground && power!=null && power.isInteractive();
    }
    private void updateMusicVisibility() {
        boolean visible=roomVisible();
        if(!visible)ambientMusic.setForeground(false);
        ambientMusic.setOutdoor(screensaver!=null && screensaver.showing());
        if(visible)ambientMusic.setForeground(true);
        if(hourlyBell!=null)hourlyBell.foreground(visible && screensaver!=null && screensaver.showing());
    }
    private void focusShelf(boolean value) {
        optionsFocused=!value && !hasApps(); shelfFocused=value;
        updateCards(); updateShelfInfo();
    }

    private void focusOptions() {
        optionsFocused=true; shelfFocused=false;
        updateCards(); updateShelfInfo();
    }

    private void updateShelfInfo() {
        positionControls();
        if(kids) {
            help.setText(shelfFocused?"↑ ↓  Shelves    ·    ← →  Select / room    ·    OK  Open    ·    Back  Floor apps":
                optionsFocused?"OK  Options    ·    ↑  Shelves    ·    ← →  Adults":"← →  Floor apps / room    ·    ↑  Shelves    ·    ↓  Options    ·    OK  Open");
            roomView.select(selected,shelfIndex,shelfFocused,optionsFocused);
            roomView.setContentDescription("Kids library · "+KidsBookcase.section(shelfIndex)+". "+help.getText());
            return;
        }
        help.setText(!hasApps()?(optionsFocused?"OK  Options    ·    ↑  Books    ·    ◀ ▶  Next room"
            :"◀ ▶  Books / next room    ·    ↑ ↓  Shelves / options    ·    OK  Open"):optionsFocused?"OK  Options    ·    ↑  Books    ·    ◀ ▶  Apps"
            :shelfFocused?"◀ ▶  Books / next room    ·    ↑ ↓  Shelves / apps    ·    OK  Open"
            :"◀ ▶  Apps / next room    ·    ↑  Books    ·    ↓  Options    ·    OK  Open");
        roomView.select(selected,shelfIndex,shelfFocused,optionsFocused);
        roomView.setContentDescription((roomIndex==3?"Disney Library":roomIndex==0?"Books & Stories":kids?"My Video Library":"Adults Library")+". "+help.getText());
    }

    private void refreshShelf() {
        recovery.removeCallbacksAndMessages(null);
        final int request=++shelfGeneration;
        final String previous=shelfItems.isEmpty()?"":shelfItems.get(Math.min(shelfIndex,shelfItems.size()-1)).key;
        final int requestedRoom=roomIndex,selectionAtRequest=shelfIndex;
        final boolean showSaved=shelfItems.isEmpty();
        shelfWorker.execute(() -> {
            if(request!=shelfGeneration)return;
            if(showSaved) {
                ShelfRepository.Snapshot saved=shelfRepository.cached(requestedRoom==1);
                if(!saved.items.isEmpty())runOnUiThread(() ->
                    displayShelf(request,saved,previous,selectionAtRequest,false));
            }
            ShelfRepository.Snapshot snapshot=shelfRepository.loadRoom(requestedRoom==1);
            if(request!=shelfGeneration)return;
            runOnUiThread(() -> displayShelf(request,snapshot,previous,selectionAtRequest,true));
        });
    }
    private void displayShelf(int request,ShelfRepository.Snapshot snapshot,String previous,int selectionAtRequest,boolean live) {
        if(request!=shelfGeneration || isFinishing() || isDestroyed())return;
        shelfItems.clear();shelfItems.addAll(snapshot.items);
        shelfIndex=Math.max(0,Math.min(shelfIndex,shelfItems.size()-1));
        if(shelfIndex==selectionAtRequest)
            for(int i=0;i<shelfItems.size();i++)if(shelfItems.get(i).key.equals(previous))shelfIndex=i;
        shelfStatus=snapshot.status;
        roomView.setItems(snapshot.items,snapshot.latest,snapshot.status);
        updateShelfInfo();
        for(ShelfItem item:snapshot.items)if(!item.imagePath.isEmpty())loadArtwork(request,snapshot,item,0,live);
        if(live) {
            if(snapshot.retryNeeded && roomVisible()) {
                long delay=ShelfRecovery.delay(metadataRetryAttempt++);
                recovery.postDelayed(() -> { if(request==shelfGeneration && roomVisible())refreshShelf(); },delay);
            } else metadataRetryAttempt=0;
        }
    }
    private void loadArtwork(int request,ShelfRepository.Snapshot snapshot,ShelfItem item,int attempt,boolean network) {
        if(request!=shelfGeneration || isDestroyed())return;
        posterWorker.execute(() -> {
            if(request!=shelfGeneration)return;
            try {
                Bitmap image=network?shelfRepository.poster(snapshot,item):shelfRepository.cachedPoster(snapshot,item);
                if(!network && image==null)return;
                if(image==null)throw new java.io.IOException("Poster not available yet");
                publishArtwork(request,item.key,image);
            } catch(Exception error) {
                if(!ShelfRecovery.retryable(error))return;
                runOnUiThread(() -> {
                    if(request!=shelfGeneration || !roomVisible() || isDestroyed())return;
                    recovery.postDelayed(() -> {
                        if(roomVisible())loadArtwork(request,snapshot,item,Math.min(attempt+1,5),true);
                    },ShelfRecovery.delay(attempt));
                });
            }
        });
    }

    private void publishArtwork(int request,String key,Bitmap bitmap) {
        if(bitmap==null) return;
        runOnUiThread(() -> {
            if(request!=shelfGeneration || isFinishing() || isDestroyed()) { bitmap.recycle(); return; }
            roomView.setArtwork(key,bitmap);
        });
    }

    private void openShelfItem() {
        if(shelfItems.isEmpty()) return;
        ShelfItem item=shelfItems.get(shelfIndex);
        if(item.key.startsWith("disney-empty:") && item.kind==ShelfItem.SHORTCUT) {
            dialog=new CreatorCredit.DialogBuilder(this).setTitle(item.title).setMessage(item.subtitle)
                .setPositiveButton("Refresh",(d,w) -> refreshShelf()).setNeutralButton("Libraries",(d,w) -> chooseLibraries())
                .setNegativeButton("Back",null).show(); return;
        }
        if(item.key.startsWith("story-empty:") && item.kind==ShelfItem.SHORTCUT) { refreshShelf(); return; }
        if(item.key.startsWith("empty:") && item.kind==ShelfItem.SHORTCUT) { chooseLibraries(); return; }
        if(item.kind==ShelfItem.CONNECT) { startActivity(new Intent(this,JellyfinConnectActivity.class)); return; }
        if(item.kind==ShelfItem.JELLYFIN) {
            Intent intent=new Intent(Intent.ACTION_VIEW)
                .setComponent(new ComponentName("org.jellyfin.androidtv","org.jellyfin.androidtv.ui.startup.StartupActivity"))
                .putExtra("ItemId",ShelfItem.canonicalId(item.itemId)).putExtra("HideSplash",true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { startActivity(intent); return; }
            catch(ActivityNotFoundException|SecurityException e) {
                Toast.makeText(this,"Item links unavailable — opening Jellyfin",Toast.LENGTH_LONG).show();
            }
        } else if(item.kind==ShelfItem.WATCH_NEXT) {
            try {
                Intent intent=Intent.parseUri(item.intentUri,Intent.URI_INTENT_SCHEME|Intent.URI_ANDROID_APP_SCHEME);
                if(intent.getComponent()!=null && !item.packageName.equals(intent.getComponent().getPackageName()))
                    throw new SecurityException("Cross-package link");
                intent.setSelector(null); intent.setPackage(item.packageName); intent.setClipData(null);
                if(!Intent.ACTION_VIEW.equals(intent.getAction()) && !Intent.ACTION_MAIN.equals(intent.getAction()))
                    throw new SecurityException("Unsupported shared action");
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent); return;
            } catch(Exception ignored) {
                Toast.makeText(this,"Shared item link unavailable — opening app",Toast.LENGTH_LONG).show();
            }
        }
        int app=item.packageName.contains("netflix")?2:item.packageName.contains("jellyfin")?1:0;
        select(app); openSelected();
    }

    private void chooseLibraries() {
        final boolean requestedKids=kids;
        shelfWorker.execute(() -> {
            try {
                JellyfinStore store=new JellyfinStore(this);
                org.json.JSONArray rows=new JellyfinApi(store.server(),store.token(),store.userId(),store.deviceId()).libraries();
                String[] names=new String[rows.length()], ids=new String[rows.length()];
                for(int i=0;i<rows.length();i++) { names[i]=rows.getJSONObject(i).optString("Name"); ids[i]=rows.getJSONObject(i).optString("Id"); }
                runOnUiThread(() -> {
                    if(isFinishing() || isDestroyed()) return;
                    String key=requestedKids?"kidsLibraries":"adultLibraries";
                    java.util.Set<String> selectedIds=new java.util.HashSet<>(prefs.getStringSet(key,java.util.Collections.emptySet()));
                    boolean[] checked=new boolean[ids.length];
                    for(int i=0;i<ids.length;i++) checked[i]=selectedIds.contains(ids[i]);
                    dialog=new CreatorCredit.DialogBuilder(this).setTitle(requestedKids?"Kids libraries":"Adults libraries")
                        .setMultiChoiceItems(names,checked,(d,index,value) -> { if(value) selectedIds.add(ids[index]); else selectedIds.remove(ids[index]); })
                        .setPositiveButton("Save",(d,w) -> { prefs.edit().putStringSet(key,selectedIds).apply(); refreshShelf(); })
                        .setNegativeButton("Cancel",null).show();
                });
            } catch(Exception e) { runOnUiThread(() -> { if(!isFinishing() && !isDestroyed()) Toast.makeText(this,"Connect to Jellyfin before choosing libraries",Toast.LENGTH_LONG).show(); }); }
        });
    }
    private void chooseNetflix() {
        final boolean requestedKids=kids;
        dialog=new CreatorCredit.DialogBuilder(this).setTitle(requestedKids?"Kids Netflix":"Netflix · Adults")
            .setItems(new String[]{"Choose a movie to continue","Choose a series to continue"},
                (d,index) -> chooseNetflixTitle(requestedKids,index==0?1:3)).show();
    }

    private void chooseNetflixTitle(boolean requestedKids,int slot) {
        shelfWorker.execute(() -> {
            WatchNextReader.Result shared=WatchNextReader.read(this);
            NetflixShelfStore netflix=new NetflixShelfStore(this);
            List<ShelfItem> choices=new ArrayList<>();
            java.util.HashSet<String> titles=new java.util.HashSet<>();
            for(ShelfItem item:shared.items) if(NetflixShelfStore.netflix(item)
                && netflix.canAssign(item.title,requestedKids) && titles.add(item.title)) choices.add(item);
            runOnUiThread(() -> {
                if(isFinishing() || isDestroyed()) return;
                if(choices.isEmpty()) {
                    dialog=new CreatorCredit.DialogBuilder(this).setTitle("Netflix · No titles for this view")
                        .setMessage("Open Netflix, choose "+(requestedKids?"the kids profile":"the adults profile")
                            +" and return to the room to refresh. If it is still empty, check Options → Allow shared TV data.")
                        .setPositiveButton("Open Netflix",(d,w) -> {
                            Intent launch=resolve(2);
                            if(launch==null) showMissing("Netflix");
                            else safeStart(new Intent(launch).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                        })
                        .setNeutralButton("Refresh",(d,w) -> { refreshShelf(); chooseNetflixTitle(requestedKids,slot); })
                        .setNegativeButton("Back",null).show();
                    return;
                }
                String[] names=new String[choices.size()];
                for(int i=0;i<names.length;i++) names[i]=choices.get(i).title;
                dialog=new CreatorCredit.DialogBuilder(this).setTitle((slot==1?"Movie":"Series")
                        +(requestedKids?" · Kids":" · Adults"))
                    .setItems(names,(d,index) -> {
                        if(!netflix.select(requestedKids,slot,choices.get(index)))
                            Toast.makeText(this,"This title belongs to the other view",Toast.LENGTH_LONG).show();
                        refreshShelf();
                    }).setNegativeButton("Cancel",null).show();
            });
        });
    }

    private void confirmSignOut() {
        dialog=new CreatorCredit.DialogBuilder(this).setTitle("Sign out of Jellyfin?")
            .setMessage("Remove this launcher's saved session. The Jellyfin TV app stays signed in.")
            .setNegativeButton("Cancel",null).setPositiveButton("Sign out",(d,w) -> {
                shelfGeneration++;
                shelfWorker.execute(() -> {
                    JellyfinStore store=new JellyfinStore(this);
                    boolean revoked=true;
                    try {
                        String token=store.token();
                        if(!token.isEmpty()) new JellyfinApi(store.server(),token,store.userId(),store.deviceId()).logout();
                    } catch(Exception e) { revoked=false; }
                    store.clear();
                    shelfRepository.clearCache();
                    final boolean success=revoked;
                    runOnUiThread(() -> {
                        if(isFinishing() || isDestroyed()) return;
                        Toast.makeText(this,success?"Jellyfin signed out":"Local session removed; server could not revoke it",Toast.LENGTH_LONG).show();
                        refreshShelf();
                    });
                });
            }).show();
    }

    @Override public void dump(String prefix,java.io.FileDescriptor fd,java.io.PrintWriter writer,String[] args) {
        writer.println(prefix+"hourlyBell="+(hourlyBell==null?"unavailable":hourlyBell.status().toString()));
    }

    @Override protected void onDestroy() {
        shelfGeneration++;recovery.removeCallbacksAndMessages(null);
        shelfWorker.shutdownNow();posterWorker.shutdownNow();
        if(screensaver!=null)screensaver.release();
        if(hourlyBell!=null)hourlyBell.release();
        if(ambientMusic!=null) ambientMusic.release();
        if(screenReceiver!=null) unregisterReceiver(screenReceiver);
        super.onDestroy();
    }

    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] results) {
        super.onRequestPermissionsResult(request,permissions,results);
        if(request==72) refreshShelf();
    }
}
