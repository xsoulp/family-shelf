package com.xsoulp.livingroom;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

/** Persistent, non-interactive creator credit outside the room camera transform. */
final class CreatorCredit extends View {
    private static final String TAG="family-shelf-creator-credit";
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final boolean compact;
    private CreatorCredit(Context context,boolean compact) {
        super(context);this.compact=compact;setTag(TAG);
        setClickable(false);setFocusable(false);setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO);
    }
    static void install(Window window,boolean compact) {
        ViewGroup decor=(ViewGroup)window.getDecorView();
        if(decor.getTag(R.string.creator_credit)!=null)return;
        CreatorCredit credit=new CreatorCredit(decor.getContext(),compact);
        decor.setTag(R.string.creator_credit,credit);
        // A visual overlay cannot enlarge wrap-content dialogs or receive their input.
        decor.getOverlay().add(credit);
        credit.layout(0,0,decor.getWidth(),decor.getHeight());
        decor.addOnLayoutChangeListener((view,l,t,r,b,oldL,oldT,oldR,oldB) ->
            credit.layout(0,0,r-l,b-t));
    }
    static void draw(Context context,Canvas canvas,int width,int height,boolean compact,Paint paint) {
        float density=context.getResources().getDisplayMetrics().density;
        paint.setStyle(Paint.Style.FILL);paint.setTypeface(Typeface.create("sans-serif",Typeface.NORMAL));
        paint.setTextSize((compact?8:10)*density);paint.setTextAlign(Paint.Align.RIGHT);
        paint.setColor(0xddeef2f5);paint.setShadowLayer(2*density,0,density,0xff17212b);
        canvas.drawText(context.getString(R.string.creator_credit),width-12*density,
            compact?14*density:height-10*density,paint);
        paint.clearShadowLayer();
    }
    @Override protected void onDraw(Canvas canvas) {draw(getContext(),canvas,getWidth(),getHeight(),compact,paint);}
    static final class DialogBuilder extends AlertDialog.Builder {
        DialogBuilder(Context context) {super(context);}
        @Override public AlertDialog create() {
            AlertDialog dialog=super.create();
            dialog.setOnShowListener(ignored -> install(dialog.getWindow(),true));
            return dialog;
        }
    }
}
