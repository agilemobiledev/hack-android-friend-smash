package com.facebook.android.friendsmash;

import java.util.Random;

import android.animation.Animator;
import android.animation.Animator.AnimatorListener;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;

/**
 *  ImageViews of the users that the playing user has to smash.  These can contain images of one
 *  of the user's friends (in the social version only) or images of celebrities
 */
public class UserImageView extends ImageView {

	private GameFragment gameFragment;
	private boolean shouldSmash;
	private boolean wrongImageSmashed = false;
	private boolean isVoid = false;
	private int extraPoints = 0;
	private AnimatorSet upMovementAnimatorSet;
	private AnimatorSet downMovementAnimatorSet;
	ValueAnimator rotationAnimation;
	
	// Default Constructor - not used
	public UserImageView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	// Constructor used by GameFragment to pass in an instance of itself
	public UserImageView(Context context, GameFragment gameFragment, boolean shouldSmash) {
		super(context);
		
		this.gameFragment = gameFragment;
		setShouldSmash(shouldSmash);
		
		upMovementAnimatorSet = new AnimatorSet();
		downMovementAnimatorSet = new AnimatorSet();
		
		setOnTouchListeners();
	}

	// Logic when a user touches this UserImageView
	private void setOnTouchListeners() {
		// Create an OnTouchListener for this UserImageView
        setOnTouchListener(new OnTouchListener() {
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				if (isShouldSmash() == true) {
					// Smashed the right image ...
					
					// Increment the score
					gameFragment.setScore(gameFragment.getScore() + 1 + getExtraPoints());
					
					// Hide the userImageView
					v.setVisibility(View.GONE);
					
					// Remove it from the userImageViews Vector in the gameFragment
					gameFragment.userImageViews.remove(v);
				} else {
					// Smashed the wrong image ...
					wrongImageSmashed();
				}
				return false;
			}});
	}
	
	// Logic when the user smashes this image, but it turns out to be the wrong image - i.e.
	// it's isShouldSmash boolean is false
	private void wrongImageSmashed() {
		// Set this flag for checking in the animation ended logic
		wrongImageSmashed = true;
		
		// Stop all movement (not rotation) animations for this UserImageView
		upMovementAnimatorSet.cancel();
		downMovementAnimatorSet.cancel();
		
		// Stop all animations for all other visible UserImageViews (and therefore hide them)
		gameFragment.hideAllUserImageViewsExcept(this);
		
		// Scale the image up
		ValueAnimator scaleAnimationX = ObjectAnimator.ofFloat(this, "scaleX", 25f);
		ValueAnimator scaleAnimationY = ObjectAnimator.ofFloat(this, "scaleY", 25f);
		scaleAnimationX.setDuration(1000);
		scaleAnimationY.setDuration(1000);
		scaleAnimationX.setInterpolator(new LinearInterpolator());
		scaleAnimationY.setInterpolator(new LinearInterpolator());
		
		// Start the animations together
		AnimatorSet animatorSet = new AnimatorSet();
		animatorSet.playTogether(scaleAnimationX, scaleAnimationY);
		animatorSet.start();
		
		// Ensure this UserImageView is in front
		gameFragment.gameFrame.bringChildToFront(this);
		
		// Create a callback after the animation has scaled to stop its rotation
		scaleAnimationY.addListener(new AnimatorListener() {
			@Override
			public void onAnimationCancel(Animator animation) {
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				// Cancel rotation and exit to home screen
				rotationAnimation.cancel();
				gameFragment.setLives(0);
			}

			@Override
			public void onAnimationRepeat(Animator animation) {
			}

			@Override
			public void onAnimationStart(Animator animation) {
			}
    	});
	}
	
	// Start firing this UserImageView across the GameFragment view
	protected void setupAndStartAnimations() {
		// Animations with Property Animator - Android 3.0 onwards only ...
		
		// Instantiate Random Generator
        Random randomGenerator = new Random();
        
        // Declare Animators
        ValueAnimator upAnimationX;
        ValueAnimator upAnimationY;
        final ValueAnimator downAnimationX;
        final ValueAnimator downAnimationY;
        
		// Calculate coordinates ...
        
        // X Range:
        int leftXExtreme = -gameFragment.getIconWidth()*3;
        int rightXExtreme = gameFragment.getScreenWidth()+(gameFragment.getIconWidth()*2);
        
        // Y Range:
        int bottomY = gameFragment.getScreenHeight()+gameFragment.getIconWidth();
        int topYLowerExtreme = (int) (gameFragment.getScreenHeight()*0.3);
        int topYUpperExtreme = 0;
        
        // Generate random centerX value
        int centerX = (gameFragment.getScreenWidth()-gameFragment.getIconWidth())/2 + gameFragment.getIconWidth() - randomGenerator.nextInt(gameFragment.getIconWidth()*2);
        
        // Generate random leftX and rightX values
        int leftX = randomGenerator.nextInt(centerX-leftXExtreme) + leftXExtreme;
        int rightX = rightXExtreme - randomGenerator.nextInt(rightXExtreme-centerX);
        
        // Generate random topY value
        int topY = randomGenerator.nextInt(topYLowerExtreme-topYUpperExtreme) + topYUpperExtreme;
        
        // Generate random time taken to rotate fully (in ms)
        int rotationTime = randomGenerator.nextInt(2500) + 500;
        
        if (randomGenerator.nextInt(2) == 0) {
        	upAnimationX = ObjectAnimator.ofFloat(this, "x", leftX, centerX);
        	upAnimationY = ObjectAnimator.ofFloat(this, "y", bottomY, topY);
        	downAnimationX = ObjectAnimator.ofFloat(this, "x", centerX, centerX+(centerX-leftX));
        	downAnimationY = ObjectAnimator.ofFloat(this, "y", topY, bottomY);
        } else {
        	upAnimationX = ObjectAnimator.ofFloat(this, "x", rightX, centerX);
        	upAnimationY = ObjectAnimator.ofFloat(this, "y", bottomY, topY);
        	downAnimationX = ObjectAnimator.ofFloat(this, "x", centerX, centerX-(rightX-centerX));
        	downAnimationY = ObjectAnimator.ofFloat(this, "y", topY, bottomY);
        }
        
        upAnimationX.setDuration(1500);
        upAnimationY.setDuration(1500);
        upAnimationX.setInterpolator(new LinearInterpolator());
        upAnimationY.setInterpolator(new DecelerateInterpolator());
        
        downAnimationX.setDuration(1500);
        downAnimationY.setDuration(1500);
        downAnimationX.setInterpolator(new LinearInterpolator());
        downAnimationY.setInterpolator(new AccelerateInterpolator());
        
        upMovementAnimatorSet.playTogether(upAnimationX, upAnimationY);
        
        // Rotation animations
        if (randomGenerator.nextInt(2) == 0) {
        	rotationAnimation = ObjectAnimator.ofFloat(this, "rotation", 0f, 360f);
        }
        else {
        	rotationAnimation = ObjectAnimator.ofFloat(this, "rotation", 0f, -360f);
        }
        rotationAnimation.setRepeatCount(ValueAnimator.INFINITE);
        rotationAnimation.setDuration(rotationTime);
        rotationAnimation.setInterpolator(new LinearInterpolator());
        
        // Create a callback after the up animation has ended to start the down animation
        upAnimationY.addListener(new AnimatorListener() {
			@Override
			public void onAnimationCancel(Animator animation) {
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				downMovementAnimatorSet.playTogether(downAnimationX, downAnimationY);
				downMovementAnimatorSet.start();
			}

			@Override
			public void onAnimationRepeat(Animator animation) {
			}

			@Override
			public void onAnimationStart(Animator animation) {
			}
    	});
        
        // Create a callback after the animation has ended
        downAnimationY.addListener(new AnimatorListener() {
			@Override
			public void onAnimationCancel(Animator animation) {
			}

			@Override
			public void onAnimationEnd(Animator animation) {
				if (wrongImageSmashed == false) {
					if (getVisibility() == View.VISIBLE && shouldSmash == true && isVoid() == false) {
						// Image is still visible, so user didn't smash it and they should have done (and it isn't void), so decrement the lives by one
						gameFragment.setLives(gameFragment.getLives() - 1);
					}
					
					// Only hide this if the wrong image has not been smashed (otherwise, other logic will be run and image still needs to be shown)
					hideAndRemoveMeFromGameFrame();
				}
			}

			@Override
			public void onAnimationRepeat(Animator animation) {
			}

			@Override
			public void onAnimationStart(Animator animation) {
			}
    	});
    	
        // Play the animations
        upMovementAnimatorSet.start();
        rotationAnimation.start();
	}
	
	// If this UserImageView is currently visible, hide it and remove it from the GameFragment view
	// and the vector storing all UserImageViews
	private void hideAndRemoveMeFromGameFrame() {
		if (getVisibility() == View.VISIBLE) {
			// Ensure it is hidden
			setVisibility(View.GONE);
		}
		
		// Remove the userImageView from the gameFrame
		gameFragment.gameFrame.removeView(this);
		
		// Remove it from the userImageViews Vector in the gameFragment
		gameFragment.userImageViews.remove(this);
	}

	
	/* Standard Getters & Setters */
	
	public boolean isShouldSmash() {
		return shouldSmash;
	}

	public void setShouldSmash(boolean shouldSmash) {
		this.shouldSmash = shouldSmash;
	}

	public boolean isVoid() {
		return isVoid;
	}

	public void setVoid(boolean isVoid) {
		this.isVoid = isVoid;
	}

	public int getExtraPoints() {
		return extraPoints;
	}

	public void setExtraPoints(int extraPoints) {
		this.extraPoints = extraPoints;
	}

}
