package com.xsoulp.livingroom;

import org.json.JSONObject;

/** Library additions are distinct from unfinished playback history. */
final class LatestItem {
    final String id,title,detail,imagePath;
    LatestItem(String id,String title,String detail,String imagePath) {
        this.id=id; this.title=title; this.detail=detail; this.imagePath=imagePath;
    }
    static LatestItem fromJson(JSONObject row) {
        String id=row.optString("Id"),type=row.optString("Type"),title=row.optString("Name");
        if(!ShelfItem.validId(id) || title.trim().isEmpty()
            || !("Movie".equals(type)||"Episode".equals(type)||"Series".equals(type))) return null;
        String detail="Movie".equals(type)?"Movie":"Series".equals(type)?"TV series":"Episode";
        if(row.optInt("ProductionYear")>0) detail+=" · "+row.optInt("ProductionYear");
        String imageId=id;
        if("Episode".equals(type) && ShelfItem.validId(row.optString("SeriesId"))) {
            imageId=row.optString("SeriesId");
            title=row.optString("SeriesName",title)+" · "+title;
        }
        return new LatestItem(id,title,detail,"Items/"+imageId+"/Images/Primary?maxWidth=192&quality=80");
    }
}
