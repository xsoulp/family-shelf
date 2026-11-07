package com.xsoulp.livingroom;

/** Retry transient startup/network failures without repeatedly retrying a rejected session or missing image. */
final class ShelfRecovery {
    static boolean retryable(Exception error) {
        if(error instanceof JellyfinApi.ApiException) {
            int status=((JellyfinApi.ApiException)error).status;
            return status==408 || status==429 || status>=500;
        }
        return error instanceof java.io.IOException;
    }
    static long delay(int attempt) {
        return Math.min(60000L,2000L << Math.min(5,Math.max(0,attempt)));
    }
}
