package com.xsoulp.livingroom;

import org.junit.Test;
import static org.junit.Assert.*;

public final class AmbientMusicStateTest {
    private static final class Fake implements AmbientMusicState.Output {
        int focus,abandoned,plays,pauses,outdoorPlays;boolean playing,kids,grant=true;
        public boolean acquireFocus() {focus++;return grant;}
        public void abandonFocus() {abandoned++;}
        public void playOutdoor() {playing=true;outdoorPlays++;plays++;}
        public void play(boolean value) {playing=true;kids=value;plays++;}
        public void pause() {playing=false;pauses++;}
        public void stop() {playing=false;}
    }
    @Test public void mutedOrBackgroundLauncherNeverRequestsFocus() {
        Fake f=new Fake();AmbientMusicState s=new AmbientMusicState(f);
        s.setKids(false);assertEquals(0,f.focus);
        s.setEnabled(false);s.setForeground(true);assertEquals(0,f.focus);assertFalse(f.playing);
    }
    @Test public void foregroundStartsKidsAndCategoryNavigationDoesNotRestartIt() {
        Fake f=new Fake();AmbientMusicState s=new AmbientMusicState(f);s.setForeground(true);
        assertTrue(f.playing&&f.kids);
        for(int i=0;i<6;i++)s.setKids(true);
        assertEquals(1,f.focus);assertEquals(1,f.plays);
    }
    @Test public void adultsChangesTrackWithTheSameFocusLease() {
        Fake f=new Fake();AmbientMusicState s=new AmbientMusicState(f);s.setForeground(true);s.setKids(false);
        assertTrue(f.playing);assertFalse(f.kids);assertEquals(2,f.plays);assertEquals(1,f.focus);
    }
    @Test public void leavingLauncherStopsAndAbandonsFocus() {
        Fake f=new Fake();AmbientMusicState s=new AmbientMusicState(f);s.setForeground(true);s.setForeground(false);
        assertFalse(f.playing);assertEquals(1,f.abandoned);
        s.focusGained();assertFalse(f.playing);
    }
    @Test public void transientLossPausesUntilFocusReturns() {
        Fake f=new Fake();AmbientMusicState s=new AmbientMusicState(f);s.setForeground(true);s.focusLost(false);
        s.setKids(false);assertFalse(f.playing);assertEquals(1,f.pauses);
        s.focusGained();assertTrue(f.playing);assertFalse(f.kids);assertEquals(1,f.focus);
    }
    @Test public void permanentLossDoesNotStealFocusBackDuringNavigation() {
        Fake f=new Fake();AmbientMusicState s=new AmbientMusicState(f);s.setForeground(true);s.focusLost(true);
        s.setKids(false);s.focusGained();assertFalse(f.playing);assertEquals(1,f.focus);
        s.setForeground(false);s.setForeground(true);assertTrue(f.playing);assertEquals(2,f.focus);
    }
    @Test public void focusDeniedKeepsLauncherSilentUntilNextForegroundEntry() {
        Fake f=new Fake();f.grant=false;AmbientMusicState s=new AmbientMusicState(f);s.setForeground(true);
        s.setKids(false);assertFalse(f.playing);assertEquals(1,f.focus);
        f.grant=true;s.setForeground(false);s.setForeground(true);assertTrue(f.playing);
    }
    @Test public void muteStopsImmediatelyAndUnmuteUsesCurrentRoom() {
        Fake f=new Fake();AmbientMusicState s=new AmbientMusicState(f);s.setForeground(true);
        s.setEnabled(false);s.setKids(false);assertFalse(f.playing);assertEquals(1,f.abandoned);
        s.setEnabled(true);assertTrue(f.playing);assertFalse(f.kids);
    }
    @Test public void pauseDuringTransientLossPreventsLaterBackgroundResume() {
        Fake f=new Fake();AmbientMusicState s=new AmbientMusicState(f);s.setForeground(true);
        s.focusLost(false);s.setForeground(false);s.focusGained();assertFalse(f.playing);
    }
    @Test public void enteringOutdoorChangesTrackWithTheSameFocusLease() {
        Fake f=new Fake();AmbientMusicState s=new AmbientMusicState(f);s.setForeground(true);s.setOutdoor(true);
        assertEquals(1,f.focus);assertEquals(1,f.outdoorPlays);assertTrue(s.audible());
        for(int i=0;i<10;i++)s.setOutdoor(true);
        assertEquals(2,f.plays);
        s.setKids(false);assertEquals(2,f.plays);
        s.setOutdoor(false);assertFalse(f.kids);assertEquals(3,f.plays);assertEquals(1,f.focus);
    }
    @Test public void outdoorEffectsFollowMuteFocusAndForeground() {
        Fake f=new Fake();AmbientMusicState s=new AmbientMusicState(f);s.setOutdoor(true);s.setForeground(true);
        assertTrue(s.audible());s.focusLost(false);assertFalse(s.audible());
        s.focusGained();assertTrue(s.audible());s.setEnabled(false);assertFalse(s.audible());
        s.setEnabled(true);assertTrue(s.audible());s.setForeground(false);assertFalse(s.audible());
    }
    @Test public void interruptedOutdoorDoesNotStartAnotherTrackUntilFocusReturns() {
        Fake f=new Fake();AmbientMusicState s=new AmbientMusicState(f);s.setForeground(true);s.focusLost(false);
        int before=f.plays;s.setOutdoor(true);assertEquals(before,f.plays);
        s.focusGained();assertEquals(1,f.outdoorPlays);assertTrue(s.audible());
    }
}
