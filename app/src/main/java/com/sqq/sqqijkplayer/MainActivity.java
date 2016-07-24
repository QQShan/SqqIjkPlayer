package com.sqq.sqqijkplayer;

import android.animation.ValueAnimator;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.sqq.sqqijkplayer.utils.CommonUtils;
import com.sqq.sqqvideo.media.IjkVideoView;
import com.sqq.sqqvideo.setting.Settings;

import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

public class MainActivity extends AppCompatActivity  implements View.OnClickListener,
        SeekBar.OnSeekBarChangeListener,VideoPlayerPresentr.VideoPlayerView {

    private final String TAG = "MainActivity";
    Settings mSettings;

    private RelativeLayout flVideoView, title_view;
    private RelativeLayout rlLoadView,voice_light;
    private LinearLayout lyFloor;
    private LinearLayout lySystemBar;
    private IjkVideoView videoView;
    private TextView tvLoadmsg, tvTitle, tvTime, tvCurtime;
    private TextView tv_voice_light;
    private TextView tv_forward;
    private ImageView ivFullScreen;
    private ImageView ivBack;
    private ImageView iv_start_pause;
    private ImageView img_voice_light;
    private SeekBar seekBar;

    GestureDetector gestureDetector;

    private AudioManager audioManager;
    private int mMaxVolume;
    private int volume=-1;
    private float brightness=-1;
    private long newPosition=-1;

    private boolean isToolbarShow = true;
    private boolean mBackPressed = false;
    private boolean isScrolling = false;

    /**
     * 屏幕大小为几比几之类的
     */
    private static int SIZE_DEFAULT = 0;
    private static int SIZE_4_3 = 1;
    private static int SIZE_16_9 = 2;
    private int currentSize = SIZE_16_9;

    private boolean isDragging;
    private int defaultTimeout = 3000;
    private static final int MESSAGE_SHOW_PROGRESS = 1;
    private static final int MESSAGE_FADE_OUT = 2;
    private static final int MESSAGE_SEEK_NEW_POSITION = 3;
    private static final int MESSAGE_HIDE_CENTER_BOX = 4;

    private int screenWidth = 0;
    private int screenHeight = 0;

    private long duration;
    private int count = 0;

    private final int CONNECTION_TIMES = 5;

    VideoPlayerPresentr vpp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        vpp = new VideoPlayerPresentr(this);
        vpp.initSystembar(this);

        String title = "自拍的拍";
        String path = "http://10.0.3.114/MyVideo/video.3gp";

        title_view = (RelativeLayout) findViewById(R.id.rl_tt);
        title_view.setOnClickListener(this);
        flVideoView = (RelativeLayout) findViewById(R.id.rl_allview);
        rlLoadView = (RelativeLayout) findViewById(R.id.load_view);
        voice_light = (RelativeLayout) findViewById(R.id.voice_light);
        ivBack = (ImageView) findViewById(R.id.iv_back);
        ivBack.setOnClickListener(this);
        ivFullScreen = (ImageView) findViewById(R.id.iv_fullscreen);
        ivFullScreen.setOnClickListener(this);
        iv_start_pause = (ImageView) findViewById(R.id.iv_start_pause);
        iv_start_pause.setOnClickListener(this);
        img_voice_light = (ImageView) findViewById(R.id.img_voice_light);

        lyFloor = (LinearLayout) findViewById(R.id.ly_floor);
        // init player
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");

        videoView = (IjkVideoView) findViewById(R.id.videoview);

        tvLoadmsg = (TextView) findViewById(R.id.tv_tip);
        tv_voice_light = (TextView) findViewById(R.id.tv_voice_light);
        tvTitle = (TextView) findViewById(R.id.tv_title);
        tvTitle.setText(title);
        tvTime = (TextView) findViewById(R.id.tv_time);
        tvCurtime = (TextView) findViewById(R.id.tv_curtime);
        tv_forward = (TextView) findViewById(R.id.tv_forward);

        seekBar = (SeekBar) findViewById(R.id.sb_time);
        seekBar.setMax(1000);
        seekBar.setOnSeekBarChangeListener(this);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);

        gestureDetector = new GestureDetector(this,new PlayerGestureListener());
        flVideoView.setClickable(true);
        flVideoView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (gestureDetector.onTouchEvent(event))
                    return true;

                // 处理手势结束
                switch (event.getAction() & MotionEvent.ACTION_MASK) {
                    case MotionEvent.ACTION_UP:
                        endGesture();
                        break;
                }
                return false;
            }
        });
        initVideo(path);
    }

    public void initVideo(final String path) {
        mSettings = new Settings(this);

        screenWidth = CommonUtils.getScreenWidth(this);
        screenHeight = CommonUtils.getScreenHeight(this);

        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) flVideoView.getLayoutParams();
        lp.height = screenWidth * 9 / 16;
        lp.width = screenWidth;
        flVideoView.setLayoutParams(lp);

        hideAll();
        videoView.setVideoURI(Uri.parse(path));
        videoView.setOnPreparedListener(new IMediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(IMediaPlayer mp) {
                Log.d(TAG, "onPrepared");
                videoView.start();
            }
        });

        videoView.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer mp, int what, int extra) {
                Log.d(TAG, "setOnInfoListener");
                switch (what) {
                    case IjkMediaPlayer.MEDIA_INFO_BUFFERING_START:
                        tvLoadmsg.setText("loading");
                        rlLoadView.setVisibility(View.VISIBLE);
                        updatePausePlay();
                        break;
                    case IjkMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                        duration = videoView.getDuration();
                        tvTime.setText(CommonUtils.generateTime(duration));

                    case IjkMediaPlayer.MEDIA_INFO_BUFFERING_END:
                        rlLoadView.setVisibility(View.GONE);
                        showAll(defaultTimeout);
                        updatePausePlay();
                        break;
                }
                return false;
            }
        });

        videoView.setOnCompletionListener(new IMediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(IMediaPlayer mp) {
                Log.d(TAG, "onCompletion");
                rlLoadView.setVisibility(View.VISIBLE);
                updatePausePlay();
                videoView.stopPlayback();
                videoView.release(true);
                videoView.setVideoURI(Uri.parse(path));
            }
        });

        videoView.setOnErrorListener(new IMediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(IMediaPlayer mp, int what, int extra) {
                if (count > CONNECTION_TIMES) {
                    new AlertDialog.Builder(MainActivity.this)
                            .setMessage("视频暂时不能播放")
                            .setPositiveButton(R.string.VideoView_error_button,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int whichButton) {
                                            MainActivity.this.finish();
                                        }
                                    })
                            .setCancelable(false)
                            .show();
                } else {
                    videoView.stopPlayback();
                    videoView.release(true);
                    videoView.setVideoURI(Uri.parse(path));
                }
                count++;
                return false;
            }
        });

    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_fullscreen:
                fullScreen();
                break;
            /*case R.id.Liv:
                startActivity(new Intent(this, LiveActivity.class));
                break;*/
            case R.id.iv_back:
                if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    // 横屏的时候
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                } else {
                    finish();
                }

                break;
            case R.id.iv_start_pause:
                if (videoView.isPlaying()) {
                    videoView.pause();
                    updatePausePlay();
                } else {
                    videoView.start();
                    updatePausePlay();
                }
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        videoView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        videoView.resume();
    }

    @Override
    public void onBackPressed() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)// 横屏
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            mBackPressed = true;
            super.onBackPressed();
        }

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mBackPressed || !videoView.isBackgroundPlayEnabled()) {
            videoView.stopPlayback();
            videoView.release(true);
            videoView.stopBackgroundPlay();
        } else {
            videoView.enterBackground();
        }
        IjkMediaPlayer.native_profileEnd();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //切换到了横屏
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) flVideoView.getLayoutParams();
            lp.height = CommonUtils.getScreenHeight(this);
            lp.width = CommonUtils.getScreenWidth(this);
            flVideoView.setLayoutParams(lp);
            ivFullScreen.setImageResource(R.drawable.icon_live_cancel_fullscreen_normal);
        } else {
            FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) flVideoView.getLayoutParams();
            lp.height = CommonUtils.getScreenWidth(this) * 9 / 16;
            lp.width = CommonUtils.getScreenWidth(this);
            flVideoView.setLayoutParams(lp);
            ivFullScreen.setImageResource(R.drawable.icon_live_fullscreen_normal);
        }
        setScreenRate(currentSize);
    }

    public void fullScreen() {
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)// 切换为竖屏
        {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        }
    }

    public void setScreenRate(int rate) {
        screenWidth = CommonUtils.getScreenWidth(this);
        screenHeight = CommonUtils.getScreenHeight(this);
        int width = 0;
        int height = 0;
        if (getRequestedOrientation() == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
            // 横屏
            if (rate == SIZE_DEFAULT) {
                width = videoView.getmVideoWidth();
                height = videoView.getmVideoHeight();
            } else if (rate == SIZE_4_3) {
                width = screenHeight / 3 * 4;
                height = screenHeight;
            } else if (rate == SIZE_16_9) {
                width = screenHeight / 9 * 16;
                height = screenHeight;
            }
        } else {
            if (rate == SIZE_DEFAULT) {
                width = videoView.getmVideoWidth();
                height = videoView.getmVideoHeight();
            } else if (rate == SIZE_4_3) {
                width = screenWidth;
                height = screenWidth * 3 / 4;
            } else if (rate == SIZE_16_9) {
                width = screenWidth;
                height = screenWidth * 9 / 16;
            }
        }
        if (width > 0 && height > 0) {
            FrameLayout.LayoutParams vlp = (FrameLayout.LayoutParams) videoView.getmRenderView().getView().getLayoutParams();
            vlp.width = width;
            vlp.height = height;
            videoView.getmRenderView().getView().setLayoutParams(vlp);
        }
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

        if (!fromUser) {
            return;
        }
        Log.d("sqqqq","onProgressChanged");
        int newPosition = (int) ((duration * progress * 1.0) / 1000);
        videoView.seekTo(newPosition);
        tvCurtime.setText(CommonUtils.generateTime(newPosition));
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        isDragging = true;
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        audioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        isDragging = false;
        handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_FADE_OUT), defaultTimeout);
        handler.sendEmptyMessageDelayed(MESSAGE_SHOW_PROGRESS, 1000);
    }

    @SuppressWarnings("HandlerLeak")
    private Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MESSAGE_FADE_OUT:
                    if (!isDragging&& !isScrolling) {
                        hideAll();
                    }
                    break;
                case MESSAGE_SHOW_PROGRESS:
                    setProgress();
                    if (!isDragging&&!isScrolling) {
                        //如果没有在手动滑动seekBar
                        msg = obtainMessage(MESSAGE_SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000);
                        updatePausePlay();
                    }
                    /*if (!isDragging && isShowing) {
                        msg = obtainMessage(MESSAGE_SHOW_PROGRESS);
                        sendMessageDelayed(msg, 1000);
                        updatePausePlay();
                    }*/
                    break;
                case MESSAGE_HIDE_CENTER_BOX:
                    voice_light.setVisibility(View.GONE);
                    rlLoadView.setVisibility(View.GONE);
                    break;

                case MESSAGE_SEEK_NEW_POSITION:
                    tv_forward.setText("");
                    videoView.seekTo((int) newPosition);
                    videoView.start();
                    newPosition = -1;
                    break;
            }
        }
    };

    private long setProgress() {
        if (isDragging||isScrolling) {
            return 0;
        }

        long position = videoView.getCurrentPosition();
        long duration = videoView.getDuration();
        if (seekBar != null) {
            if (duration > 0) {
                long pos = 1000L * position / duration;
                seekBar.setProgress((int) pos);
            }
            int percent = videoView.getBufferPercentage();
            //缓存的进度，比播放的速度快
            seekBar.setSecondaryProgress(percent * 10);
        }
        tvCurtime.setText(CommonUtils.generateTime(position));
        return position;
    }

    @Override
    public void showStatusBar() {
        if(lySystemBar==null){
            lySystemBar = (LinearLayout) findViewById(R.id.ly_system_bar);
        }
        lySystemBar.setVisibility(View.VISIBLE);
        LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) lySystemBar.getLayoutParams();
        lp.height = CommonUtils.getStatusHeight(this);
        lySystemBar.setLayoutParams(lp);
    }

    @Override
    public void hideStatusBar() {
        if(lySystemBar==null){
            lySystemBar = (LinearLayout) findViewById(R.id.ly_system_bar);
        }
        lySystemBar.setVisibility(View.GONE);
    }


    /**
     * 改变播放、暂停的按钮
     */
    private void updatePausePlay() {
        if (videoView.isPlaying()) {
            iv_start_pause.setImageResource(R.drawable.ic_stop_white_24dp);
        } else {
            iv_start_pause.setImageResource(R.drawable.ic_play_arrow_white_24dp);
        }
    }


    private void hideAll() {
        if(isToolbarShow) {
            getWindow().getDecorView().setSystemUiVisibility(View.INVISIBLE);
            hideToolbar(title_view);
            hideFloor(lyFloor);
            isToolbarShow = false;
        }
    }

    private void showAll(int timeout) {
        if(!isToolbarShow) {
            getWindow().getDecorView().setSystemUiVisibility(View.VISIBLE);
            showToolbar(title_view);
            showFloor(lyFloor);
            isToolbarShow = true;
            handler.sendEmptyMessage(MESSAGE_SHOW_PROGRESS);
            handler.removeMessages(MESSAGE_FADE_OUT);
            if (timeout != 0) {
                handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_FADE_OUT), timeout);
            }
        }
    }

    public void hideFloor(final View v) {
        ValueAnimator animator;
        if (Build.VERSION.SDK_INT >= 19) {
            animator = ValueAnimator.ofFloat(0, (CommonUtils.dip2px(this, 55) + CommonUtils.getStatusHeight(this)));
        } else {
            animator = ValueAnimator.ofFloat(0, CommonUtils.dip2px(this, 55));
        }
        animator.setTarget(v);
        animator.setDuration(400).start();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                v.setTranslationY((Float) animation.getAnimatedValue());
            }
        });
    }

    public void showFloor(final View v) {
        ValueAnimator animator;
        if (Build.VERSION.SDK_INT >= 19) {
            animator = ValueAnimator.ofFloat((CommonUtils.dip2px(this, 55) + CommonUtils.getStatusHeight(this)), 0);
        } else {
            animator = ValueAnimator.ofFloat(CommonUtils.dip2px(this, 55), 0);
        }
        animator.setTarget(v);
        animator.setDuration(400).start();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                v.setTranslationY((Float) animation.getAnimatedValue());
            }
        });
    }

    public void hideToolbar(final View v) {
        ValueAnimator animator;
        if (Build.VERSION.SDK_INT >= 19) {
            animator = ValueAnimator.ofFloat(0, -(CommonUtils.dip2px(this, 55) + CommonUtils.getStatusHeight(this)));
        } else {
            animator = ValueAnimator.ofFloat(0, -CommonUtils.dip2px(this, 55));
        }
        animator.setTarget(v);
        animator.setDuration(400).start();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                v.setTranslationY((Float) animation.getAnimatedValue());
            }
        });
    }

    public void showToolbar(final View v) {
        ValueAnimator animator;
        if (Build.VERSION.SDK_INT >= 19) {
            animator = ValueAnimator.ofFloat(-(CommonUtils.dip2px(this, 55) + CommonUtils.getStatusHeight(this)), 0);
        } else {
            animator = ValueAnimator.ofFloat(-CommonUtils.dip2px(this, 55), 0);
        }
        animator.setTarget(v);
        animator.setDuration(500).start();
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                v.setTranslationY((Float) animation.getAnimatedValue());
            }
        });
    }

    /**
     * 滑动改变声音大小
     *
     * @param percent
     */
    private void onVolumeSlide(float percent) {
        if (volume == -1) {
            volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (volume < 0)
                volume = 0;
        }
        hideAll();

        int index = (int) (percent * mMaxVolume) + volume;
        if (index > mMaxVolume)
            index = mMaxVolume;
        else if (index < 0)
            index = 0;

        // 变更声音
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);

        // 变更进度条
        int i = (int) (index * 1.0 / mMaxVolume * 100);
        String s = i + "%";
        if (i == 0) {
            s = "off";
        }
        // 显示
        img_voice_light.setImageResource(i == 0 ? R.drawable.ic_volume_off_white_36dp : R.drawable.ic_volume_up_white_36dp);
        tv_voice_light.setText(s);
        voice_light.setVisibility(View.VISIBLE);
        rlLoadView.setVisibility(View.GONE);
    }

    /**
     * 滑动改变亮度
     *
     * @param percent
     */
    private void onBrightnessSlide(float percent) {
        if (brightness < 0) {
            brightness = getWindow().getAttributes().screenBrightness;
            if (brightness <= 0.00f){
                brightness = 0.50f;
            }else if (brightness < 0.01f){
                brightness = 0.01f;
            }
        }
        //Log.d(this.getClass().getSimpleName(), "brightness:" + brightness + ",percent:" + percent);
        voice_light.setVisibility(View.VISIBLE);
        rlLoadView.setVisibility(View.GONE);
        WindowManager.LayoutParams lpa = getWindow().getAttributes();
        lpa.screenBrightness = brightness + percent;
        if (lpa.screenBrightness > 1.0f){
            lpa.screenBrightness = 1.0f;
        }else if (lpa.screenBrightness < 0.01f){
            lpa.screenBrightness = 0.01f;
        }
        tv_voice_light.setText(((int) (lpa.screenBrightness * 100)) + "%");
        img_voice_light.setImageResource(R.drawable.ic_brightness_6_white_36dp);
        getWindow().setAttributes(lpa);
    }

    private void onProgressSlide(float percent) {
        long position = videoView.getCurrentPosition();
        long duration = videoView.getDuration();
        //long deltaMax = Math.min(100 * 1000, duration - position);
        long deltaMax = duration;
        long delta = (long) (deltaMax * percent);


        newPosition = delta + position;
        if (newPosition > duration) {
            newPosition = duration;
        } else if (newPosition <= 0) {
            newPosition=0;
            delta=-position;
        }
        int showDelta = (int) delta / 1000;
        if (showDelta != 0) {
            if (seekBar != null) {
                if (duration > 0) {
                    long pos = 1000L * newPosition / duration;
                    seekBar.setProgress((int) pos);
                }
                int percents = videoView.getBufferPercentage();
                //缓存的进度，比播放的速度快
                seekBar.setSecondaryProgress(percents * 10);
            }
            videoView.pause();
            String text = showDelta > 0 ? ("+" + showDelta) : "" + showDelta;
            tv_forward.setText(text + "s");
            tvCurtime.setText(CommonUtils.generateTime(newPosition));
            tvTime.setText(CommonUtils.generateTime(duration));
        }
    }

    /**
     * 手势结束
     */
    private void endGesture() {
        isScrolling=false;
        volume = -1;
        brightness = -1f;
        if (newPosition >= 0) {
            handler.removeMessages(MESSAGE_SEEK_NEW_POSITION);
            handler.sendEmptyMessage(MESSAGE_SEEK_NEW_POSITION);
            handler.sendMessageDelayed(handler.obtainMessage(MESSAGE_FADE_OUT), defaultTimeout);
            handler.sendEmptyMessageDelayed(MESSAGE_SHOW_PROGRESS, 1000);
        }
        handler.removeMessages(MESSAGE_HIDE_CENTER_BOX);
        handler.sendEmptyMessageDelayed(MESSAGE_HIDE_CENTER_BOX, 500);
    }

    public class PlayerGestureListener extends GestureDetector.SimpleOnGestureListener {
        private boolean firstTouch;
        private boolean volumeControl;
        private boolean toSeek;

        /**
         * 双击
         */
        @Override
        public boolean onDoubleTap(MotionEvent e) {
            isScrolling = false;
            videoView.toggleAspectRatio();
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            isScrolling = false;
            firstTouch = true;
            return super.onDown(e);
        }

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            isScrolling = false;
            //点击显示和隐藏进度条、时间等
            if (isToolbarShow) {
                hideAll();
            } else {
                showAll(defaultTimeout);
            }
            return true;
        }

        /**
         * 滑动
         */
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            isScrolling = true;
            float mOldX = e1.getX(), mOldY = e1.getY();
            float deltaY = mOldY - e2.getY();
            float deltaX = mOldX - e2.getX();
            if (firstTouch) {
                toSeek = Math.abs(distanceX) >= Math.abs(distanceY);
                volumeControl=mOldX > getResources().getDisplayMetrics().widthPixels * 0.5f;
                firstTouch = false;
            }

            if (toSeek) {

                onProgressSlide(-deltaX / videoView.getWidth());
            } else {
                float percent = deltaY / videoView.getHeight();
                if (volumeControl) {
                    onVolumeSlide(percent);
                } else {
                    onBrightnessSlide(percent);
                }
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }
    }


}