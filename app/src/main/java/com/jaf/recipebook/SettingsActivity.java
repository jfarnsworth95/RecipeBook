package com.jaf.recipebook;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.jaf.recipebook.db.RecipeBookDatabase;
import com.jaf.recipebook.db.RecipeBookRepo;
import com.jaf.recipebook.db.directions.DirectionsModel;
import com.jaf.recipebook.db.ingredients.IngredientsModel;
import com.jaf.recipebook.db.recipes.RecipesModel;
import com.jaf.recipebook.db.tags.TagsModel;
import com.jaf.recipebook.events.DbRefreshEvent;
import com.jaf.recipebook.events.DbShutdownEvent;
import com.jaf.recipebook.events.RecipeSavedEvent;
import com.jaf.recipebook.helpers.FileHelper;
import com.jaf.recipebook.helpers.GoogleSignInHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    FileHelper fileHelper;
    public final String TAG = "JAF-SETTINGS";
    public final int signInCode = 101;
    GoogleSignInClient gsc;
    GoogleSignInOptions gso;
    GoogleSignInAccount gsa;
    private RecipeBookDatabase rbdb;
    private RecipeBookRepo rbr;

    private CircleImageView googlePhotoImg;
    private ArrayList<String> filesToImport;
    private ArrayList<String> currentRecipes;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileHelper = new FileHelper(this);
        setContentView(R.layout.activity_settings);

        //Initialize CircleImageView
        googlePhotoImg = findViewById(R.id.google_account_image);

        // Google Sign-in vars
        gsc = GoogleSignInHelper.getGoogleSignInClient(this);

        // Add button listener for Changing External Storage
        findViewById(R.id.toggle_external_storage_btn).setOnClickListener(view ->
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .addCategory("android.intent.category.DEFAULT")
                .setData(Uri.fromParts("package", getPackageName(), null))
        ));

        // Add button listener for Importing Downloaded recipes
        findViewById(R.id.import_downloaded_files_btn).setOnClickListener(this::onImportFilesButtonClicked);

        // Add button listener for moving to the Drive Settings Activity, and hide it if user isn't signed in
        findViewById(R.id.go_to_drive_settings_btn).setOnClickListener(this::onGoToDriveSettingsButtonClicked);

        // Add button listener for Google Sign In
        findViewById(R.id.google_sign_in_button).setOnClickListener(view -> {
            if (gsa == null) {
                startActivityForResult(gsc.getSignInIntent(), signInCode);
            } else {
                gsc.signOut().addOnCompleteListener(this, task -> onStart());
            }
        });
    }

    private void refreshRecipeNameList() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                List<RecipesModel> recipesModels = rbdb.recipeDao().getAllRecipes();
                currentRecipes = new ArrayList<>();
                for (RecipesModel m : recipesModels) {
                    currentRecipes.add(m.getName());
                }
            }
        }).start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // On return from permission request activity, set preference
        // based on if the permission was granted or not
        fileHelper.setPreference(fileHelper.EXTERNAL_STORAGE_PREFERENCE,
                                Environment.isExternalStorageManager());
        refreshRecipeNameList();
    }

    @Override
    protected void onStart() {
        super.onStart();
        rbdb = RecipeBookDatabase.getInstance(this);
        rbr = new RecipeBookRepo(rbdb);

        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        gsa = GoogleSignIn.getLastSignedInAccount(this);
        updateUi();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == signInCode){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                gsa = task.getResult(ApiException.class);
                updateUi();
            } catch (ApiException e) {
                Log.e(TAG, "onActivityResult: Failed to log user in.", e);
                Toast.makeText(this, "Something went wrong when signing you in...", Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Run onStart and after the activity result to check and display the google user account, if possible
     */
    private void updateUi() {
        if (gsa == null){
            ((Button) findViewById(R.id.google_sign_in_button)).setText(R.string.google_sign_in_button);
            findViewById(R.id.google_user_name).setVisibility(View.GONE);
            findViewById(R.id.sign_in_descriptor).setVisibility(View.GONE);
            findViewById(R.id.go_to_drive_settings_btn).setVisibility(View.GONE);
            googlePhotoImg.setVisibility(View.GONE);
        }else{
            ((Button) findViewById(R.id.google_sign_in_button)).setText(R.string.google_sign_out_button);
            if (gsa.getPhotoUrl() != null) {
                Log.i(TAG, "updateUi: User profile found, assigning to Image View.");
                Glide.with(SettingsActivity.this).load(gsa.getPhotoUrl()).into(googlePhotoImg);
                googlePhotoImg.setVisibility(View.VISIBLE);
                TextView userDisplayName = findViewById(R.id.google_user_name);
                userDisplayName.setText(gsa.getDisplayName());
                userDisplayName.setVisibility(View.VISIBLE);
                findViewById(R.id.sign_in_descriptor).setVisibility(View.VISIBLE);
                findViewById(R.id.go_to_drive_settings_btn).setVisibility(View.VISIBLE);
            } else {
                Log.i(TAG, "updateUi: No user profile photo found.");
            }
        }
    }

    /**
     * Interprets files from the Downloads folder of the user's device to JSON data, then saves
     * that to the database.
     */
    private void importLocalFiles() throws IOException{
        for (String filePath : filesToImport){
            File recipeFile = new File(fileHelper.getDownloadsDir().getPath() + "/" + filePath);
            HashMap<String, Object> jsonData = fileHelper.returnGsonFromFile(recipeFile);

            // Create Recipe Model
            Float servings = null;
            if (jsonData.containsKey("SERVINGS")){
                servings = ((Double) jsonData.get("SERVINGS")).floatValue();
            }
            String category = null;
            if (jsonData.containsKey("CATEGORY")){
                category = (String) jsonData.get("CATEGORY");
            }
            String sourceUrl = null;
            if (jsonData.containsKey("SOURCE_URL")){
                sourceUrl = (String) jsonData.get("SOURCE_URL");
            }
            RecipesModel rm = new RecipesModel(
                    (String) jsonData.get("NAME"),
                    category,
                    servings,
                    sourceUrl
            );

            // Create Ingredient Model
            ArrayList<IngredientsModel> ingredients = new ArrayList<>();
            int i = 0;
            for (String ingredientEntry : (ArrayList<String>) jsonData.get("INGREDIENTS")) {
                ingredients.add(new IngredientsModel(rm.getId(), i, ingredientEntry));
                i++;
            }

            // Create Directions Model
            DirectionsModel dm = new DirectionsModel(rm.getId(), (String) jsonData.get("DIRECTIONS"));

            // Optionally create Tag Models (if present)
            ArrayList<TagsModel> tags = new ArrayList<>();
            if (jsonData.containsKey("TAGS")){
                i = 0;
                for (String tag : (ArrayList<String>) jsonData.get("TAGS")){
                    tags.add(new TagsModel(rm.getId(), tag.toLowerCase()));
                }
            }

            rbr.insertRecipe(rm, ingredients, tags, dm);
        }
    }

    /**
     * Returns the list of Recipe Book files available for import.
     * @return List of files that can be imported into the application
     */
    private ArrayList<File> returnAvailableImports(){
        File[] downloadFiles = fileHelper.getDownloadsFolderFiles();
        ArrayList<File> availableImports = new ArrayList<>();
        for (File downloadFile : downloadFiles){
            // Check if the file has an extension, and if that extension is the app recipe extension
            int index = downloadFile.getName().lastIndexOf('.');
            if (index > 0 && downloadFile.getName().substring(index + 1).equals(this.getString(R.string.recipe_file_extension))) {
                availableImports.add(downloadFile);
            }
        }
        return availableImports;
    }

    /**
     * Returns the list of Recipe Book files that cannot be imported, and the reason why.
     * @return HashMap with the filename as key, and reason as the value.
     */
    private HashMap<File, String> returnInvalidImports(ArrayList<File> availableImports){
        HashMap<File, String> invalidImports = new HashMap<>();
        for (File potentialImport : availableImports){
            if (!fileHelper.validateRecipeFileFormat(potentialImport)){
                invalidImports.put(potentialImport, "The file structure is corrupted, and can't be imported.");
            }
        }

        return invalidImports;
    }

    /**
     * Creates the popup to import files. After inflating the popup view, it will find all files with
     *  the .rp extension, then determine if any are corrupted, or already exist in the app.
     * @param view Button trigger view
     */
    private void onImportFilesButtonClicked(View view){
        // Create empty list of file strings to be used when importing
        filesToImport = new ArrayList<>();

        // Populate the popup with the importable files
        ArrayList<File> imports = returnAvailableImports();
        HashMap<File, String> invalidImports = returnInvalidImports(imports);
        Collections.sort(imports); // Sort for display purposes

        // Stop if there are no possible files to import
        if (imports.size() == 0){
            Toast.makeText(
                    view.getContext(),
                    "No recipe files (.rp) found to import...",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_import_files, null);
        popupView.setBackground(this.getDrawable(android.R.drawable.picture_frame));

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
        popupWindow.setElevation(10); // Adds shadow to popup

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window tolken
        popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);

        // dismiss the popup window when touched
        popupView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                popupWindow.dismiss();
                return true;
            }
        });

        for (File importableFile : imports){
            View importFileRow = inflater.inflate(R.layout.fragment_import_file_row, null);
            Button tooltipBtn = importFileRow.findViewById(R.id.import_error_tooltip_btn);

            if (invalidImports.containsKey(importableFile)){
                // If invalid, show button that spawns tooltip with explanation as to why,
                // disable the checkbox, and strikeout it's text
                tooltipBtn.setTooltipText(invalidImports.get(importableFile));
                importFileRow.findViewById(R.id.shouldImportCheckbox).setEnabled(false);
                ((TextView)importFileRow.findViewById(R.id.importFilenameText)).setPaintFlags(Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                // If valid, remove the button that would show the error tooltip, add checkbox listener
                tooltipBtn.setVisibility(View.INVISIBLE);
                ((CheckBox)importFileRow.findViewById(R.id.shouldImportCheckbox))
                        .setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                        String filename = ((TextView)((TableRow)compoundButton.getParent())
                                .findViewById(R.id.importFilenameText)).getText().toString();
                        if(b){
                            filesToImport.add(filename);
                        } else {
                            filesToImport.remove(filename);
                        }
                    }
                });
            }
            // Add the filename, then add this inflated view to the popup scroll view
            ((TextView)importFileRow.findViewById(R.id.importFilenameText)).setText(importableFile.getName());
            ((LinearLayout)popupView.findViewById(R.id.import_popup_table_view)).addView(importFileRow);
        }

        // Make the progress spinner disappear, and set the scroll view & confirm button to visible
        popupView.findViewById(R.id.import_popup_scrollview).setVisibility(View.VISIBLE);

        // Add onclick listener for the confirm imports button
        popupView.findViewById(R.id.confirm_imports_btn).setOnClickListener(v -> {
            ((Button) v).setEnabled(false);
            popupView.findViewById(R.id.import_all_btn).setEnabled(false);
            try {
                if (filesToImport.size() == 0){
                    Toast.makeText(
                            v.getContext(),
                            "No files selected to import...",
                            Toast.LENGTH_LONG
                    ).show();
                    return;
                }
                importLocalFiles();
                Toast.makeText(
                        v.getContext(),
                        "Selected file(s) imported successfully!",
                        Toast.LENGTH_LONG
                ).show();
            }
            catch (IOException ex){
                Toast.makeText(
                        v.getContext(),
                    "Failed to import due to " + ex.getMessage(),
                    Toast.LENGTH_LONG
                ).show();
                Log.e(TAG, ex.getMessage(), ex);
            } finally {
                popupWindow.dismiss();
            }
        });

        popupView.findViewById(R.id.import_all_btn).setOnClickListener(v -> {
            ((Button) v).setEnabled(false);
            popupView.findViewById(R.id.confirm_imports_btn).setEnabled(false);
            try {
                HashSet<File> importFiles = new HashSet<>(imports);
                importFiles.removeAll(invalidImports.keySet());
                filesToImport = new ArrayList<>();
                for (File importFile : importFiles) {
                    filesToImport.add(importFile.getName());
                }
                importLocalFiles();
                Toast.makeText(
                        v.getContext(),
                        "Imported all files successfully!",
                        Toast.LENGTH_LONG
                ).show();
            } catch (IOException ex){
                Log.e(TAG, "Failed to import all files", ex);
            } finally {
                popupWindow.dismiss();
            }
        });
    }

    private void onGoToDriveSettingsButtonClicked(View view){
        startActivity(new Intent(this, DriveSettingsActivity.class));
    }

    @Subscribe
    public void onDbRefresh(DbRefreshEvent dbRefreshEvent){
        rbdb = RecipeBookDatabase.getInstance(this);
        rbr = new RecipeBookRepo(rbdb);
    }

    @Subscribe
    public void onDbShutdown(DbShutdownEvent dbShutdownEvent){
        rbdb.close();
        rbr = null;
    }

    @Subscribe
    public void onRecipeAdded(RecipeSavedEvent recipeSavedEvent){
        refreshRecipeNameList();
    }
}