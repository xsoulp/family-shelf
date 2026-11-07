package com.xsoulp.livingroom;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.json.JSONObject;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Foreground-only passwordless pairing, with a local password form if Quick Connect is disabled. */
public final class JellyfinConnectActivity extends Activity {
    private final Handler main=new Handler(Looper.getMainLooper());
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Runnable poll=this::check;
    private JellyfinStore store;
    private JellyfinApi pending;
    private EditText server;
    private TextView status,code;
    private Button connect;
    private String secret="";
    private boolean foreground,inFlight;
    private volatile int generation;
    private long deadline;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        store=new JellyfinStore(this);
        LinearLayout layout=new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL); layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(dp(70),dp(24),dp(70),dp(24)); layout.setBackgroundColor(0xff132932);
        TextView title=label("Connect this room to Jellyfin",25); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        layout.addView(title);
        layout.addView(label("Your Jellyfin TV app stays signed in. This room needs its own access.\n"
            +"Approve the code in Jellyfin → your profile → Quick Connect.",14));
        server=new EditText(this); server.setSingleLine(true); server.setTextColor(0xfffaf0da);
        server.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_URI);
        server.setHint("http://server:8096"); server.setHintTextColor(0xffb9c6c4);
        server.setText(store.server());
        layout.addView(server,new LinearLayout.LayoutParams(-1,dp(46)));
        connect=new Button(this); connect.setText("Get Quick Connect code");
        connect.setOnClickListener(v -> begin()); layout.addView(connect);
        code=label("",36); code.setTypeface(Typeface.MONOSPACE,Typeface.BOLD); layout.addView(code);
        status=label("Your password is not needed for Quick Connect.",15); layout.addView(status);
        Button password=new Button(this); password.setText("Sign in with username and password");
        password.setOnClickListener(v -> passwordLogin()); layout.addView(password);
        Button back=new Button(this); back.setText("Back to room"); back.setOnClickListener(v -> finish());
        layout.addView(back);
        setContentView(layout);
        CreatorCredit.install(getWindow(),false);
        connect.requestFocus();
    }
    private int dp(int value) { return Math.round(value*getResources().getDisplayMetrics().density); }
    private TextView label(String value,int size) {
        TextView view=new TextView(this); view.setText(value); view.setTextSize(size);
        view.setTextColor(0xfff4e8ce); view.setGravity(Gravity.CENTER);
        view.setPadding(0,dp(3),0,dp(3)); return view;
    }
    private JellyfinApi candidate() {
        return new JellyfinApi(server.getText().toString(),"","",store.deviceId());
    }
    private void begin() {
        generation++; secret=""; main.removeCallbacks(poll); inFlight=false;
        final JellyfinApi api;
        try { api=candidate(); } catch(Exception e) { status.setText(e.getMessage()); return; }
        final int request=generation;
        connect.setEnabled(false); server.setEnabled(false); code.setText(""); status.setText("Contacting Jellyfin…");
        worker.execute(() -> {
            try {
                JSONObject response=api.initiate();
                String nextSecret=response.getString("Secret"),nextCode=response.getString("Code");
                main.post(() -> {
                    if(request!=generation || isFinishing()) return;
                    pending=api; secret=nextSecret; deadline=android.os.SystemClock.elapsedRealtime()+180000;
                    code.setText(nextCode); status.setText("Waiting for approval in Jellyfin…");
                    connect.setText("Request a new code"); connect.setEnabled(true);
                    if(foreground) main.postDelayed(poll,3000);
                });
            } catch(Exception e) { main.post(() -> fail(request,e)); }
        });
    }
    private void check() {
        if(!foreground || inFlight || secret.isEmpty()) return;
        if(android.os.SystemClock.elapsedRealtime()>deadline) {
            secret=""; status.setText("Code expired. Request a new code."); server.setEnabled(true); return;
        }
        final int request=generation; final String pendingSecret=secret; final JellyfinApi api=pending;
        inFlight=true;
        worker.execute(() -> {
            try {
                if(api.authorized(pendingSecret)) {
                    save(api,api.authenticate(pendingSecret),request);
                } else main.post(() -> {
                    if(request!=generation || isFinishing()) return;
                    inFlight=false;
                    if(foreground) main.postDelayed(poll,4000);
                });
            } catch(Exception e) { main.post(() -> fail(request,e)); }
        });
    }
    private void save(JellyfinApi api,JSONObject auth,int request) throws Exception {
        // A generation check also prevents a cancelled request from persisting an old login.
        if(request!=generation || isFinishing()) return;
        JSONObject user=auth.getJSONObject("User");
        String token=auth.getString("AccessToken"),id=user.getString("Id");
        if(token.isEmpty() || !ShelfItem.validId(id)) throw new java.io.IOException("Invalid sign-in response");
        store.save(api.server,token,id,user.optString("Name","Jellyfin"));
        main.post(() -> {
            if(request!=generation || isFinishing()) return;
            secret=""; setResult(RESULT_OK); finish();
        });
    }
    private void fail(int request,Exception error) {
        if(request!=generation || isFinishing()) return;
        inFlight=false; secret=""; connect.setEnabled(true); server.setEnabled(true);
        String message="Could not connect. Check the server URL and connection.";
        if(error instanceof JellyfinApi.ApiException) {
            int status=((JellyfinApi.ApiException)error).status;
            message=status==401||status==403 ? "Sign-in was rejected or Quick Connect is disabled. "
                +"Try a new code or username and password." : "Jellyfin returned HTTP "+status+". Try again.";
        }
        this.status.setText(message);
    }
    private void passwordLogin() {
        generation++; main.removeCallbacks(poll); secret=""; server.setEnabled(true); connect.setEnabled(true);
        final JellyfinApi api;
        try { api=candidate(); } catch(Exception e) { status.setText(e.getMessage()); return; }
        LinearLayout form=new LinearLayout(this); form.setOrientation(LinearLayout.VERTICAL); form.setPadding(dp(20),0,dp(20),0);
        EditText username=new EditText(this); username.setHint("Username"); username.setSingleLine(true); form.addView(username);
        EditText password=new EditText(this); password.setHint("Password"); password.setSingleLine(true);
        password.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD); form.addView(password);
        new CreatorCredit.DialogBuilder(this).setTitle("Sign in to Jellyfin").setView(form)
            .setNegativeButton("Cancel",null).setPositiveButton("Sign in",(d,w) -> {
                final int request=++generation;
                final String name=username.getText().toString(),pw=password.getText().toString();
                password.setText(""); status.setText("Signing in…"); connect.setEnabled(false);
                worker.execute(() -> {
                    try { save(api,api.json("POST","Users/AuthenticateByName",
                        new JSONObject().put("Username",name).put("Pw",pw)),request); }
                    catch(Exception e) { main.post(() -> fail(request,e)); }
                });
            }).show();
    }
    @Override protected void onResume() {
        super.onResume(); foreground=true;
        if(!secret.isEmpty()) main.post(poll);
    }
    @Override protected void onPause() { foreground=false; main.removeCallbacks(poll); super.onPause(); }
    @Override protected void onDestroy() { generation++; main.removeCallbacksAndMessages(null); worker.shutdownNow(); super.onDestroy(); }
}
