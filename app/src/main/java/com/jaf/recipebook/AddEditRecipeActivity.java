package com.jaf.recipebook;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatAutoCompleteTextView;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.jaf.recipebook.db.FullRecipeTuple;
import com.jaf.recipebook.db.RecipeBookDatabase;
import com.jaf.recipebook.db.RecipeBookRepo;
import com.jaf.recipebook.db.directions.DirectionsModel;
import com.jaf.recipebook.db.ingredients.IngredientsModel;
import com.jaf.recipebook.db.recipes.RecipesModel;
import com.jaf.recipebook.db.tags.TagsModel;
import com.jaf.recipebook.events.RecipeSavedEvent;
import com.jaf.recipebook.helpers.GeneralHelper;
import com.jaf.recipebook.adapters.TagViewAdapter;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class AddEditRecipeActivity extends AppCompatActivity {
    final String TAG = "JAF-AddEditRecipe";

    long recipeId;
    private RecipeBookDatabase rbdb;
    private RecipeBookRepo rbr;

    private RecipesModel rm;
    private List<IngredientsModel> ims;
    private DirectionsModel dm;
    private List<TagsModel> tms;

    private MutableLiveData<List<TagsModel>> mutable_tms = new MutableLiveData<>(new ArrayList<>());

    private AutoCompleteTextView categoryInput;
    private ConstraintLayout rootConstraint;
    private TextInputEditText titleInput;
    private TextInputEditText ingredientInput;
    private TextInputEditText directionInput;
    private TextInputEditText servingsInput;
    private TextInputEditText sourceUrlInput;
    private TextInputEditText tagsInput;
    private RecyclerView chipGroup;
    private ScrollView scrollView;

    private Animation flashField;

    private Handler mainHandler;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recipeId = getIntent().getLongExtra("recipe_id", -1);
        Log.i(TAG, "onCreate: " + recipeId);
        rbdb = RecipeBookDatabase.getInstance(this);
        rbr = new RecipeBookRepo(rbdb);
        mainHandler = new Handler(getMainLooper());

        if (recipeId >= 0){
            setContentView(R.layout.activity_is_loading);
            queryForData();
        } else {
            setContentView(R.layout.activity_add_edit_recipe);
            Objects.requireNonNull(this.getSupportActionBar()).setTitle(R.string.add_new_recipe);
            addEditSetup();
        }

        createFlashAnimation();
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            View v = getCurrentFocus();
            if ( v instanceof AutoCompleteTextView) {
                Rect outRect = new Rect();
                v.getGlobalVisibleRect(outRect);
                if (!outRect.contains((int)event.getRawX(), (int)event.getRawY())) {
                    v.clearFocus();
                    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
                }
            }
        }
        return super.dispatchTouchEvent( event );
    }

    /**
     * Some setup can only be done once we're using the Add/Edit layout
     */
    private void addEditSetup(){
        getInputFields();
        setupServingsListener();
        setupTagsOnBlurListener();
        setupOnEnterCategoryFieldListener();
        setupChipRecycler();
        setupTagTextWatcher();
        setupCategoryAutoComplete();
    }

    /**
     * Initialize the handles for all edit fields and other required views
     */
    private void getInputFields(){
        titleInput = findViewById(R.id.textInput_RecipeTitleInput);
        ingredientInput = findViewById(R.id.textInput_RecipeIngredientsInput);
        directionInput = findViewById(R.id.textInput_RecipeDirectionsInput);
        servingsInput = findViewById(R.id.textInput_RecipeServingsInput);
        sourceUrlInput = findViewById(R.id.textInput_RecipeSourceUrlInput);
        categoryInput = findViewById(R.id.textInput_RecipeCategoryInput);
        chipGroup = findViewById(R.id.chipLayout_recipeTags);
        tagsInput = findViewById(R.id.textInput_RecipeTagsInput);

        scrollView = findViewById(R.id.add_edit_scroll_view);
        rootConstraint = findViewById(R.id.add_edit_root_constraint);
    }

    /**
     * Adds listener to Servings field, removes unnecessary decimal or trailing zeros.
     */
    private void setupServingsListener(){
        servingsInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER){
                ensurePaddedSeparator();
                return true;
            }
            return false;
        });
        servingsInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus) { ensurePaddedSeparator(); }
        });
    }

    /**
     * Remind user to apply tag if they change focus with text in the Tags Edit Field
     */
    private void setupTagsOnBlurListener(){
        tagsInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (!hasFocus && !((TextInputEditText) v).getText().toString().isEmpty()){
                Toast.makeText(this, getString(R.string.recipe_tag_remind_user_to_apply), Toast.LENGTH_LONG).show();
                tagsInput.startAnimation(flashField);
            }
        });
    }

    private void setupOnEnterCategoryFieldListener(){
        categoryInput.setOnKeyListener((v, keyCode, event) -> {
            if (event.getAction() == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER){
                categoryInput.clearFocus();
                ((InputMethodManager) getSystemService(Activity.INPUT_METHOD_SERVICE))
                        .toggleSoftInput(InputMethodManager.SHOW_IMPLICIT, 0);
            }
           return false;
        });
    }

    /**
     * Add adapter and recycler for adding/removing tags as chips onscreen.
     */
    private void setupChipRecycler(){
        final TagViewAdapter tla = new TagViewAdapter(new TagViewAdapter.TagDiff(),
            v -> {
                String tagText = ((Chip) v).getText().toString();
                ArrayList<TagsModel> newArray = new ArrayList<>(mutable_tms.getValue());
                TagsModel tagToRemove = null;
                for (TagsModel tag: newArray){
                    if (tag.getTag().equals(tagText)){
                        tagToRemove = tag;
                        break;
                    }
                }
                newArray.remove(tagToRemove);
                mutable_tms.setValue(newArray);
            }, true);
        chipGroup.setAdapter(tla);
        chipGroup.setClickable(true);
        mutable_tms.observe(this, tla::submitList);
    }

    private void createFlashAnimation(){
        flashField = new AlphaAnimation(1.0f, 0.0f);
        flashField.setDuration(300);
        flashField.setStartOffset(100);
        flashField.setRepeatMode(Animation.REVERSE);
        flashField.setRepeatCount(3);
    }

    private void setupCategoryAutoComplete(){
        rootConstraint.addOnLayoutChangeListener((v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            if(categoryInput.hasFocus()){
                int fullBottom = bottom + v.getPaddingBottom();
                int sy = scrollView.getScrollY();
                int sh = scrollView.getHeight();
                int delta = fullBottom - (sy + sh);

                scrollView.smoothScrollBy(0, delta);
            }
        });
        categoryInput.setOnFocusChangeListener((v, hasFocus) -> {
            if (hasFocus){
                rootConstraint.setPadding(
                        0,
                        0,
                        0,
                        getResources().getDimensionPixelSize(R.dimen.category_padding_bottom_lg)
                );
            } else {
                rootConstraint.setPadding(
                        0,
                        0,
                        0,
                        getResources().getDimensionPixelSize(R.dimen.category_padding_bottom_sm)
                );
            }
        });
        rbdb.getTransactionExecutor().execute(() -> {
            ArrayList<String> categories = new ArrayList<>(rbdb.recipeDao().getDistinctCategories());
            categories.removeAll(Collections.singleton(null));
            ArrayAdapter<String> adapter = new ArrayAdapter<>(
                    this, android.R.layout.select_dialog_item, categories);
            adapter.setNotifyOnChange(true);
            categoryInput.setThreshold(1);
            runOnUiThread(() -> categoryInput.setAdapter(adapter));
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_edit, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()) {
            case R.id.save_recipe_btn:
                onSave();
                return true;

            case android.R.id.home:
                finish();
                return true;

            default:
                Log.w(TAG, "onOptionsItemSelected: Unknown Item ID for selected item: "
                        + item.toString());
                return super.onOptionsItemSelected(item);
        }
    }

    /**
     * If editing existing recipe, this fetches its information.
     */
    private void queryForData(){
        mExecutor.execute(() -> {
            FullRecipeTuple frt = null;
            try {
                frt = rbr.getFullRecipeData(recipeId);
            } catch (Exception ex){
                Log.e(TAG, "Failed to query recipe", ex);
            } finally {
                if (frt != null) {
                    rm = frt.recipesModel;
                    ims = frt.ingredientsModel;
                    dm = frt.directionsModel;
                    tms = frt.tagsModel;
                    mainHandler.post(this::renderRecipe);
                } else {
                    //Kick back to main activity with error Toast
                    setResult(GeneralHelper.ACTIVITY_RESULT_DB_ERROR);
                    this.finish();
                }
            }
        });
    }

    /**
     * After getting the recipe information from the DB, render it onscreen.
     */
    private void renderRecipe(){
        setContentView(R.layout.activity_add_edit_recipe);
        Objects.requireNonNull(this.getSupportActionBar()).setTitle(rm.getName());

        addEditSetup();

        titleInput.setText(rm.getName());
        directionInput.setText(dm.getText());
        sourceUrlInput.setText(rm.getSource_url());
        categoryInput.setText(rm.getCategory());

        if (rm.getServings() != null){
            String servingStr = String.format(String.valueOf(rm.getServings()));
            if (Integer.valueOf(servingStr.split("\\.")[1]) != 0){
                servingsInput.setText(servingStr);
            } else {
                servingsInput.setText(servingStr.split("\\.")[0]);
            }
        }

        StringBuilder ingredientSb = new StringBuilder();
        for (IngredientsModel im : ims){
            ingredientSb.append(im.getText());
            ingredientSb.append(System.lineSeparator());
        }
        ingredientSb.deleteCharAt(ingredientSb.length() - 1);
        ingredientInput.setText(ingredientSb);

        mutable_tms.setValue(tms);
    }

    private void ensurePaddedSeparator(){
        String inputText = servingsInput.getText().toString();
        if (Pattern.compile("^[.,][0-9]+$").matcher(inputText).find()){
            if(Pattern.compile("^[.,]0*$").matcher(inputText).find()){
                servingsInput.setText("0");
            }else {
                servingsInput.setText("0" + inputText);
            }
        }
        else if (Pattern.compile("^[0-9]+[.,]$").matcher(inputText).find()){
            if(Pattern.compile("^0+[.,]$").matcher(inputText).find()){
                servingsInput.setText("0");
            }else {
                servingsInput.setText(inputText.substring(0, inputText.length() - 1));
            }
        }
        else if (Pattern.compile("^[.,]$").matcher(inputText).find()){
            servingsInput.setText("0");
        }
    }

    /**
     * On keyboard "Enter", populate tag if it doesn't already exist in tag list
     */
    private void setupTagTextWatcher(){
        tagsInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String tagText = v.getText().toString().trim().toLowerCase();
                if (Pattern.compile("^\\s*$").matcher(tagText).find()) {
                    return false;
                } else if (tagInList(tagText)){
                    Toast.makeText(AddEditRecipeActivity.this, getString(R.string.recipe_tag_already_exists), Toast.LENGTH_SHORT).show();
                    return false;
                }
                ArrayList<TagsModel> newArray = new ArrayList<>(Objects.requireNonNull(mutable_tms.getValue()));
                newArray.add(new TagsModel(recipeId, tagText));
                mutable_tms.setValue(newArray);
                v.setText("");

                return true;
            }
        });
    }

    /**
     * Check if tag is currently in the tag list
     * @param inputtedTag tag user is attempting to add
     * @return true if tag is in the list
     */
    private boolean tagInList(String inputtedTag){
        for(TagsModel tag : Objects.requireNonNull(mutable_tms.getValue())){
            if (tag.getTag().equals(inputtedTag)){
                return true;
            }
        }
        return false;
    }

    /**
     * Validates that all the required fields are filled, calls animation if any are empty
     * @return true is fields are filled
     */
    private boolean validateRequiredFieldsFilled(){
        boolean titleEmpty = titleInput.getText().toString().equals("");
        boolean ingredientEmpty = ingredientInput.getText().toString().equals("");
        boolean directionEmpty = directionInput.getText().toString().equals("");

        if (titleEmpty || ingredientEmpty || directionEmpty){
            flashEmptyRequiredFields(titleEmpty, ingredientEmpty, directionEmpty);
            return false;
        }
        return true;
    }

    /**
     * Runs animation to flash empty required fields
     * @param isTitleEmpty
     * @param isIngredientEmpty
     * @param isDirectionEmpty
     */
    private void flashEmptyRequiredFields(boolean isTitleEmpty, boolean isIngredientEmpty, boolean isDirectionEmpty){

        Toast.makeText(this, getString(R.string.recipe_save_missing_req), Toast.LENGTH_SHORT).show();

        if(isTitleEmpty){
            Log.d(TAG, "Missing Title field, aborting save...");
            titleInput.startAnimation(flashField);
        }
        if(isIngredientEmpty){
            Log.d(TAG, "Missing Ingredients field, aborting save...");
            ingredientInput.startAnimation(flashField);
        }
        if(isDirectionEmpty) {
            Log.d(TAG, "Missing Directions field, aborting save...");
            directionInput.startAnimation(flashField);
        }
    }

    /**
     * Convert multi-line edit field into list of Ingredient models to be saved
     * @param recipeId Recipe to associate the ingredients with
     * @return List of ordered Ingredient models
     */
    private ArrayList<IngredientsModel> getIngredientList(long recipeId){
        ArrayList<IngredientsModel> ingredientsModels = new ArrayList<>();
        int order_id = 0;
        for(String line : ingredientInput.getText().toString().trim().split(System.lineSeparator())){
            ingredientsModels.add(new IngredientsModel(recipeId, order_id, line));
            order_id += 1;
        }
        return ingredientsModels;
    }

    /**
     * Validates fields, and saves recipe to database.
     */
    private void onSave(){
        Log.d(TAG, "Attempting to save...");
        if (!validateRequiredFieldsFilled()) {
            Log.d(TAG, "Missing required fields, save aborted");
            return;
        }

        String titleSave = titleInput.getText().toString();
        String directionsSave = directionInput.getText().toString().trim();
        Float servingsSave = null;
        String sourceUrlSave = null;
        String categorySave = null;

        if (!servingsInput.getText().toString().equals("")){
            servingsSave = Float.valueOf(servingsInput.getText().toString());
        }
        if (!sourceUrlInput.getText().toString().equals("")){
            sourceUrlSave = sourceUrlInput.getText().toString();
        }
        if(!categoryInput.getText().toString().equals("")){
            categorySave = categoryInput.getText().toString();
        }

        RecipesModel rm;
        if (this.rm == null) {
            rm = new RecipesModel(titleSave, categorySave, servingsSave, sourceUrlSave, null);
        } else {
            rm = this.rm;
            rm.setName(titleSave);
            rm.setCategory(categorySave);
            rm.setServings(servingsSave);
            rm.setSource_url(sourceUrlSave);
        }
        ArrayList<IngredientsModel> ims = getIngredientList(recipeId);
        DirectionsModel dm = new DirectionsModel(recipeId, directionsSave);
        ArrayList<TagsModel> tms = new ArrayList<>(mutable_tms.getValue());

        if (recipeId == -1){
            Log.d(TAG, "Attempting to INSERT new recipe");
            rbr.insertRecipe(rm, ims, tms, dm);
        } else {
            Log.d(TAG, "Updating recipe with ID: " + recipeId);
            rbr.updateRecipe(rm, ims, tms, dm);
        }

        setContentView(R.layout.activity_is_loading);
    }

    @Subscribe
    public void onRecipeSaved(RecipeSavedEvent recipeSavedEvent){
        Log.i(TAG, "EVENT IS: " + recipeSavedEvent.recipeAdded);
        if(recipeSavedEvent.recipeAdded){
            setResult(Activity.RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, getString(R.string.recipe_save_failed), Toast.LENGTH_LONG).show();
            setContentView(R.layout.activity_add_edit_recipe);
        }
    }
}