/*
 * Copyright 2012 Google Inc.
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
package org.dicadeveloper.runnerapp.io.maps;

import org.dicadeveloper.runnerapp.Constants;
import org.dicadeveloper.runnerapp.TrackStubUtils;
import org.dicadeveloper.runnerapp.content.MyTracksProviderUtils;
import org.dicadeveloper.runnerapp.content.MyTracksProviderUtils.LocationIterator;
import org.dicadeveloper.runnerapp.content.Track;
import org.dicadeveloper.runnerapp.content.Waypoint;
import org.dicadeveloper.runnerapp.io.gdata.maps.MapsGDataConverter;
import org.dicadeveloper.runnerapp.io.sendtogoogle.SendRequest;
import com.google.android.testing.mocking.AndroidMock;
import com.google.android.testing.mocking.UsesMocks;

import android.accounts.Account;
import android.database.Cursor;
import android.location.Location;
import android.test.AndroidTestCase;

import java.util.List;

import org.xmlpull.v1.XmlPullParserException;

/**
 * Tests {@link SendMapsAsyncTask}.
 * 
 * @author Youtao Liu
 */
public class SendMapsAsyncTaskTest extends AndroidTestCase {

  private static final long TRACK_ID = 1;
  // Records the run times of {@link SendMapsAsyncTaskMock#uploadMarker(String,
  // String, String, Location)}
  private int uploadMarkerCounter = 0;
  // Records the run times of {@link
  // SendMapsAsyncTaskMock#prepareAndUploadPoints(Track, List<Location>,
  // boolean)}
  private int prepareAndUploadPointsCounter = 0;

  private SendMapsActivity sendMapsActivityMock;
  private MyTracksProviderUtils myTracksProviderUtilsMock;
  private SendRequest sendRequest;

  private class SendMapsAsyncTaskMock extends SendMapsAsyncTask {

    private boolean[] uploadMarkerResult = { false, false };
    private boolean prepareAndUploadPointsResult = false;

    private SendMapsAsyncTaskMock(SendMapsActivity activity, long trackId, Account account,
        MyTracksProviderUtils myTracksProviderUtils) {
      super(activity, trackId, account, myTracksProviderUtils);
    }

    @Override
    boolean uploadMarker(String title, String description, String iconUrl, Location location) {
      int runTimes = uploadMarkerCounter++;
      return uploadMarkerResult[runTimes];
    }

    @Override
    boolean prepareAndUploadPoints(Track track, List<Location> locations, boolean lastBatch) {
      prepareAndUploadPointsCounter++;
      return prepareAndUploadPointsResult;
    }
  }

  @Override
  @UsesMocks({ SendMapsActivity.class, MyTracksProviderUtils.class })
  protected void setUp() throws Exception {
    super.setUp();
    uploadMarkerCounter = 0;
    prepareAndUploadPointsCounter = 0;
    sendMapsActivityMock = AndroidMock.createMock(SendMapsActivity.class);
    myTracksProviderUtilsMock = AndroidMock.createMock(MyTracksProviderUtils.class);
    sendRequest = new SendRequest(TRACK_ID);
    AndroidMock.expect(sendMapsActivityMock.getApplicationContext()).andReturn(getContext());
  }

  /**
   * Tests {@link SendMapsAsyncTask#fetchSendMapId(Track)} when chooseMapId is
   * null and makes sure it returns false.
   */
  public void testFetchSendMapId_nullMapID() {
    Track track = TrackStubUtils.createTrack(1);
    AndroidMock.replay(sendMapsActivityMock, myTracksProviderUtilsMock);
    // Makes chooseMapId to null.
    SendMapsAsyncTask sendMapsAsyncTask = new SendMapsAsyncTask(sendMapsActivityMock,
        sendRequest.getTrackId(), sendRequest.getAccount(), myTracksProviderUtilsMock);
    // Returns false for an exception would be thrown.
    assertFalse(sendMapsAsyncTask.fetchSendMapId(track));
    AndroidMock.verify(sendMapsActivityMock, myTracksProviderUtilsMock);
  }

