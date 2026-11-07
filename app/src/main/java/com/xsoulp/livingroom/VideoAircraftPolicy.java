package com.xsoulp.livingroom;

/** Only supported, actively playing media apps may show aircraft. */
final class VideoAircraftPolicy {
    static boolean netflix(String name) {
        return "com.netflix.ninja".equals(name) || "com.netflix.mediaclient".equals(name);
    }
    static boolean jellyfin(String name) {
        return "org.jellyfin.androidtv".equals(name) || "org.jellyfin.mobile".equals(name)
            || "dev.jdtech.jellyfin".equals(name);
    }
    static boolean supported(String name) {return netflix(name) || jellyfin(name);}
    static boolean allowed(boolean enabled,boolean permission,boolean screenOn,boolean launcher,
            String foreground,String playingPackage) {
        return enabled && permission && screenOn && !launcher && supported(playingPackage)
            && (foreground==null || foreground.equals(playingPackage));
    }
}
