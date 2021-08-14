package com.example.chatfirebase.util;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.chatfirebase.R;

import java.util.ArrayList;
import java.util.List;

public class BaseCallActivity extends AppCompatActivity {

    private Intent sinchServiceIntent;

    private final ActivityResultLauncher<String[]> callPermissionsLauncher =
    registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissionsMap -> {
        if (!permissionsMap.containsValue(false)) {
            startService(sinchServiceIntent);
            sinchServiceIntent = null;
        }
    });

    protected void attemptToStartSinchService(Intent sinchServiceIntent) {
        List<String> missingCallPermissions = new ArrayList<>();

        String phonePermission = Manifest.permission.READ_PHONE_STATE;
        if (ContextCompat.checkSelfPermission(this, phonePermission) == PackageManager.PERMISSION_DENIED) {
            missingCallPermissions.add(phonePermission);
        }

        String microphonePermission = Manifest.permission.RECORD_AUDIO;
        if (ContextCompat.checkSelfPermission(this, microphonePermission) == PackageManager.PERMISSION_DENIED) {
            missingCallPermissions.add(microphonePermission);
        }

        if (missingCallPermissions.isEmpty()) {
            startService(sinchServiceIntent);
        }
        else if (ActivityCompat.shouldShowRequestPermissionRationale(this, phonePermission)
                || ActivityCompat.shouldShowRequestPermissionRationale(this, microphonePermission)) {

            showCallDialog((dialog, id) -> requestCallPermissions(sinchServiceIntent, missingCallPermissions));
        }
        else requestCallPermissions(sinchServiceIntent, missingCallPermissions);
    }

    private void requestCallPermissions(Intent sinchServiceIntent, List<String> missingCallPermissions) {
        this.sinchServiceIntent = sinchServiceIntent;
        callPermissionsLauncher.launch(missingCallPermissions.toArray(new String[0]));
    }

    private void showCallDialog(DialogInterface.OnClickListener positiveClick) {
        new AlertDialog.Builder(this, R.style.AlertDialog)
                .setMessage(getString(R.string.permission_microphone_phone))
                .setPositiveButton(R.string.dialog_continue, positiveClick)
                .setNegativeButton(R.string.dialog_ignore, null)
                .create()
                .show();
    }
}
