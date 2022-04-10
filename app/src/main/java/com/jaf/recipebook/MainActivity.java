package com.jaf.recipebook;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    Helper helper;

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
        setContentView(R.layout.activity_start);
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
        if (helper.appPreferences.getBoolean(this.getString(R.string.preference_local_storage_key), true)
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
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(this.getString(R.string.dialog_deny), (dialogInterface, i) ->
                            helper.appPreferences.edit().putBoolean(
                                    helper.context.getString(R.string.preference_local_storage_key),
                                    false)
                                    .apply())
                    .show();
        } else if (Environment.isExternalStorageManager() &&
                !helper.appPreferences.getBoolean(this.getString(R.string.preference_local_storage_key), false)){
            helper.appPreferences.edit()
                    .putBoolean(this.getString(R.string.preference_local_storage_key), true)
                    .apply();
        }
    }

}