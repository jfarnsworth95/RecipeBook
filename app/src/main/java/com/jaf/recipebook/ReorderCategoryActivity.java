package com.jaf.recipebook;

import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.jaf.recipebook.adapters.CategoryViewAdapter;
import com.jaf.recipebook.db.RecipeBookDatabase;
import com.jaf.recipebook.helpers.FileHelper;
import com.jaf.recipebook.helpers.GeneralHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class ReorderCategoryActivity extends AppCompatActivity {
    private final String TAG = "JAF-ReorderCategoryActivity";

    private FileHelper fh;
    private RecipeBookDatabase rbdb;
    private HashSet<String> currentCategories;
    private Handler mainHandler;
    private MutableLiveData<ArrayList<String>> categoriesOrdered;

    private CategoryViewAdapter cva;
    private Button confirmBtn;
    private Button cancelBtn;
    private RecyclerView rv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_is_loading);
        categoriesOrdered = new MutableLiveData<>();
        mainHandler = new Handler(getMainLooper());
        fh = new FileHelper(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        rbdb = RecipeBookDatabase.getInstance(this);
        fetchCategories();
    }

    private void setClassVars() {
        rv = findViewById(R.id.category_reorder_rv);
        confirmBtn = findViewById(R.id.confirm_reorder_btn);
        cancelBtn = findViewById(R.id.cancel_reorder_btn);
    }

    private void setViewAdapter() {
        cva = new CategoryViewAdapter(new CategoryViewAdapter.CategoryDiff());
        rv.setAdapter(cva);
        rv.setClickable(true);
        categoriesOrdered.observe(this, cva::submitList);
        ItemTouchHelper ith = new ItemTouchHelper(simpleCallback());
        ith.attachToRecyclerView(rv);
    }

    private void setListeners() {
        confirmBtn.setOnClickListener(v -> {
            StringBuilder sb = new StringBuilder();
            for (String category : categoriesOrdered.getValue()) {
                sb.append(category).append(",");
            }
            sb.setLength(sb.length() - 1);
            fh.setPreference(fh.CATEGORY_ORDER_PREFERENCE, String.valueOf(sb));
            finish();
        });

        cancelBtn.setOnClickListener(v -> {
            finish();
        });
    }

    private void fetchCategories() {
        rbdb.getQueryExecutor().execute(() -> {
            currentCategories = new HashSet<>(rbdb.recipeDao().getDistinctCategories());
            currentCategories.remove(null);

            mainHandler.post(this::renderList);
        });
    }

    private ItemTouchHelper.SimpleCallback simpleCallback(){
        return new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {
            @Override
            public boolean onMove(
                    @NonNull RecyclerView recyclerView,
                    @NonNull RecyclerView.ViewHolder viewHolder,
                    @NonNull RecyclerView.ViewHolder target) {
                int startPosition = viewHolder.getAdapterPosition();
                int endPosition = target.getAdapterPosition();

                ArrayList<String> copyCO = categoriesOrdered.getValue();
                assert copyCO != null;
                Collections.swap(copyCO, startPosition, endPosition);

                categoriesOrdered.postValue(copyCO);
                cva.notifyItemMoved(startPosition, endPosition);
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) { }
        };
    }

    private void renderList() {
        GeneralHelper.ensureCategoryPrefUpdated(currentCategories, fh);
        setContentView(R.layout.activity_reorder_category);
        setClassVars();
        setListeners();
        setViewAdapter();
        categoriesOrdered.postValue(
                new ArrayList<>(Arrays.asList(
                        fh.getPreference(fh.CATEGORY_ORDER_PREFERENCE, "")
                        .split(",")
                ))
        );
    }

}