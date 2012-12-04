package com.facebook.android.friendsmash;

import java.util.Iterator;
import java.util.Random;
import java.util.Vector;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 *  Fragment shown once a user starts playing a game
 */
public class GameFragment extends Fragment {
	
	public static final String [] celebs = {
		"Einstein",
        "Xzibit",
        "Goldsmith",
        "Sinatra",
        "George",
        "Jacko",
        "Rick",
        "Keanu",
        "Arnie",
        "Jean-Luc",
	};
	
	// Tag used when logging messages
    public static final String TAG = GameFragment.class.getSimpleName();
	
	// FrameLayout as the container for the game
	FrameLayout gameFrame;
	
	// FrameLayout of the progress container to show the spinner
	FrameLayout progressContainer;
	
	// TextView for the Smash Player title
	TextView smashPlayerNameTextView;
	
	// TextView for the score
	TextView scoreTextView;
	
	// LinearyLayout containing the lives images
	LinearLayout livesContainer;
	
	// Icon width for the friend images to smash
	protected int iconWidth;

	// Screen Dimensions
	private int screenWidth;
	private int screenHeight;
	
	
	// Handler for putting messages on Main UI thread from background threads periodically
	Handler timerHandler;
	
	// Runnable task used to produce images to fly across the screen
	Runnable fireImageTask = null;
	
	// Boolean indicating whether images have started firing
	private boolean imagesStartedFiring = false;
	
	
	// Index of the friend to smash (in the social game)
	private int friendToSmashIndex = -1;
	
	// Index of the celeb to smash (in the non-social game)
	private int celebToSmashIndex = -1;
	
	// ID of the friend to smash (if passed in as an attribute)
	protected String friendToSmashIDProvided = null;
	
	// Name of the friend to smash
	protected String friendToSmashFirstName = null;
	
	// Bitmap of the friend to smash
	protected Bitmap friendToSmashBitmap;
	
	
	// Score for the user
	private int score = 0;
	
	// Lives the user has remaining
	private int lives = 3;
	
	// Boolean set to true if first image has been fired
	private boolean firstImageFired = false;
	
	// Boolean indicating that the first image to be fired is pending (i.e. a Request is
	// in the process of executing in a background thread to fetch the images / information)
	private boolean firstImagePendingFiring = false;
	
	
	// Vector of UserImageView objects created and visible
	Vector<UserImageView> userImageViews = new Vector<UserImageView>();
	
	
	@SuppressWarnings("unused")
	@Override
	public void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		
		setRetainInstance(true);
		
		// Instantiate the timerHandler
		timerHandler = new Handler();
		
