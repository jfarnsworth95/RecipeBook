package com.jaf.recipebook.helpers;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.jaf.recipebook.R;
import com.jaf.recipebook.db.directions.DirectionsModel;
import com.jaf.recipebook.db.ingredients.IngredientsModel;
import com.jaf.recipebook.db.recipes.RecipesModel;
import com.jaf.recipebook.db.tags.TagsModel;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FileHelper {

    public SharedPreferences appPreferences;
    public Context context;
    public MessageDigest md;

    public final String TAG = "JAF-FileHelper";

    public final int EXTERNAL_STORAGE_PREFERENCE = 0;
    public final int CATEGORY_ORDER_PREFERENCE = 1;
    public final int DISPLAY_MODE_PREFERENCE = 2;
    public final int CLOUD_STORAGE_ACTIVE_PREFERENCE = 3;
    public final int STARTUP_COUNTER_PREFERENCE = 4;
    public final int AUTO_BACKUP_ACTIVE_PREFERENCE = 5;
    public final int BACKUP_TIMESTAMP_PREFERENCE = 6;
    public final int DISPLAY_MODE_OS = 0;
    public final int DISPLAY_MODE_LIGHT = 1;
    public final int DISPLAY_MODE_DARK = 2;
    public final String JSON_NAME = "NAME";
    public final String JSON_INGREDIENTS = "INGREDIENTS";
    public final String JSON_DIRECTIONS = "DIRECTIONS";
    public final String JSON_UUID = "UUID";
    public final String JSON_SERVINGS = "SERVINGS";
    public final String JSON_SOURCE_URL = "SOURCE_URL";
    public final String JSON_TAGS = "TAGS";
    public final String JSON_CATEGORY = "CATEGORY";

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

    public void setPreference(int preference, String s){
        switch(preference){
            case CATEGORY_ORDER_PREFERENCE:
                appPreferences.edit()
                        .putString(context.getString(R.string.preference_category_ordering), s)
                        .apply();
                break;

            default:
                Log.w(TAG, "setPreference: Unknown Preference Int Provided for string: "
                        + Integer.toString(preference));
        }
    }

    public String getPreference(int preference, String defaultRtn){
        switch(preference){
            case CATEGORY_ORDER_PREFERENCE:
                return appPreferences.getString(context.getString(R.string.preference_category_ordering), defaultRtn);

            default:
                Log.w(TAG, "getPreference: Unknown Preference Int Provided for string: "
                        + Integer.toString(preference));
                return defaultRtn;
        }

    }

    public void setPreference(int preference, boolean b){
        switch(preference){
            case EXTERNAL_STORAGE_PREFERENCE:
                appPreferences.edit()
                        .putBoolean(context.getString(R.string.preference_local_storage_key), b)
                        .apply();
                break;

            case CLOUD_STORAGE_ACTIVE_PREFERENCE:
                appPreferences.edit()
                        .putBoolean(context.getString(R.string.preference_cloud_storage_active_key), b)
                        .apply();
                break;

            case AUTO_BACKUP_ACTIVE_PREFERENCE:
                appPreferences.edit()
                        .putBoolean(context.getString(R.string.preference_auto_backup), b)
                        .apply();
                break;

            default:
                Log.w(TAG, "setPreference: Unknown Preference Int Provided for boolean: "
                        + Integer.toString(preference));
        }
    }

    public boolean getPreference(int preference, boolean defaultRtn){
        switch(preference){
            case EXTERNAL_STORAGE_PREFERENCE:
                return appPreferences.getBoolean(context.getString(R.string.preference_local_storage_key), defaultRtn);

            case CLOUD_STORAGE_ACTIVE_PREFERENCE:
                return appPreferences.getBoolean(context.getString(R.string.preference_cloud_storage_active_key), defaultRtn);

            case AUTO_BACKUP_ACTIVE_PREFERENCE:
                return appPreferences.getBoolean(context.getString(R.string.preference_auto_backup), defaultRtn);

            default:
                Log.w(TAG, "getPreference: Unknown Preference Int Provided for boolean: "
                        + Integer.toString(preference));
                return defaultRtn;
        }

    }

    public void setPreference(int preference, int i){
        switch(preference){
            case DISPLAY_MODE_PREFERENCE:
                appPreferences.edit()
                        .putInt(context.getString(R.string.preference_display_mode), i)
                        .apply();
                break;

            case STARTUP_COUNTER_PREFERENCE:
                appPreferences.edit()
                        .putInt(context.getString(R.string.preference_startup_counter), i)
                        .apply();
                break;

            default:
                Log.w(TAG, "setPreference: Unknown Preference Int Provided for integer: "
                        + Integer.toString(preference));
        }
    }

    public int getPreference(int preference, int defaultRtn){
        switch(preference){
            case DISPLAY_MODE_PREFERENCE:
                return appPreferences.getInt(context.getString(R.string.preference_display_mode), defaultRtn);

            case STARTUP_COUNTER_PREFERENCE:
                return appPreferences.getInt(context.getString(R.string.preference_startup_counter), defaultRtn);

            default:
                Log.w(TAG, "getPreference: Unknown Preference Int Provided for integer: "
                        + Integer.toString(preference));
                return defaultRtn;
        }

    }

    public void setPreference(int preference, long l) {
        switch(preference){
            case BACKUP_TIMESTAMP_PREFERENCE:
                appPreferences.edit()
                        .putLong(context.getString(R.string.preference_backup_timestamp), l)
                        .apply();
                break;

            default:
                Log.w(TAG, "setPreference: Unknown Preference Int Provided for long: "
                        + Integer.toString(preference));
        }

    }

    public long getPreference(int preference, long defaultRtn) {
        switch(preference){
            case BACKUP_TIMESTAMP_PREFERENCE:
                return appPreferences.getLong(context.getString(R.string.preference_backup_timestamp), defaultRtn);

            default:
                Log.w(TAG, "getPreference: Unknown Preference Int Provided for long: "
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
            Log.i(TAG, "getFile: Grabbing Internal");
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

    public String readUriFile(Uri file) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        try (InputStream inputStream = context.getContentResolver().openInputStream(file);
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

    public ArrayList<File> getDownloadsFolderFiles(){
        ArrayList<File> rpFilePaths = new ArrayList<>();

        // Define the projection (the columns to retrieve)
        String[] projection = {
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATA
        };

        // Define the URI to query the downloads folder
        Uri queryUri = MediaStore.Files.getContentUri("external");

        // Query the MediaStore
        ContentResolver contentResolver = context.getContentResolver();
        try (Cursor cursor = contentResolver.query(
                queryUri,
                projection,
                null,
                null,
                null)) {
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    String filePath = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA));
                    if (filePath != null && filePath.endsWith(".rp")) {
                        rpFilePaths.add(new File(filePath));
                    }
                }
            }
        }

        return rpFilePaths;
    }

    public UUID getImportFileUuid(Uri importFile){
        HashMap<String, Object> fileGson = returnGsonFromFile(importFile);
        return UUID.fromString((String) fileGson.get("UUID"));
    }

    public HashMap<String, Object> returnGsonFromFile(Uri rpFile) {
        try {
            return new Gson().fromJson(this.readUriFile(rpFile), HashMap.class);
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
    public boolean validateRecipeFileFormat(Uri recipeFile, HashSet<UUID> scannedUuids){
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
        //      "UUID": JAVA.UUID,
        //      "SERVINGS": DOUBLE, *OPTIONAL*
        //      "SOURCE_URL": STRING URL, *OPTIONAL*
        //      "TAGS": ["TAG 1", "TAG 2", ... "TAG N"], *OPTIONAL*
        //      "CATEGORY": STRING *OPTIONAL*
        //  }

        // Validate that the file is JSON formatted
        HashMap<String, Object> jsonData;
        ArrayList<String> failures = new ArrayList<>();
        try {
            jsonData = new Gson().fromJson(this.readUriFile(recipeFile), HashMap.class);
        } catch (Exception ex){
            Log.e(TAG, "validateRecipeFileFormat: Failed to interpret JSON content.", ex);
            return false;
        }

        // Confirm Headers exist (NAME, INGREDIENTS, DIRECTIONS, SERVINGS, UUID)
        try {
            String[] requiredHeaders = new String[]{JSON_NAME, JSON_INGREDIENTS, JSON_DIRECTIONS, JSON_UUID};
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
            if (!(jsonData.get(JSON_NAME) instanceof String)){
                failures.add(JSON_NAME + " is not a string.");
            }
        } catch (Exception ex){
            failures.add("Unable to confirm "+ JSON_NAME +" Json content ("+ ex.getMessage() +")");
        }

        // Confirm "Ingredients" is a list of Strings
        try {
            for (Object ingredientEntry : ((ArrayList<String>) jsonData.get(JSON_INGREDIENTS))) {
                if (!(ingredientEntry instanceof String)){
                    failures.add(JSON_INGREDIENTS + " is not a string.");
                    break;
                }
            }
        } catch (Exception ex){
            failures.add("Unable to confirm "+ JSON_INGREDIENTS +" Json content ("+ ex.getMessage() +")");
        }

        // Confirm "Directions" is a String
        try {
            if (!(jsonData.get(JSON_DIRECTIONS) instanceof String)){
                failures.add(JSON_DIRECTIONS + " are not a string.");
            }
        } catch (Exception ex){
            failures.add("Unable to confirm "+ JSON_DIRECTIONS +" Json content ("+ ex.getMessage() +")");
        }

        // Confirm "UUID" is a UUID
        try {
            if (!(jsonData.get(JSON_UUID) instanceof String )){
                failures.add(JSON_UUID + " is not a UUID.");
            }
            UUID uuid = UUID.fromString(((String) jsonData.get(JSON_UUID)));
            if (scannedUuids.contains(uuid)){
                failures.add(JSON_UUID + " has already been found in Downloads");
            }
        } catch (IllegalArgumentException exception){
            failures.add(JSON_UUID +" is not a valid UUID");
        } catch (Exception ex){
            failures.add("Unable to confirm "+ JSON_UUID +" Json content ("+ ex.getMessage() +")");
        }

        // Confirm "Servings" is a Float
        try {
            if (jsonData.get(JSON_SERVINGS) != null && !(jsonData.get(JSON_SERVINGS) instanceof Double)){
                failures.add(JSON_SERVINGS + " is not a double.");
            }
        } catch (Exception ex){
            failures.add("Unable to confirm "+ JSON_SERVINGS +" Json content ("+ ex.getMessage() +")");
        }

        // Confirm "Tags", if they exist, is a list of Strings
        try {
            if (jsonData.get(JSON_TAGS) != null) {
                if (!(jsonData.get(JSON_TAGS) instanceof ArrayList)) {
                    failures.add(JSON_TAGS + " is not a List");
                } else {
                    ArrayList list = (ArrayList) jsonData.get(JSON_TAGS);
                    for (int i = 0; i < list.size(); i++){
                        if (!(list.get(i) instanceof String)){
                            failures.add("Not all " + JSON_TAGS + "s are strings.");
                            break;
                        }
                    }
                }
            }
        } catch (Exception ex){
            failures.add("Unable to confirm "+ JSON_TAGS +" Json content ("+ ex.getMessage() +")");
        }

        // Confirm "Category", if it exists, is a String
        try {
            if (jsonData.get(JSON_CATEGORY) != null && !(jsonData.get(JSON_CATEGORY) instanceof String)){
                failures.add(JSON_CATEGORY + " is not a string.");
            }
        } catch (Exception ex){
            failures.add("Unable to confirm "+ JSON_CATEGORY +" Json content ("+ ex.getMessage() +")");
        }

        // Confirm "SOURCE_URL", if it exists, is a String
        try {
            if (jsonData.get(JSON_SOURCE_URL) != null && !(jsonData.get(JSON_SOURCE_URL) instanceof String)){
                failures.add(JSON_SOURCE_URL + " is not a string.");
            }
        } catch (Exception ex){
            failures.add("Unable to confirm "+ JSON_SOURCE_URL +" Json content ("+ ex.getMessage() +")");
        }

        // Record all the failures and log it for future reference, if needed.
        if (!failures.isEmpty()){
            Log.e(TAG, "validateRecipeFileFormat: Invalid file found. Reasons follow...\n" + failures);
            return false;
        }

        return true;
    }

    public boolean saveRecipeToDownloads(RecipesModel rm, List<IngredientsModel> ims, DirectionsModel dm, List<TagsModel> tms, boolean isBulk){
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
        //      "DIRECTIONS": "PLAIN TEXT DIRECTIONS",,
        //      "UUID": JAVA.UUID,
        //      "SERVINGS": DOUBLE, *OPTIONAL*
        //      "SOURCE_URL": STRING URL, *OPTIONAL*
        //      "TAGS": ["TAG 1", "TAG 2", ... "TAG N"], *OPTIONAL*
        //      "CATEGORY": STRING *OPTIONAL*
        //  }

        JsonArray ingredientJsonList = new JsonArray();
        for (IngredientsModel im : ims){
            ingredientJsonList.add(im.getText());
        }

        JsonArray tagsJsonList = new JsonArray();
        for (TagsModel tm : tms){
            tagsJsonList.add(tm.getTag());
        }

        String filename = rm.getName().replaceAll("[^a-zA-Z0-9\\-]", "_");
        JsonObject json = new JsonObject();
        json.addProperty(JSON_NAME, rm.getName());
        json.add(JSON_INGREDIENTS, ingredientJsonList);
        json.addProperty(JSON_DIRECTIONS, dm.getText());
        json.addProperty(JSON_UUID, rm.getUuid().toString());

        if (rm.getServings() != null){
            json.addProperty(JSON_SERVINGS, rm.getServings());
        }
        if (rm.getSource_url() != null){
            json.addProperty(JSON_SOURCE_URL, rm.getSource_url());
        }
        if (!tms.isEmpty()){
            json.add(JSON_TAGS, tagsJsonList);
        }
        if (rm.getCategory() != null){
            json.addProperty(JSON_CATEGORY, rm.getCategory());
        }

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Downloads.DISPLAY_NAME, filename + "." + context.getString(R.string.recipe_file_extension));
        contentValues.put(MediaStore.Downloads.MIME_TYPE, "application/com.jaf.recipebook");
        contentValues.put(MediaStore.Downloads.RELATIVE_PATH, "Download/");

        Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues);
        if (uri != null) {
            try (OutputStream outputStream = context.getContentResolver().openOutputStream(uri)) {
                if (outputStream != null) {
                    outputStream.write(json.toString().getBytes());
                    Log.d(TAG, "File saved successfully: " + uri.toString());

                    if (!isBulk){
                        Toast.makeText(context, context.getString(R.string.saved_recipe_in_downloads), Toast.LENGTH_LONG).show();
                    }
                }
            } catch (Exception e) {
                Toast.makeText(context, context.getString(R.string.failed_to_open_recipe), Toast.LENGTH_LONG).show();
                return false;
            }
        } else {
            Log.e(TAG, "Failed to create new MediaStore record.");
            return false;
        }

        return true;
    }

}
