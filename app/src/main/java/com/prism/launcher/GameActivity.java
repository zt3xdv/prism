package com.prism.launcher;

import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class GameActivity extends AppCompatActivity {

    private static final String TAG = "Prism";
    private static final String JAR_NAME = "minecraft.jar";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView tv = new TextView(this);
        tv.setText(R.string.loading);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(18);
        tv.setGravity(android.view.Gravity.CENTER);
        setContentView(tv);

        String username = getIntent().getStringExtra("username");
        if (username == null) username = "Steve";

        final String finalUsername = username;
        new Thread(() -> {
            try {
                File jarFile = extractJar();
                launchGame(jarFile, finalUsername);
            } catch (Exception e) {
                Log.e(TAG, "Failed to launch", e);
                runOnUiThread(() ->
                    Toast.makeText(this, "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    private File extractJar() throws Exception {
        File jarFile = new File(getFilesDir(), JAR_NAME);
        if (jarFile.exists()) return jarFile;

        try (InputStream is = getAssets().open(JAR_NAME);
             OutputStream os = new FileOutputStream(jarFile)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
        }

        Log.i(TAG, "Extracted jar to " + jarFile.getAbsolutePath());
        return jarFile;
    }

    private void launchGame(File jarFile, String username) {
        String[] resolution = PrismApp.get().getResolution().split("x");
        int width = Integer.parseInt(resolution[0]);
        int height = Integer.parseInt(resolution[1]);
        int ramMb = PrismApp.get().getRamMb();

        String jrePath = new File(getFilesDir(), "jre").getAbsolutePath();
        String ldLibPath = jrePath + "/lib:" + jrePath + "/lib/server";

        String[] command = {
            jrePath + "/bin/java",
            "-Xmx" + ramMb + "M",
            "-Xms" + Math.min(ramMb, 512) + "M",
            "-Djava.library.path=" + getFilesDir().getAbsolutePath() + "/natives",
            "-cp", jarFile.getAbsolutePath(),
            "net.minecraft.client.main.Main",
            "--username", username,
            "--version", "Prism",
            "--accessToken", "0",
            "--width", String.valueOf(width),
            "--height", String.valueOf(height)
        };

        try {
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("LD_LIBRARY_PATH", ldLibPath);
            pb.environment().put("HOME", getFilesDir().getAbsolutePath());
            pb.directory(getFilesDir());
            pb.redirectErrorStream(true);

            Log.i(TAG, "Launching: " + String.join(" ", command));
            Process process = pb.start();

            // Read output for debugging
            InputStream is = process.getInputStream();
            byte[] buf = new byte[4096];
            int len;
            while ((len = is.read(buf)) != -1) {
                Log.d(TAG, new String(buf, 0, len));
            }

            int exitCode = process.waitFor();
            Log.i(TAG, "Game exited with code: " + exitCode);

            runOnUiThread(this::finish);
        } catch (Exception e) {
            Log.e(TAG, "Launch failed", e);
            runOnUiThread(() ->
                Toast.makeText(this, "Launch error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
        }
    }
}
