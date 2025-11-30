package com.example.ipcbanking.utils;

import android.content.Context;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class FirebaseHelper {

    private FirebaseAuth mAuth;
    private Context context;

    public FirebaseHelper(Context context) {
        this.context = context;
        mAuth = FirebaseAuth.getInstance();
    }

    // ===== Login =====
    public void loginUser(String email, String password, FirebaseCallback callback) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(context, "Email or password cannot be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess(mAuth.getCurrentUser());
                    } else {
                        callback.onFailure(task.getException());
                    }
                });
    }

    // ===== Get current user =====
    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    // ===== Callback interface =====
    public interface FirebaseCallback {
        void onSuccess(FirebaseUser user);
        void onFailure(Exception e);
    }
}
