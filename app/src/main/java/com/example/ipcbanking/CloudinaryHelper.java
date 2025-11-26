package com.example.ipcbanking;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.util.HashMap;
import java.util.Map;

public class CloudinaryHelper {

    private static boolean isInitialized = false;

    // 1. Khởi tạo Cloudinary (Gọi 1 lần duy nhất trong MainActivity hoặc Application)
    public static void initCloudinary(Context context) {
        if (!isInitialized) {
            Map<String, Object> config = new HashMap<>();
            config.put("cloud_name", "ipc-media");
            config.put("secure", true);
            MediaManager.init(context, config);
            isInitialized = true;
        }
    }

    // 2. Hàm upload ảnh
    public static void uploadImage(Uri imageUri, OnImageUploadListener listener) {
        String uploadPreset = "TEN_PRESET_CUA_BAN"; // <-- Thay Preset Name (Unsigned) ở Bước 1 vào đây

        MediaManager.get().upload(imageUri)
                .unsigned(uploadPreset)
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d("Cloudinary", "Bắt đầu upload...");
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        Log.d("Cloudinary", "Đang tải: " + bytes + "/" + totalBytes);
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        // Lấy đường dẫn ảnh HTTPS trả về
                        String imageUrl = (String) resultData.get("secure_url");
                        Log.d("Cloudinary", "Upload thành công: " + imageUrl);

                        // Trả kết quả về cho Activity xử lý tiếp
                        listener.onSuccess(imageUrl);
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e("Cloudinary", "Lỗi: " + error.getDescription());
                        listener.onError(error.getDescription());
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        // Xử lý khi mạng lỗi tự thử lại
                    }
                })
                .dispatch();
    }

    // Interface để hứng kết quả trả về
    public interface OnImageUploadListener {
        void onSuccess(String imageUrl);
        void onError(String error);
    }
}