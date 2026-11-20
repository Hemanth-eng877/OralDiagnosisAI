package com.oraldiagnosis.ai.ui.diagnose;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.oraldiagnosis.ai.databinding.ActivityPredictionBinding;
import com.oraldiagnosis.ai.models.PatientItem;
import com.oraldiagnosis.ai.viewmodels.PatientViewModel;
import com.oraldiagnosis.ai.viewmodels.PredictionViewModel;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PredictionActivity extends AppCompatActivity {
    private ActivityPredictionBinding binding;
    private PredictionViewModel predictionViewModel;
    private PatientViewModel patientViewModel;
    private Bitmap selectedBitmap = null;
    private File tempImageFile = null;
    private List<PatientItem> patientList = new ArrayList<>();
    private PatientItem selectedPatient = null;
    private String userId;

    private final ActivityResultLauncher<Intent> galleryLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Intent data = result.getData();
                    try {
                        if (data.getData() != null) {
                            // Gallery image
                            Uri imageUri = data.getData();
                            selectedBitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageUri);
                        } else if (data.getExtras() != null && data.getExtras().get("data") != null) {
                            // Camera thumbnail
                            selectedBitmap = (Bitmap) data.getExtras().get("data");
                        }
                        
                        if (selectedBitmap != null) {
                            Glide.with(this).load(selectedBitmap).into(binding.ivPreview);
                            binding.llPlaceholder.setVisibility(View.GONE);
                            binding.btnPredict.setEnabled(true);
                            binding.tvResult.setVisibility(View.GONE);

                            tempImageFile = new File(getCacheDir(), "upload_oral_" + System.currentTimeMillis() + ".jpg");
                            FileOutputStream fos = new FileOutputStream(tempImageFile);
                            selectedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                            fos.flush();
                            fos.close();
                            android.util.Log.d("PredictionActivity", "Selected image path: " + tempImageFile.getAbsolutePath());
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Error loading image", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityPredictionBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SharedPreferences prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE);
        userId = prefs.getString("user_id", null);

        predictionViewModel = new ViewModelProvider(this).get(PredictionViewModel.class);
        patientViewModel = new ViewModelProvider(this).get(PatientViewModel.class);

        patientViewModel.getPatientsData().observe(this, response -> {
            if ("success".equals(response.status) && response.patients != null) {
                patientList = response.patients;
                ArrayAdapter<PatientItem> searchAdapter = new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_dropdown_item_1line,
                        patientList
                );
                binding.actvPatients.setAdapter(searchAdapter);
                
                binding.actvPatients.setOnItemClickListener((parent, view, position, id) -> {
                    selectedPatient = (PatientItem) parent.getItemAtPosition(position);
                });
            }
        });

        if (userId != null) {
            patientViewModel.loadPatients(userId);
        }

        binding.btnSelectImage.setOnClickListener(v -> {
            Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            
            Intent chooser = Intent.createChooser(galleryIntent, "Select or Capture Image");
            chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, new Intent[]{cameraIntent});
            
            galleryLauncher.launch(chooser);
        });

        binding.btnPredict.setOnClickListener(v -> {
            if (selectedPatient == null) {
                Toast.makeText(this, "Please select a patient first.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (selectedBitmap != null) {
                if (!isValidMicroscopicImage(selectedBitmap)) {
                    Toast.makeText(this, "Please upload a valid microscopic image", Toast.LENGTH_LONG).show();
                    return;
                }
            } else {
                Toast.makeText(this, "Please select an image.", Toast.LENGTH_SHORT).show();
                return;
            }
            if (tempImageFile != null && tempImageFile.exists() && userId != null) {
                binding.btnPredict.setEnabled(false);
                binding.btnPredict.setText("");
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.scanningProgress.setVisibility(View.VISIBLE);
                binding.tvResult.setVisibility(View.GONE);
                
                predictionViewModel.uploadDiagnosisToServer(userId, selectedPatient.id, tempImageFile);
            }
        });

        predictionViewModel.getUploadResult().observe(this, response -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.scanningProgress.setVisibility(View.GONE);
            binding.btnPredict.setEnabled(true);
            binding.btnPredict.setText("Run Diagnosis");
            
            if ("success".equals(response.status)) {
                String patientName = selectedPatient != null ? selectedPatient.full_name : "Unknown";
                String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(new Date());
                
                String resultText = String.format(Locale.getDefault(),
                        "Patient: %s\nDiagnosis: %s\nConfidence: %.2f%%\nTime: %s",
                        patientName, response.diagnosis, response.confidence, timestamp);
                        
                binding.tvResult.setText(resultText);
                binding.tvResult.setVisibility(View.VISIBLE);
                
                Toast.makeText(this, "Diagnosis saved successfully!", Toast.LENGTH_LONG).show();
            } else if ("invalid_image".equals(response.status)) {
                Toast.makeText(this, response.message != null ? response.message : "Please upload a valid microscopic image", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, response.message != null ? response.message : "Cloud upload failed", Toast.LENGTH_SHORT).show();
            }
        });

        predictionViewModel.getErrorMessage().observe(this, err -> {
            binding.progressBar.setVisibility(View.GONE);
            binding.scanningProgress.setVisibility(View.GONE);
            binding.btnPredict.setEnabled(true);
            binding.btnPredict.setText("Run Diagnosis");
            Toast.makeText(this, err, Toast.LENGTH_SHORT).show();
        });
    }

    private boolean isValidMicroscopicImage(Bitmap originalBitmap) {
        try {
            if (originalBitmap.getWidth() < 50 || originalBitmap.getHeight() < 50) {
                return false;
            }
            // Resize to 224x224 to match backend logic and make processing fast
            Bitmap bitmap = Bitmap.createScaledBitmap(originalBitmap, 224, 224, true);
            
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int totalPixels = width * height;
            int[] pixels = new int[totalPixels];
            bitmap.getPixels(pixels, 0, width, 0, 0, width, height);
            
            long sumR = 0, sumG = 0, sumB = 0;
            int pinkPixels = 0;
            
            for (int pixel : pixels) {
                int r = (pixel >> 16) & 0xff;
                int g = (pixel >> 8) & 0xff;
                int b = pixel & 0xff;
                
                sumR += r;
                sumG += g;
                sumB += b;
                
                if (r > g && r > 50) {
                    pinkPixels++;
                }
            }
            
            double meanR = (double) sumR / totalPixels;
            double meanG = (double) sumG / totalPixels;
            double meanB = (double) sumB / totalPixels;
            
            // Check green dominance
            if (meanG > meanR && meanG > meanB) {
                return false;
            }
            
            // Check pink saturation
            if (((double) pinkPixels / totalPixels) < 0.05) {
                return false;
            }
            
            // Note: We skip the np.std(img_array) < 5 blank image check here 
            // as the pink saturation check naturally drops completely blank/white/black images.
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
