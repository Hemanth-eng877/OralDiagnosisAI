package com.oraldiagnosis.ai.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.oraldiagnosis.ai.firebase.FirebaseManager;
import com.oraldiagnosis.ai.models.ReportItem;
import com.oraldiagnosis.ai.models.ReportResponse;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ReportViewModel extends AndroidViewModel {
    private MutableLiveData<ReportResponse> reportsData = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public ReportViewModel(@NonNull Application application) {
        super(application);
        FirebaseManager.init(application);
    }

    public LiveData<ReportResponse> getReportsData() {
        return reportsData;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadReports(String userId) {
        FirebaseManager.getFirestore().collection("diagnosis_records")
                .whereEqualTo("user_id", userId)
                .addSnapshotListener((querySnap, error) -> {
                    if (error != null) {
                        errorMessage.setValue("Failed to load diagnosis reports: " + error.getMessage());
                        return;
                    }
                    if (querySnap != null) {
                        List<DocumentSnapshot> docs = new ArrayList<>(querySnap.getDocuments());
                        docs.sort((d1, d2) -> {
                            Date date1 = d1.getDate("created_at");
                            Date date2 = d2.getDate("created_at");
                            if (date1 == null && date2 == null) return 0;
                            if (date1 == null) return 1;
                            if (date2 == null) return -1;
                            return date2.compareTo(date1);
                        });
                        
                        List<ReportItem> firestoreReports = new ArrayList<>();
                        for (DocumentSnapshot doc : docs) {
                            String patientId = doc.getString("patient_id");
                            String patientName = doc.getString("patient_name");
                            String diagnosis = doc.getString("diagnosis");
                            Double confDouble = doc.getDouble("confidence");
                            float confidence = confDouble != null ? confDouble.floatValue() : 0.0f;
                            
                            String date = "";
                            if (doc.getTimestamp("created_at") != null) {
                                Date d = doc.getTimestamp("created_at").toDate();
                                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                                date = sdf.format(d);
                            }

                            firestoreReports.add(new ReportItem(doc.getId(), patientId, patientName, null, "", doc.getString("image_filename"), diagnosis, confidence, date));
                        }
                        reportsData.setValue(new ReportResponse("success", firestoreReports));
                    }
                });
    }
}
