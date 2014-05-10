package com.thinktank.recorderisraelstasolution;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.thinktank.recorderisraelstasolution.R;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.Timer;
import java.util.TimerTask;

public class AndroidVideoCapture2 extends Activity {

	// set the maximum time per recording in milliseconds (60,000 = 60 seconds)
	int maxLength = 120000;

	// set the max file size per recording in bytes
	int maxSize = 100 * 1048576; // 1*1,048,576 = 1 megabyte

	// milliseconds before the OnClickListener can be used again; prevents
	// crashing due to double clicking (don't make less than 3000)
	int clickDelay = 3000;
	CountDownTimer clickDelayTimer; // Dynamically set in code to ClickDelay
	boolean justClicked;

	// control how long the thank you screen is displayed
	int thanksOverlayDelay = 2000; // in milliseconds
	boolean thanksOverlay;
	CountDownTimer thanksOverlayTimer;

	private Camera myCamera;
	private MyCameraSurfaceView myCameraSurfaceView;
	private MediaRecorder mediaRecorder;

	FrameLayout cameraPreview;
	ImageView messageOverlay;
	TextView micLevel;
	TextView mTextField;
	CountDownTimer timeRemaining;
	
	int amplitude;
	int amplitudePercent;

	SurfaceHolder surfaceHolder;
	boolean recording;

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		justClicked = false;
		recording = false;

		// Get Camera for preview
		myCamera = getCameraInstance();

		// If there is no camera, exit gracefully
		if (myCamera == null) {
			Toast.makeText(AndroidVideoCapture2.this, "Fail to get Camera", Toast.LENGTH_LONG).show();
		}

