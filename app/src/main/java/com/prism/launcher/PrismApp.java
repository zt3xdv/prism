package com.prism.launcher;

import android.app.Application;
import android.content.SharedPreferences;

public class PrismApp extends Application {

    private static PrismApp instance;
    private SharedPreferences prefs;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        prefs = getSharedPreferences("prism_config", MODE_PRIVATE);
    }

    public static PrismApp get() {
        return instance;
    }

    public SharedPreferences getPrefs() {
        return prefs;
    }

    public String getUsername() {
        return prefs.getString("username", "Steve");
    }

    public void setUsername(String username) {
        prefs.edit().putString("username", username).apply();
    }

    public int getRamMb() {
        return prefs.getInt("ram_mb", 1024);
    }

    public void setRamMb(int mb) {
        prefs.edit().putInt("ram_mb", mb).apply();
    }

    public String getResolution() {
        return prefs.getString("resolution", "854x480");
    }

    public void setResolution(String res) {
        prefs.edit().putString("resolution", res).apply();
    }
}
