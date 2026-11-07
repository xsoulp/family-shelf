package com.xsoulp.livingroom;
import java.time.Instant;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.*;
public final class HourlyBellClockTest {
    private long at(String time) {return Instant.parse(time).toEpochMilli();}
    private final TimeZone lisbon=TimeZone.getTimeZone("Europe/Lisbon");
    @Test public void everyHourMapsToOneThroughTwelve() {
        long midnight=at("2026-11-07T00:00:00Z");
        for(int hour=0;hour<24;hour++) {
            long boundary=midnight+hour*3600000L;HourlyBellClock c=new HourlyBellClock(Long.MIN_VALUE);
            assertEquals(0,c.poll(boundary-500,lisbon,true));
            assertEquals(hour%12==0?12:hour%12,c.poll(boundary+10,lisbon,true));
            assertEquals(0,c.poll(boundary+500,lisbon,true));
        }
    }
    @Test public void mutedBoundaryNeverReplaysOnUnmute() {
        long t=at("2026-11-07T15:00:00Z");HourlyBellClock c=new HourlyBellClock(Long.MIN_VALUE);
        c.poll(t-500,lisbon,true);assertEquals(0,c.poll(t,lisbon,false));assertEquals(0,c.poll(t+500,lisbon,true));
    }
    @Test public void openingOrResumingDoesNotReplayMissedHours() {
        long t=at("2026-11-07T15:00:00Z");HourlyBellClock c=new HourlyBellClock(Long.MIN_VALUE);
        assertEquals(0,c.poll(t+100,lisbon,true));c.reset();assertEquals(0,c.poll(t+1000,lisbon,true));
    }
    @Test public void lateTicksAndClockJumpsFailClosed() {
        long t=at("2026-11-07T15:00:00Z");HourlyBellClock c=new HourlyBellClock(Long.MIN_VALUE);
        c.poll(t-500,lisbon,true);assertEquals(0,c.poll(t+2500,lisbon,true));
        c.reset();c.poll(t-3600000,lisbon,true);assertEquals(0,c.poll(t,lisbon,true));
        assertEquals(0,c.poll(t-3600000,lisbon,true));
    }
    @Test public void savedBoundarySuppressesActivityRecreationDuplicates() {
        long t=at("2026-11-07T15:00:00Z");HourlyBellClock c=new HourlyBellClock(t);
        c.poll(t-500,lisbon,true);assertEquals(0,c.poll(t,lisbon,true));
    }
    @Test public void repeatedDstHourCanStrikeAtBothRealBoundaries() {
        HourlyBellClock c=new HourlyBellClock(Long.MIN_VALUE);
        long first=at("2026-10-25T00:00:00Z"),second=at("2026-10-25T01:00:00Z");
        c.poll(first-500,lisbon,true);assertEquals(1,c.poll(first,lisbon,true));
        c.poll(second-500,lisbon,true);assertEquals(1,c.poll(second,lisbon,true));
    }
    @Test public void springClockJumpUsesTheHourActuallyShown() {
        HourlyBellClock c=new HourlyBellClock(Long.MIN_VALUE);long t=at("2026-03-29T01:00:00Z");
        c.poll(t-500,lisbon,true);assertEquals(2,c.poll(t,lisbon,true));
    }
}
