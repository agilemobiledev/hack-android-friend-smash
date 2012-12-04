package com.facebook.android.friendsmash;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.view.Surface;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import com.facebook.Session;
import com.facebook.SessionState;
import com.facebook.widget.LoginButton;

/**
 *  Entry point for the app that represents the home screen with the Play button etc. and
 *  also the login screen for the social version of the app - these screens will switch
 *  within this activity using Fragments.
 */
public class HomeActivity extends FragmentActivity {
	
	// Tag used when logging messages
    public static final String TAG = HomeActivity.class.getSimpleName();

	// ADD THE FOLLOWING LINE OF CODE TO DECLARE YOUR FACEBOOKSESSIONMANAGER OBJECT AS AN ATTRIBUTE
	// IN YOUR ACTIVITY:
	FacebookSessionManager facebookSessionManager;
    
    // Fragment attributes for switching between fragments and saving/restoring them
    // during the activity lifecycle
    final String FB_LOGGED_OUT_HOME_FRAGMENT_KEY = "fb_logged_out_home_fragment";
 	FBLoggedOutHomeFragment fbLoggedOutHomeFragment;
 	final String HOME_FRAGMENT_KEY = "home_fragment";
 	HomeFragment homeFragment;
 	
 	// Default orientation of the device at a point in time
 	protected int defaultOrientation;
 	
 	// Boolean recording whether the activity has been resumed so that
 	// the logic in onSessionStateChange is only executed if this is the case
 	private boolean isResumed = false;
 	
 	// Boolean indicating whether or not a switch to the HomeFragment should be made, which is attempted
 	// once the user information has been fetched - only used in the social version
 	protected boolean shouldSwitchToHomeFragment = false;
    
 	// Constructor
 	public HomeActivity() {
 		super();
 		
    	// ADD THE FOLLOWING LINE OF CODE IN YOUR ACTIVITY CONSTRUCTOR TO INSTANTIATE YOUR FACEBOOKSESSIONMANAGER:
    	facebookSessionManager = new FacebookSessionManager(this, new FacebookSessionManager.SessionStateChangeListener() {
    		public void onSessionStateChange(SessionState state, Exception exception) {
    			// Add code here to accommodate session changes
    			updateView();
    			if (homeFragment != null) {
    				if (state.isOpened()) {
	    				if (state.equals(SessionState.OPENED_TOKEN_UPDATED)) {
	    					// Only callback if the opened token has been updated - i.e. the user
	    					// has provided write permissions
	    					homeFragment.tokenUpdated();
	                    }
    				}
    			}
    		}
    	});
 	}
 	
 	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	
    	// ADD THE FOLLOWING LINE OF CODE INTO YOUR OVERRIDDEN ONCREATE METHOD IN YOUR ACTIVITY
    	// AFTER CALLING SUPER.ONCREATE(...):
    	facebookSessionManager.onCreate(savedInstanceState);
        
		defaultOrientation = getRequestedOrientation();
		setContentView(R.layout.activity_fragment);
		
		restoreFragment(savedInstanceState);
		FragmentManager manager = getSupportFragmentManager();
		Fragment activeFragment = manager.findFragmentById(R.id.fragmentContainer);
		Fragment correctFragment = getFragment();
		
