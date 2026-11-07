package com.xsoulp.livingroom;

import java.util.Calendar;
import java.util.TimeZone;

/** Only an observed hour boundary may chime; missed hours and clock jumps never catch up. */
final class HourlyBellClock {
    private long previous=Long.MIN_VALUE,lastStrike;
    HourlyBellClock(long lastStrike) {this.lastStrike=lastStrike;}
    void reset() {previous=Long.MIN_VALUE;}
    long lastStrike() {return lastStrike;}
    int poll(long now,TimeZone zone,boolean audible) {
        long before=previous;previous=now;
        if(before==Long.MIN_VALUE || now<before || now-before>5000)return 0;
        Calendar clock=Calendar.getInstance(zone);clock.setTimeInMillis(now);
        long boundary=now-clock.get(Calendar.MINUTE)*60000L-clock.get(Calendar.SECOND)*1000L-clock.get(Calendar.MILLISECOND);
        if(!audible || before>=boundary || now-boundary>2000 || boundary<=lastStrike)return 0;
        lastStrike=boundary;
        int hour=clock.get(Calendar.HOUR);return hour==0?12:hour;
    }
}
