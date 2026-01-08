package com.example.hinurse20.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.hinurse20.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MedicalRecordAdapter extends RecyclerView.Adapter<MedicalRecordAdapter.RecordViewHolder> {
    private final List<MedicalRecordItem> records = new ArrayList<>();
    private OnRecordClickListener listener;

    public interface OnRecordClickListener {
        void onRecordClick(MedicalRecordItem record);
    }

    public void setOnRecordClickListener(OnRecordClickListener listener) {
        this.listener = listener;
    }

    public static class MedicalRecordItem {
        private String recordId;
        private String nurseName;
        private String doctorName;
        private String doctorId;
        private Date recordDate;
        private String type;
        private String diagnosis;
        private String treatment;
        private String status;
        private String priority;
        private String notes;
        private String patientId;
        private String patientName;

        public MedicalRecordItem(String recordId, String nurseName, String doctorName, String doctorId, Date recordDate,
                                String type, String diagnosis, String treatment, String status,
                                String priority, String notes, String patientId, String patientName) {
            this.recordId = recordId;
            this.nurseName = nurseName;
            this.doctorName = doctorName;
            this.doctorId = doctorId;
            this.recordDate = recordDate;
            this.type = type;
            this.diagnosis = diagnosis;
            this.treatment = treatment;
            this.status = status;
            this.priority = priority;
            this.notes = notes;
            this.patientId = patientId;
            this.patientName = patientName;
        }

        // Getters
        public String getRecordId() { return recordId; }
        public String getNurseName() { return nurseName; }
        public String getDoctorName() { return doctorName; }
        public String getDoctorId() { return doctorId; }
        public Date getRecordDate() { return recordDate; }
        public String getType() { return type; }
        public String getDiagnosis() { return diagnosis; }
        public String getTreatment() { return treatment; }
        public String getStatus() { return status; }
        public String getPriority() { return priority; }
        public String getNotes() { return notes; }
        public String getPatientId() { return patientId; }
        public String getPatientName() { return patientName; }
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medical_record_card, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        MedicalRecordItem record = records.get(position);
        
        // Set record number (1-indexed)
        holder.textRecordNumber.setText(String.valueOf(position + 1));
        
        // Set nurse/doctor name
        String nameToShow = record.getNurseName();
        if (nameToShow == null || nameToShow.isEmpty()) {
            nameToShow = record.getDoctorName();
        }
        if (nameToShow == null || nameToShow.isEmpty()) {
            nameToShow = "Unknown";
        }
        holder.textNurseName.setText(nameToShow);
        
        // Set date
        if (record.getRecordDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            holder.textRecordDate.setText(sdf.format(record.getRecordDate()));
        } else {
            holder.textRecordDate.setText("N/A");
        }
        
        // Set click listener
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecordClick(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public void setRecords(List<MedicalRecordItem> newRecords) {
        records.clear();
        records.addAll(newRecords);
        notifyDataSetChanged();
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView textRecordNumber;
        TextView textNurseName;
        TextView textRecordDate;

        RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            textRecordNumber = itemView.findViewById(R.id.textRecordNumber);
            textNurseName = itemView.findViewById(R.id.textNurseName);
            textRecordDate = itemView.findViewById(R.id.textRecordDate);
        }
    }
}

