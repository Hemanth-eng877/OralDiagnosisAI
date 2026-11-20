package com.oraldiagnosis.ai.ui.patients;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.oraldiagnosis.ai.databinding.ActivityPatientFormBinding;
import com.oraldiagnosis.ai.models.PatientItem;
import com.oraldiagnosis.ai.viewmodels.PatientViewModel;

public class PatientFormActivity extends AppCompatActivity {
    private ActivityPatientFormBinding binding;
    private PatientViewModel viewModel;
    private PatientItem existingPatient = null;
    private String userId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPatientFormBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", null);

        if (getIntent().hasExtra("patient")) {
            existingPatient = (PatientItem) getIntent().getSerializableExtra("patient");
        }

        viewModel = new ViewModelProvider(this).get(PatientViewModel.class);

        if (existingPatient != null) {
            binding.tvFormTitle.setText("Edit Patient Record");
            binding.etFullName.setText(existingPatient.full_name);
            if (existingPatient.age != null) {
                binding.etAge.setText(String.valueOf(existingPatient.age));
            }
            binding.actvGender.setText(existingPatient.gender, false);
            binding.etPhone.setText(existingPatient.phone);
            binding.etEmail.setText(existingPatient.email);
            binding.etNotes.setText(existingPatient.notes);
        }

        String[] genders = new String[]{"Male", "Female", "Other"};
        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                this,
                android.R.layout.simple_dropdown_item_1line,
                genders
        );
        binding.actvGender.setAdapter(adapter);

        binding.btnSave.setOnClickListener(v -> {
            String name = binding.etFullName.getText().toString().trim();
            if (name.isEmpty()) {
                Toast.makeText(this, "Patient name is required.", Toast.LENGTH_SHORT).show();
                return;
            }
            String ageStr = binding.etAge.getText().toString().trim();
            Integer age = ageStr.isEmpty() ? null : Integer.parseInt(ageStr);

            PatientItem item = new PatientItem(
                    existingPatient != null ? existingPatient.id : null,
                    userId,
                    name,
                    age,
                    binding.actvGender.getText().toString().trim(),
                    binding.etPhone.getText().toString().trim(),
                    binding.etEmail.getText().toString().trim(),
                    binding.etNotes.getText().toString().trim()
            );

            binding.btnSave.setEnabled(false);
            if (existingPatient != null && existingPatient.id != null) {
                viewModel.editPatient(existingPatient.id, item);
            } else {
                viewModel.addPatient(item);
            }
        });

        binding.btnCancel.setOnClickListener(v -> finish());

        viewModel.getActionResult().observe(this, res -> {
            binding.btnSave.setEnabled(true);
            if ("success".equals(res.status)) {
                Toast.makeText(this, res.message != null ? res.message : "Patient saved successfully", Toast.LENGTH_SHORT).show();
                finish();
            } else {
                Toast.makeText(this, res.message != null ? res.message : "Error saving patient", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
