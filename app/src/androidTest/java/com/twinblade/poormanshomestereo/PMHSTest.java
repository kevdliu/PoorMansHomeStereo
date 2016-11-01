/*
 * This is an example test project created in Eclipse to test NotePad which is a sample
 * project located in AndroidSDK/samples/android-11/NotePad
 *
 *
 * You can run these test cases either on the emulator or on device. Right click
 * the test project and select Run As --> Run As Android JUnit Test
 *
 * @author Renas Reda, renas.reda@robotium.com
 *
 */

package com.twinblade.poormanshomestereo;

import com.robotium.solo.Solo;
import com.roughike.bottombar.BottomBar;

import android.support.test.InstrumentationRegistry;
import android.support.test.rule.ActivityTestRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;


@RunWith(AndroidJUnit4.class)
public class PMHSTest {

   /* private static final String NOTE_1 = "Note 1";
    private static final String NOTE_2 = "Note 2";
*/

    @Rule
    public ActivityTestRule<ControllerActivity> activityTestRule =
            new ActivityTestRule<>(ControllerActivity.class);

    private Solo solo;


    @Before
    public void setUp() throws Exception {
        //setUp() is run before a test case is started.
        //This is where the solo object is created.
        solo = new Solo(InstrumentationRegistry.getInstrumentation(),
                activityTestRule.getActivity());
    }

    @After
    public void tearDown() throws Exception {
        //tearDown() is run after a test case has finished.
        //finishOpenedActivities() will finish all the activities that have been opened during the test execution.
        solo.finishOpenedActivities();
    }

    @Test
    public void testSongToPlayClicked() throws Exception {
        solo.assertCurrentActivity("Expected Controller Activity", ControllerActivity.class);
        BottomBar nav_bar = (BottomBar) solo.getCurrentActivity().findViewById(R.id.nav_bar);
        int[] nav_location = new int[2];
        int width = nav_bar.getWidth();
        int height = nav_bar.getHeight();
        nav_bar.getLocationOnScreen(nav_location);

        // Click queue tab
        solo.clickOnScreen(nav_location[0] + (3 * width / 8), nav_location[1] + (height / 2));
        assertTrue(solo.waitForFragmentByTag(Constants.FRAGMENT_QUEUE, 5*1000));

        // Get ListView of songs
        ArrayList<View> views = solo.getViews();
        ListView songs = null;
        for (View v : views) {
            if (v instanceof ListView) {
                songs = (ListView) v;
                break;
            }
        }

        // Queue should be empty
        assertTrue(songs != null);
        assertTrue(songs.getCount() == 0);

        // Click songs tab
        solo.clickOnScreen(nav_location[0] + (5 * width / 8), nav_location[1] + (height / 2));
        assertTrue(solo.waitForFragmentByTag(Constants.FRAGMENT_SONGS, 5*1000));

        // Click first song
        int[] songs_list_location = new int[2];
        songs.getLocationOnScreen(songs_list_location);
        solo.clickOnScreen(songs_list_location[0] + songs.getWidth() / 3, songs_list_location[1] + songs.getHeight() / 3);

        assertTrue(solo.waitForFragmentByTag(Constants.FRAGMENT_QUEUE, 5*1000));

        // Reset view
        views = solo.getViews();
        songs = null;
        for (View v : views) {
            if (v instanceof ListView) {
                songs = (ListView) v;
                break;
            }
        }

        // Songs should be added to queue
        assertTrue(songs.getCount() > 0);
    }

    @Test
    public void testRapidFragmentSwitching() throws Exception {

        solo.assertCurrentActivity("Expected Controller Activity", ControllerActivity.class);

        BottomBar nav_bar = (BottomBar) solo.getCurrentActivity().findViewById(R.id.nav_bar);
        int[] location = new int[2];
        int width = nav_bar.getWidth();
        int height = nav_bar.getHeight();
        nav_bar.getLocationOnScreen(location);
        for (int i = 0; i < 50; i++) {
            solo.clickOnScreen(location[0] + (width / 8), location[1] + (height / 2));
            assertTrue(solo.waitForFragmentByTag(Constants.FRAGMENT_SPEAKERS, 5 * 1000));
            solo.clickOnScreen(location[0] + (3 * width / 8), location[1] + (height / 2));
            assertTrue(solo.waitForFragmentByTag(Constants.FRAGMENT_QUEUE, 5 * 1000));
            solo.clickOnScreen(location[0] + (5 * width / 8), location[1] + (height / 2));
            assertTrue(solo.waitForFragmentByTag(Constants.FRAGMENT_SONGS, 5 * 1000));
            solo.clickOnScreen(location[0] + (7 * width / 8), location[1] + (height / 2));
            assertTrue(solo.waitForFragmentByTag(Constants.FRAGMENT_SEARCH, 5 * 1000));
        }
    }
}
