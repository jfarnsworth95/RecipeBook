package com.jaf.recipebook;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import android.util.Pair;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.google.android.gms.common.api.Scope;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.Scopes;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.services.drive.model.FileList;
import com.google.gson.Gson;

import java.io.File;
import java.util.List;

public class GoogleSignInActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_SIGN_IN = 100;
    private static final String TAG = "GoogleSignInActivity";

    private GoogleSignInClient mGoogleSignInClient;
    private DriveServiceHelper mDriveServiceHelper;
    private Helper helper;

    private LinearLayout gDriveAction;
    private Button searchFile;
    private Button searchFolder;
    private Button createTextFile;
    private Button createFolder;
    private Button uploadFile;
    private Button downloadFile;
    private Button deleteFileFolder;
    private TextView email;
    private Button viewFileFolder;
    private Button queryFiles;
    private Button createLocalFile;
    private Button readFile;
    private Button saveFile;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_sign_in);
        initView();

        createTextFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }
                // you can provide  folder id in case you want to save this file inside some folder.
                // if folder id is null, it will save file to the root
                mDriveServiceHelper.createTextFile("textfilename.txt", "some text", null)
                        .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                            @Override
                            public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                                Gson gson = new Gson();
                                Log.w(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "onFailure: " + e.getMessage());
                            }
                        });
            }
        });

        createFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }
                // you can provide  folder id in case you want to save this file inside some folder.
                // if folder id is null, it will save file to the root
                mDriveServiceHelper.createFolder("testDummyss", null)
                        .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                            @Override
                            public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                                Gson gson = new Gson();
                                Log.w(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "onFailure: " + e.getMessage());

                            }
                        });
            }
        });

        searchFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }
                mDriveServiceHelper.searchFile("textfilename.txt", "text/plain")
                        .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
                            @Override
                            public void onSuccess(List<GoogleDriveFileHolder> googleDriveFileHolders) {

                                Gson gson = new Gson();
                                Log.w(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolders));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "onFailure: " + e.getMessage());
                            }
                        });

            }
        });

        searchFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }

                mDriveServiceHelper.searchFolder("testDummy")
                        .addOnSuccessListener(new OnSuccessListener<List<GoogleDriveFileHolder>>() {
                            @Override
                            public void onSuccess(List<GoogleDriveFileHolder> googleDriveFileHolders) {
                                Gson gson = new Gson();
                                Log.w(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolders));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "onFailure: " + e.getMessage());
                            }
                        });
            }
        });

        uploadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (mDriveServiceHelper == null) {
                    return;
                }
                mDriveServiceHelper.uploadFile(helper.getFile("test.txt"), "text/plain", null)
                        .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                            @Override
                            public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                                Gson gson = new Gson();
                                Log.w(TAG, "onSuccess: " + gson.toJson(googleDriveFileHolder));
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "onFailure: " + e.getMessage());
                            }
                        });
            }
        });

        downloadFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }
                mDriveServiceHelper.downloadFile(new File(helper.getAppLocalDataFolder(),
                                                                "downloaded.txt"),
                                            "1q9ooCmF8sEz2bpscbua9D-LusmJST1Fe")
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                Log.i(TAG, "onSuccess: Downloaded File");
                            }
                        })
                        .addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                Log.w(TAG, "onFailure: FAILED TO GRAB FILE", e);
                            }
                        });
            }
        });

        deleteFileFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }
                try {
                    mDriveServiceHelper.deleteFolderOrFile("1FW-4PGFE779Jq32M5vAdjcCdf3Fuc07Z")
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void unused) {
                                    Log.i(TAG, "onSuccess: Deleted file");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAG, "onFailure: " + e.getMessage());
                                }
                            });
                } catch (Exception ex){
                    Log.e(TAG, "onClick: Drive Exception", ex);
                }

            }
        });

        queryFiles.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }
                try {
                    mDriveServiceHelper.queryFiles()
                            .addOnSuccessListener(new OnSuccessListener<FileList>() {
                                @Override
                                public void onSuccess(FileList fileList) {

                                    Gson gson = new Gson();
                                    Log.w(TAG, "onSuccess: " + gson.toJson(fileList));
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAG, "onFailure: " + e.getMessage());
                                }
                            });
                } catch (Exception ex){
                    Log.e(TAG, "onClick: Drive Exception", ex);
                }

            }
        });

        createLocalFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                helper.createFileUri("test.txt", "My test content.");
            }
        });

        readFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }
                try {
                    mDriveServiceHelper.readFile("1q9ooCmF8sEz2bpscbua9D-LusmJST1Fe")
                            .addOnSuccessListener(new OnSuccessListener<Pair<String, String>>() {
                                @Override
                                public void onSuccess(Pair<String, String> readPair) {
                                    Log.i(TAG, "onSuccess: " + readPair.toString());
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAG, "onFailure: " + e.getMessage());
                                }
                            });
                } catch (Exception ex){
                    Log.e(TAG, "onClick: Drive Exception", ex);
                }

            }
        });

        saveFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mDriveServiceHelper == null) {
                    return;
                }
                try {
                    mDriveServiceHelper.saveFile("1q9ooCmF8sEz2bpscbua9D-LusmJST1Fe", "test1.txt", "UPDATED TEXT.")
                            .addOnSuccessListener(new OnSuccessListener<com.google.api.services.drive.model.File>() {
                                @Override
                                public void onSuccess(com.google.api.services.drive.model.File fileList) {
                                    Log.w(TAG, "onSuccess: ");
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    Log.w(TAG, "onFailure: " + e.getMessage());
                                }
                            });
                } catch (Exception ex){
                    Log.e(TAG, "onClick: Drive Exception", ex);
                }

            }
        });

    }

    @Override
    protected void onStart() {
        super.onStart();
        helper = new Helper(this);
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
        gDriveAction = findViewById(R.id.g_drive_action);
        searchFile = findViewById(R.id.search_file);
        searchFolder = findViewById(R.id.search_folder);
        createTextFile = findViewById(R.id.create_text_file);
        createFolder = findViewById(R.id.create_folder);
        uploadFile = findViewById(R.id.upload_file);
        downloadFile = findViewById(R.id.download_file);
        deleteFileFolder = findViewById(R.id.delete_file_folder);
        viewFileFolder = findViewById(R.id.view_file_folder);
        queryFiles = findViewById(R.id.query_files_btn);
        createLocalFile = findViewById(R.id.create_local_file);
        readFile = findViewById(R.id.read_file);
        saveFile = findViewById(R.id.save_file);
    }
}
