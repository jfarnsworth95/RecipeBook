package com.jaf.recipebook.helpers;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

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
        if (stringyCategoryOrder.isEmpty()){
            return new ArrayList<>();
        } else {
            return new ArrayList<>(Arrays.asList(stringyCategoryOrder.split(",")));
        }
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

    /** Defines the operation to be performed with the inset */
    public enum InsetUsage { IGNORE, PADDING, MARGIN }

    /** Defines the operation to be performed with each inset */
    public record InsetConfiguration(InsetUsage left, InsetUsage top, InsetUsage right, InsetUsage bottom) {}

    /**
     * Adds system window insets to the margin of a view.
     * @param view The view to add insets to
     * @param insetConfiguration The configuration of the insets
     */
    public static void handleInsets(View view, InsetConfiguration insetConfiguration) {
        // collect initial view values
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
        final Rect initialMargins = new Rect(
                layoutParams != null ? layoutParams.leftMargin : 0,
                layoutParams != null ? layoutParams.topMargin : 0,
                layoutParams != null ? layoutParams.rightMargin : 0,
                layoutParams != null ? layoutParams.bottomMargin : 0
        );
        final Rect initialPadding = new Rect(
                view.getPaddingLeft(),
                view.getPaddingTop(),
                view.getPaddingRight(),
                view.getPaddingBottom()
        );

        // add handler
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {

            // get the insets
            Insets systemInsets = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());

            // map the inset values to padding and margin
            Rect padding = new Rect(
                    insetConfiguration.left() == InsetUsage.PADDING ? systemInsets.left : 0,
                    insetConfiguration.top() == InsetUsage.PADDING ? systemInsets.top : 0,
                    insetConfiguration.right() == InsetUsage.PADDING ? systemInsets.right : 0,
                    insetConfiguration.bottom() == InsetUsage.PADDING ? systemInsets.bottom : 0
            );
            v.setPadding(
                    initialPadding.left + padding.left,
                    initialPadding.top + padding.top,
                    initialPadding.right + padding.right,
                    initialPadding.bottom + padding.bottom
            );

            Rect margin = new Rect(
                    insetConfiguration.left() == InsetUsage.MARGIN ? systemInsets.left : 0,
                    insetConfiguration.top() == InsetUsage.MARGIN ? systemInsets.top : 0,
                    insetConfiguration.right() == InsetUsage.MARGIN ? systemInsets.right : 0,
                    insetConfiguration.bottom() == InsetUsage.MARGIN ? systemInsets.bottom : 0
            );
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) v.getLayoutParams();
            if (params != null) {
                params.leftMargin = initialMargins.left + margin.left;
                params.topMargin = initialMargins.top + margin.top;
                params.rightMargin = initialMargins.right + margin.right;
                params.bottomMargin = initialMargins.bottom + margin.bottom;
                v.setLayoutParams(params);
            }

            return WindowInsetsCompat.CONSUMED;
        });
    }

}
