package com.xsoulp.livingroom;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.SoundPool;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;

/** Quiet, local looping music; never continues outside the foreground launcher. */
final class AmbientMusic implements AmbientMusicState.Output {
    private static final float LEVEL=.13f,OUTDOOR_LEVEL=.07f,WHOOSH_LEVEL=.16f;
    private final Context context;
    private final AudioManager audio;
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final AudioAttributes attributes=new AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build();
    private final AmbientMusicState state;
    private final AudioManager.OnAudioFocusChangeListener focusListener;
    private AudioFocusRequest focusRequest;
    private MediaPlayer player;
    private boolean prepared,wantsPlayback;
    private int currentMode=-1,whooshSample,whooshStream;
    private volatile boolean whooshLoaded;
    private final SoundPool effects;
    private Runnable fade,pendingEnable,pendingForeground;
    private static long lastPlayerStopMs;
    AmbientMusic(Context context) {
        this.context=context.getApplicationContext();
        audio=(AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
        effects=new SoundPool.Builder().setMaxStreams(1).setAudioAttributes(attributes).build();
        effects.setOnLoadCompleteListener((pool,sample,status) -> {if(sample==whooshSample)whooshLoaded=status==0;});
        whooshSample=effects.load(context,R.raw.airplane_whoosh,1);
        state=new AmbientMusicState(this);
        focusListener=change -> {
            if(change==AudioManager.AUDIOFOCUS_GAIN) state.focusGained();
            else state.focusLost(change==AudioManager.AUDIOFOCUS_LOSS);
        };
    }
    void setForeground(boolean value) {
        if(pendingForeground!=null)handler.removeCallbacks(pendingForeground);
        pendingForeground=null;
        long settle=lastPlayerStopMs==0?0:Math.max(0,500-(android.os.SystemClock.uptimeMillis()-lastPlayerStopMs));
        if(value && settle>0) {
            pendingForeground=() -> {pendingForeground=null;state.setForeground(true);};
            handler.postDelayed(pendingForeground,settle);
        } else state.setForeground(value);
    }
    void setEnabled(boolean value) {
        if(pendingEnable!=null)handler.removeCallbacks(pendingEnable);
        pendingEnable=null;
        // AudioFlinger may briefly report our just-released player as active.
        // Defer unmute so the existing-other-app guard can stay in place.
        long settle=lastPlayerStopMs==0?0:Math.max(0,500-(android.os.SystemClock.uptimeMillis()-lastPlayerStopMs));
        if(value && settle>0) {
            pendingEnable=() -> {pendingEnable=null;state.setEnabled(true);};
            handler.postDelayed(pendingEnable,settle);
        } else state.setEnabled(value);
    }
    void setKids(boolean value) { state.setKids(value); }
    void setOutdoor(boolean value) {state.setOutdoor(value);}
    boolean effectsAudible() {return state.audible();}
    void flightWhoosh() {
        if(currentMode!=2 || !state.audible() || !whooshLoaded)return;
        if(whooshStream!=0)effects.stop(whooshStream);
        whooshStream=effects.play(whooshSample,WHOOSH_LEVEL,WHOOSH_LEVEL,1,0,1);
    }
    void release() {
        if(pendingEnable!=null)handler.removeCallbacks(pendingEnable);
        if(pendingForeground!=null)handler.removeCallbacks(pendingForeground);
        pendingForeground=null;pendingEnable=null;state.setForeground(false);state.release();effects.release();
    }
    @Override public boolean acquireFocus() {
        // Home should not interrupt a music app already playing in the background.
        if(audio==null || audio.isMusicActive()) return false;
        try {
            int result;
            if(Build.VERSION.SDK_INT>=26) {
                focusRequest=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
                    .setOnAudioFocusChangeListener(focusListener,handler).build();
                result=audio.requestAudioFocus(focusRequest);
            } else result=audio.requestAudioFocus(focusListener,AudioManager.STREAM_MUSIC,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
            return result==AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
        } catch(RuntimeException unavailable) { return false; }
    }
    @Override public void abandonFocus() {
        if(audio==null)return;
        if(Build.VERSION.SDK_INT>=26 && focusRequest!=null) audio.abandonAudioFocusRequest(focusRequest);
        else audio.abandonAudioFocus(focusListener);
        focusRequest=null;
    }
    @Override public void play(boolean kids) {playMode(kids?1:0);}
    @Override public void playOutdoor() {playMode(2);}
    private void playMode(int mode) {
        wantsPlayback=true;
        if(player!=null && currentMode==mode) {
            if(prepared && !player.isPlaying()) { player.start();fadeIn(player); }
            return;
        }
        stop();wantsPlayback=true;currentMode=mode;
        MediaPlayer next=new MediaPlayer();player=next;
        next.setAudioAttributes(attributes);next.setVolume(0,0);
        next.setOnPreparedListener(ready -> {
            if(ready!=player)return;
            prepared=true;ready.setLooping(true);
            if(wantsPlayback) { ready.start();fadeIn(ready); }
        });
        next.setOnErrorListener((failed,what,extra) -> { if(failed==player)stop();return true; });
        int resource=mode==1?R.raw.kids_ambient:R.raw.adults_ambient;
        try(AssetFileDescriptor descriptor=mode==2?context.getAssets().openFd("outdoor-ambient.mp3"):
                context.getResources().openRawResourceFd(resource)) {
            next.setDataSource(descriptor.getFileDescriptor(),descriptor.getStartOffset(),descriptor.getLength());
            next.prepareAsync();
        } catch(Exception unavailable) { stop(); }
    }
    private void fadeIn(MediaPlayer target) {
        cancelFade();
        final long start=android.os.SystemClock.uptimeMillis();
        final float level=currentMode==2?OUTDOOR_LEVEL:LEVEL;
        fade=new Runnable() {
            @Override public void run() {
                if(target!=player || !prepared || !wantsPlayback)return;
                float value=Math.min(1,(android.os.SystemClock.uptimeMillis()-start)/650f)*level;
                target.setVolume(value,value);
                if(value<level)handler.postDelayed(this,32);
            }
        };
        handler.post(fade);
    }
    private void cancelFade() { if(fade!=null)handler.removeCallbacks(fade);fade=null; }
    @Override public void pause() {
        wantsPlayback=false;cancelFade();stopWhoosh();
        if(player!=null && prepared) { player.pause();player.setVolume(0,0); }
    }
    private void stopWhoosh() {if(whooshStream!=0){effects.stop(whooshStream);whooshStream=0;}}
    @Override public void stop() {
        wantsPlayback=false;cancelFade();prepared=false;stopWhoosh();
        if(player!=null) {
            lastPlayerStopMs=android.os.SystemClock.uptimeMillis();
            MediaPlayer old=player;player=null;old.release();
        }
    }
}
