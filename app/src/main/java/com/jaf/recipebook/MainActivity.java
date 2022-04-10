package com.jaf.recipebook;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.splashscreen.SplashScreen;

import android.os.Bundle;
import android.util.Log;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Thread.sleep(2000);
        }catch (InterruptedException ex){
            Log.d("SplashRender", "onCreate: Failed to sleep...");
        }

        SplashScreen splash = SplashScreen.installSplashScreen(this);
        setContentView(R.layout.activity_start);
    }
}