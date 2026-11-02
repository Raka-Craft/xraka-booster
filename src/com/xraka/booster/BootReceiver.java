package com.xraka.booster;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context ctx, Intent intent) {
        final SharedPreferences prefs = ctx.getSharedPreferences("xraka", Context.MODE_PRIVATE);
        if (!prefs.getBoolean("boot_reapply", true)) return;
        new Thread(new Runnable() {
            public void run() {
                for (Features.F f : Features.all()) {
                    if (f.kind != Features.MODE_CMD) continue;
                    if (f.id.equals("doze_white")) continue;
                    if (f.onCmd == null) continue;
                    if (!prefs.getBoolean("feat_" + f.id, false)) continue;
                    try { Shell.root(f.onCmd); } catch (Exception e) { /* abaikan */ }
                }
            }
        }).start();
    }
}
