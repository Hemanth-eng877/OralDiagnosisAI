package com.oraldiagnosis.ai.ui.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.oraldiagnosis.ai.databinding.ActivitySettingsBinding;

public class SettingsActivity extends AppCompatActivity {
    private ActivitySettingsBinding binding;
    private static final String DEFAULT_BASE_URL = "Local On-Device Inference";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String currentUrl = prefs.getString("base_url", DEFAULT_BASE_URL);
        binding.tvCurrentUrl.setText(currentUrl);
        binding.etBaseUrl.setText(currentUrl);
        binding.etBaseUrl.setEnabled(false); // No longer needed

        binding.btnTestConnection.setOnClickListener(v -> {
            Toast.makeText(this, "AI Inference is now completely on-device. No server needed.", Toast.LENGTH_LONG).show();
        });

        binding.btnSaveSettings.setOnClickListener(v -> {
            Toast.makeText(this, "Settings saved. (On-Device Inference Active)", Toast.LENGTH_SHORT).show();
            finish();
        });

        binding.btnResetUrl.setOnClickListener(v -> {
            Toast.makeText(this, "Reset to Local On-Device Inference", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}
