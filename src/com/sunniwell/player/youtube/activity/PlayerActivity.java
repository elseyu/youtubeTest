package com.sunniwell.player.youtube.activity;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.android.youtube.player.YouTubePlayerView;
import com.sunniwell.player.youtube.R;
import com.sunniwell.player.youtube.youtube.YoutubeConnector;

import org.greenrobot.eventbus.EventBus;

import java.util.Date;
import java.util.Random;

public class PlayerActivity extends YouTubeFailureRecoveryActivity implements YouTubePlayer.OnInitializedListener {
    private static final String TAG = "PlayerActivity";
    private static final int HIDE_PLAY_BUTTON = 101;
    private static final int HIDE_CONTROL_VIEW = 102;
    private static final int SHOW_PAUSE_BUTTON = 103;
    private static final String PLAYER_INFO_FRAGMENT = "PlayerInfoFragment";
    /**
     * 显示限制
     * 3000 豪秒
     */
    private static final int TIME_SHOW = 3000;

    private static final int MSG_HIDE = 0;
    private static final int MSG_TIME = 1;
    private static final int MSG_SEEK = 2;

    private YouTubePlayer mPlayer;
    private View mRootView;
    private PlayerHandler mHandler;
    private boolean mIsPause = false;
    private View mVideoInfoView;
    private TextView mTittleTextView;
    private TextView mNowTimeText;
    private TextView mCurrentTimeText;
    private SeekBar mSeekBar;
    private TextView mDurationTimeText;

    private int mSeekProgress = -1;
    private boolean isShow = false;
    private PopupWindow mPlayWindow;
    private PopupWindow mPauseWindow;
    private PopupWindow mVideoInfoWindow;

