package com.jaf.recipebook.helpers;

import android.content.Context;
import android.icu.text.RelativeDateTimeFormatter;
import android.icu.text.SimpleDateFormat;
import android.icu.util.TimeZone;
import android.media.metrics.Event;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.api.client.extensions.android.http.AndroidHttp;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.Task;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException;
import com.google.api.client.http.FileContent;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import com.jaf.recipebook.R;
import com.jaf.recipebook.db.RecipeBookDatabase;
import com.jaf.recipebook.events.DbRefreshEvent;
import com.jaf.recipebook.events.DbShutdownEvent;
import com.jaf.recipebook.events.DriveDataDeletedEvent;
import com.jaf.recipebook.events.DriveDbLastModifiedEvent;
import com.jaf.recipebook.events.DriveTimestampResultEvent;
import com.jaf.recipebook.events.DriveUploadCompeleteEvent;

import org.greenrobot.eventbus.EventBus;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.Time;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Objects;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A utility for performing read/write operations on Drive files via the REST API and opening a
 * file picker UI via Storage Access Framework.
 */
public class DriveServiceHelper {
    private FileHelper fh;
    private final Executor mExecutor = Executors.newSingleThreadExecutor();
    private final Drive mDriveService;
    private Context context;
    private final String TAG = "JAF-DriveServiceHelper";

    final String mainStorageFileName = "recipeBookDatabase";
    final String shmStorageFileName = "recipeBookDatabase-shm";
    final String walStorageFileName = "recipeBookDatabase-wal";
    final String timestampFileName = "recipeBookTimestamp";

    public DriveServiceHelper(Drive driveService, Context context) {
        mDriveService = driveService;
        this.context = context;
        fh = new FileHelper(context);
    }

    private OnFailureListener queryFileFailureListener = e -> {
        Log.e(TAG, "Failed to fetch app files from the user's drive", e);
        EventBus.getDefault().post(new DbRefreshEvent(false));
    };

