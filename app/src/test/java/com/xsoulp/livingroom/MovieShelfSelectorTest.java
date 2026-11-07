package com.xsoulp.livingroom;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import static org.junit.Assert.*;

public final class MovieShelfSelectorTest {
    private ShelfItem film(String id,String title,boolean ongoing) {
        return new ShelfItem(ShelfItem.JELLYFIN,(ongoing?"jf:":"latest:")+id,title,"Movie",
            "org.jellyfin.androidtv",id,"","",ongoing?1:0,ongoing?100:0);
    }
    private List<ShelfItem> select(List<ShelfItem> recent,List<ShelfItem> ongoing) {
        return MovieShelfSelector.recent(recent,ongoing,Collections.emptyList());
    }
    @Test public void skipsOngoingIdAndBackfillsBothRecentSlots() {
        ShelfItem resume=film("11111111111111111111111111111111","Same movie",true);
        ShelfItem duplicate=film("11111111-1111-1111-1111-111111111111","Same movie",false);
        ShelfItem next=film("22222222222222222222222222222222","Next movie",false);
        ShelfItem last=film("33333333333333333333333333333333","Last movie",false);
        assertEquals(Arrays.asList(next,last),select(Arrays.asList(duplicate,next,last),Arrays.asList(resume)));
    }
    @Test public void excludesAllOngoingMoviesNotOnlyFirstTwo() {
        ShelfItem first=film("1","First",true),second=film("2","Second",true),third=film("3","Third",true);
        assertTrue(select(Arrays.asList(film("3","Third",false)),Arrays.asList(first,second,third)).isEmpty());
    }
    @Test public void distinctJellyfinMoviesWithSameTitleRemainDistinct() {
        ShelfItem remake=film("2","Same title",false);
        assertEquals(Arrays.asList(remake),select(Arrays.asList(remake),Arrays.asList(film("1","Same title",true))));
    }
    @Test public void confirmedNetflixMovieIsExcludedByNormalizedTitle() {
        ShelfItem netflix=new ShelfItem(ShelfItem.WATCH_NEXT,"wn:1","CAFÉ: Night","Netflix",
            "com.netflix.ninja","","","",1,100);
        ShelfItem duplicate=film("1","Cafe Night",false),other=film("2","Other",false);
        assertEquals(Arrays.asList(other),MovieShelfSelector.recent(Arrays.asList(duplicate,other),
            Collections.emptyList(),Arrays.asList(netflix)));
    }
    @Test public void duplicateCandidatesAcrossLibrariesDoNotFillBothSlots() {
        ShelfItem a=film("ABC","A",false),duplicate=film("abc","A",false),b=film("DEF","B",false);
        assertEquals(Arrays.asList(a,b),select(Arrays.asList(a,duplicate,b),Collections.emptyList()));
    }
    @Test public void noAlternativeLeavesRecentMovieSlotsEmpty() {
        ShelfItem a=film("1","A",true);
        assertTrue(select(Arrays.asList(film("1","A",false)),Arrays.asList(a)).isEmpty());
    }
}
