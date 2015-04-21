package com.essentialappsfm.sporttechniquesprinting;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.os.Bundle;
import android.provider.Settings.Secure;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.MediaController;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.LogRecord;


public class CameraActivity extends Activity
{
    private static final int VIDEO_REQUEST_CODE = 200;
   // private static final String TAG = CameraActivity.class.getSimpleName();
    public final int MEDIA_TYPE_VIDEO = 1;
    private Intent intentVideo;
    private VideoView videoDisplay;
    private TextView timeText;
    private SeekBar seekbar;
    private Uri fileUri;
    private String stringUri;
    private String username = "Fergus";
    private ImageButton controlButton;
    boolean isPlaying = true;

    private FrameLayout frameLayout;
    private DrawingSurface drawSurface;
    SurfaceHolder holder;
    int buttonClick = 0;
    private static final String TAG = "Tag";

    private String device;

    private boolean isCameraSupported()
    {
        if(getApplicationContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA))
        {
            // this device has a camera
            return true;
        }
        else
        {
            // no camera on this device
            return false;
        }
    }

    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        // Checking camera availability
        if (!isCameraSupported())
        {
            Toast.makeText(getApplicationContext(),"Sorry! There is no camera present on this device",Toast.LENGTH_LONG).show();

            // will close the app if the device doesn't have camera
            finish();
        }

        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_view);

        //create new Intent
        intentVideo = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);

        //These lines are moved to move to next view
        intentVideo.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 1); // set the video image quality to high
        intentVideo.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 2);
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        fileUri = saveOutputMediaFileUri(MEDIA_TYPE_VIDEO);  // create a file to save the video
        intentVideo.putExtra(MediaStore.EXTRA_OUTPUT, fileUri);  // set the image file name

        // start the Video Capture Intent
        startActivityForResult(intentVideo, VIDEO_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == VIDEO_REQUEST_CODE)
        {
            if (resultCode == RESULT_OK)
            {
                videoDisplay = (VideoView) findViewById(R.id.videoView);
                videoDisplay.setOnPreparedListener(prepareListener);
                videoDisplay.setOnCompletionListener(completelistener);
                videoDisplay.setVideoPath(fileUri.getPath());

                controlButton = (ImageButton) findViewById(R.id.play_button);
                timeText = (TextView) findViewById(R.id.textviewTime);

                seekbar = (SeekBar) findViewById(R.id.seekbarMain);
                seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener()
                {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser)
                    {
                        videoDisplay.seekTo(progress);
                        timeText.setText(String.valueOf(videoDisplay.getCurrentPosition()/1000 + "." + videoDisplay.getCurrentPosition()%1000));
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar)
                    {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar)
                    {
                    }
                });

                // Video captured and saved to fileUri Intent
                stringUri = fileUri.toString();

                Video myVideo = new Video();
                myVideo.id = Secure.getString(this.getContentResolver(), Secure.ANDROID_ID);
                myVideo.user = username;
                myVideo.vidPath = stringUri;

                SaveMongoInstance task = new SaveMongoInstance();
                task.execute(myVideo);

                Toast.makeText(this, "Video Saved to:" + stringUri, Toast.LENGTH_LONG).show();
            }
            else if (resultCode == RESULT_CANCELED)
            {
                // User cancelled the video capture
                // user cancelled recording
                Toast.makeText(getApplicationContext(),"Video Recording Cancelled", Toast.LENGTH_SHORT).show();
            }
            else
            {
                // failed to record video
                Toast.makeText(getApplicationContext(), "Failed to Record Video", Toast.LENGTH_SHORT).show();
            }
        }
    }

    MediaPlayer.OnPreparedListener prepareListener = new MediaPlayer.OnPreparedListener()
    {
        @Override
        public void onPrepared(MediaPlayer mp)
        {
            mp.setVolume(0f, 0f);
            seekbar.postDelayed(incrementSeekbar, 1000);
        }
    };
    MediaPlayer.OnCompletionListener completelistener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp)
        {
            controlButton.setImageResource(R.drawable.ic_action_play);
            seekbar.setProgress(0);
        }
    };

    private Runnable incrementSeekbar = new Runnable()
    {
        @Override
        public void run()
        {
            seekbar.setProgress(videoDisplay.getCurrentPosition());
            seekbar.setMax(videoDisplay.getDuration());
            seekbar.postDelayed(incrementSeekbar, 1000);
        }
    };

    public void playPause(View view)
    {
        videoControl(videoDisplay, controlButton);
    }

    public void videoControl(VideoView vid, ImageButton button)
    {
        if(vid.isPlaying())
        {
            button.setImageResource(R.drawable.ic_action_play);
            vid.pause();
        }
        else
        {
            button.setImageResource(R.drawable.ic_action_pause);
            vid.start();
        }
        isPlaying = !isPlaying;
    }

    public void startDrawing(View v)    {
        buttonClick++;
        frameLayout = (FrameLayout) findViewById(R.id.videoFrame);

        if(videoDisplay.isPlaying())     {
            Toast.makeText(getApplicationContext(),"Video must be paused to use Drawing tools", Toast.LENGTH_SHORT).show();
        }
             {
            if(buttonClick%2 == 0){
                frameLayout.removeView(drawSurface);
            }
            else{
                drawSurface = new DrawingSurface(this);
                holder = drawSurface.getHolder();
                holder.setFormat(PixelFormat.TRANSPARENT);
                drawSurface.setZOrderOnTop(true);
                frameLayout.addView(drawSurface);
            }
        }
        Log.d(TAG, "Buttonclick = " + buttonClick);
    }

    public void advanceSide(View view)
    {
        Intent i = new Intent(this, ComparisonActivity.class);
        i.setData(fileUri);
        startActivity(i);
    }


    /** Create a file Uri for saving an image or video to specific folder
     * https://developer.android.com/guide/topics/media/camera.html#saving-media
     * */
    public Uri saveOutputMediaFileUri(int type)
    {
        return Uri.fromFile(saveOutputMediaFile(type));
    }

    /** Create a File for saving an image or video */
    public File saveOutputMediaFile(int type)
    {
        // To be safe, you should check that the SDCard is mounted

        if(Environment.getExternalStorageState() != null)
        {
            // this works for Android 2.2 and above
            File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "SprintVideos");

            // This location works best if you want the created images to be shared
            // between applications and persist after your app has been uninstalled.

            // Create the storage directory if it does not exist
            if (! mediaStorageDir.exists())
            {
                if (! mediaStorageDir.mkdirs())
                {
                    Log.d(TAG, "failed to create directory");
                    return null;
                }
            }

            // Create a media file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            File mediaFile;
            if(type == MEDIA_TYPE_VIDEO)
            {
                mediaFile = new File(mediaStorageDir.getPath() + File.separator +
                        "VID_"+ timeStamp + ".mp4");
            } else
            {
                return null;
            }
            return mediaFile;
        }
        return null;
    }
}
