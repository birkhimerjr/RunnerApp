/*
 * Copyright 2013 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.dicadeveloper.runnerapp.endtoendtest.sync;

import org.dicadeveloper.runnerapp.TrackListActivity;
import org.dicadeveloper.runnerapp.endtoendtest.EndToEndTestUtils;
import org.dicadeveloper.runnerapp.endtoendtest.GoogleUtils;
import org.dicadeveloper.runnerapp.endtoendtest.RunConfiguration;
import org.dicadeveloper.runnerapp.io.sendtogoogle.SendToGoogleUtils;
import org.dicadeveloper.runnerapp.io.sync.SyncUtils;
import org.dicadeveloper.runnerapp.util.PreferencesUtils;
import com.google.android.gms.auth.GoogleAuthException;
import org.dicadeveloper.runnerapp.R;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpResponse;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

import android.app.Instrumentation;
import android.content.Context;
import android.util.Log;
import android.widget.ListView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import junit.framework.Assert;

/**
 * Utilites for Google Drive sync endtoend test.
 * 
 * @author Youtao Liu
 */
public class SyncTestUtils {

  private static final String TAG = SyncTestUtils.class.getSimpleName();
  public static boolean isCheckedRunSyncTest = false;
  public static final String KML_FILE_POSTFIX = ".kml";
  public static final long MAX_TIME_TO_WAIT_SYNC = 100000;

  /**
   * Sets up sync tests.
   * 
   * @param instrumentation the instrumentation is used for test
   * @param trackListActivity the startup activity
   * @return a Google Drive object
   */
  public static Drive setUpForSyncTest(Instrumentation instrumentation,
      TrackListActivity trackListActivity) throws IOException, GoogleAuthException {
    if (!isCheckedRunSyncTest || RunConfiguration.getInstance().getRunSyncTest()) {
      EndToEndTestUtils.setupForAllTest(instrumentation, trackListActivity);
    }
    if (!isCheckedRunSyncTest) {
      isCheckedRunSyncTest = true;
    }
    if (RunConfiguration.getInstance().getRunSyncTest()) {
      enableSync(GoogleUtils.ACCOUNT_1);
      Drive drive1 = getGoogleDrive(EndToEndTestUtils.trackListActivity.getApplicationContext());
      removeKMLFiles(drive1);
      EndToEndTestUtils.deleteAllTracks();
      enableSync(GoogleUtils.ACCOUNT_2);
      Drive drive2 = getGoogleDrive(EndToEndTestUtils.trackListActivity.getApplicationContext());
      removeKMLFiles(drive2);
      EndToEndTestUtils.deleteAllTracks();
      return drive2;
    }
    return null;
  }

  /**
   * Gets drive object of Google Drive.
   * 
   * @param context the context of application
   * @return a Google Drive object
   */
  public static Drive getGoogleDrive(Context context) throws IOException, GoogleAuthException {
    String googleAccount = PreferencesUtils.getString(context, R.string.google_account_key,
        PreferencesUtils.GOOGLE_ACCOUNT_DEFAULT);
    GoogleAccountCredential credential = SendToGoogleUtils.getGoogleAccountCredential(context,
        googleAccount, SendToGoogleUtils.DRIVE_SCOPE);
    return SyncUtils.getDriveService(credential);
  }

  /**
   * Gets KML files from Google Drive of current account.
   * 
   * @param context the context of application
   * @param drive a Google Drive object
   * @return KML file on Google drive
   * @throws IOException
   */
  public static List<File> getDriveFiles(Context context, Drive drive) throws IOException {
    File folder = SyncUtils.getMyTracksFolder(context, drive);
    if (folder == null) {
      return new ArrayList<File>();
    }
    String folderId = folder.getId();
    return drive.files().list()
        .setQ(String.format(Locale.US, SyncUtils.MY_TRACKS_FOLDER_FILES_QUERY, folderId)).execute()
        .getItems();
  }

  /**
   * Finds whether a file is existed by the name of track.
   * 
   * @param trackName name of track
   * @param drive a Google Drive object
   * @return the file be found
   * @throws IOException
   */
  public static File getFile(String trackName, Drive drive) throws IOException {
    List<File> files = getDriveFiles(EndToEndTestUtils.trackListActivity.getApplicationContext(),
        drive);
    for (int i = 0; i < files.size(); i++) {
      File file = files.get(i);
      String title = file.getTitle();
      if (title.equals(trackName + KML_FILE_POSTFIX)) {
        return file;
      }
    }
    return null;
  }

  /**
   * Removes all KML files on Google Drive.
   * 
   * @param drive a Google Drive object
   * @throws IOException
   */
  public static void removeKMLFiles(Drive drive) throws IOException {
    List<File> files = getDriveFiles(
        EndToEndTestUtils.trackListActivity.getApplicationContext(), drive);
    for (int i = 0; i < files.size(); i++) {
      File file = files.get(i);
      removeFile(file, drive);
    }
  }

  /**
   * Removes one file on Google Drive.
   * 
   * @param file the file to remove
   * @param drive a Google Drive object
   * @throws IOException
   */
  public static void removeFile(File file, Drive drive) throws IOException {
    drive.files().trash(file.getId()).execute();
  }

