package com.xsoulp.livingroom;

import org.junit.Test;
import static org.junit.Assert.*;

public final class IdleScreensaverStateTest {
    @Test public void startsAfterExactlyTwoMinutesWithoutInput() {
        IdleScreensaverState s=new IdleScreensaverState();s.resume(500,true);
        assertFalse(s.due(120499,false));assertTrue(s.due(120500,false));
    }
    @Test public void inputRestartsTheTwoMinuteWait() {
        IdleScreensaverState s=new IdleScreensaverState();s.resume(0,true);s.input(119000);
        assertFalse(s.due(120000,false));assertEquals(119000,s.remaining(120000));
        assertTrue(s.due(239000,false));
    }
    @Test public void doesNotCoverAnotherAppOrASleepingScreen() {
        IdleScreensaverState s=new IdleScreensaverState();s.resume(0,true);s.pause();
        assertFalse(s.due(300000,false));s.resume(300000,false);assertFalse(s.due(600000,false));
        s.screen(true,600000);assertFalse(s.due(600001,false));assertTrue(s.due(720000,false));
    }
    @Test public void wakeInputIsConsumedAndNextInputWorksNormally() {
        IdleScreensaverState s=new IdleScreensaverState();s.resume(0,true);s.show();
        assertTrue(s.input(130000));assertFalse(s.showing());assertFalse(s.input(130100));
        assertFalse(s.due(130101,false));
    }
    @Test public void dialogsAndAnAlreadyVisibleSaverDoNotTriggerAgain() {
        IdleScreensaverState s=new IdleScreensaverState();s.resume(0,true);
        assertFalse(s.due(200000,true));s.show();assertFalse(s.due(200000,false));
    }
}
