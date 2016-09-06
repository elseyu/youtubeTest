package com.sunniwell.player.youtube.widget.matrix;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.opengl.GLUtils;
import android.view.View;

import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import javax.microedition.khronos.opengles.GL10;

public class IconItem {
    public static final int PIC_STATE_NULL = 0;//no any bitmap loaded

    public static final int PIC_STRECH_FULL = 0;// strech to full size
    public static final int PIC_KEEP_ASPECT = 1;// strech to same aspect
    public static final int PIC_KEEP_SIZE = 2;// keep original size
    public static final int PIC_AS_ICON = 3;
    public static final int PIC_AS_WOOD = 4;
    public static final int PIC_STRECH_PIC_PART = 5;
    public static final int PIC_AS_STRECH_ICON = 6;
    public static final int PIC_AS_FIX_ICON = 7;

    public static  int PicWidth = 240;
    public static  int PicHeight = 135;
    public static  int TxtHeight = 26;

    static private float mRad[] = {8, 8, 8, 8, 8, 8, 8, 8};

    private int mPicMode = PIC_STRECH_FULL;
    private int mWidth = 0;
    private int mHeight = 0;
    private int mTxtHeight = 0;
    private Rect mInserts = new Rect(0, 0, 0, 0);
    private String mPicUrl = null;
    private String mName = null;
    private String mTag = null;
    private int mBgColor = Color.BLACK;
    private int mTagColor = Color.BLACK;
    private int mTagBgColor = Color.GREEN;
    private int mNameColor = Color.WHITE;
    private int mNameBgColor = Color.argb(127, 0, 0, 0);//0B1746 //Color.argb(255,64,64,128)
    private boolean mIsShowTag = true;
    private boolean mIsShowName = true;
    private boolean mManualLastShowNameStatus = false;
    private int mFrameWidth = 1;
    private int mFrameColor = Color.GRAY;
    private Runnable mTask = null;//BitmapManager后台下载和解码图片的任务
    private Bitmap mBitmap = null;//外部所设Bitmap
    private Object mExtParam;//扩展参数
    private static Bitmap mTagBitmapHot, mTagBitmapRecommend, mTagBitmapNewest;

    private boolean mNeedScroll = false;
    private int mScrollX = 0;
    private long mLastMs = 0;
    private int mPicState = PIC_STATE_NULL;//图片载入状态
    private boolean mPicLoaded = false;//图片是否已经载入到纹理中

    public boolean ismPicLoaded() {
        return mPicLoaded;
    }

    public void setmPicLoaded(boolean mPicLoaded) {
        this.mPicLoaded = mPicLoaded;
    }

    private int mTextureId = 0;
    private ByteBuffer mIndexBuffer;
    private FloatBuffer mVertexBuffer;
    private FloatBuffer mTextureBuffer;

    private float mVertices[] = {
            // Vertices according to faces
            -0.5f, 0.5f, -0.5f, // v2 hl
            -0.5f, -0.5f, -0.5f, // Vertex 0 bl
            0.5f, 0.5f, -0.5f, // v3 hr
            0.5f, -0.5f, -0.5f, // v1 br
    };

    private float mTexture[] = {
            // Mapping coordinates for the mVertices
            0.0f, 0.0f, 0.0f, 1.0f, 1.0f, 0.0f, 1.0f, 1.0f,};

    private byte mIndices[] = {
            // Faces definition
            0, 1, 3, 0, 3, 2, // Face front
            // 0,2,3,0,3,1
    };
    private Bitmap mBackgroud;
    private int mBackgroudColor = 0x0;

    public IconItem() {
        mTextureId = 0;
        mNeedScroll = false;
        mScrollX = 0;
        mLastMs = 0;
        mWidth = PicWidth;
        mHeight = PicHeight;
        mTxtHeight = TxtHeight;
        /*mTxtHeight = LayoutUtil.getHeight(mTxtHeight);*/
    }

    public void setBackgroudColor(int color) {
        mBackgroudColor = color;
    }

