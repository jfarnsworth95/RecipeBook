package com.jaf.recipebook;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.jaf.recipebook.adapters.RecipeViewAdapter;
import com.jaf.recipebook.db.RecipeBookDao;
import com.jaf.recipebook.db.RecipeBookDatabase;
import com.jaf.recipebook.fragments.IsLoading;
import com.jaf.recipebook.fragments.ListRecipes;
import com.jaf.recipebook.fragments.NoSavedRecipes;
import com.jaf.recipebook.fragments.SearchReturnsEmpty;
import com.jaf.recipebook.helpers.FileHelper;
import com.jaf.recipebook.helpers.GeneralHelper;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    FileHelper fileHelper;
    public final String TAG = "JAF-MAIN";

    private final int FRAGMENT_LOADING = 11;
    private final int FRAGMENT_LIST = 12;
    private final int FRAGMENT_NO_SAVED_RECIPES = 13;
    private final int FRAGMENT_SEARCH_RETURNED_EMPTY = 14;

    private RecipeBookDatabase rbd;

    private Fragment currentFrag = null;
    private Handler mainHandler;
    private MutableLiveData<List<RecipeBookDao.BasicRecipeTuple>> recipesToRender;
    private Runnable workRunnableSearch = null;

    ActivityResultLauncher<Intent> addEditActivityResultLauncher;
    ActivityResultLauncher<Intent> settingsActivityResultLauncher;
    ActivityResultLauncher<Intent> viewActivityResultLauncher;

    Button addFirstRecipeBtn;
    CheckBox titleCB;
    CheckBox tagsCB;
    CheckBox ingredientCB;
    CheckBox directionsCB;
    CheckBox categoryCB;
    CheckBox sourceCB;
    ConstraintLayout searchBar;
    EditText searchBarEditText;
    FloatingActionButton addRecipeFab;
    Fragment isLoadingFrag;
    Fragment listRecipesFrag;
    Fragment noSavedRecipesFrag;
    Fragment searchReturnsEmptyFrag;
    ImageButton expandSearchOptionsBtn;
    RecyclerView mainRecyclerView;
    TableLayout searchBarOptionsContainer;


    // TODO Directions not saving properly on Mobile Test
    // TODO Scroll on recycler view lets user keep going
    // TODO Dark mode is fucked on Mobile test
    // TODO Why is the checkbox acting weird? Layout inspector says its still material checkbox
    // TODO Bulk delete from main view?
    // TODO Don't reload when returning to main view if no edits/additions happened
    // TODO Marquee for View menu header

    // TODO Add all recipes from downloads button & all recipes without same name
    // TODO Try increasing row height and text size, it's a tad small atm

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Temp fix while testing locally
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        super.onCreate(savedInstanceState);

        SplashScreen.installSplashScreen(this);
        setContentView(R.layout.activity_start);
        setupComponentHandlers();
        setClassVars();
        setupListeners();

        swapFragments(FRAGMENT_LOADING);
    }

    @Override
    protected void onResume(){
        super.onResume();

        // If MANAGE_EXTERNAL_STORAGE permission not granted and the Shared Preference for using
        // external storage is true (or doesn't exist yet), open dialog requesting the permission
        validateExternalPermission();

        // Check if user wants to connect to their Google Drive to backup the Recipe Files
        // TODO: Add method for adding Google Drive connection

        queryForRecipes();
        if (!searchBarEditText.getText().toString().isEmpty()) {
            searchBar.setVisibility(View.VISIBLE);
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
                settingsActivityResultLauncher
                        .launch(new Intent(this, SettingsActivity.class));
                return true;

            case R.id.search_btn:
                // Open Search Bar
                toggleSearchBarVisible();
                return true;

            default:
                Log.w(TAG, "onOptionsItemSelected: Unknown Item ID for selected item: "
                        + item.toString());
                return super.onOptionsItemSelected(item);
        }
    }

    private void setClassVars(){
        mainHandler = new Handler(getMainLooper());
        fileHelper = new FileHelper(this);

        addEditActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), o -> {
                if (o.getResultCode() == Activity.RESULT_OK){
                    Toast.makeText(this, getString(R.string.recipe_saved), Toast.LENGTH_SHORT).show();
                } else if (o.getResultCode() == GeneralHelper.ACTIVITY_RESULT_DB_ERROR) {
                    Toast.makeText(this, getString(R.string.failed_to_open_recipe), Toast.LENGTH_LONG).show();
                }
            }
        );

        settingsActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {});

        viewActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), o -> {
                if (o.getResultCode() == GeneralHelper.ACTIVITY_RESULT_DB_ERROR) {
                    Toast.makeText(this, getString(R.string.failed_to_open_recipe), Toast.LENGTH_LONG).show();
                }
            }
        );

        recipesToRender = new MutableLiveData<>(new ArrayList<>());
        rbd = RecipeBookDatabase.getInstance(this);

        isLoadingFrag = new IsLoading();
        listRecipesFrag = new ListRecipes();
        noSavedRecipesFrag = new NoSavedRecipes();
        searchReturnsEmptyFrag = new SearchReturnsEmpty();
    }

    private void setupComponentHandlers(){
        searchBar = findViewById(R.id.main_search_bar);
        searchBarEditText = findViewById(R.id.searchbar_edit_text);
        searchBarOptionsContainer = findViewById(R.id.searchbar_options_container);
        expandSearchOptionsBtn = findViewById(R.id.toggle_search_options_btn);

        titleCB = findViewById(R.id.search_title_checkbox);
        tagsCB = findViewById(R.id.search_tags_checkbox);
        ingredientCB = findViewById(R.id.search_ingredients_checkbox);
        directionsCB = findViewById(R.id.search_directions_checkbox);
        categoryCB = findViewById(R.id.search_categories_checkbox);
        sourceCB = findViewById(R.id.search_source_checkbox);
    }

    private void setupListeners(){
        expandSearchOptionsBtn.setOnClickListener(v -> toggleSearchOptionsVisible());

        titleCB.setOnClickListener(v -> searchCheckboxListener((CheckBox) v));
        tagsCB.setOnClickListener(v -> searchCheckboxListener((CheckBox) v));
        ingredientCB.setOnClickListener(v -> searchCheckboxListener((CheckBox) v));
        directionsCB.setOnClickListener(v -> searchCheckboxListener((CheckBox) v));
        categoryCB.setOnClickListener(v -> searchCheckboxListener((CheckBox) v));
        sourceCB.setOnClickListener(v -> searchCheckboxListener((CheckBox) v));

        searchBarEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                if (workRunnableSearch != null) {
                    mainHandler.removeCallbacks(workRunnableSearch);
                }
                workRunnableSearch = () -> {
                    queryForRecipes();
                };
                mainHandler.postDelayed(workRunnableSearch, 1000);
            }
        });
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

    public void queryForRecipes(){
        String searchQuery = searchBarEditText.getText().toString();
        rbd.getQueryExecutor().execute(() -> {
            ArrayList<RecipeBookDao.BasicRecipeTuple> recipes;
            if (searchQuery.isEmpty()) {
                recipes = new ArrayList<>(rbd.recipeBookDao().getAllRecipes());
                recipesToRender.postValue(recipes);
            } else {
                recipes = new ArrayList<>(rbd.recipeBookDao().searchAllForParameter(
                        (titleCB.isChecked() ? searchQuery : null),
                        (categoryCB.isChecked() ? searchQuery : null),
                        (sourceCB.isChecked() ? searchQuery : null),
                        (ingredientCB.isChecked() ? searchQuery : null),
                        (directionsCB.isChecked() ? searchQuery : null),
                        (tagsCB.isChecked() ? searchQuery : null)
                ));
                recipesToRender.postValue(recipes);
            }

            if (recipes.isEmpty() && searchQuery.isEmpty()){
                swapFragments(FRAGMENT_NO_SAVED_RECIPES);
            } else if (recipes.isEmpty()){
                swapFragments(FRAGMENT_SEARCH_RETURNED_EMPTY);
            } else {
                swapFragments(FRAGMENT_LIST);
            }
        });
    }

    private void searchCheckboxListener(CheckBox checkBoxClicked){
        if (!titleCB.isChecked() && !tagsCB.isChecked() &&
                !ingredientCB.isChecked() && !directionsCB.isChecked() &&
                !categoryCB.isChecked() && !sourceCB.isChecked()){
            checkBoxClicked.setChecked(true);
            Toast.makeText(this, "We can't search nothing...", Toast.LENGTH_SHORT).show();
        } else {
            queryForRecipes();
        }
    }

    private void setupNoSavedRecipesListeners(){
        addFirstRecipeBtn = noSavedRecipesFrag.getView().findViewById(R.id.add_first_recipe_btn);
    }

    private void setupListRecipesListeners(){
        View listRecipesFragView = listRecipesFrag.getView();

        addRecipeFab = listRecipesFragView.findViewById(R.id.main_fab_add_recipe);
        addRecipeFab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditRecipeActivity.class);
            addEditActivityResultLauncher.launch(intent);
        });

        mainRecyclerView = listRecipesFragView.findViewById(R.id.main_recipes_recycler);
        final RecipeViewAdapter rva = new RecipeViewAdapter(new RecipeViewAdapter.RecipeDiff(), v -> {
            String id = ((TextView) v.findViewById(R.id.frag_recipe_list_row_recipe_id)).getText().toString();
            String name = ((TextView) v.findViewById(R.id.frag_recipe_list_row_text_view)).getText().toString();

            Intent intent = new Intent(MainActivity.this, ViewRecipeActivity.class);
            intent.putExtra("recipe_id", Long.valueOf(id));
            intent.putExtra("recipe_name", name);
            viewActivityResultLauncher.launch(intent);
        });
        mainRecyclerView.setAdapter(rva);
        mainRecyclerView.setClickable(true);
        recipesToRender.observe(this, rva::submitList);

        mainRecyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL));
    }

    private void swapFragments(int fragmentAttach){
        Fragment swappedTo = null;
        switch (fragmentAttach){
            case FRAGMENT_LOADING:
                if (getSupportFragmentManager().findFragmentByTag(IsLoading.class.getSimpleName()) == null){
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setReorderingAllowed(true)
                            .add(R.id.main_fragment_container_view, isLoadingFrag, IsLoading.class.getSimpleName())
                            .commit();
                } else if (currentFrag != isLoadingFrag) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setReorderingAllowed(true)
                            .attach(isLoadingFrag)
                            .commit();
                } else {
                    return;
                }
                swappedTo = isLoadingFrag;
                break;

            case FRAGMENT_LIST:
                if (getSupportFragmentManager().findFragmentByTag(ListRecipes.class.getSimpleName()) == null){
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setReorderingAllowed(true)
                            .add(R.id.main_fragment_container_view, listRecipesFrag, ListRecipes.class.getSimpleName())
                            .runOnCommit(this::setupListRecipesListeners)
                            .commit();
                } else if (currentFrag != listRecipesFrag) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setReorderingAllowed(true)
                            .attach(listRecipesFrag)
                            .runOnCommit(this::setupListRecipesListeners)
                            .commit();
                } else {
                    return;
                }
                swappedTo = listRecipesFrag;
                break;

            case FRAGMENT_NO_SAVED_RECIPES:
                if (getSupportFragmentManager().findFragmentByTag(NoSavedRecipes.class.getSimpleName()) == null){
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setReorderingAllowed(true)
                            .add(R.id.main_fragment_container_view, noSavedRecipesFrag, NoSavedRecipes.class.getSimpleName())
                            .runOnCommit(this::setupNoSavedRecipesListeners)
                            .commit();
                } else if (currentFrag != noSavedRecipesFrag){
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setReorderingAllowed(true)
                            .attach(noSavedRecipesFrag)
                            .runOnCommit(this::setupNoSavedRecipesListeners)
                            .commit();
                } else {
                    return;
                }
                swappedTo = noSavedRecipesFrag;
                break;

            case FRAGMENT_SEARCH_RETURNED_EMPTY:
                if (getSupportFragmentManager().findFragmentByTag(SearchReturnsEmpty.class.getSimpleName()) == null){
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setReorderingAllowed(true)
                            .add(R.id.main_fragment_container_view, searchReturnsEmptyFrag, SearchReturnsEmpty.class.getSimpleName())
                            .commit();
                } else if (currentFrag != searchReturnsEmptyFrag) {
                    getSupportFragmentManager()
                            .beginTransaction()
                            .setReorderingAllowed(true)
                            .attach(searchReturnsEmptyFrag)
                            .commit();
                } else {
                    return;
                }
                swappedTo = searchReturnsEmptyFrag;
                break;
        }

        if (currentFrag != null){
            getSupportFragmentManager()
                    .beginTransaction()
                    .setReorderingAllowed(true)
                    .detach(currentFrag)
                    .commit();
        }
        currentFrag = swappedTo;
    }

    private void toggleSearchBarVisible(){
        searchBar.setVisibility(searchBar.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void toggleSearchOptionsVisible(){
        boolean isOptionsVisible = searchBarOptionsContainer.getVisibility() == View.VISIBLE;
        searchBarOptionsContainer.setVisibility(isOptionsVisible ? View.GONE : View.VISIBLE);
        expandSearchOptionsBtn.setImageDrawable(isOptionsVisible ?
            getDrawable(R.drawable.baseline_expand_more_32) :
            getDrawable(R.drawable.baseline_expand_less_32));
    }

}