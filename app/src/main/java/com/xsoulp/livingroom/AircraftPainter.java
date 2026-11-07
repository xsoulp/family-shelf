package com.xsoulp.livingroom;

import android.content.Context;
import android.graphics.*;
import java.io.InputStream;

/** Painted manga airliner with native operator tail tinting; shared by both scenes. */
final class AircraftPainter {
    static final float VIDEO_SCALE=.5f;
    private static Bitmap sprite;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final Path tail=new Path(),stripe=new Path();
    private final RectF destination=new RectF(-92,-35,92,26.333f);
    AircraftPainter(Context context) {
        if(sprite==null)try(InputStream input=context.getAssets().open("aircraft-realistic-manga.png")) {
            BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=2;
            sprite=BitmapFactory.decodeStream(input,null,options);
        } catch(java.io.IOException ignored) { }
        // Source-normalized fin registration, below its ink edge and above the fuselage.
        fin(tail,new float[]{.038f,.081f,.080f,.083f,.181f,.428f,.207f,.474f,.229f,.493f,.085f,.526f});
        fin(stripe,new float[]{.059f,.385f,.175f,.302f,.215f,.420f,.090f,.520f});
    }
    private void fin(Path path,float[] points) {
        path.reset();path.moveTo(destination.left+points[0]*destination.width(),destination.top+points[1]*destination.height());
        for(int i=2;i<points.length;i+=2)path.lineTo(destination.left+points[i]*destination.width(),destination.top+points[i+1]*destination.height());
        path.close();
    }
    void draw(Canvas canvas,BridgeFlightTracker.Event plane,float x,float y,long motion,boolean day,boolean overlay) {
        AirlineStyle airline=AirlineStyle.forCallsign(plane.label);
        float size=overlay?VIDEO_SCALE:1f;
        int saved=canvas.save();canvas.translate(x,y);canvas.scale(size,size);
        if(plane.heading>=180)canvas.scale(-1,1);
        paint.setStyle(Paint.Style.FILL);paint.setColor(Color.WHITE);paint.setColorFilter(null);
        if(sprite!=null) {
            canvas.drawBitmap(sprite,null,destination,paint);
            int clipped=canvas.save();canvas.clipPath(tail);
            paint.setColorFilter(new LightingColorFilter(airline.tail,0));
            canvas.drawBitmap(sprite,null,destination,paint);
            canvas.clipPath(stripe);paint.setColorFilter(new LightingColorFilter(airline.accent,0));
            canvas.drawBitmap(sprite,null,destination,paint);
            canvas.restoreToCount(clipped);paint.setColorFilter(null);
        } else {
            paint.setColor(0xffe8edf2);canvas.drawOval(-70,-6,70,9,paint);
            paint.setColor(airline.tail);canvas.drawPath(tail,paint);
        }
        if(motion%1600<250){paint.setColor(0xffee6359);canvas.drawCircle(-8,9,1.3f,paint);}
        canvas.restoreToCount(saved);
        paint.setTypeface(Typeface.create("sans-serif",Typeface.BOLD));paint.setTextAlign(Paint.Align.LEFT);paint.setTextSize(13);
        paint.setColor(day?0xff233a50:0xfff0f4ff);paint.setShadowLayer(3,0,1,overlay?0xff000000:0x88000000);
        String caption=plane.label+(airline.name.isEmpty()?"":" · "+airline.name);
        canvas.drawText(caption,x-65*size,y-46*size,paint);paint.clearShadowLayer();
    }
}
