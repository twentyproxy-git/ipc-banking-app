package com.example.ipcbanking.utils;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryHelper {

    // === CẤU HÌNH ===
    private static final String CLOUD_NAME = "ipc-media";
    // [LƯU Ý] Đảm bảo tên preset này khớp với setting "Unsigned uploading enabled" trên Dashboard Cloudinary
    // Nếu bạn chưa tạo preset riêng, có thể dùng "ml_default" (thường là mặc định) hoặc "bank_app_preset" nếu đã tạo.
    private static final String UPLOAD_PRESET = "bank_app_preset";
    private static boolean isInitialized = false;

    // 1. Khởi tạo
    public static void initCloudinary(Context context) {
        if (!isInitialized) {
            try {
                Map<String, Object> config = new HashMap<>();
                config.put("cloud_name", CLOUD_NAME);
                config.put("secure", true);
                MediaManager.init(context, config);
                isInitialized = true;
                Log.d("CloudinaryHelper", "✅ Init Completed");
            } catch (Exception e) {
                Log.w("CloudinaryHelper", "⚠️ Skip init, ignore (Already initialized)");
            }
        }
    }

    // 2. Upload bằng mảng Byte (Dành cho ảnh Bitmap từ Camera)
    public static void uploadImageBytes(byte[] imageData, String publicId, OnImageUploadListener listener) {
        MediaManager.get().upload(imageData)
                .unsigned(UPLOAD_PRESET)
                .option("public_id", publicId)
                .option("folder", "bank_app_ekyc")
                .callback(createCallback(listener, publicId))
                .dispatch();
    }

    // 3. [MỚI] Upload bằng URI (Dành cho ảnh chọn từ Thư viện)
    public static void uploadImageUri(Uri imageUri, String publicId, OnImageUploadListener listener) {
        MediaManager.get().upload(imageUri)
                .unsigned(UPLOAD_PRESET)
                .option("public_id", publicId)
                .option("folder", "bank_app_customers") // Lưu vào folder riêng cho gọn
                .callback(createCallback(listener, publicId))
                .dispatch();
    }

    // Hàm tạo callback chung để tránh lặp code
    private static UploadCallback createCallback(OnImageUploadListener listener, String publicId) {
        return new UploadCallback() {
            @Override
            public void onStart(String requestId) {
                Log.d("CloudinaryHelper", "⏳ Start uploading: " + publicId);
            }

            @Override
            public void onProgress(String requestId, long bytes, long totalBytes) { }

            @Override
            public void onSuccess(String requestId, Map resultData) {
                String url = (String) resultData.get("secure_url");
                Log.d("CloudinaryHelper", "✅ Upload completed: " + url);
                listener.onSuccess(url);
            }

            @Override
            public void onError(String requestId, ErrorInfo error) {
                Log.e("CloudinaryHelper", "❌ Error: " + error.getDescription());
                listener.onError(error.getDescription());
            }

            @Override
            public void onReschedule(String requestId, ErrorInfo error) { }
        };
    }

    // Interface callback
    public interface OnImageUploadListener {
        void onSuccess(String imageUrl);
        void onError(String error);
    }
}