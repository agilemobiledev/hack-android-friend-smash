package com.facebook.android.friendsmash;

import java.util.Arrays;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.facebook.Session;
import com.facebook.widget.ProfilePictureView;
import com.facebook.widget.WebDialog;

/**
 *  Fragment to be shown once the user is logged in on the social version of the game or
 *  the start screen for the non-social version of the game
 */
public class HomeFragment extends Fragment {
	
	// Tag used when logging errors
    public static final String TAG = HomeFragment.class.getSimpleName();
	
	// Store the Application (as you can't always get to it when you can't access the Activity - e.g. during rotations)
	FriendSmashApplication application;
	
	// FrameLayout of the gameOverContainer
	FrameLayout gameOverContainer;
	
	// FrameLayout of the progressContainer
	FrameLayout progressContainer;
	
	// TextView for the You Scored message
	TextView youScoredTextView;
	
	// userImage ProfilePictureView to display the user's profile pic
	ProfilePictureView userImage;
	
	// TextView for the user's name
	TextView welcomeTextView;
	
	// Buttons ...
	ImageView playButton;
	ImageView scoresButton;
	ImageView challengeButton;
	ImageView bragButton;
	
	// Parameters of a WebDialog that should be displayed
	WebDialog dialog = null;
	String dialogAction = null;
	Bundle dialogParams = null;
	
	// Runnable task used to show the Game Over message briefly at the end of a game
	// so that the user doesn't accidentally press any buttons once the game
	// is over
	Runnable gameOverTask = null;
	
	// Boolean indicating whether or not the game over message is displaying
	boolean gameOverMessageDisplaying = false;
	
	// Handler for putting messages on Main UI thread from background threads periodically
	Handler timerHandler;
	
	// Boolean indicating if the game has been launched directly from deep linking already
	// so that it isn't launched again when the view is created (e.g. on rotation)
	private boolean gameLaunchedFromDeepLinking = false;
	
	// Attributes for posting back to Facebook
	private static final List<String> PERMISSIONS = Arrays.asList("publish_actions");
	private static final int REAUTH_ACTIVITY_CODE = 100;
	private static final String PENDING_POST_KEY = "pendingPost";
	private boolean pendingPost = false;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setRetainInstance(true);
		
		// Instantiate the timerHandler
		timerHandler = new Handler();
		
