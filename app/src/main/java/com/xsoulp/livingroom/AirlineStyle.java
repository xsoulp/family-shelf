package com.xsoulp.livingroom;

import java.util.Locale;

/** Callsign operator families select illustrative colours, not a specific airframe's paint scheme. */
final class AirlineStyle {
    final String name;
    final int tail,accent;
    private AirlineStyle(String name,int tail,int accent) {this.name=name;this.tail=tail;this.accent=accent;}
    static AirlineStyle forCallsign(String label) {
        String id=label==null?"":label.trim().toUpperCase(Locale.ROOT).replace(" ","");
        if(id.startsWith("EZY") || id.startsWith("EJU") || id.startsWith("EZS"))return new AirlineStyle("easyJet",0xffff741c,0xffffd29d);
        if(id.startsWith("RYR") || id.startsWith("RUK") || id.startsWith("MAY") || id.startsWith("RYS"))return new AirlineStyle("Ryanair",0xff143675,0xffffd339);
        if(id.startsWith("VLG"))return new AirlineStyle("Vueling",0xff747a82,0xffffdd35);
        if(id.startsWith("TAP"))return new AirlineStyle("TAP",0xff259457,0xffed424e);
        return new AirlineStyle("",0xff607d9e,0xffc5d5e5);
    }
}
