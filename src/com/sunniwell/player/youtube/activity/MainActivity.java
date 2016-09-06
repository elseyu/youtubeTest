package com.sunniwell.player.youtube.activity;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.Rect;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubeThumbnailLoader;
import com.google.android.youtube.player.YouTubeThumbnailView;
import com.nostra13.universalimageloader.cache.disc.impl.UnlimitedDiskCache;
import com.nostra13.universalimageloader.cache.disc.naming.Md5FileNameGenerator;
import com.nostra13.universalimageloader.cache.memory.impl.WeakMemoryCache;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.core.assist.QueueProcessingType;
import com.squareup.picasso.Picasso;
import com.sunniwell.player.youtube.R;
import com.sunniwell.player.youtube.bean.VideoBean;
import com.sunniwell.player.youtube.widget.bridge.EffectNoDrawBridge;
import com.sunniwell.player.youtube.widget.bridge.OpenEffectBridge;
import com.sunniwell.player.youtube.widget.view.MainUpView;
import com.sunniwell.player.youtube.youtube.YoutubeConnector;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import rx.Scheduler;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private static final int RECOVERY_DIALOG_REQUEST = 1;
    private static final double THUMBNAIL_ASPECT_RATIO = 9 / 16d;
    private static final int VIDEO_WALL_COLUMN_NUM = 4;

    private GridView mGridVideoWall = null;
    private Dialog mErrorDialog = null;
    private List<VideoBean> mSearchResults;
    private View mLastSelectView = null;
    private TextView mPageTextView = null;
    private YoutubeConnector mYouTubeSearcher = null;
    private MainUpView mMainView = null;

    private List<VideoBean> mNextPage;

    private ThumbnailListener mThumbnailListener = null;

    private Map<YouTubeThumbnailView, YouTubeThumbnailLoader> mThumbnailViewToLoaderMap;

    private Handler mHandler;
    private int mCurSelection = 1;
    private DisplayImageOptions mDisplayOptions;
    private boolean isLoadingMore = false;
    private Button mSearchButton;
    private EditText mSearchText;

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        initImageLoader();
        setContentView(R.layout.activity_main);
        mHandler = new VideoWallHandler(this);
        mThumbnailListener = new ThumbnailListener();
        mThumbnailViewToLoaderMap = new HashMap<YouTubeThumbnailView, YouTubeThumbnailLoader>();
        mGridVideoWall = (GridView) findViewById(R.id.video_wall);
        mPageTextView = (TextView) findViewById(R.id.page_index);
        mSearchText = (EditText) findViewById(R.id.search_text);
        mSearchButton = (Button) findViewById(R.id.search_button);
        mSearchText.requestFocus();
        initSearch();
        initMainUpView();
        mYouTubeSearcher = new YoutubeConnector(MainActivity.this);
        searchOnYoutube("");
    }

    private void initSearch() {
        View.OnKeyListener onKeyListener = new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    switch (keyCode) {
                        case KeyEvent.KEYCODE_DPAD_DOWN:
                            if (mGridVideoWall.getCount() > 2) {
                                mGridVideoWall.requestFocus();
                                mGridVideoWall.setSelection(mCurSelection);
                            }
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
        mGridVideoWall.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (!hasFocus) {
                    mMainView.setVisibility(View.INVISIBLE);
                } else {
                    mMainView.setVisibility(View.VISIBLE);
                    mMainView.setFocusView(mLastSelectView,1.0f);
                }
            }
        });
        mSearchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String keyWord = mSearchText.getText().toString();
                if (!keyWord.trim().equals("")) {
                    new Thread(){
                        public void run(){
                            mSearchResults = mYouTubeSearcher.search(keyWord);
                            if (mSearchResults != null) {
                                mHandler.post(new Runnable(){
                                    public void run(){
                                        mAdapter.notifyDataSetChanged();
                                    }
                                });
                            }
                        }
                    }.start();
                }
            }
        });
    }

    private void initMainUpView() {
        mMainView = (MainUpView) findViewById(R.id.main_up_view);
        EffectNoDrawBridge bridge = new EffectNoDrawBridge();
        bridge.setTranDurAnimTime(200);
        mMainView.setEffectBridge(bridge);
//        mMainView.setUpRectResource(R.drawable.test_rectangle);
        mMainView.setUpRectResource(R.drawable.item_focus);
//        mMainView.setShadowResource(R.drawable.item_shadow);
        mMainView.setDrawUpRectPadding(new Rect(10, 10, 10, 10));
    }

    private void searchOnYoutube(final String keywords){
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

    private void updateVideosFound(){
        mGridVideoWall.setAdapter(mAdapter);
        mGridVideoWall.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String videoId = mSearchResults.get(position).getId();
                Intent intent = new Intent(getApplicationContext(), PlayerActivity.class);
                intent.putExtra("VIDEO_ID", videoId);
                intent.putExtra("VIDEO_TITTLE",mSearchResults.get(position).getTitle());
                startActivity(intent);
            }
        });
        mGridVideoWall.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                mCurSelection = position;
                updatePageIndex();
                if ((mSearchResults.size() - 1 - position) < VIDEO_WALL_COLUMN_NUM) {
                    if (!isLoadingMore) {
                        getNextPage();
                    }
                    isLoadingMore = true;
                }
                if (mLastSelectView != null) {
                    TextView textView = (TextView) mLastSelectView.findViewById(R.id.tittle_text);
                    textView.setEllipsize(TextUtils.TruncateAt.END);

                }
                if (view != null) {
                    TextView textView = (TextView) view.findViewById(R.id.tittle_text);
                    textView.setEllipsize(TextUtils.TruncateAt.MARQUEE);
                    mMainView.setFocusView(view,mLastSelectView,1.0f);
                    mLastSelectView = view;
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        mHandler.sendEmptyMessageDelayed(VideoWallHandler.DELAY_SELECT_ITEM,200);
        updatePageIndex();
    }

    private void updatePageIndex() {
        if (mPageTextView != null) {
            String pageIndex = (mCurSelection + 1) + "/" + mSearchResults.size();
            mPageTextView.setText(pageIndex);
        }
    }

    private void getNextPage() {
        new Thread(){
            public void run(){
                mNextPage = mYouTubeSearcher.getNextPage();
                if (mNextPage != null) {
                    mSearchResults.addAll(mNextPage);
                    mHandler.post(new Runnable(){
                        public void run(){
                            mAdapter.notifyDataSetChanged();
                            updatePageIndex();
                            isLoadingMore = false;
                        }
                    });
                }
            }
        }.start();
    }

    private BaseAdapter mAdapter = new BaseAdapter() {

        LinearLayout.LayoutParams layoutParams =  null;

        @Override
        public int getCount() {
            return mSearchResults.size();
        }

        @Override
        public Object getItem(int position) {
            return mSearchResults.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (layoutParams == null) {
                layoutParams = new LinearLayout.LayoutParams(mGridVideoWall.getColumnWidth(), (int) (mGridVideoWall.getColumnWidth() * THUMBNAIL_ASPECT_RATIO));
            }
            VideoBean searchResult = mSearchResults.get(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getApplicationContext()).inflate(R.layout.item_video,parent,false);
                YouTubeThumbnailView thumbnailView = (YouTubeThumbnailView) convertView.findViewById(R.id.thumbnail_image);
                thumbnailView.setTag(searchResult.getId());
                thumbnailView.initialize(YoutubeConnector.KEY,mThumbnailListener);
                thumbnailView.setLayoutParams(layoutParams);
//                ImageLoader.getInstance().displayImage(searchResult.getThumbnailURL(),thumbnailView,mDisplayOptions);
            } else {
                YouTubeThumbnailView thumbnailView = (YouTubeThumbnailView) convertView.findViewById(R.id.thumbnail_image);
                YouTubeThumbnailLoader loader = mThumbnailViewToLoaderMap.get(thumbnailView);
                if (loader == null) {
                    thumbnailView.setTag(searchResult.getId());
                } else {
                    thumbnailView.setImageResource(R.drawable.loading_thumbnail);
                    if (searchResult.getId() != null || !searchResult.getId().equals("")) {
                        loader.setVideo(searchResult.getId());
                    }
                }
//                ImageLoader.getInstance().displayImage(searchResult.getThumbnailURL(),thumbnailView,mDisplayOptions);
            }
            TextView textView = (TextView) convertView.findViewById(R.id.tittle_text);
            textView.setText(searchResult.getTitle());
            return convertView;
        }
    };

    private class ThumbnailListener implements YouTubeThumbnailView.OnInitializedListener,
            YouTubeThumbnailLoader.OnThumbnailLoadedListener {

        @Override
        public void onInitializationSuccess(YouTubeThumbnailView youTubeThumbnailView,
                                            YouTubeThumbnailLoader youTubeThumbnailLoader) {
            youTubeThumbnailLoader.setOnThumbnailLoadedListener(this);
            mThumbnailViewToLoaderMap.put(youTubeThumbnailView,youTubeThumbnailLoader);
            youTubeThumbnailView.setImageResource(R.drawable.loading_thumbnail);
            youTubeThumbnailLoader.setVideo((String) youTubeThumbnailView.getTag());
        }

        @Override
        public void onInitializationFailure(YouTubeThumbnailView youTubeThumbnailView,
                                            YouTubeInitializationResult errorReason) {
            youTubeThumbnailView.setImageResource(R.drawable.no_thumbnail);
            if (errorReason.isUserRecoverableError()) {
                if (mErrorDialog == null || !mErrorDialog.isShowing()) {
                    mErrorDialog = errorReason.getErrorDialog(MainActivity.this, RECOVERY_DIALOG_REQUEST);
                    mErrorDialog.show();
                }
            } else {
                String errorMessage = String.format(getString(R.string.error_player), errorReason.toString());
                Toast.makeText(getApplicationContext(), errorMessage, Toast.LENGTH_LONG).show();
            }
        }

        @Override
        public void onThumbnailLoaded(YouTubeThumbnailView youTubeThumbnailView, String s) {

        }

        @Override
        public void onThumbnailError(YouTubeThumbnailView youTubeThumbnailView,
                                     YouTubeThumbnailLoader.ErrorReason errorReason) {
            youTubeThumbnailView.setImageResource(R.drawable.no_thumbnail);
        }

    }
    private static class VideoWallHandler extends Handler{

        private static final int DELAY_SELECT_ITEM = 100;
        private MainActivity mMainActivity;

        public VideoWallHandler(MainActivity activity) {
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

        mDisplayOptions = new DisplayImageOptions.Builder()
                .cacheInMemory(true) // 设置下载的图片是否缓存在内存中
                .cacheOnDisk(true)
                .showImageOnFail(R.drawable.no_thumbnail)
                .showImageForEmptyUri(R.drawable.no_thumbnail)
                .showImageOnLoading(R.drawable.loading_thumbnail)
                .build();
    }
}
