package com.jaf.recipebook;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.Scope;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;

import java.io.File;
import java.util.List;

public class GoogleSignInActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SIGN_IN = 100;
    private static final String TAG = "GoogleSignInActivity";

    private GoogleSignInClient mGoogleSignInClient;
    private DriveServiceHelper mDriveServiceHelper;
    private FileHelper fileHelper;

    private Button deleteAllFilesBtn;
    private Button runCurrentTestBtn;
//    private Button searchFile;
//    private Button searchFolder;
//    private Button createTextFile;
//    private Button createFolder;
//    private Button uploadFile;
//    private Button downloadFile;
//    private Button deleteDriveFile;
//    private Button syncDrive;
//    private Button queryFiles;
//    private Button createLocalFile;
//    private Button readFile;
//    private Button saveFile;
    private TextView email;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_sign_in);
        initView();

        deleteAllFilesBtn.setOnLongClickListener(new View.OnLongClickListener() {
             @Override
             public boolean onLongClick(View view) {
                 File[] files = fileHelper.getAllFiles();
                 for (File file : files){
                     fileHelper.deleteFile(file.getName());
                 }
                 return true;
             }
        });

        runCurrentTestBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Toast.makeText(view.getContext(), "RUNNING TEST", Toast.LENGTH_LONG).show();
                fileHelper.validateRecipeFileFormat(fileHelper.getFile("Beef_Stew.rp"));
            }
        });