  /**
   * Tests the method {@link SendMapsAsyncTask#uploadAllTrackPoints(Track)} when
   * the location iterator has no item. And makes sure it returns true.
   */
  public void testUploadAllTrackPoints_empptyLocationIterator() {
    LocationIterator locationIterator = AndroidMock.createMock(LocationIterator.class);
    AndroidMock.expect(locationIterator.hasNext()).andReturn(false);
    locationIterator.close();

    Track track = TrackStubUtils.createTrack(1);
    AndroidMock.expect(myTracksProviderUtilsMock.getTrackPointLocationIterator(
        TRACK_ID, -1L, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY))
        .andReturn(locationIterator);
    AndroidMock.replay(sendMapsActivityMock, myTracksProviderUtilsMock, locationIterator);
    SendMapsAsyncTask sendMapsAsyncTask = new SendMapsAsyncTask(sendMapsActivityMock,
        sendRequest.getTrackId(), sendRequest.getAccount(), myTracksProviderUtilsMock);
    assertTrue(sendMapsAsyncTask.uploadAllTrackPoints(track));
    AndroidMock.verify(sendMapsActivityMock, myTracksProviderUtilsMock, locationIterator);
  }

  /**
   * Tests the method {@link SendMapsAsyncTask#uploadAllTrackPoints(Track)} when
   * uploads the first marker is failed.
   */
  @UsesMocks(Cursor.class)
  public void testUploadAllTrackPoints_uploadFirstMarkerFailed() {
    Track track = TrackStubUtils.createTrack(1);

    LocationIterator locationIterator = AndroidMock.createMock(LocationIterator.class);
    AndroidMock.expect(locationIterator.hasNext()).andReturn(true);
    AndroidMock.expect(locationIterator.next()).andReturn(new Location("1"));
    locationIterator.close();

    AndroidMock.expect(myTracksProviderUtilsMock.getTrackPointLocationIterator(
        TRACK_ID, -1L, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY))
        .andReturn(locationIterator);

    AndroidMock.replay(sendMapsActivityMock, myTracksProviderUtilsMock, locationIterator);
    SendMapsAsyncTaskMock sendMapsAsyncTask = new SendMapsAsyncTaskMock(sendMapsActivityMock,
        sendRequest.getTrackId(), sendRequest.getAccount(), myTracksProviderUtilsMock);
    sendMapsAsyncTask.uploadMarkerResult[0] = false;
    assertFalse(sendMapsAsyncTask.uploadAllTrackPoints(track));
    assertEquals(1, uploadMarkerCounter);
    AndroidMock.verify(sendMapsActivityMock, myTracksProviderUtilsMock, locationIterator);
  }

  /**
   * Tests the method {@link SendMapsAsyncTask#uploadAllTrackPoints(Track)} when
   * uploads the two markers is successful but failed when
   * prepareAndUploadPoints.
   */
  @UsesMocks(Cursor.class)
  public void testUploadAllTrackPoints_prepareAndUploadPointsFailed() {
    Track track = TrackStubUtils.createTrack(1);

    LocationIterator locationIterator = AndroidMock.createMock(LocationIterator.class);
    AndroidMock.expect(locationIterator.hasNext()).andReturn(true).times(2);
    AndroidMock.expect(locationIterator.hasNext()).andReturn(false);
    AndroidMock.expect(locationIterator.next()).andReturn(new Location("1")).times(2);
    locationIterator.close();

    AndroidMock.expect(myTracksProviderUtilsMock.getTrackPointLocationIterator(
        TRACK_ID, -1L, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY))
        .andReturn(locationIterator);

    AndroidMock.replay(sendMapsActivityMock, myTracksProviderUtilsMock, locationIterator);
    SendMapsAsyncTaskMock sendMapsAsyncTask = new SendMapsAsyncTaskMock(sendMapsActivityMock,
        sendRequest.getTrackId(), sendRequest.getAccount(), myTracksProviderUtilsMock);
    // For will be failed when run prepareAndUploadPoints, it no require to
    // set uploadMarkerResult[1].
    sendMapsAsyncTask.uploadMarkerResult[0] = true;
    sendMapsAsyncTask.prepareAndUploadPointsResult = false;
    assertFalse(sendMapsAsyncTask.uploadAllTrackPoints(track));
    assertEquals(1, uploadMarkerCounter);
    assertEquals(1, prepareAndUploadPointsCounter);
    AndroidMock.verify(sendMapsActivityMock, myTracksProviderUtilsMock, locationIterator);
  }

