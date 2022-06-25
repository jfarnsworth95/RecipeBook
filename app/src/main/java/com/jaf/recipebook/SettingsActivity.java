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
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    FileHelper fileHelper;
    public final String TAG = "JAF-SETTINGS";
    public final int signInCode = 101;
    GoogleSignInClient gsc;
    GoogleSignInOptions gso;
    GoogleSignInAccount gsa;

    private CircleImageView googlePhotoImg;
    private ArrayList<String> filesToImport;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileHelper = new FileHelper(this);
        setContentView(R.layout.activity_settings);

        //Initialize CircleImageView
        googlePhotoImg = findViewById(R.id.google_account_image);

        // Google Sign-in vars
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                .build();
        gsc = GoogleSignIn.getClient(this, gso);

        // Add button listener for Changing External Storage
        findViewById(R.id.toggle_external_storage_btn).setOnClickListener(view ->
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .addCategory("android.intent.category.DEFAULT")
                .setData(Uri.fromParts("package", getPackageName(), null))
        ));

        // Add button listener for Importing Downloaded recipes
        findViewById(R.id.import_downloaded_files_btn).setOnClickListener(this::onImportFilesButtonClicked);

        // Add button listener for Google Sign In
        findViewById(R.id.google_sign_in_button).setOnClickListener(view -> {
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
        // On return from permission request activity, set preference
        // based on if the permission was granted or not
        fileHelper.setPreference(fileHelper.EXTERNAL_STORAGE_PREFERENCE,
                                Environment.isExternalStorageManager());
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        gsa = GoogleSignIn.getLastSignedInAccount(this);
        updateUi();
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
            } else {
                Log.i(TAG, "updateUi: No user profile photo found.");
            }
        }
    }

    /**
     * Copies over a list of file(s) from the Downloads folder of the user's device to save in the
     * app folder.
     */
    private void importLocalFiles(ArrayList<String> filesToImport) throws IOException{
        for (String filePath : filesToImport){
            File sourceFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), filePath);
            fileHelper.copyFile(sourceFile, fileHelper.getFile(sourceFile.getName()));
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
            if (fileHelper.getFile(potentialImport.getName()).exists()){
                invalidImports.put(potentialImport, "A recipe by that name already exists.");
                continue;
            }
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
        View popupView = inflater.inflate(R.layout.import_files_popup, null);
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
            View importFileRow = inflater.inflate(R.layout.import_file_row, null);
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
        popupView.findViewById(R.id.confirm_imports_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (filesToImport.size() == 0){
                        Toast.makeText(
                                view.getContext(),
                                "No files selected to import...",
                                Toast.LENGTH_LONG
                        ).show();
                        return;
                    }
                    importLocalFiles(filesToImport);
                    Toast.makeText(
                            view.getContext(),
                            "Selected file(s) imported successfully!",
                            Toast.LENGTH_LONG
                    ).show();
                }
                catch (IOException ex){
                    Toast.makeText(
                        view.getContext(),
                        "Failed to import due to " + ex.getMessage(),
                        Toast.LENGTH_LONG
                    ).show();
                    Log.e(TAG, ex.getMessage(), ex);
                } finally {
                    popupWindow.dismiss();
                }
            }
        });
    }

}