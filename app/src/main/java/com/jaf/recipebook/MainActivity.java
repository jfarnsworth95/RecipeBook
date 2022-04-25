package com.jaf.recipebook;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import android.app.ActionBar;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class MainActivity extends AppCompatActivity {

    Helper helper;
    public final String TAG = "JAF-MAIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Thread.sleep(2000);
        }catch (InterruptedException ex){
            Log.d("SplashRender", "onCreate: Failed to sleep...");
        }

        SplashScreen.installSplashScreen(this);
        helper = new Helper(this);

        ActionBar actionBar = getActionBar();

        setContentView(R.layout.activity_start);

        findViewById(R.id.breakItem).setOnClickListener(view -> {
            Log.i(TAG, "Begin the problems");
            startActivity(new Intent(this, GoogleSignInActivity.class));
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        // If MANAGE_EXTERNAL_STORAGE permission not granted and the Shared Preference for using
        // external storage is true (or doesn't exist yet), open dialog requesting the permission
        validateExternalPermission();

        // Check if user wants to connect to their Google Drive to backup the Recipe Files
        // TODO: Add method for adding Google Drive connection
    }

    public void validateExternalPermission(){
        if (helper.getPreference(helper.EXTERNAL_STORAGE_PREFERENCE, true)
                && !Environment.isExternalStorageManager()) {
            // Permission missing, but the user has indicated that they want external storage
            //      Could also be their first time starting up the app.
            new AlertDialog.Builder(this)
                    .setTitle("Requesting Access to Files")
                    .setMessage("Optionally, this app can put it's files where you can view/edit " +
                            "them in the explorer. Primarily, this is intended so you can back " +
                            "them up yourself, or import new recipes by dropping a file there.")
                    .setPositiveButton(this.getString(R.string.dialog_allow), (dialogInterface, i) -> {
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                .addCategory("android.intent.category.DEFAULT")
                                .setData(uri)
                        );
                        helper.setPreference(helper.EXTERNAL_STORAGE_PREFERENCE, true);
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(this.getString(R.string.dialog_deny), (dialogInterface, i) -> {
                                helper.setPreference(helper.EXTERNAL_STORAGE_PREFERENCE, true);
                    })
                    .show();
        } else if (Environment.isExternalStorageManager() && !helper.getPreference(helper.EXTERNAL_STORAGE_PREFERENCE, false)){
            helper.setPreference(helper.EXTERNAL_STORAGE_PREFERENCE, true);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        switch(item.getItemId()){

            case R.id.action_settings:
                // Open Settings Activity
                startActivity(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.search_btn:
                // Open Search Bar
                return true;

            default:
                Log.w(TAG, "onOptionsItemSelected: Unknown Item ID for selected item: "
                        + item.toString());
                return super.onOptionsItemSelected(item);
        }
    }
}