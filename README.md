# SunniwellYouTube说明
## 1. 项目简介
-   使用谷歌提供的YouTubeDataApi,YouTubePlayerApi组合而成的一个小应用,前者提供了获取数据的接口,后者提供了播放接口.
-   显示逻辑用了两个方案:
    >   - 图片显示用了Gridview,用了一个MainUpView的控件,可以实现焦点平滑移动
    >   - 用上了MatrixView //已删除
-   由于api支持的最低版本是4.4.4,在系统版本比较低的盒子运行会出现一些问题,包括:
    >   - 如果用自定义的播放控制组件(包括播放,暂停按钮,时间,进度条等),会导致在播放的时候加载框不会消失 
    >   - 如果使用简单模式,可以隐藏自带的进度条,但是又会导致暂停按钮不消失
    >   - 最后使用的是默认模式,只不过加了一点处理
    >   - 由于要求支持盒子的版本4.4,所以没办法完全定制播放器.后续打算放弃4.4的版本,完全定制播放器

## 2. 工程目录结构
    --activity
        -MainActivity.java
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
        --view
            -MainUpView.java
    --youtube
        -YoutubeConnector.java
        
-   MainActivity是一开始的程序入口,显示用了Gridview + MainUpView; PlayerViewActyvity是播放器的页面,继承自YouTubeFailureRecoveryActyvity,具有失败恢复功能(其实没什么luan用)
-   bean目录下的VideoBean就是存放video信息,只获取了:videoId,tittle,thumbnailUrl,description
-   widget包下是一些自定义控件,bridge包是和MainUpView相关的,用到了桥接模式
-   youtube包下是与YouTubeDataApi通信相关的接口,用来获取数据

## 3.如何使用
- 此工程一开始用eclipse构建,但是开发是在Android Studio上进行的,可用AS直接打开工程,Eclipse未测试
- 打开后直接运行即可
- 注意,由于国内的网络环境,需要连接翻墙的WiFi才能正常访问
- 新增注意:需要先安装YouTube客户端

## 4.效果展示
- 默认
    ![image](https://github.com/elseyu/youtubeTest/raw/master/images/095344.png)
- 搜索
    ![image](https://github.com/elseyu/youtubeTest/raw/master/images/095529.png)
    ![image](https://github.com/elseyu/youtubeTest/raw/master/images/095554.png)

- 播放
    ![image](https://github.com/elseyu/youtubeTest/raw/master/images/100642.png)
    ![image](https://github.com/elseyu/youtubeTest/raw/master/images/100708.png)
## 5.优化
-   其实可以用rxJava来使代码更简洁
-   在IconItem.java其实可以不用ImageLoader的缓存,自己做也行,实现起来也不复杂,这样的优点的不依赖任何框架,缺点是自己写估计没有框架的缓存策略好
-   MatrixView比较难用,可以考虑下把相关接口抽取出来,只需要set一个Adapter或者其他Listener就可以和其它view一样正常使用...这个我正在思考怎么做.