package com.oraldiagnosis.ai.viewmodels;

import android.app.Application;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.oraldiagnosis.ai.models.DiagnosisResponse;
import com.oraldiagnosis.ai.tflite.TFLiteInferenceManager;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PredictionViewModel extends AndroidViewModel {
    private MutableLiveData<DiagnosisResponse> uploadResult = new MutableLiveData<>();
    private MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private FirebaseFirestore db;
    private ExecutorService executorService;

    public PredictionViewModel(@NonNull Application application) {
        super(application);
        db = FirebaseFirestore.getInstance();
        executorService = Executors.newSingleThreadExecutor();
    }

    public LiveData<DiagnosisResponse> getUploadResult() {
        return uploadResult;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }



    public void uploadDiagnosisToServer(String userId, String patientId, File imageFile) {
        executorService.execute(() -> {
            TFLiteInferenceManager inferenceManager = null;
            try {
                // 1. Load Bitmap
                Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
                if (bitmap == null) {
                    DiagnosisResponse err = new DiagnosisResponse("invalid_image", null, 0.0f, null);
                    err.message = "Failed to load image";
                    uploadResult.postValue(err);
                    return;
                }

                // 2. Initialize TFLite Inference Manager
                inferenceManager = new TFLiteInferenceManager(getApplication(), "oral_model.tflite");
                TFLiteInferenceManager.PredictionResult result = inferenceManager.predict(bitmap);

                if (result.confidence < 90.0f) {
                    DiagnosisResponse err = new DiagnosisResponse("invalid_image", null, 0.0f, null);
                    err.message = "Please upload a valid microscopic oral cytology image.";
                    uploadResult.postValue(err);
                    return;
                }

                // 3. Get patient name from Firestore
                db.collection("patients").document(patientId).get().addOnSuccessListener(documentSnapshot -> {
                    String patientName = documentSnapshot.getString("full_name");
                    if (patientName == null) patientName = "Unknown";
                    
                    // 4. Save result to Firestore
                    saveToFirestore(userId, patientId, patientName, imageFile.getName(), result.label, result.confidence);
                }).addOnFailureListener(e -> {
                    DiagnosisResponse err = new DiagnosisResponse("error", null, 0.0f, null);
                    err.message = "Failed to fetch patient details";
                    uploadResult.postValue(err);
                });

            } catch (Exception e) {
                e.printStackTrace();
                // If model loading or inference fails, display: "Unable to load AI model."
                errorMessage.postValue("Unable to load AI model.");
            } finally {
                if (inferenceManager != null) {
                    inferenceManager.close();
                }
            }
        });
    }

    private void saveToFirestore(String userId, String patientId, String patientName, String originalFilename, String diagnosis, float confidence) {
        String uniqueFilename = UUID.randomUUID().toString().replace("-", "") + "_" + originalFilename;
        String documentId = db.collection("diagnosis_records").document().getId();

        Map<String, Object> data = new HashMap<>();
        data.put("id", documentId);
        data.put("uid", userId);
        data.put("user_id", userId);
        data.put("patient_id", patientId);
        data.put("patient_name", patientName);
        data.put("image_filename", uniqueFilename);
        data.put("image_path", uniqueFilename);
        data.put("diagnosis", diagnosis);
        data.put("confidence", confidence);
        data.put("created_at", FieldValue.serverTimestamp());

        db.collection("diagnosis_records").document(documentId).set(data)
                .addOnSuccessListener(aVoid -> {
                    uploadResult.postValue(new DiagnosisResponse("success", diagnosis, confidence, uniqueFilename));
                })
                .addOnFailureListener(e -> {
                    DiagnosisResponse err = new DiagnosisResponse("error", null, 0.0f, null);
                    err.message = "Cloud upload failed";
                    uploadResult.postValue(err);
                });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        if (executorService != null) {
            executorService.shutdown();
        }
    }
}