  /**
   * Tests the method {@link SendMapsAsyncTask#uploadAllTrackPoints(Track)} when
   * uploads the last marker is failed.
   */
  @UsesMocks(Cursor.class)
  public void testUploadAllTrackPoints_uploadLastMarkerFailed() {
    Track track = TrackStubUtils.createTrack(1);

    LocationIterator locationIterator = AndroidMock.createMock(LocationIterator.class);
    AndroidMock.expect(locationIterator.hasNext()).andReturn(true).times(2);
    AndroidMock.expect(locationIterator.hasNext()).andReturn(false);
    AndroidMock.expect(locationIterator.next()).andReturn(new Location("1")).times(2);
    locationIterator.close();

    AndroidMock.expect(myTracksProviderUtilsMock.getTrackPointLocationIterator(
        TRACK_ID, -1L, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY))
        .andReturn(locationIterator);

    AndroidMock.replay(sendMapsActivityMock, myTracksProviderUtilsMock, locationIterator);
    SendMapsAsyncTaskMock sendMapsAsyncTask = new SendMapsAsyncTaskMock(sendMapsActivityMock,
        sendRequest.getTrackId(), sendRequest.getAccount(), myTracksProviderUtilsMock);
    sendMapsAsyncTask.uploadMarkerResult[0] = true;
    sendMapsAsyncTask.uploadMarkerResult[1] = false;
    sendMapsAsyncTask.prepareAndUploadPointsResult = true;
    assertFalse(sendMapsAsyncTask.uploadAllTrackPoints(track));
    assertEquals(2, uploadMarkerCounter);
    assertEquals(1, prepareAndUploadPointsCounter);
    AndroidMock.verify(sendMapsActivityMock, myTracksProviderUtilsMock, locationIterator);
  }

  /**
   * Tests the method {@link SendMapsAsyncTask#uploadAllTrackPoints(Track)} when
   * return true.a
   */
  @UsesMocks(Cursor.class)
  public void testUploadAllTrackPoints_success() {
    Track track = TrackStubUtils.createTrack(1);

    LocationIterator locationIterator = AndroidMock.createMock(LocationIterator.class);
    AndroidMock.expect(locationIterator.hasNext()).andReturn(true).times(2);
    AndroidMock.expect(locationIterator.hasNext()).andReturn(false);
    AndroidMock.expect(locationIterator.next()).andReturn(new Location("1")).times(2);
    locationIterator.close();

    AndroidMock.expect(myTracksProviderUtilsMock.getTrackPointLocationIterator(
        TRACK_ID, -1L, false, MyTracksProviderUtils.DEFAULT_LOCATION_FACTORY))
        .andReturn(locationIterator);

    AndroidMock.replay(sendMapsActivityMock, myTracksProviderUtilsMock, locationIterator);
    SendMapsAsyncTaskMock sendMapsAsyncTask = new SendMapsAsyncTaskMock(sendMapsActivityMock,
        sendRequest.getTrackId(), sendRequest.getAccount(), myTracksProviderUtilsMock);
    sendMapsAsyncTask.uploadMarkerResult[0] = true;
    sendMapsAsyncTask.uploadMarkerResult[1] = true;
    sendMapsAsyncTask.prepareAndUploadPointsResult = true;
    assertTrue(sendMapsAsyncTask.uploadAllTrackPoints(track));
    assertEquals(2, uploadMarkerCounter);
    assertEquals(1, prepareAndUploadPointsCounter);
    AndroidMock.verify(sendMapsActivityMock, myTracksProviderUtilsMock, locationIterator);
  }

  /**
   * Tests the method {@link SendMapsAsyncTask#uploadWaypoints()} when cursor is
   * null.
   */
  @UsesMocks(Cursor.class)
  public void testUploadWaypoints_nullCursor() {
    AndroidMock.expect(myTracksProviderUtilsMock.getWaypointCursor(
        TRACK_ID, -1L, Constants.MAX_LOADED_WAYPOINTS_POINTS)).andReturn(null);
    AndroidMock.replay(sendMapsActivityMock, myTracksProviderUtilsMock);
    SendMapsAsyncTask sendMapsAsyncTask = new SendMapsAsyncTask(sendMapsActivityMock,
        sendRequest.getTrackId(), sendRequest.getAccount(), myTracksProviderUtilsMock);
    assertTrue(sendMapsAsyncTask.uploadWaypoints());
    AndroidMock.verify(sendMapsActivityMock, myTracksProviderUtilsMock);
  }