		application = (FriendSmashApplication) getActivity().getApplication();
	}
	
	@SuppressWarnings("unused")
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		
		View v;
		
		if (FriendSmashApplication.isSocial == false) {
			v = inflater.inflate(R.layout.fragment_home, parent, false);
		} else {
			v = inflater.inflate(R.layout.fragment_home_fb_logged_in, parent, false);
			
			// Set the userImage ProfilePictureView
			userImage = (ProfilePictureView) v.findViewById(R.id.userImage);
			
			// Set the welcomeTextView TextView
			welcomeTextView = (TextView)v.findViewById(R.id.welcomeTextView);
			
			if (application.getCurrentFBUser() != null) {
				// Personalize this HomeFragment if the currentFBUser has been fetched
				
				// Set the id for the userImage ProfilePictureView
	            // that in turn displays the profile picture
	            userImage.setProfileId(application.getCurrentFBUser().getId());
	            // and show the cropped (square) version ...
	            userImage.setCropped(true);
	            
	            // Set the welcomeTextView Textview's text to the user's name
	            welcomeTextView.setText("Welcome, " + application.getCurrentFBUser().getFirstName());
			}
			
			scoresButton = (ImageView)v.findViewById(R.id.scoresButton);
			scoresButton.setOnTouchListener(new View.OnTouchListener()
	    	{
	            @Override
				public boolean onTouch(View v, MotionEvent event) {
	            	onScoresButtonTouched();
					return false;
				}
	        });
			
			challengeButton = (ImageView)v.findViewById(R.id.challengeButton);
			challengeButton.setOnTouchListener(new View.OnTouchListener()
	    	{
	            @Override
				public boolean onTouch(View v, MotionEvent event) {
	            	onChallengeButtonTouched();
					return false;
				}
	        });
			
			bragButton = (ImageView)v.findViewById(R.id.bragButton);
			bragButton.setOnTouchListener(new View.OnTouchListener()
	    	{
	            @Override
				public boolean onTouch(View v, MotionEvent event) {
	            	onBragButtonTouched();
					return false;
				}
	        });
			
			updateButtonVisibility();
		}
		
		gameOverContainer = (FrameLayout)v.findViewById(R.id.gameOverContainer);
		
		progressContainer = (FrameLayout)v.findViewById(R.id.progressContainer);
		
		youScoredTextView = (TextView)v.findViewById(R.id.youScoredTextView);
		updateYouScoredTextView();
		
		playButton = (ImageView)v.findViewById(R.id.playButton);
		playButton.setOnTouchListener(new View.OnTouchListener()
    	{
            @Override
			public boolean onTouch(View v, MotionEvent event) {
            	onPlayButtonTouched();
				return false;
			}
        });
		
		// Instantiate the gameOverTask
		gameOverTask = new Runnable()
		{
			public void run()
			{
				// Hide the gameOverContainer
				gameOverContainer.setVisibility(View.INVISIBLE);
				
				// Set the gameOverMessageDisplaying boolean to false
				gameOverMessageDisplaying = false;
				
				if (FriendSmashApplication.isSocial == true) {
					// Post all information to facebook as appropriate
					facebookPostAll();
					
					// Set the scoreboardEntriesVector to null so that the scoreboard is refreshed
					// now that the player has played another game in case they have a higher score or
					// any of their friends have a higher score
					application.scoreboardEntriesVector = null;
				}
			}
		};
		
		// Hide the gameOverContainer
		gameOverContainer.setVisibility(View.INVISIBLE);
		
		// Hide the progressContainer
		if (progressContainer != null) {
			progressContainer.setVisibility(View.INVISIBLE);
		}
		
		// Restore the state
		restoreState(savedInstanceState);
		
		return v;
	}
	
	// Restores the state during onCreateView
	private void restoreState(Bundle savedInstanceState) {
		if (savedInstanceState != null) {
			pendingPost = savedInstanceState.getBoolean(PENDING_POST_KEY, false);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();
		
		// Cancel any running gameOverTasks
		timerHandler.removeCallbacks(gameOverTask);
		
		// Hide the gameOverContainer
		gameOverContainer.setVisibility(View.INVISIBLE);
	}
	
	@Override
	public void onResume() {
		super.onResume();

		if (gameLaunchedFromDeepLinking == false) {
			// As long as the game hasn't been launched yet from deep linking, see if it has been
			// deep linked and launch the game appropriately
			Uri target = getActivity().getIntent().getData();
			if (target != null) {
				String path = target.getPath();
				
				Intent i = new Intent(getActivity(), GameActivity.class);
				
			    // Target is the deep-link Uri, so skip loading this home screen and load the game
				// directly with the sending user's picture to smash
				String graphRequestIDsForSendingUser = target.getQueryParameter("request_ids");
				
				if (graphRequestIDsForSendingUser != null) {
					// Deep linked through a Request and use the latest request (request_id) if multiple requests have been sent
					String [] graphRequestIDsForSendingUsers = graphRequestIDsForSendingUser.split(",");
					String graphRequestIDForSendingUser = graphRequestIDsForSendingUsers[graphRequestIDsForSendingUsers.length-1];
					Bundle bundle = new Bundle();
					bundle.putString("request_id", graphRequestIDForSendingUser);
					i.putExtras(bundle);
					gameLaunchedFromDeepLinking = true;
					startGame(i);
				} else if (path.contains("challenge_brag_") == true) {
					// Deep linked through a feed post, so extract the sending user's user id from the last part
					// of the path and start the game smashing this user
					String sendingUserID = path.replaceFirst("/challenge_brag_", "");
					Bundle bundle = new Bundle();
					bundle.putString("user_id", sendingUserID);
					i.putExtras(bundle);
					gameLaunchedFromDeepLinking = true;
					startGame(i);
				}
			} else {
			    // Launched with no deep-link Uri, so just continue as normal and load the home screen
			}
		}
		
		if (gameLaunchedFromDeepLinking == false && gameOverMessageDisplaying == true) {
			// The game hasn't just been launched from deep linking and the game over message should still be displaying, so ...
			
			// Complete the game over logic
			completeGameOver(750);
		}
	}
	
	@SuppressWarnings("unused")
	private void startGame(Intent i) {
		if (FriendSmashApplication.isSocial == true) {
			if (application.getFriends() != null) {
				// Only start a game if the application data for the current user hasn't been destroyed
		        startActivityForResult(i, 0);
			} else {
				// If the application data has been destroyed, re-fetch it
				((HomeActivity) getActivity()).fetchUserInformation();
			}
		} else {
			startActivityForResult(i, 0);
		}
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		// If a dialog exists, create a new dialog (as the screen may have rotated so needs
		// new dimensions) and show it
		if (dialog != null) {
			((FriendSmashApplication)getActivity().getApplication()).facebookIntegrator.showDialogWithoutNotificationBar(this, dialogAction, dialogParams);
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		
		// If a dialog exists and is showing, dismiss it
		if (dialog != null) {
			dialog.dismiss();
		}
	}
	
	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		outState.putBoolean(PENDING_POST_KEY, pendingPost);
	}

	// Called when the Play button is touched
	private void onPlayButtonTouched() {
        Intent i = new Intent(getActivity(), GameActivity.class);
        startGame(i);
    }
	
	// Called when teh Challenge button is touched
	private void onChallengeButtonTouched() {
		((FriendSmashApplication)getActivity().getApplication()).facebookIntegrator.sendRequest(this);
	}
	
	// Called when the Brag button is touched
	private void onBragButtonTouched() {
		((FriendSmashApplication)getActivity().getApplication()).facebookIntegrator.sendBrag(this);
	}
	
	// Called when the Scores button is touched
	private void onScoresButtonTouched() {
		Intent i = new Intent(getActivity(), ScoreboardActivity.class);
		startActivityForResult(i, 0);
	}
	
	// Called when the Activity is returned to - needs to be caught for the following two scenarios:
	// 1. Returns from an authentication dialog requesting write permissions - tested with
	//    requestCode == REAUTH_ACTIVITY_CODE - if successfully got permissions, execute a session
	//    state change callback to then attempt to post their information to Facebook (again)
	// 2. Returns from a finished game - test status with resultCode and if successfully ended, update
	//    their score and complete the game over process, otherwise show an error if there is one
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		
		if (requestCode == REAUTH_ACTIVITY_CODE) {
            // This ensures a session state change is recorded so that the tokenUpdated() callback is triggered
			// to attempt a post if the write permissions have been granted
			Log.i(FriendSmashApplication.TAG, "Reauthorized with publish permissions.");
			Session.getActiveSession().onActivityResult(getActivity(), requestCode, resultCode, data);
        } else if (resultCode == Activity.RESULT_OK && data != null) {
			Bundle bundle = data.getExtras();
			application.setScore(bundle.getInt("score"));
			updateYouScoredTextView();
			updateButtonVisibility();
			completeGameOver(1500);
		} else if (resultCode == Activity.RESULT_CANCELED && data != null) {
			Bundle bundle = data.getExtras();
			((HomeActivity)getActivity()).showError(bundle.getString("error"));
		}
	}
	
	// Update with the user's score
	private void updateYouScoredTextView() {
		if (youScoredTextView != null) {
			if (application.getScore() >= 0) {
				youScoredTextView.setText("You Scored " + application.getScore() + "!");
			}
			else {
				youScoredTextView.setText(getResources().getString(R.string.no_score));
			}
		}
	}
	
	// Hide/show buttons based on whether the user has played ag ame yet or not
	private void updateButtonVisibility() {
		if (scoresButton != null && challengeButton != null && bragButton != null) {
			if (application.getScore() >= 0) {
				// The player has played at least one game, so show the buttons
				scoresButton.setVisibility(View.VISIBLE);
				challengeButton.setVisibility(View.VISIBLE);
				bragButton.setVisibility(View.VISIBLE);
			}
			else {
				// The player hasn't played a game yet, so hide the buttons (except scoresButton
				// that should always be shown)
				scoresButton.setVisibility(View.VISIBLE);
				challengeButton.setVisibility(View.INVISIBLE);
				bragButton.setVisibility(View.INVISIBLE);
			}
		}
	}
	
	// Complete the game over process
	private void completeGameOver(int millisecondsToShow) {
		// Show the gameOverContainer
		gameOverContainer.setVisibility(View.VISIBLE);
		
		// Set the gameOverMessageDisplaying boolean to true
		gameOverMessageDisplaying = true;
		
		// Cancel any running gameOverTasks
		timerHandler.removeCallbacks(gameOverTask);
		
		// Hide the gameOverContainer after a short period of time
		if (gameOverTask != null)
 		{
 			timerHandler.postDelayed(gameOverTask, millisecondsToShow);
 		}
	}
	
	
	/* Facebook Integration */
	
	// Called when the session state has changed
	protected void tokenUpdated() {
		if (pendingPost == true) {
			facebookPostAll();
        }
	}
	
	// Request write permissions from the user, returning true if the user already has write permissions
	// but false otherwise and showing them a permissions dialog to provide these permissions if their
	// session is open
	private boolean requestWritePermissions() {
		
		pendingPost = false;
		Session session = Session.getActiveSession();
		
		if (session == null || session.isOpened() == false) {
            return false;
        }

        List<String> permissions = session.getPermissions();
        if (permissions.containsAll(PERMISSIONS) == false) {
            pendingPost = true;
            Session.ReauthorizeRequest reauthRequest = new Session.ReauthorizeRequest(this, PERMISSIONS).
                    setRequestCode(REAUTH_ACTIVITY_CODE);
            session.reauthorizeForPublish(reauthRequest);
            return false;
        }
        
        // If you get this far, then the user has write permissions already, so you can continue appropriately
        return true;
	}
	
	// Post all information to Facebook for the user (score, achievement and custom OG action)
	private void facebookPostAll() {
		// Make sure we have write permissions and only continue if we do
		if (requestWritePermissions() == true) {
			
			// Post the score to Facebook
			((FriendSmashApplication)getActivity().getApplication()).facebookIntegrator.postScore(this);

			// Post Achievemnt to Facebook
			((FriendSmashApplication)getActivity().getApplication()).facebookIntegrator.postAchievement(this);

			// Post Custom OG action to Facebook
			((FriendSmashApplication)getActivity().getApplication()).facebookIntegrator.postOG(this);
		}
	}
}
