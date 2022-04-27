package com.jaf.recipebook;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.provider.OpenableColumns;
import android.util.Pair;

import androidx.annotation.Nullable;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.AbstractInputStreamContent;
import com.google.api.client.http.ByteArrayContent;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * A utility for performing read/write operations on Drive files via the REST API and opening a
 * file picker UI via Storage Access Framework.
 */
public class DriveServiceHelper {
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;
    private Context context;
    private final String TAG = "DriveServiceHelper";

    public DriveServiceHelper(Drive driveService, Context context) {
        mDriveService = driveService;
        this.context = context;
    }

    public static Drive getGoogleDriveService(Context context, GoogleSignInAccount account, String appName) {
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        context, Collections.singleton(DriveScopes.DRIVE_FILE));
        credential.setSelectedAccount(account.getAccount());
        com.google.api.services.drive.Drive googleDriveService =
                new com.google.api.services.drive.Drive.Builder(
                        AndroidHttp.newCompatibleTransport(),
                        new GsonFactory(),
                        credential)
                        .setApplicationName(appName)
                        .build();
        return googleDriveService;
    }

    public Task<File> createFile(String filename) {
        final TaskCompletionSource<File> tcs = new TaskCompletionSource<File>();
        mExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        File metadata = new File()
                                .setParents(Collections.singletonList("root"))
                                .setMimeType("text/plain")
                                .setName(filename);
                        File googleFile = null;
                        try {
                            googleFile = mDriveService.files().create(metadata).execute();
                        } catch (UserRecoverableAuthIOException ex) {
                            context.startActivity(ex.getIntent());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        File finalResult = googleFile;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
                    }
                });

        return tcs.getTask();
    }

    /**
     * Opens the file identified by {@code fileId} and returns a {@link Pair} of its name and
     * contents.
     */
    public Task<Pair<String, String>> readFile(String fileId) {
        final TaskCompletionSource<Pair<String, String>> tcs = new TaskCompletionSource<Pair<String, String>>();
        mExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        Pair<String, String> pair = null;

                        try {
                            // Retrieve the metadata as a File object.
                            File metadata = mDriveService.files().get(fileId).execute();
                            String name = metadata.getName();

                            InputStream is = mDriveService.files().get(fileId).executeMediaAsInputStream();
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is));

                            StringBuilder stringBuilder = new StringBuilder();
                            String line;

                            while ((line = reader.readLine()) != null) {
                                stringBuilder.append(line);
                            }
                            String contents = stringBuilder.toString();
                            reader.close();
                            is.close();

                            pair = Pair.create(name, contents);

                        } catch (UserRecoverableAuthIOException ex) {
                            context.startActivity(ex.getIntent());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        Pair<String, String> finalResult = pair;

                        new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
                    }
                });

        return tcs.getTask();
    }

    /**
     * Updates the file identified by {@code fileId} with the given {@code name} and {@code
     * content}.
     */
    public Task<File> saveFile(String fileId, String name, String content) {
        final TaskCompletionSource<File> tcs = new TaskCompletionSource<File>();
        mExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        // Create a File containing any metadata changes.
                        File metadata = new File().setName(name);

                        // Convert content to an AbstractInputStreamContent instance.
                        ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);

                        File result = null;
                        try {
                            // Update the metadata and contents.
                            result = mDriveService.files().update(fileId, metadata, contentStream).execute();
                        } catch (UserRecoverableAuthIOException ex) {
                            context.startActivity(ex.getIntent());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        File finalResult = result;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
                    }
                });

        return tcs.getTask();
    }

    public Task<List<GoogleDriveFileHolder>> searchFolder(final String folderName) {
        final TaskCompletionSource<List<GoogleDriveFileHolder>> tcs = new TaskCompletionSource<List<GoogleDriveFileHolder>>();
        mExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        List<GoogleDriveFileHolder> googleDriveFileHolderList = new ArrayList<>();

                        try {
                            // Retrive the metadata as a File object.
                            FileList result = mDriveService.files().list()
                                    .setQ("mimeType = '" + DriveFolder.MIME_TYPE + "' and name = '" + folderName + "' ")
                                    .setSpaces("drive")
                                    .execute();

                            for (int i = 0; i < result.getFiles().size(); i++) {

                                GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
                                googleDriveFileHolder.setId(result.getFiles().get(i).getId());
                                googleDriveFileHolder.setName(result.getFiles().get(i).getName());

                                googleDriveFileHolderList.add(googleDriveFileHolder);
                            }

                        } catch (UserRecoverableAuthIOException ex) {
                            context.startActivity(ex.getIntent());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        List<GoogleDriveFileHolder> finalResult = googleDriveFileHolderList;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
                    }
                });

        return tcs.getTask();
    }

    public Task<GoogleDriveFileHolder> createTextFile(final String fileName, final String content, @Nullable final String folderId) {
        final TaskCompletionSource<GoogleDriveFileHolder> tcs = new TaskCompletionSource<GoogleDriveFileHolder>();
        mExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        List<String> root;
                        if (folderId == null) {
                            root = Collections.singletonList("root");
                        } else {
                            root = Collections.singletonList(folderId);
                        }
                        File metadata = new File()
                                .setParents(root)
                                .setMimeType("text/plain")
                                .setName(fileName);
                        ByteArrayContent contentStream = ByteArrayContent.fromString("text/plain", content);

                        GoogleDriveFileHolder googleDriveFileHolder = null;
                        try {
                            File googleFile = mDriveService.files().create(metadata, contentStream).execute();
                            if (googleFile == null) {
                                throw new IOException("Null result when requesting file creation.");
                            }
                            googleDriveFileHolder = new GoogleDriveFileHolder();
                            googleDriveFileHolder.setId(googleFile.getId());
                        } catch (UserRecoverableAuthIOException ex) {
                            context.startActivity(ex.getIntent());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        GoogleDriveFileHolder finalResult = googleDriveFileHolder;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
                    }
                });

        return tcs.getTask();
    }

    public Task<GoogleDriveFileHolder> createFolder(final String folderName, @Nullable final String folderId) {
        final TaskCompletionSource<GoogleDriveFileHolder> tcs = new TaskCompletionSource<GoogleDriveFileHolder>();
        mExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
                        List<String> root;
                        if (folderId == null) {
                            root = Collections.singletonList("root");
                        } else {
                            root = Collections.singletonList(folderId);
                        }
                        File metadata = new File()
                                .setParents(root)
                                .setMimeType(DriveFolder.MIME_TYPE)
                                .setName(folderName);

                        try {
                            File googleFile = mDriveService.files().create(metadata).execute();
                            if (googleFile == null) {
                                throw new IOException("Null result when requesting file creation.");
                            }
                            googleDriveFileHolder.setId(googleFile.getId());
                        } catch (UserRecoverableAuthIOException ex) {
                            context.startActivity(ex.getIntent());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        GoogleDriveFileHolder finalResult = googleDriveFileHolder;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
                    }
                });

        return tcs.getTask();
    }

    /**
     * Returns a {@link FileList} containing all the visible files in the user's My Drive.
     *
     * <p>The returned list will only contain files visible to this app, i.e. those which were
     * created by this app. To perform operations on files not created by the app, the project must
     * request Drive Full Scope in the <a href="https://play.google.com/apps/publish">Google
     * Developer's Console</a> and be submitted to Google for verification.</p>
     */
    public Task<FileList> queryFiles() {
        final TaskCompletionSource<FileList> tcs = new TaskCompletionSource<FileList>();
        mExecutor.execute(
            new Runnable() {
                @Override
                public void run() {
                    FileList result = null;
                    try {
                        result = mDriveService.files().list().setSpaces("drive").execute();
                    } catch (UserRecoverableAuthIOException ex) {
                        context.startActivity(ex.getIntent());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    FileList finalResult = result;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
                }
            });

        return tcs.getTask();
    }

    public Task<List<GoogleDriveFileHolder>> searchFile(final String fileName, final String mimeType) {
        final TaskCompletionSource<List<GoogleDriveFileHolder>> tcs = new TaskCompletionSource<List<GoogleDriveFileHolder>>();
        mExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        List<GoogleDriveFileHolder> googleDriveFileHolderList = new ArrayList<>();

                        try {
                            // Retrive the metadata as a File object.
                            FileList result = mDriveService.files().list()
                                    .setQ("name = '" + fileName + "' and mimeType ='" + mimeType + "'")
                                    .setSpaces("drive")
                                    .setFields("files(id, name,size,createdTime,modifiedTime,starred)")
                                    .execute();

                            for (int i = 0; i < result.getFiles().size(); i++) {
                                GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
                                googleDriveFileHolder.setId(result.getFiles().get(i).getId());
                                googleDriveFileHolder.setName(result.getFiles().get(i).getName());
                                googleDriveFileHolder.setModifiedTime(result.getFiles().get(i).getModifiedTime());
                                googleDriveFileHolder.setSize(result.getFiles().get(i).getSize());
                                googleDriveFileHolderList.add(googleDriveFileHolder);

                            }
                        } catch (UserRecoverableAuthIOException ex) {
                            context.startActivity(ex.getIntent());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        List<GoogleDriveFileHolder> finalResult = googleDriveFileHolderList;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
                    }
                });

        return tcs.getTask();
    }

    public Task<GoogleDriveFileHolder> uploadFile(final File googleDiveFile, final AbstractInputStreamContent content) {
        final TaskCompletionSource<GoogleDriveFileHolder> tcs = new TaskCompletionSource<GoogleDriveFileHolder>();
        mExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();

                        try {
                            File fileMeta = mDriveService.files().create(googleDiveFile, content).execute();
                            googleDriveFileHolder.setId(fileMeta.getId());
                            googleDriveFileHolder.setName(fileMeta.getName());
                        } catch (UserRecoverableAuthIOException ex) {
                            context.startActivity(ex.getIntent());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        GoogleDriveFileHolder finalResult = googleDriveFileHolder;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
                    }
                });

        return tcs.getTask();
    }

    public Task<GoogleDriveFileHolder> uploadFile(final java.io.File localFile, final String mimeType, @Nullable final String folderId) {
        final TaskCompletionSource<GoogleDriveFileHolder> tcs = new TaskCompletionSource<GoogleDriveFileHolder>();
        mExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        // Retrieve the metadata as a File object.
                        List<String> root;
                        if (folderId == null) {
                            root = Collections.singletonList("root");
                        } else {
                            root = Collections.singletonList(folderId);
                        }
                        File metadata = new File()
                                .setParents(root)
                                .setMimeType(mimeType)
                                .setName(localFile.getName());
                        FileContent fileContent = new FileContent(mimeType, localFile);

                        GoogleDriveFileHolder googleDriveFileHolder = new GoogleDriveFileHolder();
                        try {
                            File fileMeta = mDriveService.files().create(metadata, fileContent).execute();
                            googleDriveFileHolder.setId(fileMeta.getId());
                            googleDriveFileHolder.setName(fileMeta.getName());
                        } catch (UserRecoverableAuthIOException ex) {
                            context.startActivity(ex.getIntent());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        GoogleDriveFileHolder finalResult = googleDriveFileHolder;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
                    }
                });

        return tcs.getTask();
    }

    /**
     * Opens the file at the {@code uri} returned by a Storage Access Framework {@link Intent}
     * created by {@link #createFilePickerIntent()} using the given {@code contentResolver}.
     */
    public Task<Pair<String, String>> openFileUsingStorageAccessFramework(ContentResolver contentResolver, Uri uri) {
        final TaskCompletionSource<Pair<String, String>> tcs = new TaskCompletionSource<Pair<String, String>>();
        mExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {

                        // Retrieve the document's display name from its metadata.
                        String name = null;
                        Cursor cursor = contentResolver.query(uri, null, null, null, null);
                        if (cursor != null && cursor.moveToFirst()) {
                            int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                            name = cursor.getString(nameIndex);
                        }

                        // Read the document's contents as a String.
                        String content = null;
                        try {
                            InputStream is = contentResolver.openInputStream(uri);
                            BufferedReader reader = new BufferedReader(new InputStreamReader(is)) ;
                            StringBuilder stringBuilder = new StringBuilder();
                            String line;
                            while ((line = reader.readLine()) != null) {
                                stringBuilder.append(line);
                            }
                            content = stringBuilder.toString();
                            reader.close();
                            is.close();
                        } catch (IOException ex){
                            ex.printStackTrace();
                        }

                        Pair<String, String> finalResult = Pair.create(name, content);

                        new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
                    }
                });

        return tcs.getTask();
    }

    public Task<Void> downloadFile(final java.io.File fileSaveLocation, final String fileId) {
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<Void>();
        mExecutor.execute(
            new Runnable() {
                @Override
                public void run() {
                    try {
                        OutputStream outputStream = new FileOutputStream(fileSaveLocation);
                        mDriveService.files().get(fileId).executeMediaAndDownloadTo(outputStream);
                        outputStream.close();
                    } catch (UserRecoverableAuthIOException ex) {
                        context.startActivity(ex.getIntent());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(null), 1000);
                }
            });
        return tcs.getTask();
    }

    public Task<InputStream> downloadFile(final String fileId) {
        final TaskCompletionSource<InputStream> tcs = new TaskCompletionSource<InputStream>();
        mExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        InputStream result = null;
                        try {
                            result = mDriveService.files().get(fileId).executeMediaAsInputStream();
                        } catch (UserRecoverableAuthIOException ex) {
                            context.startActivity(ex.getIntent());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        InputStream finalResult = result;
                        new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
                    }
                });

        return tcs.getTask();
    }

    public Task<Void> deleteFolderOrFile(final String fileId) {
        final TaskCompletionSource<Void> tcs = new TaskCompletionSource<Void>();
        mExecutor.execute(
                new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (fileId != null) {
                                mDriveService.files().delete(fileId).execute();
                            }
                        } catch (UserRecoverableAuthIOException ex) {
                            context.startActivity(ex.getIntent());
                        } catch (IOException ex) {
                            ex.printStackTrace();
                        }
                        new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(null), 1000);
                    }
                });

        return tcs.getTask();
    }

    /**
     * Returns an {@link Intent} for opening the Storage Access Framework file picker.
     */
    public Intent createFilePickerIntent() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/plain");

        return intent;
    }
}