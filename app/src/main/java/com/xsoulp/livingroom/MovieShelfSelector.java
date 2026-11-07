package com.xsoulp.livingroom;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Select recent films after excluding resume records and confirmed Netflix films. */
final class MovieShelfSelector {
    static List<ShelfItem> recent(List<ShelfItem> candidates,List<ShelfItem> ongoing,
                                  List<ShelfItem> visibleMovies) {
        Set<String> excludedIds=new HashSet<>(), netflixTitles=new HashSet<>();
        for(ShelfItem item:ongoing) if(item.kind==ShelfItem.JELLYFIN)
            excludedIds.add(id(item.itemId));
        for(ShelfItem item:visibleMovies) {
            if(item.kind==ShelfItem.JELLYFIN) excludedIds.add(id(item.itemId));
            else if(item.kind==ShelfItem.WATCH_NEXT && item.packageName.contains("netflix"))
                netflixTitles.add(title(item.title));
        }
        List<ShelfItem> result=new ArrayList<>();
        Set<String> seen=new HashSet<>();
        for(ShelfItem item:candidates) {
            String identity=id(item.itemId);
            if(item.kind!=ShelfItem.JELLYFIN || identity.isEmpty() || excludedIds.contains(identity)
                    || netflixTitles.contains(title(item.title)) || !seen.add(identity)) continue;
            result.add(item);
            if(result.size()==2) break;
        }
        return result;
    }
    private static String id(String value) { return value.replace("-","").toLowerCase(Locale.ROOT); }
    private static String title(String value) {
        return Normalizer.normalize(value,Normalizer.Form.NFD).replaceAll("\\p{M}","")
            .toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]+"," ").trim();
    }
}
