package com.xsoulp.livingroom;

import org.junit.Test;
import static org.junit.Assert.*;

public final class KidsBookcaseTest {
    @Test public void everyItemHasOneRowAndKeepsItsHorizontalPosition() {
        int visited=0;
        for(int row=0;row<6;row++) for(int col=0;col<KidsBookcase.columns(row);col++) {
            int index=KidsBookcase.start(row)+col;
            assertEquals(row,KidsBookcase.row(index));assertEquals(col,KidsBookcase.column(index));visited++;
        }
        assertEquals(28,visited);
    }
    @Test public void downCrossesContentSectionsWithinOneBookcase() {
        assertEquals(8,KidsBookcase.vertical(4,1));
        assertEquals(16,KidsBookcase.vertical(12,1));
        assertEquals(22,KidsBookcase.vertical(16,1));
    }
    @Test public void unequalRowsKeepNearestColumnAndRightEdge() {
        assertEquals(21,KidsBookcase.vertical(15,1));
        assertEquals(15,KidsBookcase.vertical(21,-1));
        assertEquals(19,KidsBookcase.vertical(14,1));
        assertEquals(14,KidsBookcase.vertical(19,-1));
    }
    @Test public void verticalAndHorizontalEdgesNeverLeaveTheKidsBookcase() {
        assertEquals(0,KidsBookcase.vertical(0,-1));assertEquals(27,KidsBookcase.vertical(27,1));
        for(int row=0;row<6;row++) {
            int first=KidsBookcase.start(row),last=first+KidsBookcase.columns(row)-1;
            assertEquals(first,KidsBookcase.horizontal(first,-1));assertEquals(last,KidsBookcase.horizontal(last,1));
        }
    }
    @Test public void cameraKeepsEveryCoverInItsViewport() {
        for(int i=0;i<28;i++) {
            float camera=KidsBookcase.camera(i),bottom=KidsBookcase.bottom(KidsBookcase.row(i));
            assertTrue(camera>=0 && camera<=KidsBookcase.WORLD_HEIGHT-941);
            assertTrue(bottom-KidsBookcase.COVER_HEIGHT-camera>=180);
            assertTrue(bottom-camera<=760);
            assertTrue(KidsBookcase.left(i)>=294);
            assertTrue(KidsBookcase.left(i)+KidsBookcase.width(i)<=1378);
        }
    }
    @Test public void storedIndexesAreClampedAfterMigration() {
        assertEquals(0,KidsBookcase.clamp(-1));assertEquals(27,KidsBookcase.clamp(100));
        assertEquals("Books & Stories",KidsBookcase.section(0));
        assertEquals("Kids media",KidsBookcase.section(8));assertEquals("Disney",KidsBookcase.section(16));
    }
}
