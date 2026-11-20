package com.oraldiagnosis.ai.ui.login;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.oraldiagnosis.ai.databinding.ActivityForgotPasswordBinding;
import com.oraldiagnosis.ai.viewmodels.AuthViewModel;

public class ForgotPasswordActivity extends AppCompatActivity {
    private ActivityForgotPasswordBinding binding;
    private AuthViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityForgotPasswordBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        viewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        binding.btnReset.setOnClickListener(v -> {
            String email = binding.etEmail.getText() != null ? binding.etEmail.getText().toString().trim() : "";
            binding.btnReset.setEnabled(false);
            binding.pbLoading.setVisibility(View.VISIBLE);
            viewModel.resetPassword(email);
        });

        binding.tvBackToLogin.setOnClickListener(v -> finish());

        viewModel.getResetPasswordResult().observe(this, response -> {
            binding.btnReset.setEnabled(true);
            binding.pbLoading.setVisibility(View.GONE);
            if ("success".equals(response.status)) {
                Toast.makeText(this, response.message, Toast.LENGTH_LONG).show();
                finish();
            } else {
                Toast.makeText(this, response.message, Toast.LENGTH_LONG).show();
            }
        });
    }
}
