package com.xraka.booster;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class Shell {
    public static class Result {
        public int code = -1;
        public String out = "";
        public boolean ok() { return code == 0; }
    }

    public static Result run(String... cmd) {
        Result r = new Result();
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            r.out = sb.toString().trim();
            r.code = p.waitFor();
        } catch (Exception e) {
            r.code = -1;
            r.out = "ERROR: " + e.getMessage();
        }
        return r;
    }

    public static Result root(String command) {
        return run("su", "-c", command);
    }

    public static Result sh(String command) {
        return run("sh", "-c", command);
    }

    public static boolean hasRoot() {
        Result r = root("id");
        return r.code == 0 && r.out != null && r.out.contains("uid=0");
    }
}
