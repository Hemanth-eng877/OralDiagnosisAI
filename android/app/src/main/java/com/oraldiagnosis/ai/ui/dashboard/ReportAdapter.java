package com.oraldiagnosis.ai.ui.dashboard;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.oraldiagnosis.ai.databinding.ItemReportBinding;
import com.oraldiagnosis.ai.models.ReportItem;

import java.util.ArrayList;
import java.util.List;

public class ReportAdapter extends RecyclerView.Adapter<ReportAdapter.ReportViewHolder> {
    private List<ReportItem> reportList = new ArrayList<>();

    public void setReports(List<ReportItem> reports) {
        this.reportList = reports != null ? reports : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemReportBinding binding = ItemReportBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new ReportViewHolder(binding);
    }

    public interface OnItemClickListener {
        void onItemClick(ReportItem item);
    }
    
    private OnItemClickListener listener;
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }

    static class ReportViewHolder extends RecyclerView.ViewHolder {
        private final ItemReportBinding binding;

        public ReportViewHolder(ItemReportBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(ReportItem item, OnItemClickListener listener) {
            binding.tvPatientName.setText(item.patient_name != null && !item.patient_name.isEmpty() ? item.patient_name : "Patient ID: " + item.patient_id);
            binding.tvDiagnosis.setText(item.diagnosis != null ? item.diagnosis : "Unknown");
            binding.tvConfidence.setText(String.format("%.2f%%", item.confidence));
            binding.tvDate.setText(item.created_at != null ? item.created_at : "");
            
            if ("OSCC".equalsIgnoreCase(item.diagnosis)) {
                binding.tvDiagnosis.setTextColor(0xFFEF4444);
            } else {
                binding.tvDiagnosis.setTextColor(0xFF10B981);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(item);
            });
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        holder.bind(reportList.get(position), listener);
    }
    
    @Override
    public int getItemCount() {
        return reportList.size();
    }
}
