package com.jaf.recipebook.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.util.Log;

import com.google.gson.Gson;
import com.jaf.recipebook.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
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

    public File getDownloadsDir() {
        return Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
    }

    public File[] getDownloadsFolderFiles(){
        return getDownloadsDir().listFiles();
    }

    public HashMap<String, Object> returnGsonFromFile(File rpFile) {
        try {
            return new Gson().fromJson(this.readFile(rpFile), HashMap.class);
        } catch (Exception ex){
            Log.e(TAG, "validateRecipeFileFormat: Failed to interpret JSON content.", ex);
            return null;
        }
    }

    /**
     * Checks the structure of the provided file to see if it can be read into the application.
     * @param recipeFile Recipe file to read, should have the ".rp" extension.
     * @return True if the structure is valid.
     */
    public boolean validateRecipeFileFormat(File recipeFile){
        // Expected File Structure:
        //  {
        //      "NAME": STRING,
        //      "INGREDIENTS": [
        //	    	"MEASUREMENT & INGREDIENT NAME",
        //	    	"MEASUREMENT & INGREDIENT NAME",
        //	    	"" <!-- SPACER ROW FOR DISPLAY PURPOSES -->
        //	    	"MEASUREMENT & INGREDIENT NAME",
        //	    	...
        //	    ],
        //      "DIRECTIONS": "PLAIN TEXT DIRECTIONS",
        //      "SERVINGS": DOUBLE, *OPTIONAL*
        //      "SOURCE_URL": STRING URL, *OPTIONAL*
        //      "TAGS": ["TAG 1", "TAG 2", ... "TAG N"], *OPTIONAL*
        //      "CATEGORY": STRING *OPTIONAL*
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

        // Confirm Headers exist (NAME, INGREDIENTS, DIRECTIONS, SERVINGS)
        try {
            String[] requiredHeaders = new String[]{"NAME", "INGREDIENTS", "DIRECTIONS"};
            for (String header : requiredHeaders) {
                if (jsonData.get(header) == null) {
                    failures.add("Unable to confirm existence of required JSON headers.");
                    break;
                }
            }
        } catch (Exception ex){
            failures.add("Failed to confirm existence of headers (" + ex.getMessage() +")");
        }

        // Confirm "NAME" is a String
        try {
            if (!(jsonData.get("NAME") instanceof String)){
                failures.add("NAME is not a string.");
            }
        } catch (Exception ex){
            failures.add("Unable to confirm NAME Json content ("+ ex.getMessage() +")");
        }

        // Confirm "Ingredients" is a list of Strings
        try {
            for (Object ingredientEntry : ((ArrayList<String>) jsonData.get("INGREDIENTS"))) {
                if (!(ingredientEntry instanceof String)){
                    failures.add("Ingredient is not a string.");
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
            if (jsonData.get("SERVINGS") != null && !(jsonData.get("SERVINGS") instanceof Double)){
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

        // Confirm "Category", if it exists, is a String
        try {
            if (jsonData.get("CATEGORY") != null && !(jsonData.get("CATEGORY") instanceof String)){
                failures.add("Category is not a string.");
            }
        } catch (Exception ex){
            failures.add("Unable to confirm CATEGORY Json content ("+ ex.getMessage() +")");
        }

        // Confirm "SOURCE_URL", if it exists, is a String
        try {
            if (jsonData.get("SOURCE_URL") != null && !(jsonData.get("SOURCE_URL") instanceof String)){
                failures.add("SOURCE_URL is not a string.");
            }
        } catch (Exception ex){
            failures.add("Unable to confirm SOURCE_URL Json content ("+ ex.getMessage() +")");
        }

        // Record all the failures and log it for future reference, if needed.
        if (failures.size() > 0){
            Log.e(TAG, "validateRecipeFileFormat: Invalid file found. Reasons follow...\n" + failures);
            return false;
        }

        return true;
    }

}
