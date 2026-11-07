package com.xsoulp.livingroom;

/** Opposite-edge entry keeps the upper/lower row and the last Kids section. */
final class RoomEdges {
    static int enterShelf(boolean kids,int rememberedKids,int row,int direction) {
        row=Math.max(0,Math.min(1,row));
        if(!kids)return row*4+(direction>0?0:3);
        int section=KidsBookcase.row(rememberedKids)/2;
        int target=section*2+row;
        return KidsBookcase.start(target)+(direction>0?0:KidsBookcase.columns(target)-1);
    }
}
