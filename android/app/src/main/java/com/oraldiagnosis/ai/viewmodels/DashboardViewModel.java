package com.oraldiagnosis.ai.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.oraldiagnosis.ai.firebase.FirebaseManager;
import com.oraldiagnosis.ai.models.DashboardResponse;
import com.oraldiagnosis.ai.models.ReportItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DashboardViewModel extends AndroidViewModel {
    private MutableLiveData<DashboardResponse> dashboardData = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public DashboardViewModel(@NonNull Application application) {
        super(application);
        FirebaseManager.init(application);
    }

    public LiveData<DashboardResponse> getDashboardData() {
        return dashboardData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadDashboard(String userId) {
        android.util.Log.d("DashboardViewModel", "Fetching dashboard for UID: " + userId);
        
        Task<QuerySnapshot> patientsTask = FirebaseManager.getFirestore().collection("patients")
                .whereEqualTo("user_id", userId)
                .get();

        Task<QuerySnapshot> diagnosisTask = FirebaseManager.getFirestore().collection("diagnosis_records")
                .whereEqualTo("user_id", userId)
                .get();

        Tasks.whenAllComplete(patientsTask, diagnosisTask)
                .addOnCompleteListener(task -> {
                    if (patientsTask.isSuccessful() && diagnosisTask.isSuccessful()) {
                        QuerySnapshot patientsSnap = patientsTask.getResult();
                        QuerySnapshot diagnosesSnap = diagnosisTask.getResult();

                        int patientCount = patientsSnap.size();
                        int reportCount = diagnosesSnap.size();
                        android.util.Log.d("DashboardViewModel", "Patients count: " + patientCount + ", Reports count: " + reportCount);

                        int osccCount = 0;
                        int normalCount = 0;
                        
                        List<DocumentSnapshot> docs = new ArrayList<>(diagnosesSnap.getDocuments());
                        docs.sort((d1, d2) -> {
                            java.util.Date date1 = d1.getDate("created_at");
                            java.util.Date date2 = d2.getDate("created_at");
                            if (date1 == null && date2 == null) return 0;
                            if (date1 == null) return 1;
                            if (date2 == null) return -1;
                            return date2.compareTo(date1);
                        });

                        List<DashboardResponse.ReportItem> recentReports = new ArrayList<>();
                        
                        for (int i = 0; i < docs.size(); i++) {
                            DocumentSnapshot doc = docs.get(i);
                            String diagnosis = doc.getString("diagnosis");
                            if ("OSCC".equals(diagnosis)) {
                                osccCount++;
                            } else if ("Normal".equals(diagnosis)) {
                                normalCount++;
                            }
                            
                            if (i < 5) {
                                DashboardResponse.ReportItem item = new DashboardResponse.ReportItem();
                                item.id = doc.getId();
                                item.patient_id = doc.getString("patient_id");
                                item.diagnosis = diagnosis;
                                Double confDouble = doc.getDouble("confidence");
                                item.confidence = confDouble != null ? confDouble.floatValue() : 0.0f;
                                
                                recentReports.add(item);
                            }
                        }

                        DashboardResponse response = new DashboardResponse();
                        response.status = "success";
                        response.patient_count = patientCount;
                        response.report_count = reportCount;
                        response.oscc_count = osccCount;
                        response.normal_count = normalCount;
                        response.recent_reports = recentReports;
                        dashboardData.setValue(response);
                    } else {
                        Exception pEx = patientsTask.getException();
                        Exception dEx = diagnosisTask.getException();
                        if (pEx != null) android.util.Log.e("DashboardViewModel", "Patients Query Error: " + pEx.getMessage());
                        if (dEx != null) android.util.Log.e("DashboardViewModel", "Diagnosis Query Error: " + dEx.getMessage());
                        errorMessage.setValue("Failed to load dashboard data.");
                    }
                });
    }
}
