package com.oraldiagnosis.ai.ui.patients;

import android.view.LayoutInflater;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.oraldiagnosis.ai.databinding.ItemPatientBinding;
import com.oraldiagnosis.ai.models.PatientItem;

import java.util.ArrayList;
import java.util.List;

public class PatientAdapter extends RecyclerView.Adapter<PatientAdapter.PatientViewHolder> {
    private List<PatientItem> patientList = new ArrayList<>();
    private final OnPatientActionListener listener;

    public interface OnPatientActionListener {
        void onEdit(PatientItem patient);
        void onDelete(PatientItem patient);
    }

    public PatientAdapter(OnPatientActionListener listener) {
        this.listener = listener;
    }

    public void setPatients(List<PatientItem> patients) {
        this.patientList = patients != null ? patients : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public PatientViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPatientBinding binding = ItemPatientBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false);
        return new PatientViewHolder(binding);
    }

    @Override
    public void onBindViewHolder(@NonNull PatientViewHolder holder, int position) {
        holder.bind(patientList.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return patientList.size();
    }

    static class PatientViewHolder extends RecyclerView.ViewHolder {
        private final ItemPatientBinding binding;

        public PatientViewHolder(ItemPatientBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        public void bind(PatientItem item, OnPatientActionListener listener) {
            binding.tvPatientName.setText(item.full_name != null ? item.full_name : "Unnamed Patient");
            String ageStr = item.age != null ? String.valueOf(item.age) : "N/A";
            String genderStr = item.gender != null && !item.gender.isEmpty() ? item.gender : "U";
            binding.tvAgeGender.setText(ageStr + " / " + genderStr);
            binding.tvPhone.setText(item.phone != null && !item.phone.isEmpty() ? item.phone : "No Phone");
            
            // Dummy logic for latest diagnosis since backend doesn't provide it yet
            binding.tvStatusBadge.setText("Not Tested");
            binding.tvStatusBadge.setTextColor(android.graphics.Color.parseColor("#94A3B8"));
            binding.tvStatusBadge.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#3394A3B8")));

            binding.btnEdit.setOnClickListener(v -> {
                if (listener != null) listener.onEdit(item);
            });
            binding.btnDelete.setOnClickListener(v -> {
                if (listener != null) listener.onDelete(item);
            });
        }
    }
}
