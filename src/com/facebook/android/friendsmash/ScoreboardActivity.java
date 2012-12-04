package com.facebook.android.friendsmash;

import android.support.v4.app.Fragment;

/**
 *  Activity used once a user opens the Facebook scoreboard - all logic
 *  is within ScoreboardFragment
 */
public class ScoreboardActivity extends SingleFragmentActivity {

	@Override
	protected Fragment createFragment() {
		return new ScoreboardFragment();
	}
	
}
