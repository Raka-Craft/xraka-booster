package com.xraka.booster;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout homeBox, gamesBox, settingsBox;
    private Button navHome, navGames, navSettings;
    private TextView rootStatusView;
    private TextView shizukuStatusView;
    private boolean suppress = false;
    private volatile Boolean rootCache = null;

    private static class Row {
        Switch sw; TextView status; Features.F f;
    }
    private final Map<String, Row> rows = new HashMap<String, Row>();

    private int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("xraka", MODE_PRIVATE);

        FrameLayout root = new FrameLayout(this);
        ImageView bg = new ImageView(this);
        bg.setImageResource(R.drawable.bg_anime);
        bg.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(bg, new FrameLayout.LayoutParams(-1, -1));
        View shade = new View(this);
        shade.setBackgroundColor(Color.argb(120, 4, 5, 12));
        root.addView(shade, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout main = new LinearLayout(this);
        main.setOrientation(LinearLayout.VERTICAL);
        root.addView(main, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
       header.setPadding(dp(16), dp(18), dp(16), dp(8));
        TextView title = new TextView(this);
        title.setText("XRaka Booster");
        title.setTextSize(26);
        title.setTextColor(Color.WHITE);
        title.setTypeface(null, android.graphics.Typeface.BOLD);
        header.addView(title);
        TextView sub = new TextView(this);
        sub.setText("Booster game khusus HP root. Setiap saklar menjalankan perintah asli dan melaporkan hasilnya apa adanya.");
        sub.setTextSize(13);
        sub.setTextColor(Color.rgb(200, 204, 220));
        header.addView(sub);
        rootStatusView = new TextView(this);
        rootStatusView.setText("Status Root: memeriksa...");
        rootStatusView.setTextSize(13);
        rootStatusView.setTextColor(Color.rgb(255, 213, 109));
        rootStatusView.setPadding(0, dp(6), 0, 0);
        header.addView(rootStatusView);
        shizukuStatusView = new TextView(this);
        shizukuStatusView.setText("Status Shizuku: memeriksa...");
        shizukuStatusView.setTextSize(13);
        shizukuStatusView.setTextColor(Color.rgb(255, 213, 109));
        shizukuStatusView.setPadding(0, dp(2), 0, 0);
        header.addView(shizukuStatusView);
        main.addView(header);

        ScrollView scroll = new ScrollView(this);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(-1, 0, 1f));
        FrameLayout content = new FrameLayout(this);
        scroll.addView(content);
        main.addView(scroll);

        homeBox = new LinearLayout(this); homeBox.setOrientation(LinearLayout.VERTICAL);
        gamesBox = new LinearLayout(this); gamesBox.setOrientation(LinearLayout.VERTICAL);
        settingsBox = new LinearLayout(this); settingsBox.setOrientation(LinearLayout.VERTICAL);
        content.addView(homeBox);
        content.addView(gamesBox);
        content.addView(settingsBox);

        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        nav.setBackgroundColor(Color.argb(235, 8, 9, 18));
        nav.setPadding(dp(8), dp(8), dp(8), dp(10));
        navHome = navButton("Beranda");
        navGames = navButton("Game");
        navSettings = navButton("Pengaturan");
        nav.addView(navHome); nav.addView(navGames); nav.addView(navSettings);
        main.addView(nav);

        navHome.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showTab(0); }
        });
        navGames.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { buildGames(); showTab(1); }
        });
        navSettings.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showTab(2); }
        });

        setContentView(root);

        try { rikka.shizuku.Shizuku.addRequestPermissionResultListener(permListener); } catch (Throwable t) { /* abaikan */ }
        buildHome();
        buildGames();
        buildSettings();
        showTab(0);
        checkRoot(true);
    }

    private Button navButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(14);
        b.setTextColor(Color.WHITE);
        b.setLayoutParams(new LinearLayout.LayoutParams(0, dp(46), 1f));
        return b;
    }

    private void styleNav() {
        Button[] bs = {navHome, navGames, navSettings};
        int active = gamesBox.getVisibility() == View.VISIBLE ? 1
                : (settingsBox.getVisibility() == View.VISIBLE ? 2 : 0);
        for (int i = 0; i < 3; i++) {
            bs[i].setBackground(gradBg(10, i == active));
        }
    }

    private void showTab(int i) {
        homeBox.setVisibility(i == 0 ? View.VISIBLE : View.GONE);
        gamesBox.setVisibility(i == 1 ? View.VISIBLE : View.GONE);
        settingsBox.setVisibility(i == 2 ? View.VISIBLE : View.GONE);
        styleNav();
    }

    private final rikka.shizuku.Shizuku.OnRequestPermissionResultListener permListener =
        new rikka.shizuku.Shizuku.OnRequestPermissionResultListener() {
            public void onRequestPermissionResult(int requestCode, int grantResult) {
                boolean granted = grantResult == PackageManager.PERMISSION_GRANTED;
                Toast.makeText(MainActivity.this,
                    granted ? "Izin Shizuku diberikan." : "Izin Shizuku ditolak.",
                    Toast.LENGTH_SHORT).show();
                checkRoot(true);
            }
        };

    private void checkRoot(final boolean updateHeader) {
        new Thread(new Runnable() {
            public void run() {
                Priv.resetRootCache();
                final boolean r = Priv.hasRootCached();
                rootCache = r;
                final boolean alive = Priv.shizukuAlive();
                final boolean granted = alive && Priv.shizukuGranted();
                if (updateHeader) runOnUiThread(new Runnable() {
                    public void run() {
                        if (r) {
                            rootStatusView.setText("Status Root: Terdeteksi. Semua fitur bisa bekerja.");
                            rootStatusView.setTextColor(Color.rgb(109, 255, 143));
                        } else {
                            rootStatusView.setText("Status Root: Tidak terdeteksi. Fitur bertanda ROOT tidak akan bekerja di HP ini.");
                            rootStatusView.setTextColor(Color.rgb(255, 138, 128));
                        }
                        if (granted) {
                            shizukuStatusView.setText("Status Shizuku: Aktif dan izin diberikan. Fitur bertanda ROOT / SHIZUKU bisa bekerja.");
                            shizukuStatusView.setTextColor(Color.rgb(109, 255, 143));
                        } else if (alive) {
                            shizukuStatusView.setText("Status Shizuku: Terdeteksi, tapi izin belum diberikan. Tekan tombol Minta Izin Shizuku di Pengaturan.");
                            shizukuStatusView.setTextColor(Color.rgb(255, 213, 109));
                        } else {
                            shizukuStatusView.setText("Status Shizuku: Tidak aktif. Pasang aplikasi Shizuku bila ingin memakai fitur tanpa root.");
                            shizukuStatusView.setTextColor(Color.rgb(255, 138, 128));
                        }
                    }
                });
            }
        }).start();
    }

    private GradientDrawable cardBg() {
        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(dp(14));
        g.setColor(Color.argb(205, 13, 15, 28));
        g.setStroke(dp(1), Color.argb(90, 160, 107, 255));
        return g;
    }

    private GradientDrawable gradBg(int radiusDp, boolean vivid) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
            vivid ? new int[]{Color.rgb(3, 3, 8), Color.rgb(59, 17, 115), Color.rgb(123, 47, 247)}
                  : new int[]{Color.rgb(3, 3, 8), Color.rgb(17, 13, 30), Color.rgb(40, 29, 70)});
        g.setCornerRadius(dp(radiusDp));
        return g;
    }

    private TextView sectionTitle(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(17);
        t.setTextColor(Color.rgb(196, 164, 255));
        t.setTypeface(null, android.graphics.Typeface.BOLD);
        t.setPadding(dp(14), dp(16), dp(14), dp(4));
        return t;
    }

    private void buildHome() {
        homeBox.removeAllViews();
        rows.clear();
        homeBox.addView(sectionTitle("MODE PERFORMA"));
        boolean actionsStarted = false;
        for (final Features.F f : Features.all()) {
            if (f.kind == Features.ACTION_CMD && !actionsStarted) {
                actionsStarted = true;
                homeBox.addView(sectionTitle("AKSI CEPAT (SEKALI JALAN)"));
            }
            homeBox.addView(featureRow(f));
        }
        TextView note = new TextView(this);
        note.setText("Catatan: mode performa kembali ke bawaan HP setelah HP direstart. Aktifkan saklar Jalankan Ulang Setelah Restart di Pengaturan, atau buka lagi aplikasi ini setelah restart.");
        note.setTextSize(12);
        note.setTextColor(Color.rgb(160, 166, 186));
        note.setPadding(dp(14), dp(10), dp(14), dp(20));
        homeBox.addView(note);
    }

    private View featureRow(final Features.F f) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBg());
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(10), dp(6), dp(10), dp(6));
        card.setLayoutParams(lp);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = new TextView(this);
        int lv = Features.levelOf(f);
        String suf = lv == Features.LEVEL_ROOT ? "  (ROOT)" : lv == Features.LEVEL_SHELL ? "  (ROOT / SHIZUKU)" : "";
        name.setText(f.title + suf);
        name.setTextSize(16);
        name.setTextColor(Color.WHITE);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        top.addView(name);
        final Switch sw = new Switch(this);
        top.addView(sw);
        card.addView(top);

        TextView desc = new TextView(this);
        desc.setText(f.desc);
        desc.setTextSize(13);
        desc.setTextColor(Color.rgb(184, 188, 203));
        desc.setPadding(0, dp(4), 0, 0);
        card.addView(desc);

        final TextView status = new TextView(this);
        status.setTextSize(12);
        status.setPadding(0, dp(6), 0, 0);
        card.addView(status);

        final Row row = new Row();
        row.sw = sw; row.status = status; row.f = f;
        rows.put(f.id, row);

        boolean saved = prefs.getBoolean("feat_" + f.id, false);
        if (f.kind == Features.ACTION_CMD) {
            setStatus(row, "Belum dijalankan.", Color.rgb(150, 156, 176));
        } else if (saved) {
            suppress = true; sw.setChecked(true); suppress = false;
            setStatus(row, "Aktif (tersimpan). Berlaku sejak saklar dinyalakan.", Color.rgb(109, 255, 143));
        } else {
            setStatus(row, "Nonaktif.", Color.rgb(150, 156, 176));
        }

        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton btn, boolean checked) {
                if (suppress) return;
                onToggle(row, checked);
            }
        });
        return card;
    }

    private void setStatus(Row row, String text, int color) {
        row.status.setText(text);
        row.status.setTextColor(color);
    }

    private void setSwitch(Row row, boolean v) {
        suppress = true;
        row.sw.setChecked(v);
        suppress = false;
    }

    private void onToggle(final Row row, final boolean on) {
        final Features.F f = row.f;
        row.sw.setEnabled(false);
        setStatus(row, "Menjalankan perintah...", Color.rgb(255, 213, 109));
        new Thread(new Runnable() {
            public void run() {
                final Shell.Result res = applyFeature(f, on);
                runOnUiThread(new Runnable() {
                    public void run() {
                        row.sw.setEnabled(true);
                        finishToggle(row, on, res);
                    }
                });
            }
        }).start();
    }

    private void finishToggle(Row row, boolean on, Shell.Result res) {
        Features.F f = row.f;
        boolean ok = res != null && res.ok();
        String detail = detailOf(res == null ? "" : res.out);
        if (f.kind == Features.ACTION_CMD) {
            setSwitch(row, false);
            if (ok) setStatus(row, "Berhasil dijalankan pukul " + nowTime() + "." + detail, Color.rgb(109, 255, 143));
            else setStatus(row, "Gagal dijalankan." + failHint(f, res) + detail, Color.rgb(255, 138, 128));
            return;
        }
        if (ok) {
            prefs.edit().putBoolean("feat_" + f.id, on).apply();
            setSwitch(row, on);
            if (on) setStatus(row, "Aktif - perintah berhasil diterapkan." + detail, Color.rgb(109, 255, 143));
            else setStatus(row, "Nonaktif - sudah dikembalikan ke bawaan.", Color.rgb(150, 156, 176));
        } else {
            setSwitch(row, !on);
            setStatus(row, (on ? "Gagal menyalakan." : "Gagal mematikan.") + failHint(f, res) + detail, Color.rgb(255, 138, 128));
        }
    }

    private String failHint(Features.F f, Shell.Result res) {
        String out = (res == null || res.out == null) ? "" : res.out;
        if (out.contains("NEED_ROOT")) return " Fitur ini wajib root penuh, Shizuku tidak cukup.";
        if (out.contains("NEED_SHIZUKU_GRANT")) return " Izin Shizuku belum diberikan. Tekan tombol Minta Izin Shizuku di Pengaturan.";
        if (out.contains("NEED_PRIVILEGE")) return " Butuh root atau Shizuku yang aktif dan diberi izin.";
        if (out.contains("NO_GAMES")) return " Pilih game dulu di tab Game.";
        if (out.contains("NO_RUNNING")) return " Game pilihan belum ada yang berjalan.";
        if (out.contains("NO_PERMISSION")) return " Izin Jangan Ganggu belum diberikan.";
        if (Features.levelOf(f) != Features.LEVEL_NONE && (rootCache == null || !rootCache) && !Priv.shizukuReady())
            return " HP ini belum terdeteksi root dan Shizuku belum aktif.";
        return " Perintah sistem menolak.";
    }

    private String detailOf(String out) {
        if (out == null) return "";
        Matcher m = Pattern.compile("(POLICY|GPU|BLOCK|NET|STOPPED|PRIO|GAMES):(\\d+)").matcher(out);
        if (m.find()) {
            String k = m.group(1); int n = Integer.parseInt(m.group(2));
            String label = k.equals("POLICY") ? "klaster CPU" : k.equals("GPU") ? "bagian GPU"
                : k.equals("BLOCK") ? "perangkat penyimpanan" : k.equals("NET") ? "parameter jaringan"
                : k.equals("STOPPED") ? "aplikasi dihentikan" : k.equals("PRIO") ? "proses diprioritaskan"
                : "game diproses";
            return " (" + n + " " + label + ")";
        }
        Matcher m2 = Pattern.compile("RES:(\\d+x\\d+)").matcher(out);
        if (m2.find()) return " (resolusi sekarang " + m2.group(1) + ")";
        return "";
    }

    private String nowTime() {
        return new SimpleDateFormat("HH:mm", Locale.getDefault()).format(new Date());
    }

    private Set<String> selectedGames() {
        return new HashSet<String>(prefs.getStringSet("games", new HashSet<String>()));
    }

    // Mengembalikan Result; code 0 = sukses. Dipakai juga oleh BootReceiver lewat Features statis di bawah.
    private Shell.Result applyFeature(Features.F f, boolean on) {
        if (f.kind == Features.MODE_DND) return applyDnd(on);
        if (f.kind == Features.MODE_RESOLUTION) return applyResolution(on);
        String cmd;
        if (f.id.equals("doze_white")) {
            Set<String> games = selectedGames();
            if (games.isEmpty()) { Shell.Result r = new Shell.Result(); r.code = 1; r.out = "NO_GAMES"; return r; }
            StringBuilder sb = new StringBuilder();
            int n = 0;
            for (String pkg : games) {
                sb.append("cmd deviceidle whitelist ").append(on ? "+" : "-").append(pkg).append(" 2>/dev/null; ");
                n++;
            }
            sb.append("echo GAMES:").append(n);
            cmd = sb.toString();
            Shell.Result r = Priv.exec(cmd, false);
            if (r.code != 0) { r.code = 0; } // cmd deviceidle kadang memberi kode aneh walau berhasil
            return r;
        }
        if (f.id.equals("act_stopbg")) {
            List<String> pkgs = backgroundPackages();
            if (pkgs.isEmpty()) { Shell.Result r = new Shell.Result(); r.code = 0; r.out = "STOPPED:0"; return r; }
            StringBuilder sb = new StringBuilder("n=0; for p in");
            for (String pkg : pkgs) sb.append(" ").append(pkg);
            sb.append("; do am force-stop $p 2>/dev/null && n=$((n+1)); done; echo STOPPED:$n");
            return Priv.exec(sb.toString(), false);
        }
        if (f.id.equals("act_prio")) {
            Set<String> games = selectedGames();
            if (games.isEmpty()) { Shell.Result r = new Shell.Result(); r.code = 1; r.out = "NO_GAMES"; return r; }
            StringBuilder sb = new StringBuilder("n=0; for p in");
            for (String pkg : games) sb.append(" ").append(pkg);
            sb.append("; do for pid in $(pidof $p); do renice -n -15 -p $pid 2>/dev/null && n=$((n+1)); done; done; echo PRIO:$n; [ $n -gt 0 ] || echo NO_RUNNING; [ $n -gt 0 ]");
            Shell.Result r = Priv.exec(sb.toString(), true);
            if (r.code != 0 && (r.out == null || !r.out.contains("NO_RUNNING"))) { /* tetap gagal */ }
            if (r.code != 0) { Shell.Result x = new Shell.Result(); x.code = 1; x.out = "NO_RUNNING"; return x; }
            return r;
        }
        cmd = on ? f.onCmd : f.offCmd;
        if (cmd == null) { Shell.Result r = new Shell.Result(); r.code = 1; r.out = ""; return r; }
        return Priv.exec(cmd, Features.levelOf(f) == Features.LEVEL_ROOT);
    }

    private Shell.Result applyDnd(boolean on) {
        Shell.Result r = new Shell.Result();
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (!nm.isNotificationPolicyAccessGranted()) {
            r.code = 1; r.out = "NO_PERMISSION";
            Intent i = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try { startActivity(i); } catch (Exception e) { /* abaikan */ }
            return r;
        }
        nm.setInterruptionFilter(on ? NotificationManager.INTERRUPTION_FILTER_NONE
                                     : NotificationManager.INTERRUPTION_FILTER_ALL);
        r.code = 0; r.out = "DND_OK";
        return r;
    }

    private Shell.Result applyResolution(boolean on) {
        Shell.Result r = new Shell.Result();
        if (!on) {
            Shell.Result x = Priv.exec("wm size reset", false);
            prefs.edit().remove("orig_w").remove("orig_h").apply();
            r.code = x.code; r.out = x.out;
            return r;
        }
        Shell.Result cur = Priv.exec("wm size", false);
        int W = 0, H = 0;
        Matcher m = Pattern.compile("Physical size:\\s*(\\d+)x(\\d+)").matcher(cur.out == null ? "" : cur.out);
        if (m.find()) { W = Integer.parseInt(m.group(1)); H = Integer.parseInt(m.group(2)); }
        if (W == 0) {
            DisplayMetrics dm = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(dm);
            W = dm.widthPixels; H = dm.heightPixels;
        }
        if (!prefs.contains("orig_w")) prefs.edit().putInt("orig_w", W).putInt("orig_h", H).apply();
        int pct = prefs.getInt("res_percent", 80);
        int nw = (W * pct / 100) & ~1;
        int nh = (H * pct / 100) & ~1;
        Shell.Result x = Priv.exec("wm size " + nw + "x" + nh, false);
        r.code = x.code;
        r.out = "RES:" + nw + "x" + nh;
        return r;
    }

    private List<String> backgroundPackages() {
        List<String> out = new ArrayList<String>();
        PackageManager pm = getPackageManager();
        Set<String> games = selectedGames();
        String homePkg = null;
        try {
            Intent hi = new Intent(Intent.ACTION_MAIN);
            hi.addCategory(Intent.CATEGORY_HOME);
            android.content.pm.ResolveInfo ri = pm.resolveActivity(hi, 0);
            if (ri != null && ri.activityInfo != null) homePkg = ri.activityInfo.packageName;
        } catch (Exception e) { /* abaikan */ }
        for (ApplicationInfo ai : pm.getInstalledApplications(0)) {
            if ((ai.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            if (ai.packageName.equals(getPackageName())) continue;
            if (games.contains(ai.packageName)) continue;
            if (ai.packageName.equals(homePkg)) continue;
            out.add(ai.packageName);
        }
        return out;
    }

    private TextView infoText(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(13);
        t.setTextColor(Color.rgb(184, 188, 203));
        t.setPadding(dp(14), dp(2), dp(14), dp(8));
        return t;
    }

    private Button bigButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(15);
        b.setTextColor(Color.WHITE);
        b.setBackground(gradBg(12, true));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(50));
        lp.setMargins(dp(10), dp(8), dp(10), dp(8));
        b.setLayoutParams(lp);
        return b;
    }

    private void buildGames() {
        gamesBox.removeAllViews();
        gamesBox.addView(sectionTitle("GAME PILIHAN"));
        gamesBox.addView(infoText("Tambahkan game dari aplikasi yang terpasang di HP ini. Game pilihan dipakai oleh fitur Game Anti Ke-Kill, Prioritaskan Game, dan dikecualikan dari Hentikan Aplikasi Latar."));
        Button add = bigButton("Tambah Game dari Aplikasi di HP");
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { showAppPicker(); }
        });
        gamesBox.addView(add);

        final PackageManager pm = getPackageManager();
        List<String> games = new ArrayList<String>(selectedGames());
        Collections.sort(games, new Comparator<String>() {
            public int compare(String a, String b) {
                return appLabel(pm, a).compareToIgnoreCase(appLabel(pm, b));
            }
        });
        if (games.isEmpty()) {
            gamesBox.addView(infoText("Belum ada game dipilih. Tekan tombol di atas untuk memilih."));
        }
        for (final String pkg : games) {
            LinearLayout card = new LinearLayout(this);
            card.setOrientation(LinearLayout.HORIZONTAL);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setBackground(cardBg());
            card.setPadding(dp(12), dp(10), dp(12), dp(10));
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.setMargins(dp(10), dp(6), dp(10), dp(6));
            card.setLayoutParams(lp);

            ImageView icon = new ImageView(this);
            icon.setLayoutParams(new LinearLayout.LayoutParams(dp(42), dp(42)));
            try { icon.setImageDrawable(pm.getApplicationIcon(pkg)); } catch (Exception e) { /* abaikan */ }
            card.addView(icon);

            LinearLayout mid = new LinearLayout(this);
            mid.setOrientation(LinearLayout.VERTICAL);
            mid.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            mid.setPadding(dp(10), 0, dp(6), 0);
            TextView nm = new TextView(this);
            nm.setText(appLabel(pm, pkg));
            nm.setTextSize(15);
            nm.setTextColor(Color.WHITE);
            nm.setTypeface(null, android.graphics.Typeface.BOLD);
            mid.addView(nm);
            TextView pk = new TextView(this);
            pk.setText(pkg);
            pk.setTextSize(11);
            pk.setTextColor(Color.rgb(150, 156, 176));
            mid.addView(pk);
            card.addView(mid);

            Button play = new Button(this);
            play.setText("Main");
            play.setTextColor(Color.WHITE);
            play.setBackground(gradBg(10, true));
            play.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) { launchGame(pkg); }
            });
            card.addView(play);
            Button del = new Button(this);
            del.setText("Hapus");
            del.setTextColor(Color.WHITE);
            del.setBackground(gradBg(10, false));
            del.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    Set<String> s = selectedGames();
                    s.remove(pkg);
                    prefs.edit().putStringSet("games", s).apply();
                    buildGames();
                }
            });
            card.addView(del);
            gamesBox.addView(card);
        }
    }

    private String appLabel(PackageManager pm, String pkg) {
        try {
            ApplicationInfo ai = pm.getApplicationInfo(pkg, 0);
            return pm.getApplicationLabel(ai).toString();
        } catch (Exception e) {
            return pkg;
        }
    }

    private void showAppPicker() {
        final PackageManager pm = getPackageManager();
        Set<String> sel = selectedGames();
        final List<String> pkgs = new ArrayList<String>();
        List<String> names = new ArrayList<String>();
        List<ApplicationInfo> apps = pm.getInstalledApplications(0);
        Collections.sort(apps, new Comparator<ApplicationInfo>() {
            public int compare(ApplicationInfo a, ApplicationInfo b) {
                return pm.getApplicationLabel(a).toString().compareToIgnoreCase(pm.getApplicationLabel(b).toString());
            }
        });
        for (ApplicationInfo ai : apps) {
            if (ai.packageName.equals(getPackageName())) continue;
            if (sel.contains(ai.packageName)) continue;
            if (pm.getLaunchIntentForPackage(ai.packageName) == null) continue;
            pkgs.add(ai.packageName);
            names.add(pm.getApplicationLabel(ai).toString());
        }
        if (pkgs.isEmpty()) {
            Toast.makeText(this, "Tidak ada aplikasi lain yang bisa dipilih.", Toast.LENGTH_SHORT).show();
            return;
        }
        final boolean[] checked = new boolean[pkgs.size()];
        AlertDialog.Builder b = new AlertDialog.Builder(this);
        b.setTitle("Pilih aplikasi untuk ditambahkan");
        b.setMultiChoiceItems(names.toArray(new CharSequence[0]), checked,
            new android.content.DialogInterface.OnMultiChoiceClickListener() {
                public void onClick(android.content.DialogInterface d, int which, boolean isChecked) {
                    checked[which] = isChecked;
                }
            });
        b.setPositiveButton("Tambahkan", new android.content.DialogInterface.OnClickListener() {
            public void onClick(android.content.DialogInterface d, int w) {
                Set<String> s = selectedGames();
                int n = 0;
                for (int i = 0; i < pkgs.size(); i++) if (checked[i]) { s.add(pkgs.get(i)); n++; }
                prefs.edit().putStringSet("games", s).apply();
                buildGames();
                Toast.makeText(MainActivity.this, n + " aplikasi ditambahkan.", Toast.LENGTH_SHORT).show();
            }
        });
        b.setNegativeButton("Batal", null);
        b.show();
    }

    private void launchGame(final String pkg) {
        if (!prefs.getBoolean("auto_apply", true)) {
            openGame(pkg);
            return;
        }
        Toast.makeText(this, "Menerapkan mode performa yang aktif...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                for (Features.F f : Features.all()) {
                    if (f.kind != Features.MODE_CMD) continue;
                    if (!prefs.getBoolean("feat_" + f.id, false)) continue;
                    try { applyFeature(f, true); } catch (Exception e) { /* abaikan */ }
                }
                runOnUiThread(new Runnable() {
                    public void run() { openGame(pkg); }
                });
            }
        }).start();
    }

    private void openGame(String pkg) {
        Intent i = getPackageManager().getLaunchIntentForPackage(pkg);
        if (i == null) {
            Toast.makeText(this, "Aplikasi ini tidak bisa dibuka dari sini.", Toast.LENGTH_SHORT).show();
            return;
        }
        i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i);
    }

    private View switchRow(String title, String desc, boolean initial, final String prefKey) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(cardBg());
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.setMargins(dp(10), dp(6), dp(10), dp(6));
        card.setLayoutParams(lp);
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView name = new TextView(this);
        name.setText(title);
        name.setTextSize(15);
        name.setTextColor(Color.WHITE);
        name.setTypeface(null, android.graphics.Typeface.BOLD);
        name.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
        top.addView(name);
        Switch sw = new Switch(this);
        sw.setChecked(initial);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean c) {
                prefs.edit().putBoolean(prefKey, c).apply();
            }
        });
        top.addView(sw);
        card.addView(top);
        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextSize(13);
        d.setTextColor(Color.rgb(184, 188, 203));
        d.setPadding(0, dp(4), 0, 0);
        card.addView(d);
        return card;
    }

    private void buildSettings() {
        settingsBox.removeAllViews();
        settingsBox.addView(sectionTitle("PENGATURAN"));

        settingsBox.addView(infoText("Status root menentukan fitur mana yang bisa bekerja. XRaka Booster tidak pernah mengaku berhasil: saklar hanya menyala kalau perintah sistem benar-benar menerima perintahnya."));
        Button cek = bigButton("Cek Ulang Status Root");
        cek.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                rootCache = null;
                checkRoot(true);
                Toast.makeText(MainActivity.this, "Memeriksa root...", Toast.LENGTH_SHORT).show();
            }
        });
        settingsBox.addView(cek);

        Button shz = bigButton("Minta Izin Shizuku");
        shz.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!Priv.shizukuAlive()) {
                    Toast.makeText(MainActivity.this, "Shizuku belum aktif. Pasang aplikasi Shizuku lalu aktifkan lewat Wireless Debugging.", Toast.LENGTH_LONG).show();
                    Intent i = getPackageManager().getLaunchIntentForPackage("moe.shizuku.privileged.api");
                    if (i != null) startActivity(i);
                    return;
                }
                if (Priv.shizukuGranted()) {
                    Toast.makeText(MainActivity.this, "Izin Shizuku sudah diberikan.", Toast.LENGTH_SHORT).show();
                    checkRoot(true);
                    return;
                }
                try {
                    rikka.shizuku.Shizuku.requestPermission(1001);
                    Toast.makeText(MainActivity.this, "Permintaan izin dikirim ke Shizuku.", Toast.LENGTH_SHORT).show();
                } catch (Throwable t) {
                    Toast.makeText(MainActivity.this, "Gagal meminta izin Shizuku.", Toast.LENGTH_SHORT).show();
                }
            }
        });
        settingsBox.addView(shz);
        settingsBox.addView(infoText("Shizuku adalah aplikasi tambahan gratis (ada di Play Store) yang memberi izin tingkat ADB tanpa root dan tanpa mengubah sistem, jadi aplikasi seperti WhatsApp dan GoPay tetap aman. Cara mengaktifkan: buka Shizuku, pilih mulai lewat Wireless Debugging, sambungkan, lalu kembali ke sini dan tekan Minta Izin Shizuku. Shizuku berhenti tiap HP restart dan harus diaktifkan ulang. Fitur bertanda ROOT / SHIZUKU bisa bekerja lewat Shizuku; fitur bertanda ROOT saja tetap wajib root penuh."));

        settingsBox.addView(sectionTitle("Resolusi Render"));
        settingsBox.addView(infoText("Persentase untuk fitur Resolusi Render Turun di Beranda. Makin kecil persennya, makin ringan GPU bekerja dan makin tinggi FPS, tapi tampilan makin buram dan besar."));
        RadioGroup rg = new RadioGroup(this);
        rg.setPadding(dp(14), 0, dp(14), dp(6));
        int cur = prefs.getInt("res_percent", 80);
        int[] vals = {90, 80, 70};
        String[] labs = {"90% - Ringan (sedikit lebih ringan, gambar masih tajam)",
                         "80% - Seimbang (anjuran untuk kebanyakan HP)",
                         "70% - Paling Ringan (FPS tertinggi, gambar lebih buram)"};
        for (int i = 0; i < vals.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(labs[i]);
            rb.setTextColor(Color.WHITE);
            rb.setTextSize(14);
            rb.setId(vals[i]);
            rb.setPadding(0, dp(6), 0, dp(6));
            if (vals[i] == cur) rb.setChecked(true);
            rg.addView(rb);
        }
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup g, int id) {
                prefs.edit().putInt("res_percent", id).apply();
            }
        });
        settingsBox.addView(rg);

        settingsBox.addView(switchRow("Terapkan Mode Saat Buka Game",
            "Saat kamu menekan tombol Main di tab Game, semua mode performa yang saklarnya menyala diterapkan ulang dulu (berguna setelah HP restart), baru gamenya dibuka.",
            prefs.getBoolean("auto_apply", true), "auto_apply"));
        settingsBox.addView(switchRow("Jalankan Ulang Mode Setelah Restart HP",
            "Setelah HP menyala lagi, mode performa yang terakhir menyala dijalankan ulang otomatis lewat akses root. Matikan ini kalau kamu tidak mau ada perubahan setelah restart. Khusus root, karena Shizuku tidak menyala otomatis setelah restart.",
            prefs.getBoolean("boot_reapply", true), "boot_reapply"));

        Button offAll = bigButton("Matikan Semua Fitur");
        offAll.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { turnOffAll(); }
        });
        settingsBox.addView(offAll);

        Button reset = bigButton("Hapus Semua Data XRaka Booster");
        reset.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                prefs.edit().clear().apply();
                recreate();
            }
        });
        settingsBox.addView(reset);

        settingsBox.addView(sectionTitle("TENTANG"));
        settingsBox.addView(infoText("XRaka Booster versi 1.3. Aplikasi booster game khusus HP yang sudah di-root, sekarang juga mendukung Shizuku untuk sebagian fitur tanpa root. Di HP yang belum di-root dan tanpa Shizuku, fitur bertanda ROOT akan gagal dan saklarnya kembali mati dengan jujur, sedangkan Mode Jangan Ganggu dan daftar Game tetap bisa dipakai. Mengaktifkan root di HP adalah urusan pemilik HP dan sebaiknya dibantu orang yang paham, karena root yang salah bisa merusak sistem."));
    }

    private void turnOffAll() {
        Toast.makeText(this, "Mematikan semua mode...", Toast.LENGTH_SHORT).show();
        new Thread(new Runnable() {
            public void run() {
                for (Features.F f : Features.all()) {
                    if (f.kind == Features.ACTION_CMD) continue;
                    if (!prefs.getBoolean("feat_" + f.id, false)) continue;
                    try {
                        if (f.kind == Features.MODE_DND) {
                            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                            if (nm.isNotificationPolicyAccessGranted())
                                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
                        } else {
                            applyFeature(f, false);
                        }
                    } catch (Exception e) { /* abaikan */ }
                    prefs.edit().putBoolean("feat_" + f.id, false).apply();
                }
                runOnUiThread(new Runnable() {
                    public void run() {
                        buildHome();
                        Toast.makeText(MainActivity.this, "Semua mode sudah dimatikan.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).start();
    }
}
