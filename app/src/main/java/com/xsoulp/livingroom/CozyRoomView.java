package com.xsoulp.livingroom;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** A baked library room with large live book covers and a readable selection panel.
 * All state belongs to the UI thread. The camera animates only on selection changes. */
final class CozyRoomView extends FrameLayout {
    static final int PAGE_SIZE=8;
    static final float W=1672,H=941;
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG);
    private final TextPaint text=new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final List<ShelfItem> items=new ArrayList<>();
    private final List<LatestItem> latest=new ArrayList<>();
    private final Map<String,Bitmap> artwork=new HashMap<>();
    private Bitmap room,adultRoomPhoto;
    private boolean photoChecked;
    private boolean kids;
    private int pageSize() { return kids?KidsBookcase.COUNT:PAGE_SIZE; }
    private int selected,shelfIndex;
    private boolean shelfFocused,optionsFocused,active;
    private ValueAnimator motion,bookMotion;
    private final float[] bookLift=new float[KidsBookcase.COUNT];
    private final Matrix bookMatrix=new Matrix();
    private final RectF presentedBook=new RectF();
    private final float[] sourceCorners=new float[8],bookCorners=new float[8];
    private float zoom=1,pan=0,cameraY=0;
    private String status="Loading Continue watching…";
    private volatile String previewMode="landscape",previewTitle="Mountain and lake",previewItemKey="";
    interface PosterClick { void accept(int index); }
    private PosterClick posterClick,sectionClick;
    private Runnable cameraListener;
    void setCameraListener(Runnable listener) { cameraListener=listener; }
    float cameraY() { return cameraY; }
    private void cameraChanged() { if(cameraListener!=null)cameraListener.run(); }
    private final RectF[] slots=new RectF[KidsBookcase.COUNT];
    void setKids(boolean value) {
        if(kids!=value) {
            if(bookMotion!=null)bookMotion.cancel();
            java.util.Arrays.fill(bookLift,0);
        }
        kids=value;
        for(int i=0;i<pageSize();i++) {
            if(kids) {
                float left=KidsBookcase.left(i),bottom=KidsBookcase.bottom(KidsBookcase.row(i));
                slots[i]=new RectF(left,bottom-KidsBookcase.COVER_HEIGHT,left+KidsBookcase.width(i),bottom);
            } else {
                float x=625+(i%4)*187,top=i<4?110:377;
                slots[i]=new RectF(x,top,x+152,i<4?314:580);
            }
        }
    }
    CozyRoomView(Context context) {
        super(context); setKids(false); setWillNotDraw(false); setBackgroundColor(Color.BLACK);
    }
    void loadRoom(boolean lowDetail) {
        try(InputStream input=getContext().getAssets().open(kids?"library-kids-continuous.png":"library-adults.png")) {
            BitmapFactory.Options options=new BitmapFactory.Options();
            options.inPreferredConfig=Bitmap.Config.RGB_565; options.inSampleSize=lowDetail?2:1;
            Bitmap old=room; room=BitmapFactory.decodeStream(input,null,options);
            if(old!=null) old.recycle();
            if(room==null) throw new IllegalStateException("Could not decode cozy room");
        } catch(java.io.IOException e) { throw new IllegalStateException("Missing cozy room asset",e); }
        if(!kids && !photoChecked) {
            photoChecked=true;
            try(InputStream input=getContext().getAssets().open("adult-room-photo.jpg")) {
                BitmapFactory.Options options=new BitmapFactory.Options();
                options.inSampleSize=2;
                adultRoomPhoto=BitmapFactory.decodeStream(input,null,options);
            } catch(java.io.IOException ignored) { /* Public builds retain the example portrait. */ }
        }
        invalidate();
    }
    void setActive(boolean value) {
        active=value;
        if(!value && motion!=null) motion.cancel();
        if(!value && bookMotion!=null)bookMotion.cancel();
        if(value) {
            if(kids) cameraY=shelfFocused?KidsBookcase.camera(shelfIndex):KidsBookcase.FLOOR_CAMERA;
            animateBooks();cameraChanged();invalidate();
        }
    }
    void setPosterClick(PosterClick callback) { posterClick=callback; }
    void setSectionClick(PosterClick callback) { sectionClick=callback; }
    void setItems(List<ShelfItem> values,List<LatestItem> additions,String message) {
        items.clear(); items.addAll(values); latest.clear(); latest.addAll(additions); status=message;
        java.util.HashSet<String> keep=new java.util.HashSet<>();
        for(ShelfItem item:items) keep.add(item.key);
        for(LatestItem item:latest) keep.add("latest:"+item.id);
        artwork.keySet().retainAll(keep);
        updatePreviewDescription();
        invalidate();
    }
    void setArtwork(String key,Bitmap image) { artwork.put(key,image); invalidate(); }
    void select(int app,int poster,boolean shelf,boolean options) {
        float targetCamera=kids?(shelf?KidsBookcase.camera(poster):KidsBookcase.FLOOR_CAMERA):0;
        boolean changed=selected!=app||shelfIndex!=poster||shelfFocused!=shelf||optionsFocused!=options
            || (motion==null && Math.abs(cameraY-targetCamera)>.5f);
        boolean bookChanged=shelfIndex!=poster || shelfFocused!=shelf;
        selected=app; shelfIndex=poster; shelfFocused=shelf; optionsFocused=options;
        if(bookChanged || (shelf && bookLift[poster]==0))animateBooks();
        updatePreviewDescription();
        if(changed && active) {
            if(motion!=null) motion.cancel();
            final float fromZoom=zoom,fromPan=pan,targetZoom=1f;
            final float targetPan=0,fromCamera=cameraY;
            motion=ValueAnimator.ofFloat(0,1); motion.setDuration(kids?450:280);
            motion.setInterpolator(new android.view.animation.DecelerateInterpolator());
            motion.addUpdateListener(a -> {
                float f=(float)a.getAnimatedValue(); zoom=fromZoom+(targetZoom-fromZoom)*f;
                pan=fromPan+(targetPan-fromPan)*f;
                cameraY=fromCamera+(targetCamera-fromCamera)*f;cameraChanged();invalidate();
            });
            motion.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator animation) {
                    if(motion==animation)motion=null;
                }
            });
            motion.start();
        } else {
            if(!active) { cameraY=targetCamera;cameraChanged(); }
            invalidate();
        }
    }
    private void animateBooks() {
        if(bookMotion!=null)bookMotion.cancel();
        final float[] from=bookLift.clone();
        final int target=shelfFocused?shelfIndex:-1;
        bookMotion=ValueAnimator.ofFloat(0,1);bookMotion.setDuration(420);
        bookMotion.setInterpolator(new android.view.animation.DecelerateInterpolator());
        bookMotion.addUpdateListener(a -> {
            float progress=(float)a.getAnimatedValue();
            for(int i=0;i<bookLift.length;i++)bookLift[i]=from[i]+((i==target?1:0)-from[i])*progress;
            invalidate();
        });
        bookMotion.start();
    }
    private void updatePreviewDescription() {
        if(shelfFocused && !items.isEmpty()) {
            ShelfItem item=items.get(Math.min(shelfIndex,items.size()-1));
            previewMode="item"; previewTitle=item.title; previewItemKey=item.key;
        } else { previewMode="library"; previewTitle="Library"; }
        setContentDescription((kids?"Kids library · "+KidsBookcase.section(shelfIndex):"Adults library")+". Selection: "+previewTitle+". "+status);
    }
    String previewMode() { return previewMode; }
    String previewTitle() { return previewTitle; }
    String previewItemKey() { return previewItemKey; }
    static float bookWidth() { return 146; }
    static float bookHeight() { return 199; }

    private void transform(Canvas c) {
        float scale=Math.min(getWidth()/W,getHeight()/H);
        c.translate((getWidth()-W*scale)/2,(getHeight()-H*scale)/2);
        c.scale(scale,scale);
        c.translate(W/2+pan,H/2); c.scale(zoom,zoom); c.translate(-W/2,-H/2);
        if(kids) c.translate(0,-cameraY);
    }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.save(); transform(canvas);
        paint.setColor(Color.WHITE); paint.setStyle(Paint.Style.FILL);
        if(room!=null) canvas.drawBitmap(room,null,new RectF(0,0,W,kids?KidsBookcase.WORLD_HEIGHT:H),paint);
        if(!kids && adultRoomPhoto!=null) {
            // Fit the original photograph within the existing frame, preserving both faces.
            RectF frame=new RectF(200,382,334,480);
            float scale=Math.max(frame.width()/adultRoomPhoto.getWidth(),frame.height()/adultRoomPhoto.getHeight());
            float width=adultRoomPhoto.getWidth()*scale,height=adultRoomPhoto.getHeight()*scale;
            int saved=canvas.save();canvas.clipRect(frame);
            canvas.drawBitmap(adultRoomPhoto,null,new RectF(frame.centerX()-width/2,frame.centerY()-height/2,
                frame.centerX()+width/2,frame.centerY()+height/2),paint);
            canvas.restoreToCount(saved);
        }
        drawShelf(canvas);
        updatePreviewDescription();
        canvas.restore();
    }
    @Override protected void dispatchDraw(Canvas canvas) {
        // Child controls are positioned in display pixels; use the same subtle camera motion.
        canvas.save();
        float scale=Math.min(getWidth()/W,getHeight()/H);
        float cx=getWidth()/2f,cy=getHeight()/2f;
        canvas.translate(cx+pan*scale,cy); canvas.scale(zoom,zoom); canvas.translate(-cx,-cy);
        super.dispatchDraw(canvas); canvas.restore();
    }
    private void drawShelf(Canvas c) {
        int page=items.isEmpty()?0:Math.min(shelfIndex,items.size()-1)/pageSize();
        int start=page*pageSize();
        for(int slot=0;slot<pageSize() && start+slot<items.size();slot++) {
            ShelfItem item=items.get(start+slot); RectF rect=slots[slot];
            if(bookLift[slot]<.001f)drawBook(c,item,rect,slot,0);
        }

        if(!kids) {
            badge(c,"CONTINUE WATCHING  ·  2 MOVIES + 2 SERIES",600,94);
            badge(c,"RECENTLY ADDED  ·  2 MOVIES + 2 SERIES",600,353);
        }
        if(!kids && items.isEmpty()) block(c,status,180,150,1050,32,0xffe5d7c3,3);
        float lifted=0;for(float value:bookLift)lifted=Math.max(lifted,value);
        if(lifted>.001f) {
            paint.setColor(Color.argb((int)(lifted*35),7,12,19));
            c.drawRect(0,kids?cameraY:0,W,(kids?cameraY:0)+H,paint);
            // Returning books go behind the newly selected cover.
            for(int slot=0;slot<pageSize() && start+slot<items.size();slot++)
                if(slot!=shelfIndex && bookLift[slot]>=.001f)drawBook(c,items.get(start+slot),slots[slot],slot,bookLift[slot]);
            if(shelfIndex>=start && shelfIndex<start+pageSize() && shelfIndex<items.size()) {
                int slot=shelfIndex-start;
                if(bookLift[slot]>=.001f)drawBook(c,items.get(shelfIndex),slots[slot],slot,bookLift[slot]);
            }
        }
    }
    private void drawBook(Canvas c,ShelfItem item,RectF rect,int slot,float lift) {
        BookPresentation.bounds(rect.left,rect.top,rect.right,rect.bottom,kids?cameraY:0,lift,bookCorners);
        presentedBook.set(bookCorners[0],bookCorners[1],bookCorners[4],bookCorners[5]);
        if(lift>.01f) {
            paint.setColor(Color.argb((int)(lift*75),0,0,0));
            c.drawRoundRect(presentedBook.left+12,presentedBook.top+18,presentedBook.right+18,presentedBook.bottom+24,10,10,paint);
        }
        sourceCorners[0]=rect.left;sourceCorners[1]=rect.top;
        sourceCorners[2]=rect.right;sourceCorners[3]=rect.top;
        sourceCorners[4]=rect.right;sourceCorners[5]=rect.bottom;
        sourceCorners[6]=rect.left;sourceCorners[7]=rect.bottom;
        bookMatrix.setPolyToPoly(sourceCorners,0,bookCorners,0,4);
        int saved=c.save();c.concat(bookMatrix);
            // A narrow shaded spine makes each real cover read as a book on the shelf.
            paint.setColor(kids?0xff60432d:slot<4?0xff612127:0xff3a294d);
            c.drawRoundRect(new RectF(rect.left-7,rect.top+2,rect.right,rect.bottom+2),3,3,paint);
            paint.setColor(0xff12171c); c.drawRoundRect(rect,3,3,paint);
            Bitmap image=artwork.get(item.key);
            RectF imageRect=new RectF(rect);
            if(image!=null) {
                if(item.kind==ShelfItem.WATCH_NEXT) {
                    // Shared cards may be landscape. Keep the complete artwork and title visible.
                    float scale=Math.min(imageRect.width()/image.getWidth(),imageRect.height()/image.getHeight());
                    float width=image.getWidth()*scale,height=image.getHeight()*scale;
                    c.drawBitmap(image,null,new RectF(imageRect.centerX()-width/2,imageRect.centerY()-height/2,
                        imageRect.centerX()+width/2,imageRect.centerY()+height/2),paint);
                } else cover(c,image,imageRect);
            }
            else {
                paint.setColor(item.kind==ShelfItem.SHORTCUT?0xff39302b:0xff42374e);
                c.drawRect(imageRect,paint);
                if(!kids) label(c,item.kind==ShelfItem.CONNECT?"CONNECT ACCOUNT":item.kind==ShelfItem.SHORTCUT?"NO TITLE":"NO ARTWORK",rect.left+7,rect.top+42,16,0xfff2dfbd);
            }
            if(item.durationMs>0) {
                paint.setColor(0xff494544); c.drawRect(rect.left+7,rect.bottom-8,rect.right-7,rect.bottom-3,paint);
                paint.setColor(0xffe7bf7b); c.drawRect(rect.left+7,rect.bottom-8,rect.left+7+(rect.width()-14)*item.progress(),rect.bottom-3,paint);
            }
            if(shelfFocused && slot==shelfIndex) {
                paint.setStyle(Paint.Style.STROKE); paint.setColor(0xff8ed8ff); paint.setStrokeWidth(3);
                c.drawRoundRect(new RectF(rect.left-3,rect.top-3,rect.right+3,rect.bottom+3),5,5,paint);
                paint.setStyle(Paint.Style.FILL);
            }
        c.restoreToCount(saved);
    }
    private void badge(Canvas c,String value,float x,float y) {
        paint.setColor(0xd0201712); c.drawRoundRect(x-7,y-23,x+660,y+3,5,5,paint);
        label(c,value,x,y-3,17,0xfff5e8d5);
    }
    private void cover(Canvas c,Bitmap b,RectF dest) {
        float factor=Math.max(dest.width()/b.getWidth(),dest.height()/b.getHeight());
        int width=Math.min(b.getWidth(),Math.round(dest.width()/factor));
        int height=Math.min(b.getHeight(),Math.round(dest.height()/factor));
        Rect src=new Rect((b.getWidth()-width)/2,(b.getHeight()-height)/2,(b.getWidth()+width)/2,(b.getHeight()+height)/2);
        c.drawBitmap(b,src,dest,paint);
    }
    private void label(Canvas c,String value,float x,float y,float size,int color) {
        text.setTextSize(size); text.setColor(color); text.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        c.drawText(value,x,y,text);
    }
    private float block(Canvas c,String value,float x,float y,float width,float size,int color,int lines) {
        text.setTextSize(size); text.setColor(color); text.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        StaticLayout layout=StaticLayout.Builder.obtain(value,0,value.length(),text,Math.max(1,(int)width))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL).setIncludePad(false).setMaxLines(lines)
            .setEllipsize(TextUtils.TruncateAt.END).build();
        c.save(); c.translate(x,y); layout.draw(c); c.restore(); return y+layout.getHeight();
    }
    @Override public boolean onTouchEvent(MotionEvent event) {
        if(event.getAction()!=MotionEvent.ACTION_UP) return true;
        float scale=Math.min(getWidth()/W,getHeight()/H);
        float x=(event.getX()-(getWidth()-W*scale)/2)/scale;
        float y=(event.getY()-(getHeight()-H*scale)/2)/scale;
        x=(x-W/2-pan)/zoom+W/2; y=(y-H/2)/zoom+H/2+(kids?cameraY:0);
        int start=items.isEmpty()?0:Math.min(shelfIndex,items.size()-1)/pageSize()*pageSize();
        int selectedSlot=shelfIndex-start;
        if(shelfFocused && selectedSlot>=0 && selectedSlot<pageSize() && shelfIndex<items.size()) {
            RectF source=slots[selectedSlot];
            BookPresentation.bounds(source.left,source.top,source.right,source.bottom,kids?cameraY:0,bookLift[selectedSlot],bookCorners);
            if(x>=bookCorners[0] && x<=bookCorners[4] && y>=bookCorners[1] && y<=bookCorners[5] && posterClick!=null) {
                posterClick.accept(shelfIndex);performClick();return true;
            }
        }
        for(int i=0;i<pageSize() && start+i<items.size();i++) if(slots[i].contains(x,y) && posterClick!=null) {
            posterClick.accept(start+i); performClick(); return true;
        }
        return performClick();
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
