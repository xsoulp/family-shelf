package com.xsoulp.livingroom;

import org.junit.*;
import org.junit.rules.TemporaryFolder;
import static org.junit.Assert.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

public final class ShelfCacheTest {
    @Rule public TemporaryFolder temporary=new TemporaryFolder();
    private ShelfCache cache;
    private File directory;
    @Before public void setup() throws Exception { directory=temporary.newFolder();cache=new ShelfCache(directory); }
    private ShelfItem item() {
        ShelfItem item=new ShelfItem(ShelfItem.JELLYFIN,"jf:example","Example story","Saved progress",
            "org.jellyfin.androidtv","example","Items/example/Images/Primary?tag=v1","",4000,8000,"Overview");
        item.sortDate="2026-11-04";return item;
    }
    @Test public void shelvesSurviveANewCacheInstanceWithIdentityAndProgress() {
        cache.saveShelves("account-room-libraries",Collections.singletonList(item()));
        List<ShelfItem> saved=new ShelfCache(directory).shelves("account-room-libraries",1);
        assertEquals(1,saved.size());ShelfItem i=saved.get(0);
        assertEquals("jf:example",i.key);assertEquals("Example story",i.title);
        assertEquals(4000,i.positionMs);assertEquals(8000,i.durationMs);
        assertEquals(item().imagePath,i.imagePath);assertEquals("Overview",i.overview);
        assertEquals(item().sortDate,i.sortDate);
    }
    @Test public void roomAccountLibraryAndSelectionChangesCannotReadOtherScopes() {
        cache.saveShelves("account-A:kids:library-A:selection-A",Collections.singletonList(item()));
        for(String other:new String[]{"account-B:kids:library-A:selection-A","account-A:adults:library-A:selection-A",
                "account-A:kids:library-B:selection-A","account-A:kids:library-A:selection-B"})
            assertTrue(cache.shelves(other,1).isEmpty());
    }
    @Test public void corruptTruncatedAndWrongSizeShelvesAreIgnored() {
        cache.write("shelves:scope","[{".getBytes(StandardCharsets.UTF_8));
        assertTrue(cache.shelves("scope",1).isEmpty());
        cache.saveShelves("scope",Collections.singletonList(item()));
        assertTrue(cache.shelves("scope",28).isEmpty());
    }
    @Test public void diskArtworkSurvivesRestartAndUsesImageIdentity() {
        byte[] png={1,2,3,4};cache.write("poster:scope:Items/one?tag=a",png);
        assertArrayEquals(png,new ShelfCache(directory).read("poster:scope:Items/one?tag=a"));
        assertNull(cache.read("poster:scope:Items/one?tag=b"));
        assertNull(cache.read("poster:other-scope:Items/one?tag=a"));
    }
    @Test public void secretScopeAndPathsNeverAppearInFilenamesOrShelfData() throws Exception {
        String scope=ShelfCache.hash("server:user:secret-token:../../elsewhere");
        cache.saveShelves(scope,Collections.singletonList(item()));
        for(File file:directory.listFiles()) {
            assertTrue(file.getName().matches("[a-f0-9]{64}"));
            assertFalse(new String(Files.readAllBytes(file.toPath()),StandardCharsets.UTF_8).contains("secret-token"));
        }
    }
    @Test public void oversizedWritesAreIgnoredAndCacheHasAFixedEntryLimit() {
        cache.write("too-large",new byte[1024*1024+1]);assertNull(cache.read("too-large"));
        for(int i=0;i<140;i++)cache.write("poster:"+i,new byte[]{(byte)i});
        assertTrue(directory.listFiles().length<=128);
    }
    @Test public void signOutRemovesSavedShelvesAndImages() {
        cache.saveShelves("scope",Collections.singletonList(item()));cache.write("poster:scope",new byte[]{1});
        cache.clear();assertTrue(cache.shelves("scope",1).isEmpty());assertNull(cache.read("poster:scope"));
    }
}
