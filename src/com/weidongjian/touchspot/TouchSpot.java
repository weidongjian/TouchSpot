package com.weidongjian.touchspot;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.RelativeLayout;

public class TouchSpot extends Activity {
	private TouchSpotView spotView;
	private RelativeLayout relativeLayout;
	private Button startButton;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_touch_spot);
		relativeLayout = (RelativeLayout) findViewById(R.id.rl_touchSpot);
		
		spotView = new TouchSpotView(this, getPreferences(Context.MODE_PRIVATE), relativeLayout);
		relativeLayout.addView(spotView, 0);
		
//		startButton = (Button) findViewById(R.id.start_game);
//		startButton.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View arg0) {
//				spotView.resume(getApplicationContext());
//			}
//		});
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		spotView.pause();
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		spotView.resume(this);
	}
}