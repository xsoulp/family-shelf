package com.xsoulp.livingroom;

import android.content.*;
import android.media.*;
import android.os.*;
import java.util.TimeZone;
import org.json.JSONObject;

/** Screensaver-only hour strikes; reuses the launcher's audio-focus policy without interrupting video. */
final class HourlyBell {
    interface Audible {boolean allowed();}
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Context context;
    private final SharedPreferences prefs;
    private final Audible audible;
    private final SoundPool sound;
    private final HourlyBellClock clock;
    private final String configuredZone;
    private final int sample;
    private final int[] streams=new int[4];
    private volatile boolean loaded;
    private boolean foreground,released;
    private int remaining,strikes,lastCount,slot;
    private final Runnable tick=this::tick,ring=this::ring;
    private final SharedPreferences.OnSharedPreferenceChangeListener changed=(p,key) -> {
        if(("hourlyBell".equals(key) && !p.getBoolean(key,true))
            || ("backgroundMusic".equals(key) && !p.getBoolean(key,true)))cancel();
    };
    private final BroadcastReceiver timeChanged=new BroadcastReceiver() {
        @Override public void onReceive(Context c,Intent i) {clock.reset();cancel();}
    };
    HourlyBell(Context context,Audible audible) {
        this.context=context;this.audible=audible;prefs=context.getSharedPreferences("room",Context.MODE_PRIVATE);
        clock=new HourlyBellClock(prefs.getLong("hourlyBellLastStrike",Long.MIN_VALUE));
        OutdoorSceneConfig config=OutdoorSceneConfig.load(context);configuredZone=config==null?null:config.zone;
        sound=new SoundPool.Builder().setMaxStreams(4).setAudioAttributes(new AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        sound.setOnLoadCompleteListener((pool,id,status) -> {loaded=status==0;});
        sample=sound.load(context,R.raw.church_bell,1);
        prefs.registerOnSharedPreferenceChangeListener(changed);
        IntentFilter filter=new IntentFilter();filter.addAction(Intent.ACTION_TIME_CHANGED);filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        if(Build.VERSION.SDK_INT>=33)context.registerReceiver(timeChanged,filter,Context.RECEIVER_NOT_EXPORTED);
        else context.registerReceiver(timeChanged,filter);
    }
    void foreground(boolean value) {
        if(foreground==value)return;
        foreground=value;clock.reset();handler.removeCallbacks(tick);
        if(value)tick();else cancel();
    }
    private boolean allowed() {return !released && foreground && loaded && prefs.getBoolean("hourlyBell",true)
        && prefs.getBoolean("backgroundMusic",true) && audible.allowed();}
    private void tick() {
        if(released || !foreground)return;
        long now=System.currentTimeMillis();
        int count=clock.poll(now,configuredZone==null?TimeZone.getDefault():TimeZone.getTimeZone(configuredZone),allowed());
        if(count>0 && clock.lastStrike()>prefs.getLong("hourlyBellLastStrike",Long.MIN_VALUE)) {
            prefs.edit().putLong("hourlyBellLastStrike",clock.lastStrike()).apply();start(count);
        }
        if(!allowed())cancel();
        handler.postDelayed(tick,500-now%500);
    }
    boolean preview() {if(!allowed())return false;start(1);return true;}
    private void start(int count) {cancel();lastCount=count;remaining=count;ring();}
    private void ring() {
        if(remaining<=0 || !allowed()){cancel();return;}
        int index=slot++%streams.length;
        if(streams[index]!=0)sound.stop(streams[index]);
        streams[index]=sound.play(sample,.17f,.17f,1,0,1);
        if(streams[index]!=0)strikes++;
        remaining--;
        if(remaining>0)handler.postDelayed(ring,2400);
    }
    private void cancel() {
        handler.removeCallbacks(ring);remaining=0;
        for(int i=0;i<streams.length;i++)if(streams[i]!=0){sound.stop(streams[i]);streams[i]=0;}
    }
    JSONObject status() {
        JSONObject out=new JSONObject();try {out.put("loaded",loaded).put("foreground",foreground)
            .put("allowed",allowed()).put("strikes",strikes).put("remaining",remaining).put("lastCount",lastCount);}
        catch(Exception ignored){}return out;
    }
    void release() {
        released=true;foreground=false;handler.removeCallbacks(tick);cancel();
        prefs.unregisterOnSharedPreferenceChangeListener(changed);context.unregisterReceiver(timeChanged);sound.release();
    }
}
