package com.jaf.recipebook;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.chip.Chip;
import com.google.android.material.textfield.TextInputEditText;
import com.jaf.recipebook.db.RecipeBookDatabase;
import com.jaf.recipebook.db.RecipeBookRepo;
import com.jaf.recipebook.db.directions.DirectionsModel;
import com.jaf.recipebook.db.ingredients.IngredientsModel;
import com.jaf.recipebook.db.recipes.RecipesModel;
import com.jaf.recipebook.db.tags.TagsModel;
import com.jaf.recipebook.tagAdapters.TagViewAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class AddEditRecipe extends AppCompatActivity {
    final String TAG = "JAF-AddEditRecipe";

    long recipeId;
    private RecipeBookDatabase rbdb;
    private RecipeBookRepo rbr;

    private RecipesModel rm;
    private List<IngredientsModel> ims;
    private DirectionsModel dm;
    private List<TagsModel> tms;

    private MutableLiveData<List<TagsModel>> mutable_tms = new MutableLiveData<>(new ArrayList<>());

    private TextInputEditText titleInput;
    private TextInputEditText ingredientInput;
    private TextInputEditText directionInput;
    private TextInputEditText servingsInput;
    private TextInputEditText sourceUrlInput;
    private TextInputEditText categoryInput;
    private TextInputEditText tagsInput;
    private RecyclerView chipGroup;

    private Handler mainHandler;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        recipeId = getIntent().getLongExtra("recipe_id", -1);
        rbdb = RecipeBookDatabase.getInstance(this);
        rbr = new RecipeBookRepo(rbdb);
        mainHandler = new Handler(getMainLooper());

        if (recipeId >= 0){
            setContentView(R.layout.activity_is_loading);
            queryForData();
        } else {
            setContentView(R.layout.activity_add_edit_recipe);
            Objects.requireNonNull(this.getSupportActionBar()).setTitle(R.string.add_new_recipe);
        }

        getInputFields();
        setupServingsListener();
        setupChipRecycler();
        setupTagTextWatcher();
    }

    private void getInputFields(){
        titleInput = findViewById(R.id.textInput_RecipeTitleInput);
        ingredientInput = findViewById(R.id.textInput_RecipeIngredientsInput);
        directionInput = findViewById(R.id.textInput_RecipeDirectionsInput);
        servingsInput = findViewById(R.id.textInput_RecipeServingsInput);
        sourceUrlInput = findViewById(R.id.textInput_RecipeSourceUrlInput);
        categoryInput = findViewById(R.id.textInput_RecipeCategoryInput);
        chipGroup = findViewById(R.id.chipLayout_recipeTags);
        tagsInput = findViewById(R.id.textInput_RecipeTagsInput);
    }

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

    private void setupChipRecycler(){
        final TagViewAdapter tla = new TagViewAdapter(new TagViewAdapter.TagDiff(),
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
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
                    }
                });
        chipGroup.setAdapter(tla);
        chipGroup.setClickable(true);
        mutable_tms.observe(this, tags -> {
            Log.i(TAG, "OBSERVE TRIGGERED");
            Log.i(TAG, tags.toString());
            tla.submitList(tags);
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_add_edit, menu);
        return true;
    }

    private void queryForData(){
//        mExecutor.execute(() -> {
//            FullRecipeTuple frt = null;
//            try {
//                frt = rbr.getFullRecipeData(recipeId);
//            } catch (Exception ex){
//                Log.e(TAG, "Failed to query recipe", ex);
//            } finally {
//                if (frt != null) {
//                    rm = frt.recipesModel;
//                    ims = frt.ingredientsModel;
//                    dm = frt.directionsModel;
//                    tms = frt.tagsModel;
//                    mainHandler.post(this::renderRecipe);
//                } else {
//                    // TODO Kick back to main activity with error Toast
//                    setResult(Activity.RESULT_CANCELED);
//                    this.finish();
//                }
//            }
//        });
    }

    private void renderRecipe(){
        setContentView(R.layout.activity_add_edit_recipe);
        Objects.requireNonNull(this.getSupportActionBar()).setTitle(rm.getName());

        titleInput.setText(rm.getName());
        directionInput.setText(dm.getText());
        sourceUrlInput.setText(rm.getSource_url());
        categoryInput.setText(rm.getCategory());

        if (rm.getServings() != null){
            servingsInput.setText(String.format(String.valueOf(rm.getServings())));
        }

        StringBuilder ingredientSb = new StringBuilder();
        for (IngredientsModel im : ims){
            ingredientSb.append(im.getText());
            ingredientSb.append("\n");
        }
        ingredientSb.deleteCharAt(ingredientSb.length() - 1);
        ingredientInput.setText(ingredientSb);

//        tla = new TagViewAdapter(this, new ArrayList<>(tms));

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

    private void setupTagTextWatcher(){
        tagsInput.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                String tagText = v.getText().toString().trim().toLowerCase();
                if (Pattern.compile("^\\s*$").matcher(tagText).find()) {
                    return false;
                } else if (tagInList(tagText)){
                    Toast.makeText(AddEditRecipe.this, getString(R.string.recipe_tag_already_exists), Toast.LENGTH_SHORT).show();
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

    private boolean tagInList(String inputtedTag){
        for(TagsModel tag : Objects.requireNonNull(mutable_tms.getValue())){
            if (tag.getTag().equals(inputtedTag)){
                return true;
            }
        }
        return false;
    }

    private void onSave(){
        //servingsInputAsFloat = Float.valueOf(servingsInput.getText().toString());
        //RecipesModel recipesModel = new RecipesModel(titleInput.toString(), categoryInput.toString(), servingsInput.getDecimal())
    }
}