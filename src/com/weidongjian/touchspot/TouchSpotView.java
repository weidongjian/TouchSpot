package com.weidongjian.touchspot;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.Animator.AnimatorListener;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class TouchSpotView extends View {
	private static final String HIGH_SCORE = "HIGH_SCORE";
	private static final int INITIAL_ANIMATION_DURATION = 6000;
	private static final int LIVES = 3;
	private static final int INITIAL_SPOTS = 5;
	private static final int SPOTS_DELAY = 500;
	private static final int MAX_STREAM = 4;
	private static final int SOUND_QUALITY = 100;
	private static final int HIT_SOUND_ID = 1;
	private static final int MISS_SOUND_ID = 2;
	private static final int DISAPPEAR_SOUND_ID = 3;
	private static final int SOUND_PRIORITY = 1;
	private static final int SPOT_DIAMETER = 100;
	private static final int MAX_LIVES = 7;
	private static final Random random = new Random();
	private static final float SCALE_X = 0.25f;
	private static final float SCALE_Y = 0.25f;
	private SharedPreferences share;
	private RelativeLayout relativeLayout;
	private int highScore;
	private int viewHeight;
	private int viewWidth;
	private int animationTime;
	private int spotsTouched;
	private int score;
	private int level;
	private int volume; //sound effect volume
	
	private boolean gamePause;
	private boolean gameOver;
	private boolean dialogDisplay;
	
	private Resources resoureces;
	private LayoutInflater inflater;
	private TextView highScoreTextView;
	private TextView scoreTextView;
	private TextView leveTextView;
	private LinearLayout lifeLayout;
	private Handler spotHandler;
	private SoundPool soundPool;
	private final Queue<ImageView> spots = new ConcurrentLinkedQueue<ImageView>();
	private final Queue<Animator> animators = new ConcurrentLinkedQueue<Animator>();
	private Map<Integer, Integer> soundMap;
	
	public TouchSpotView(Context context, SharedPreferences sharedPreference, RelativeLayout parentLayout) {
		super(context);
		
		share = sharedPreference;
		highScore = share.getInt(HIGH_SCORE, 0);
		resoureces = context.getResources();
		relativeLayout = parentLayout;
		inflater = (LayoutInflater) context.getSystemService(
		         Context.LAYOUT_INFLATER_SERVICE);
		
		highScoreTextView = (TextView) relativeLayout.findViewById(R.id.tv_high_score);
		scoreTextView = (TextView) relativeLayout.findViewById(R.id.tv_score);
		leveTextView = (TextView) relativeLayout.findViewById(R.id.tv_level);
		lifeLayout = (LinearLayout) relativeLayout.findViewById(R.id.ll_life);
		
		spotHandler = new Handler();
	}
	
	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		viewHeight = h;
		viewWidth = w;
	}
	
	public void pause() {
		gamePause = true;
		soundPool.release();
		soundPool = null;
		cancelAnimations();
	}
	
	private void cancelAnimations() {
		for (Animator animator : animators) {
			animator.cancel();
		}
		
		for (ImageView imageView : spots) {
			relativeLayout.removeView(imageView);
		}
		
		spotHandler.removeCallbacks(addSpotRunnable);
		animators.clear();
		spots.clear();
	}
	
	public void resume(Context context) {
		gamePause = false;
		initializeSoundEffects(context);
		
		if (!dialogDisplay) {
			resetGame();
		}
	}
	
	public void resetGame() {
		spots.clear();
		animators.clear();
		lifeLayout.removeAllViews();
		
		animationTime = INITIAL_ANIMATION_DURATION;
		score = 0;
		level = 1;
		gameOver = false;
		spotsTouched = 0;
		displayScore();
		
		for (int i = 0; i < LIVES; i++) {
			lifeLayout.addView(inflater.inflate(R.layout.life, null));
		}
		
		for (int i = 1; i < INITIAL_SPOTS; i++) {
			spotHandler.postDelayed(addSpotRunnable , i * SPOTS_DELAY);
		}
	}
	
	private void initializeSoundEffects(Context context) {
		soundPool = new SoundPool(MAX_STREAM, AudioManager.STREAM_MUSIC, SOUND_QUALITY);
		
		AudioManager manager = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
		volume = manager.getStreamVolume(AudioManager.STREAM_MUSIC);
		
		soundMap = new HashMap<Integer, Integer>();
		soundMap.put(HIT_SOUND_ID, soundPool.load(context, R.raw.hit, SOUND_PRIORITY));
		soundMap.put(MISS_SOUND_ID, soundPool.load(context, R.raw.miss, SOUND_PRIORITY));
		soundMap.put(DISAPPEAR_SOUND_ID, soundPool.load(context, R.raw.disappear, SOUND_PRIORITY));
	}

	private void displayScore() {
		highScoreTextView.setText(resoureces.getString(R.string.high_score) + " " + highScore);
		scoreTextView.setText(resoureces.getString(R.string.score) + " " + score);
		leveTextView.setText(resoureces.getString(R.string.level) + " " + level);
	}
	
	private Runnable addSpotRunnable = new Runnable() {
		@Override
		public void run() {
			addNewSpot();
		}
	};
	
	private void addNewSpot() {
		int x = random.nextInt(viewWidth - SPOT_DIAMETER);
		int y = random.nextInt(viewHeight - SPOT_DIAMETER);
		int x2 = random.nextInt(viewWidth - SPOT_DIAMETER);
		int y2 = random.nextInt(viewHeight - SPOT_DIAMETER);
		
		final ImageView spot = (ImageView) inflater.inflate(R.layout.untouch, null);
		spots.add(spot);
		spot.setLayoutParams(new RelativeLayout.LayoutParams(SPOT_DIAMETER, SPOT_DIAMETER));
		spot.setImageResource(random.nextInt(2) == 0 ? R.drawable.green_spot : R.drawable.red_spot);
		spot.setX(x);
		spot.setY(y);
		spot.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				touchSpot(spot);
			}
		});
		relativeLayout.addView(spot);
		
		spot.animate().x(x2).y(y2).scaleX(SCALE_X).scaleY(SCALE_Y).setDuration(animationTime)
		.setListener(new AnimatorListenerAdapter() 
		 {
			@Override
			public void onAnimationStart(Animator animation) {
				animators.add(animation);
			}
			
			@Override
			public void onAnimationEnd(Animator animation) {
				animators.remove(animation);
				if (!gamePause && spots.contains(spot)) {
					missSpot(spot);
				}
			}
		});
	}
	
	@Override
	public boolean onTouchEvent(MotionEvent event) {
		if (soundPool != null) {
			soundPool.play(MISS_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1f);
		}
		score -= 15 * level;
		score = Math.max(score, 0);
		displayScore();
		return true;
	}
	
	private void touchSpot(ImageView spot) {
		relativeLayout.removeView(spot);
		spots.remove(spot);
		
		if (soundPool != null) {
			soundPool.play(HIT_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1);
		}
		spotsTouched++;
		score += 10 * level;
		displayScore();
		
		if (spotsTouched % 10 == 0) {
			level++;
			animationTime *= 0.95;
			
			if (lifeLayout.getChildCount() < MAX_LIVES) {
				ImageView life = (ImageView) inflater.inflate(R.layout.life, null);
				lifeLayout.addView(life);
			}
		}
		displayScore();
		if (!gameOver) {
			addNewSpot();
		}
	}
	
	public void missSpot(ImageView spot) {
		spots.remove(spot);
		relativeLayout.removeView(spot);
		
		if (gameOver) {
			return;
		}
		
		if (soundPool != null) {
			soundPool.play(MISS_SOUND_ID, volume, volume, SOUND_PRIORITY, 0, 1);
		}
		
		if (lifeLayout.getChildCount() == 0) {
			gameOver = true;
			if (score > highScore) {
				SharedPreferences.Editor editor = share.edit();
				editor.putInt(HIGH_SCORE, score);
				editor.commit();
				highScore = score;
			}
			cancelAnimations();
			
			new AlertDialog.Builder(getContext()).setTitle(R.string.gameOver)
			.setMessage(resoureces.getString(R.string.score) + " " + score)
			.setPositiveButton(R.string.reset_game, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					displayScore();
					dialogDisplay = false;
					resetGame();
				}
			}).show();
		}
		else {
			lifeLayout.removeViewAt(lifeLayout.getChildCount() - 1);
			addNewSpot();
		}
	}
}

















