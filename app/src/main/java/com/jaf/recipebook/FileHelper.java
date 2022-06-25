package com.jaf.recipebook;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Environment;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.JsonReader;
import android.util.Log;

import com.google.gson.Gson;

import org.json.JSONArray;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

import javax.annotation.Nullable;

public class FileHelper {

    public SharedPreferences appPreferences;
    public Context context;
    public MessageDigest md;

    public final String TAG = "JAF-FileHelper";

    public final int EXTERNAL_STORAGE_PREFERENCE = 0;

    public FileHelper(Context context){
        this.context = context;
        appPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);

        try {
            this.md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex){
            Log.e(TAG, "Unable to create MD5 checksum digester", ex);
            this.md = null;
        }
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

    public Uri getFileUri(String filename){
        return Uri.fromFile(getFile(filename));
    }

    public Uri getFileUri(File file){ return Uri.fromFile(file); }

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

    public String readFile(File file) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(inputStream)))) {
            String line;
            while ((line = reader.readLine()) != null) {
                stringBuilder.append(line);
            }
        }
        return stringBuilder.toString();
    }

    public void saveFileUri(Uri uri, String newContent) throws FileNotFoundException, IOException {
        ParcelFileDescriptor pfd = context.getContentResolver().
                openFileDescriptor(uri, "w");

        FileOutputStream fileOutputStream =
                new FileOutputStream(pfd.getFileDescriptor());
        fileOutputStream.write(newContent.getBytes());

        fileOutputStream.close();
        pfd.close();
    }

    public void createFileUri(String filename, String content) throws FileNotFoundException, IOException{
        Log.i(TAG, "createFileUri: creating file...");
        Uri uri = getFileUri(filename);
        saveFileUri(uri, content);
    }

    public boolean doesFileExist(String filename){
        return new File(getFileUri(filename).getPath()).exists();
    }

    /**
     * Generates an MD5 Checksum for the provided file.
     * @param file The file to generate a checksum for.
     * @return An MD5 Hash representation of the file as a String.
     */
    public String getChecksumMD5(File file){
        if (this.md != null){
            try {
                InputStream fis = new FileInputStream(file);
                byte[] buffer = new byte[1024];
                int nread;
                while ((nread = fis.read(buffer)) != -1) {
                    md.update(buffer, 0, nread);
                }

                // I'm trusting a random source here, be kind StackOverflow gods.
                StringBuilder result = new StringBuilder();
                for (byte b : md.digest()) {
                    result.append(String.format("%02x", b));
                }
                return result.toString();

            } catch (IOException ex){
                Log.e(TAG, "getChecksumMD5: File not found...", ex);
                return null;
            }
        }else{
            Log.e(TAG, "getChecksumMD5: Failed to get Checksum");
            return null;
        }
    }

    public File[] getDownloadsFolderFiles(){
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                .listFiles();
    }

    /**
     * Checks the structure of the provided file to see if it can be read into the application.
     * @param recipeFile Recipe file to read, should have the ".rp" extension.
     * @return True if the structure is valid.
     */
    public boolean validateRecipeFileFormat(File recipeFile){
        // Expected File Structure:
        //  {
        //      "Ingredients": [
        //	    	["MEASUREMENT", "INGREDIENT NAME"],
        //	    	["MEASUREMENT", "INGREDIENT NAME"],
        //	    	[] <!-- SPACER ROW FOR DISPLAY PURPOSES -->
        //	    	["MEASUREMENT", "INGREDIENT NAME"],
        //	    	...
        //	    ],
        //      "DIRECTIONS": "PLAIN TEXT DIRECTIONS",
        //      "SERVINGS": DOUBLE,
        //      "SOURCE_URL": STRING URL, *OPTIONAL*
        //      "TAGS": ["TAG 1", "TAG 2", ... "TAG N"], *OPTIONAL*
        //      "CATEGORY": ["CATEGORY 1", "CATEGORY 2", ..., "CATEGORY N"] *OPTIONAL*
        //  }

        // Validate that the file is JSON formatted
        HashMap<String, Object> jsonData;
        ArrayList<String> failures = new ArrayList<>();
        try {
            jsonData = new Gson().fromJson(this.readFile(recipeFile), HashMap.class);
        } catch (Exception ex){
            Log.e(TAG, "validateRecipeFileFormat: Failed to interpret JSON content.", ex);
            return false;
        }

        // Confirm Headers exist (INGREDIENTS, DIRECTIONS, SERVINGS)
        try {
            String[] requiredHeaders = new String[]{"INGREDIENTS", "DIRECTIONS", "SERVINGS"};
            for (String header : requiredHeaders) {
                if (jsonData.get(header) == null) {
                    failures.add("Unable to confirm existence of required JSON headers.");
                    break;
                }
            }
        } catch (Exception ex){
            failures.add("Failed to confirm existence of headers (" + ex.getMessage() +")");
        }

        // Confirm "Ingredients" is a list of 'size 2 lists'
        try {
            for (Object ingredientEntry : ((ArrayList<String>) jsonData.get("INGREDIENTS"))) {
                if (((ArrayList)ingredientEntry).size() != 2 && ((ArrayList)ingredientEntry).size() != 0) {
                    failures.add("Ingredient entry has incorrect number of indices.");
                    break;
                }
            }
        } catch (Exception ex){
            failures.add("Unable to confirm Ingredient Json content ("+ ex.getMessage() +")");
        }

        // Confirm "Directions" is a String
        try {
            if (!(jsonData.get("DIRECTIONS") instanceof String)){
                failures.add("Directions are not a string.");
            }
        } catch (Exception ex){
            failures.add("Unable to confirm Directions Json content ("+ ex.getMessage() +")");
        }

        // Confirm "Servings" is a Float
        try {
            if (!(jsonData.get("SERVINGS") instanceof Double)){
                failures.add("Servings is not a double.");
            }
        } catch (Exception ex){
            failures.add("Unable to confirm Servings Json content ("+ ex.getMessage() +")");
        }

        // Confirm "Tags", if they exist, is a list of Strings
        try {
            if (jsonData.get("TAGS") != null) {
                if (!(jsonData.get("TAGS") instanceof ArrayList)) {
                    failures.add("Tags is not a List");
                } else {
                    ArrayList list = (ArrayList) jsonData.get("TAGS");
                    for (int i = 0; i < list.size(); i++){
                        if (!(list.get(i) instanceof String)){
                            failures.add("Not all Tags are strings.");
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex){
            failures.add("Unable to confirm Tags Json content ("+ ex.getMessage() +")");
        }

        // Confirm "Category", if it exists, is a String in the list of available Categories
        try {
            if (jsonData.get("CATEGORY") != null) {
                if (!(jsonData.get("CATEGORY") instanceof ArrayList)) {
                    failures.add("CATEGORY is not a List");
                } else {
                    ArrayList list = (ArrayList) jsonData.get("CATEGORY");
                    for (int i = 0; i < list.size(); i++){
                        if (!(list.get(i) instanceof String)){
                            failures.add("Not all Categories are strings.");
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex){
            failures.add("Unable to confirm CATEGORY Json content ("+ ex.getMessage() +")");
        }

        // Record all the failures and log it for future reference, if needed.
        if (failures.size() > 0){
            Log.e(TAG, "validateRecipeFileFormat: Invalid file found. Reasons follow...\n" + failures);
            return false;
        }

        return true;
    }

    public void copyFile(File source, File destination) throws IOException{
        InputStream in = new FileInputStream(source);
        try {
            OutputStream out = new FileOutputStream(destination);
            try {
                // Transfer bytes from in to out
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            } finally {
                out.close();
            }
        } finally {
            in.close();
        }
    }

}