//        createTextFile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDriveServiceHelper == null) {
//                    return;
//                }
//                // you can provide  folder id in case you want to save this file inside some folder.
//                // if folder id is null, it will save file to the root
//                mDriveServiceHelper.createTextFile("test_newtype.txt", "some text", "1z0BgFNQJ9ZXh07YfIiTVhuZyvYEzPAmW")
//                        .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
//                            @Override
//                            public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
//                                Gson gson = new Gson();
//                                Log.w(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.w(TAG, "onFailure: " + e.getMessage());
//                            }
//                        });
//            }
//        });
//
//        createFolder.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDriveServiceHelper == null) {
//                    return;
//                }
//                // you can provide  folder id in case you want to save this file inside some folder.
//                // if folder id is null, it will save file to the root
//                mDriveServiceHelper.createFolder("RecipeBook", null)
//                        .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
//                            @Override
//                            public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
//                                Gson gson = new Gson();
//                                Log.w(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.w(TAG, "onFailure: " + e.getMessage());
//
//                            }
//                        });
//            }
//        });
//
//        searchFile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDriveServiceHelper == null) {
//                    return;
//                }
//                mDriveServiceHelper.searchFile("RecipeBook", "application/vnd.google-apps.folder")
//                        .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
//                            @Override
//                            public void onSuccess(List<GoogleDriveFileHolder> googleDriveFileHolders) {
//                                GoogleDriveFileHolder file = googleDriveFileHolders.get(0);
//
//                                Gson gson = new Gson();
//                                Log.w(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolders));
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.w(TAG, "onFailure: " + e.getMessage());
//                            }
//                        });
//
//            }
//        });
//
//        searchFolder.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDriveServiceHelper == null) {
//                    return;
//                }
//
//                mDriveServiceHelper.searchFolder("testDummy")
//                        .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
//                            @Override
//                            public void onSuccess(List<GoogleDriveFileHolder> googleDriveFileHolders) {
//                                Gson gson = new Gson();
//                                Log.w(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolders));
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.w(TAG, "onFailure: " + e.getMessage());
//                            }
//                        });
//            }
//        });
//
//        uploadFile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//
//                if (mDriveServiceHelper == null) {
//                    return;
//                }
//                mDriveServiceHelper.uploadFile(fileHelper.getFile("test_newtype.txt"), "text/plain", null, "1z0BgFNQJ9ZXh07YfIiTVhuZyvYEzPAmW")
//                        .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
//                            @Override
//                            public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
//                                Gson gson = new Gson();
//                                Log.w(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.w(TAG, "onFailure: " + e.getMessage());
//                            }
//                        });
//            }
//        });
//
//        downloadFile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDriveServiceHelper == null) {
//                    return;
//                }
//                mDriveServiceHelper.downloadFile(fileHelper.getAppLocalDataFolder(),
//                                            "1q9ooCmF8sEz2bpscbua9D-LusmJST1Fe", "downloaded.txt")
//                        .addOnSuccessListener(new OnSuccessListener<Void>() {
//                            @Override
//                            public void onSuccess(Void aVoid) {
//                                Log.i(TAG, "onSuccess: Downloaded File");
//                            }
//                        })
//                        .addOnFailureListener(new OnFailureListener() {
//                            @Override
//                            public void onFailure(@NonNull Exception e) {
//                                Log.w(TAG, "onFailure: FAILED TO GRAB FILE", e);
//                            }
//                        });
//            }
//        });
//
//        Context context = this; // Passed into Sync Drive listener
//        deleteDriveFile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                File fileToDelete = new File(fileHelper.getAppLocalDataFolder(), "French Toast.rp");
//                DriveSyncHelper.deleteFileFromDriveAndLocal(mDriveServiceHelper, fileToDelete);
//            }
//        });
//
//        syncDrive.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                DriveSyncHelper.syncLocalWithDrive(mDriveServiceHelper, context);
//            }
//        });
//
//        queryFiles.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDriveServiceHelper == null) {
//                    return;
//                }
//                try {
//                    mDriveServiceHelper.queryFiles()
//                            .addOnSuccessListener(new OnSuccessListener<FileList>() {
//                                @Override
//                                public void onSuccess(FileList fileList) {
//                                    Gson gson = new Gson();
//                                    Log.w(TAG, "onSuccess: " + gson.toJson(fileList));
//                                }
//                            })
//                            .addOnFailureListener(new OnFailureListener() {
//                                @Override
//                                public void onFailure(@NonNull Exception e) {
//                                    Log.w(TAG, "onFailure: " + e.getMessage());
//                                }
//                            });
//                } catch (Exception ex){
//                    Log.e(TAG, "onClick: Drive Exception", ex);
//                }
//
//            }
//        });
//
//        createLocalFile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                fileHelper.createFileUri("test.txt", "My test content.");
//            }
//        });
//
//        readFile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDriveServiceHelper == null) {
//                    return;
//                }
//                try {
//                    mDriveServiceHelper.readFile("1q9ooCmF8sEz2bpscbua9D-LusmJST1Fe")
//                            .addOnSuccessListener(new OnSuccessListener<Pair<String, String>>() {
//                                @Override
//                                public void onSuccess(Pair<String, String> readPair) {
//                                    Log.i(TAG, "onSuccess: " + readPair.toString());
//                                }
//                            })
//                            .addOnFailureListener(new OnFailureListener() {
//                                @Override
//                                public void onFailure(@NonNull Exception e) {
//                                    Log.w(TAG, "onFailure: " + e.getMessage());
//                                }
//                            });
//                } catch (Exception ex){
//                    Log.e(TAG, "onClick: Drive Exception", ex);
//                }
//
//            }
//        });
//
//        saveFile.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                if (mDriveServiceHelper == null) {
//                    return;
//                }
//                try {
//                    mDriveServiceHelper.saveFile("1q9ooCmF8sEz2bpscbua9D-LusmJST1Fe", "test1.txt", "UPDATED TEXT.")
//                            .addOnSuccessListener(new OnSuccessListener<com.google.api.services.drive.model.File>() {
//                                @Override
//                                public void onSuccess(com.google.api.services.drive.model.File fileList) {
//                                    Log.w(TAG, "onSuccess: ");
//                                }
//                            })
//                            .addOnFailureListener(new OnFailureListener() {
//                                @Override
//                                public void onFailure(@NonNull Exception e) {
//                                    Log.w(TAG, "onFailure: " + e.getMessage());
//                                }
//                            });
//                } catch (Exception ex){
//                    Log.e(TAG, "onClick: Drive Exception", ex);
//                }
//
//            }
//        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        fileHelper = new FileHelper(this);
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(getApplicationContext());
        if (account == null) {
            signIn();
        } else {
            email.setText(account.getEmail());
            mDriveServiceHelper = new DriveServiceHelper(DriveServiceHelper
                .getGoogleDriveService(getApplicationContext(), account, "appName"), this);
        }
    }

    private void signIn() {
        mGoogleSignInClient = buildGoogleSignInClient();
        startActivityForResult(mGoogleSignInClient.getSignInIntent(), REQUEST_CODE_SIGN_IN);
    }

    private GoogleSignInClient buildGoogleSignInClient() {
        GoogleSignInOptions signInOptions =
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestScopes(new Scope(Scopes.DRIVE_APPFOLDER))
                        .requestEmail()
                        .build();
        return GoogleSignIn.getClient(getApplicationContext(), signInOptions);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        switch (requestCode) {
            case REQUEST_CODE_SIGN_IN:
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
                    Log.w(TAG, "Signed in as " + googleSignInAccount.getEmail());
                    email.setText(googleSignInAccount.getEmail());

                    mDriveServiceHelper = new DriveServiceHelper(DriveServiceHelper
                            .getGoogleDriveService( getApplicationContext(),
                                                    googleSignInAccount,
                                                    "appName"), null);

                    Log.w(TAG, "handleSignInResult: " + mDriveServiceHelper);
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Unable to sign in.", e);
                }
            });
    }

    private void initView() {
        email = findViewById(R.id.email);
        deleteAllFilesBtn = findViewById(R.id.delete_all_files_button);
        runCurrentTestBtn = findViewById(R.id.run_current_test_button);
//        searchFile = findViewById(R.id.search_file);
//        searchFolder = findViewById(R.id.search_folder);
//        createTextFile = findViewById(R.id.create_text_file);
//        createFolder = findViewById(R.id.create_folder);
//        uploadFile = findViewById(R.id.upload_file);
//        downloadFile = findViewById(R.id.download_file);
//        deleteDriveFile = findViewById(R.id.delete_file_folder);
//        syncDrive = findViewById(R.id.sync_drive);
//        queryFiles = findViewById(R.id.query_files_btn);
//        createLocalFile = findViewById(R.id.create_local_file);
//        readFile = findViewById(R.id.read_file);
//        saveFile = findViewById(R.id.save_file);
    }
}