  /**
   * Tests the method {@link SendMapsAsyncTask#uploadWaypoints()} when there is
   * only one point.
   */
  @UsesMocks(Cursor.class)
  public void testUploadWaypoints_onePoint() {
    Cursor cursorMock = AndroidMock.createMock(Cursor.class);
    AndroidMock.expect(cursorMock.moveToFirst()).andReturn(true);
    // Only one point, so next is null.
    AndroidMock.expect(cursorMock.moveToNext()).andReturn(false);
    cursorMock.close();

    AndroidMock.expect(myTracksProviderUtilsMock.getWaypointCursor(
        TRACK_ID, -1L, Constants.MAX_LOADED_WAYPOINTS_POINTS)).andReturn(cursorMock);

    AndroidMock.replay(sendMapsActivityMock, myTracksProviderUtilsMock, cursorMock);
    SendMapsAsyncTask sendMapsAsyncTask = new SendMapsAsyncTask(sendMapsActivityMock,
        sendRequest.getTrackId(), sendRequest.getAccount(), myTracksProviderUtilsMock);

    assertTrue(sendMapsAsyncTask.uploadWaypoints());
    AndroidMock.verify(sendMapsActivityMock, myTracksProviderUtilsMock, cursorMock);
  }

  /**
   * Tests the method {@link SendMapsAsyncTask#uploadWaypoints()}. Makes sure a
   * cursor is created and a way point is created with such cursor.
   * 
   * @throws XmlPullParserException
   */
  @UsesMocks(Cursor.class)
  public void testUploadWaypoints() throws XmlPullParserException {
    Cursor cursorMock = AndroidMock.createMock(Cursor.class);
    MapsGDataConverter mapsGDataConverterMock = new MapsGDataConverter();

    AndroidMock.expect(cursorMock.moveToFirst()).andReturn(true);
    AndroidMock.expect(cursorMock.moveToNext()).andReturn(true);
    cursorMock.close();

    AndroidMock.expect(myTracksProviderUtilsMock.getWaypointCursor(
        TRACK_ID, -1L, Constants.MAX_LOADED_WAYPOINTS_POINTS)).andReturn(cursorMock);
    Waypoint waypoint = new Waypoint();
    waypoint.setLocation(TrackStubUtils.createMyTracksLocation());
    AndroidMock.expect(myTracksProviderUtilsMock.createWaypoint(cursorMock))
        .andReturn(waypoint).times(1);

    AndroidMock.replay(sendMapsActivityMock, myTracksProviderUtilsMock, cursorMock);
    SendMapsAsyncTask sendMapsAsyncTask = new SendMapsAsyncTask(sendMapsActivityMock,
        sendRequest.getTrackId(), sendRequest.getAccount(), myTracksProviderUtilsMock);
    sendMapsAsyncTask.setMapsGDataConverter(mapsGDataConverterMock);
    // Would be failed for there is no source for uploading.
    assertFalse(sendMapsAsyncTask.uploadWaypoints());
    AndroidMock.verify(sendMapsActivityMock, myTracksProviderUtilsMock, cursorMock);
  }

  /**
   * Tests the method {@link SendMapsAsyncTask#getPercentage(int, int)}.
   */
  public void testCountPercentage() {
    assertEquals(SendMapsAsyncTask.PROGRESS_UPLOAD_DATA_MIN, SendMapsAsyncTask.getPercentage(0, 5));
    assertEquals(
        SendMapsAsyncTask.PROGRESS_UPLOAD_DATA_MAX, SendMapsAsyncTask.getPercentage(50, 50));
    assertEquals((int) ((double) 5 / 11
        * (SendMapsAsyncTask.PROGRESS_UPLOAD_DATA_MAX - SendMapsAsyncTask.PROGRESS_UPLOAD_DATA_MIN)
        + SendMapsAsyncTask.PROGRESS_UPLOAD_DATA_MIN), SendMapsAsyncTask.getPercentage(5, 11));
  }

}