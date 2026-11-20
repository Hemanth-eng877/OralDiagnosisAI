package com.oraldiagnosis.ai.ui.history;

import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.oraldiagnosis.ai.databinding.ActivityHistoryBinding;
import com.oraldiagnosis.ai.models.ReportItem;
import com.oraldiagnosis.ai.ui.dashboard.ReportAdapter;
import com.oraldiagnosis.ai.viewmodels.ReportViewModel;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {
    private ActivityHistoryBinding binding;
    private ReportViewModel viewModel;
    private ReportAdapter adapter;
    private List<ReportItem> fullList = new ArrayList<>();
    private String currentFilter = "All"; // "All", "OSCC", "Normal", "Recent"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        String userId = prefs.getString("user_id", null);

        adapter = new ReportAdapter();
        binding.rvReports.setLayoutManager(new LinearLayoutManager(this));
        binding.rvReports.setAdapter(adapter);

        adapter.setOnItemClickListener(item -> {
            showReportDetailsDialog(item);
        });

        viewModel = new ViewModelProvider(this).get(ReportViewModel.class);
        viewModel.getReportsData().observe(this, response -> {
            if ("success".equals(response.status) && response.reports != null) {
                fullList = response.reports;
                applyFilters();
            }
        });

        viewModel.getErrorMessage().observe(this, err -> {
            Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });

        binding.etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                applyFilters();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        binding.rgFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == binding.rbAll.getId()) {
                currentFilter = "All";
            } else if (checkedId == binding.rbOscc.getId()) {
                currentFilter = "OSCC";
            } else if (checkedId == binding.rbNormal.getId()) {
                currentFilter = "Normal";
            } else if (checkedId == binding.rbRecent.getId()) {
                currentFilter = "Recent";
            }
            applyFilters();
        });

        if (userId != null) {
            viewModel.loadReports(userId);
        }
    }

    private void applyFilters() {
        String query = binding.etSearch.getText().toString().toLowerCase().trim();
        List<ReportItem> filtered = new ArrayList<>();
        
        for (ReportItem item : fullList) {
            // Apply text search
            String name = item.patient_name != null ? item.patient_name.toLowerCase() : "";
            boolean matchesSearch = query.isEmpty() || name.contains(query) || (item.diagnosis != null && item.diagnosis.toLowerCase().contains(query));
            
            if (!matchesSearch) continue;
            
            // Apply category filter
            if (currentFilter.equals("All") || currentFilter.equals("Recent")) {
                filtered.add(item);
            } else if (currentFilter.equals("OSCC") && "OSCC".equalsIgnoreCase(item.diagnosis)) {
                filtered.add(item);
            } else if (currentFilter.equals("Normal") && "Normal".equalsIgnoreCase(item.diagnosis)) {
                filtered.add(item);
            }
        }
        
        // If Recent, just keep top 5
        if (currentFilter.equals("Recent") && filtered.size() > 5) {
            filtered = filtered.subList(0, 5);
        }
        
        adapter.setReports(filtered);
        
        if (filtered.isEmpty()) {
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.rvReports.setVisibility(View.GONE);
        } else {
            binding.tvEmptyState.setVisibility(View.GONE);
            binding.rvReports.setVisibility(View.VISIBLE);
        }
    }
    
    private void showReportDetailsDialog(ReportItem item) {
        String message = String.format("Patient: %s\nDiagnosis: %s\nConfidence: %.2f%%\nDate: %s", 
                item.patient_name != null ? item.patient_name : "Unknown",
                item.diagnosis,
                item.confidence,
                item.created_at != null ? item.created_at : "Unknown");
                
        new AlertDialog.Builder(this)
                .setTitle("Report Details")
                .setMessage(message)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