  /**
   * Enables sync feature in MyTracks settings.
   * 
   * @param accountName the account which is used to sync with
   */
  public static void enableSync(String accountName) {
//    EndToEndTestUtils.instrumentation.waitForIdleSync();
//    EndToEndTestUtils.findMenuItem(
//        EndToEndTestUtils.activityMytracks.getString(R.string.menu_settings), true);
//    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
//        .getString(R.string.settings_google));
//    EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
//        .getString(R.string.settings_google_account_title));
//    EndToEndTestUtils.instrumentation.waitForIdleSync();
//
//    // Whether test account is bound.
//    if (EndToEndTestUtils.SOLO.waitForText(accountName, 1, EndToEndTestUtils.SHORT_WAIT_TIME)) {
//      EndToEndTestUtils.SOLO.clickOnText(accountName);
//      EndToEndTestUtils.instrumentation.waitForIdleSync();
//      
//      if (EndToEndTestUtils.findTextView(EndToEndTestUtils.activityMytracks
//          .getString(R.string.settings_google_account_confirm_title)) != null) {
//        EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
//            .getString(R.string.generic_yes));
//      }
//    } else {
//      Assert.fail();
//      return;
//    }
//
//    EndToEndTestUtils.SOLO.waitForDialogToClose(EndToEndTestUtils.SHORT_WAIT_TIME);
//    EndToEndTestUtils.instrumentation.waitForIdleSync();
//    CheckBox syncCheckBox = EndToEndTestUtils.SOLO.getCurrentViews(CheckBox.class).get(0);
//    if (!syncCheckBox.isChecked()) {
//      EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
//          .getString(R.string.menu_sync_drive));
//
//      EndToEndTestUtils.SOLO.waitForText(
//          EndToEndTestUtils.activityMytracks.getString(R.string.sync_drive_confirm_message).split(
//              "%")[0], 1, EndToEndTestUtils.SHORT_WAIT_TIME);
//      Assert.assertTrue(EndToEndTestUtils.SOLO.searchText(accountName, true));
//      EndToEndTestUtils.SOLO.clickOnText(EndToEndTestUtils.activityMytracks
//          .getString(R.string.generic_yes));
//    }
//
//    // Add this sleep to work around a exception after switch account.
//    EndToEndTestUtils.sleep(15000);
//    EndToEndTestUtils.SOLO.goBack();
//    EndToEndTestUtils.SOLO.goBack();
//    EndToEndTestUtils.instrumentation.waitForIdleSync();
  }

  /**
   * Checks the files number on Google Drive
   * 
   * @param drive a Google Drive object
   * @throws IOException
   */
  public static void checkFilesNumber(Drive drive) throws IOException {
    EndToEndTestUtils.instrumentation.waitForIdleSync();
    long startTime = System.currentTimeMillis();
    int trackNumber = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount();
    List<File> files = getDriveFiles(EndToEndTestUtils.trackListActivity.getApplicationContext(),
        drive);
    while (System.currentTimeMillis() - startTime < MAX_TIME_TO_WAIT_SYNC) {
      try {
        if (files.size() == trackNumber) {
          return;
        }
        trackNumber = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount();
        files = getDriveFiles(EndToEndTestUtils.trackListActivity.getApplicationContext(), drive);
        EndToEndTestUtils.sleep(EndToEndTestUtils.SHORT_WAIT_TIME);
        EndToEndTestUtils.findMenuItem(
            EndToEndTestUtils.trackListActivity.getString(R.string.menu_sync_now), true);
      } catch (GoogleJsonResponseException e) {
        Log.e(TAG, e.getMessage(), e);
      }
    }
    Assert.assertEquals(files.size(), trackNumber);
  }

  /**
   * Checks the files number on Google Drive
   * 
   * @param number number of files on Google Drive
   */
  public static void checkTracksNumber(int number) {
    long startTime = System.currentTimeMillis();
    int trackNumber = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount();
    while (System.currentTimeMillis() - startTime < MAX_TIME_TO_WAIT_SYNC) {
      if (trackNumber == number) {
        return;
      }
      trackNumber = EndToEndTestUtils.SOLO.getCurrentViews(ListView.class).get(0).getCount();
      Log.d(TAG, trackNumber + ":" + number);
    }
    Assert.assertEquals(trackNumber, number);
  }

  /**
   * Checks one file on Google Drive
   * 
   * @param trackName the name of track
   * @param shouldExist true means this track should be existed
   * @param drive a Google Drive object
   * @return true means the actual result is same as expectation
   * @throws IOException
   */
  public static boolean checkFile(String trackName, boolean shouldExist, Drive drive)
      throws IOException {
    long startTime = System.currentTimeMillis();
    while (System.currentTimeMillis() - startTime < MAX_TIME_TO_WAIT_SYNC) {
      boolean exist = getFile(trackName, drive) != null;
      if (exist == shouldExist) {
        return true;
      }
    }
    Assert.fail();
    return false;
  }

  /**
   * Gets the content of a file on Google Drive.
   * 
   * @param file file to read
   * @param drive a Google Drive object
   * @return the string content of the file
   * @throws IOException
   */
  public static String getContentOfFile(File file, Drive drive) throws IOException {
    HttpResponse resp = drive.getRequestFactory()
        .buildGetRequest(new GenericUrl(file.getDownloadUrl())).execute();
    BufferedReader br = new BufferedReader(new InputStreamReader(resp.getContent()));
    StringBuilder sb = new StringBuilder();
    String line;
    while ((line = br.readLine()) != null) {
      sb.append(line);
    }
    String fileContent = sb.toString();
    br.close();
    return fileContent;
  }
}
