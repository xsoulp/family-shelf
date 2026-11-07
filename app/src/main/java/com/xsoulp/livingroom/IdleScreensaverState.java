package com.xsoulp.livingroom;

/** Monotonic idle timer. Wake input is consumed so it cannot open a shelf item. */
final class IdleScreensaverState {
    static final long TIMEOUT_MS=120_000;
    private long lastInput;
    private boolean foreground,screenOn,showing;
    void resume(long now,boolean interactive) { foreground=true;screenOn=interactive;lastInput=now;showing=false; }
    void pause() { foreground=false;showing=false; }
    void screen(boolean on,long now) { screenOn=on;lastInput=now;if(!on)showing=false; }
    boolean input(long now) { lastInput=now;boolean wake=showing;showing=false;return wake; }
    boolean due(long now,boolean dialogShowing) {
        return foreground && screenOn && !showing && !dialogShowing && now-lastInput>=TIMEOUT_MS;
    }
    boolean showing() { return showing; }
    void show() { showing=true; }
    long remaining(long now) { return Math.max(0,TIMEOUT_MS-(now-lastInput)); }
}