		// find the elements in the main.xml for where to put the video preview,
		// overlay, and timer text
		setContentView(R.layout.main);
		myCameraSurfaceView = new MyCameraSurfaceView(this, myCamera);
		FrameLayout myCameraPreview = (FrameLayout) findViewById(R.id.cameraPreview);
		myCameraPreview.addView(myCameraSurfaceView);
		cameraPreview = (FrameLayout) findViewById(R.id.cameraPreview);
		cameraPreview.setOnClickListener(myViewOnClickListener);
		messageOverlay = (ImageView) findViewById(R.id.messageOverlay);
		micLevel = (TextView) findViewById(R.id.micLevel);
		mTextField = (TextView) findViewById(R.id.recordingTimer);
	}
	
	View.OnClickListener myViewOnClickListener = new View.OnClickListener() {
		@Override
		public void onClick(View v) {
			// test to see if there was a click this prevents crashing due to
			// double clicking
			if (!justClicked) {
				justClicked = true;
				// start a timer in milliseconds*clickDelay
				clickDelayTimer = new CountDownTimer(clickDelay, 1000) {
					public void onTick(long millisUntilFinished) {

					}

					// unlock statements when the clickDelayTimer timer finishes
					public void onFinish() {
						justClicked = false;
					}
				};
				clickDelayTimer.start();

				// if the camera is recording when clicked, stop recording
				if (recording) {
					stopRecording();
				} else {
					if (thanksOverlay) {
						thanksOverlayTimer.cancel();
					}
					releaseCamera();
					if (!prepareMediaRecorder()) {
						Toast.makeText(AndroidVideoCapture2.this, "Fail in prepareMediaRecorder()!\n - Ended -", Toast.LENGTH_LONG).show();
						finish();
					}

					mediaRecorder.start();
					recording = true;
					messageOverlay.setImageResource(R.drawable.overlayrecording);
					
					new Timer().scheduleAtFixedRate(new TimerTask() {          
					    @Override
					    public void run() {
					    	runOnUiThread(new Runnable(){
					    	    @Override
					    	    public void run(){
					    	    	if (recording) {
										amplitude = mediaRecorder.getMaxAmplitude()/300;
						    	    	//micLevel.setText(Integer.toString(amplitude));
						    	    	if (amplitude < 10) {
											micLevel.setText("");
						    	    	};
						    	    	if (amplitude > 9 && amplitude < 20) {
											micLevel.setText("|");
						    	    	};
						    	    	if (amplitude > 19 && amplitude < 30) {
											micLevel.setText("||");
						    	    	};
						    	    	if (amplitude > 29 && amplitude < 40) {
											micLevel.setText("|||");
						    	    	};
						    	    	if (amplitude > 39 && amplitude < 50) {
											micLevel.setText("||||");
						    	    	};
						    	    	if (amplitude > 49 && amplitude < 60) {
											micLevel.setText("||||");
						    	    	};
						    	    	if (amplitude > 59 && amplitude < 70) {
											micLevel.setText("|||||");
						    	    	};
						    	    	if (amplitude > 69 && amplitude < 80) {
											micLevel.setText("||||||");
						    	    	};
						    	    	if (amplitude > 79 && amplitude < 90) {
											micLevel.setText("|||||||");
						    	    	};
						    	    	if (amplitude > 89) {
											micLevel.setText("||||||||");
						    	    	};
					    	    	} else {
					    	    		micLevel.setText("");
					    	    	}
					    	    }
					    	});
					    }
					}, 50, 50);
					
					timeRemaining = new CountDownTimer(maxLength, 1000) {

						public void onTick(long millisUntilFinished) {
							if (recording) {
								mTextField.setText("seconds remaining: " + millisUntilFinished / 1000);
							}
						}

						// when the timer is up, stop recording
						public void onFinish() {
							stopRecording();
						}
					};
					timeRemaining.start();
				}
			}
		}
	};

	// this function chooses the camera hardware and gets an instance of it
	private Camera getCameraInstance() {
		Camera c = null;
		try {
			// attempt to get a Camera instance
			c = Camera.open(1); // selects the camera, 1 = front, 0 = back
		} catch (Exception e) { // Camera is not available (in use or does not
								// exist)
		}
		return c; // returns null if camera is unavailable
	}

	// this function prepares the media recorder object, including camera and
	// format settings, don't change these unless you know what you're doing
	private boolean prepareMediaRecorder() {
		myCamera = getCameraInstance();
		myCamera.unlock();

		mediaRecorder = new MediaRecorder();
		mediaRecorder.setCamera(myCamera);
		mediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
		mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

		// This must be set to QUALITY_LOW for front-facing cameras, as high
		// resolution is not supported on the front-facing camera. When using
		// rear facing cameras, this may be set to QUALITY_HIGH
		mediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_LOW));

		// Get the current date and time and write it to a string
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
		String currentDateandTime = sdf.format(new Date());
		mediaRecorder.setOutputFile(Environment.getExternalStorageDirectory().getPath() + "/Movies/thinktank-" + currentDateandTime + ".mp4");
		mediaRecorder.setMaxDuration(maxLength);
		mediaRecorder.setMaxFileSize(maxSize);
		mediaRecorder.setPreviewDisplay(myCameraSurfaceView.getHolder().getSurface());

		try {
			mediaRecorder.prepare();
		} catch (IllegalStateException e) {
			releaseMediaRecorder();
			return false;
		} catch (IOException e) {
			releaseMediaRecorder();
			return false;
		}
		return true;

	}

	/*
	 * everything below here is either auto-generated or just flat-out shouldn't
	 * be changed Ñ it has to do with releasing and capturing the hardware for
	 * recording, and setting up the SurfaceView call backs
	 */
	private void stopRecording() {
		mediaRecorder.stop();
		releaseMediaRecorder();

		// put the thanks overlay on the screen for thanksOverlayDelay
		messageOverlay.setImageResource(R.drawable.overlaythanks);
		if (!thanksOverlay) {
			thanksOverlay = true;
			thanksOverlayTimer = new CountDownTimer(thanksOverlayDelay, 1000) {
				public void onTick(long millisUntilFinished) {

				}

				// unlock statements when the clickDelayTimer timer finishes
				public void onFinish() {
					thanksOverlay = false;

					// restore please help us overlay
					messageOverlay.setImageResource(R.drawable.overlay);
				}
			};
		}
		thanksOverlayTimer.start();
		recording = false;
		timeRemaining.cancel();
		mTextField.setText(" ");
	}

	// When the program is put into the background, stop recording and exit the
	// program
	@Override
	protected void onPause() {
		super.onPause();
		if (recording) {
			stopRecording();
		}
		releaseCamera();
		finish();
	}

	private void releaseMediaRecorder() {
		if (mediaRecorder != null) {
			mediaRecorder.reset(); // clear recorder configuration
			mediaRecorder.release(); // release the recorder object
			mediaRecorder = null;
			// myCamera.lock(); // lock camera for later use
		}
	}

	private void releaseCamera() {
		if (myCamera != null) {
			myCamera.release(); // release the camera for other applications
			myCamera = null;
		}
	}

	public class MyCameraSurfaceView extends SurfaceView implements SurfaceHolder.Callback {
		private SurfaceHolder mHolder;
		private Camera mCamera;

		public MyCameraSurfaceView(Context context, Camera camera) {
			super(context);
			mCamera = camera;
			mHolder = getHolder();
			mHolder.addCallback(this);
			// deprecated setting, but required on Android versions prior to 3.0
			// mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		}

		@Override
		public void surfaceChanged(SurfaceHolder holder, int format, int weight, int height) {
			if (mHolder.getSurface() == null) {
				// preview surface does not exist
				return;
			}
			// stop preview before making changes
			try {
				mCamera.stopPreview();
			} catch (Exception e) {
				// ignore: tried to stop a non-existent preview
			}
			try {
				mCamera.setPreviewDisplay(mHolder);
				mCamera.startPreview();
			} catch (Exception e) {
			}
		}

		@Override
		public void surfaceCreated(SurfaceHolder holder) {
			// TODO Auto-generated method stub
			// The Surface has been created, now tell the camera where to draw
			// the preview.
			try {
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();
			} catch (IOException e) {
			}
		}

		@Override
		public void surfaceDestroyed(SurfaceHolder holder) {
			// TODO Auto-generated method stub

		}
	}
}