package com.facebook.android.friendsmash;

import java.util.Iterator;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.facebook.widget.ProfilePictureView;

/**
 *  Fragment shown once a user opens the scoreboard
 */
public class ScoreboardFragment extends Fragment {
	
	// Tag used when logging messages
    public static final String TAG = ScoreboardFragment.class.getSimpleName();
	
    // Store the Application (as you can't always get to it when you can't access the Activity - e.g. during rotations)
 	FriendSmashApplication application;
    
	// LinearLayout as the container for the scoreboard entries
    LinearLayout scoreboardContainer;
	
	// FrameLayout of the progress container to show the spinner
	FrameLayout progressContainer;
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		application = (FriendSmashApplication) getActivity().getApplication();
		
		setRetainInstance(true);
	}
	
	@TargetApi(13)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.fragment_scoreboard, parent, false);
		
		scoreboardContainer = (LinearLayout)v.findViewById(R.id.scoreboardContainer);
		progressContainer = (FrameLayout)v.findViewById(R.id.progressContainer);

		// Set the progressContainer as invisible by default
		progressContainer.setVisibility(View.INVISIBLE);
		
		// Note: Scoreboard is populated during onResume below
		
		return v;
	}
	
	// Close the game and show the specified error in a toast to the user
	protected void closeAndShowError(String error) {
		Bundle bundle = new Bundle();
		bundle.putString("error", error);
		
		Intent i = new Intent();
		i.putExtras(bundle);
		
		getActivity().setResult(Activity.RESULT_CANCELED, i);
		getActivity().finish();
	}
	
	@Override
	public void onResume() {
		super.onResume();

		// Populate scoreboard - fetch information if necessary ...
		if (application.scoreboardEntriesVector == null) {
			// scoreboardEntriesVector is null, so fetch the information from Facebook (scoreboard will be updated in
			// the scoreboardEntriesFetched callback) and show the progress spinner while doing so
			progressContainer.setVisibility(View.VISIBLE);
			((FriendSmashApplication)getActivity().getApplication()).facebookIntegrator.fetchScoreboardEntries(this);
		} else {
			// Information has already been fetched, so populate the scoreboard
			populateScoreboard();
		}
	}

	protected void populateScoreboard() {
		// Ensure all components are firstly removed from scoreboardContainer
		scoreboardContainer.removeAllViews();
		
		// Ensure the progress spinner is hidden
		progressContainer.setVisibility(View.INVISIBLE);
		
		// Ensure scoreboardEntriesVector is not null and not empty first
		if (application.scoreboardEntriesVector == null || application.scoreboardEntriesVector.size() <= 0) {
			closeAndShowError("No scores to display");
		} else {
			// Iterate through scoreboardEntriesVector, creating new UI elements for each entry
			int index = 0;
			Iterator<ScoreboardEntry> scoreboardEntriesIterator = application.scoreboardEntriesVector.iterator();
			while (scoreboardEntriesIterator.hasNext()) {
				// Get the current scoreboard entry
				ScoreboardEntry currentScoreboardEntry = scoreboardEntriesIterator.next();
				
				// FrameLayout Container for the currentScoreboardEntry ...
				
				// Create and add a new FrameLayout to display the details of this entry
				FrameLayout frameLayout = new FrameLayout(getActivity());
				scoreboardContainer.addView(frameLayout);
				
				// Set the attributes for this frameLayout
				int topPadding = Utility.convertPixelToDensityPixel(4, getActivity());
				frameLayout.setPadding(0, topPadding, 0, 0);
				
				// ImageView background image ...
				{
					// Create and add an ImageView for the background image to this entry
					ImageView backgroundImageView = new ImageView(getActivity());
					frameLayout.addView(backgroundImageView);
					
					// Set the image of the backgroundImageView
					String uri = "drawable/scores_stub_even";
					if (index % 2 != 0) {
						// Odd entry
						uri = "drawable/scores_stub_odd";
					}
				    int imageResource = getResources().getIdentifier(uri, null, getActivity().getPackageName());
				    Drawable image = getResources().getDrawable(imageResource);
				    backgroundImageView.setImageDrawable(image);
					
				    // Other attributes of backgroundImageView to modify
				    FrameLayout.LayoutParams backgroundImageViewLayoutParams = new FrameLayout.LayoutParams(
				    		FrameLayout.LayoutParams.WRAP_CONTENT,
				    		FrameLayout.LayoutParams.WRAP_CONTENT);
				    int backgroundImageViewMarginTop = Utility.convertPixelToDensityPixel(4, getActivity());
				    backgroundImageViewLayoutParams.setMargins(0, backgroundImageViewMarginTop, 0, 0);
				    backgroundImageViewLayoutParams.gravity = Gravity.LEFT;
					if (index % 2 != 0) {
						// Odd entry
						backgroundImageViewLayoutParams.gravity = Gravity.RIGHT;
					}
					backgroundImageView.setLayoutParams(backgroundImageViewLayoutParams);
				}
				
			    // ProfilePictureView of the current user ...
				{
				    // Create and add a ProfilePictureView for the current user entry's profile picture
				    ProfilePictureView profilePictureView = new ProfilePictureView(getActivity());
				    frameLayout.addView(profilePictureView);
				    
				    // Set the attributes of the profilePictureView
				    int profilePictureViewWidth = Utility.convertPixelToDensityPixel(69, getActivity());
				    FrameLayout.LayoutParams profilePictureViewLayoutParams = new FrameLayout.LayoutParams(profilePictureViewWidth, profilePictureViewWidth);
				    int profilePictureViewMarginLeft = 0;
				    int profilePictureViewMarginTop = Utility.convertPixelToDensityPixel(9, getActivity());;
				    int profilePictureViewMarginRight = 0;
				    int profilePictureViewMarginBottom = 0;
				    if (index % 2 == 0) {
				    	profilePictureViewMarginLeft = Utility.convertPixelToDensityPixel(5, getActivity());
					} else {
						profilePictureViewMarginRight = Utility.convertPixelToDensityPixel(5, getActivity());
					}
				    profilePictureViewLayoutParams.setMargins(profilePictureViewMarginLeft, profilePictureViewMarginTop,
				    		profilePictureViewMarginRight, profilePictureViewMarginBottom);
				    profilePictureViewLayoutParams.gravity = Gravity.LEFT;
					if (index % 2 != 0) {
						// Odd entry
						profilePictureViewLayoutParams.gravity = Gravity.RIGHT;
					}
					profilePictureView.setLayoutParams(profilePictureViewLayoutParams);
				    
				    // Finally set the id of the user to show their profile pic
				    profilePictureView.setProfileId(currentScoreboardEntry.getId());
				}
				
				// LinearLayout to hold the text in this entry
				
				// Create and add a LinearLayout to hold the TextViews
				LinearLayout textViewsLinearLayout = new LinearLayout(getActivity());
				frameLayout.addView(textViewsLinearLayout);
				
				// Set the attributes for this textViewsLinearLayout
				FrameLayout.LayoutParams textViewsLinearLayoutLayoutParams = new FrameLayout.LayoutParams(
						FrameLayout.LayoutParams.WRAP_CONTENT,
						FrameLayout.LayoutParams.WRAP_CONTENT);
				int textViewsLinearLayoutMarginLeft = 0;
			    int textViewsLinearLayoutMarginTop = Utility.convertPixelToDensityPixel(20, getActivity());;
			    int textViewsLinearLayoutMarginRight = 0;
			    int textViewsLinearLayoutMarginBottom = 0;
			    if (index % 2 == 0) {
			    	textViewsLinearLayoutMarginLeft = Utility.convertPixelToDensityPixel(86, getActivity());
				} else {
					textViewsLinearLayoutMarginRight = Utility.convertPixelToDensityPixel(86, getActivity());
				}
			    textViewsLinearLayoutLayoutParams.setMargins(textViewsLinearLayoutMarginLeft, textViewsLinearLayoutMarginTop,
			    		textViewsLinearLayoutMarginRight, textViewsLinearLayoutMarginBottom);
			    textViewsLinearLayoutLayoutParams.gravity = Gravity.LEFT;
				if (index % 2 != 0) {
					// Odd entry
					textViewsLinearLayoutLayoutParams.gravity = Gravity.RIGHT;
				}
				textViewsLinearLayout.setLayoutParams(textViewsLinearLayoutLayoutParams);
				textViewsLinearLayout.setOrientation(LinearLayout.VERTICAL);
				
				// TextView with the position and name of the current user
				{
					// Set the text that should go in this TextView first
					int position = index+1;
					String userFullName = currentScoreboardEntry.getName();
					String currentUserFirstName = userFullName.substring(0, userFullName.indexOf(' '));
					String currentScoreboardEntryTitle = position + ". " + currentUserFirstName;
					
					// Create and add a TextView for the current user position and first name
				    TextView titleTextView = new TextView(getActivity());
				    textViewsLinearLayout.addView(titleTextView);
				    
				    // Set the text and other attributes for this TextView
				    titleTextView.setText(currentScoreboardEntryTitle);
				    titleTextView.setTextAppearance(getActivity(), R.style.ScoreboardPlayerNameFont);
				}
				
				// TextView with the score of the current user
				{
					// Create and add a TextView for the current user score
				    TextView scoreTextView = new TextView(getActivity());
				    textViewsLinearLayout.addView(scoreTextView);
				    
				    // Set the text and other attributes for this TextView
				    scoreTextView.setText("Score: " + currentScoreboardEntry.getScore());
				    scoreTextView.setTextAppearance(getActivity(), R.style.ScoreboardPlayerScoreFont);
				}
			    
			    // Increment the index before looping back
				index++;
			}
		}
	}
}
