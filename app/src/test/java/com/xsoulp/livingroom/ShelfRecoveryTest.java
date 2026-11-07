package com.xsoulp.livingroom;

import org.junit.Test;
import static org.junit.Assert.*;

public final class ShelfRecoveryTest {
    @Test public void retriesStartupNetworkAndTemporaryServerFailures() {
        assertTrue(ShelfRecovery.retryable(new java.net.ConnectException()));
        assertTrue(ShelfRecovery.retryable(new java.net.SocketTimeoutException()));
        assertTrue(ShelfRecovery.retryable(new java.net.UnknownHostException()));
        for(int status:new int[]{408,429,500,502,503,504})
            assertTrue(ShelfRecovery.retryable(new JellyfinApi.ApiException(status)));
    }
    @Test public void missingArtAndRejectedSessionsDoNotLoop() {
        for(int status:new int[]{400,401,403,404,410})
            assertFalse(ShelfRecovery.retryable(new JellyfinApi.ApiException(status)));
        assertFalse(ShelfRecovery.retryable(new IllegalArgumentException()));
    }
    @Test public void retryDelayBacksOffAndStaysBounded() {
        assertEquals(2000,ShelfRecovery.delay(0));assertEquals(4000,ShelfRecovery.delay(1));
        assertEquals(8000,ShelfRecovery.delay(2));assertEquals(16000,ShelfRecovery.delay(3));
        assertEquals(32000,ShelfRecovery.delay(4));
        assertEquals(60000,ShelfRecovery.delay(5));assertEquals(60000,ShelfRecovery.delay(1000));
    }
}
