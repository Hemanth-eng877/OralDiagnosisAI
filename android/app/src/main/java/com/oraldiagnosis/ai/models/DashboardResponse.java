package com.oraldiagnosis.ai.models;

import java.util.List;

public class DashboardResponse {
    public String status;
    public int patient_count;
    public int report_count;
    public int oscc_count;
    public int normal_count;
    public List<ReportItem> recent_reports;

    public static class ReportItem {
        public String id;
        public String patient_id;
        public String diagnosis;
        public float confidence;
    }
}