		// Get the friend to smash bitmap and name
		if (FriendSmashApplication.isSocial == true) {
			// User is logged into FB, so choose a random FB friend to smash
			friendToSmashIndex = getRandomFriendIndex();
		} else {
			// User is not logged into FB, so choose a random celebrity to smash
			celebToSmashIndex = getRandomCelebIndex();
		}
	}
	
	@SuppressWarnings({ "deprecation" })
	@TargetApi(13)
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup parent,
			Bundle savedInstanceState) {
		
		View v = inflater.inflate(R.layout.fragment_game, parent, false);
		
		gameFrame = (FrameLayout)v.findViewById(R.id.gameFrame);
		progressContainer = (FrameLayout)v.findViewById(R.id.progressContainer);
		smashPlayerNameTextView = (TextView)v.findViewById(R.id.smashPlayerNameTextView);
		scoreTextView = (TextView)v.findViewById(R.id.scoreTextView);
		livesContainer = (LinearLayout)v.findViewById(R.id.livesContainer);
		
		// Set the progressContainer as invisible by default
		progressContainer.setVisibility(View.INVISIBLE);
		
		// Set the icon width (for the images to be smashed)
		DisplayMetrics metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
		setIconWidth((int) (96 * metrics.density));
		
		// Set the screen dimensions
		WindowManager wm = (WindowManager) getActivity().getSystemService(Context.WINDOW_SERVICE);
		Display display = wm.getDefaultDisplay();
		if (Build.VERSION.SDK_INT >= 13) {
			Point size = new Point();
			display.getSize(size);
			setScreenWidth(size.x);
			setScreenHeight(size.y);
		}
		else {
			setScreenWidth(display.getWidth());
			setScreenHeight(display.getHeight());
		}
		
		// Always keep the Action Bar hidden
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActivity().getActionBar().hide();
		}
		
		// Instantiate the fireImageTask for future fired images
		fireImageTask = new Runnable()
		{
			public void run()
			{
				spawnImage(false);
			}
		};
		
		// Refresh the score board
		setScore(getScore());
		
		// Refresh the lives
		setLives(getLives());
		
		// Note: Images will start firing in the onResume method below
		
		return v;
	}
	
	// Sets the name of the player to smash in the top left TextView
	@SuppressWarnings("unused")
	protected void setSmashPlayerNameTextView() {
		// Set the Smash Player Name title
        if (FriendSmashApplication.isSocial == true) {
			// User is logged into FB ...
        	if (friendToSmashFirstName == null) {
        		// A name hasn't been set yet (i.e. it hasn't been fetched through a passed in id, so
        		// a random friend needs to be used instead, so fetch this name
        		String friendToSmashName = ((FriendSmashApplication) getActivity().getApplication()).getFriends().get(friendToSmashIndex).getName();
            	friendToSmashFirstName = friendToSmashName.split(" ")[0];
        	}
        	smashPlayerNameTextView.setText("Smash " + friendToSmashFirstName + " !");
		} else {
			// User is not logged into FB ...
			smashPlayerNameTextView.setText("Smash " + celebs[celebToSmashIndex] + " !");
		}
	}
	
	// Select a random friend to smash
	private int getRandomFriendIndex() {
		Random randomGenerator = new Random();
		int friendIndex = randomGenerator.nextInt(((FriendSmashApplication) getActivity().getApplication()).getFriends().size());
		return friendIndex;
	}
	
	// Select a random celebrity to smash (in the non-social game) or avoid smashing (in the social game)
	private int getRandomCelebIndex() {
		Random randomGenerator = new Random();
		int celebIndex = randomGenerator.nextInt(celebs.length);
		return celebIndex;
	}
	
	// Set the image on the UserImageView to the specified bitmap of the user's friend and fire it
	protected void setFriendImageAndFire(UserImageView imageView, Bitmap friendBitmap, boolean extraImage) {
		imageView.setImageBitmap(friendBitmap);
		
		if (extraImage == true) {
			// If this is an extra image, give it an extra point when smashed
			imageView.setExtraPoints(1);
		}
		
		fireImage(imageView, extraImage);
	}
	
	// Set the image on the UserImageView to the celebrity and fire it
	private void setCelebImageAndFire(UserImageView imageView, int celebIndex, boolean extraImage) {
		String uri = "drawable/nonfriend_" + (celebIndex+1);

	    int imageResource = getResources().getIdentifier(uri, null, getActivity().getPackageName());

	    Drawable image = getResources().getDrawable(imageResource);
	    imageView.setImageDrawable(image);
	    
	    fireImage(imageView, extraImage);
	}
	
	// Fire the UserImageView and setup the timer to start another image shortly (as long as the image that
	// is fired isn't an extra image)
	private void fireImage(UserImageView imageView, boolean extraImage) {
		// Fire image
	    imageView.setupAndStartAnimations();
	    
	    if (extraImage == false) {
	    	// If this isn't an extra image spawned, fire another image shortly
	    	fireAnotherImage();
	    }
	    
		// By this point, all network calls would have executed and the first image has fired with the next lined up
		// , so set firstImagePendingFiring to false
		firstImagePendingFiring = false;
	}
	
	// Fire another image shortly
	private void fireAnotherImage() {
		// Fire another image shortly ...
 		if (fireImageTask != null)
 		{
 			timerHandler.postDelayed(fireImageTask, 700);
 		}
	}

	// Called when the first image should be fired (only called during onResume)
	// If the game has been deep linked into (i.e. a user has clicked on a feed post or request in
	// Facebook), then fetch the specific user that should be smashed
	@SuppressWarnings("unused")
	private void fireFirstImage() {
		if (FriendSmashApplication.isSocial == true) {
			// Get any bundle parameters there are
			Bundle bundle = getActivity().getIntent().getExtras();
			
			String requestID = null;
			String userID = null;
			if (bundle != null) {
				requestID = bundle.getString("request_id");
				userID = bundle.getString("user_id");
			}
			
			if (requestID != null && friendToSmashIDProvided == null) {
				// Deep linked from request
				// Make a request to get a specific user to smash if they haven't been fetched already
				
				// Show the spinner for this part
				progressContainer.setVisibility(View.VISIBLE);
				
				// Get and set the id of the friend to smash and start firing the image
				((FriendSmashApplication)getActivity().getApplication()).facebookIntegrator.fireFirstImageWithRequestID(this, requestID);
			} else if (userID != null && friendToSmashIDProvided == null) {
				// Deep linked from feed post
				// Make a request to get a specific user to smash if they haven't been fetched already
				
				// Show the spinner for this part
				progressContainer.setVisibility(View.VISIBLE);
				
				// Get and set the id of the friend to smash and start firing the image
				((FriendSmashApplication)getActivity().getApplication()).facebookIntegrator.fireFirstImageWithUserID(this, userID);
			} else {
				// requestID is null, userID is null or friendToSmashIDProvided is already set,
				// so use the randomly generated friend of the user or the already set friendToSmashIDProvided
				// So set the smashPlayerNameTextView text and hide the progress spinner as there is nothing to fetch
				progressContainer.setVisibility(View.INVISIBLE);			
				setSmashPlayerNameTextView();
				
				// Now you're ready to fire the first image
				spawnImage(false);
			}
		} else {
			// Non-social, so set the smashPlayerNameTextView text and hide the progress spinner as there is nothing to fetch
			progressContainer.setVisibility(View.INVISIBLE);			
			setSmashPlayerNameTextView();
			
			// Now you're ready to fire the first image
			spawnImage(false);
		}
	}
	
	// Spawn a new UserImageView, set its bitmap (fetch it from Facebook if it hasn't already been fetched)
	// and fire it once the image has been set (and fetched if appropriate)
	@SuppressWarnings("unused")
	protected void spawnImage(final boolean extraImage) {
		// Instantiate Random Generator
        Random randomGenerator = new Random();
        
        // 1 in every 5 images should be a celebrity the user should not smash - calculate that here
        // Unless it is the first image fired, in which case it should always be the smashable image
        boolean shouldSmash = true;
        if (firstImageFired == true) {
        	if (randomGenerator.nextInt(5) == 4 && firstImageFired == true) {
            	shouldSmash = false;
            } 
        } else if (firstImageFired == false) {
        	shouldSmash = true;
        	firstImageFired = true;
        }
		
		// Create a new ImageView with a user to smash
        final UserImageView userImageView = (new UserImageView(getActivity(), this, shouldSmash));
        userImageView.setLayoutParams(new LinearLayout.LayoutParams(iconWidth, iconWidth));
        gameFrame.addView(userImageView);
        userImageViews.add(userImageView);
        
        // Set the bitmap of the userImageView ...
        if (userImageView.isShouldSmash() == true) {
        	// The user should smash this image, so set the correct image
	        if (FriendSmashApplication.isSocial == true) {
				// User is logged into FB ...
				if (friendToSmashBitmap != null) {
					// Bitmap for the friend to smash has already been retrieved, so use this
					setFriendImageAndFire(userImageView, friendToSmashBitmap, extraImage);
				} else {
					// Otherwise, the Bitmap for the friend to smash hasn't been retrieved, so retrieve it and set it
					
					// Show the spinner while retrieving
					progressContainer.setVisibility(View.VISIBLE);
					
					// If a friend has been passed in, use that attribute, otherwise use the random friend that has been selected
					final String friendToSmashID = friendToSmashIDProvided != null ? friendToSmashIDProvided :
						((FriendSmashApplication) getActivity().getApplication()).getFriends().get(friendToSmashIndex).getId();
					
					// Fetch the bitmap and fire the image
					((FriendSmashApplication)getActivity().getApplication()).facebookIntegrator.
						fetchFriendBitmapAndFireImages(this, userImageView, friendToSmashID, extraImage);
				}
			} else {
				// User is not logged into FB ...
				setCelebImageAndFire(userImageView, celebToSmashIndex, extraImage);
			}
        } else {
        	// The user should not smash this image, so set it to a random celebrity (but not the one being shown if it's the non-social game)
        	int randomCelebToSmashIndex;
        	do {
        		randomCelebToSmashIndex = randomGenerator.nextInt(celebs.length);
        	} while (randomCelebToSmashIndex == celebToSmashIndex);
        	setCelebImageAndFire(userImageView, randomCelebToSmashIndex, extraImage);
        }
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
	
	// Hide all the UserImageViews currently on display except the one specified
	// Called when the user has smashed the wrong image so that this is displayed large
	public void hideAllUserImageViewsExcept(UserImageView userImageView) {
		// Stop new animations
		timerHandler.removeCallbacks(fireImageTask);
		
		// Stop animations on all existing visible UserImageViews (which will hide them automatically)
		Iterator<UserImageView> userImageViewsIterator = userImageViews.iterator();
		while (userImageViewsIterator.hasNext()) {
			UserImageView currentUserImageView = (UserImageView) userImageViewsIterator.next();
			if (currentUserImageView.equals(userImageView) == false) {
				currentUserImageView.setVisibility(View.GONE);
			}
		}
	}
	
	// Mark all the existing visible UserImageViews as void (called when the game is paused)
	public void markAllUserImageViewsAsVoid() {
		Iterator<UserImageView> userImageViewsIterator = userImageViews.iterator();
		while (userImageViewsIterator.hasNext()) {
			UserImageView currentUserImageView = (UserImageView) userImageViewsIterator.next();
			currentUserImageView.setVoid(true);
		}
	}
	
	@Override
	public void onPause() {
		super.onPause();

		// Stop the firing images
		stopTheFiringImages();
	}
	
	@SuppressWarnings("unused")
	@Override
	public void onResume() {
		super.onResume();

		// Stop any firing images (even though this is called in onPause, there might be new firing images
		// if they were pending while onPause was called
		stopTheFiringImages();
		
		if (imagesStartedFiring == false) {
			// Fire first image
			if (FriendSmashApplication.isSocial == true) {
				// Only fire for the social game if there isn't a first image pending firing
				if (firstImagePendingFiring == false) {
					// ... and also set the firstImagePendingFiring to true - will be set back
					// to false later once the images have actually started firing (i.e. all network
					// calls have executed) - note, this is only relevant for the social version
					firstImagePendingFiring = true;
					imagesStartedFiring = true;
					fireFirstImage();
				}
			} else {
				imagesStartedFiring = true;
				fireFirstImage();
			}
		}
	}
	
	// Stop the firing of all images (and mark the existing ones as void) - called when the game is paused
	private void stopTheFiringImages() {
		// Mark all existing in flight UserImageViews as void (so they don't affect the user's lives once landed)
		markAllUserImageViewsAsVoid();
		
		// Stop new animations and indicate that images have not started firing
		timerHandler.removeCallbacks(fireImageTask);
		imagesStartedFiring = false;
	}
	
	// Get the current score
	public int getScore() {
		return score;
	}

	// Set the score and if the score is divisible by 10, spawn more images ...
	// ... the higher the score, the more images that will be spawned
	public void setScore(int score) {
		this.score = score;
		
		// Update the scoreTextView
		scoreTextView.setText("Score: " + score);
		
		// If they start scoring well, spawn more images
		if (score > 0 && score % 10 == 0) {
			// Every multiple of 10, spawn extra images ...
			for (int i=0; i<score/20; i++) {
				spawnImage(true);
			}
		}
	}

	// Get the user's number of lives they have remaining
	public int getLives() {
		return lives;
	}

	// Set the number of lives that the user has, update the display appropriately and
	// end the game if they have run out of lives
	public void setLives(int lives) {
		this.lives = lives;
		
		if (getActivity() != null) {
			// Update the livesContainer
			livesContainer.removeAllViews();
			for (int i=0; i<lives; i++) {
				ImageView heartImageView = new ImageView(getActivity());
				heartImageView.setImageResource(R.drawable.heart_red);
			    livesContainer.addView(heartImageView);
			}
			
			if (lives <= 0) {
				// User has no lives left, so end the game, passing back the score
				Bundle bundle = new Bundle();
				bundle.putInt("score", getScore());
				
				Intent i = new Intent();
				i.putExtras(bundle);
			
				getActivity().setResult(Activity.RESULT_OK, i);
				getActivity().finish();
			}
		}
	}
	
	
	/* Standard Getters & Setters */
	
	public int getIconWidth() {
		return iconWidth;
	}

	public void setIconWidth(int iconWidth) {
		this.iconWidth = iconWidth;
	}
	
	public int getScreenWidth() {
		return screenWidth;
	}

	public void setScreenWidth(int screenWidth) {
		this.screenWidth = screenWidth;
	}

	public int getScreenHeight() {
		return screenHeight;
	}

	public void setScreenHeight(int screenHeight) {
		this.screenHeight = screenHeight;
	}
}
