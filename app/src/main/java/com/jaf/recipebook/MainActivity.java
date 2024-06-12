package com.jaf.recipebook;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.splashscreen.SplashScreen;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import androidx.appcompat.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.textview.MaterialTextView;
import com.jaf.recipebook.adapters.RecipeViewAdapter;
import com.jaf.recipebook.db.FullRecipeTuple;
import com.jaf.recipebook.db.RecipeBookDao;
import com.jaf.recipebook.db.RecipeBookDatabase;
import com.jaf.recipebook.db.RecipeBookRepo;
import com.jaf.recipebook.db.recipes.RecipesModel;
import com.jaf.recipebook.events.DbCheckpointCreated;
import com.jaf.recipebook.events.DbRefreshEvent;
import com.jaf.recipebook.events.DbShutdownEvent;
import com.jaf.recipebook.events.DriveTimestampResultEvent;
import com.jaf.recipebook.events.RecipeSavedEvent;
import com.jaf.recipebook.fragments.IsLoading;
import com.jaf.recipebook.fragments.ListRecipes;
import com.jaf.recipebook.fragments.NoSavedRecipes;
import com.jaf.recipebook.fragments.SearchReturnsEmpty;
import com.jaf.recipebook.helpers.DriveServiceHelper;
import com.jaf.recipebook.helpers.FileHelper;
import com.jaf.recipebook.helpers.GeneralHelper;
import com.jaf.recipebook.helpers.GoogleSignInHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    public final String TAG = "JAF-MAIN";

    private final int FRAGMENT_LOADING = 11;
    private final int FRAGMENT_LIST = 12;
    private final int FRAGMENT_NO_SAVED_RECIPES = 13;
    private final int FRAGMENT_SEARCH_RETURNED_EMPTY = 14;

    FileHelper fh;
    private DriveServiceHelper dsh;
    private RecipeBookDatabase rbd;
    private RecipeBookRepo rbr;

    boolean leaveMenuEmpty = false;
    boolean hasRecipes = false;
    boolean showingOptions = false;
    private Fragment currentFrag = null;
    private Handler mainHandler;
    private MutableLiveData<List<RecipeBookDao.BasicRecipeTuple>> recipesToRender;
    private Runnable workRunnableSearch = null;
    private HashSet<String> categories;
    private HashSet<ConstraintLayout> bulkActionList;
    private long newTimestamp = 0;

    ActivityResultLauncher<Intent> addEditActivityResultLauncher;
    ActivityResultLauncher<Intent> driveSettingsActivityResultLauncher;
    ActivityResultLauncher<Intent> settingsActivityResultLauncher;
    ActivityResultLauncher<Intent> viewActivityResultLauncher;

    Button addFirstRecipeBtn;
    CheckBox titleCB;
    CheckBox tagsCB;
    CheckBox ingredientCB;
    CheckBox directionsCB;
    CheckBox sourceCB;
    ConstraintLayout searchBar;
    ConstraintLayout categoryTabContainer;
    EditText searchBarEditText;
    FloatingActionButton addRecipeFab;
    Fragment isLoadingFrag;
    Fragment listRecipesFrag;
    Fragment noSavedRecipesFrag;
    Fragment searchReturnsEmptyFrag;
    ImageButton clearSearchBarBtn;
    ImageButton expandSearchOptionsBtn;
    MaterialTextView categoryReminder;
    MaterialTextView categoryTv;
    MaterialTextView searchingForReminderTv;
    RecyclerView mainRecyclerView;
    TabLayout categoryTabLayout;
    TableLayout searchBarOptionsContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SplashScreen.installSplashScreen(this);
        setContentView(R.layout.activity_start);
        setupComponentHandlers();
        setClassVars();
        setupListeners();

        dsh = GoogleSignInHelper.getDriveServiceHelper(this, false);
        fh.setPreference(
                fh.STARTUP_COUNTER_PREFERENCE,
                fh.getPreference(fh.STARTUP_COUNTER_PREFERENCE, 0
                ) + 1);
        Log.i(TAG, String.valueOf(fh.getPreference(fh.STARTUP_COUNTER_PREFERENCE, 0)));

        if (dsh != null){
            dsh.downloadTimestamp();
        }
    }

    @Override
    protected void onResume(){
        super.onResume();
        swapFragments(FRAGMENT_LOADING);

        clearBulkActionList();

        // If MANAGE_EXTERNAL_STORAGE permission not granted and the Shared Preference for using
        // external storage is true (or doesn't exist yet), open dialog requesting the permission
        validateExternalPermission();

        // Check if user wants to connect to their Google Drive to backup the Recipe Files
        promptGoogleDriveLogin();

        dataRefresh();
        if (!searchBarEditText.getText().toString().isEmpty()) {
            setSearchBarVisible(false);
        } else if (categoryTabLayout.getTabCount() > 0 && categoryTabLayout.getSelectedTabPosition() > 0){
            setCategoriesTabsVisible();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu (Menu menu){
        menu.clear();
        if (leaveMenuEmpty) {
            return true;
        }

        if (bulkActionList.isEmpty()) {
            getMenuInflater().inflate(R.menu.menu_main, menu);
            if (hasRecipes){
                getMenuInflater().inflate(R.menu.menu_search, menu);
            }
            if (!categories.isEmpty()){
                if (categoryTabContainer.getVisibility() == View.VISIBLE){
                    getMenuInflater().inflate(R.menu.menu_categories_hide, menu);
                } else {
                    getMenuInflater().inflate(R.menu.menu_categories_show, menu);
                }
            }
        } else {
            getMenuInflater().inflate(R.menu.main_menu_bulk_action, menu);
            if (bulkActionList.size() < recipesToRender.getValue().size()){
                getMenuInflater().inflate(R.menu.menu_bulk_select_all, menu);
            } else {
                getMenuInflater().inflate(R.menu.menu_bulk_deselect_all, menu);
            }
        }
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

            case R.id.action_category_show:
            case R.id.action_category_hide:
                // Toggle Category tabs show/hide
                toggleCategoryTabVisibility();
                return true;

            case R.id.bulk_select_all_btn:
                for(int i = 0; i < mainRecyclerView.getChildCount(); i ++) {
                    ConstraintLayout cl = (ConstraintLayout) mainRecyclerView.getChildAt(i);
                    cl.findViewById(R.id.frag_recipe_list_row_selected).setVisibility(View.VISIBLE);
                    bulkActionList.add(cl);
                }
                invalidateOptionsMenu();
                return true;

            case R.id.bulk_deselect_all_btn:
                clearBulkActionList();
                return true;

            case R.id.bulk_delete_btn:
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.delete_confirmation_title))
                        .setMessage(getString(R.string.delete_confirmation_msg))
                        .setPositiveButton(this.getString(R.string.bulk_delete_confirm), (dialogInterface, i) -> {
                            leaveMenuEmpty = true;
                            invalidateMenu();
                            bulkDelete();
                        })
                        .setNegativeButton(this.getString(R.string.bulk_delete_cancel), (dialogInterface, i) -> {})
                        .show();
                return true;

            case R.id.bulk_download_btn:
                bulkDownload();
                return true;

            default:
                Log.w(TAG, "onOptionsItemSelected: Unknown Item ID for selected item: "
                        + item.toString());
                return super.onOptionsItemSelected(item);
        }
    }

    private void clearBulkActionList(){
        for (ConstraintLayout rowItem : bulkActionList){
            rowItem.findViewById(R.id.frag_recipe_list_row_selected).setVisibility(View.GONE);
        }
        bulkActionList = new HashSet<>();
        invalidateOptionsMenu();
    }

    private void setClassVars(){
        mainHandler = new Handler(getMainLooper());
        fh = new FileHelper(this);
        bulkActionList = new HashSet<>();

        addEditActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), o -> {
                if (o.getResultCode() == Activity.RESULT_OK){
                    Toast.makeText(this, getString(R.string.recipe_saved), Toast.LENGTH_SHORT).show();
                } else if (o.getResultCode() == GeneralHelper.ACTIVITY_RESULT_DB_ERROR) {
                    Toast.makeText(this, getString(R.string.failed_to_open_recipe), Toast.LENGTH_LONG).show();
                }
            }
        );

        driveSettingsActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {});

        settingsActivityResultLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), o -> {
            if (o.getResultCode() == GeneralHelper.ACTIVITY_RESULT_SIGN_IN_PROMPT) {
                dsh = GoogleSignInHelper.getDriveServiceHelper(this, false);
                if (dsh != null) {
                    fh.setPreference(
                            fh.STARTUP_COUNTER_PREFERENCE,
                            fh.getPreference(fh.STARTUP_COUNTER_PREFERENCE, 0
                            ) + 1);
                    promptGoogleDriveLogin();
                }
            }
        });

        viewActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), o -> {
                if (o.getResultCode() == GeneralHelper.ACTIVITY_RESULT_DB_ERROR) {
                    Toast.makeText(this, getString(R.string.failed_to_open_recipe), Toast.LENGTH_LONG).show();
                } else if (o.getResultCode() == GeneralHelper.ACTIVITY_RESULT_UPDATE_SEARCH){
                    if (categoryTabLayout.getTabCount() > 0) categoryTabLayout.selectTab(categoryTabLayout.getTabAt(0));
                    setSearchBarVisible(false);
                    toggleSearchOptionsVisible();
                    tagsCB.setChecked(true);
                    searchBarEditText.setText(o.getData().getStringExtra("search_input"));
                }
            }
        );

        recipesToRender = new MutableLiveData<>(new ArrayList<>());
        rbd = RecipeBookDatabase.getInstance(this);
        rbr = new RecipeBookRepo(rbd);

        isLoadingFrag = new IsLoading();
        listRecipesFrag = new ListRecipes();
        noSavedRecipesFrag = new NoSavedRecipes();
        searchReturnsEmptyFrag = new SearchReturnsEmpty();
    }

    private void setupComponentHandlers(){
        searchBar = findViewById(R.id.main_search_bar);
        searchBarEditText = findViewById(R.id.searchbar_edit_text);
        searchBarOptionsContainer = findViewById(R.id.searchbar_options_container);
        clearSearchBarBtn = findViewById(R.id.clear_searchbar_btn);
        expandSearchOptionsBtn = findViewById(R.id.toggle_search_options_btn);
        categoryTabContainer = findViewById(R.id.category_tab_container);
        categoryTabLayout = findViewById(R.id.category_tab_layout);

        titleCB = findViewById(R.id.search_title_checkbox);
        tagsCB = findViewById(R.id.search_tags_checkbox);
        ingredientCB = findViewById(R.id.search_ingredients_checkbox);
        directionsCB = findViewById(R.id.search_directions_checkbox);
        sourceCB = findViewById(R.id.search_source_checkbox);
        categoryReminder = findViewById(R.id.category_reminder);
        categoryTv = findViewById(R.id.search_categories_text);
        searchingForReminderTv = findViewById(R.id.searching_for_reminder);
    }

    private void setupListeners(){
        Context context = this;
        getOnBackPressedDispatcher().addCallback(new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (bulkActionList.isEmpty()){
                    new AlertDialog.Builder(context)
                        .setTitle(getString(R.string.exit_confirmation))
                        .setPositiveButton(context.getString(R.string.affirmative_text),
                                (dialogInterface, i) -> {
                                    finish();
                                })
                        .setNegativeButton(context.getString(R.string.negative_text),
                                (dialogInterface, i) -> {})
                        .show();
                } else {
                    clearBulkActionList();
                }
            }
        });

        categoryTabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                categoryTv.setText(tab.getText().toString());
                if (tab.getPosition() > 0) {
                    setCategoryReminderVisible();
                } else {
                    setCategoryReminderGone();
                }
                queryForRecipes();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) { }

            @Override
            public void onTabReselected(TabLayout.Tab tab) { }
        });

        expandSearchOptionsBtn.setOnClickListener(v -> toggleSearchOptionsVisible());
        clearSearchBarBtn.setOnClickListener(v -> searchBarEditText.setText("") );

        titleCB.setOnClickListener(v -> searchCheckboxListener((CheckBox) v));
        tagsCB.setOnClickListener(v -> searchCheckboxListener((CheckBox) v));
        ingredientCB.setOnClickListener(v -> searchCheckboxListener((CheckBox) v));
        directionsCB.setOnClickListener(v -> searchCheckboxListener((CheckBox) v));
        sourceCB.setOnClickListener(v -> searchCheckboxListener((CheckBox) v));
        categoryTv.setOnClickListener(v -> {
            if (!categories.isEmpty()) setCategoriesTabsVisible();
        });

        searchBarEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0){
                    searchingForReminderTv.setVisibility(View.VISIBLE);
                    searchingForReminderTv.setText(s.toString());
                } else {
                    searchingForReminderTv.setVisibility(View.GONE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
                queryForRecipes();
            }
        });
        searchBarEditText.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    if (workRunnableSearch != null) {
                        mainHandler.removeCallbacks(workRunnableSearch);
                    }
                    queryForRecipes();
                }
                return false;
            }
        });
    }

    public void validateExternalPermission(){
        if (fh.getPreference(fh.EXTERNAL_STORAGE_PREFERENCE, true)
                && !Environment.isExternalStorageManager()) {
            // Permission missing, but the user has indicated that they want external storage
            //      Could also be their first time starting up the app.
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.validate_ext_permission_title))
                    .setMessage(getString(R.string.validate_ext_permission_msg))
                    .setPositiveButton(this.getString(R.string.dialog_allow), (dialogInterface, i) -> {
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                                .addCategory("android.intent.category.DEFAULT")
                                .setData(uri)
                        );
                        fh.setPreference(fh.EXTERNAL_STORAGE_PREFERENCE, true);
                        dialogInterface.dismiss();
                    })
                    .setNegativeButton(this.getString(R.string.dialog_deny), (dialogInterface, i) -> {
                                fh.setPreference(fh.EXTERNAL_STORAGE_PREFERENCE, true);
                    })
                    .show();
        } else if (Environment.isExternalStorageManager() && !fh.getPreference(fh.EXTERNAL_STORAGE_PREFERENCE, false)){
            fh.setPreference(fh.EXTERNAL_STORAGE_PREFERENCE, true);
        }
    }

    private void promptGoogleDriveLogin(){
        boolean cloudStorageActive = fh.getPreference(fh.BACKUP_TIMESTAMP_PREFERENCE, false);
        int startupCounter = fh.getPreference(fh.STARTUP_COUNTER_PREFERENCE, 0);
        if (dsh == null && startupCounter == 2){
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.sign_in_prompt_title))
                    .setMessage(getString(R.string.sign_in_prompt_msg))
                    .setPositiveButton(this.getString(R.string.affirmative_text), (dialogInterface, i) -> {
                        Intent intent = new Intent(this, SettingsActivity.class);
                        intent.putExtra("flashSignIn", true);
                        settingsActivityResultLauncher.launch(intent);
                        dialogInterface.cancel();
                    })
                    .setNegativeButton(this.getString(R.string.maybe_later), (dialogInterface, i) -> {
                        fh.setPreference(
                                fh.STARTUP_COUNTER_PREFERENCE,
                                fh.getPreference(fh.STARTUP_COUNTER_PREFERENCE, 0
                                ) + 1);
                    })
                    .show();
        }
        if (dsh != null && !cloudStorageActive && startupCounter == 3){
            fh.setPreference(
                    fh.STARTUP_COUNTER_PREFERENCE,
                    fh.getPreference(fh.STARTUP_COUNTER_PREFERENCE, 0
                    ) + 1);
            new AlertDialog.Builder(this)
                    .setTitle(getString(R.string.enable_backup_prompt_title))
                    .setMessage(getString(R.string.enable_backup_prompt_msg))
                    .setPositiveButton(this.getString(R.string.affirmative_text), (dialogInterface, i) -> {
                        Intent intent = new Intent(this, DriveSettingsActivity.class);
                        intent.putExtra("flashToggle", true);
                        driveSettingsActivityResultLauncher.launch(intent);
                        dialogInterface.cancel();
                    })
                    .setNegativeButton(this.getString(R.string.maybe_later), (dialogInterface, i) -> {})
                    .show();
        }
    }

    private void queryForCategories(){
        rbd.getQueryExecutor().execute(() -> {
            categories = new HashSet<>(rbd.recipeDao().getDistinctCategories());
            categories.remove(null);

            runOnUiThread(() -> {
                GeneralHelper.ensureCategoryPrefUpdated(categories, fh);
                ArrayList<String> orderedCategories = GeneralHelper.getCategoryOrderPreference(fh);
                if (!orderedCategories.isEmpty()){
                    HashSet<String> activeTabLabels = new HashSet<>();
                    for (int i = 1; i < categoryTabLayout.getTabCount(); i ++) {
                        activeTabLabels.add(categoryTabLayout.getTabAt(i).getText().toString());
                    }

                    ArrayList<String> currentUiCategories = new ArrayList<>();
                    for (int i = 1; i < categoryTabLayout.getTabCount(); i ++){
                        currentUiCategories.add(categoryTabLayout.getTabAt(i).getText().toString());
                    }
                    if (!categories.equals(activeTabLabels) || !currentUiCategories.equals(orderedCategories)){
                        categoryTabLayout.removeAllTabs();
                        categoryTabLayout.addTab(categoryTabLayout.newTab().setText(getString(R.string.all_recipes)));
                        for (String category : orderedCategories){
                            TabLayout.Tab newTab = categoryTabLayout.newTab().setText(category);
                            categoryTabLayout.addTab(newTab);
                        }
                    }
                }
                clearBulkActionList();
            });
        });
    }

    public void queryForRecipes(){

        int currentTabIndex = categoryTabLayout.getSelectedTabPosition();
        String searchQuery = searchBarEditText.getText().toString();
        clearBulkActionList();

        rbd.getQueryExecutor().execute(() -> {
            ArrayList<RecipeBookDao.BasicRecipeTuple> recipes;
            if (searchQuery.isEmpty() && currentTabIndex < 1) {
                recipes = new ArrayList<>(rbd.recipeBookDao().getAllRecipes());
                if (!recipes.isEmpty()){
                    hasRecipes = true;
                }
            } else if (currentTabIndex >= 1 && !searchQuery.isEmpty()) {
                String categoryText = categoryTabLayout.getTabAt(currentTabIndex).getText().toString();
                recipes = new ArrayList<>(rbd.recipeBookDao().searchAllForParameter(
                        (titleCB.isChecked() ? searchQuery : null),
                        categoryText,
                        (sourceCB.isChecked() ? searchQuery : null),
                        (ingredientCB.isChecked() ? searchQuery : null),
                        (directionsCB.isChecked() ? searchQuery : null),
                        (tagsCB.isChecked() ? searchQuery : null)
                ));
            } else if (currentTabIndex >= 1) {
                String categoryText = categoryTabLayout.getTabAt(currentTabIndex).getText().toString();
                recipes = new ArrayList<>(rbd.recipeBookDao().getRecipesForCategory(categoryText));
            } else {
                recipes = new ArrayList<>(rbd.recipeBookDao().searchAllForParameter(
                    (titleCB.isChecked() ? searchQuery : null),
                    (sourceCB.isChecked() ? searchQuery : null),
                    (ingredientCB.isChecked() ? searchQuery : null),
                    (directionsCB.isChecked() ? searchQuery : null),
                    (tagsCB.isChecked() ? searchQuery : null)
                ));
            }
            recipesToRender.postValue(recipes);
            if (recipes.isEmpty() && (!searchQuery.isEmpty() || currentTabIndex >= 1)) {
                swapFragments(FRAGMENT_SEARCH_RETURNED_EMPTY);
            } else if (recipes.isEmpty()){
                swapFragments(FRAGMENT_NO_SAVED_RECIPES);
            } else {
                swapFragments(FRAGMENT_LIST);
            }
        });
    }

    private void dataRefresh(){
        leaveMenuEmpty = false;
        queryForCategories();
        queryForRecipes();
    }

    private void searchCheckboxListener(CheckBox checkBoxClicked){
        if (!titleCB.isChecked() && !tagsCB.isChecked() && !ingredientCB.isChecked() &&
                !directionsCB.isChecked() && !sourceCB.isChecked()){
            checkBoxClicked.setChecked(true);
            Toast.makeText(this, getString(R.string.search_needs_something), Toast.LENGTH_SHORT).show();
        } else {
            queryForRecipes();
        }
    }

    private void setupNoSavedRecipesListeners(){
        addFirstRecipeBtn = noSavedRecipesFrag.getView().findViewById(R.id.add_first_recipe_btn);
        addFirstRecipeBtn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditRecipeActivity.class);
            addEditActivityResultLauncher.launch(intent);
        });
    }

    private void setupListRecipesListeners(){
        View listRecipesFragView = listRecipesFrag.getView();

        addRecipeFab = listRecipesFragView.findViewById(R.id.main_fab_add_recipe);
        addRecipeFab.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AddEditRecipeActivity.class);
            addEditActivityResultLauncher.launch(intent);
        });

        mainRecyclerView = listRecipesFragView.findViewById(R.id.main_recipes_recycler);
        final RecipeViewAdapter rva = new RecipeViewAdapter(
                new RecipeViewAdapter.RecipeDiff(),
                v -> {
                    if (bulkActionList.isEmpty()) {
                        String id = ((TextView) v.findViewById(R.id.frag_recipe_list_row_recipe_id)).getText().toString();
                        String name = ((TextView) v.findViewById(R.id.frag_recipe_list_row_text_view)).getText().toString();

                        Intent intent = new Intent(MainActivity.this, ViewRecipeActivity.class);
                        intent.putExtra("recipe_id", Long.valueOf(id));
                        intent.putExtra("recipe_name", name);
                        viewActivityResultLauncher.launch(intent);
                    } else {
                        selectRowItemEffect((ConstraintLayout) v);
                    }
                },
                v -> {
                    selectRowItemEffect((ConstraintLayout) v);
                    return true;
                });
        mainRecyclerView.setAdapter(rva);
        mainRecyclerView.setClickable(true);
        recipesToRender.observe(this, rva::submitList);

        mainRecyclerView.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL));
    }

    private void selectRowItemEffect(ConstraintLayout v){
        if (bulkActionList.contains(v)){
            v.findViewById(R.id.frag_recipe_list_row_selected).setVisibility(View.GONE);
            bulkActionList.remove(v);
        } else {
            v.findViewById(R.id.frag_recipe_list_row_selected).setVisibility(View.VISIBLE);
            bulkActionList.add(v);
        }
        invalidateOptionsMenu();
    }

    private void swapFragments(int fragmentAttach){
        Fragment swappedTo = null;
        switch (fragmentAttach){
            case FRAGMENT_LOADING:
                if (getSupportFragmentManager().findFragmentById(isLoadingFrag.getId()) == null){
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
                if (getSupportFragmentManager().findFragmentById(listRecipesFrag.getId()) == null){
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
                if (getSupportFragmentManager().findFragmentById(noSavedRecipesFrag.getId()) == null){
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
                if (getSupportFragmentManager().findFragmentById(searchReturnsEmptyFrag.getId()) == null){
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
        if (searchBar.getVisibility() == View.GONE){
            setSearchBarVisible(true);
        } else {
            setSearchBarGone();
        }
    }

    private void toggleSearchOptionsVisible(){
        if (searchBarOptionsContainer.getVisibility() == View.VISIBLE) {
            setSearchOptionsGone();
        } else {
            setSearchOptionsVisible();
        }
    }

    private void toggleCategoryTabVisibility(){
        if (categoryTabContainer.getVisibility() == View.GONE){
            setCategoriesTabsVisible();
        } else {
            setCategoriesTabsGone();
        }
    }

    private void setSearchBarVisible(boolean showKeyboard){
        setCategoriesTabsGone();
        searchBar.setVisibility(View.VISIBLE);

        if (showKeyboard){
            searchBarEditText.requestFocus();
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .showSoftInput(searchBarEditText, InputMethodManager.SHOW_IMPLICIT);
        }
    }

    private void setSearchBarGone(){
        searchBar.setVisibility(View.GONE);
        ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                .hideSoftInputFromWindow(searchBarEditText.getWindowToken(), 0);
    }

    private void setSearchOptionsVisible() {
        showingOptions = true;
        searchBarOptionsContainer.setVisibility(View.VISIBLE);
        expandSearchOptionsBtn.setImageDrawable(getDrawable(R.drawable.baseline_expand_more_32));
    }

    private void setSearchOptionsGone() {
        showingOptions = false;
        searchBarOptionsContainer.setVisibility(View.GONE);
        expandSearchOptionsBtn.setImageDrawable(getDrawable(R.drawable.baseline_expand_less_32));
    }

    private void setCategoryReminderVisible(){
        if (categoryTabLayout.getSelectedTabPosition() > 0 && !showingOptions) {
            categoryReminder.setVisibility(View.VISIBLE);
            categoryReminder.setText(
                    categoryTabLayout.getTabAt(
                            categoryTabLayout.getSelectedTabPosition()
                    ).getText().toString()
            );
        } else {
            categoryReminder.setVisibility(View.GONE);
        }
    }

    private void setCategoryReminderGone(){
        categoryReminder.setVisibility(View.GONE);
    }

    private void setCategoriesTabsVisible(){
        setSearchBarGone();
        categoryTabContainer.setVisibility(View.VISIBLE);
        invalidateOptionsMenu();
    }

    private void setCategoriesTabsGone(){
        categoryTabContainer.setVisibility(View.GONE);
        invalidateOptionsMenu();
    }

    private void bulkDownload(){
        rbd.getQueryExecutor().execute(() -> {
            for (ConstraintLayout recipeRow : bulkActionList){
                long recipeId = Long.valueOf(
                    ((TextView) recipeRow.findViewById(R.id.frag_recipe_list_row_recipe_id))
                    .getText().toString());
                FullRecipeTuple frt = rbr.getFullRecipeData(recipeId);
                fh.saveRecipeToDownloads(
                        frt.recipesModel,
                        frt.ingredientsModel,
                        frt.directionsModel,
                        frt.tagsModel,
                        true
                );
            }
            runOnUiThread(() -> {
                Toast.makeText(this, getString(R.string.download_to_local_success), Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void bulkDelete(){
        swapFragments(FRAGMENT_LOADING);
        setCategoriesTabsGone();
        setSearchBarGone();

        rbd.getQueryExecutor().execute(() -> {
            Iterator<ConstraintLayout> iterator = bulkActionList.iterator();
            while (iterator.hasNext()) {
                ConstraintLayout recipeRow = iterator.next();
                long recipeId = Long.valueOf(
                    ((TextView) recipeRow.findViewById(R.id.frag_recipe_list_row_recipe_id))
                    .getText().toString());
                RecipesModel rm = rbd.recipeDao().getRecipe(recipeId).blockingGet();
                rbr.deleteRecipe(rm, !iterator.hasNext());
            }
        });
    }

    @Subscribe
    public void onRecipeSaved(RecipeSavedEvent recipeSavedEvent){
        Log.i(TAG, "onRecipeSaved called from Main");
        if (recipeSavedEvent.recipeSaved) {
            if (dsh != null && fh.getPreference(fh.AUTO_BACKUP_ACTIVE_PREFERENCE, false)) {
                Log.i(TAG, "Backing up from Main");
                rbr.createCheckpoint();
            }
        } else {
            Log.e(TAG, "Recipe Save failed...");
        }
        runOnUiThread(this::dataRefresh);
    }

    // TODO Download from sync not working, names don't match

    @Subscribe
    public void onTimestampDownloaded(DriveTimestampResultEvent driveTimestampResultEvent){
        Log.i(TAG, "Last timestamp at " + Long.toString(driveTimestampResultEvent.timestamp));
        if (dsh != null
                && fh.getPreference(fh.AUTO_BACKUP_ACTIVE_PREFERENCE, false)
                && driveTimestampResultEvent.timestamp > fh.getPreference(fh.BACKUP_TIMESTAMP_PREFERENCE, (long) 0)) {
            runOnUiThread(() -> {
                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.cloud_has_more_recent_upload))
                        .setMessage(getString(R.string.request_replace_data))
                        .setNegativeButton(R.string.negative_response, (dialog, which) -> { })
                        .setPositiveButton(R.string.affirmative_text, (dialog, which) -> {
                            swapFragments(FRAGMENT_LOADING);
                            leaveMenuEmpty = true;
                            invalidateMenu();
                            newTimestamp = driveTimestampResultEvent.timestamp;
                            dsh.download();
                        })
                        .create().show();
            });
        }
    }

    @Subscribe
    public void onDbShutdownEvent(DbShutdownEvent dbShutdownEvent) {
        rbd.close();
        rbr = null;
    }

    @Subscribe
    public void onDbRefreshEvent(DbRefreshEvent dbRefreshEvent){
        if (dbRefreshEvent.shouldRefresh){
            fh.setPreference(fh.BACKUP_TIMESTAMP_PREFERENCE, newTimestamp);
        }
        rbd = RecipeBookDatabase.getInstance(this);
        rbr =  new RecipeBookRepo(rbd);
        dataRefresh();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void checkpointAttempted(DbCheckpointCreated dbCheckpointCreated) {
        if (dbCheckpointCreated.success){
            dsh.upload();
        } else {
            Toast.makeText(this, getString(R.string.save_failed), Toast.LENGTH_LONG).show();
        }
    }

}