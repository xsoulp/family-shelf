package com.xsoulp.livingroom;
import org.junit.Test;
import static org.junit.Assert.*;
public final class BookPresentationTest {
    @Test public void shelvedBookKeepsItsBaseAndHasPerspective() {
        float[] q=new float[8];BookPresentation.bounds(300,400,458,614,0,0,q);
        assertTrue(q[0]>300);assertEquals(614,q[7],.001f);
        assertTrue(q[1]<q[3]);assertTrue(q[5]<q[7]);
    }
    @Test public void liftedCoverIsStraightCenteredAndPreservesAspect() {
        float[] q=new float[8];BookPresentation.bounds(300,400,458,614,900,1,q);
        assertEquals(q[1],q[3],.001f);assertEquals(q[5],q[7],.001f);
        assertEquals(q[0],q[6],.001f);assertEquals(q[2],q[4],.001f);
        assertEquals(836,(q[0]+q[2])/2,.001f);
        assertEquals(900+470.5,(q[1]+q[5])/2,.001f);
        assertEquals(158f/214,(q[2]-q[0])/(q[5]-q[1]),.001f);
    }
    @Test public void intermediatePosesRemainConvexAndNonzeroAcrossRows() {
        for(int index=0;index<KidsBookcase.COUNT;index++)for(int step=0;step<=20;step++) {
            float[] q=new float[8];float bottom=KidsBookcase.bottom(KidsBookcase.row(index));
            BookPresentation.bounds(KidsBookcase.left(index),bottom-214,KidsBookcase.left(index)+KidsBookcase.width(index),bottom,KidsBookcase.camera(index),step/20f,q);
            for(int i=0;i<4;i++) {
                int a=i*2,b=((i+1)%4)*2,c=((i+2)%4)*2;
                float cross=(q[b]-q[a])*(q[c+1]-q[b+1])-(q[b+1]-q[a+1])*(q[c]-q[b]);
                assertTrue(cross>0);
            }
        }
    }
}
