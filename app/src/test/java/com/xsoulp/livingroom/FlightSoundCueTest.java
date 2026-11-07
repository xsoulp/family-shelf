package com.xsoulp.livingroom;

import org.junit.Test;
import static org.junit.Assert.*;

public final class FlightSoundCueTest {
    @Test public void playsOnceAsPlaneApproachesCentre() {
        FlightSoundCue c=new FlightSoundCue();c.begin();
        assertFalse(c.at(0));assertFalse(c.at(.39f));assertTrue(c.at(.4f));
        assertFalse(c.at(.41f));assertFalse(c.at(.6f));assertFalse(c.at(.99f));
    }
    @Test public void anotherPlaneGetsItsOwnSound() {
        FlightSoundCue c=new FlightSoundCue();c.begin();assertTrue(c.at(.5f));
        c.begin();assertFalse(c.at(.1f));assertTrue(c.at(.5f));assertFalse(c.at(.6f));
    }
    @Test public void hiddenOrFinishedPlaneNeverCausesADelayedSound() {
        FlightSoundCue c=new FlightSoundCue();assertFalse(c.at(.5f));
        c.begin();c.stop();assertFalse(c.at(.5f));
        c.begin();assertFalse(c.at(1));
    }
}
