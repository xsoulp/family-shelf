package com.xsoulp.livingroom;

import org.junit.Test;
import static org.junit.Assert.*;

public final class RoomEdgesTest {
    @Test public void eachKidsShelfExitsLeftAndRightAtTheEnd() {
        for(int row=0;row<6;row++) {
            int first=KidsBookcase.start(row),last=first+KidsBookcase.columns(row)-1;
            assertTrue(KidsBookcase.atHorizontalEdge(first,-1));
            assertTrue(KidsBookcase.atHorizontalEdge(last,1));
            assertFalse(KidsBookcase.atHorizontalEdge(first,1));
            assertFalse(KidsBookcase.atHorizontalEdge(last,-1));
        }
    }
    @Test public void ordinarySelectionRemainsWithinTheRowUntilItsEdge() {
        for(int row=0;row<6;row++) for(int col=1;col<KidsBookcase.columns(row)-1;col++) {
            int index=KidsBookcase.start(row)+col;
            assertFalse(KidsBookcase.atHorizontalEdge(index,-1));
            assertFalse(KidsBookcase.atHorizontalEdge(index,1));
            assertEquals(index-1,KidsBookcase.horizontal(index,-1));
            assertEquals(index+1,KidsBookcase.horizontal(index,1));
        }
    }
    @Test public void enterAdultsAtTheOppositeEdgeAndMatchingRow() {
        assertEquals(0,RoomEdges.enterShelf(false,0,0,1));
        assertEquals(3,RoomEdges.enterShelf(false,0,0,-1));
        assertEquals(4,RoomEdges.enterShelf(false,0,1,1));
        assertEquals(7,RoomEdges.enterShelf(false,0,1,-1));
    }
    @Test public void returnFromAdultsPreservesEveryKidsSectionAndRow() {
        for(int row=0;row<6;row++) {
            int first=KidsBookcase.start(row),last=first+KidsBookcase.columns(row)-1;
            assertEquals(first,RoomEdges.enterShelf(true,first,row%2,1));
            assertEquals(last,RoomEdges.enterShelf(true,last,row%2,-1));
        }
    }
    @Test public void oppositeEdgeRoundTripRestoresKidsBoundary() {
        for(int row=0;row<6;row++) for(int direction:new int[]{-1,1}) {
            int start=KidsBookcase.start(row)+(direction<0?0:KidsBookcase.columns(row)-1);
            int adults=RoomEdges.enterShelf(false,start,row%2,direction);
            int back=RoomEdges.enterShelf(true,start,adults/4,-direction);
            assertEquals(start,back);
        }
    }
    @Test public void floorFitsBeneathTheLastShelf() {
        assertTrue(KidsBookcase.bottom(5)-KidsBookcase.FLOOR_CAMERA<650);
        float app=KidsBookcase.FLOOR_APP_Y-KidsBookcase.FLOOR_CAMERA;
        assertTrue(app>760 && app<860);
        assertEquals(KidsBookcase.WORLD_HEIGHT-941,KidsBookcase.FLOOR_CAMERA,.001f);
    }
}
