package com.jaf.recipebook.helpers;

import android.app.Activity;
import android.content.Context;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.common.api.Scope;

public class GoogleSignInHelper {

    private static final String TAG = "GoogleSignInHelper";
    public static final String APP_NAME = "appName";
    public static final int REQUEST_CODE_SIGN_IN = 100;

    public static DriveServiceHelper getDriveServiceHelper(Activity activity, boolean attemptSignIn){
        GoogleSignInAccount account = getGoogleSignInAccount(activity);
        if (account == null) {
            if (attemptSignIn){
                signIn(activity);
            }
            return null;
        } else {
            return new DriveServiceHelper(DriveServiceHelper
                    .getGoogleDriveService(activity, account, APP_NAME), activity);
        }
    }

    public static DriveServiceHelper getDriveServiceHelper(Activity activity, GoogleSignInAccount gsa) {
        return new DriveServiceHelper(DriveServiceHelper
                .getGoogleDriveService(activity, gsa, APP_NAME), activity);
    }

    public static GoogleSignInAccount getGoogleSignInAccount(Activity activity) {
        return GoogleSignIn.getLastSignedInAccount(activity);
    }

    public static void signIn(Activity activity) {
        activity.startActivityForResult(getGoogleSignInClient(activity).getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    public static GoogleSignInOptions getGoogleSignInOptions() {
        return new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                        .requestScopes(new Scope(Scopes.DRIVE_FILE))
                        .requestEmail()
                        .build();
    }

    public static GoogleSignInClient getGoogleSignInClient(Context context) {
        GoogleSignInOptions signInOptions = getGoogleSignInOptions();
        return GoogleSignIn.getClient(context, signInOptions);
    }
}
