package com.example.ipcbanking.utils;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public class QRCodeHelper {

    private Context context;

    public QRCodeHelper(Context context) {
        this.context = context;
    }

    public Bitmap generateQRCode(String content) {
        QRCodeWriter writer = new QRCodeWriter();
        try {
            BitMatrix bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
                }
            }
            return bmp;
        } catch (WriterException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void saveQRCodeToGallery(Bitmap bitmap, String fileNamePrefix, String bookingId) {
        if (bitmap == null) {
            Toast.makeText(context, "Failed to generate QR Code.", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = fileNamePrefix + bookingId + ".png";
        OutputStream fos = null;

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
                values.put(MediaStore.MediaColumns.MIME_TYPE, "image/png");
                values.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/IPC Bank Tickets");

                Uri imageUri = context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
                if (imageUri != null) {
                    fos = context.getContentResolver().openOutputStream(imageUri);
                }
            } else {
                String imagesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() + "/IPC Bank Tickets";
                File dir = new File(imagesDir);
                if (!dir.exists()) {
                    dir.mkdirs();
                }
                File imageFile = new File(imagesDir, fileName);
                fos = new FileOutputStream(imageFile);
            }

            if (fos != null) {
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                Toast.makeText(context, "Ticket QR code saved to Gallery.", Toast.LENGTH_LONG).show();
            } else {
                throw new IOException("Failed to create output stream.");
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(context, "Failed to save QR Code: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String createFlightBookingJson(String bookingId, String userId, String passengerName, String flightNumber, String airline, String depAirport, String arrAirport, String depTime, String flightClass) {
        JSONObject json = new JSONObject();
        try {
            json.put("bookingType", "FLIGHT");
            json.put("bookingId", bookingId);
            json.put("passengerId", userId);
            json.put("passengerName", passengerName);
            json.put("flightNumber", flightNumber);
            json.put("airline", airline);
            json.put("departureAirport", depAirport);
            json.put("arrivalAirport", arrAirport);
            json.put("departureTime", depTime);
            json.put("flightClass", flightClass);
            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String createMovieBookingJson(String bookingId, String userId, String movieTitle, String cinemaName, String showtime, String seats) {
        JSONObject json = new JSONObject();
        try {
            json.put("bookingType", "MOVIE");
            json.put("bookingId", bookingId);
            json.put("userId", userId);
            json.put("movieTitle", movieTitle);
            json.put("cinemaName", cinemaName);
            json.put("showtime", showtime);
            json.put("seats", seats);
            return json.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }
}
