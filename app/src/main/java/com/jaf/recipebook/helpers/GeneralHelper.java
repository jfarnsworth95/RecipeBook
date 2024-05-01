package com.jaf.recipebook.helpers;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.PopupWindow;

import androidx.appcompat.content.res.AppCompatResources;

import com.jaf.recipebook.R;
import com.jaf.recipebook.db.ingredients.IngredientsModel;

import java.util.List;

public class GeneralHelper {
    public static final int ACTIVITY_RESULT_DB_ERROR = 10001;
    public static final int ACTIVITY_RESULT_DELETE_RECIPE = 10002;

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
}