    public void setSize(int width, int height) {
    	/*width = LayoutUtil.getWidth(width);
    	height = LayoutUtil.getHeight(height);*/
    	
        int i;
        float vertices[] = {
                // Vertices according to faces
                -0.5f, 0.5f, -0.5f, // v2 hl
                -0.5f, -0.5f, -0.5f, // Vertex 0 bl
                0.5f, 0.5f, -0.5f, // v3 hr
                0.5f, -0.5f, -0.5f, // v1 br
        };

        for (i = 0; i < 12; i += 3) {
            mVertices[i] = vertices[i] * width;
            mVertices[i + 1] = vertices[i + 1] * height;
            mVertices[i + 2] = -1.1f;// 100;//*= width;
        }

        ByteBuffer byteBuf = ByteBuffer.allocateDirect(mVertices.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        mVertexBuffer = byteBuf.asFloatBuffer();
        mVertexBuffer.put(mVertices);
        mVertexBuffer.position(0);

        byteBuf = ByteBuffer.allocateDirect(mTexture.length * 4);
        byteBuf.order(ByteOrder.nativeOrder());
        mTextureBuffer = byteBuf.asFloatBuffer();
        mTextureBuffer.put(mTexture);
        mTextureBuffer.position(0);

        mIndexBuffer = ByteBuffer.allocateDirect(mIndices.length);
        mIndexBuffer.put(mIndices);
        mIndexBuffer.position(0);
        mWidth = width;
        mHeight = height;
    }

    public int getWidth() {
        return mWidth;
    }

    public int getHeight() {
        return mHeight;
    }

    public int getPicWidth() {
        return mWidth;
    }

    public int getPicHeight() {
        if (mIsShowName) {
            return mHeight - mTxtHeight;
        } else {
            return mHeight;
        }
    }

    public void setInsert(int left, int top, int right, int bottom) {
        mInserts.left = left;
        mInserts.top = top;
        mInserts.right = right;
        mInserts.bottom = bottom;
    }

    public Rect getInsert() {
        return mInserts;
    }

    public void setBackgroud(Bitmap bm) {
        mBackgroud = bm;
    }

    public boolean isNeedScroll() {
        return mNeedScroll;
    }

    public void setPicState(int picState) {
        mPicState = picState;
    }

    public int getPicState() {
        return mPicState;
    }

    public void setScrollPos(long now) {
        long dura;
        if (now <= 0) {
            mLastMs = 0;
            mScrollX = 0;
        } else if (mLastMs <= 0) {
            mLastMs = now;
            mScrollX = 0;
        } else {
            dura = now - mLastMs;
            if (30 < dura)
                dura = 30;
            mScrollX += dura / 12;
            mLastMs = now;
        }
    }

    public int getScrollPos() {
        return mScrollX;
    }

    public int getTextId() {
        return mTextureId;
    }

    public void setPicMode(int picMode) {
        mPicMode = picMode;
    }

    public int getPicMode() {
        return mPicMode;
    }

    public String getName() {
        return mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getTag() {
        return mTag;
    }

    public void setTag(String tag) {
        this.mTag = tag;
    }

    public String getPicUrl() {
        return mPicUrl;
    }

    public void setPicUrl(String picUrl) {
        this.mPicUrl = picUrl;
    }

    public int getBgColor() {
        return mBgColor;
    }

    public void setBgColor(int bgColor) {
        this.mBgColor = bgColor;
    }

    public int getTagColor() {
        return mTagColor;
    }

    public void setTagColor(int tagColor) {
        this.mTagColor = tagColor;
    }

    public int getTagBgColor() {
        return mTagBgColor;
    }

    public void setTagBgColor(int tagBgColor) {
        this.mTagBgColor = tagBgColor;
    }

    public int getNameColor() {
        return mNameColor;
    }

    public void setNameColor(int nameColor) {
        this.mNameColor = nameColor;
    }

    public int getNameBgColor() {
        return mNameBgColor;
    }

    public void setNameBgColor(int nameBgColor) {
        this.mNameBgColor = nameBgColor;
    }

    public boolean mIsShowTag() {
        return mIsShowTag;
    }

    public void setShowTag(boolean isShowTag) {
        this.mIsShowTag = isShowTag;
    }

    public boolean mIsShowName() {
        return mIsShowName;
    }

    public void setShowName(boolean isShowName) {
        this.mIsShowName = isShowName;
    }

    public int getFrameWidth() {
        return mFrameWidth;
    }

    public void setFrameWidth(int frameWidth) {
        this.mFrameWidth = frameWidth;
    }

    public int getFrameColor() {
        return mFrameColor;
    }

    public void setFrameColor(int frameColor) {
        this.mFrameColor = frameColor;
    }

    public Object getExtParam() {
        return mExtParam;
    }

    public void setExtParam(Object extParam) {
        this.mExtParam = extParam;
    }

    public void setTextHeight(int h) {
        mTxtHeight = h;
    }

    public int getTextHeight() {
        return mTxtHeight;
    }

    public static void setTagBitmap(Bitmap hot, Bitmap recommend, Bitmap newest) {
        mTagBitmapHot = hot;
        mTagBitmapRecommend = recommend;
        mTagBitmapNewest = newest;
    }

    public void setBitmapTask(Runnable task) {
        this.mTask = task;
    }

    public Runnable getBitmapTask() {
        return mTask;
    }

    public void setBitmap(Bitmap bitmap) {
        this.mBitmap = bitmap;
    }

    public Bitmap getBitmap() {
        Bitmap bmp = mBitmap;//优先使用外部设置的Bitmap

        //外部没有设置BITMAP，使用缓存的BITMAP
        if ((bmp == null || bmp.isRecycled()) && mPicUrl != null)
            bmp = ImageLoader.getInstance().getMemoryCache().get(mPicUrl);
        if (bmp == null || bmp.isRecycled())
            return null;
        return bmp;
    }

    public boolean loadBitmap(long now) {
        int timeout = 3000;
        long mTaskMs = 0;
        Bitmap bmp = null;
        if (mPicUrl == null) {
//			Log.d(TAG,"loadBitmap error.....mgr = "+mgr);
            return false;
        }

        bmp = mBitmap;
        if (bmp == null || bmp.isRecycled())
            bmp = ImageLoader.getInstance().getMemoryCache().get(mPicUrl);

        if (bmp == null || bmp.isRecycled()) {
            ImageLoader.getInstance().loadImage(mPicUrl,new SimpleImageLoadingListener() {
                @Override
                public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
//                    ImageLoader.getInstance().getMemoryCache().put(mPicUrl,loadedImage);
                    mBitmap = loadedImage;
                }
            });
        }
        if (mBitmap == null) return false;
        return true;
    }

    //载入纹理
    public boolean loadTexture(GL10 gl, Bitmap defBmp, Bitmap drwBmp, boolean showName) {
        boolean ret = false;
        Bitmap bmp = mBitmap;
        if (bmp == null || bmp.isRecycled()) {
            bmp = ImageLoader.getInstance().getMemoryCache().get(mPicUrl);
            mPicLoaded = false;
            mBitmap = bmp;
        }


        //真正的背景图来了，必须更新纹理
        if ((bmp != null && !bmp.isRecycled() && !mPicLoaded) || mManualLastShowNameStatus != showName) {
            releaseTexture(gl);
        }

        mManualLastShowNameStatus = showName;
        if (getTextId() <= 0 && drwBmp != null) {
            if (bmp == null || bmp.isRecycled()) {
                generateBitmap(gl, defBmp, drwBmp, showName);
                mPicLoaded = false;
            } else {
                mPicLoaded = generateBitmap(gl, bmp, drwBmp, showName);
//                mPicLoaded = false;
                ret = true;
            }

            if (drwBmp != null && !drwBmp.isRecycled())
                setTexture(gl, drwBmp);
            else
                ret = false;
        }
        return ret;
    }

    public boolean setTexture(GL10 gl, Bitmap bmp) {
        int[] textures = new int[1];

        if (bmp == null || bmp.isRecycled())
            return false;

        gl.glGenTextures(1, textures, 0);
        gl.glBindTexture(GL10.GL_TEXTURE_2D, textures[0]);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER,
                GL10.GL_NEAREST);// GL_LINEAR);//
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER,
                GL10.GL_LINEAR);
//		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
//				GL10.GL_REPEAT);
//		gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
//				GL10.GL_REPEAT);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S,
                GL10.GL_CLAMP_TO_EDGE);
        gl.glTexParameterf(GL10.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T,
                GL10.GL_CLAMP_TO_EDGE);
        GLUtils.texImage2D(GL10.GL_TEXTURE_2D, 0, bmp, 0);

        if (textures[0] <= 0)
            return false;
        mTextureId = textures[0];
        return true;
    }

    public boolean releaseTexture(GL10 gl) {
        boolean ret = false;
        int[] textures = {mTextureId};

        if (mTextureId != 0) {
            gl.glDeleteTextures(textures.length, textures, 0);
            ret = true;
        }
        mTextureId = 0;
        mPicLoaded = false;
        return ret;
    }

    public boolean isTextureValid() {
        return mTextureId != 0;
    }

    //是否需要重画
    public boolean draw(GL10 gl) {

        gl.glBindTexture(GL10.GL_TEXTURE_2D, mTextureId);
        gl.glEnableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glEnableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        gl.glFrontFace(GL10.GL_CCW);
        gl.glVertexPointer(3, GL10.GL_FLOAT, 0, mVertexBuffer);
        gl.glTexCoordPointer(2, GL10.GL_FLOAT, 0, mTextureBuffer);

        gl.glDrawElements(GL10.GL_TRIANGLES, mIndices.length,
                GL10.GL_UNSIGNED_BYTE, mIndexBuffer);

        gl.glDisableClientState(GL10.GL_VERTEX_ARRAY);
        gl.glDisableClientState(GL10.GL_TEXTURE_COORD_ARRAY);

        return (mNeedScroll || !mPicLoaded);
    }


    private boolean generateBitmap(GL10 gl, Bitmap bckBmp, Bitmap dstBmp, boolean showName) {
        boolean ret = true;
        try {
            Canvas canvas = new Canvas(dstBmp);
            Paint paint = new Paint();
            paint.setAntiAlias(true);// 消除锯齿
            paint.setFlags(Paint.ANTI_ALIAS_FLAG);
            PaintFlagsDrawFilter pfd = new PaintFlagsDrawFilter(0, Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
            Rect picRect = new Rect(0, 0, getPicWidth(), getPicHeight());
            Rect dstRect = new Rect(0, 0, dstBmp.getWidth(), dstBmp.getHeight());
            RectF clipRect = new RectF(1, 1, dstBmp.getWidth(),
                    dstBmp.getHeight());
            RectF frmRect = new RectF(1, 1, dstBmp.getWidth(),
                    dstBmp.getHeight());
            Rect strRect = new Rect();
            Rect rect = new Rect();
            Path frmPath = new Path();
            int x, y, w, h;
            y = dstRect.bottom - mTxtHeight;

            dstBmp.eraseColor(Color.TRANSPARENT);
            canvas.save();

            //在系统的裁剪区域基础上，再限制画图区域
            if (mPicMode == PIC_AS_WOOD || mPicMode == PIC_AS_STRECH_ICON) {
                clipRect.set(dstRect);
                frmPath.addRect(clipRect, Path.Direction.CCW);
            } else {
                clipRect.set(dstRect);
                if (mPicMode != PIC_STRECH_PIC_PART) {
                    frmPath.addRoundRect(clipRect, mRad, Path.Direction.CCW);
                } else {
                    frmPath.addRect(clipRect, Path.Direction.CCW);
                }
            }
            canvas.clipPath(frmPath, Region.Op.REPLACE);
            canvas.setDrawFilter(pfd);

            // copy source mBitmap
            if (bckBmp != null) {
//			paint.setDither(true);//设定是否使用图像抖动处理，会使绘制出来的图片颜色更加平滑和饱满，图像更加清晰
//			paint.setShadowLayer(22 ,5,5,Color.RED);
//			//在图形下面设置阴影层，产生阴影效果，radius为阴影的角度，dx和dy为阴影在x轴和y轴上的距离，color为阴影的颜色
                Rect bckRect = new Rect(0, 0, bckBmp.getWidth(), bckBmp.getHeight());
                if (mBackgroud != null) {
                    Rect srcRect = new Rect(0, 0, mBackgroud.getWidth(), mBackgroud.getHeight());
                    Rect dstRect0 = new Rect(0, 0, bckBmp.getWidth(), bckBmp.getHeight());
                    canvas.drawBitmap(mBackgroud, dstRect0, srcRect, paint);
//				canvas.drawARGB(0x30, 207, 211, 209);
                }

                if (mBackgroudColor != 0) {
                    canvas.drawColor(mBackgroudColor);
                }

                switch (mPicMode) {
                    case PIC_KEEP_ASPECT:
                        rect.set(picRect);
                        if (bckRect.width() * picRect.height() < bckRect.height()
                                * picRect.width()) {
                            w = bckRect.width() * picRect.height() / bckRect.height();
                            rect.left = (picRect.left + picRect.right - w) / 2;
                            rect.right = rect.left + w;
                        } else {
                            h = bckRect.height() * picRect.width() / bckRect.width();
                            rect.top = (picRect.top + picRect.bottom - h) / 2;
                            rect.bottom = rect.top + h;
                        }
                        if (mBgColor != Color.TRANSPARENT) {
                            paint.setColor(mBgColor);
                            canvas.drawRect(picRect, paint);
                        }
                        canvas.drawBitmap(bckBmp, bckRect, rect, paint);
                        y = rect.bottom;
                        break;
                    case PIC_KEEP_SIZE:// keep orignal size
                        w = picRect.width() - bckRect.width();
                        h = picRect.height() - bckRect.height();
                        rect.left = picRect.left + w / 2;
                        rect.right = picRect.right - w / 2;
                        rect.top = picRect.top + h / 2;
                        rect.bottom = picRect.bottom - h / 2;
                        if (mBgColor != Color.TRANSPARENT) {
                            paint.setColor(mBgColor);
                            canvas.drawRect(picRect, paint);
                        }
                        canvas.drawBitmap(bckBmp, picRect, rect, paint);
                        y = rect.bottom;
                        break;
                    case PIC_AS_ICON:
                        rect.set(picRect);
                        if (mIsShowName)
                            rect.bottom -= mTxtHeight;
                        w = rect.width() < rect.height() ? rect.width() : rect.height();
                        if (bckRect.width() * 2 < w && bckRect.height() * 2 < w) {
                            w = w / 2;
                            rect.left = rect.centerX() - w / 2;
                            rect.right = rect.left + w;
                            rect.top = rect.centerY() - w / 2;
                            rect.bottom = rect.top + w;
                        } else if (bckRect.width() < w && bckRect.height() < w) {
                            rect.left = rect.centerX() - bckRect.width() / 2;
                            rect.right = rect.left + bckRect.width();
                            rect.top = rect.centerY() - bckRect.height() / 2;
                            rect.bottom = rect.top + bckRect.height();
                        } else {
                            rect.left = rect.centerX() - w / 2;
                            rect.right = rect.left + w;
                            rect.top = rect.centerY() - w / 2;
                            rect.bottom = rect.top + w;
                        }
                        if (mBgColor != Color.TRANSPARENT) {
                            paint.setColor(mBgColor);
                            canvas.drawRect(picRect, paint);
                        }
                        canvas.drawBitmap(bckBmp, bckRect, rect, paint);
                        y = rect.bottom;
                        break;
                    case PIC_AS_FIX_ICON:
                        rect.set(picRect);
                        if (mIsShowName)
                            rect.bottom -= mTxtHeight;
//				w = rect.width() < rect.height() ? rect.width() : rect.height();
//				w = w*2/5;
                        w = 72;
                        rect.left = rect.centerX() - w / 2;
                        rect.right = rect.left + w;
                        rect.top = rect.centerY() - w / 2;
                        rect.bottom = rect.top + w;
                        if (mBgColor != Color.TRANSPARENT) {
                            paint.setColor(mBgColor);
                            canvas.drawRect(picRect, paint);
                        }
                        canvas.drawBitmap(bckBmp, bckRect, rect, paint);
                        break;
                    case PIC_AS_STRECH_ICON:
                        rect.set(picRect);
                        if (mIsShowName)
                            rect.bottom -= TxtHeight;
                        w = rect.width() < rect.height() ? rect.width() : rect.height();
                        rect.left = rect.centerX() - w / 2;
                        rect.right = rect.left + w;
                        rect.top = rect.centerY() - w / 2;
                        rect.bottom = rect.top + w;
                        if (mBgColor != Color.TRANSPARENT) {
                            paint.setColor(mBgColor);
                            canvas.drawRect(picRect, paint);
                        }
                        canvas.drawBitmap(bckBmp, bckRect, rect, paint);
                        y = rect.bottom;
                        break;
                    case PIC_AS_WOOD:
                        canvas.drawBitmap(bckBmp, bckRect, picRect, paint);
                        y = picRect.bottom;
//				Log.i(TAG, "back w:"+bckRect.right+" h:"+bckRect.bottom
//						+" pic w:"+picRect.right+" h:"+picRect.bottom
//						+" dstBmp w:"+dstBmp.getWidth()+" h:"+dstBmp.getHeight());
                        break;
                    case PIC_STRECH_PIC_PART:
                        Rect picRect1 = new Rect(dstRect.left, dstRect.top,
                                dstRect.left + mWidth, dstRect.top + mHeight - mTxtHeight);
                        canvas.drawBitmap(bckBmp, bckRect, picRect1, paint);
                        y = picRect1.bottom;
                        break;
                    default:// strech to full size
                        canvas.drawBitmap(bckBmp, bckRect, picRect, paint);
                        y = picRect.bottom;
                        break;
                }
            }

            // draw name
            if (mIsShowName && showName) {
                if (mNameBgColor != Color.TRANSPARENT) {
                    x = dstRect.left;
//                y = dstRect.bottom - mTxtHeight;
                    paint.setColor(mNameBgColor);
                    canvas.drawRect(x, y, dstRect.right, dstRect.bottom, paint);
                }

                if (mName != null) {
                    /*paint.setTextSize(LayoutUtil.getHeight(24));*/
                    paint.setTextSize(24);
                    paint.getTextBounds(mName, 0, mName.length(), strRect);
                    paint.setColor(mNameColor);

                    if (strRect.width() < dstRect.width()) {
                        x = dstRect.left + (dstRect.width() - strRect.width()) / 2;
                        y = dstRect.bottom - (mTxtHeight - strRect.height()) / 2;
                        mNeedScroll = false;
                    } else {
                        if (strRect.width() < mScrollX)
                            mScrollX = -strRect.width();
                        x = dstRect.left - mScrollX;
                        y = dstRect.bottom - (mTxtHeight - strRect.height()) / 2;
                        mNeedScroll = true;
                    }
                    /*canvas.drawText(mName, 0, mName.length(), x, y - LayoutUtil.getHeight(3), paint);*/
                    canvas.drawText(mName, 0, mName.length(), x, y - 3, paint);
                }
            }

            ret = drawTag(canvas, paint);
            // draw frame
            if (0 < mFrameWidth) {
                // 测试发现:开启裁剪圆角更平滑
                paint.setStyle(Paint.Style.STROKE);
                paint.setColor(mFrameColor);
                frmRect.set(picRect);
                if (mPicMode == PIC_AS_WOOD || mPicMode == PIC_AS_STRECH_ICON) {
                    frmRect.left += 1;
                    frmRect.top += 1;
                    frmRect.right -= 1;
                    paint.setStrokeWidth(3);
                    canvas.drawRect(frmRect, paint);
                } else {
                    paint.setStrokeWidth(2);
                    canvas.drawRoundRect(frmRect, mRad[0], mRad[0], paint);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return ret;
    }

    private boolean drawTag(Canvas canvas, Paint paint) {
        if (mIsShowTag && mTag != null && !mTag.equals("")) {
            /**暂没有引进ThemeManager故注释掉  zoupl*/
//            return ThemeManager.getInstance().drawTag(mTag, canvas, paint);
        }
        return true;
    }

    public static Bitmap createBitmap(int w, int h, int type) {
        Bitmap bitmap = Bitmap.createBitmap(w, h, Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint p = new Paint();
        if (type == 0) {
            p.setStrokeWidth(0);
            p.setColor(Color.GRAY);
        } else {
            p.setStrokeWidth(0);
            p.setColor(Color.YELLOW);
        }
        p.setStyle(Paint.Style.STROKE);

        Rect rect0 = canvas.getClipBounds();
        RectF outerRect = new RectF(rect0);
        canvas.drawRoundRect(outerRect, 5.0f, 5.0f, p);
//		canvas.restore();
        return bitmap;
    }

    public static IconItem mediaBean2IconItem(String url) {
        IconItem item = new IconItem();
        item.setPicMode(IconItem.PIC_AS_WOOD);
        item.setPicUrl(url);
        item.setShowTag(false);
        item.setShowName(true);
        item.setNameColor(Color.rgb(0xff, 0xff, 0xff));
        item.setFrameColor(Color.TRANSPARENT);
        item.setNameBgColor(Color.TRANSPARENT);
        return item;
    }
}
