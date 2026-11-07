package com.xsoulp.livingroom;

import android.content.Context;
import android.graphics.*;
import android.os.SystemClock;
import android.view.View;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;

/** Animated weather over matching manga plates. The bell-tower dial is a live clock. */
final class OutdoorScreensaverView extends View {
    static final float W=1672,H=941;
    private final Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final RectF whole=new RectF(0,0,W,H),panel=new RectF(42,744,510,891);
    private final Path shape=new Path();
    private final Calendar calendar=Calendar.getInstance();
    private final SimpleDateFormat updated=new SimpleDateFormat("HH:mm",Locale.ENGLISH);
    private final SimpleDateFormat date=new SimpleDateFormat("EEE, d MMM",Locale.ENGLISH);
    private final ArrayDeque<BridgeFlightTracker.Event> flights=new ArrayDeque<>();
    private Bitmap day,night,cloud;
    private final RectF cloudBounds=new RectF();
    private final ColorFilter nightCloudTint=new PorterDuffColorFilter(0xff586c95,PorterDuff.Mode.MULTIPLY);
    private final ColorFilter overcastCloudTint=new PorterDuffColorFilter(0xffb4bfcb,PorterDuff.Mode.MULTIPLY);
    private OutdoorWeather weather;
    private OutdoorSceneConfig config;
    private String flightStatus="Checking bridge traffic…";
    private boolean active;
    private int previewPhase=-1;
    private long planeStart;
    private BridgeFlightTracker.Event plane;
    private final AircraftPainter aircraftPainter;
    private final FlightSoundCue soundCue=new FlightSoundCue();
    private Runnable flightSound;
    void setFlightSound(Runnable callback) {flightSound=callback;}
    OutdoorScreensaverView(Context context) {
        super(context);aircraftPainter=new AircraftPainter(context);setVisibility(GONE);setFocusable(false);setBackgroundColor(0xff081730);
        setContentDescription("Outdoor screensaver. Press any button to return to your library.");
    }
    void setWeather(OutdoorWeather value) {weather=value;invalidate();}
    void flightStatus(String status) {flightStatus=status;invalidate();}
    void addFlights(List<BridgeFlightTracker.Event> events) {
        for(BridgeFlightTracker.Event e:events)if(flights.size()<5)flights.add(e);
    }
    void start(OutdoorSceneConfig config,int phase) {
        this.config=config;previewPhase=phase;
        TimeZone zone=TimeZone.getTimeZone(config.zone);calendar.setTimeZone(zone);date.setTimeZone(zone);updated.setTimeZone(zone);
        if(day==null)day=load("screensaver-lisbon-day.png");
        if(night==null)night=load("screensaver-lisbon-night.png");
        if(cloud==null)cloud=load("screensaver-cloud-manga.png");
        active=true;setVisibility(VISIBLE);invalidate();
    }
    void stop() {active=false;setVisibility(GONE);flights.clear();plane=null;soundCue.stop();}
    void release() {stop();if(day!=null)day.recycle();if(night!=null)night.recycle();if(cloud!=null)cloud.recycle();day=null;night=null;cloud=null;}
    private Bitmap load(String name) {
        try(InputStream in=getContext().getAssets().open(name)) {return BitmapFactory.decodeStream(in);}
        catch(java.io.IOException e) {return null;}
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);if(!active || config==null)return;
        long now=System.currentTimeMillis(),motion=SystemClock.uptimeMillis();
        boolean isDay=previewPhase>=0?previewPhase==1:weather!=null?weather.day(now):OutdoorWeather.fallbackDay(now,calendar.getTimeZone());
        float scale=Math.min(getWidth()/W,getHeight()/H);
        c.save();c.translate((getWidth()-W*scale)/2,(getHeight()-H*scale)/2);c.scale(scale,scale);
        p.setStyle(Paint.Style.FILL);p.setColor(Color.WHITE);p.setShader(null);p.clearShadowLayer();
        Bitmap plate=isDay?day:night;if(plate!=null)c.drawBitmap(plate,null,whole,p);
        boolean fresh=weather!=null && weather.fresh(now);
        int clouds=fresh?weather.clouds:20;
        if(fresh && clouds>65) {p.setColor(isDay?0x323d4a61:0x2224314a);c.drawRect(whole,p);}
        if(!isDay && clouds<65)drawStars(c,motion);
        drawClouds(c,motion,isDay,clouds,fresh?weather.wind:5);
        if(fresh && (weather.rain() || weather.snow()))drawPrecipitation(c,motion,weather.snow());
        if(fresh && (weather.code==45 || weather.code==48)) {p.setColor(0x447d8897);c.drawRect(whole,p);}
        drawPlane(c,motion,now,isDay);
        drawClock(c,now,isDay);
        p.setStyle(Paint.Style.FILL);p.setColor(0xa0142230);c.drawRoundRect(panel,16,16,p);
        text(c,config.label.toUpperCase(Locale.ENGLISH),62,774,17,0xffe4e9dc);
        String temperature=weather==null?"— °C":String.format(Locale.ENGLISH,"%.0f °C",weather.temperature);
        text(c,temperature,60,833,54,Color.WHITE);
        String condition=weather==null?"Weather unavailable":weather.condition()+(fresh?"":" · saved reading");
        text(c,condition,64,867,20,0xffecf1ed);
        text(c,date.format(new Date(now)),W-260,895,18,0xfff5edda);
        String source="Weather: Open-Meteo"+(weather==null?" · reconnecting":" · "+(fresh?"updated ":"saved ")+updated.format(new Date(weather.observedAt)));
        text(c,source,48,914,14,0xffe5ebee);
        text(c,"Flights: ADSB.lol · ODbL · "+flightStatus,620,914,13,0xffe5ebee);
        if(previewPhase>=0)text(c,(isDay?"DAY":"NIGHT")+" PREVIEW · Press any button to return",52,42,16,Color.WHITE);
        c.restore();p.clearShadowLayer();if(active)postInvalidateDelayed(66);
    }
    private void text(Canvas c,String value,float x,float y,float size,int color) {
        p.setStyle(Paint.Style.FILL);p.setTextAlign(Paint.Align.LEFT);p.setTextSize(size);p.setTypeface(Typeface.DEFAULT);
        p.setColor(color);p.setShadowLayer(3,0,1,0xaa000000);c.drawText(value,x,y,p);p.clearShadowLayer();
    }
    private void drawStars(Canvas c,long now) {
        for(int i=0;i<46;i++) {
            float x=(i*193.7f+120)%W,y=18+(i*41.3f)%128;
            int alpha=(int)(110+55*Math.sin(now/1800.0+i));p.setColor(Color.argb(alpha,231,240,255));c.drawCircle(x,y,i%4==0?1.5f:1,p);
        }
    }
    private void drawClouds(Canvas c,long now,boolean day,int cover,double wind) {
        if(cloud==null)return;
        int count=cover<10?1:cover<45?3:cover<80?5:7;
        double seconds=now/1000.0,speed=3+Math.min(35,wind)*.45;
        int saved=c.save();c.clipRect(0,0,W,220);
        p.setColor(Color.WHITE);p.setAlpha(day?205:155);
        p.setColorFilter(day?(cover>65?overcastCloudTint:null):nightCloudTint);
        for(int i=0;i<count;i++) {
            float width=330+(i%3)*92,height=width*cloud.getHeight()/cloud.getWidth();
            float x=(float)((i*367.0+seconds*speed*(i%2==0?1:.75))%(W+width))-width;
            float y=12+(i*29)%62;
            cloudBounds.set(x,y,x+width,y+height);
            if(i%2==0)c.drawBitmap(cloud,null,cloudBounds,p);
            else {
                int flipped=c.save();c.scale(-1,1,cloudBounds.centerX(),cloudBounds.centerY());
                c.drawBitmap(cloud,null,cloudBounds,p);c.restoreToCount(flipped);
            }
        }
        p.setColorFilter(null);p.setAlpha(255);c.restoreToCount(saved);
    }
    private void drawPrecipitation(Canvas c,long now,boolean snow) {
        p.setColor(snow?0xcceaf3ff:0x608ab6dc);p.setStrokeWidth(snow?2:1.1f);
        for(int i=0;i<(snow?65:95);i++) {
            float x=(i*173.3f+(snow?now*.006f:now*.018f))%W,y=(i*97.3f+now*(snow?.027f:.36f))%H;
            if(snow)c.drawCircle(x,y,1.8f,p);else c.drawLine(x,y,x-4,y+15,p);
        }
    }
    private void drawClock(Canvas c,long now,boolean day) {
        final float x=1375,y=537,r=70;
        p.setColor(day?0xfff7efd9:0xfff2dfbc);c.drawCircle(x,y,r,p);
        p.setColor(0xff352e28);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.create("serif",Typeface.NORMAL));p.setTextSize(14);
        for(int i=1;i<=12;i++) {
            double a=Math.toRadians(i*30-90);float tx=x+(float)Math.cos(a)*55,ty=y+(float)Math.sin(a)*55;
            c.drawText(Integer.toString(i),tx,ty+5,p);
        }
        calendar.setTimeInMillis(now);
        float sec=calendar.get(Calendar.SECOND)+calendar.get(Calendar.MILLISECOND)/1000f;
        float min=calendar.get(Calendar.MINUTE)+sec/60,hour=calendar.get(Calendar.HOUR)+min/60;
        hand(c,x,y,hour*30,35,4.5f,0xff332d27);hand(c,x,y,min*6,49,3,0xff332d27);
        hand(c,x,y,sec*6,52,1.1f,0xff984c3a);p.setStyle(Paint.Style.FILL);p.setColor(0xff352e28);c.drawCircle(x,y,4,p);
    }
    private void hand(Canvas c,float x,float y,float angle,float length,float width,int color) {
        double a=Math.toRadians(angle-90);p.setColor(color);p.setStrokeWidth(width);p.setStrokeCap(Paint.Cap.ROUND);
        c.drawLine(x,y,x+(float)Math.cos(a)*length,y+(float)Math.sin(a)*length,p);p.setStrokeCap(Paint.Cap.BUTT);
    }
    private void drawPlane(Canvas c,long motion,long now,boolean day) {
        if(plane!=null && motion-planeStart>=18000)plane=null;
        while(plane==null && !flights.isEmpty()) {BridgeFlightTracker.Event e=flights.remove();if(now-e.time<60_000){plane=e;planeStart=motion;soundCue.begin();}}
        if(plane==null)return;
        float f=(motion-planeStart)/18000f,x=-70+(W+140)*f,y=120+(float)Math.sin(f*Math.PI)*12;
        if(soundCue.at(f) && flightSound!=null)flightSound.run();
        boolean right=plane.heading<180;if(!right)x=W-x;
        aircraftPainter.draw(c,plane,x,y,motion,day,false);
    }
}
