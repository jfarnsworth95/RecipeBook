package com.jaf.recipebook;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.tasks.Task;

import de.hdodenhof.circleimageview.CircleImageView;

public class SettingsActivity extends AppCompatActivity {

    FileHelper fileHelper;
    public final String TAG = "JAF-SETTINGS";
    public final int signInCode = 101;
    GoogleSignInClient gsc;
    GoogleSignInOptions gso;
    GoogleSignInAccount gsa;

    private CircleImageView googlePhotoImg;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fileHelper = new FileHelper(this);
        setContentView(R.layout.activity_settings);

        //Initialize CircleImageView
        googlePhotoImg = (CircleImageView) findViewById(R.id.google_account_image);

        // Google Sign-in vars
        gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                .build();
        gsc = GoogleSignIn.getClient(this, gso);

        // Add button listener for Changing External Storage
        findViewById(R.id.toggle_external_storage_btn).setOnClickListener(view ->
            startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                .addCategory("android.intent.category.DEFAULT")
                .setData(Uri.fromParts("package", getPackageName(), null))
        ));

        // Add button listener for Google Sign In
        findViewById(R.id.google_sign_in_button).setOnClickListener(view -> {
            if (gsa == null) {
                startActivityForResult(gsc.getSignInIntent(), signInCode);
            } else {
                gsc.signOut().addOnCompleteListener(this, task -> onStart());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // On return from permission request activity, set preference
        // based on if the permission was granted or not
        fileHelper.setPreference(fileHelper.EXTERNAL_STORAGE_PREFERENCE,
                                Environment.isExternalStorageManager());
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check for existing Google Sign In account, if the user is already signed in
        // the GoogleSignInAccount will be non-null.
        gsa = GoogleSignIn.getLastSignedInAccount(this);
        updateUi();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == signInCode){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                gsa = task.getResult(ApiException.class);
                updateUi();
            } catch (ApiException e) {
                Log.e(TAG, "onActivityResult: Failed to log user in.", e);
                Toast.makeText(this, "Something went wrong when signing you in...", Toast.LENGTH_LONG).show();
            }
        }
    }

    private void updateUi() {
        if (gsa == null){
            ((Button) findViewById(R.id.google_sign_in_button)).setText(R.string.google_sign_in_button);
            ((TextView) findViewById(R.id.google_user_name)).setVisibility(View.GONE);
            ((TextView) findViewById(R.id.sign_in_descriptor)).setVisibility(View.GONE);
            googlePhotoImg.setVisibility(View.GONE);
        }else{
            ((Button) findViewById(R.id.google_sign_in_button)).setText(R.string.google_sign_out_button);
            if (gsa.getPhotoUrl() != null) {
                Log.i(TAG, "updateUi: User profile found, assigning to Image View.");
                Glide.with(SettingsActivity.this).load(gsa.getPhotoUrl()).into(googlePhotoImg);
                googlePhotoImg.setVisibility(View.VISIBLE);
                TextView userDisplayName = findViewById(R.id.google_user_name);
                userDisplayName.setText(gsa.getDisplayName());
                userDisplayName.setVisibility(View.VISIBLE);
                ((TextView) findViewById(R.id.sign_in_descriptor)).setVisibility(View.VISIBLE);
            } else {
                Log.i(TAG, "updateUi: No user profile photo found.");
            }
        }
    }
}