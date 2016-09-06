package com.sunniwell.player.youtube.activity;

import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.sunniwell.player.youtube.R;
import com.sunniwell.player.youtube.bean.VideoBean;
import com.sunniwell.player.youtube.widget.matrix.IconItem;
import com.sunniwell.player.youtube.widget.matrix.MatrixRenderer;
import com.sunniwell.player.youtube.widget.matrix.MatrixView;
import com.sunniwell.player.youtube.widget.matrix.OnItemSelectedListener;
import com.sunniwell.player.youtube.youtube.YoutubeConnector;

import java.io.File;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

import rx.functions.Action1;

/**
 * Created by Administrator on 2016/8/30.
 */
public class MatrixViewActivity extends Activity {
    private static final String TAG = "MatrixViewActivity";
    private static final int TAG_MATRIX = 0;

    private static final int MATRIX_ITEM_CLICK = 3;//媒资item点击

    private ViewGroup mVideoWall;
    private MatrixView mMatrixView;
    private Bitmap mDefaultVodBitmap = null;
    private Bitmap mFocusBitmap = null;
    private List<VideoBean> mSearchResults;
    private TextView mPageTextView = null;
    private YoutubeConnector mYouTubeSearcher = null;
    private List<VideoBean> mNextPage;
    private ArrayList<IconItem> mItemList = null;

    private Handler mHandler;
    private DisplayImageOptions mDisplayOptions;
    private boolean isLoadingMore = false;
    private Button mSearchButton;
    private EditText mSearchText;

