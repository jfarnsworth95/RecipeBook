package com.jaf.recipebook;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import com.jaf.recipebook.db.RecipeBookDatabase;
import com.jaf.recipebook.db.recipes.RecipesModel;
import com.jaf.recipebook.helpers.FileHelper;
import com.jaf.recipebook.helpers.GeneralHelper;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    FileHelper fileHelper;
    public final String TAG = "JAF-MAIN";

    private RecipeBookDatabase rbd;

    public Button btnAddRecipe;
    public Button btnEditLastRecipe;
    public Button btnViewLastRecipe;

    private long lastRecipeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate...");
        super.onCreate(savedInstanceState);

        try {
            Thread.sleep(2000);
        }catch (InterruptedException ex){
            Log.d("SplashRender", "onCreate: Failed to sleep...");
        }

        SplashScreen.installSplashScreen(this);
        fileHelper = new FileHelper(this);

        ActionBar actionBar = getActionBar();
        setContentView(R.layout.activity_start);

        ActivityResultLauncher<Intent> addEditActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), o -> {
                if (o.getResultCode() == Activity.RESULT_OK){
                    Toast.makeText(this, getString(R.string.recipe_saved), Toast.LENGTH_SHORT).show();
                } else if (o.getResultCode() == GeneralHelper.ACTIVITY_RESULT_DB_ERROR) {
                    Toast.makeText(this, getString(R.string.failed_to_open_recipe), Toast.LENGTH_LONG).show();
                }
            }
        );

        btnAddRecipe = findViewById(R.id.btnMainTest);
        btnAddRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditRecipeActivity.class);
            addEditActivityResultLauncher.launch(intent);
        });

        btnEditLastRecipe = findViewById(R.id.btnEditLastRecipe);
        btnEditLastRecipe.setEnabled(false);
        btnEditLastRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditRecipeActivity.class);
            intent.putExtra("recipe_id", lastRecipeId);
            addEditActivityResultLauncher.launch(intent);
        });

        btnViewLastRecipe = findViewById(R.id.btnViewLastRecipe);
        btnViewLastRecipe.setEnabled(false);
        btnViewLastRecipe.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditRecipeActivity.class);
            intent.putExtra("recipe_id", lastRecipeId);
            addEditActivityResultLauncher.launch(intent);
        });

        rbd = RecipeBookDatabase.getInstance(this);
        TEST_METHOD_REMOVE_LATER();
    }

    @Override
    protected void onResume(){
        Log.i(TAG, "onResume...");
        super.onResume();

        // If MANAGE_EXTERNAL_STORAGE permission not granted and the Shared Preference for using
        // external storage is true (or doesn't exist yet), open dialog requesting the permission
        validateExternalPermission();

        // Check if user wants to connect to their Google Drive to backup the Recipe Files
        // TODO: Add method for adding Google Drive connection

        TEST_METHOD_REMOVE_LATER();
    }

    public void validateExternalPermission(){
        if (fileHelper.getPreference(fileHelper.EXTERNAL_STORAGE_PREFERENCE, true)
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
                        fileHelper.setPreference(fileHelper.EXTERNAL_STORAGE_PREFERENCE, true);
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(this.getString(R.string.dialog_deny), (dialogInterface, i) -> {
                                fileHelper.setPreference(fileHelper.EXTERNAL_STORAGE_PREFERENCE, true);
                    })
                    .show();
        } else if (Environment.isExternalStorageManager() && !fileHelper.getPreference(fileHelper.EXTERNAL_STORAGE_PREFERENCE, false)){
            fileHelper.setPreference(fileHelper.EXTERNAL_STORAGE_PREFERENCE, true);
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

    public void TEST_METHOD_REMOVE_LATER(){
        rbd.getQueryExecutor().execute(() -> {
            ArrayList<RecipesModel> rms = new ArrayList<>(rbd.recipeDao().getAllRecipes());
            Log.i(TAG, "QUERY RESULT SIZE: " + rms.size());
            if (rms.size() > 0) {
                lastRecipeId = rms.get(rms.size() - 1).getId();
                runOnUiThread(() -> {
                    btnViewLastRecipe.setEnabled(true);
                    btnEditLastRecipe.setEnabled(true);
                });
            }
        });
    }

}