		if (activeFragment != correctFragment)
		{
			manager.beginTransaction()
				.add(R.id.fragmentContainer, correctFragment)
				.commit();
		}
    }
 	
 	@Override
    public void onStart() {
 		super.onStart();
 		
 	    // ADD THE FOLLOWING LINE OF CODE INTO YOUR OVERRIDDEN ONSTART METHOD IN YOUR ACTIVITY
 	 	// AFTER CALLING SUPER.ONSTART(...):
 	 	facebookSessionManager.onStart();
    }
 	
 	@Override
    public void onStop() {
 		super.onStop();
 		
 	    // ADD THE FOLLOWING LINE OF CODE INTO YOUR OVERRIDDEN ONSTOP METHOD IN YOUR ACTIVITY
 	 	// AFTER CALLING SUPER.ONSTOP(...):
 	 	facebookSessionManager.onStop();
    }
 	
 	@Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
 		super.onActivityResult(requestCode, resultCode, data);
 		
 	    // ADD THE FOLLOWING LINE OF CODE INTO YOUR OVERRIDDEN ONACTIVITYRESULT METHOD IN YOUR ACTIVITY
 	 	// AFTER CALLING SUPER.ONACTIVITYRESULT(...):
 		facebookSessionManager.onActivityResult(requestCode, resultCode, data);
    }
 	
 	// Restore the fragment from the Bundle (and instantiate the others)
    private void restoreFragment(Bundle savedInstanceState) {
    	if (savedInstanceState != null) {
            FragmentManager manager = getSupportFragmentManager();
            fbLoggedOutHomeFragment = (FBLoggedOutHomeFragment) manager.getFragment(savedInstanceState, FB_LOGGED_OUT_HOME_FRAGMENT_KEY);
            homeFragment = (HomeFragment) manager.getFragment(savedInstanceState, HOME_FRAGMENT_KEY);
        }
    	if (fbLoggedOutHomeFragment == null) {
    		fbLoggedOutHomeFragment = new FBLoggedOutHomeFragment();
    	}
    	if (homeFragment == null) {
    		homeFragment = new HomeFragment();
    	}
    }
 	
	@SuppressWarnings("unused")
	protected Fragment getFragment() {
		if (FriendSmashApplication.isSocial == false) {
			return homeFragment;
		} else {
			Session session = Session.getActiveSession();
			if (session != null && session.isOpened()) {
				((FriendSmashApplication)getApplication()).setLoggedIn(true);
				return homeFragment;
			} else {
				((FriendSmashApplication)getApplication()).setLoggedIn(false);
				return fbLoggedOutHomeFragment;
			}
		}
	}
	
	@Override
    public void onResume() {
        super.onResume();
        isResumed = true;
        
        // Hide the notification bar
 		getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
 		
 		// Fetch the user information if the application data has been destroyed (i.e. the app hasn't been used
 		// for a while and memory on the device is low) - only do this if the session is open for the social version only
 		if (FriendSmashApplication.isSocial == true) {
	 		if ( ((FriendSmashApplication)getApplication()).getFriends() == null ||
	 				((FriendSmashApplication)getApplication()).getCurrentFBUser() == null ) {
	 			fetchUserInformation();
	 		}
 		}
    }
	
	//  Method to start the fixing of the orientation
	protected void startOrientationFix() {
		switch (((WindowManager) getSystemService(WINDOW_SERVICE))
		        .getDefaultDisplay().getRotation()) {
		    case Surface.ROTATION_90: 
		        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE); 
		        break;
		    case Surface.ROTATION_180: 
		        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT); 
		        break;          
		    case Surface.ROTATION_270: 
		        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE); 
		        break;
		    default : 
		        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT); 
		    }
	}
	
	// Method to end the fixing of the orientation
	protected void endOrientationFix() {
		setRequestedOrientation(defaultOrientation);
	}

    @Override
    public void onPause() {
        super.onPause();
        isResumed = false;
    }
	
	@Override
	protected void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		
	    // ADD THE FOLLOWING LINE OF CODE INTO YOUR OVERRIDDEN ONSAVEINSTANCESTATE METHOD IN YOUR ACTIVITY
	 	// AFTER CALLING SUPER.ONSAVEINSTANCESTATE(...):
		facebookSessionManager.onSaveInstanceState(outState);
		
        FragmentManager manager = getSupportFragmentManager();
        // Save the fragment that's currently displayed
        Fragment f = manager.findFragmentById(R.id.fragmentContainer);
        if (f == fbLoggedOutHomeFragment) {
        	manager.putFragment(outState, FB_LOGGED_OUT_HOME_FRAGMENT_KEY, fbLoggedOutHomeFragment);
        } else if (f == homeFragment) {
        	manager.putFragment(outState, HOME_FRAGMENT_KEY, homeFragment);
        }
	}
	
	/* Facebook Integration Only ... */

	// Call back on HomeActivity when the session state changes to update the view accordingly
	protected void updateView() {
		if (isResumed) {
			Session session = Session.getActiveSession();
			if (session.isOpened() && ((FriendSmashApplication)getApplication()).isLoggedIn() == false && homeFragment != null) {
				// Not logged in, but should be ...
				if (fbLoggedOutHomeFragment != null && fbLoggedOutHomeFragment.progressContainer != null) {
					fbLoggedOutHomeFragment.progressContainer.setVisibility(View.VISIBLE);
				}
				// Get the user information and load the HomeFragment
				shouldSwitchToHomeFragment = true;
				fetchUserInformation();
				// Set loggedIn boolean to true now that the user is logged in
				((FriendSmashApplication)getApplication()).setLoggedIn(true);
				// Set the application.scoreboardEntriesVector to null
				((FriendSmashApplication)getApplication()).scoreboardEntriesVector = null;
	        } else if (session.isClosed() && ((FriendSmashApplication)getApplication()).isLoggedIn() == true && fbLoggedOutHomeFragment != null) {
				// Logged in, but shouldn't be, so load the fbLoggedOutHomeFragment
	        	FragmentManager manager = getSupportFragmentManager();
	        	FragmentTransaction transaction = manager.beginTransaction();
	            transaction.replace(R.id.fragmentContainer, fbLoggedOutHomeFragment).commit();
	            // Set loggedIn boolean to false now that the user is logged out
	            ((FriendSmashApplication)getApplication()).setLoggedIn(false);
	            // Set the application.scoreboardEntriesVector to null
				((FriendSmashApplication)getApplication()).scoreboardEntriesVector = null;
	        }
			
			// Note that error checking for failed logins is done as within an ErrorListener attached to the
			// LoginButton within FBLoggedOutHomeFragment
		}
	}
	
	// Fetch user information (either because the user has just logged in or the app has been destroyed by the Android
	// system due to memory constraints and the data needs to be re-fetched).  In the former case of the user logging in,
	// the shouldSwitchToHomeFragment boolean will be set and the activity will switch to the personalized homeFragment
	protected void fetchUserInformation() {
		final Session session = Session.getActiveSession();
		if (session != null && session.isOpened()) {
			// If the session is open, make an API call to get user information required for the app
			
			if (shouldSwitchToHomeFragment == true) {
				// Fix the screen orientation during these requests to avoid issues in fragment switching if this activity
				// is destroyed and recreated
				startOrientationFix();
			} else {
				// It is likely the homeFragment is displaying (i.e. session has been resumed after a while)
				// so show the progressContainer in this while fetching the fresh user data
				if (homeFragment != null && homeFragment.progressContainer != null) {
					homeFragment.progressContainer.setVisibility(View.VISIBLE);
				}
			}
			
			// Fetch the user information using the facebookIntegrator - will end by calling endFetchUserInformation()
			((FriendSmashApplication)getApplication()).facebookIntegrator.fetchUserInformation(this);
		}
	}
	
	// Called once the user information has been fetched to do some tidying up
	protected void endFetchUserInformation() {
		// End the orientation fix in case it is fixed
		endOrientationFix();

		// Hide the homeFragment progressContainer in case it is showing
		if (homeFragment != null && homeFragment.progressContainer != null) {
			homeFragment.progressContainer.setVisibility(View.INVISIBLE);
		}
	}
	
	// Switches to the personalized homeFragment as the user has just logged in
	protected void loadPersonalizedFragment() {
		// Load the HomeFragment personalized
		FragmentManager manager = getSupportFragmentManager();
		if (manager != null) {
			// Now that the user info has been fetched, change to this HomeFragment
			FragmentTransaction transaction = manager.beginTransaction();
			transaction.replace(R.id.fragmentContainer, homeFragment).commit();
			
			// Reset the shouldSwitchToHomeFragment
			shouldSwitchToHomeFragment = false;
			
			// Hide the progressContainer in fbLoggedOutHomeFragment 
			if (fbLoggedOutHomeFragment != null && fbLoggedOutHomeFragment.progressContainer != null) {
				fbLoggedOutHomeFragment.progressContainer.setVisibility(View.INVISIBLE);
			}
		}
		else {
			showErrorAndLogout("Error switching screens - Please try again");
		}
	}
	
	// Show user error message as a toast and log them out
	protected void showErrorAndLogout(String error) {
		// Show the error
		showError(error);
		
		// Hide the spinner if it's showing
		if (fbLoggedOutHomeFragment != null && fbLoggedOutHomeFragment.progressContainer != null) {
			fbLoggedOutHomeFragment.progressContainer.setVisibility(View.INVISIBLE);
		}
		
		// Close the session, which will cause a callback to show the logout screen
		Session.getActiveSession().closeAndClearTokenInformation();
		
		// Clear any permissions associated with the LoginButton
		LoginButton loginButton = (LoginButton) findViewById(R.id.loginButton);
		if (loginButton != null) {
			loginButton.clearPermissions();
		}
	}
	
	// Show user error message as a toast
	protected void showError(String error) {
		Toast.makeText(this, error, Toast.LENGTH_LONG).show();
	}
	
}
