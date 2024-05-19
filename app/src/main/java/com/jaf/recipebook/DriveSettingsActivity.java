package com.jaf.recipebook;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.PopupWindow;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.jaf.recipebook.db.RecipeBookDatabase;
import com.jaf.recipebook.events.DbRefreshEvent;
import com.jaf.recipebook.events.DriveDataDeletedEvent;
import com.jaf.recipebook.events.DriveDbLastModifiedEvent;
import com.jaf.recipebook.events.DriveUploadCompeleteEvent;
import com.jaf.recipebook.helpers.DriveServiceHelper;
import com.jaf.recipebook.helpers.FileHelper;
import com.jaf.recipebook.helpers.GeneralHelper;
import com.jaf.recipebook.helpers.GoogleSignInHelper;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class DriveSettingsActivity extends AppCompatActivity {

    public final String TAG = "DriveSettingsActivity";

    private Button uploadBtn;
    private Button downloadBtn;
    private Button deleteBtn;
    private SwitchMaterial autoBackupToggle;
    private TextView lastUpdatedTextView;
    private ProgressBar progressBar;

    private DriveServiceHelper mDriveServiceHelper;
    private FileHelper fh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        fh = new FileHelper(this);
        setContentView(R.layout.activity_drive_settings);

        uploadBtn = findViewById(R.id.drive_setting_upload_btn);
        downloadBtn = findViewById(R.id.drive_setting_download_btn);
        autoBackupToggle = findViewById(R.id.auto_backup_toggle);
        deleteBtn = findViewById(R.id.drive_setting_delete_btn);
        lastUpdatedTextView = findViewById(R.id.drive_setting_last_updated);
        progressBar = findViewById(R.id.drive_settings_progress_bar);

        uploadBtn.setOnClickListener(onUploadClicked());
        downloadBtn.setOnClickListener(onDownloadClicked());

        autoBackupToggle.setChecked(fh.getPreference(fh.AUTO_BACKUP_ACTIVE_PREFERENCE, false));
        autoBackupToggle.setOnCheckedChangeListener(onBackupDataToggled());

        deleteBtn.setOnClickListener(onDeleteClicked());

        mDriveServiceHelper = GoogleSignInHelper.getDriveServiceHelper(this, true);
    }

    @Override
    protected void onStart() {
        super.onStart();

        if(!EventBus.getDefault().isRegistered(this)){
            EventBus.getDefault().register(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setPendingState(true);
        refreshLastUpdate();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    private void refreshLastUpdate() {
        mDriveServiceHelper.getLastBackupDate();
    }

    private void setPendingState(boolean isPending) {

        if (isPending){
            lastUpdatedTextView.setText(this.getString(R.string.last_synced_text));
            progressBar.setVisibility(View.VISIBLE);
            uploadBtn.setBackgroundTintList(ColorStateList.valueOf(this.getColor(R.color.inactive)));
            downloadBtn.setBackgroundTintList(ColorStateList.valueOf(this.getColor(R.color.inactive)));
            deleteBtn.setBackgroundTintList(ColorStateList.valueOf(this.getColor(R.color.inactive)));
        } else {
            progressBar.setVisibility(View.GONE);
            uploadBtn.setBackgroundTintList(ColorStateList.valueOf(this.getColor(R.color.secondary)));
            downloadBtn.setBackgroundTintList(ColorStateList.valueOf(this.getColor(R.color.secondary)));
            deleteBtn.setBackgroundTintList(ColorStateList.valueOf(this.getColor(R.color.danger)));
        }

        uploadBtn.setEnabled(!isPending);
        downloadBtn.setEnabled(!isPending);
        deleteBtn.setEnabled(!isPending);
    }

    private View.OnClickListener onUploadClicked() {
        return view -> {
            setPendingState(true);
            Toast.makeText(view.getContext(), "Uploading...", Toast.LENGTH_LONG).show();
            mDriveServiceHelper.upload();
        };
    }

    private View.OnClickListener onDownloadClicked() {
        return view -> {
            setPendingState(true);
            Toast.makeText(view.getContext(), "Downloading...", Toast.LENGTH_LONG).show();
            RecipeBookDatabase.stopDb();
            mDriveServiceHelper.download();
        };
    }

    private CompoundButton.OnCheckedChangeListener onBackupDataToggled() {
        return (buttonView, isChecked) -> {
            fh.setPreference(fh.AUTO_BACKUP_ACTIVE_PREFERENCE, isChecked);
            if (isChecked){
                setPendingState(true);
                mDriveServiceHelper.upload();
            }
        };
    }

    private View.OnClickListener onDeleteClicked() {
        return view -> spawnWarningPopup(view);
    }

    private void spawnWarningPopup(View view){
        PopupWindow popupWindow = GeneralHelper.popupInflator(this, view, R.layout.popup_drive_deletion_warning);

        popupWindow.getContentView().findViewById(R.id.delete_drive_data_final_btn).setOnLongClickListener(v -> {
            Toast.makeText(v.getContext(), "Deleting...", Toast.LENGTH_LONG).show();
            mDriveServiceHelper.delete();
            setPendingState(true);
            popupWindow.dismiss();
            return false;
        });
    }
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case GoogleSignInHelper.REQUEST_CODE_SIGN_IN:
                if (resultCode == Activity.RESULT_OK && resultData != null) {
                    handleSignInResult(resultData);
                }
                break;
        }
        super.onActivityResult(requestCode, resultCode, resultData);
    }

    private void handleSignInResult(Intent result) {
        GoogleSignIn.getSignedInAccountFromIntent(result)
            .addOnSuccessListener(new OnSuccessListener<GoogleSignInAccount>() {
                @Override
                public void onSuccess(GoogleSignInAccount googleSignInAccount) {
                    mDriveServiceHelper = new DriveServiceHelper(DriveServiceHelper
                            .getGoogleDriveService( getApplicationContext(),
                                    googleSignInAccount,
                                    GoogleSignInHelper.APP_NAME), null);

                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Unable to sign in.", e);
                }
            });
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void getLastUploadedString(DriveDbLastModifiedEvent driveDbLastModifiedEvent){
        setPendingState(false);
        lastUpdatedTextView.setText(driveDbLastModifiedEvent.status);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void downloadDone(DbRefreshEvent dbRefreshEvent){
        onResume();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void uploadDone(DriveUploadCompeleteEvent driveUploadCompeleteEvent){
        onResume();
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void driveDataDoneDeleting(DriveDataDeletedEvent ddde){
        onResume();
    }

}