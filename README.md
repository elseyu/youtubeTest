# SunniwellYouTube说明
## 1. 项目简介
-   使用谷歌提供的YouTubeDataApi,YouTubePlayerApi组合而成的一个小应用,前者提供了获取数据的接口,后者提供了播放接口.
-   显示逻辑用了两个方案:
    >   - 图片显示用了Gridview,用了一个MainUpView的控件,可以实现焦点平滑移动
    >   - 用上了MatrixView
-   由于api支持的最低版本是4.4.4,在系统版本比较低的盒子运行会出现一些问题,包括:
    >   - 如果用自定义的播放控制组件(包括播放,暂停按钮,时间,进度条等),会导致在播放的时候加载框不会消失 
    >   - 如果使用简单模式,可以隐藏自带的进度条,但是又会导致暂停按钮不消失
    >   - 最后使用的是默认模式,只不过加了一点处理

## 2. 工程目录结构
    --activity
        -MainActivity.java
        -MatrixViewActyvity.java
        -PlayerViewActyvity.java
        -YouTubeFailureRecoveryActyvity.java
    --bean
        -VideoBean.java
    --util
        -Utils.java
    --widget
        --bridge
            -BaseEffectBridge.java
            -BaseEffectBridgeWrapper.java
            -EffectNoDrawBridge.java
            -OpenEffectBridge.java
        --matrix
            -IconItem.java
            -MatrixRenderer.java
            -MatrixView.java
            -MatrixViewGesture.java
            -OnItemSelectedListener.java
        --view
            -MainUpView.java
    --youtube
        -YoutubeConnector.java
        
-   MainActivity是一开始的程序入口,显示用了Gridview + MainUpView,现在程序入口是MatrixViewActyvity; PlayerViewActyvity是播放器的页面,继承自YouTubeFailureRecoveryActyvity,具有失败恢复功能(其实没什么luan用)
-   bean目录下的VideoBean就是存放video信息,只获取了:videoId,tittle,thumbnailUrl,description
-   widget包下是一些自定义控件,比如matrix包下是MatrixView相关的控件,bridge包是和MainUpView相关的,用到了桥接模式
-   youtube包下是与YouTubeDataApi通信相关的接口,用来获取数据

## 3.关于MatrixView
- 首先在IconItem.java修改了item的长宽,还有bitmap的存取逻辑,重要方法是getBitmap(),loadBitMap().里面用到了图片缓存,我直接用了ImageLoader的缓存:
- 
        ImageLoader.getInstance().getMemoryCache().get(mPicUrl);

- MatrixView在使用的时候需要进行一些初始化工作,初始化逻辑在MatrixViewActyvity.java的initMatrixView()里面:

        private void initMatrixView() {
        
            ...
            
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
>   - 我为MatrixView新增了一个接口: setOnItemSelectedListener(),主要用于更新页码的逻辑,由于MatrixView的绘制是在多线程环境下的,所以更行UI要用到Handler.
>   - 还需要注意的是,如果不设置监听,在快速移动的过程中获取MatrixView.getItemIndex()的时候,可能会导致数据不准确

-   点击事件的逻辑我是写在Activty的onKeyDown()方法里面
-   其它没什么了

## 4.优化
-   其实可以用rxJava来使代码更简洁
-   在IconItem.java其实可以不用ImageLoader的缓存,自己做也行,实现起来也不复杂,这样的优点的不依赖任何框架,缺点是自己写估计没有框架的缓存策略好
-   MatrixView比较难用,可以考虑下把相关接口抽取出来,只需要set一个Adapter或者其他Listener就可以和其它view一样正常使用...这个我正在思考怎么做.