package com.xsoulp.livingroom;

import android.content.Context;
import android.graphics.Canvas;
import android.os.SystemClock;
import android.view.View;
import java.util.ArrayDeque;
import java.util.List;

/** Silent, non-interactive top-of-video aircraft band. */
final class NetflixAircraftView extends View {
    private final AircraftPainter painter;
    private final android.graphics.Paint creditPaint=new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
    private final ArrayDeque<BridgeFlightTracker.Event> queue=new ArrayDeque<>();
    private BridgeFlightTracker.Event plane;
    private long started;
    private boolean stopped,finished;
    private final Runnable done;
    NetflixAircraftView(Context context,Runnable done) {
        super(context);painter=new AircraftPainter(context);this.done=done;
        setFocusable(false);setClickable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    void add(List<BridgeFlightTracker.Event> events) {
        for(BridgeFlightTracker.Event event:events)if(queue.size()<5)queue.add(event);
        invalidate();
    }
    void stop() {stopped=true;queue.clear();plane=null;}
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);if(stopped)return;
        long now=System.currentTimeMillis(),motion=SystemClock.uptimeMillis();
        if(plane!=null && motion-started>=18000)plane=null;
        while(plane==null && !queue.isEmpty()) {
            BridgeFlightTracker.Event event=queue.remove();
            if(now-event.time<60000){plane=event;started=motion;}
        }
        if(plane==null) {if(!finished){finished=true;post(done);}return;}
        float progress=(motion-started)/18000f;
        float x=-70+(1672+140)*progress,y=30+(float)Math.sin(progress*Math.PI)*3;
        if(plane.heading>=180)x=1672-x;
        canvas.save();canvas.scale(getWidth()/1672f,getWidth()/1672f);
        painter.draw(canvas,plane,x,y,motion,false,true);canvas.restore();
        CreatorCredit.draw(getContext(),canvas,getWidth(),getHeight(),true,creditPaint);
        postInvalidateDelayed(33);
    }
}
