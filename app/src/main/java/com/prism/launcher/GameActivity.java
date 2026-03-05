package com.prism.launcher;

import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

public class GameActivity extends AppCompatActivity implements SurfaceHolder.Callback {

    private static final String TAG = "Prism";
    private SurfaceView surfaceView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        surfaceView = new SurfaceView(this);
        surfaceView.getHolder().addCallback(this);
        setContentView(surfaceView);

        new Thread(() -> {
            try {
                extractAssets();
            } catch (Exception e) {
                Log.e(TAG, "Failed to extract assets", e);
                runOnUiThread(() ->
                    Toast.makeText(this, "Error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        String username = getIntent().getStringExtra("username");
        if (username == null) username = "Steve";
        final String finalUsername = username;

        new Thread(() -> {
            try {
                launchGame(finalUsername);
            } catch (Exception e) {
                Log.e(TAG, "Launch failed", e);
                runOnUiThread(() ->
                    Toast.makeText(this, "Launch error: " + e.getMessage(),
                            Toast.LENGTH_LONG).show());
            }
        }).start();
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {}

    private void extractAssets() throws Exception {
        File baseDir = getFilesDir();
        File marker = new File(baseDir, ".extracted");
        if (marker.exists()) {
            Log.i(TAG, "Assets already extracted");
            return;
        }

        Log.i(TAG, "Extracting game assets...");

        // Extract minecraft.jar from APK assets
        extractAsset("minecraft.jar", new File(baseDir, "minecraft.jar"));

        // Extract JRE components if present
        String[] jreFiles = null;
        try {
            jreFiles = getAssets().list("jre");
        } catch (Exception e) {
            Log.w(TAG, "No JRE in assets");
        }

        if (jreFiles != null) {
            for (String file : jreFiles) {
                extractAsset("jre/" + file, new File(baseDir, "jre/" + file));
            }
        }

        marker.createNewFile();
        Log.i(TAG, "Assets extracted successfully");
    }

    private void extractAsset(String assetName, File output) throws Exception {
        output.getParentFile().mkdirs();
        if (output.exists()) return;

        try (InputStream is = getAssets().open(assetName);
             OutputStream os = new FileOutputStream(output)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = is.read(buf)) != -1) {
                os.write(buf, 0, len);
            }
        }
        Log.d(TAG, "Extracted: " + assetName);
    }

    private void launchGame(String username) {
        File baseDir = getFilesDir();
        File jreDir = new File(baseDir, "jre");
        File jarFile = new File(baseDir, "minecraft.jar");
        File nativesDir = new File(getApplicationInfo().nativeLibraryDir);
        File libDir = new File(baseDir, "libraries");

        String[] resolution = PrismApp.get().getResolution().split("x");
        int width = Integer.parseInt(resolution[0]);
        int height = Integer.parseInt(resolution[1]);
        int ramMb = PrismApp.get().getRamMb();

        // Build classpath from libraries dir
        StringBuilder classpath = new StringBuilder();
        classpath.append(jarFile.getAbsolutePath());
        if (libDir.exists()) {
            addJarsToClasspath(libDir, classpath);
        }

        String javaExec = jreDir.getAbsolutePath() + "/bin/java";
        String ldLibPath = nativesDir.getAbsolutePath()
                + ":" + jreDir.getAbsolutePath() + "/lib/aarch64"
                + ":" + jreDir.getAbsolutePath() + "/lib/aarch64/server";

        List<String> args = new ArrayList<>();
        args.add(javaExec);
        args.add("-Xmx" + ramMb + "M");
        args.add("-Xms" + Math.min(ramMb, 512) + "M");
        args.add("-Djava.library.path=" + nativesDir.getAbsolutePath());
        args.add("-Dorg.lwjgl.opengl.libname=libgl4es_114.so");
        args.add("-Dos.name=Linux");
        args.add("-cp");
        args.add(classpath.toString());
        args.add("net.minecraft.client.main.Main");
        args.add("--username");
        args.add(username);
        args.add("--version");
        args.add("Prism");
        args.add("--accessToken");
        args.add("0");
        args.add("--width");
        args.add(String.valueOf(width));
        args.add("--height");
        args.add(String.valueOf(height));

        try {
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.environment().put("LD_LIBRARY_PATH", ldLibPath);
            pb.environment().put("HOME", baseDir.getAbsolutePath());
            pb.environment().put("LIBGL_ES", "2");
            pb.directory(baseDir);
            pb.redirectErrorStream(true);

            Log.i(TAG, "Launching Minecraft...");
            Log.d(TAG, "Command: " + String.join(" ", args));

            Process process = pb.start();

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
                Toast.makeText(this, "Error: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
        }
    }

    private void addJarsToClasspath(File dir, StringBuilder classpath) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isDirectory()) {
                addJarsToClasspath(f, classpath);
            } else if (f.getName().endsWith(".jar")) {
                classpath.append(":").append(f.getAbsolutePath());
            }
        }
    }
}
