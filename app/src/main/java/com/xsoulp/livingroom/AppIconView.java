package com.xsoulp.livingroom;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.drawable.Drawable;
import android.view.View;

/** Invisible Kids app targets sit over painted tennis balls; Adults supplies circular focus. */
final class AppIconView extends View {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path clip=new Path();
    private final int app;
    private Drawable icon;
    private String label="App";
    private boolean highlighted,available,replacement,toy;
    void setToy(boolean value) { if(toy!=value) {toy=value;invalidate();} }
    AppIconView(Context context,int app) {super(context);this.app=app;}
    void setIcon(Drawable value,boolean selected,boolean installed) {
        icon=value; highlighted=selected; available=installed; invalidate();
    }
    void setLabel(String value) { label=value; }
    void setReplacement(boolean value) { replacement=value; invalidate(); }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c);
        if(toy) { drawToy(c);return; }
        float cx=getWidth()/2f,cy=getHeight()/2f,r=Math.min(cx,cy)-5;
        if(replacement) {
            // Cover the baked SmartTube emblem with the installed game's actual icon.
            paint.setStyle(Paint.Style.FILL); paint.setColor(0xff090b14);
            c.drawCircle(cx,cy,Math.min(cx,cy)-1,paint);
            if(icon!=null) {
                float half=r*.78f;
                int saved=c.save();
                clip.reset(); clip.addCircle(cx,cy,r-3,Path.Direction.CW); c.clipPath(clip);
                icon.setBounds(Math.round(cx-half),Math.round(cy-half),Math.round(cx+half),Math.round(cy+half));
                icon.draw(c); c.restoreToCount(saved);
            } else {
                paint.setColor(0xfff2e1be); paint.setTextAlign(Paint.Align.CENTER);
                paint.setTextSize(r*.22f); c.drawText(label,cx,cy+r*.08f,paint);
            }
        }
        // The reference scenes contain the app emblems; the live target supplies focus.
        paint.setStyle(Paint.Style.STROKE); paint.setStrokeWidth(highlighted?4:1.5f);
        paint.setColor(highlighted?0xff8ed8ff:Color.TRANSPARENT); c.drawCircle(cx,cy,r,paint);
        paint.setStyle(Paint.Style.FILL);
    }
    private void drawToy(Canvas c) {
        if(!highlighted)return;
        float cx=getWidth()/2f,cy=getHeight()/2f;
        float radius=getWidth()*KidsBookcase.FLOOR_BALL_RADIUS/KidsBookcase.FLOOR_APP_SIZE;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Math.max(2,getWidth()*.019f));
        paint.setColor(0xffa4e5ff);c.drawCircle(cx,cy,radius+3,paint);
        paint.setStyle(Paint.Style.FILL);
    }
}