    @Override
    protected void onCreate(Bundle bundle) {
        super.onCreate(bundle);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        mRootView = LayoutInflater.from(this).inflate(R.layout.activity_player,null,false);
        setContentView(mRootView);
        initYouTubePlayerView();
        initVideoInfo();
        mHandler = new PlayerHandler();
    }



    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider,
                                        YouTubeInitializationResult result) {
        Toast.makeText(this, getString(R.string.failed), Toast.LENGTH_LONG).show();
    }

    @Override
    protected YouTubePlayer.Provider getYouTubePlayerProvider() {
        return (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtube_player_view);
    }

    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer player,
                                        boolean restored) {
        if (player == null) return;
        PlayerActivity.this.mPlayer = player;
        mPlayer.setPlayerStyle(YouTubePlayer.PlayerStyle.DEFAULT);
        mPlayer.setShowFullscreenButton(false);
        if(!restored) {
            player.loadVideo(getIntent().getStringExtra("VIDEO_ID"));
            Log.d(TAG, "onInitializationSuccess: " + getIntent().getStringExtra("VIDEO_ID"));
        }
        mPlayer.setPlaybackEventListener(new PlaybackEventListener());
        mPlayer.setPlayerStateChangeListener(new PlayerStateChangeListener());
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent keyEvent) {
        super.onKeyDown(keyCode,keyEvent);
        if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    if (mPlayer != null) {
                        if (mIsPause) {
                            mPlayer.play();
//                            showPlayingStyle();
                            mIsPause = false;
                        } else {
                            mPlayer.pause();
//                            showPausedStyle();
                            mIsPause = true;
                        }

                    }
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    isShow = false;
                    mHandler.removeMessages(HIDE_PLAY_BUTTON);
                    finish();
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    showVideoInfo();
                    break;
            }
        }
        return false;
    }

    private void showVideoInfo() {
        isShow = true;
        mVideoInfoWindow.showAtLocation(mRootView, Gravity.BOTTOM,0,0);
        mVideoInfoView.requestFocus();
        mSeekBar.requestFocus();
        mSeekProgress = -1;
        setTime();
        mNowTimeText.setText(DateFormat.getDateFormat(getApplicationContext()).format(new Date()));
        mHandler.removeMessages(MSG_TIME);
        mHandler.sendEmptyMessageDelayed(MSG_TIME, 500);
        mHandler.removeMessages(MSG_HIDE);
        mHandler.sendEmptyMessageDelayed(MSG_HIDE, TIME_SHOW);
    }


    private void hidePlayInfo() {
        mVideoInfoWindow.dismiss();
        isShow = false;
    }

    private void initYouTubePlayerView() {
        YouTubePlayerView playerView = (YouTubePlayerView) findViewById(R.id.youtube_player_view);
        playerView.initialize(YoutubeConnector.KEY,this);
    }

    private void initVideoInfo() {
        ImageView playImage = new ImageView(this);
        playImage.setImageResource(R.drawable.play);
        mPlayWindow = new PopupWindow(playImage,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        ImageView pauseImage = new ImageView(this);
        pauseImage.setImageResource(R.drawable.pause);
        mPauseWindow = new PopupWindow(pauseImage,
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        mVideoInfoView = LayoutInflater.from(this).inflate(R.layout.popup_video_info, null, false);
        mVideoInfoWindow = new PopupWindow(mVideoInfoView,
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        mVideoInfoWindow.setFocusable(true);
        mTittleTextView = (TextView) mVideoInfoView.findViewById(R.id.tittle);
        mNowTimeText = (TextView) mVideoInfoView.findViewById(R.id.now_time);
        mCurrentTimeText = (TextView) mVideoInfoView.findViewById(R.id.current_time);
        mSeekBar = (SeekBar) mVideoInfoView.findViewById(R.id.vod_seek);
        mDurationTimeText = (TextView) mVideoInfoView.findViewById(R.id.duration_time);

        if (getIntent().getStringExtra("VIDEO_TITTLE") != null) {
            mTittleTextView.setText(mTittleTextView.getText() + ":" +
                    getIntent().getStringExtra("VIDEO_TITTLE"));
        }

        mSeekBar.setOnKeyListener(mOnKeyListener);
        mSeekBar.setMax(0);
        mSeekBar.setProgress(0);
    }

    private void showPlayingStyle() {
        if (mPauseWindow != null && mPauseWindow.isShowing()) {
            mPauseWindow.dismiss();
        }
        mPlayWindow.showAtLocation(mRootView,Gravity.CENTER,0,0);
        if (mHandler.hasMessages(HIDE_PLAY_BUTTON)) {
            mHandler.removeMessages(HIDE_PLAY_BUTTON);
        }
        mHandler.sendEmptyMessageDelayed(HIDE_PLAY_BUTTON,TIME_SHOW);
    }

    private void showPausedStyle() {
        if (mPlayWindow != null && mPlayWindow.isShowing()) {
            mPlayWindow.dismiss();
        }
        mPauseWindow.showAtLocation(mRootView,Gravity.CENTER,0,0);
    }


    private void setTime(){
        mCurrentTimeText.setText("00:00:00");
        mDurationTimeText.setText("00:00:00");
        if(mPlayer != null && isShow) {
            int duration = mPlayer.getDurationMillis();
            if(duration > 0){
                int current = mPlayer.getCurrentTimeMillis();
                if(mSeekBar.getMax() == 0){
                    mSeekBar.setMax(duration / 1000);
                }
                if(mSeekProgress != -1){
                    mSeekBar.setProgress(mSeekProgress);
                } else {
                    mSeekBar.setProgress(current / 1000);
                }
                String currentString = makeTimeString(current / 1000);
                String durationString = makeTimeString(duration / 1000);
                if(mSeekProgress != -1){
                    mCurrentTimeText.setText(makeTimeString(mSeekProgress));
                    mDurationTimeText.setText(durationString);
                } else {
                    mCurrentTimeText.setText(currentString);
                    mDurationTimeText.setText(durationString);
                }
            }
        }
    }

    private String makeTimeString(int secs) {
        return String.format("%1$02d:%2$02d:%3$02d", secs / 3600, (secs % 3600) / 60, (secs % 3600 % 60) % 60);
    }

    private class PlaybackEventListener implements YouTubePlayer.PlaybackEventListener {

        @Override
        public void onPlaying() {
            Log.d(TAG, "onPlaying: ");
        }

        @Override
        public void onPaused() {
            Log.d(TAG, "onPaused: ");
        }

        @Override
        public void onStopped() {
            Log.d(TAG, "onStopped: ");
        }

        @Override
        public void onBuffering(boolean b) {
            Log.d(TAG, "onBuffering: ");
        }

        @Override
        public void onSeekTo(int i) {
            Log.d(TAG, "onSeekTo: ");
        }

    }
    private class PlayerStateChangeListener implements YouTubePlayer.PlayerStateChangeListener {

        @Override
        public void onLoading() {
            Log.d(TAG, "onLoading: ");
        }

        @Override
        public void onLoaded(String s) {
            Log.d(TAG, "onLoaded: ");
            mPlayer.play();
        }

        @Override
        public void onAdStarted() {
            Log.d(TAG, "onAdStarted: ");
        }

        @Override
        public void onVideoStarted() {
            Log.d(TAG, "onVideoStarted: ");
        }

        @Override
        public void onVideoEnded() {
            Log.d(TAG, "onVideoEnded: ");
        }
        @Override
        public void onError(YouTubePlayer.ErrorReason errorReason) {
            Log.d(TAG, "onError: " + errorReason.toString());
        }

    }
    private class PlayerHandler extends Handler {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case HIDE_PLAY_BUTTON:
                    if (mPlayWindow != null && mPlayWindow.isShowing()) {
                        mPlayWindow.dismiss();
                    }
                    break;
                case SHOW_PAUSE_BUTTON:
                    break;
                case MSG_HIDE:
                    this.removeMessages(MSG_TIME);
                    hidePlayInfo();
                    break;
                case MSG_TIME:
                    if (isShow) {
                        setTime();
                    }
                    mHandler.removeMessages(MSG_TIME);
                    mHandler.sendEmptyMessageDelayed(MSG_TIME, 500);
                    break;
                case MSG_SEEK:
                    mPlayer.seekToMillis(mSeekBar.getProgress() * 1000);
                    break;
                default:
                    break;
            }
        }

    }

    private View.OnKeyListener mOnKeyListener = new View.OnKeyListener() {
        @Override
        public boolean onKey(View v, int keyCode, KeyEvent event) {

            if(event.getAction() == KeyEvent.ACTION_DOWN){

                mHandler.removeMessages(MSG_HIDE);
                mHandler.sendEmptyMessageDelayed(MSG_HIDE, TIME_SHOW);
                int progress = mSeekBar.getProgress();
                int max = mSeekBar.getMax();
                switch (keyCode){
                    case KeyEvent.KEYCODE_DPAD_LEFT:
                        if(mSeekProgress == -1){
                            mSeekProgress = progress - 10;
                        } else {
                            mSeekProgress = mSeekProgress - 10;
                        }
                        if(mSeekProgress < 0){
                            mSeekProgress = 0;
                        }
                        if (isShow) {
                            setTime();
                        }
                        mHandler.removeMessages(MSG_SEEK);
                        mHandler.sendEmptyMessageDelayed(MSG_SEEK, 300);
                        return true;
                    case KeyEvent.KEYCODE_DPAD_RIGHT:
                        if(mSeekProgress == -1){
                            mSeekProgress = progress + 10;
                        } else {
                            mSeekProgress = mSeekProgress + 10;
                        }
                        if(mSeekProgress > max){
                            mSeekProgress = max;
                        }
                        if (isShow) {
                            setTime();
                        }
                        mHandler.removeMessages(MSG_SEEK);
                        mHandler.sendEmptyMessageDelayed(MSG_SEEK, 300);
                        return true;
                    case KeyEvent.KEYCODE_BACK:
                        isShow = false;
                        hidePlayInfo();
                    default:
                        break;
                }
            }
            return false;
        }
    };

}
