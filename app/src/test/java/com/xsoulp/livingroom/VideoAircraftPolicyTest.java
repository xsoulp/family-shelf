package com.xsoulp.livingroom;
import org.junit.Test;
import static org.junit.Assert.*;
public final class VideoAircraftPolicyTest {
    @Test public void netflixClientsMayShowAircraftWhilePlaying() {
        for(String app:new String[]{"com.netflix.ninja","com.netflix.mediaclient"})
            assertTrue(VideoAircraftPolicy.allowed(true,true,true,false,app,app));
    }
    @Test public void allLauncherJellyfinClientsMayShowAircraftWhilePlaying() {
        for(String app:new String[]{"org.jellyfin.androidtv","org.jellyfin.mobile","dev.jdtech.jellyfin"})
            assertTrue(VideoAircraftPolicy.allowed(true,true,true,false,app,app));
    }
    @Test public void playingSessionSufficesWhenForegroundIsUnknown() {
        assertTrue(VideoAircraftPolicy.allowed(true,true,true,false,null,"org.jellyfin.androidtv"));
        assertFalse(VideoAircraftPolicy.allowed(true,true,true,false,null,null));
    }
    @Test public void staleBackgroundSessionCannotAuthorizeAnotherAppsOverlay() {
        assertFalse(VideoAircraftPolicy.allowed(true,true,true,false,"org.jellyfin.androidtv","com.netflix.ninja"));
        assertFalse(VideoAircraftPolicy.allowed(true,true,true,false,"com.netflix.ninja","org.jellyfin.androidtv"));
        assertFalse(VideoAircraftPolicy.allowed(true,true,true,false,"other.app","org.jellyfin.androidtv"));
    }
    @Test public void pausedUnsupportedAndLauncherNeverGetAnOverlay() {
        assertFalse(VideoAircraftPolicy.allowed(true,true,true,false,"org.jellyfin.androidtv",null));
        assertFalse(VideoAircraftPolicy.allowed(true,true,true,true,"org.jellyfin.androidtv","org.jellyfin.androidtv"));
        assertFalse(VideoAircraftPolicy.supported("org.jellyfin.unrelated"));
        assertFalse(VideoAircraftPolicy.supported("com.netflix.unrelated"));
    }
    @Test public void disabledDeniedAndSleepingAllStopTheFeature() {
        String app="org.jellyfin.androidtv";
        assertFalse(VideoAircraftPolicy.allowed(false,true,true,false,app,app));
        assertFalse(VideoAircraftPolicy.allowed(true,false,true,false,app,app));
        assertFalse(VideoAircraftPolicy.allowed(true,true,false,false,app,app));
    }
}
