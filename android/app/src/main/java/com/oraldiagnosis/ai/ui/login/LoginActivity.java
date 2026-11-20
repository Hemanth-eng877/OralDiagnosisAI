package com.oraldiagnosis.ai.ui.login;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.oraldiagnosis.ai.databinding.ActivityLoginBinding;
import com.oraldiagnosis.ai.ui.dashboard.DashboardActivity;
import com.oraldiagnosis.ai.ui.register.RegisterActivity;
import com.oraldiagnosis.ai.ui.settings.SettingsActivity;
import com.oraldiagnosis.ai.viewmodels.AuthViewModel;

public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnLogin.setOnClickListener(v -> {
            String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
            String password = binding.etPassword.getText() != null ? binding.etPassword.getText().toString() : "";
            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please enter both email and password.", Toast.LENGTH_SHORT).show();
                return;
            }
            binding.btnLogin.setEnabled(false);
            binding.btnLogin.setText("");
            binding.pbLoading.setVisibility(android.view.View.VISIBLE);
            viewModel.login(email, password);
        });

        binding.tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
        });

        binding.btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
        });

        viewModel.getLoginResult().observe(this, authResponse -> {
            binding.btnLogin.setEnabled(true);
            binding.btnLogin.setText(com.oraldiagnosis.ai.R.string.login);
            binding.pbLoading.setVisibility(android.view.View.GONE);
            if ("success".equals(authResponse.status)) {
                SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
                prefs.edit()
                        .putString("user_id", authResponse.user_id)
                        .putString("user_name", authResponse.user_name)
                        .apply();
                startActivity(new Intent(LoginActivity.this, DashboardActivity.class));
                finish();
            } else {
                Toast.makeText(this, authResponse.message != null ? authResponse.message : "Login error", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
