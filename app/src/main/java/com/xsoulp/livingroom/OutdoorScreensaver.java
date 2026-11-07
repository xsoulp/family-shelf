package com.xsoulp.livingroom;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.*;
import android.view.*;
import android.widget.FrameLayout;
import java.util.concurrent.*;
import org.json.JSONObject;

/** Foreground-only idle screen. Network work is bounded and never runs on the UI thread. */
final class OutdoorScreensaver {
    private final Activity activity;
    private final OutdoorScreensaverView view;
    private final OutdoorSceneConfig config;
    private final IdleScreensaverState idle=new IdleScreensaverState();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newFixedThreadPool(2);
    private final BridgeFlightTracker tracker=new BridgeFlightTracker();
    private final SharedPreferences prefs,cache;
    private final Runnable changed;
    interface DialogCheck { boolean showing(); }
    private final DialogCheck dialogShowing;
    private volatile boolean destroyed;
    private boolean resumed,touchConsumed,weatherBusy,flightBusy;
    private int wakeKey=-1,generation,flightFailures;
    private long weatherRequested;
    private OutdoorWeather weather;
    private final Runnable idleCheck=this::checkIdle;
    private final Runnable weatherTick=this::refreshWeather;
    private final Runnable flightTick=this::refreshFlights;
    OutdoorScreensaver(Activity activity,FrameLayout container,Runnable changed,Runnable flightSound,DialogCheck dialogShowing) {
        this.activity=activity;this.changed=changed;this.dialogShowing=dialogShowing;
        config=OutdoorSceneConfig.load(activity);prefs=activity.getSharedPreferences("room",Context.MODE_PRIVATE);
        cache=activity.getSharedPreferences("outdoor-weather",Context.MODE_PRIVATE);
        view=new OutdoorScreensaverView(activity);view.setFlightSound(flightSound);container.addView(view,new FrameLayout.LayoutParams(-1,-1));
        if(config!=null)try {
            JSONObject j=new JSONObject(cache.getString(cacheKey(),""));weather=new OutdoorWeather(j);view.setWeather(weather);
        } catch(Exception ignored) { /* A missing/corrupt cache never supplies invented weather. */ }
    }
    private String cacheKey() {return "weather:"+config.latitude+":"+config.longitude;}
    boolean configured() {return config!=null;}
    boolean enabled() {return prefs.getBoolean("outdoorScreensaver",true);}
    boolean showing() {return idle.showing();}
    String place() {return config==null?"Not configured":config.label;}
    void setEnabled(boolean value) {prefs.edit().putBoolean("outdoorScreensaver",value).apply();hide();reset();}
    void resume() {resumed=true;idle.resume(SystemClock.uptimeMillis(),interactive());reset();}
    void pause() {resumed=false;hide();idle.pause();handler.removeCallbacks(idleCheck);handler.removeCallbacks(weatherTick);handler.removeCallbacks(flightTick);}
    void screen(boolean on) {hide();idle.screen(on,SystemClock.uptimeMillis());if(on)reset();else {handler.removeCallbacks(idleCheck);handler.removeCallbacks(weatherTick);handler.removeCallbacks(flightTick);}}
    private boolean interactive() {PowerManager p=(PowerManager)activity.getSystemService(Context.POWER_SERVICE);return p!=null&&p.isInteractive();}
    void activity() {if(!showing())reset();}
    private void reset() {
        idle.input(SystemClock.uptimeMillis());handler.removeCallbacks(idleCheck);
        if(resumed && interactive() && enabled() && configured())handler.postDelayed(idleCheck,IdleScreensaverState.TIMEOUT_MS);
    }
    private void checkIdle() {
        if(destroyed || !resumed || !interactive() || !enabled() || !configured())return;
        if(idle.due(SystemClock.uptimeMillis(),dialogShowing.showing()))show(-1);
        else if(!showing())handler.postDelayed(idleCheck,Math.max(1000,idle.remaining(SystemClock.uptimeMillis())));
    }
    void returnToRoom() {hide();reset();}
    void preview(int phase) {if(configured() && resumed)show(phase);}
    private void show(int phase) {
        generation++;idle.show();view.start(config,phase);
        activity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        handler.removeCallbacks(idleCheck);changed.run();refreshWeather();refreshFlights();
    }
    private void hide() {
        boolean visible=showing();generation++;view.stop();idle.input(SystemClock.uptimeMillis());
        handler.removeCallbacks(weatherTick);handler.removeCallbacks(flightTick);
        activity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(visible)changed.run();
    }
    boolean key(KeyEvent event) {
        int key=event.getKeyCode();
        if(wakeKey==key) {if(event.getAction()==KeyEvent.ACTION_UP)wakeKey=-1;reset();return true;}
        if(showing()) {hide();wakeKey=event.getAction()==KeyEvent.ACTION_DOWN?key:-1;reset();return true;}
        reset();return false;
    }
    boolean touch(MotionEvent event) {
        if(showing() && event.getActionMasked()==MotionEvent.ACTION_DOWN) {hide();touchConsumed=true;}
        if(touchConsumed) {
            if(event.getActionMasked()==MotionEvent.ACTION_UP || event.getActionMasked()==MotionEvent.ACTION_CANCEL)touchConsumed=false;
            reset();return true;
        }
        if(event.getActionMasked()==MotionEvent.ACTION_DOWN)reset();return false;
    }
    private void refreshWeather() {
        handler.removeCallbacks(weatherTick);
        if(destroyed || !showing() || config==null)return;
        long now=System.currentTimeMillis();
        if(weatherBusy || (weather!=null && now-weatherRequested<15*60_000L)) {handler.postDelayed(weatherTick,60_000);return;}
        weatherBusy=true;weatherRequested=now;final int request=generation;
        worker.execute(() -> {
            JSONObject result=null;OutdoorWeather value=null;
            try {result=OutdoorWeatherClient.fetch(config.latitude,config.longitude);value=new OutdoorWeather(result);}
            catch(Exception ignored) { /* Retain and label a saved observation, or show unavailable. */ }
            final JSONObject json=result;final OutdoorWeather observation=value;
            handler.post(() -> {
                weatherBusy=false;if(destroyed)return;
                if(observation!=null) {weather=observation;cache.edit().putString(cacheKey(),json.toString()).apply();view.setWeather(weather);}
                if(request==generation && showing())handler.postDelayed(weatherTick,observation==null?60_000:15*60_000L);
                if(observation==null)weatherRequested=0;
            });
        });
    }
    private void refreshFlights() {
        handler.removeCallbacks(flightTick);
        if(destroyed || !showing())return;
        if(flightBusy) {handler.postDelayed(flightTick,15000);return;}
        flightBusy=true;final int request=generation;
        worker.execute(() -> {
            JSONObject result=null;long requestedRetry=0;
            try {result=BridgeFlightsClient.fetch();}
            catch(BridgeFlightsClient.Unavailable error) {requestedRetry=error.retryMs;}
            catch(Exception ignored){}
            final long retryAfter=requestedRetry;
            final JSONObject response=result;
            handler.post(() -> {
                flightBusy=false;if(destroyed || request!=generation || !showing())return;
                boolean ok=false;
                if(response!=null)try {
                    view.addFlights(tracker.accept(response,System.currentTimeMillis()));
                    double t=response.getDouble("now");long time=(long)(t<1e12?t*1000:t);
                    ok=Math.abs(System.currentTimeMillis()-time)<=BridgeFlightTracker.MAX_AGE;
                } catch(Exception ignored){}
                if(ok) {flightFailures=0;view.flightStatus("live bridge area");}
                else {flightFailures=Math.min(5,flightFailures+1);view.flightStatus("temporarily unavailable");}
                handler.postDelayed(flightTick,ok?15000:Math.max(retryAfter,Math.min(300000,15000L << flightFailures)));
            });
        });
    }
    void release() {destroyed=true;pause();view.release();worker.shutdownNow();handler.removeCallbacksAndMessages(null);}
}
