package com.jaf.recipebook;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.api.client.util.DateTime;
import com.google.api.services.drive.model.FileList;
import com.google.api.services.drive.model.File;

import java.util.ArrayList;

public class DriveSyncHelper {

    private static final String TAG = "DriveSyncHelper";

    /**
     * Should be called when any file creation/edit/delete is performed to keep the
     * user's Google Drive synced with the local device.
     * <br>
     * Checks that all files are shared between local and Drive, then checks if all
     * files are the same version.
     * @param dsh Service Helper for interacting with the user's Google Drive
     */
    public static void syncLocalWithDrive(DriveServiceHelper dsh, Context context){
        FileHelper fh = new FileHelper(context);

        OnSuccessListener queryFileSuccessListener = new OnSuccessListener<FileList>() {
            @Override
            public void onSuccess(FileList driveFileList) {

                // Check that the app folder exists
                String appFolderId = ensureAppFolderExists(dsh, driveFileList, context);

                // Now get the files from the local device
                java.io.File[] localFiles = fh.getAllFiles();

                // Validate that all local files exist in & all content matches drive folder
                ArrayList<java.io.File> filesToUpload = new ArrayList<>();
                ArrayList<File> filesToDownload = new ArrayList<>();
                for (java.io.File file : localFiles){
                    boolean foundInDrive = false;
                    String localChecksum = fh.getChecksumMD5(file);
                    for (File driveFile : driveFileList.getFiles()){

                        if (driveFile.getName().equals(context.getString(R.string.app_name))
                                && driveFile.getMimeType().equals("application/vnd.google-apps.folder")){
                            // Ignore the main Drive App Folder
                            continue;
                        }

                        // If we find the file, check if the modified dates match
                        // If not, find the most recent version
                        if (file.getName().equals(driveFile.getName())){
                            if(!localChecksum.equals(driveFile.getMd5Checksum())) {
                                if (file.lastModified() > driveFile.getModifiedTime().getValue()) {
                                    // If local file is more recent
                                    filesToUpload.add(file);
                                } else {
                                    // If drive file is more recent
                                    filesToDownload.add(driveFile);
                                }
                            }
                            foundInDrive = true;
                            break;
                        }
                    }

                    // If we don't find the file in the drive, upload it
                    if (!foundInDrive){
                        filesToUpload.add(file);
                    }
                }

                // Validate that all drive files exist in & all content matches drive folder
                // Last loop already compared, so we're just looking for missing this time
                for (File driveFile : driveFileList.getFiles()){
                    boolean foundInLocal = false;

                    if (driveFile.getName().equals(context.getString(R.string.app_name))
                            && driveFile.getMimeType().equals("application/vnd.google-apps.folder")){
                        // Ignore the main Drive App Folder
                        continue;
                    }

                    for (java.io.File file : localFiles){
                        if (file.getName().equals(driveFile.getName())){
                            foundInLocal = true;
                            break;
                        }
                    }

                    if (!foundInLocal){
                        filesToDownload.add(driveFile);
                    }
                }

                // Upload missing files to Drive
                uploadDesyncedFiles(dsh, filesToUpload, appFolderId);

                // Download missing files from Drive
                java.io.File localAppFolder = fh.getAppLocalDataFolder();
                for (File file : filesToDownload){
                    dsh.downloadFile(localAppFolder, file.getId(), file.getName());
                }
            }
        };

        // Get list of Files in the Drive
        dsh.queryFiles().addOnSuccessListener(queryFileSuccessListener)
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "Failed to fetch app files from the user's drive", e);
                }
            });

    }

    /**
     * Checks if the "RecipeBook" folder exists in the user's Google Drive, and
     * creates it if it's missing. Then returns the ID of the app folder.
     * @param dsh Will be used to create the App Folder, if missing
     * @param queryList Existing fetched list of App Files/Folders
     * @param context Used to grab app_name to create main app folder
     * @return String ID for the App Folder
     */
    public static String ensureAppFolderExists(DriveServiceHelper dsh, FileList queryList, Context context){
        String appFolderId = "";
        for (File file: queryList.getFiles()){
            if (file.getMimeType().equals("application/vnd.google-apps.folder") &&
                file.getName().equals(context.getString(R.string.app_name)) ){
                appFolderId = file.getId();
                break;
            }
        }

        if(appFolderId.equals("")){
            return dsh.createFolder(context.getString(R.string.app_name), null).getResult().getId();
        }

        return appFolderId;
    }

    /**
     * Upload the list of local files to the Drive that are currently out of sync, or
     * don't exist in the user's Google Drive
     * @param dsh Service Helper for interacting with the user's Google Drive
     * @param files List of local files to upload
     * @param appFolderId ID of the app folder in the User's Drive
     */
    public static void uploadDesyncedFiles(DriveServiceHelper dsh, ArrayList<java.io.File> files, String appFolderId){
        for (java.io.File file : files){
            uploadDesyncedFile(dsh, file, new DateTime(file.lastModified()), appFolderId);
        }
    }

    /**
     * Upload the list of local files to the Drive that are currently out of sync, or
     * don't exist in the user's Google Drive
     * @param dsh Service Helper for interacting with the user's Google Drive
     * @param file Local file to upload
     * @param appFolderId ID of the app folder in the User's Drive
     */
    public static void uploadDesyncedFile(DriveServiceHelper dsh, java.io.File file, DateTime modifiedTime, String appFolderId){
        Log.d(TAG, "uploadDesyncedFile: Uploading '" + file.getName() + "' from local...");
        dsh.uploadFile(file, "text/plain", modifiedTime, appFolderId)
            .addOnSuccessListener(new OnSuccessListener<GoogleDriveFileHolder>() {
                @Override
                public void onSuccess(GoogleDriveFileHolder googleDriveFileHolder) {
                    Log.d(TAG, "uploadDesyncedFile: Uploaded as "
                            + googleDriveFileHolder.getName() + " (" + googleDriveFileHolder.getId() + ")");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "uploadDesyncedFile: Failed upload of " + file.getName(), e);
                }
            });
    }

    /**
     * Delete the provided file from the user's Google Drive and local device.
     * @param dsh Service Helper for interacting with the user's Google Drive
     * @param fileToDelete Local file to delete
     */
    public static void deleteFileFromDriveAndLocal(DriveServiceHelper dsh, java.io.File fileToDelete){
        if (fileToDelete.exists()) {
            dsh.queryFiles().addOnSuccessListener(new OnSuccessListener<FileList>() {
                @Override
                public void onSuccess(FileList fileList) {
                    File driveFile = null;
                    for (File currentDriveFile : fileList.getFiles()) {
                        if (currentDriveFile.getName().equals(fileToDelete.getName())) {
                            driveFile = currentDriveFile;
                            break;
                        }
                    }
                    if (driveFile == null) {
                        Log.e(TAG, "deleteFileFromDrive: Failed to find file '"
                                + fileToDelete.getName() + "' in the google drive. Only deleting locally.");
                        fileToDelete.delete();
                    } else {
                        Log.d(TAG, "deleteFileFromDrive: Attemping to delete '"
                                + driveFile.getName() + "' (" + driveFile.getId() + ")");
                        dsh.deleteFolderOrFile(driveFile.getId())
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        Log.d(TAG, "deleteFileFromDrive: Deleted file");
                                        fileToDelete.delete();
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        Log.d(TAG, "deleteFileFromDrive: Failed to delete file", e);
                                        fileToDelete.delete();
                                    }
                                });
                    }
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(TAG, "deleteFileFromDriveAndLocal: Failed to query drive files. Deleting locally", e);
                    fileToDelete.delete();
                }
            });
        } else {
            Log.e(TAG, "deleteFileFromDrive: Failed to find file: " + fileToDelete.getName());
        }
    }

    /**
     *
     * Delete the provided file from the user's Google Drive only.
     * @param dsh Service Helper for interacting with the user's Google Drive
     * @param fileToDelete Drive File to delete
     */
    public static void deleteFileFromDrive(DriveServiceHelper dsh, File fileToDelete){
        dsh.deleteFolderOrFile(fileToDelete.getId())
            .addOnSuccessListener(new OnSuccessListener<Void>() {
                @Override
                public void onSuccess(Void unused) {
                    Log.d(TAG, "deleteFileFromDrive: Deleted "
                            + fileToDelete.getName() + " (" + fileToDelete.getId() + ")");
                }
            })
            .addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.d(TAG, "deleteFileFromDrive: Failed to delete "
                            + fileToDelete.getName() + " (" + fileToDelete.getId() + ")", e);
                }
            });
    }

}
