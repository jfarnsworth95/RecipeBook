package com.jaf.recipebook;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.text.SpannableString;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.textview.MaterialTextView;
import com.jaf.recipebook.db.FullRecipeTuple;
import com.jaf.recipebook.db.RecipeBookDatabase;
import com.jaf.recipebook.db.RecipeBookRepo;
import com.jaf.recipebook.db.directions.DirectionsModel;
import com.jaf.recipebook.db.ingredients.IngredientsModel;
import com.jaf.recipebook.db.recipes.RecipesModel;
import com.jaf.recipebook.db.tags.TagsModel;
import com.jaf.recipebook.helpers.FileHelper;
import com.jaf.recipebook.helpers.GeneralHelper;
import com.jaf.recipebook.adapters.TagViewAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ViewRecipeActivity extends AppCompatActivity {
    final String TAG = "JAF-ViewRecipe";

    FileHelper fh;

    long recipeId;
    String recipeName;
    private RecipeBookDatabase rbdb;
    private RecipeBookRepo rbr;

    private RecipesModel rm;
    private List<IngredientsModel> ims;
    private DirectionsModel dm;
    private List<TagsModel> tms;

    MaterialTextView titleTv;
    MaterialTextView ingredientsTv;
    MaterialTextView directionsTv;
    MaterialTextView servingsTv;
    MaterialTextView sourceTv;
    MaterialTextView categoryTv;
    RecyclerView tagRecyclerView;

    LinearLayout ingredientsLl;
    LinearLayout directionsLl;
    LinearLayout servingsLl;
    LinearLayout sourceLl;
    LinearLayout categoryLl;
    MaterialTextView tagsHeaderTv;

    AlertDialog deleteConfirmationDialog;

    private ActivityResultLauncher<Intent> addEditActivityResultLauncher;
    private MutableLiveData<List<TagsModel>> mutable_tms = new MutableLiveData<>(new ArrayList<>());
    private Handler mainHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_is_loading);
        mainHandler = new Handler(getMainLooper());
        recipeId = getIntent().getLongExtra("recipe_id", -1);
        recipeName = getIntent().getStringExtra("recipe_name");

        fh = new FileHelper(this);
        Objects.requireNonNull(this.getSupportActionBar()).setTitle(recipeName);

        deleteConfirmationDialog = setupDeleteRecipeConfirmationDialog();

        prepareRegisterForEditActivity();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setContentView(R.layout.activity_is_loading);
        refreshData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_view, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch(item.getItemId()){
            case R.id.edit_recipe_btn:
                Intent intent = new Intent(ViewRecipeActivity.this, AddEditRecipeActivity.class);
                intent.putExtra("recipe_id", recipeId);
                addEditActivityResultLauncher.launch(intent);
                return true;

            case R.id.view_recipe_more_options:
//                moreOptionsDialog.show();
                return true;

            case R.id.view_recipe_copy_to_clipboard:
                copyToClipboard();
                return true;

            case R.id.view_recipe_save_to_downloads:
                fh.saveRecipeToDownloads(rm, ims, dm, tms, false);
                return true;

            case R.id.view_recipe_delete_recipe:
                deleteConfirmationDialog.show();
                return true;

            default:
                Log.w(TAG, "onOptionsItemSelected: Unknown Item ID for selected item: "
                        + item.toString());
                return super.onOptionsItemSelected(item);
        }
    }

    private void prepareRegisterForEditActivity(){
        addEditActivityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(), o -> {
                    if (o.getResultCode() == Activity.RESULT_OK){
                        Toast.makeText(this, getString(R.string.recipe_saved), Toast.LENGTH_SHORT).show();
                    } else if (o.getResultCode() == GeneralHelper.ACTIVITY_RESULT_DB_ERROR) {
                        Toast.makeText(this, getString(R.string.failed_to_open_recipe), Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void fetchViewHandles(){
        titleTv = findViewById(R.id.view_recipe_title_text);
        ingredientsTv = findViewById(R.id.view_ingredients_body);
        directionsTv = findViewById(R.id.view_directions_body);
        servingsTv = findViewById(R.id.view_servings_body);
        sourceTv = findViewById(R.id.view_source_url_body);
        categoryTv = findViewById(R.id.view_category_body);
        tagRecyclerView = findViewById(R.id.view_chipLayout_RecipeTags);

        ingredientsLl = findViewById(R.id.view_ingredients_container);
        directionsLl = findViewById(R.id.view_directions_container);
        servingsLl = findViewById(R.id.view_servings_container);
        sourceLl = findViewById(R.id.view_source_container);
        categoryLl = findViewById(R.id.view_category_container);
        tagsHeaderTv = findViewById(R.id.view_tags_header);
    }

    private void setupChipRecycler(){
        final TagViewAdapter tla = new TagViewAdapter(new TagViewAdapter.TagDiff(),
            v -> {
                Intent intent = new Intent();
                intent.putExtra("search_input", ((Chip) v).getText().toString());
                setResult(GeneralHelper.ACTIVITY_RESULT_UPDATE_SEARCH, intent);
                finish();
            }, false);
        tagRecyclerView.setAdapter(tla);
        tagRecyclerView.setClickable(false);
        mutable_tms.observe(this, tla::submitList);
    }

    private AlertDialog setupDeleteRecipeConfirmationDialog(){
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.dialog_recipe_view_delete_confirmation)
                .setPositiveButton(R.string.affirmative_text, (dialog, which) -> {
                    deleteRecipe();
                })
                .setNegativeButton(R.string.negative_text, (dialog, which) -> {
                });
        return builder.create();
    }

    private void refreshData(){
        if (rbdb == null){
            rbdb = RecipeBookDatabase.getInstance(this);
        }
        rbr = new RecipeBookRepo(rbdb);

        rbdb.getTransactionExecutor().execute(() -> {
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

    private void renderRecipe(){
        setContentView(R.layout.activity_view_recipe);
        fetchViewHandles();
        setupChipRecycler();

        Objects.requireNonNull(this.getSupportActionBar()).setTitle(rm.getName());

        // Set Title
        SpannableString spanString = new SpannableString(rm.getName());
        spanString.setSpan(new StyleSpan(Typeface.BOLD_ITALIC), 0, spanString.length(), 0);
        titleTv.setText(spanString);

        // Set Ingredients
        StringBuilder ingredientSb = GeneralHelper.convertIngredientModelArrayToString(ims);
        ingredientsTv.setText(ingredientSb);

        // Set Directions
        directionsTv.setText(dm.getText());

        // Set Servings, hide if empty
        if (rm.getServings() == null){
            servingsLl.setVisibility(View.GONE);
        } else {
            servingsLl.setVisibility(View.VISIBLE);

            String servingStr = String.format(String.valueOf(rm.getServings()));
            if (Integer.valueOf(servingStr.split("\\.")[1]) != 0){
                servingsTv.setText(servingStr);
            } else {
                servingsTv.setText(servingStr.split("\\.")[0]);
            }
        }

        // Set Source URL, hide if empty
        if (rm.getSource_url() == null){
            sourceLl.setVisibility(View.GONE);
        } else {
            sourceLl.setVisibility(View.VISIBLE);
            sourceTv.setText(rm.getSource_url());
        }

        // Set Category, hide if empty
        if (rm.getCategory() == null){
            categoryLl.setVisibility(View.GONE);
        } else {
            categoryLl.setVisibility(View.VISIBLE);
            categoryTv.setText(rm.getCategory());
        }

        // Update RecyclerView for tag listing, hide if empty
        if (tms == null || tms.isEmpty()){
            tagsHeaderTv.setVisibility(View.GONE);
            tagRecyclerView.setVisibility(View.GONE);
        } else {
            tagsHeaderTv.setVisibility(View.VISIBLE);
            tagRecyclerView.setVisibility(View.VISIBLE);
            mutable_tms.setValue(tms);
        }
    }

    private void copyToClipboard(){
        StringBuilder sb = new StringBuilder();

        // Get Name
        sb.append(recipeName)
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        // Insert Ingredients Header and spacing
        sb.append(getString(R.string.recipe_view_ingredients))
                .append(":")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        // Get Ingredients
        sb.append(ingredientsTv.getText().toString());

        // Insert Directions Header and spacing
        sb.append(System.lineSeparator())
                .append(System.lineSeparator())
                .append(getString(R.string.recipe_view_directions))
                .append(":")
                .append(System.lineSeparator())
                .append(System.lineSeparator());

        // Get Directions
        sb.append(directionsTv.getText().toString());

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Your Recipe as Text Block", sb);
        clipboard.setPrimaryClip(clip);
    }

    private void deleteRecipe(){
        setContentView(R.layout.activity_is_loading);
        rbdb.getTransactionExecutor().execute(() -> {
            rbr.deleteRecipe(rm, false);
            setResult(GeneralHelper.ACTIVITY_RESULT_DELETE_RECIPE);
            this.finish();
        });
    }
}