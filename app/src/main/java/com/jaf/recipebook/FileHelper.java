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
    public final String CATEGORY_STORE = "CATEGORIES";

    public final int ADD_CATEGORY = 10;
    public final int DELETE_CATEGORY = 11;
    public final int UPDATE_CATEGORY = 12;

    public final int ADD_TAG = 20;
    public final int DELETE_TAG = 21;
    public final int UPDATE_TAG = 22;

    // Headers in JSON Recipe files
    public final String NAME_HEADER = "NAME";
    public final String INGREDIENT_HEADER = "INGREDIENTS";
    public final String DIRECTIONS_HEADER = "DIRECTIONS";
    public final String SERVINGS_HEADER = "SERVINGS";
    public final String SOURCE_URL_HEADER = "SOURCE_URL";
    public final String TAGS_HEADER = "TAGS";
    public final String CATEGORY_HEADER = "CATEGORY";

    // Config Files
    public final String CATEGORY_FILE = "categories.json";
    public final String TAG_FILE = "tags.json";

    public FileHelper(Context context){
        this.context = context;
        // app preferences is used for small amounts of persistent data, like storage preference and
        // recipe categories (That's actually everything I'm using it for right now...)
        appPreferences = context.getSharedPreferences(context.getString(R.string.preference_file_key),
                Context.MODE_PRIVATE);

        try {
            // md is only used for the checksum method
            this.md = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException ex){
            Log.e(TAG, "Unable to create MD5 checksum digester", ex);
            this.md = null;
        }
    }

    /**
     * Gets the list of categories that recipes can fall under.
     * @return Set of categories valid for this instance.
     */
    public HashSet<String> getCategoriesTitles(){
        return (HashSet<String>) appPreferences.getStringSet(CATEGORY_STORE, new HashSet<String>());
    }

    /**
     * Add a category to the user app preferences.
     * @param newCategory Unique category to add.
     */
    public void addCategoryTitle(String newCategory){
        HashSet<String> currentSet = getCategoriesTitles();
        currentSet.add(newCategory);
        appPreferences.edit().putStringSet(CATEGORY_STORE, currentSet);
    }

    /**
     * Delete a category from the user app preferences. This will cause a sync event with relevant
     * recipe files to remove their internal category listing.
     * @param categoryToRemove
     */
    public void deleteCategoryTitle(String categoryToRemove){
        HashSet<String> currentSet = getCategoriesTitles();
        currentSet.remove(categoryToRemove);
        //TODO: Delete entry in Category File
        appPreferences.edit().putStringSet(CATEGORY_STORE, currentSet);
    }

    /**
     * Use this method to set boolean preferences.
     * @param preference INT representing the preference, found in this class.
     * @param b True/False for the provided preference.
     */
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

    /**
     * Returns user preference.
     * @param preference INT representing the preference, found in this class.
     * @param defaultRtn If the preference hasn't already been set, what this should return.
     * @return User preference
     */
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

    /**
     * Get all files that Recipe Book directly handles
     * @return List of files in the Recipe Book data folder
     */
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

    /**
     * Get all file URIs that Recipe Book directly handles
     * @return List of file URIs in the Recipe Book data folder
     */
    public Uri[] getAllFileUris(){
        File[] files = getAllFiles();

        Uri[] uris = new Uri[files.length];
        for (int x = 0; x < files.length; x ++){
            uris[x] = Uri.fromFile(files[x]);
        }
        return uris;
    }

    /**
     * Get file from internal Recipe Book data folder based on the unique filename.
     * @param filename
     * @return
     */
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

    /**
     * Get URI from a filename for a file in the Recipe Book data folder.
     * @param filename
     * @return
     */
    public Uri getFileUri(String filename){
        return Uri.fromFile(getFile(filename));
    }

    /**
     *
     * @param file
     * @return
     */
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
     * Takes a file and returns a HashMap of it's values in JSON style.
     * @param file File to read JSON content
     * @return HashMap of JSON data
     * @throws IOException Thrown if error occurs when reading the file
     */
    public HashMap<String, Object> getJsonContent(File file) throws IOException{
        HashMap<String, Object> jsonData;
        ArrayList<String> failures = new ArrayList<>();
        return new Gson().fromJson(this.readFile(file), HashMap.class);
    }

    /**
     * Checks the structure of the provided file to see if it can be read into the application.
     * @param recipeFile Recipe file to read, should have the ".rp" extension.
     * @return True if the structure is valid.
     */
    public boolean validateRecipeFileFormat(File recipeFile){
        // Expected File Structure:
        //  {
        //      "NAME": "RECIPE NAME",
        //      "INGREDIENTS": [
        //	    	["MEASUREMENT", "INGREDIENT NAME"],
        //	    	["MEASUREMENT", "INGREDIENT NAME"],
        //	    	[] <!-- SPACER ROW FOR DISPLAY PURPOSES (OPTIONAL) -->
        //          ["HEADER"] <!-- SPECIAL HEADER FOR INGREDIENT GROUPINGS (OPTIONAL) -->
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
            jsonData = getJsonContent(recipeFile);
        } catch (IOException ex){
            Log.e(TAG, "validateRecipeFileFormat: Failed to read file", ex);
            return false;
        } catch (Exception ex){
            Log.e(TAG, "validateRecipeFileFormat: Failed to interpret JSON content.", ex);
            return false;
        }

        // Confirm Headers exist (NAME, INGREDIENTS, DIRECTIONS, SERVINGS)
        try {
            String[] requiredHeaders = new String[]{NAME_HEADER, INGREDIENT_HEADER,
                                                    DIRECTIONS_HEADER, SERVINGS_HEADER};
            for (String header : requiredHeaders) {
                if (jsonData.get(header) == null) {
                    failures.add("Unable to confirm existence of required JSON headers.");
                    break;
                }
            }
        } catch (Exception ex){
            failures.add("Failed to confirm existence of headers (" + ex.getMessage() +")");
        }

        // Confirm "Name" is a String
        try {
            if (!(jsonData.get(NAME_HEADER) instanceof String)){
                failures.add("NAME is not a string.");
            }
        } catch (Exception ex){
            failures.add("Unable to confirm NAME Json content ("+ ex.getMessage() +")");
        }

        // Confirm "Ingredients" is a list of 'size 0-2 lists'
        try {
            for (Object ingredientEntry : ((ArrayList<String>) jsonData.get(INGREDIENT_HEADER))) {
                if (((ArrayList)ingredientEntry).size() > 2 || ((ArrayList)ingredientEntry).size() < 0) {
                    failures.add("Ingredient entry has incorrect number of indices.");
                    break;
                }
            }
        } catch (Exception ex){
            failures.add("Unable to confirm Ingredient Json content ("+ ex.getMessage() +")");
        }

        // Confirm "Directions" is a String
        try {
            if (!(jsonData.get(DIRECTIONS_HEADER) instanceof String)){
                failures.add("Directions are not a string.");
            }
        } catch (Exception ex){
            failures.add("Unable to confirm Directions Json content ("+ ex.getMessage() +")");
        }

        // Confirm "Servings" is a Float
        try {
            if (!(jsonData.get(SERVINGS_HEADER) instanceof Double)){
                failures.add("Servings is not a double.");
            }
        } catch (Exception ex){
            failures.add("Unable to confirm Servings Json content ("+ ex.getMessage() +")");
        }

        // Confirm "Tags", if they exist, is a list of Strings
        try {
            if (jsonData.get(TAGS_HEADER) != null) {
                if (!(jsonData.get(TAGS_HEADER) instanceof ArrayList)) {
                    failures.add("Tags is not a List");
                } else {
                    ArrayList list = (ArrayList) jsonData.get(TAGS_HEADER);
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
            if (jsonData.get(CATEGORY_HEADER) != null) {
                if (!(jsonData.get(CATEGORY_HEADER) instanceof ArrayList)) {
                    failures.add("CATEGORY is not a List");
                } else {
                    ArrayList list = (ArrayList) jsonData.get(CATEGORY_HEADER);
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

    /**
     * When provided a Category title, and operation ID, this method will perform the functions to
     * Add/Remove/Update any category entry, then update the recipe files effected to match.
     * @param title Category title to modify
     * @param operation Integer ID to Add/Remove/Update
     * @param entries Optional list of files effected (for imports).
     * @param newTitle Optional Category title that will replace the old title.
     * @return True if successfully updates all related files.
     */
    public boolean modifyCategory(String title, int operation, @Nullable ArrayList<String> entries, @Nullable String newTitle){
        // Records any failures, but doesn't stop further attempts
        boolean isSuccessful = true;

        // Get the category file
        File categoryFile = getFile(CATEGORY_FILE);

        // Make the category file content into a JSON object
        HashMap<String, Object> jsonData;
        try {
            jsonData = getJsonContent(categoryFile);
        } catch (Exception ex){
            Log.e(TAG, "modifyCategory: Failed to interpret JSON content.", ex);
            return false;
        }

        // Determine if we Create/Update/Delete
        switch(operation) {
            case (ADD_CATEGORY):
                // Add new header (title) and entry, if not null
                if (entries == null) {
                    jsonData.put(title, new ArrayList<String>());
                } else {
                    jsonData.put(title, entries);
                }
                break;

            case (DELETE_CATEGORY):
                // Delete existing header (title)
                jsonData.remove(title);

                // Get all recipes with that category and remove it from them
                ArrayList<String> filenamesWithCategory_r = getFilesForCategory(title);
                for (String filename : filenamesWithCategory_r){
                    if (!removeCategoryInRecipe(new File(filename), title)){
                        Log.w(TAG, "modifyCategory: Failed to remove category from '" + filename + "'");
                        isSuccessful = false;
                    }
                }
                break;

            case (UPDATE_CATEGORY):
                // Update title with new title as the JSON header.
                ArrayList<String> oldList = (ArrayList<String>) jsonData.get(title);
                jsonData.remove(title);
                jsonData.put(newTitle, oldList);

                // Get all recipes with that category and update it for them
                ArrayList<String> filenamesWithCategory_u = getFilesForCategory(title);
                for (String filename : filenamesWithCategory_u){
                    if (!updateCategoryInRecipe(new File(filename), title, newTitle)){
                        Log.w(TAG, "modifyCategory: Failed to update category for '" + filename + "'");
                        isSuccessful = false;
                    }
                }
                break;

            default:
                Log.e(TAG, "modifyCategory: Unknown operation passed in.");
                return false;
        }

        // Write JSON content back to file
        try {
            saveFileUri(getFileUri(categoryFile), new Gson().toJson(jsonData));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "modifyCategory: File not found", e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "modifyCategory: IO Exception", e);
            return false;
        }

        return isSuccessful;
    }

    /**
     * Fetches the list of files that fall under the provided category
     * @param category Recipe category
     * @return List of files, if the category is valid. Invalid categories return empty list.
     */
    public ArrayList<String> getFilesForCategory(String category){
        HashMap<String, Object> jsonData;
        try {
            jsonData = getJsonContent(getFile(CATEGORY_FILE));
        } catch (IOException ex){
            Log.e(TAG, "getFilesForCategory: Failed to get category file", ex);
            return new ArrayList<>();
        }

        if (jsonData.containsKey(category)){
            return (ArrayList<String>) jsonData.get(category);
        } else {
            return new ArrayList<>();
        }
    }

    /**
     * Removes Category entry from a recipe.
     * @param recipe File to remove category from.
     * @param categoryToRemove Category to remove.
     * @return True, if successful. False returns will write failure to log.
     */
    public boolean removeCategoryInRecipe(File recipe, String categoryToRemove){
        return updateCategoryInRecipe(recipe, categoryToRemove, null);
    }

    /**
     * Replaces Category entry in a recipe file.
     * @param recipe File to update Category.
     * @param oldCategory Category to remove.
     * @param newCategory Category to add.
     * @return True, if successful. False returns will write failure to log.
     */
    public boolean updateCategoryInRecipe(File recipe, String oldCategory, @Nullable String newCategory){
        // Convert file content to HashMap (JSON)
        HashMap<String, Object> jsonData;
        try {
            jsonData = getJsonContent(recipe);
        } catch (IOException ex){
            Log.e(TAG, "validateRecipeFileFormat: Failed to read file", ex);
            return false;
        } catch (Exception ex){
            Log.e(TAG, "validateRecipeFileFormat: Failed to interpret JSON content.", ex);
            return false;
        }

        // Update the Category entry
        ArrayList<String> categories;
        if (jsonData.containsKey(CATEGORY_HEADER)) {
            categories = (ArrayList<String>) jsonData.get(CATEGORY_HEADER);
        } else {
            categories = new ArrayList<String>();
        }
        categories.remove(oldCategory);
        if (newCategory != null){
            categories.add(newCategory);
        }
        jsonData.put(CATEGORY_HEADER, categories);

        try {
            saveFileUri(getFileUri(recipe), new Gson().toJson(jsonData));
        } catch(IOException ex){
            Log.e(TAG, "removeCategoryInRecipe: Failed to save changes when updating the categories...", ex);
            return false;
        }

        return true;
    }

    public boolean updateTags(String tag, int operation, File recipeFile) {
        return updateTags(tag, operation, recipeFile.getPath());
    }

    public boolean updateTags(String tag, int operation, String recipeFile){
        // Get the category file
        File tagFile = getFile(TAG_FILE);

        // Make the tag file content into a JSON object
        HashMap<String, Object> jsonData;
        try {
            jsonData = getJsonContent(tagFile);
        } catch (Exception ex){
            Log.e(TAG, "updateTags: Failed to interpret TAG JSON content.", ex);
            return false;
        }

        // Determine if we Create or Delete
        switch(operation) {
            case (ADD_TAG):
                if (jsonData.containsKey(tag)){
                    ArrayList<String> relevantTags = (ArrayList<String>) jsonData.get(tag);
                    if (!relevantTags.contains(recipeFile)) {
                        relevantTags.add(recipeFile);
                        jsonData.put(tag, relevantTags);
                    } else {
                        // If the tag to add is already there, just exit and return a success
                        return true;
                    }
                } else {
                    jsonData.put(tag, new ArrayList<String>(Arrays.asList(recipeFile)));
                }
                break;

            case (DELETE_TAG):
                if (jsonData.containsKey(tag)){
                    ArrayList<String> relevantTags = (ArrayList<String>) jsonData.get(tag);
                    relevantTags.remove(recipeFile);
                    jsonData.put(tag, relevantTags);
                }
                break;

            default:
                Log.e(TAG, "updateTags: Unknown operation passed in.");
                return false;
        }

        // Write JSON content back to file
        try {
            saveFileUri(getFileUri(tagFile), new Gson().toJson(jsonData));
        } catch (FileNotFoundException e) {
            Log.e(TAG, "updateTags: File not found", e);
            return false;
        } catch (IOException e) {
            Log.e(TAG, "updateTags: IO Exception", e);
            return false;
        }

        return true;
    }

    public boolean writeToRecipeFile(String recipeName, ArrayList<ArrayList<String>> ingredients,
                                     String directions, Float servings, @Nullable String sourceUrl,
                                     @Nullable ArrayList<String> tags, @Nullable ArrayList<String> categories){
        // Create the HashMap that will be converted to JSON later
        HashMap<String, Object> jsonData = new HashMap<>();

        // Insert required fields
        jsonData.put(NAME_HEADER, recipeName);
        jsonData.put(INGREDIENT_HEADER, ingredients);
        jsonData.put(DIRECTIONS_HEADER, directions);
        jsonData.put(SERVINGS_HEADER, servings);

        // Insert optional fields, if available
        if (sourceUrl != null){
            jsonData.put(SOURCE_URL_HEADER, sourceUrl);
        }
        if (tags != null){
            jsonData.put(TAGS_HEADER, tags);
            // TODO : Add Entry for recipe file in TAGS file
        }
        if (sourceUrl != null){
            jsonData.put(CATEGORY_HEADER, categories);
            // TODO : Add Entry for recipe file in CATEGORY file
        }

        // Create filename from recipe name, and save the data
        // It would be nice to find something to do the filename for me, but... yeah. Effort.
        String[] invalidChars = new String[]{"<", ">" , ":" , "\"", "/", "\\", "|", "?", "*"};
        String recipeFileName = recipeName.replace(" ", "_");
        for (String invalidChar : invalidChars){
            recipeFileName = recipeFileName.replace(invalidChar, "");
        }
        try {
            saveFileUri(getFileUri(recipeFileName), new Gson().toJson(jsonData));
        } catch (IOException ex){
            Log.e(TAG, "writeToRecipeFile: Failed to save recipe " + recipeName + " (" + recipeFileName + ")", ex);
            return false;
        }

        return true;
    }

//    public boolean updateRecipeFile(String originalRecipeName, String recipeName,
//                                    ArrayList<ArrayList<String>> ingredients, String directions,
//                                    Float servings, @Nullable String sourceUrl,
//                                    @Nullable ArrayList<String> tags, @Nullable ArrayList<String> categories){
//
//    }

}
