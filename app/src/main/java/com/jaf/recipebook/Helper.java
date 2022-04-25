package com.jaf.recipebook;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import com.google.api.services.drive.Drive;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Objects;

public class Helper {

    public SharedPreferences appPreferences;
    public Context context;

    public final String TAG = "JAF-HELPER";

    public final int EXTERNAL_STORAGE_PREFERENCE = 0;

    public Helper(Context context){
        this.context = context;
        appPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);
    }

    public void setPreference(int preference, boolean b){
        switch(preference){
            case EXTERNAL_STORAGE_PREFERENCE:
                appPreferences.edit()
                        .putBoolean(context.getString(R.string.preference_local_storage_key), b)
                        .apply();

            default:
                Log.w(TAG, "setPreference: Unknown Preference Int Provided: "
                        + Integer.toString(preference));
        }

    }

    public boolean getPreference(int preference, boolean defaultRtn){
        switch(preference){
            case EXTERNAL_STORAGE_PREFERENCE:
                return appPreferences.getBoolean(context.getString(R.string.preference_local_storage_key), defaultRtn);

            default:
                Log.w(TAG, "getPreference: Unknown Preference Int Provided: "
                        + Integer.toString(preference));
                return defaultRtn;
        }

    }

    public File[] getAllFiles(){
        File[] files;
        if(Environment.isExternalStorageManager()){
            // Fetch all external app files
            files = context.getExternalFilesDir(null).listFiles();
        } else {
            // Fetch all internal app files
            files = context.getFilesDir().listFiles();
        }

        return files;
    }

    public Uri[] getAllFileUris(){
        File[] files = getAllFiles();

        Uri[] uris = new Uri[files.length];
        for (int x = 0; x < files.length; x ++){
            uris[x] = Uri.fromFile(files[x]);
        }
        return uris;
    }

    public Uri getFileUri(String filename){
        return Uri.fromFile(getFile(filename));
    }

    public File getFile(String filename){
        if(Environment.isExternalStorageManager()){
            // Fetch external app file
            Log.i(TAG, "getFileUri: Grabbing External");
            return new File(context.getExternalFilesDir(null), filename);
        } else {
            // Fetch internal app file
            Log.i(TAG, "getFileUri: Grabbing Internal");
            return new File(context.getFilesDir(), filename);
        }
    }

    public File getAppLocalDataFolder(){
        if(Environment.isExternalStorageManager()){
            return context.getExternalFilesDir(null);
        } else {
            return context.getFilesDir();
        }
    }

    public boolean deleteFile(String filename){
        File file;
        if(Environment.isExternalStorageManager()){
            // Fetch external app file
            file = new File(context.getExternalFilesDir(null), filename);
        } else {
            // Fetch internal app file
            file = new File(context.getFilesDir(), filename);
        }
        return file.delete();
    }

    public String readFileUri(Uri uri) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream =
                     context.getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }

    public void editFileUri(Uri uri, String newContent) {
        try {
            ParcelFileDescriptor pfd = context.getContentResolver().
                    openFileDescriptor(uri, "w");

            FileOutputStream fileOutputStream =
                    new FileOutputStream(pfd.getFileDescriptor());
            fileOutputStream.write(newContent.getBytes());

            fileOutputStream.close();
            pfd.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "editFileUri: File not found", e);
            e.printStackTrace();
        } catch (IOException e) {
            Log.e(TAG, "editFileUri: IO Exception", e);
            e.printStackTrace();
        }
    }

    public void createFileUri(String filename, String content){
        Log.i(TAG, "createFileUri: creating file...");
        Uri uri = getFileUri(filename);
        editFileUri(uri, content);
    }

    public boolean doesFileExist(String filename){
        return new File(getFileUri(filename).getPath()).exists();
    }

}
