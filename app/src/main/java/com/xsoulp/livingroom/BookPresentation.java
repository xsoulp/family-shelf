package com.xsoulp.livingroom;

/** Four-corner interpolation from a shelf-bound book to a straight foreground cover. */
final class BookPresentation {
    static void bounds(float left,float top,float right,float bottom,float camera,float lift,float[] out) {
        float f=Math.max(0,Math.min(1,lift)),width=right-left,height=bottom-top;
        float targetHeight=420,targetWidth=targetHeight*width/height;
        float targetLeft=(1672-targetWidth)/2,targetTop=camera+(941-targetHeight)/2;
        float[] shelf={left+width*.15f,top,right,top+height*.065f,right,bottom-height*.035f,left+width*.15f,bottom};
        float[] front={targetLeft,targetTop,targetLeft+targetWidth,targetTop,
            targetLeft+targetWidth,targetTop+targetHeight,targetLeft,targetTop+targetHeight};
        for(int i=0;i<8;i++)out[i]=shelf[i]+(front[i]-shelf[i])*f;
    }
}
