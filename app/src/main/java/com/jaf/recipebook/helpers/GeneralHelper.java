package com.jaf.recipebook.helpers;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.appcompat.content.res.AppCompatResources;

import com.jaf.recipebook.R;
import com.jaf.recipebook.db.RecipeBookDatabase;
import com.jaf.recipebook.db.ingredients.IngredientsModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

public class GeneralHelper {
    public static final int ACTIVITY_RESULT_DB_ERROR = 10001;
    public static final int ACTIVITY_RESULT_DELETE_RECIPE = 10002;
    public static final int ACTIVITY_RESULT_UPDATE_SEARCH = 10003;
    public static final int ACTIVITY_RESULT_SIGN_IN_PROMPT = 10003;

    public static StringBuilder convertIngredientModelArrayToString(List<IngredientsModel> ims){
        StringBuilder ingredientSb = new StringBuilder();
        for (IngredientsModel im : ims){
            ingredientSb.append(im.getText());
            ingredientSb.append(System.lineSeparator());
        }
        ingredientSb.deleteCharAt(ingredientSb.length() - 1);
        return ingredientSb;
    }

    public static PopupWindow popupInflator(Context context, View onClickView, int popupLayout){
        LayoutInflater inflater = (LayoutInflater)
                context.getSystemService(LAYOUT_INFLATER_SERVICE);
        View popupView = inflater.inflate(popupLayout, null);
        popupView.setBackground(AppCompatResources.getDrawable(context, android.R.drawable.picture_frame));

        // create the popup window
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow popupWindow = new PopupWindow(popupView, width, height, true);

        // show the popup window
        // which view you pass in doesn't matter, it is only used for the window token
        popupWindow.showAtLocation(onClickView, Gravity.CENTER, 0, 0);

        // dismiss the popup window when touched
        popupView.setOnTouchListener((v, event) -> {
            popupWindow.dismiss();
            return true;
        });

        // Dim background when active
        View parentView = (View) popupWindow.getContentView().getParent();
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        WindowManager.LayoutParams p = (WindowManager.LayoutParams) parentView.getLayoutParams();
        p.flags |= WindowManager.LayoutParams.FLAG_DIM_BEHIND;
        p.dimAmount = 0.5f;
        wm.updateViewLayout(parentView, p);

        return popupWindow;
    }

    public static ArrayList<String> getCategoryOrderPreference(FileHelper fh){
        String stringyCategoryOrder = fh.getPreference(fh.CATEGORY_ORDER_PREFERENCE, "");
        return new ArrayList<>(Arrays.asList(stringyCategoryOrder.split(",")));
    }

    public static void ensureCategoryPrefUpdated(HashSet<String> recipeCategories, FileHelper fh){
        ArrayList<String> currentList = getCategoryOrderPreference(fh);
        HashSet<String> currentPreferenceSet = new HashSet<>(currentList);

        if (!currentPreferenceSet.equals(recipeCategories)) {
            StringBuilder rebuildList = new StringBuilder();
            for (String category : currentList) {
                if (recipeCategories.contains(category)) {
                    rebuildList.append(category).append(",");
                }
            }
            recipeCategories.removeAll(currentPreferenceSet);
            for (String category : recipeCategories) {
                rebuildList.append(category).append(",");
            }
            if (rebuildList.length() > 0 && rebuildList.length() > 0) {
                rebuildList.setLength(rebuildList.length() - 1);
            }
            fh.setPreference(fh.CATEGORY_ORDER_PREFERENCE, rebuildList.toString());
        }
    }

    public static Animation createFlashAnimation(int repeatCount){
        Animation flashField = new AlphaAnimation(1.0f, 0.0f);
        flashField.setDuration(300);
        flashField.setStartOffset(100);
        flashField.setRepeatMode(Animation.REVERSE);
        flashField.setRepeatCount(repeatCount * 2);
        return flashField;
    }

    public static void backgroundHighlightAnimation(Context context, View viewToAnim, Handler handler) {
        ColorDrawable startColor = new ColorDrawable(context.getColor(R.color.transparent));
        Drawable endColor = context.getDrawable(R.drawable.rounded_edit_text);
        TransitionDrawable td = new TransitionDrawable(new Drawable[]{startColor, endColor});
        viewToAnim.setBackground(td);
        td.startTransition(2000);
        handler.postDelayed(() -> td.reverseTransition(2000), 2000);
    }

}
