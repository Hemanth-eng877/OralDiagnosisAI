package com.oraldiagnosis.ai.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.Query;
import com.oraldiagnosis.ai.firebase.FirebaseManager;
import com.oraldiagnosis.ai.models.GenericResponse;
import com.oraldiagnosis.ai.models.PatientItem;
import com.oraldiagnosis.ai.models.PatientResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PatientViewModel extends AndroidViewModel {
    private MutableLiveData<PatientResponse> patientsData = new MutableLiveData<>();
    private MutableLiveData<GenericResponse> actionResult = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public PatientViewModel(@NonNull Application application) {
        super(application);
        FirebaseManager.init(application);
    }

    public LiveData<PatientResponse> getPatientsData() {
        return patientsData;
    }

    public LiveData<GenericResponse> getActionResult() {
        return actionResult;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadPatients(String userId) {
        FirebaseManager.getFirestore().collection("patients")
                .whereEqualTo("user_id", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<PatientItem> firestorePatients = new ArrayList<>();
                    for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                        String name = doc.getString("full_name");
                        Integer age = doc.getLong("age") != null ? doc.getLong("age").intValue() : null;
                        String gender = doc.getString("gender");
                        String phone = doc.getString("phone");
                        String email = doc.getString("email");
                        String notes = doc.getString("notes");

                        firestorePatients.add(new PatientItem(doc.getId(), userId, name, age, gender, phone, email, notes));
                    }
                    
                    // Sort locally to avoid Firestore composite index requirement
                    firestorePatients.sort((p1, p2) -> {
                        DocumentSnapshot d1 = queryDocumentSnapshots.getDocuments().stream().filter(d -> d.getId().equals(p1.id)).findFirst().orElse(null);
                        DocumentSnapshot d2 = queryDocumentSnapshots.getDocuments().stream().filter(d -> d.getId().equals(p2.id)).findFirst().orElse(null);
                        if (d1 == null || d2 == null) return 0;
                        java.util.Date date1 = d1.getDate("created_at");
                        java.util.Date date2 = d2.getDate("created_at");
                        if (date1 == null && date2 == null) return 0;
                        if (date1 == null) return 1;
                        if (date2 == null) return -1;
                        return date2.compareTo(date1);
                    });
                    
                    patientsData.setValue(new PatientResponse("success", firestorePatients));
                })
                .addOnFailureListener(e -> {
                    errorMessage.setValue("Failed to fetch patients from database: " + e.getMessage());
                });
    }

    public void addPatient(PatientItem patient) {
        Map<String, Object> data = new HashMap<>();
        data.put("full_name", patient.full_name);
        data.put("age", patient.age);
        data.put("gender", patient.gender);
        data.put("phone", patient.phone);
        data.put("email", patient.email);
        data.put("notes", patient.notes);
        data.put("user_id", patient.user_id);
        
        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            data.put("uid", user.getUid());
        } else {
            data.put("uid", patient.user_id);
        }
        
        data.put("created_at", FieldValue.serverTimestamp());

        FirebaseManager.getFirestore().collection("patients")
                .add(data)
                .addOnSuccessListener(documentReference -> {
                    patient.id = documentReference.getId();
                    // Update document with its own ID
                    documentReference.update("id", patient.id);
                    actionResult.setValue(new GenericResponse("success", "Patient added successfully."));
                })
                .addOnFailureListener(e -> {
                    actionResult.setValue(new GenericResponse("error", "Failed to add patient: " + e.getMessage()));
                });
    }

    public void editPatient(String patientId, PatientItem patient) {
        Map<String, Object> data = new HashMap<>();
        data.put("full_name", patient.full_name);
        data.put("age", patient.age);
        data.put("gender", patient.gender);
        data.put("phone", patient.phone);
        data.put("email", patient.email);
        data.put("notes", patient.notes);

        FirebaseManager.getFirestore().collection("patients").document(patientId)
                .update(data)
                .addOnSuccessListener(aVoid -> {
                    actionResult.setValue(new GenericResponse("success", "Patient updated successfully."));
                })
                .addOnFailureListener(e -> {
                    actionResult.setValue(new GenericResponse("error", "Failed to edit patient: " + e.getMessage()));
                });
    }

    public void deletePatient(String patientId, String userId) {
        FirebaseManager.getFirestore().collection("patients").document(patientId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    actionResult.setValue(new GenericResponse("success", "Patient deleted successfully."));
                })
                .addOnFailureListener(e -> {
                    actionResult.setValue(new GenericResponse("error", "Failed to delete patient: " + e.getMessage()));
                });
    }
}