    private int mUnitWidth;
    private int mUnitHeight;
    public static final int MAX_COLUMN = 4;//最大列数
    private static final int MAX_ROW = 3;//最大行数
    private long scrollTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_matrix_view);
        initImageLoader();
        mVideoWall = (ViewGroup) findViewById(R.id.video_wall);
        initMatrixView();
        mHandler = new VideoWallHandler(this);
        mPageTextView = (TextView) findViewById(R.id.page_index);
        mSearchText = (EditText) findViewById(R.id.search_text);
        mSearchButton = (Button) findViewById(R.id.search_button);
        mSearchText.requestFocus();
        initSearch();
        mYouTubeSearcher = new YoutubeConnector(this);
        searchOnYoutube("");
    }

    private void initSearch() {
        View.OnKeyListener onKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                            setFocusable(true);
                            requestFocus();
                            return true;
                        default:
                            break;
                    }
                }
                return false;
            }
        };
        mSearchButton.setOnKeyListener(onKeyListener);
        mSearchText.setOnKeyListener(onKeyListener);
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyWord = mSearchText.getText().toString();
                if (!keyWord.trim().equals("")) {
                    new Thread(){
                        public void run(){
                            mSearchResults = mYouTubeSearcher.search(keyWord);
                            if (mSearchResults != null) {
                                mMatrixView.clearItemList();
                                if (mItemList == null) {
                                    mItemList = new ArrayList<>();
                                }
                                mItemList.clear();
                                for (VideoBean bean : mSearchResults) {
                                    IconItem iconItem = IconItem.mediaBean2IconItem(bean.getThumbnailURL());
                                    iconItem.setName(bean.getTitle());
                                    iconItem.setSize(mUnitWidth, mUnitHeight);
                                    iconItem.setNameBgColor(Color.TRANSPARENT);
                                    mItemList.add(iconItem);
                                }
                                mMatrixView.addItemList(mItemList);
                                mHandler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        updatePageIndex();
                                    }
                                });
                            }
                        }
                    }.start();
                }
            }
        });
    }

    private void searchOnYoutube(final String keywords) {
        new Thread(){
            public void run(){
                mSearchResults = mYouTubeSearcher.search(keywords);
                if (mSearchResults != null) {
                    mHandler.post(new Runnable(){
                        public void run(){
                            updateVideosFound();
                        }
                    });
                }
            }
        }.start();
    }

    private void updateVideosFound() {

        if (mItemList == null) {
            mItemList = new ArrayList<>();
        }
        if (mSearchResults != null) {
            for (VideoBean bean : mSearchResults) {
                IconItem iconItem = IconItem.mediaBean2IconItem(bean.getThumbnailURL());
                iconItem.setName(bean.getTitle());
                iconItem.setSize(mUnitWidth, mUnitHeight);
                iconItem.setNameBgColor(Color.TRANSPARENT);
                mItemList.add(iconItem);
            }
        }
        mMatrixView.addItemList(mItemList);
        updatePageIndex();
    }

    private void updatePageIndex() {
        if (mPageTextView != null) {
            String pageIndex = (mMatrixView.getFocusIndex() + 1) + "/" + mMatrixView.getItemSize();
            mPageTextView.setText(pageIndex);
        }
    }

    private void getNextPage() {
        isLoadingMore = true;
        new Thread(){
            public void run(){
                mNextPage = mYouTubeSearcher.getNextPage();
                if (mNextPage != null) {
//                    mSearchResults.addAll(mNextPage);
                    for (VideoBean bean : mNextPage) {
                        mSearchResults.add(bean);
                        IconItem iconItem = IconItem.mediaBean2IconItem(bean.getThumbnailURL());
                        iconItem.setName(bean.getTitle());
                        iconItem.setSize(mUnitWidth, mUnitHeight);
                        iconItem.setNameBgColor(Color.TRANSPARENT);
                        mMatrixView.addItem(iconItem);
                    }
                    mHandler.post(new Runnable(){
                        public void run(){
                            updatePageIndex();
                            isLoadingMore = false;
                        }
                    });
                }
            }
        }.start();
    }

    private static class VideoWallHandler extends Handler{

        private static final int DELAY_SELECT_ITEM = 100;
        private Activity mMainActivity;

        public VideoWallHandler(Activity activity) {
            mMainActivity = activity;
        }
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case DELAY_SELECT_ITEM:
                    break;
                default:
                    break;
            }
        }

    }

    private void initMatrixView() {
        if(mVideoWall != null) {
            mVideoWall.removeAllViews();
            mMatrixView = new MatrixView(this);
            mVideoWall.addView(mMatrixView);
            mMatrixView.setFocusable(true);
        }

        //设置MatrixView的缺省图片和焦点
        if (mDefaultVodBitmap == null)
            mDefaultVodBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.loading_thumbnail);
        if (mFocusBitmap == null)
            mFocusBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.item_focus);

        if(mFocusBitmap != null && !mFocusBitmap.isRecycled())
            mMatrixView.setFocusBitmap(mFocusBitmap);
        if(mDefaultVodBitmap != null && !mDefaultVodBitmap.isRecycled())
            mMatrixView.setDefaultBitmap(mDefaultVodBitmap);

        mUnitWidth = (int) (IconItem.PicWidth * 1.1f);
        mUnitHeight = (int) (IconItem.PicHeight * 1.1f + IconItem.TxtHeight);
        mMatrixView.setUnitSize(mUnitWidth, mUnitHeight);
        mMatrixView.setTag(TAG_MATRIX);
        mMatrixView.clearItemList();
        mMatrixView.setFocusStatus(false);
        mMatrixView.setViewColNum(MAX_COLUMN);
        mMatrixView.setViewRowNum(MAX_ROW);
        mMatrixView.setOnTouchListener(mTouchListener);
        mMatrixView.setPause(false);
        mMatrixView.setOnGenericMotionListener(mOnGenericMotionListener);
        mMatrixView.getRender().setOnItemSelectedListener(new OnItemSelectedListener() {
            @Override
            public void onItemSelected(int index) {
                mHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        updatePageIndex();
                    }
                });
                if (index >= mMatrixView.getItemSize() - MAX_COLUMN && !isLoadingMore) {
                    getNextPage();
                }
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        super.onKeyDown(keyCode, event);
        Log.d(TAG, "onKeyDown: " + getFocusIndex());
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            switch (keyCode) {
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    if (hasFocus()) {
                        if (mMatrixView.getFocusIndex() > -1 &&
                                mMatrixView.getFocusIndex() < mSearchResults.size()) {
                            String videoId = mSearchResults.get(mMatrixView.getFocusIndex()).getId();
                            Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
                            intent.putExtra("VIDEO_ID", videoId);
                            intent.putExtra("VIDEO_TITTLE",mSearchResults.
                                    get(mMatrixView.getFocusIndex()).getTitle());
                            startActivity(intent);
                        }
                    }
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    finish();
                    break;
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (hasFocus()) {
                        if (getFocusIndex() < MAX_COLUMN) {
                            Log.d(TAG, "onKeyDown: clearFocus");
                            mMatrixView.setFocusable(false);
                            clearFocus();
                            mSearchText.requestFocus();
                            return true;
                        }
                        onkey(keyCode);
                        return true;
                    }
                case KeyEvent.KEYCODE_DPAD_RIGHT:
                    if (hasFocus()) {
                        onkey(keyCode);
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_LEFT:
                    if (hasFocus()) {
                        onkey(keyCode);
                        return true;
                    }
                    break;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (hasFocus()) {
                        onkey(keyCode);
                        return true;
                    }
                    break;
            }
        }
        return false;
    }

    private View.OnTouchListener mTouchListener = new View.OnTouchListener() {

        @Override
        public boolean onTouch(View arg0, MotionEvent arg1) {
            int tag = (Integer)arg0.getTag();
            switch (tag){
                case TAG_MATRIX:
                    if (mMatrixView.onTouch(arg1)) {
                        int pos = mMatrixView.getFocusIndex();
                        Message msg = new Message();
                        msg.what = MATRIX_ITEM_CLICK;
                        msg.arg1 = pos;
//                        mHandler.sendMessage(msg);
                        return true;
                    }
                    break;
                default:
                    break;
            }

            return false;
        }
    };

    private View.OnGenericMotionListener mOnGenericMotionListener = new View.OnGenericMotionListener() {
        @Override
        public boolean onGenericMotion(View v, MotionEvent event) {
            if(event.getAction() == MotionEvent.ACTION_SCROLL){
                if(scrollTime == 0 || scrollTime + 500 < System.currentTimeMillis()){
                    scrollTime = System.currentTimeMillis();
                    if(event.getAxisValue(MotionEvent.AXIS_VSCROLL) < 0.0f){
                        int pos = mMatrixView.getFocusIndex();
                        if(pos<mMatrixView.getItemSize()-MAX_COLUMN) {
                            pos=pos+MAX_COLUMN;
                        }
                        mMatrixView.setFocusIndex(pos);
                        mMatrixView.setFocusStatus(true);
                    }
                    else{
                        int pos = mMatrixView.getFocusIndex();
                        if(pos>MAX_COLUMN){
                            pos=pos-MAX_COLUMN;
                        }
                        mMatrixView.setFocusIndex(pos);
                        mMatrixView.setFocusStatus(true);
                    }
                }
            }
            else {
                switch (event.getButtonState()){
                    case MotionEvent.BUTTON_BACK:
                    case MotionEvent.BUTTON_SECONDARY:
                        return true;
                    case MotionEvent.BUTTON_PRIMARY:
                        if (null != mMatrixView && mMatrixView.getItemSize() > 0) {
//                            mHandler.sendEmptyMessage(FOCUS_MATRIX_VIEW);
                        }
                        return false;
                    case MotionEvent.BUTTON_TERTIARY:
                        return true;
                    default:
                        break;
                }
            }
            return false;
        }
    };

    public void clearItemList() {
        if (mMatrixView != null) {
            mMatrixView.clearItemList();
        }
    }

    public void refresh() {

    }

    private void test() {
        ArrayList<IconItem> iconItemList = new ArrayList<IconItem>();
        for (int i = 0; i < 20; i ++) {
//            IconItem item = IconItem.mediaBean2IconItem(bean, true);
            IconItem item = new IconItem();
            item.setName(i + "asdasdasdasdasdasdasdasds");
            item.setSize(mUnitWidth, mUnitHeight);
            item.setNameBgColor(Color.TRANSPARENT);

            iconItemList.add(item);
        }
        if (mMatrixView != null){
            mMatrixView.addItemList(iconItemList);
        }
    }

    public void onkey(int keyCode) {
        switch (keyCode) {
            case KeyEvent.KEYCODE_DPAD_CENTER:
            case KeyEvent.KEYCODE_ENTER:
                mMatrixView.onKey(keyCode);
                break;
            default:
                mMatrixView.onKey(keyCode);
                break;
        }
    }

    /**VodMatrixFragment中只有mMatrixView可以获得焦点，判断其是否获得焦点*/
    public boolean hasFocus() {
        return mMatrixView.hasFocus();
    }

    /**是否可以获得焦点*/
    public void setFocusable(boolean able) {
        mMatrixView.setFocusable(able);
    }

    /**请求焦点*/
    public void requestFocus() {
        mMatrixView.setFocusStatus(true);
        if (!mMatrixView.hasFocus()) {
            mMatrixView.requestFocus();
        }
    }

    /**失去焦点*/
    public void clearFocus() {
        if (mMatrixView != null) {
            mMatrixView.clearFocus();
            mMatrixView.setFocusStatus(false);

        }
    }

    public int getFocusIndex() {
        if (mMatrixView != null && mMatrixView.hasFocus()) {
            return mMatrixView.getFocusIndex();
        } else {
            return -1;
        }
    }

    private void initImageLoader() {
        // 创建配置ImageLoader(所有的选项都是可选的,只使用那些你真的想定制)，这个可以设定在APPLACATION里面，设置为全局的配置参数
        ImageLoaderConfiguration config = new ImageLoaderConfiguration.Builder(getApplicationContext())
                .memoryCache(new WeakMemoryCache())
                .memoryCacheExtraOptions(800, 480) // max width, max height，即保存的每个缓存文件的最大长宽
                // .discCacheExtraOptions(480, 800, CompressFormat.JPEG, 75, null) 设置缓存的详细信息，会让imageLoader慢下来，最好不要设置这个
                .threadPoolSize(3)// 线程池内加载的数量
                .threadPriority(Thread.NORM_PRIORITY - 2)
                .denyCacheImageMultipleSizesInMemory()
                // .memoryCache(new UsingFreqLimitedMemoryCache(2 * 1024 * 1024))implementation你可以通过自己的内存缓存实现
                // .memoryCacheSize(2 * 1024 * 1024)
                // .discCacheSize(50 * 1024 * 1024)
                .diskCacheFileNameGenerator(new Md5FileNameGenerator())// 将保存的时候的URI名称用MD5
                // .discCacheFileNameGenerator(new HashCodeFileNameGenerator())//将保存的时候的URI名称用HASHCODE加密
                .tasksProcessingOrder(QueueProcessingType.FIFO)
                .diskCacheFileCount(100) //缓存的File数量
                .diskCache(new UnlimitedDiskCache(new File("")))// 自定义缓存路径
                // .defaultDisplayImageOptions(DisplayImageOptions.createSimple())
                // .imageDownloader(new BaseImageDownloader(context, 5 * 1000, 30 * 1000)) // connectTimeout (5 s), readTimeout (30 s)超时时间
                .writeDebugLogs() // Remove for release app
                .build();

        // Initialize ImageLoader with configuration.
        ImageLoader.getInstance().init(config);// 全局初始化此配置

//        mDisplayOptions = new DisplayImageOptions.Builder()
//                .cacheInMemory(true) // 设置下载的图片是否缓存在内存中
//                .cacheOnDisk(true)
//                .showImageOnFail(R.drawable.no_thumbnail)
//                .showImageForEmptyUri(R.drawable.no_thumbnail)
//                .showImageOnLoading(R.drawable.loading_thumbnail)
//                .build();
    }
}