    public static Drive getGoogleDriveService(Context context, GoogleSignInAccount account, String appName) {
        GoogleAccountCredential credential =
                GoogleAccountCredential.usingOAuth2(
                        context, Arrays.asList(DriveScopes.DRIVE_FILE, DriveScopes.DRIVE_APPDATA));
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

    private java.io.File getLocalDatabaseFile() {
        return context.getDatabasePath(context.getString(R.string.database_name));
    }

    /**
     * Returns a {@link FileList} containing all the visible files in the user's My Drive.
     *
     * <p>The returned list will only contain files visible to this app, i.e. those which were
     * created by this app. To perform operations on files not created by the app, the project must
     * request Drive Full Scope in the <a href="https://play.google.com/apps/publish">Google
     * Developer's Console</a> and be submitted to Google for verification.</p>
     */
    protected Task<FileList> queryFiles() {
        final TaskCompletionSource<FileList> tcs = new TaskCompletionSource<>();
        mExecutor.execute(
                () -> {
                    FileList result = null;
                    try {
                        result = mDriveService.files()
                                    .list()
                                    .setSpaces("appDataFolder")
                                    .setFields("files(id, name, mimeType, size, md5Checksum, modifiedTime)")
                                    .execute();
                    } catch (UserRecoverableAuthIOException ex) {
                        context.startActivity(ex.getIntent());
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                    FileList finalResult = result;
                    new Handler(Looper.getMainLooper()).postDelayed(() -> tcs.setResult(finalResult), 1000);
                });

        return tcs.getTask();
    }

    protected Runnable downloadFileToTmp(final String fileId, java.io.File tmpFile) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream osDb = Files.newOutputStream(tmpFile.toPath());
                    mDriveService.files().get(fileId).executeMediaAndDownloadTo(osDb);
                } catch (UserRecoverableAuthIOException ex) {
                    context.startActivity(ex.getIntent());
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    protected void clearTempFiles(){
        java.io.File cacheFolder = context.getCacheDir();
        java.io.File tmpFolder = new java.io.File(cacheFolder.getPath() + "/" + "tmp");
        for (java.io.File file : Objects.requireNonNull(tmpFolder.listFiles())){
            Log.d(TAG, "Cached file " + file.getName() + " was deleted: " + file.delete());
        }
    }

    protected boolean clearDbFileCache(java.io.File dbFile, java.io.File shmFile, java.io.File walFile) {

        java.io.File cacheFolder = context.getCacheDir();
        java.io.File cacheDbFile =
                new java.io.File(cacheFolder.getPath() + "/" + dbFile.getName());
        java.io.File cacheShmFile =
                new java.io.File(cacheFolder.getPath() + "/" + shmFile.getName());
        java.io.File cacheWalFile =
                new java.io.File(cacheFolder.getPath() + "/" + walFile.getName());

        return cacheDbFile.delete() && cacheShmFile.delete() && cacheWalFile.delete();
    }

    protected boolean saveDbFilesToCache(java.io.File dbFile, java.io.File shmFile, java.io.File walFile)
            throws IOException{

        java.io.File cacheFolder = context.getCacheDir();
        java.io.File cacheDbFile =
                new java.io.File(cacheFolder.getPath() + "/" + dbFile.getName());
        java.io.File cacheShmFile =
                new java.io.File(cacheFolder.getPath() + "/" + shmFile.getName());
        java.io.File cacheWalFile =
                new java.io.File(cacheFolder.getPath() + "/" + walFile.getName());

        Log.i(TAG, "Making temporary backup to cache...");
        Path dbPath = Files.copy(dbFile.toPath(), cacheDbFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        // Wal and Shm files can be empty. Theoretically.
        Path shmPath;
        Path walPath;
        try {
            shmPath = Files.copy(shmFile.toPath(), cacheShmFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            cacheShmFile.createNewFile();
            shmPath = cacheShmFile.toPath();
        }
        try {
            walPath = Files.copy(walFile.toPath(), cacheWalFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            cacheWalFile.createNewFile();
            walPath = cacheWalFile.toPath();
        }
        return dbPath != null && shmPath != null && walPath != null;
    }

    protected boolean attemptRecoverFromCache() {

        java.io.File dbFile = getLocalDatabaseFile();
        java.io.File shmFile = new java.io.File(dbFile.getPath() + "-shm");
        java.io.File walFile = new java.io.File(dbFile.getPath() + "-wal");

        java.io.File cacheFolder = context.getCacheDir();
        java.io.File cacheDbFile =
                new java.io.File(cacheFolder.getPath() + "/" + dbFile.getName());
        java.io.File cacheShmFile =
                new java.io.File(cacheFolder.getPath() + "/" + shmFile.getName());
        java.io.File cacheWalFile =
                new java.io.File(cacheFolder.getPath() + "/" + walFile.getName());

        try {
            Path dbRevert = Files.move(cacheDbFile.toPath(), dbFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            Path shmRevert = Files.move(cacheShmFile.toPath(), shmFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
            Path walRevert = Files.move(cacheWalFile.toPath(), walFile.toPath(),
                    StandardCopyOption.REPLACE_EXISTING);

            return dbRevert != null && shmRevert != null && walRevert != null;
        } catch (IOException ex) {
            Log.e(TAG, "Failed to recover DB files from cache and place them in the DB folder");
            return false;
        }
    }

    private void getLastBackupDateTask(FileList driveFileList) {
        DateTime dbFileLastModified = null;
        for(File df : driveFileList.getFiles()){
            if (df.getName().equals(mainStorageFileName)){
                dbFileLastModified = df.getModifiedTime();
                break;
            }
        }

        if (dbFileLastModified == null){
            Log.d(TAG, "Database is not currently backed up to the drive");
            EventBus.getDefault().post(new DriveDbLastModifiedEvent(context.getString(R.string.database_not_backed_up), false));
        } else {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
            sdf.setTimeZone(TimeZone.getDefault());
            EventBus.getDefault().post(new DriveDbLastModifiedEvent(
                    context.getString(R.string.database_last_backed_up) + " " + sdf.format(dbFileLastModified.getValue()),
                    true));
            Log.d(TAG, "Database last updated: " + dbFileLastModified);
        }
    }

    public void getLastBackupDate() {
        OnSuccessListener queryFileSuccessListener = (OnSuccessListener<FileList>) driveFileList -> new Thread(() -> getLastBackupDateTask(driveFileList)).start();

        queryFiles()
                .addOnSuccessListener(queryFileSuccessListener)
                .addOnFailureListener(queryFileFailureListener);

    }

    private void uploadTask(FileList driveFileList, long timestamp){
        try {
            FileOutputStream fos = this.context.openFileOutput(timestampFileName, Context.MODE_PRIVATE);
            char[] charArray = Long.toString(timestamp).toCharArray();
            for (char ch : charArray){
                fos.write(ch);
            }
            fos.close();
        } catch (IOException ex) {
            EventBus.getDefault().post(new DriveUploadCompeleteEvent(false));
            Log.e(TAG, (ex.getMessage() != null ? ex.getMessage() : "IO Exception when uploading"));
            return;
        }

        String mainDbFileId = null;
        String shmDbFileId = null;
        String walDbFileId = null;
        String timestampFileId = null;
        for(File df : driveFileList.getFiles()){
            switch (df.getName()){
                case mainStorageFileName: mainDbFileId = df.getId(); break;
                case shmStorageFileName: shmDbFileId = df.getId(); break;
                case walStorageFileName: walDbFileId = df.getId(); break;
                case timestampFileName: timestampFileId = df.getId(); break;
                default:
                    Log.e(TAG, "Unknown file in App Drive Folder: ("
                            + df.getName() + " | " + df.getId() + ")");
                    break;
            }
        }

        File storageFile = new File();
        storageFile.setName(mainStorageFileName);

        File storageFileShm = new File();
        storageFileShm.setName(shmStorageFileName);

        File storageFileWal = new File();
        storageFileWal.setName(walStorageFileName);

        File storageTimestampFile = new File();
        storageTimestampFile.setName(timestampFileName);


        java.io.File dbFile = getLocalDatabaseFile();
        java.io.File shmFile = new java.io.File(dbFile.getPath() + "-shm");
        java.io.File walFile = new java.io.File(dbFile.getPath() + "-wal");
        java.io.File timestampFile = new java.io.File(this.context.getFilesDir() + "/" + timestampFileName);
        FileContent mediaContent = new FileContent("", dbFile);
        FileContent mediaContentShm = new FileContent("", shmFile);
        FileContent mediaContentWal = new FileContent("", walFile);
        FileContent mediaContentTimestamp = new FileContent("", timestampFile);
        try {
            if (mainDbFileId != null){
                mDriveService.files().update(mainDbFileId, storageFile, mediaContent).execute();
            } else {
                storageFile.setParents(Collections.singletonList("appDataFolder"));
                mDriveService.files().create(storageFile, mediaContent).execute();
            }

            if (shmDbFileId != null){
                if (shmFile.exists()){
                    mDriveService.files().update(shmDbFileId, storageFileShm, mediaContentShm).execute();
                } else {
                    mDriveService.files().delete(shmDbFileId).execute();
                }
            } else {
                if (shmFile.exists()) {
                    storageFileShm.setParents(Collections.singletonList("appDataFolder"));
                    mDriveService.files().create(storageFileShm, mediaContentShm).execute();
                }
            }

            if (walDbFileId != null){
                if (walFile.exists()){
                    mDriveService.files().update(walDbFileId, storageFileWal, mediaContentWal).execute();
                } else {
                    mDriveService.files().delete(walDbFileId).execute();
                }
            } else {
                if (walFile.exists()) {
                    storageFileWal.setParents(Collections.singletonList("appDataFolder"));
                    mDriveService.files().create(storageFileWal, mediaContentWal).execute();
                }
            }

            if (timestampFileId != null) {
                mDriveService.files().update(timestampFileId, storageTimestampFile, mediaContentTimestamp).execute();
            } else {
                storageTimestampFile.setParents(Collections.singletonList("appDataFolder"));
                mDriveService.files().create(storageTimestampFile, mediaContentTimestamp).execute();
            }
        }
        catch(UserRecoverableAuthIOException ex){
            EventBus.getDefault().post(new DriveUploadCompeleteEvent(false));
            context.startActivity(ex.getIntent());
        }
        catch(Exception e){
            EventBus.getDefault().post(new DriveUploadCompeleteEvent(false));
            e.printStackTrace();
        } finally {
            Log.i(TAG, "Drive upload finished");
            EventBus.getDefault().post(new DriveUploadCompeleteEvent(true));
        }
    }

    public void upload(){
        long timestamp = new Date().getTime();
        fh.setPreference(fh.BACKUP_TIMESTAMP_PREFERENCE, timestamp);

        OnSuccessListener queryFileSuccessListener = (OnSuccessListener<FileList>) driveFileList -> new Thread(() -> uploadTask(driveFileList, timestamp)).start();

        queryFiles()
                .addOnSuccessListener(queryFileSuccessListener)
                .addOnFailureListener(queryFileFailureListener);
    }

    private void downloadTimestampTask(FileList driveFileList) {
        String timestampFileId = null;
        for(File df : driveFileList.getFiles()) {
            switch (df.getName()) {
                case timestampFileName:
                    timestampFileId = df.getId();
                    break;
            }
        }

        if (timestampFileId == null){
            Log.w(TAG, "Timestamp file not found in Google Drive. Aborting...");
            EventBus.getDefault().post(new DriveTimestampResultEvent(Long.valueOf(0)));
            return;
        }

        java.io.File cacheFolder = context.getCacheDir();
        java.io.File tmpFileTs =
                new java.io.File(cacheFolder.getPath() + "/" + timestampFileName);

        try {
            ExecutorService es = Executors.newCachedThreadPool();
            es.execute(downloadFileToTmp(timestampFileId, tmpFileTs));
            es.shutdown();
            boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
            if (finished) {
                if (tmpFileTs.exists()) {
                    String timestampStr = fh.readFile(tmpFileTs);
                    EventBus.getDefault().post(new DriveTimestampResultEvent(Long.valueOf(timestampStr)));
                }
            }
        } catch(Exception e) {
            Log.e(TAG, e.getMessage());
        }

    }

    public void downloadTimestamp(){
        OnSuccessListener queryFileSuccessListener = (OnSuccessListener<FileList>) driveFileList -> new Thread(() -> downloadTimestampTask(driveFileList)).start();

        queryFiles()
                .addOnSuccessListener(queryFileSuccessListener)
                .addOnFailureListener(queryFileFailureListener);

    }

    // TODO Something isn't right with the WAL or SHM I think. Database is always one transaction before the current data when uploaded.

    private void downloadTask(FileList driveFileList){
        // Validate all files are present in the Drive space
        String mainDbFileId = null;
        String shmDbFileId = null;
        String walDbFileId = null;
        String timestampFileId = null;
        for(File df : driveFileList.getFiles()){
            switch (df.getName()){
                case mainStorageFileName: mainDbFileId = df.getId(); break;
                case shmStorageFileName: shmDbFileId = df.getId(); break;
                case walStorageFileName: walDbFileId = df.getId(); break;
                case timestampFileName: timestampFileId = df.getId(); break;
                default:
                    Log.e(TAG, "Unknown file in App Drive Folder: ("
                            + df.getName() + " | " + df.getId() + ")");
                    break;
            }
        }

        if (mainDbFileId == null){
            Log.w(TAG, "Main DB file not found in Google Drive. Aborting...");
            EventBus.getDefault().post(new DbRefreshEvent(false));
            return;
        }

        // Create references to temp folder to download the files to, and download them
        java.io.File currentDbFile = getLocalDatabaseFile();
        java.io.File cacheFolder = context.getCacheDir();
        java.io.File tmpFolder = new java.io.File(cacheFolder.getPath() + "/" + "tmp");
        if (!tmpFolder.exists()){
            tmpFolder.mkdir();
        }

        java.io.File tmpFileDb =
                new java.io.File(tmpFolder.getPath() + "/" + currentDbFile.getName());
        java.io.File tmpFileShm =
                new java.io.File(tmpFolder.getPath() + "/" + currentDbFile.getName() + "-shm");
        java.io.File tmpFileWal =
                new java.io.File(tmpFolder.getPath() + "/" + currentDbFile.getName() + "-wal");
        java.io.File tmpFileTs =
                new java.io.File(tmpFolder.getPath() + "/" + timestampFileName);
        try {
            ExecutorService es = Executors.newCachedThreadPool();
            es.execute(downloadFileToTmp(mainDbFileId, tmpFileDb));
            if (shmDbFileId != null){
                es.execute(downloadFileToTmp(shmDbFileId, tmpFileShm));
            }
            if (walDbFileId != null){
                es.execute(downloadFileToTmp(walDbFileId, tmpFileShm));
            }
            if (timestampFileId != null) {
                es.execute(downloadFileToTmp(timestampFileId, tmpFileTs));
            }
            es.shutdown();

            boolean finished = es.awaitTermination(1, TimeUnit.MINUTES);
            if (finished){
                if (tmpFileDb.exists()){
                    Log.i(TAG, "Closing DB to move in Drive backups...");

                    RecipeBookDatabase.stopDb();
                    EventBus.getDefault().post(new DbShutdownEvent(true));
                    java.io.File dbFile = getLocalDatabaseFile();
                    java.io.File shmFile = new java.io.File(dbFile.getPath() + "-shm");
                    java.io.File walFile = new java.io.File(dbFile.getPath() + "-wal");

                    if (!saveDbFilesToCache(dbFile, shmFile, walFile)) {
                        Log.e(TAG, "Failed to save DB copy to cache, aborting...");
                        throw new Exception("Something went wrong saving to the cache, aborting...");
                    }

                    // Move files downloaded from Drive into App DB Folder
                    Log.i(TAG, "Replacing DB files with downloads");
                    Path dbMove = Files.move(tmpFileDb.toPath(), dbFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING);

                    Path dbShmMove = null;
                    if (tmpFileShm.exists()){
                        dbShmMove = Files.move(tmpFileShm.toPath(), shmFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                    }
                    Path dbWalMove = null;
                    if (tmpFileWal.exists()){
                        dbWalMove = Files.move(tmpFileWal.toPath(), walFile.toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                    }

                    boolean clearCache = false;
                    // Verify files copied, only check if WAL or SHM got moved if their tmp files exist
                    if (dbMove != null && (!tmpFileShm.exists() || dbShmMove != null) && (!tmpFileWal.exists() || dbWalMove != null)){
                        Log.i(TAG, "DB Files successfully replaced, clearing cache/tmp files...");
                        clearCache = true;
                    } else {
                        Log.e(TAG, "Failed to replace DB files after modifying app db " +
                                "folder, attempting to revert...");
                        if (attemptRecoverFromCache()) {
                            Log.i(TAG, "Successfully restored DB files");
                            clearCache = true;
                        } else {
                            Log.wtf(TAG, "Failed to recover DB files. Leaving Cache intact.");
                        }
                    }

                    if (clearCache){
                        Log.d(TAG, "Clearing DB files from Cache");
                        if (clearDbFileCache(dbFile, shmFile, walFile)){
                            Log.d(TAG, "Cache successfully cleared");
                        } else {
                            Log.w(TAG, "Failed to clear cache of DB backups");
                        }
                    }

                    if (tmpFileTs.exists()) {
                        String timestampStr = fh.readFile(tmpFileTs);
                        fh.setPreference(fh.BACKUP_TIMESTAMP_PREFERENCE, Long.valueOf(timestampStr));
                    }

                } else {
                    Log.e(TAG, "Main DB file not downloaded to the temp folder, aborting... " +
                        "(tmpFileDb:" + tmpFileDb.exists() +
                        ", tmpFileShm:" + tmpFileShm.exists() +
                        ", tmpFileWal:" + tmpFileWal.exists() +
                        ")");
                }
                EventBus.getDefault().post(new DbRefreshEvent(true));
            } else {
                Log.e(TAG, "Failed to download DB files from user drive before time expired");
                EventBus.getDefault().post(new DbRefreshEvent(false));
            }
        }
        catch(Exception e){
            attemptRecoverFromCache();
            EventBus.getDefault().post(new DbRefreshEvent(false));
            e.printStackTrace();
        } finally {
            clearTempFiles();
        }
    }

    public void download(){
        OnSuccessListener queryFileSuccessListener = (OnSuccessListener<FileList>) driveFileList -> new Thread(() -> downloadTask(driveFileList)).start();

        queryFiles()
                .addOnSuccessListener(queryFileSuccessListener)
                .addOnFailureListener(queryFileFailureListener);

    }

    private void deleteTask(FileList driveFileList) {
        boolean didFail = false;
        for(File df : driveFileList.getFiles()){
            try {
                mDriveService.files().delete(df.getId()).execute();
            } catch (UserRecoverableAuthIOException ex) {
                didFail = true;
                context.startActivity(ex.getIntent());
            } catch (IOException ex) {
                didFail = true;
                Log.e(TAG, "Failed to delete drive file", ex);
            }
        }

        if (didFail){
            Log.w(TAG, "Not all files in Drive deleted.");
        } else {
            Log.i(TAG, "All app drive files successfully deleted.");
        }
        EventBus.getDefault().post(new DriveDataDeletedEvent(true));
    }

    public void delete() {
        OnSuccessListener queryFileSuccessListener = new OnSuccessListener<FileList>() {
            @Override
            public void onSuccess(FileList driveFileList) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        deleteTask(driveFileList);
                    }
                }).start();
            }
        };

        queryFiles()
                .addOnSuccessListener(queryFileSuccessListener)
                .addOnFailureListener(queryFileFailureListener);
    }

}