package com.xsoulp.livingroom;

import android.content.Context;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.json.JSONArray;

/** Read-only fallback for Jellyfin clients publishing STATE_NONE during actual playback. */
final class JellyfinPlaybackProbe {
    static boolean playing(Context context,String player) throws Exception {
        JellyfinStore store=new JellyfinStore(context);
        String token=store.token();
        if(token.isEmpty() || store.server().isEmpty())return false;
        Set<String> addresses=new HashSet<>();
        for(NetworkInterface network:Collections.list(NetworkInterface.getNetworkInterfaces()))
            for(InetAddress address:Collections.list(network.getInetAddresses()))
                if(!address.isLoopbackAddress())addresses.add(address.getHostAddress());
        JellyfinApi api=new JellyfinApi(store.server(),token,store.userId(),store.deviceId());
        byte[] bytes=api.request("GET","Sessions?ActiveWithinSeconds=20",null,512*1024);
        return JellyfinPlaybackMatch.playing(new JSONArray(new String(bytes,StandardCharsets.UTF_8)),
            store.userId(),addresses,player,System.currentTimeMillis());
    }
}
