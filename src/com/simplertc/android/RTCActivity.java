package com.simplertc.android;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Point;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.view.Window;
import android.widget.EditText;
import android.widget.Toast;

import org.json.JSONObject;
import org.webrtc.MediaStream;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.VideoRenderer;
import org.webrtc.VideoRendererGui;

import com.simplertc.android.RTCEventSource.EventHandler;

public class RTCActivity extends Activity implements WebRtcClient.RTCListener {
	private com.simplertc.android.AppRTCGLView vsv;
	private WebRtcClient client;
	private VideoRenderer.Callbacks localRender;
	private VideoRenderer.Callbacks remoteRender;
	PowerManager powerManager = null;
    WakeLock wakeLock = null;
    
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		powerManager = (PowerManager)this.getSystemService(RTCActivity.POWER_SERVICE);
        wakeLock = this.powerManager.newWakeLock(PowerManager.FULL_WAKE_LOCK, "My Lock");

		Thread.setDefaultUncaughtExceptionHandler(new UnhandledExceptionHandler(
				this));

		requestWindowFeature(Window.FEATURE_NO_TITLE);

		// Camera display view
		Point displaySize = new Point();
		getWindowManager().getDefaultDisplay().getSize(displaySize);

		vsv = new AppRTCGLView(this, displaySize);
		VideoRendererGui.setView(vsv);
		remoteRender = VideoRendererGui.create(0, 0, 100, 100);
		localRender = VideoRendererGui.create(70, 5, 25, 25);
		setContentView(vsv);

		PeerConnectionFactory.initializeAndroidGlobals(this, true, true);
		client = new WebRtcClient(RTCActivity.this);
		client.addListener(RTCEvents.error, new EventHandler() {
			
			@Override
			public void onExcute(String type, JSONObject data) throws Exception {
				Toast.makeText(getApplicationContext(), "error "+data.getString("errorMessage"),
						Toast.LENGTH_SHORT).show();
			}
		});
		
		final EditText roomInput = new EditText(this);
		roomInput.setText("ws://leechanproxy.cloudapp.net:8000/");
		roomInput.setSelection(roomInput.getText().length());
		DialogInterface.OnClickListener listener = new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				String url = roomInput.getText().toString();
				client.configAudio().configVideo("front", "480", "640");
				client.connectChannel(url);
			}
		};
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage("Enter room URL").setView(roomInput)
				.setPositiveButton("Go!", listener).show();
	}

	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
	}

	@Override
	public void onPause() {
		super.onPause();
		wakeLock.release();
		vsv.onPause();
		client.stopLocalVideo();
	}

	@Override
	public void onResume() {
		super.onResume();
		wakeLock.acquire();
		vsv.onResume();
		client.restartLocalVideo();	
	}
	
	 @Override
	 protected void onDestroy() {
	    if(client!=null)
	    	client.close();
	    super.onDestroy();
	  }
	 
	 
	@Override
	public void onStatusChanged(final String newStatus) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				Toast.makeText(getApplicationContext(), newStatus,
						Toast.LENGTH_SHORT).show();
			}
		});
	}

	@Override
	public void onLocalStream(final MediaStream localStream) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				localStream.videoTracks.get(0).addRenderer(
						new VideoRenderer(localRender));
			}
		});
	}

	@Override
	public void onAddRemoteStream(final MediaStream remoteStream) {
		runOnUiThread(new Runnable() {
			@Override
			public void run() {
				remoteStream.videoTracks.get(0).addRenderer(new VideoRenderer(remoteRender));
			}
		});

	}

	@Override
	public void onRemoveRemoteStream(final MediaStream remoteStream) {
		runOnUiThread(new Runnable() {

			@Override
			public void run() {
				remoteStream.videoTracks.get(0).dispose();
			}
		});

	}
}
