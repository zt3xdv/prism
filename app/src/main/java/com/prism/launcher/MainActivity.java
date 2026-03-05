package com.prism.launcher;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;

public class MainActivity extends AppCompatActivity {

    private TextInputEditText usernameInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        usernameInput = findViewById(R.id.usernameInput);

        String savedUsername = PrismApp.get().getUsername();
        if (!savedUsername.equals("Steve")) {
            usernameInput.setText(savedUsername);
        }

        findViewById(R.id.btnPlay).setOnClickListener(v -> onPlay());
        findViewById(R.id.btnOptions).setOnClickListener(v -> showOptions());
    }

    private void onPlay() {
        String username = usernameInput.getText() != null
                ? usernameInput.getText().toString().trim() : "";

        if (username.isEmpty()) {
            Toast.makeText(this, R.string.error_no_username, Toast.LENGTH_SHORT).show();
            return;
        }

        PrismApp.get().setUsername(username);

        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("username", username);
        startActivity(intent);
    }

    private void showOptions() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_options, null);

        SeekBar seekRam = dialogView.findViewById(R.id.seekRam);
        TextView ramValue = dialogView.findViewById(R.id.ramValue);
        Spinner spinnerRes = dialogView.findViewById(R.id.spinnerResolution);

        String[] resolutions = {"854x480", "1280x720", "1920x1080"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, resolutions);
        spinnerRes.setAdapter(adapter);

        int currentRam = PrismApp.get().getRamMb();
        seekRam.setProgress(currentRam);
        ramValue.setText(currentRam + " MB");

        String currentRes = PrismApp.get().getResolution();
        for (int i = 0; i < resolutions.length; i++) {
            if (resolutions[i].equals(currentRes)) {
                spinnerRes.setSelection(i);
                break;
            }
        }

        seekRam.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar sb, int progress, boolean fromUser) {
                int rounded = Math.max(256, (progress / 256) * 256);
                ramValue.setText(rounded + " MB");
            }
            @Override
            public void onStartTrackingTouch(SeekBar sb) {}
            @Override
            public void onStopTrackingTouch(SeekBar sb) {}
        });

        new AlertDialog.Builder(this, R.style.AppTheme)
                .setTitle(R.string.btn_options)
                .setView(dialogView)
                .setPositiveButton(R.string.btn_save, (d, w) -> {
                    int ram = Math.max(256, (seekRam.getProgress() / 256) * 256);
                    PrismApp.get().setRamMb(ram);
                    PrismApp.get().setResolution(
                            spinnerRes.getSelectedItem().toString());
                })
                .setNegativeButton(R.string.btn_back, null)
                .show();
    }
}
