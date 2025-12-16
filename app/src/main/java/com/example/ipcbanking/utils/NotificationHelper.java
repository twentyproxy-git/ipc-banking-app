package com.example.ipcbanking.utils;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.example.ipcbanking.R;

import java.util.Random;

public class NotificationHelper {

    private static final String CHANNEL_ID = "OTP_CHANNEL";
    private static final String CHANNEL_NAME = "OTP Notifications";
    private static final String CHANNEL_DESC = "Channel for sending OTP codes";

    private Context mContext;
    private NotificationManager mManager;

    public NotificationHelper(Context context) {
        mContext = context;
        mManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(CHANNEL_DESC);
            mManager.createNotificationChannel(channel);
        }
    }

    public void sendOtpNotification(String otp) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_security_message) // Ensure you have this drawable
                .setContentTitle("IPC Bank OTP")
                .setContentText("Your verification code is: " + otp)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        // Use a random ID for each notification to ensure they all appear
        mManager.notify(new Random().nextInt(), builder.build());
    }
}
