package com.xsoulp.livingroom;

import org.json.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class BridgeFlightTrackerTest {
    private static final long NOW=1793995317501L;
    private JSONObject snapshot(long now,double x,double y) throws Exception {
        JSONObject a=new JSONObject().put("hex","abc123").put("flight","EXAMPLE123")
            .put("lat",BridgeFlightTracker.BRIDGE_LAT+y/111195).put("lon",BridgeFlightTracker.BRIDGE_LON+x/86770)
            .put("alt_baro",2500).put("seen_pos",.1).put("track",23);
        return new JSONObject().put("now",now).put("ac",new JSONArray().put(a));
    }
    @Test public void currentAirbornePositionAboveBridgeCreatesOneEvent() throws Exception {
        BridgeFlightTracker t=new BridgeFlightTracker();
        assertEquals(1,t.accept(snapshot(NOW,0,0),NOW).size());
        assertTrue(t.accept(snapshot(NOW+15000,10,10),NOW+15000).isEmpty());
    }
    @Test public void detectsARealCrossingBetweenTwoPolls() throws Exception {
        BridgeFlightTracker t=new BridgeFlightTracker();
        assertTrue(t.accept(snapshot(NOW,-1000,0),NOW).isEmpty());
        assertEquals(1,t.accept(snapshot(NOW+15000,1000,0),NOW+15000).size());
    }
    @Test public void outsideBridgeOrAirportGroundDoesNotTrigger() throws Exception {
        BridgeFlightTracker t=new BridgeFlightTracker();assertTrue(t.accept(snapshot(NOW,2500,3000),NOW).isEmpty());
        JSONObject ground=snapshot(NOW,0,0);ground.getJSONArray("ac").getJSONObject(0).put("alt_baro","ground");
        assertTrue(t.accept(ground,NOW).isEmpty());
    }
    @Test public void staleResponseAndOldOrMissingPositionsDoNotTrigger() throws Exception {
        BridgeFlightTracker t=new BridgeFlightTracker();assertTrue(t.accept(snapshot(NOW,0,0),NOW+60000).isEmpty());
        JSONObject old=snapshot(NOW,0,0);old.getJSONArray("ac").getJSONObject(0).put("seen_pos",40);
        assertTrue(t.accept(old,NOW).isEmpty());
        old=snapshot(NOW,0,0);old.getJSONArray("ac").getJSONObject(0).remove("lat");assertTrue(t.accept(old,NOW).isEmpty());
    }
    @Test public void doesNotInventCrossingsAcrossLongGapsOrImpossibleGpsJumps() throws Exception {
        BridgeFlightTracker t=new BridgeFlightTracker();t.accept(snapshot(NOW,-1000,0),NOW);
        assertTrue(t.accept(snapshot(NOW+45000,1000,0),NOW+45000).isEmpty());
        t=new BridgeFlightTracker();t.accept(snapshot(NOW,-100000,0),NOW);
        assertTrue(t.accept(snapshot(NOW+15000,100000,0),NOW+15000).isEmpty());
    }
    @Test public void secondsAndMillisecondsProviderTimestampsAreSupported() throws Exception {
        JSONObject j=snapshot(NOW,0,0);j.put("now",NOW/1000.0);
        assertEquals(1,new BridgeFlightTracker().accept(j,NOW).size());
    }
    @Test public void aLaterDistinctPassCanAnimateAgainWithoutRepeatingAHover() throws Exception {
        BridgeFlightTracker t=new BridgeFlightTracker();assertEquals(1,t.accept(snapshot(NOW,0,0),NOW).size());
        for(int seconds=15;seconds<=90;seconds+=15)
            assertTrue(t.accept(snapshot(NOW+seconds*1000,10,10),NOW+seconds*1000).isEmpty());
        assertTrue(t.accept(snapshot(NOW+105000,1000,0),NOW+105000).isEmpty());
        assertEquals(1,t.accept(snapshot(NOW+120000,0,0),NOW+120000).size());
    }
    @Test public void corridorGeometryRejectsNearButNonCrossingPaths() {
        assertTrue(BridgeFlightTracker.intersects(-1000,0,1000,0));
        assertFalse(BridgeFlightTracker.intersects(-1000,2500,1000,2500));
        assertFalse(BridgeFlightTracker.intersects(1000,-2000,1000,2000));
    }
}
