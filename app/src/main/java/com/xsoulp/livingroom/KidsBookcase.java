package com.xsoulp.livingroom;

/** One six-row scene: stories, kids video, then Disney; stable source item order. */
final class KidsBookcase {
    static final int COUNT=28, ROWS=6;
    static final float WORLD_HEIGHT=2508, COVER_HEIGHT=214;
    private static final int[] STARTS={0,4,8,12,16,22};
    private static final int[] COLUMNS={4,4,4,4,6,6};
    private static final float[] BOTTOMS={619,927,1236,1548,1858,2165};
    static int clamp(int index) { return Math.max(0,Math.min(COUNT-1,index)); }
    static int row(int index) {
        index=clamp(index);
        for(int r=ROWS-1;r>=0;r--) if(index>=STARTS[r]) return r;
        return 0;
    }
    static int columns(int row) { return COLUMNS[row]; }
    static int start(int row) { return STARTS[row]; }
    static int column(int index) { index=clamp(index);return index-STARTS[row(index)]; }
    static int vertical(int index,int direction) {
        index=clamp(index);
        int from=row(index),to=Math.max(0,Math.min(ROWS-1,from+direction));
        int column=Math.round(column(index)/(float)(columns(from)-1)*(columns(to)-1));
        return start(to)+column;
    }
    static boolean atHorizontalEdge(int index,int direction) {
        int col=column(index),next=col+direction;
        return next<0 || next>=columns(row(index));
    }
    static final float FLOOR_CAMERA=WORLD_HEIGHT-941, FLOOR_APP_Y=2364;
    // Registered to the three painted tennis balls in the 1024 x 1536 manga plate.
    static final float FLOOR_APP_SIZE=124,FLOOR_BALL_RADIUS=52;
    static float floorAppX(int app) {return app==1?645:app==0?838:1029;}
    static int horizontal(int index,int direction) {
        index=clamp(index);int r=row(index);
        return start(r)+Math.max(0,Math.min(columns(r)-1,column(index)+direction));
    }
    static float bottom(int row) { return BOTTOMS[row]; }
    static float camera(int index) {
        return Math.max(0,Math.min(WORLD_HEIGHT-941,bottom(row(index))-COVER_HEIGHT/2-450));
    }
    static float left(int index) {
        int r=row(index);float step=1084f/columns(r),width=width(index);
        return 294+step*(column(index)+.5f)-width/2;
    }
    static float width(int index) { return columns(row(index))==6?146:158; }
    static String heading(int row) {
        return new String[]{"BOOKS & STORIES","MORE STORIES","CONTINUE WATCHING",
            "RECENTLY ADDED","DISNEY","MORE DISNEY"}[row];
    }
    static String section(int index) {
        int r=row(index);return r<2?"Books & Stories":r<4?"Kids media":"Disney";
    }
}
