package com.xsoulp.livingroom;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public final class JellyfinApiTest {
    private static final String USER="11111111111111111111111111111111";
    private static final String PARENT="22222222222222222222222222222222";
    private static final String MOVIE="33333333333333333333333333333333";
    private ServerSocket server;
    private Thread worker;
    private volatile Exception serverFailure;
    private JellyfinApi api;
    private final List<String> requests=new java.util.concurrent.CopyOnWriteArrayList<>();
    private volatile int modernStatus=200;
    private volatile String auth,query;

    @Before public void start() throws Exception {
        server=new ServerSocket(0,8,java.net.InetAddress.getByName("127.0.0.1"));
        worker=new Thread(() -> {
            while(!server.isClosed()) {
                try(Socket socket=server.accept()) { respond(socket); }
                catch(Exception error) { if(!server.isClosed()) serverFailure=error; }
            }
        });
        worker.setDaemon(true); worker.start();
        api=new JellyfinApi("http://127.0.0.1:"+server.getLocalPort()+"/jellyfin",
            "test-token",USER,"test-device");
    }
    @After public void stop() throws Exception {
        server.close(); worker.join(2000);
        assertNull("Loopback server failed",serverFailure);
    }
    private void respond(Socket socket) throws java.io.IOException {
        socket.setSoTimeout(5000);
        BufferedReader input=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.US_ASCII));
        URI uri=URI.create(input.readLine().split(" ")[1]);
        String path=uri.getPath();
        requests.add(path);
        auth=null;
        String line;
        while((line=input.readLine())!=null && !line.isEmpty()) {
            int colon=line.indexOf(':');
            if(colon>0 && line.substring(0,colon).equalsIgnoreCase("Authorization"))
                auth=line.substring(colon+1).trim();
        }
        query=uri.getRawQuery();
        boolean modern=path.endsWith("/UserViews") || path.endsWith("/UserItems/Resume");
        int status=modern?modernStatus:200;
        // Simulate a server with legacy header authentication disabled.
        if(auth==null || !auth.contains("Token=\"test-token\"")) status=401;
        String body=path.endsWith("/Views") || path.endsWith("/UserViews")?
            "{\"Items\":[{\"Id\":\""+PARENT+"\",\"Name\":\"Movies (kids)\"}]}":
            "{\"Items\":[{\"Id\":\""+MOVIE+"\",\"Name\":\"Example movie\",\"Type\":\"Movie\","
            +"\"DateCreated\":\"2026-11-01T12:00:00Z\",\"RunTimeTicks\":6000000000,"
            +"\"UserData\":{\"Played\":false,\"PlaybackPositionTicks\":3000000000,"
            +"\"LastPlayedDate\":\"2026-11-01T13:00:00Z\"}}]}";
        byte[] bytes=body.getBytes(StandardCharsets.UTF_8);
        java.io.OutputStream output=socket.getOutputStream();
        output.write(("HTTP/1.1 "+status+" Test\r\nContent-Type: application/json\r\nContent-Length: "
            +bytes.length+"\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        output.write(bytes); output.flush();
    }
    @Test public void startupServerFailureCanRecoverWithoutOpeningJellyfin() throws Exception {
        modernStatus=503;
        try { api.libraries();fail("Expected temporary server failure"); }
        catch(JellyfinApi.ApiException e) { assertTrue(ShelfRecovery.retryable(e)); }
        modernStatus=200;
        assertEquals(1,api.libraries().length());
        assertEquals(2,requests.size());
        assertTrue(auth.contains("Token=\"test-token\""));
    }
    @Test public void modernLibrariesUseAuthorizationTokenAndBasePath() throws Exception {
        assertEquals(1,api.libraries().length());
        assertEquals("/jellyfin/UserViews",requests.get(0));
        assertEquals("userId="+USER,query);
        assertTrue(auth.startsWith("MediaBrowser "));
        assertTrue(auth.contains("Token=\"test-token\""));
        assertFalse(query.contains("test-token"));
    }
    @Test public void legacyViewsFallbackOnlyWhenRouteMissing() throws Exception {
        modernStatus=404;
        assertEquals(1,api.libraries().length());
        assertEquals(2,requests.size());
        assertEquals("/jellyfin/Users/"+USER+"/Views",requests.get(1));
    }
    @Test public void invalidSessionDoesNotTryLegacyRoutes() throws Exception {
        modernStatus=401;
        try { api.libraries(); fail("Expected authentication failure"); }
        catch(JellyfinApi.ApiException expected) { assertEquals(401,expected.status); }
        assertEquals(1,requests.size());
    }
    @Test public void forbiddenSessionDoesNotTryLegacyRoutes() throws Exception {
        modernStatus=403;
        try { api.shelf(PARENT,true,false); fail("Expected permission failure"); }
        catch(JellyfinApi.ApiException expected) { assertEquals(403,expected.status); }
        assertEquals(1,requests.size());
    }
    @Test public void resumeIsScopedToUserLibraryAndType() throws Exception {
        List<ShelfItem> items=api.shelf(PARENT,true,false);
        assertEquals(1,items.size());
        assertEquals("/jellyfin/UserItems/Resume",requests.get(0));
        assertTrue(query.contains("userId="+USER));
        assertTrue(query.contains("parentId="+PARENT));
        assertTrue(query.contains("includeItemTypes=Movie"));
        assertEquals(300000,items.get(0).positionMs);
        assertEquals("2026-11-01T13:00:00Z",items.get(0).sortDate);
    }
    @Test public void seriesResumeRequestsEpisodes() throws Exception {
        api.shelf(PARENT,true,true);
        assertTrue(query.contains("includeItemTypes=Episode"));
        assertTrue(query.contains("parentId="+PARENT));
    }
    @Test public void legacyResumeFallbackKeepsLibraryScope() throws Exception {
        modernStatus=404;
        assertEquals(1,api.shelf(PARENT,true,false).size());
        assertEquals("/jellyfin/Users/"+USER+"/Items/Resume",requests.get(1));
        assertTrue(query.contains("parentId="+PARENT));
    }
    @Test public void recentMoviesStillUseDateCreatedItemsEndpoint() throws Exception {
        ShelfItem item=api.shelf(PARENT,false,false).get(0);
        assertEquals("/jellyfin/Items",requests.get(0));
        assertTrue(query.contains("sortBy=DateCreated"));
        assertTrue(query.contains("includeItemTypes=Movie"));
        assertEquals("2026-11-01T12:00:00Z",item.sortDate);
        assertEquals(0,item.positionMs);
    }
}
