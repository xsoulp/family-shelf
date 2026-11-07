package com.xsoulp.livingroom;

import java.util.*;
import org.json.*;

/** Only recent airborne positions crossing the bridge corridor can create an animation. */
final class BridgeFlightTracker {
    static final double BRIDGE_LAT=38.68917,BRIDGE_LON=-9.17694;
    static final double HALF_LENGTH=1400,CORRIDOR=400;
    static final long MAX_AGE=20_000,DEDUP_MS=60_000;
    static final class Event {
        final String id,label;final double heading;final long time;
        Event(String id,String label,double heading,long time) {this.id=id;this.label=label;this.heading=heading;this.time=time;}
    }
    private static final class Position {
        final double x,y;final long time;
        Position(double x,double y,long time) {this.x=x;this.y=y;this.time=time;}
    }
    private final Map<String,Position> previous=new HashMap<>();
    private final Map<String,Long> emitted=new HashMap<>();
    List<Event> accept(JSONObject json,long now) throws JSONException {
        double timestamp=json.getDouble("now");long source=(long)(timestamp<1e12?timestamp*1000:timestamp);
        List<Event> events=new ArrayList<>();
        if(Math.abs(now-source)>MAX_AGE)return events;
        JSONArray rows=json.getJSONArray("ac");
        for(Iterator<Map.Entry<String,Position>> it=previous.entrySet().iterator();it.hasNext();)
            if(now-it.next().getValue().time>60_000)it.remove();
        for(Iterator<Map.Entry<String,Long>> it=emitted.entrySet().iterator();it.hasNext();)
            if(now-it.next().getValue()>DEDUP_MS)it.remove();
        for(int i=0;i<rows.length();i++) {
            JSONObject a=rows.optJSONObject(i);if(a==null)continue;
            String id=a.optString("hex").trim();double lat=a.optDouble("lat",Double.NaN),lon=a.optDouble("lon",Double.NaN);
            double age=a.optDouble("seen_pos",Double.NaN),alt=a.optDouble("alt_baro",Double.NaN);
            if(id.isEmpty() || !OutdoorWeather.finite(lat) || !OutdoorWeather.finite(lon)
                || !OutdoorWeather.finite(age) || age<0 || age>20 || !OutdoorWeather.finite(alt) || alt<300
                || Math.abs(lat)>90 || Math.abs(lon)>180)continue;
            long observed=source-(long)(age*1000);if(now-observed>MAX_AGE)continue;
            // Local metre projection; the bridge runs approximately north/south.
            Position p=new Position((lon-BRIDGE_LON)*86770,(lat-BRIDGE_LAT)*111195,observed);
            Position old=previous.put(id,p);
            boolean crossing=inside(p.x,p.y) && (old==null || !inside(old.x,old.y));
            if(!crossing && old!=null && !inside(old.x,old.y) && !inside(p.x,p.y) && observed>old.time && observed-old.time<=30_000
                && Math.hypot(p.x-old.x,p.y-old.y)<(observed-old.time)*.4+100)
                crossing=intersects(old.x,old.y,p.x,p.y);
            if(!crossing || emitted.containsKey(id))continue;
            emitted.put(id,now);
            String label=a.optString("flight",id).trim().replaceAll("[^A-Za-z0-9 -]","");
            if(label.isEmpty())label=id;
            double heading=a.optDouble("track",90);if(!OutdoorWeather.finite(heading))heading=90;
            events.add(new Event(id,label,heading,now));
        }
        return events;
    }
    static boolean inside(double x,double y) { return Math.abs(x)<=CORRIDOR && Math.abs(y)<=HALF_LENGTH; }
    static boolean intersects(double x1,double y1,double x2,double y2) {
        // Liang-Barsky segment/rectangle clipping also catches crossings between polls.
        double[] p={-(x2-x1),x2-x1,-(y2-y1),y2-y1};
        double[] q={x1+CORRIDOR,CORRIDOR-x1,y1+HALF_LENGTH,HALF_LENGTH-y1};
        double lo=0,hi=1;
        for(int i=0;i<4;i++) {
            if(p[i]==0) {if(q[i]<0)return false;continue;}
            double r=q[i]/p[i];if(p[i]<0)lo=Math.max(lo,r);else hi=Math.min(hi,r);
            if(lo>hi)return false;
        }
        return true;
    }
}
