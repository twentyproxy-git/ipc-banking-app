package com.example.ipcbanking;

import android.content.Context;
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;

public class FirebaseSeeder {

    private final FirebaseAuth auth;

    public FirebaseSeeder(Context context) {
        this.auth = FirebaseAuth.getInstance();
    }

    public void seedUsers() {
        createIfNotExists("aventurine@ipc.com", "aventurine123");
        createIfNotExists("topaz@ipc.com", "topaz123");
        createIfNotExists("sunday@ipc.com", "sunday123");
    }

    private void createIfNotExists(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    Log.d("FirebaseSeeder", "✅ Have finished in creation of user data: " + email);
                })
                .addOnFailureListener(e -> {
                    if (e instanceof FirebaseAuthUserCollisionException) {
                        Log.d("FirebaseSeeder", "ℹ️ User data has already existed (Ignore): " + email);
                    } else {
                        Log.e("FirebaseSeeder", "❌ Error occurred during creation of user data: " + email + ": " + e.getMessage());
                    }
                });
    }
}