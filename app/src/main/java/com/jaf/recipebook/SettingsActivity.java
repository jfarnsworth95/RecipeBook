package com.jaf.recipebook;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import android.app.UiModeManager;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.jaf.recipebook.db.RecipeBookDatabase;
import com.jaf.recipebook.db.RecipeBookRepo;
import com.jaf.recipebook.db.directions.DirectionsModel;
import com.jaf.recipebook.db.ingredients.IngredientsModel;
import com.jaf.recipebook.db.recipes.RecipesModel;
import com.jaf.recipebook.db.tags.TagsModel;
import com.jaf.recipebook.events.DbCheckpointCreated;
import com.jaf.recipebook.events.DbRefreshEvent;
import com.jaf.recipebook.events.DbShutdownEvent;
import com.jaf.recipebook.events.DriveUploadCompeleteEvent;
import com.jaf.recipebook.events.RecipeSavedEvent;
import com.jaf.recipebook.helpers.DriveServiceHelper;
import com.jaf.recipebook.helpers.FileHelper;
import com.jaf.recipebook.helpers.GeneralHelper;
import com.jaf.recipebook.helpers.GoogleSignInHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    FileHelper fh;
    public final String TAG = "JAF-SETTINGS";
    public final int signInCode = 101;
    public final int IMPORT_ALL = 1000;
    public final int IMPORT_SELECTED = 1001;
    GoogleSignInClient gsc;
    GoogleSignInAccount gsa;
    private DriveServiceHelper dsh;
    private RecipeBookDatabase rbdb;
    private RecipeBookRepo rbr;
    private Handler mainHandler;

    private boolean flashSignIn;

    private CircleImageView googlePhotoImg;
    private HashSet<Uri> filesToImport;
    private HashSet<Uri> duplicateImports;
    private HashMap<Uri, String> invalidImports;
    private HashSet<Uri> validImports;
    private HashSet<UUID> currentRecipeUuids;
    private HashSet<String> categories;

    private AlertDialog loadingIndicator;
    private Button changeDisplayModeBtn;
    private Button importDownloadsBtn;
    private Button changeCategoryOrderBtn;
    private Button goToDriveSettingsBtn;
    private Button googleSignInBtn;

    ActivityResultLauncher<Intent> downloadToLocal;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fh = new FileHelper(this);
        dsh = GoogleSignInHelper.getDriveServiceHelper(this, false);
        flashSignIn = getIntent().getBooleanExtra("flashSignIn", false);
        setContentView(R.layout.activity_settings);
        mainHandler = new Handler(getMainLooper());

        getClassVars();

        loadingIndicator = new AlertDialog.Builder(this)
                .setView(getLayoutInflater().inflate(R.layout.popup_is_loading, null))
                .create();

        // Google Sign-in vars
        gsc = GoogleSignInHelper.getGoogleSignInClient(this);

        downloadToLocal = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), o -> {
                loadingIndicator.hide();
                if (o.getData() != null) {
                    List<Uri> uriList = new ArrayList<>();
                    if (o.getData().getClipData() != null) {
                        int count = o.getData().getClipData().getItemCount();
                        for (int i = 0; i < count; i++) {
                            Uri uri = o.getData().getClipData().getItemAt(i).getUri();
                            uriList.add(uri);
                        }
                    } else if (o.getData().getData() != null) {
                        Uri uri = o.getData().getData();
                        uriList.add(uri);
                    }
                    onImportFilesButtonClicked(uriList);
                } else if (o.getResultCode() == RESULT_CANCELED) {
                    Toast.makeText(this, getString(R.string.cancel_import), Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, getString(R.string.unexpected_error_importing), Toast.LENGTH_LONG).show();
                }
            }
        );

        importDownloadsBtn.setOnClickListener(v -> {
            loadingIndicator.show();
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            downloadToLocal.launch(intent);
        });

        changeDisplayModeBtn.setOnClickListener(v -> {
            UiModeManager umm = (UiModeManager) getSystemService(UI_MODE_SERVICE);
            int displayModeIndex = fh.getPreference(fh.DISPLAY_MODE_PREFERENCE, fh.DISPLAY_MODE_OS);

            new AlertDialog.Builder(this)
                .setTitle(getString(R.string.display_mode_popup_title))
                .setSingleChoiceItems(R.array.display_mode_options, displayModeIndex, (dialog, which) -> { })
                .setNegativeButton(R.string.negative_text, (dialog, which) -> { })
                .setPositiveButton(R.string.affirmative_text, (dialog, which) -> {
                    int setDisplayMode =((AlertDialog)dialog).getListView().getCheckedItemPosition();
                    fh.setPreference(fh.DISPLAY_MODE_PREFERENCE, setDisplayMode);
                    umm.setApplicationNightMode(setDisplayMode);
                })
                .create().show();
        });

        // Add button listener for moving to the Drive Settings Activity, and hide it if user isn't signed in
        goToDriveSettingsBtn.setOnClickListener(this::onGoToDriveSettingsButtonClicked);

        // Add button listener for Google Sign In
        googleSignInBtn.setOnClickListener(view -> {
            if (gsa == null) {
                startActivityForResult(gsc.getSignInIntent(), signInCode);
            } else {
                gsc.signOut().addOnCompleteListener(this, task -> onStart());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshRecipeNameList();
        getCategories();
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

    private void getClassVars() {

        //Initialize CircleImageView
        googlePhotoImg = findViewById(R.id.google_account_image);

        changeDisplayModeBtn = findViewById(R.id.change_display_mode);
        importDownloadsBtn = findViewById(R.id.import_downloaded_files_btn);
        changeCategoryOrderBtn = findViewById(R.id.change_category_order_btn);
        goToDriveSettingsBtn = findViewById(R.id.go_to_drive_settings_btn);
        googleSignInBtn = findViewById(R.id.google_sign_in_button);
    }

    private void refreshRecipeNameList() {
        new Thread(() -> {
            List<RecipesModel> recipesModels = rbdb.recipeDao().getAllRecipes();
            currentRecipeUuids = new HashSet<>();
            for (RecipesModel m : recipesModels) {
                currentRecipeUuids.add(m.getUuid());
            }
        }).start();
    }

    private void getCategories() {
        rbdb.getQueryExecutor().execute(() -> {
            categories = new HashSet<>(rbdb.recipeDao().getDistinctCategories());
            mainHandler.post(this::adjustCategoryBtn);
        });
    }

    private void adjustCategoryBtn() {
        if (categories.size() > 1) {
            changeCategoryOrderBtn.setTooltipText("");
            changeCategoryOrderBtn.setOnClickListener(v -> {
                startActivity(new Intent(this, ReorderCategoryActivity.class));
            });
            changeCategoryOrderBtn.setEnabled(true);
            changeCategoryOrderBtn.setBackgroundColor(getColor(R.color.secondary));
        }
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
                Toast.makeText(this, getString(R.string.sign_in_failed), Toast.LENGTH_LONG).show();
            }
        }
    }

    /**
     * Run onStart and after the activity result to check and display the google user account, if possible
     */
    private void updateUi() {
        if (gsa == null){
            googleSignInBtn.setText(R.string.google_sign_in_button);
            findViewById(R.id.google_user_name).setVisibility(View.GONE);
            findViewById(R.id.sign_in_descriptor).setVisibility(View.GONE);
            findViewById(R.id.go_to_drive_settings_btn).setVisibility(View.GONE);
            googlePhotoImg.setVisibility(View.GONE);

            if (flashSignIn) {
                googleSignInBtn.startAnimation(GeneralHelper.createFlashAnimation(2));
            }
        }else{
            if (flashSignIn) {
                setResult(GeneralHelper.ACTIVITY_RESULT_SIGN_IN_PROMPT);
            }
            googleSignInBtn.setText(R.string.google_sign_out_button);
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
    private void importLocalFiles(boolean shouldOverwriteDuplicates) throws IOException{
        Iterator<Uri> iterator = filesToImport.iterator();
        while (iterator.hasNext()){
            Uri recipeFile = iterator.next();
            HashMap<String, Object> jsonData = fh.returnGsonFromFile(recipeFile);

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
                    sourceUrl,
                    UUID.fromString((String) jsonData.get("UUID"))
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
                for (String tag : (ArrayList<String>) jsonData.get("TAGS")){
                    tags.add(new TagsModel(rm.getId(), tag.toLowerCase()));
                }
            }

            if (duplicateImports.contains(recipeFile)){
                if (shouldOverwriteDuplicates){ // Either we overwrite, or ignore entirely
                    rbr.updateRecipe(rm, ingredients, tags, dm, !iterator.hasNext());
                }
            } else {
                rbr.insertRecipe(rm, ingredients, tags, dm, !iterator.hasNext());
            }
        }

    }

    /**
     * Returns the list of Recipe Book files available for import.
     * @return List of files that can be imported into the application
     */
    private ArrayList<Uri> returnAvailableImports(List<Uri> uriList){
        ArrayList<Uri> availableImports = new ArrayList<>();
        for (Uri downloadUri : uriList){
            DocumentFile df = DocumentFile.fromSingleUri(this, downloadUri);
            if (df == null) {
                continue;
            }
            String fileName = df.getName();

            // Check if the file has an extension, and if that extension is the app recipe extension
            int index = fileName.lastIndexOf('.');
            if (index > 0 && fileName.substring(index + 1).equals(this.getString(R.string.recipe_file_extension))) {
                availableImports.add(downloadUri);
            }
        }
        return availableImports;
    }

    /**
     * Returns the list of Recipe Book files that cannot be imported, and the reason why.
     */
    private void validateImportFiles(ArrayList<Uri> availableImports){
        invalidImports = new HashMap<>();
        duplicateImports = new HashSet<>();
        HashSet<UUID> scannedUuids = new HashSet<>();
        for (Uri potentialImport : availableImports){
            if (!fh.validateRecipeFileFormat(potentialImport, scannedUuids)){
                invalidImports.put(potentialImport, "The file structure is corrupted, or this unique id was found in another file.");
            } else if (currentRecipeUuids.contains(fh.getImportFileUuid(potentialImport))){
                duplicateImports.add(potentialImport);
            }
            scannedUuids.add(fh.getImportFileUuid(potentialImport));
        }

        validImports = new HashSet<>(availableImports);
        validImports.removeAll(invalidImports.keySet());
    }

    /**
     * Creates the popup to import files. After inflating the popup view, it will find all files with
     *  the .rp extension, then determine if any are corrupted, or already exist in the app.
     */
    private void onImportFilesButtonClicked(List<Uri> uriList){
        loadingIndicator.show();
        new Thread(() -> {
            // Create empty list of file strings to be used when importing
            filesToImport = new HashSet<>();

            // Populate the popup with the importable files
            ArrayList<Uri> imports = returnAvailableImports(uriList);
            validateImportFiles(imports);
            Collections.sort(imports); // Sort for display purposes

            // Stop if there are no possible files to import
            if (imports.isEmpty()){
                runOnUiThread(() -> {
                    loadingIndicator.hide();
                    Toast.makeText(
                            this,
                            getString(R.string.no_rp_files_found),
                            Toast.LENGTH_LONG
                    ).show();
                });
            } else {
                createPopupWindow(imports);
            }
        }).start();
    }

    private void createPopupWindow(ArrayList<Uri> imports){
        // inflate the layout of the popup window
        LayoutInflater inflater = (LayoutInflater)
                getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(R.layout.popup_import_files, null);

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);
        popupWindow.setElevation(10); // Adds shadow to popup

        runOnUiThread(() -> {
            // show the popup window
            // which view you pass in doesn't matter, it is only used for the window token
            popupWindow.showAtLocation(importDownloadsBtn, Gravity.CENTER, 0, 0);

            // dismiss the popup window when touched
            popupView.setOnTouchListener((popupV, event) -> {
                popupWindow.dismiss();
                return true;
            });

            for (Uri importableFile : imports){
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
                            .setOnCheckedChangeListener((compoundButton, b) -> {
                                String filename = ((TextView)((TableRow)compoundButton.getParent())
                                        .findViewById(R.id.importFilenameText)).getText().toString();
                                if(b){
                                    filesToImport.add(importableFile);
                                } else {
                                    filesToImport.remove(importableFile);
                                }
                            });
                }

                // Add the filename, then add this inflated view to the popup scroll view
                String importableFileName = Objects.requireNonNull(DocumentFile.fromSingleUri(this, importableFile)).getName();
                ((TextView)importFileRow.findViewById(R.id.importFilenameText)).setText(importableFileName);
                ((LinearLayout)popupView.findViewById(R.id.import_popup_table_view)).addView(importFileRow);
            }

            // Make the progress spinner disappear, and set the scroll view & confirm button to visible
            popupView.findViewById(R.id.import_popup_scrollview).setVisibility(View.VISIBLE);

            // Add onclick listener for the confirm imports button
            popupView.findViewById(R.id.confirm_imports_btn).setOnClickListener(pv -> {
                pv.setEnabled(false);
                popupView.findViewById(R.id.import_all_btn).setEnabled(false);

                if (Collections.disjoint(filesToImport, duplicateImports)){ // if true, no common elements
                    importCheckedFiles(false, pv, popupWindow);
                } else {
                    askUserHowToHandleDuplicates(IMPORT_SELECTED, pv, popupWindow);
                }

            });

            popupView.findViewById(R.id.import_all_btn).setOnClickListener(pv -> {
                pv.setEnabled(false);
                popupView.findViewById(R.id.confirm_imports_btn).setEnabled(false);

                if (Collections.disjoint(validImports, duplicateImports)){
                    importAllFiles(false, pv, popupWindow);
                } else {
                    askUserHowToHandleDuplicates(IMPORT_ALL, pv, popupWindow);
                }
            });
            loadingIndicator.hide();
        });

    }

    private void importCheckedFiles(boolean shouldOverwrite, View v, PopupWindow popupWindow){
        try {
            if (filesToImport.isEmpty()){
                Toast.makeText(
                        v.getContext(),
                        getString(R.string.no_files_found_for_import),
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            if (duplicateImports.containsAll(filesToImport) && !shouldOverwrite){
                Toast.makeText(v.getContext(), getString(R.string.only_dups), Toast.LENGTH_LONG)
                        .show();
                return;
            }

            importLocalFiles(shouldOverwrite);
            Toast.makeText(
                    v.getContext(),
                    getString(R.string.import_success),
                    Toast.LENGTH_LONG
            ).show();
        }
        catch (IOException ex){
            Log.e(TAG, ex.getMessage(), ex);
            Toast.makeText(
                    v.getContext(),
                    "Failed to import due to " + ex.getMessage(),
                    Toast.LENGTH_LONG
            ).show();
        } finally {
            getCategories();
            popupWindow.dismiss();
        }
    }

    private void importAllFiles(boolean shouldOverwrite, View v, PopupWindow popupWindow) {

        try {
            filesToImport = new HashSet<>();
            filesToImport.addAll(validImports);

            if (validImports.equals(duplicateImports) && !shouldOverwrite){
                Toast.makeText(v.getContext(), getString(R.string.only_dups), Toast.LENGTH_LONG)
                        .show();
                return;
            }

            importLocalFiles(shouldOverwrite);
            Toast.makeText(v.getContext(), getString(R.string.import_success), Toast.LENGTH_LONG)
                    .show();
        } catch (IOException ex) {
            Toast.makeText(v.getContext(), "Failed to import due to " + ex.getMessage(), Toast.LENGTH_LONG)
                    .show();
            Log.e(TAG, ex.getMessage(), ex);
        } finally {
            getCategories();
            popupWindow.dismiss();
        }
    }

    private void onGoToDriveSettingsButtonClicked(View view){
        startActivity(new Intent(this, DriveSettingsActivity.class));
    }

    private void askUserHowToHandleDuplicates(int importOption, View v, PopupWindow popupWindow){
        AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext());
        builder.setMessage(R.string.duplicate_warning_message)
                .setPositiveButton(R.string.confirm_overwrite, (dialog, which) -> {
                    if (importOption == IMPORT_ALL) {
                        importAllFiles(true, v, popupWindow);
                    } else {
                        importCheckedFiles(true, v, popupWindow);
                    }
                })
                .setNegativeButton(R.string.ignore_duplicates, (dialog, which) -> {
                    if (importOption == IMPORT_ALL) {
                        importAllFiles(false, v, popupWindow);
                    } else {
                        importCheckedFiles(false, v, popupWindow);
                    }
                });
        builder.create().show();
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
    public void onRecipeSaved(RecipeSavedEvent recipeSavedEvent){
        Log.i(TAG, "onRecipeSaved called from Settings");
        refreshRecipeNameList();
        if (recipeSavedEvent.recipeSaved) {
            if (dsh != null && fh.getPreference(fh.AUTO_BACKUP_ACTIVE_PREFERENCE, false)) {
                Log.i(TAG, "Backing up from Settings");
                rbr.createCheckpoint();
            }
        } else {
            Log.e(TAG, "One or more recipe imports failed in Settings");
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void checkpointAttempted(DbCheckpointCreated dbCheckpointCreated) {
        if (dbCheckpointCreated.success){
            dsh.upload();
        } else {
            Toast.makeText(this, getString(R.string.save_failed), Toast.LENGTH_LONG).show();
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void uploadAttempted(DriveUploadCompeleteEvent duce) {
        if(duce.done){
            Log.i(TAG, "Successful upload");
        } else {
            Log.e(TAG, "Failed to upload");
            Toast.makeText(this, getString(R.string.upload_failed), Toast.LENGTH_SHORT).show();
        }
    }
}