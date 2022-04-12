package com.jaf.recipebook;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class SettingsActivity extends AppCompatActivity {

    Helper helper;
    public final String TAG = "JAF-SETTINGS";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.i(TAG, "onCreate: CREATING");
        helper = new Helper(this);
        setContentView(R.layout.activity_settings);

        // Add button listener
        findViewById(R.id.toggle_external_storage_btn).setOnClickListener(view ->
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .addCategory("android.intent.category.DEFAULT")
                .setData(Uri.fromParts("package", getPackageName(), null))
        ));
    }

    @Override
    protected void onResume() {
        super.onResume();
        helper.setPreference(helper.EXTERNAL_STORAGE_PREFERENCE, Environment.isExternalStorageManager());
        Log.i(TAG, "PREFERENCE 'IS EXTERNAL' = " + helper.getPreference(helper.EXTERNAL_STORAGE_PREFERENCE, false));
    }
}