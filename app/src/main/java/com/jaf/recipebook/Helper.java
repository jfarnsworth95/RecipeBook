package com.jaf.recipebook;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;

import java.io.File;

public class Helper {

    public SharedPreferences appPreferences;
    public Context context;

    public Helper(Context context){
        this.context = context;
        appPreferences = getSharedPreferences();
    }

    public SharedPreferences getSharedPreferences(){
        return context.getSharedPreferences(context.getString(R.string.preference_file_key),
                                                              Context.MODE_PRIVATE);
    }
    
}
