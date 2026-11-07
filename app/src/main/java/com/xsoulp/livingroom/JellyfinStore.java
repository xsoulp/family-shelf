package com.xsoulp.livingroom;

import android.content.Context;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.util.Base64;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

/** User token encrypted with a non-exportable Android Keystore key; no passwords persisted. */
final class JellyfinStore {
    private static final String ALIAS="livingroom-jellyfin-v1";
    private final SharedPreferences prefs;
    JellyfinStore(Context context) { prefs=context.getSharedPreferences("jellyfin",Context.MODE_PRIVATE); }
    String server() { return prefs.getString("server",""); }
    String userId() { return prefs.getString("userId",""); }
    String userName() { return prefs.getString("userName",""); }
    synchronized String deviceId() {
        String id=prefs.getString("deviceId","");
        if (id.isEmpty()) { id=UUID.randomUUID().toString(); prefs.edit().putString("deviceId",id).commit(); }
        return id;
    }
    private SecretKey key() throws Exception {
        KeyStore store=KeyStore.getInstance("AndroidKeyStore"); store.load(null);
        if (store.containsAlias(ALIAS)) return (SecretKey)store.getKey(ALIAS,null);
        KeyGenerator generator=KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES,"AndroidKeyStore");
        generator.init(new KeyGenParameterSpec.Builder(ALIAS,
            KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());
        return generator.generateKey();
    }
    String token() {
        try {
            String encrypted=prefs.getString("token","");
            if (encrypted.isEmpty()) return "";
            Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,
                Base64.decode(prefs.getString("iv",""),Base64.NO_WRAP)));
            return new String(cipher.doFinal(Base64.decode(encrypted,Base64.NO_WRAP)),StandardCharsets.UTF_8);
        } catch (Exception e) { return ""; }
    }
    void save(String server,String token,String userId,String userName) throws Exception {
        Cipher cipher=Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE,key());
        String encrypted=Base64.encodeToString(cipher.doFinal(token.getBytes(StandardCharsets.UTF_8)),Base64.NO_WRAP);
        if (!prefs.edit().putString("server",server).putString("userId",userId).putString("userName",userName)
                .putString("token",encrypted).putString("iv",Base64.encodeToString(cipher.getIV(),Base64.NO_WRAP)).commit())
            throw new java.io.IOException("Could not save the session");
    }
    void clear() { prefs.edit().remove("token").remove("iv").remove("userId").remove("userName").commit(); }
}
