package com.sqq.sqqijkplayer;

import android.app.Activity;
import android.graphics.Color;
import android.os.Build;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

/**
 * Created by sqq on 2016/7/21.
 */
public class VideoPlayerPresentr {

    public interface VideoPlayerView{
        public void showStatusBar();
        public void hideStatusBar();
    }

    VideoPlayerView view;

    public VideoPlayerPresentr(VideoPlayerView view){
        this.view = view;
    }

    public void initSystembar(Activity act) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //5.0实现全透明
            Window window = act.getWindow();
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.TRANSPARENT);
            view.showStatusBar();
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            //4.4全透明
            act.getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            view.showStatusBar();
        } else {
            view.hideStatusBar();
        }
    }
}
