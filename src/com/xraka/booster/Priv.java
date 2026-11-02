package com.xraka.booster;

import android.content.pm.PackageManager;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import rikka.shizuku.Shizuku;

// Jembatan hak akses: root (su) bila ada, kalau tidak Shizuku (izin tingkat ADB)
// untuk perintah yang memang cukup dengan level shell.
public class Priv {
    private static Boolean rootCached = null;

    public static boolean hasRootCached() {
        if (rootCached == null) rootCached = Shell.hasRoot();
        return rootCached;
    }

    public static void resetRootCache() { rootCached = null; }

    public static boolean shizukuAlive() {
        try { return Shizuku.pingBinder(); } catch (Throwable t) { return false; }
    }

    public static boolean shizukuGranted() {
        try { return Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED; }
        catch (Throwable t) { return false; }
    }

    public static boolean shizukuReady() {
        return shizukuAlive() && shizukuGranted();
    }

    public static Shell.Result shizukuExec(String command) {
        Shell.Result r = new Shell.Result();
        try {
            // Di Shizuku API 13, newProcess bersifat internal sehingga dipanggil lewat refleksi.
            java.lang.reflect.Method m = Shizuku.class.getDeclaredMethod(
                "newProcess", String[].class, String[].class, String.class);
            m.setAccessible(true);
            Process p = (Process) m.invoke(null,
                new Object[]{ new String[]{"sh", "-c", command}, null, null });
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) sb.append(line).append("\n");
            r.out = sb.toString().trim();
            r.code = p.waitFor();
        } catch (Throwable t) {
            r.code = -1;
            r.out = "ERROR: " + t.getMessage();
        }
        return r;
    }

    // Menjalankan perintah dengan hak tertinggi yang tersedia.
    // rootOnly=true  -> hanya root yang cukup (file kernel/sistem).
    // rootOnly=false -> root atau Shizuku.
    public static Shell.Result exec(String command, boolean rootOnly) {
        if (hasRootCached()) return Shell.root(command);
        if (!rootOnly) {
            if (shizukuReady()) return shizukuExec(command);
            Shell.Result r = new Shell.Result();
            r.code = 1;
            r.out = (shizukuAlive() && !shizukuGranted()) ? "NEED_SHIZUKU_GRANT" : "NEED_PRIVILEGE";
            return r;
        }
        Shell.Result r = new Shell.Result();
        r.code = 1;
        r.out = "NEED_ROOT";
        return r;
    }
}
