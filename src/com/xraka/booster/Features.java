package com.xraka.booster;

import java.util.ArrayList;
import java.util.List;

public class Features {
    public static final int MODE_CMD = 0;
    public static final int MODE_RESOLUTION = 1;
    public static final int MODE_DND = 2;
    public static final int ACTION_CMD = 3;

    public static class F {
        public String id, title, desc;
        public boolean root;
        public int kind;
        public String onCmd, offCmd;
    }

    private static F mk(String id, String title, String desc, boolean root, int kind, String on, String off) {
        F f = new F();
        f.id = id; f.title = title; f.desc = desc; f.root = root; f.kind = kind;
        f.onCmd = on; f.offCmd = off;
        return f;
    }

    public static List<F> all() {
        List<F> L = new ArrayList<F>();

        L.add(mk("gov_cpu", "Governor CPU Performa",
            "Mengunci pengatur kecepatan CPU ke mode performance di semua klaster. CPU jadi selalu siap di kecepatan tinggi sehingga game tidak patah-patah saat adegan ramai. Baterai lebih boros saat mode ini menyala.",
            true, MODE_CMD,
            "n=0; for f in /sys/devices/system/cpu/cpufreq/policy*/scaling_governor; do echo performance > $f 2>/dev/null && n=$((n+1)); done; echo POLICY:$n; [ $n -gt 0 ]",
            "n=0; for f in /sys/devices/system/cpu/cpufreq/policy*/scaling_governor; do if echo schedutil > $f 2>/dev/null || echo ondemand > $f 2>/dev/null; then n=$((n+1)); fi; done; echo POLICY:$n; [ $n -gt 0 ]"));

        L.add(mk("freq_cpu", "Kunci Frekuensi CPU Maksimum",
            "Memaksa frekuensi minimum CPU sama dengan frekuensi maksimumnya. CPU tidak pernah turun kecepatan di tengah permainan, menghilangkan jeda saat kecepatan CPU naik-turun. Ini mode paling boros baterai dan paling panas, pakai seperlunya.",
            true, MODE_CMD,
            "n=0; for d in /sys/devices/system/cpu/cpufreq/policy*; do mx=$(cat $d/scaling_max_freq 2>/dev/null); [ -n \"$mx\" ] && echo $mx > $d/scaling_min_freq 2>/dev/null && n=$((n+1)); done; echo POLICY:$n; [ $n -gt 0 ]",
            "n=0; for d in /sys/devices/system/cpu/cpufreq/policy*; do mn=$(cat $d/cpuinfo_min_freq 2>/dev/null); [ -n \"$mn\" ] && echo $mn > $d/scaling_min_freq 2>/dev/null && n=$((n+1)); done; echo POLICY:$n; [ $n -gt 0 ]"));

        L.add(mk("gpu_perf", "Governor GPU Performa",
            "Mengatur pengatur kecepatan GPU (Mali atau Adreno) ke mode performance lewat devfreq. GPU berhenti menghemat daya dan fokus menggambar frame, cocok untuk game berat yang sering turun FPS.",
            true, MODE_CMD,
            "n=0; for d in /sys/class/devfreq/*; do case \"$d\" in *gpu*|*mali*|*kgsl*|*GPUSS*|*graphics*) echo performance > $d/governor 2>/dev/null && n=$((n+1));; esac; done; echo 0 > /sys/class/kgsl/kgsl-3d0/min_pwrlevel 2>/dev/null && n=$((n+1)); echo GPU:$n; [ $n -gt 0 ]",
            "n=0; for d in /sys/class/devfreq/*; do case \"$d\" in *gpu*|*mali*|*kgsl*|*GPUSS*|*graphics*) echo simple_ondemand > $d/governor 2>/dev/null && n=$((n+1));; esac; done; echo GPU:$n; [ $n -gt 0 ]"));

        L.add(mk("io_sched", "Penjadwal I/O Kilat",
            "Mengganti penjadwal baca-tulis penyimpanan ke noop atau deadline. Antrean data jadi lebih sederhana dan cepat sehingga loading game dan perpindahan map lebih singkat.",
            true, MODE_CMD,
            "n=0; for b in /sys/block/sda /sys/block/mmcblk0 /sys/block/mmcblk1 /sys/block/nvme0n1; do [ -e $b/queue/scheduler ] || continue; cur=$(cat $b/queue/scheduler); if echo \"$cur\" | grep -q noop; then echo noop > $b/queue/scheduler 2>/dev/null && n=$((n+1)); elif echo \"$cur\" | grep -q deadline; then echo deadline > $b/queue/scheduler 2>/dev/null && n=$((n+1)); fi; done; echo BLOCK:$n; [ $n -gt 0 ]",
            "n=0; for b in /sys/block/sda /sys/block/mmcblk0 /sys/block/mmcblk1 /sys/block/nvme0n1; do [ -e $b/queue/scheduler ] || continue; cur=$(cat $b/queue/scheduler); for s in mq-deadline bfq cfq; do if echo \"$cur\" | grep -q $s; then echo $s > $b/queue/scheduler 2>/dev/null && n=$((n+1)); break; fi; done; done; echo BLOCK:$n; [ $n -gt 0 ]"));

        L.add(mk("swap_low", "Swappiness Rendah",
            "Menurunkan nilai swappiness kernel ke 10. Sistem jadi jarang memindahkan isi RAM ke swap/ZRAM yang lambat, sehingga data game tetap di RAM dan perpindahan adegan lebih mulus.",
            true, MODE_CMD,
            "echo 10 > /proc/sys/vm/swappiness && [ \"$(cat /proc/sys/vm/swappiness)\" = \"10\" ]",
            "echo 60 > /proc/sys/vm/swappiness && [ \"$(cat /proc/sys/vm/swappiness)\" = \"60\" ]"));

        L.add(mk("res_down", "Resolusi Render Turun",
            "Menurunkan resolusi tampilan sistem lewat perintah wm size. Jumlah piksel yang digambar GPU berkurang banyak, ini cara paling terasa menaikkan FPS di HP kentang. Ukuran persennya bisa diatur di tab Pengaturan. Semua tulisan dan gambar jadi sedikit lebih besar, itu normal.",
            true, MODE_RESOLUTION, null, null));

        L.add(mk("anim_off", "Animasi Sistem Mati",
            "Mematikan animasi jendela dan transisi sistem Android (skala animasi 0). HP terasa jauh lebih responsif karena tidak ada waktu terbuang untuk animasi buka-tutup aplikasi.",
            true, MODE_CMD,
            "settings put global window_animation_scale 0 && settings put global transition_animation_scale 0 && settings put global animator_duration_scale 0",
            "settings put global window_animation_scale 1 && settings put global transition_animation_scale 1 && settings put global animator_duration_scale 1"));

        L.add(mk("dnd", "Mode Jangan Ganggu",
            "Membisukan semua suara, getaran, panggilan, dan notifikasi selama bermain supaya tidak ada gangguan di tengah pertandingan. Fitur ini tidak butuh root, cukup beri izin akses Jangan Ganggu saat diminta.",
            false, MODE_DND, null, null));

        L.add(mk("doze_white", "Game Anti Ke-Kill",
            "Memasukkan game pilihanmu ke daftar putih Doze (deviceidle whitelist). Sistem tidak akan membekukan atau membunuh game itu di latar, jadi game tidak tertutup sendiri dan notifikasi game tetap jalan. Pilih game dulu di tab Game.",
            true, MODE_CMD, null, null));

        L.add(mk("net_tune", "Jaringan Game Stabil",
            "Menyetel parameter TCP kernel: fast open menyala, slow start setelah diam dimatikan, dan buffer jaringan diperbesar. Tujuannya koneksi game online lebih responsif dan ping lebih stabil. Hasilnya tergantung jaringan dan server game juga.",
            true, MODE_CMD,
            "n=0; echo 3 > /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null && n=$((n+1)); echo 0 > /proc/sys/net/ipv4/tcp_slow_start_after_idle 2>/dev/null && n=$((n+1)); echo 16777216 > /proc/sys/net/core/rmem_max 2>/dev/null && n=$((n+1)); echo 16777216 > /proc/sys/net/core/wmem_max 2>/dev/null && n=$((n+1)); echo NET:$n; [ $n -ge 2 ]",
            "n=0; echo 1 > /proc/sys/net/ipv4/tcp_fastopen 2>/dev/null && n=$((n+1)); echo 1 > /proc/sys/net/ipv4/tcp_slow_start_after_idle 2>/dev/null && n=$((n+1)); echo 212992 > /proc/sys/net/core/rmem_max 2>/dev/null && n=$((n+1)); echo 212992 > /proc/sys/net/core/wmem_max 2>/dev/null && n=$((n+1)); echo NET:$n; [ $n -ge 2 ]"));

        L.add(mk("thermal_free", "Mode Suhu Bebas (Thermal)",
            "Menghentikan layanan thermal bawaan HP sehingga CPU dan GPU tidak diturunkan kecepatannya saat HP mulai panas. FPS jadi stabil lebih lama, TAPI HP bisa jadi jauh lebih panas dan baterai cepat habis. Matikan mode ini kalau HP terasa terlalu panas. Pakai dengan tanggung jawab sendiri.",
            true, MODE_CMD,
            "stop thermal-engine 2>/dev/null; stop vendor.thermal-engine 2>/dev/null; stop vendor.thermal 2>/dev/null; stop thermald 2>/dev/null; setprop ctl.stop thermal-engine 2>/dev/null; echo THERMAL_STOP_SENT; true",
            "start thermal-engine 2>/dev/null; start vendor.thermal-engine 2>/dev/null; start vendor.thermal 2>/dev/null; start thermald 2>/dev/null; echo THERMAL_START_SENT; true"));

        L.add(mk("act_boost", "Boost Sekarang (Gabungan)",
            "Satu sentuhan menjalankan tiga pembersihan sekaligus: membuang cache RAM kernel, memangkas cache semua aplikasi, dan menyinkronkan penyimpanan. Jalankan tepat sebelum membuka game berat.",
            true, ACTION_CMD,
            "ok=0; sync; echo 3 > /proc/sys/vm/drop_caches 2>/dev/null && ok=1; pm trim-caches 999999999999 2>/dev/null; echo BOOST_OK:$ok; [ $ok = 1 ]",
            null));

        L.add(mk("act_ram", "Bersihkan RAM (Drop Cache)",
            "Memerintahkan kernel membuang seluruh page cache lewat drop_caches. RAM lega seketika sehingga game punya ruang lebih besar. Efeknya sesaat, paling pas dijalankan sebelum bermain.",
            true, ACTION_CMD,
            "sync; echo 3 > /proc/sys/vm/drop_caches",
            null));

        L.add(mk("act_stopbg", "Hentikan Aplikasi Latar",
            "Menghentikan paksa (force stop) semua aplikasi pengguna yang berjalan di latar, kecuali XRaka Booster dan game pilihanmu. RAM dan CPU jadi fokus ke game. Aplikasi yang dihentikan bisa dibuka lagi seperti biasa.",
            true, ACTION_CMD, null, null));

        L.add(mk("act_cache", "Bersihkan Cache Semua Aplikasi",
            "Memangkas cache semua aplikasi lewat perintah pm trim-caches. Ruang penyimpanan lega dan tumpukan sampah aplikasi hilang tanpa menghapus data penting seperti akun atau simpanan game.",
            true, ACTION_CMD,
            "pm trim-caches 999999999999",
            null));

        L.add(mk("act_prio", "Prioritaskan Game yang Berjalan",
            "Menaikkan prioritas proses (renice) game pilihanmu yang sedang berjalan sampai -15. Penjadwal CPU akan mendahulukan game itu dibanding proses lain. Buka gamenya dulu, lalu jalankan aksi ini.",
            true, ACTION_CMD, null, null));

        return L;
    }

    public static F byId(String id) {
        for (F f : all()) if (f.id.equals(id)) return f;
        return null;
    }

    public static final int LEVEL_NONE = 0;
    public static final int LEVEL_SHELL = 1; // cukup root ATAU Shizuku (izin tingkat ADB)
    public static final int LEVEL_ROOT = 2;  // wajib root penuh (file kernel/sistem)

    public static int levelOf(F f) {
        if (f.id.equals("res_down") || f.id.equals("anim_off") || f.id.equals("doze_white")
                || f.id.equals("act_stopbg") || f.id.equals("act_cache")) return LEVEL_SHELL;
        return f.root ? LEVEL_ROOT : LEVEL_NONE;
    }
}
