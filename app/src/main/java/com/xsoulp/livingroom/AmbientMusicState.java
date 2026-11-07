package com.xsoulp.livingroom;

/** Lifecycle/focus policy independent of Android's player, tested with a fake output. */
final class AmbientMusicState {
    interface Output {
        boolean acquireFocus();
        void abandonFocus();
        void play(boolean kids);
        void playOutdoor();
        void pause();
        void stop();
    }
    private final Output output;
    private boolean enabled=true,foreground,kids=true,outdoor,held,interrupted,blocked;
    private Integer playingMode;
    AmbientMusicState(Output output) { this.output=output; }
    void setEnabled(boolean value) {
        if(enabled!=value) blocked=false;
        enabled=value; reconcile();
    }
    void setForeground(boolean value) {
        if(foreground!=value) blocked=false;
        foreground=value; reconcile();
    }
    void setKids(boolean value) { kids=value; reconcile(); }
    void setOutdoor(boolean value) {outdoor=value;reconcile();}
    boolean audible() {return enabled && foreground && held && !interrupted && !blocked;}
    void focusGained() { if(!held)return;interrupted=false;reconcile(); }
    void focusLost(boolean permanent) {
        if(!held)return;
        if(permanent) { blocked=true;release(); }
        else { interrupted=true;playingMode=null;output.pause(); }
    }
    void release() {
        output.stop();
        if(held) { held=false;output.abandonFocus(); }
        interrupted=false;playingMode=null;
    }
    private void reconcile() {
        if(!enabled || !foreground || blocked) { release();return; }
        if(!held) {
            held=output.acquireFocus();
            if(!held) { blocked=true;return; }
        }
        int mode=outdoor?2:kids?1:0;
        if(!interrupted && (playingMode==null || playingMode!=mode)) {
            if(outdoor)output.playOutdoor();else output.play(kids);
            playingMode=mode;
        }
    }
}
