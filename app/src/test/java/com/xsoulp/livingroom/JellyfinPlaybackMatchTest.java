package com.xsoulp.livingroom;
import java.util.*;
import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;
public final class JellyfinPlaybackMatchTest {
    private final long now=1794038400000L;
    private JSONObject session() throws Exception {
        return new JSONObject().put("UserId","account").put("RemoteEndPoint","10.0.0.2")
            .put("Client","Android TV").put("LastPlaybackCheckIn","2026-11-07T08:00:00.1234567Z")
            .put("NowPlayingItem",new JSONObject().put("MediaType","Video"))
            .put("PlayState",new JSONObject().put("PositionTicks",10000000).put("IsPaused",false));
    }
    private boolean match(JSONObject session) {
        return JellyfinPlaybackMatch.playing(new JSONArray().put(session),"account",Collections.singleton("10.0.0.2"),"org.jellyfin.androidtv",now);
    }
    @Test public void recentLocalVideoQualifies() throws Exception {assertTrue(match(session()));}
    @Test public void pausedOrStoppedDoesNotQualify() throws Exception {
        JSONObject s=session();s.getJSONObject("PlayState").put("IsPaused",true);assertFalse(match(s));
        s=session();s.remove("NowPlayingItem");assertFalse(match(s));
    }
    @Test public void anotherDeviceAndAccountCannotTriggerThisBox() throws Exception {
        assertFalse(match(session().put("RemoteEndPoint","10.0.0.3")));
        assertFalse(match(session().put("UserId","other")));
    }
    @Test public void staleFutureAndMissingCheckInsFailClosed() throws Exception {
        for(String stamp:new String[]{"2026-11-07T07:59:00Z","2026-11-07T08:01:00Z","","broken"})
            assertFalse(match(session().put("LastPlaybackCheckIn",stamp)));
    }
    @Test public void clientMustMatchMediaController() throws Exception {
        assertFalse(match(session().put("Client","Jellyfin Web")));
        assertFalse(match(session().put("Client","Findroid")));
        assertTrue(JellyfinPlaybackMatch.playing(new JSONArray().put(session().put("Client","Findroid")),"account",Collections.singleton("10.0.0.2"),"dev.jdtech.jellyfin",now));
    }
    @Test public void audioAndUnknownPauseStateDoNotQualify() throws Exception {
        JSONObject s=session();s.getJSONObject("NowPlayingItem").put("MediaType","Audio");assertFalse(match(s));
        s=session();s.getJSONObject("PlayState").remove("IsPaused");assertFalse(match(s));
    }
    @Test public void ipv4MappedAddressIsRecognized() throws Exception {assertTrue(match(session().put("RemoteEndPoint","::ffff:10.0.0.2")));}
}
