package com.xsoulp.livingroom;

import android.app.NotificationManager;
import android.content.*;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.media.session.*;
import android.os.*;
import android.provider.Settings;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.view.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.json.JSONObject;

/** System-bound Netflix/Jellyfin playback observer. Component name retained to preserve granted access. Notification bodies and screen contents are never read. */
public final class NetflixAircraftService extends NotificationListenerService {
    private static NetflixAircraftService instance;
    private static String foregroundPackage;
    private static boolean launcherVisible;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final ExecutorService playbackWorker=Executors.newSingleThreadExecutor();
    private boolean probing;
    private String fallbackPlayer="",playbackError="";
    private long fallbackAt;
    private final Runnable playbackTick=this::probePlayback;
    private final BridgeFlightTracker tracker=new BridgeFlightTracker();
    private final List<MediaController> controllers=new ArrayList<>();
    private MediaSessionManager sessions;
    private WindowManager windows;
    private SharedPreferences prefs;
    private int generation,failures;
    private boolean connected,active,busy,destroyed;
    private long nextFetch;
    private int polls,detected,shown;
    private String lastFlight="",lastError="";
    private NetflixAircraftView overlay;
    private final Runnable pollTick=this::poll;
    private final MediaSessionManager.OnActiveSessionsChangedListener sessionListener=this::replaceControllers;
    private final MediaController.Callback playbackListener=new MediaController.Callback() {
        @Override public void onPlaybackStateChanged(PlaybackState state) {reconcile();}
        @Override public void onSessionDestroyed() {refreshSessions();}
    };
    private final SharedPreferences.OnSharedPreferenceChangeListener preferenceListener=(p,k) -> {
        if("netflixAircraftOverlay".equals(k))reconcile();
    };
    private final BroadcastReceiver screenReceiver=new BroadcastReceiver() {
        @Override public void onReceive(Context c,Intent i) {reconcile();}
    };
    public static ComponentName component(Context context) {return new ComponentName(context,NetflixAircraftService.class);}
    static void foreground(String name) {
        foregroundPackage=name;refresh();
    }
    static void launcher(boolean visible) {launcherVisible=visible;refresh();}
    static void refresh() {if(instance!=null)instance.handler.post(instance::reconcile);}
    static boolean playbackAccess(Context context) {
        if(Build.VERSION.SDK_INT>=27) {
            NotificationManager manager=(NotificationManager)context.getSystemService(NOTIFICATION_SERVICE);
            return manager!=null && manager.isNotificationListenerAccessGranted(component(context));
        }
        String allowed=Settings.Secure.getString(context.getContentResolver(),"enabled_notification_listeners");
        return allowed!=null && Arrays.asList(allowed.split(":")).contains(component(context).flattenToString());
    }
    @Override public void onCreate() {
        super.onCreate();instance=this;
        windows=(WindowManager)getSystemService(WINDOW_SERVICE);
        sessions=(MediaSessionManager)getSystemService(MEDIA_SESSION_SERVICE);
        prefs=getSharedPreferences("room",MODE_PRIVATE);prefs.registerOnSharedPreferenceChangeListener(preferenceListener);
        IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_SCREEN_ON);filter.addAction(Intent.ACTION_SCREEN_OFF);
        if(Build.VERSION.SDK_INT>=33)registerReceiver(screenReceiver,filter,Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(screenReceiver,filter);
    }
    @Override public void onListenerConnected() {
        connected=true;
        try {sessions.addOnActiveSessionsChangedListener(sessionListener,component(this),handler);refreshSessions();}
        catch(SecurityException denied) {lastError="Playback-state access is unavailable";connected=false;reconcile();}
    }
    @Override public void onListenerDisconnected() {
        connected=false;detachControllers();
        if(sessions!=null)sessions.removeOnActiveSessionsChangedListener(sessionListener);
        reconcile();
    }
    @Override public void onNotificationPosted(StatusBarNotification notification) { }
    @Override public void onNotificationRemoved(StatusBarNotification notification) { }
    private void refreshSessions() {
        if(!connected || destroyed)return;
        try {replaceControllers(sessions.getActiveSessions(component(this)));}
        catch(SecurityException denied) {connected=false;lastError="Playback-state access was revoked";reconcile();}
    }
    private void detachControllers() {
        for(MediaController c:controllers)c.unregisterCallback(playbackListener);
        controllers.clear();
    }
    private void replaceControllers(List<MediaController> values) {
        detachControllers();
        if(values!=null)for(MediaController c:values)if(VideoAircraftPolicy.supported(c.getPackageName())) {
            controllers.add(c);c.registerCallback(playbackListener,handler);
        }
        reconcile();
    }
    private String playingPackage() {
        String foreground=RemoteButtonService.connected?foregroundPackage:null;
        for(MediaController controller:controllers) {
            PlaybackState state=controller.getPlaybackState();
            if(state!=null && state.getState()==PlaybackState.STATE_PLAYING
                && (foreground==null || foreground.equals(controller.getPackageName())))return controller.getPackageName();
        }
        String candidate=fallbackCandidate();
        if(candidate!=null && candidate.equals(fallbackPlayer) && SystemClock.uptimeMillis()-fallbackAt<10000)return candidate;
        return null;
    }
    private String fallbackCandidate() {
        if(!connected || destroyed || launcherVisible || !interactive() || !Settings.canDrawOverlays(this)
            || !prefs.getBoolean("netflixAircraftOverlay",true))return null;
        String foreground=RemoteButtonService.connected?foregroundPackage:null;
        for(MediaController controller:controllers) {
            if(!VideoAircraftPolicy.jellyfin(controller.getPackageName())
                || (foreground!=null && !foreground.equals(controller.getPackageName())))continue;
            PlaybackState state=controller.getPlaybackState();
            if(state==null || state.getState()==PlaybackState.STATE_NONE)return controller.getPackageName();
        }
        return null;
    }
    private void probePlayback() {
        final String candidate=fallbackCandidate();if(candidate==null || probing)return;
        probing=true;
        playbackWorker.execute(() -> {
            boolean playing=false;String error="";
            try {playing=JellyfinPlaybackProbe.playing(this,candidate);}
            catch(Exception ignored) {error="Jellyfin playback check unavailable";}
            final boolean result=playing;final String failure=error;
            handler.post(() -> {
                probing=false;if(destroyed)return;
                fallbackPlayer=result && candidate.equals(fallbackCandidate())?candidate:"";
                fallbackAt=SystemClock.uptimeMillis();playbackError=failure;reconcile();
            });
        });
    }
    private boolean interactive() {
        PowerManager p=(PowerManager)getSystemService(POWER_SERVICE);return p!=null && p.isInteractive();
    }
    private boolean eligible() {
        String foreground=RemoteButtonService.connected?foregroundPackage:null;
        return connected && !destroyed && VideoAircraftPolicy.allowed(prefs.getBoolean("netflixAircraftOverlay",true),
            Settings.canDrawOverlays(this),interactive(),launcherVisible,foreground,playingPackage());
    }
    private void reconcile() {
        handler.removeCallbacks(playbackTick);
        if(fallbackCandidate()!=null)handler.postDelayed(playbackTick,3000);
        else fallbackPlayer="";
        boolean allowed=eligible();
        if(active==allowed)return;
        active=allowed;generation++;handler.removeCallbacks(pollTick);
        if(active)schedule();else hideOverlay();
    }
    private void schedule() {
        handler.removeCallbacks(pollTick);
        if(active)handler.postDelayed(pollTick,Math.max(0,nextFetch-SystemClock.uptimeMillis()));
    }
    private void poll() {
        reconcile();if(!active)return;
        if(busy) {handler.postDelayed(pollTick,1000);return;}
        if(SystemClock.uptimeMillis()<nextFetch){schedule();return;}
        busy=true;polls++;final int request=generation;
        nextFetch=SystemClock.uptimeMillis()+15000;
        worker.execute(() -> {
            JSONObject response=null;long retry=0;
            try {response=BridgeFlightsClient.fetch();}
            catch(BridgeFlightsClient.Unavailable e) {retry=e.retryMs;}
            catch(Exception ignored){}
            final JSONObject data=response;final long retryAfter=retry;
            handler.post(() -> {
                busy=false;if(destroyed)return;
                nextFetch=Math.max(nextFetch,SystemClock.uptimeMillis()+retryAfter);
                reconcile();
                if(!active || request!=generation){schedule();return;}
                boolean ok=false;
                if(data!=null)try {
                    double stamp=data.getDouble("now");long time=(long)(stamp<1e12?stamp*1000:stamp);
                    ok=Math.abs(System.currentTimeMillis()-time)<=BridgeFlightTracker.MAX_AGE;
                    if(ok) {
                        List<BridgeFlightTracker.Event> events=tracker.accept(data,System.currentTimeMillis());
                        detected+=events.size();if(!events.isEmpty())show(events);
                    }
                } catch(Exception ignored){}
                if(ok){failures=0;lastError="";}
                else {failures=Math.min(5,failures+1);lastError="Flight feed temporarily unavailable";
                    nextFetch=Math.max(nextFetch,SystemClock.uptimeMillis()+Math.min(300000,15000L<<failures));}
                schedule();
            });
        });
    }
    private void show(List<BridgeFlightTracker.Event> events) {
        if(!eligible())return;
        if(overlay==null) {
            overlay=new NetflixAircraftView(this,this::hideOverlay);
            Point size=new Point();windows.getDefaultDisplay().getRealSize(size);
            int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
            WindowManager.LayoutParams params=new WindowManager.LayoutParams(-1,Math.round(size.y*.22f),type,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                    |WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,
                PixelFormat.TRANSLUCENT);
            params.gravity=Gravity.TOP|Gravity.START;params.alpha=.75f;params.setTitle("Family Shelf · Video aircraft");
            try {windows.addView(overlay,params);shown++;}
            catch(RuntimeException denied) {overlay.stop();overlay=null;lastError="Android did not allow the overlay";return;}
        }
        lastFlight=events.get(events.size()-1).label;overlay.add(events);
    }
    private void hideOverlay() {
        if(overlay!=null) {overlay.stop();try {windows.removeViewImmediate(overlay);}catch(RuntimeException ignored){}overlay=null;}
    }
    @Override protected void dump(FileDescriptor fd,PrintWriter writer,String[] args) {
        JSONObject status=new JSONObject();
        try {String player=playingPackage();
            status.put("connected",connected).put("active",active)
            .put("netflixPlaying",VideoAircraftPolicy.netflix(player)).put("jellyfinPlaying",VideoAircraftPolicy.jellyfin(player))
            .put("jellyfinServerFallback",!fallbackPlayer.isEmpty()).put("playbackCheckError",playbackError)
            .put("playbackPlaying",player!=null).put("playingPackage",player==null?JSONObject.NULL:player)
            .put("screenOn",interactive()).put("overlayAllowed",Settings.canDrawOverlays(this))
            .put("launcherVisible",launcherVisible).put("foreground",foregroundPackage==null?JSONObject.NULL:foregroundPackage)
            .put("polls",polls).put("detected",detected).put("windowsShown",shown).put("visible",overlay!=null)
            .put("soundEnabled",false).put("aircraftScale",AircraftPainter.VIDEO_SCALE)
            .put("lastFlight",lastFlight).put("error",lastError);}
        catch(Exception ignored){}
        writer.println(status.toString());
    }
    @Override public void onDestroy() {
        destroyed=true;active=false;generation++;handler.removeCallbacksAndMessages(null);hideOverlay();
        detachControllers();if(sessions!=null)sessions.removeOnActiveSessionsChangedListener(sessionListener);
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceListener);unregisterReceiver(screenReceiver);
        worker.shutdownNow();playbackWorker.shutdownNow();if(instance==this)instance=null;
        super.onDestroy();
    }
}
