package com.xsoulp.livingroom;

/** One quiet flyby sound as each visible aircraft approaches the centre of the sky. */
final class FlightSoundCue {
    private boolean active,played;
    void begin() {active=true;played=false;}
    void stop() {active=false;}
    boolean at(float progress) {
        if(!active || played || progress<.4f || progress>=1)return false;
        played=true;return true;
    }
}
