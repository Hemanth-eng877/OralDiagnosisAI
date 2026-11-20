package com.oraldiagnosis.ai.viewmodels;

import android.app.Application;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.firebase.FirebaseNetworkException;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.oraldiagnosis.ai.firebase.FirebaseManager;
import com.oraldiagnosis.ai.models.AuthResponse;

import java.util.HashMap;
import java.util.Map;

public class AuthViewModel extends AndroidViewModel {
    private MutableLiveData<AuthResponse> loginResult = new MutableLiveData<>();
    private MutableLiveData<AuthResponse> signupResult = new MutableLiveData<>();
    private MutableLiveData<AuthResponse> resetPasswordResult = new MutableLiveData<>();

    public AuthViewModel(@NonNull Application application) {
        super(application);
        FirebaseManager.init(application);
    }

    public LiveData<AuthResponse> getLoginResult() {
        return loginResult;
    }

    public LiveData<AuthResponse> getSignupResult() {
        return signupResult;
    }

    public LiveData<AuthResponse> getResetPasswordResult() {
        return resetPasswordResult;
    }

    public void resetPassword(String email) {
        if (email == null || email.trim().isEmpty()) {
            resetPasswordResult.setValue(new AuthResponse("error", "Please enter your email address", null, null));
            return;
        }
        FirebaseManager.getAuth().sendPasswordResetEmail(email.trim())
                .addOnSuccessListener(aVoid -> {
                    resetPasswordResult.setValue(new AuthResponse("success", "Password reset email sent. Please check your inbox.", null, null));
                })
                .addOnFailureListener(e -> {
                    String errorMsg = "Unable to send reset email.";
                    if (e instanceof com.google.firebase.auth.FirebaseAuthInvalidUserException) {
                        errorMsg = "No account found with this email";
                    } else if (e instanceof FirebaseNetworkException) {
                        errorMsg = "Unable to send reset email. Check your connection.";
                    } else {
                        errorMsg = e.getMessage();
                    }
                    resetPasswordResult.setValue(new AuthResponse("error", errorMsg, null, null));
                });
    }

    public void login(String email, String password) {
        authenticateViaFirebase(email, password, false);
    }

    public void signup(String name, String email, String password) {
        authenticateViaFirebase(email, password, true, name);
    }

    private void authenticateViaFirebase(String email, String password, boolean isSignup, String... optionalName) {
        FirebaseAuth auth = FirebaseManager.getAuth();
        if (isSignup) {
            String name = optionalName.length > 0 ? optionalName[0] : "Doctor";
            auth.createUserWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser() != null ? authResult.getUser().getUid() : "u_" + System.currentTimeMillis();
                        Map<String, Object> userData = new HashMap<>();
                        userData.put("uid", uid);
                        userData.put("full_name", name);
                        userData.put("name", name);
                        userData.put("email", email);
                        userData.put("role", "Doctor");
                        userData.put("created_at", com.google.firebase.firestore.FieldValue.serverTimestamp());

                        FirebaseManager.getFirestore().collection("users").document(uid).set(userData)
                                .addOnSuccessListener(aVoid -> {
                                    logAuthEvent(FirebaseAnalytics.Event.SIGN_UP, email, true);
                                    signupResult.setValue(new AuthResponse("success", "Account created successfully.", uid, name));
                                })
                                .addOnFailureListener(e -> {
                                    String errorMsg = "Failed to save user profile.";
                                    android.util.Log.e("AuthViewModel", "Firestore write failure: " + e.getMessage(), e);
                                    if (e instanceof FirebaseFirestoreException) {
                                        FirebaseFirestoreException fsEx = (FirebaseFirestoreException) e;
                                        android.util.Log.e("AuthViewModel", "Firestore Error Code: " + fsEx.getCode());
                                        if (fsEx.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                            errorMsg = "Permission denied to create user profile.";
                                        } else {
                                            errorMsg = "Firestore error: [" + fsEx.getCode() + "] " + fsEx.getMessage();
                                        }
                                    }
                                    logAuthEvent(FirebaseAnalytics.Event.SIGN_UP, email, false);
                                    signupResult.setValue(new AuthResponse("error", errorMsg, null, null));
                                });
                    })
                    .addOnFailureListener(e -> {
                        String errorMsg = "Signup failed";
                        if (e instanceof com.google.firebase.auth.FirebaseAuthException) {
                            com.google.firebase.auth.FirebaseAuthException authEx = (com.google.firebase.auth.FirebaseAuthException) e;
                            android.util.Log.e("AuthViewModel", "FirebaseAuthException Error Code: " + authEx.getErrorCode());
                            android.util.Log.e("AuthViewModel", "FirebaseAuthException Message: " + authEx.getMessage());
                            errorMsg = "Signup error: [" + authEx.getErrorCode() + "] " + authEx.getMessage();
                        } else {
                            android.util.Log.e("AuthViewModel", "Signup Failure: " + e.getMessage(), e);
                            errorMsg = "Signup error: " + e.getMessage();
                        }
                        
                        if (e instanceof FirebaseAuthUserCollisionException) {
                            errorMsg = "Email already exists.";
                        } else if (e instanceof FirebaseAuthWeakPasswordException) {
                            errorMsg = "Weak password. Please use at least 6 characters.";
                        } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                            errorMsg = "Invalid email format.";
                        } else if (e instanceof FirebaseNetworkException) {
                            errorMsg = "Network failure. Please check your connection.";
                        }
                        
                        logAuthEvent(FirebaseAnalytics.Event.SIGN_UP, email, false);
                        signupResult.setValue(new AuthResponse("error", errorMsg, null, null));
                    });
        } else {
            auth.signInWithEmailAndPassword(email, password)
                    .addOnSuccessListener(authResult -> {
                        String uid = authResult.getUser() != null ? authResult.getUser().getUid() : "u_" + System.currentTimeMillis();
                        FirebaseManager.getFirestore().collection("users").document(uid).get()
                                .addOnSuccessListener(doc -> {
                                    String name = doc.exists() && doc.getString("full_name") != null ? doc.getString("full_name") : "Doctor";
                                    logAuthEvent(FirebaseAnalytics.Event.LOGIN, email, true);
                                    loginResult.setValue(new AuthResponse("success", "Welcome back.", uid, name));
                                })
                                .addOnFailureListener(e -> {
                                    logAuthEvent(FirebaseAnalytics.Event.LOGIN, email, true);
                                    loginResult.setValue(new AuthResponse("success", "Welcome back.", uid, "Doctor"));
                                });
                    })
                    .addOnFailureListener(e -> {
                        String errorMsg = "Invalid email or password.";
                        if (e instanceof FirebaseNetworkException) {
                            errorMsg = "Network failure. Please check your connection.";
                        }
                        logAuthEvent(FirebaseAnalytics.Event.LOGIN, email, false);
                        loginResult.setValue(new AuthResponse("error", errorMsg, null, null));
                    });
        }
    }

    private void logAuthEvent(String eventType, String email, boolean success) {
        try {
            Bundle bundle = new Bundle();
            bundle.putString(FirebaseAnalytics.Param.METHOD, "email_password");
            bundle.putString("email", email);
            bundle.putBoolean("success", success);
            FirebaseManager.getAnalytics(getApplication()).logEvent(eventType, bundle);
        } catch (Exception ignored) {}
    }
}
