package com.sunniwell.player.youtube.widget.matrix;


import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.NinePatch;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.NinePatchDrawable;
import android.opengl.GLSurfaceView.Renderer;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;
import android.view.MotionEvent;

import java.util.ArrayList;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

public class MatrixRenderer implements Renderer {

	public static final String TAG = "SWMatrixRenderer";
	private Rect mRect;
	public int mUnitWidth, mUnitHeight;
	
//	private int mRenderedCount = 0;
//	private boolean mIsRenderedEnough = false;
	
	private Bitmap mFocusBmp;
	private IconItem mFocusPic;
	private boolean mFocusEnable;
	private Bitmap mDefaultBmp;
	private Bitmap mDrawBmp;
	
	private volatile ArrayList<IconItem> mList = null;
	private static Object syncObj = new Object();
	private int mRowNum, mColNum;
	private int mStartRow, mNextStartRow;
	private int mFocusIdx;
	private int mLastFocusIdx;
	private long mLastMs;
	private float mOffset;
	private float mSpeed;
	private int mKeyCode;
	private long mKeyMs;
	public int mFocusStartRow, mFocusStartCol, mFocusEndRow, mFocusEndCol;
	private boolean mMoving = false;
	private int mZ = -100;
	private long mReleaseCPUTime;//ms
	private long mReleaseCPUDura;
	private boolean isPause = false;
	public boolean mRedraw = true;
	public int mTextureNum = 0;
	
    private int mFocusZIndex;
    private int mFocusWidth;
    private int mFocusHeight;
    private boolean mFocusAnimationEnable = true;

	private Handler mFocusChangeHandler = null;

	private OnItemSelectedListener mOnItemSelectedListener;

	int mFocusChangeType =0;
	private GL10 mGl10;

	MatrixRenderer() {
		mUnitWidth = IconItem.PicWidth;
		mUnitHeight = IconItem.PicHeight;
		mFocusBmp = null;
		mFocusPic = null;
		mFocusEnable = false;
		mDefaultBmp = null;
		mDrawBmp = null;

        mRect = new Rect(-100,100,100,-100);

		mRowNum = 3;
		mColNum = 6;
		mStartRow = 0;
		mNextStartRow = 0;
		mFocusIdx = 0;
		mLastFocusIdx = -1;
		mLastMs = 0;
		mOffset = 0;
		mSpeed = 1.0f;
		mKeyCode = 0;
		mKeyMs = 0;

		mFocusStartRow = 0;
		mFocusStartCol = 0;
		mFocusEndRow = 0;
		mFocusEndCol = 0;
		mMoving = false;

		mReleaseCPUTime = 0;//ms
		mReleaseCPUDura = 3000;
	}


	public void setOnItemSelectedListener(OnItemSelectedListener onItemSelectedListener) {
		this.mOnItemSelectedListener = onItemSelectedListener;
	}

    public void setFocusBitmap(Bitmap bitmap, int zIndex, int width, int height, boolean anim){
        mFocusBmp = bitmap;
        mFocusZIndex = zIndex;
        mFocusWidth = width;
        mFocusHeight = height;
        mRedraw = true;
        mFocusAnimationEnable = anim;
    }

	public void onDestroy(){
        if(mList != null)
            mList.clear();

        mFocusPic = null;

		if(mDefaultBmp != null){
			mDefaultBmp.recycle();
			mDefaultBmp = null;
		}
		if(mFocusBmp != null){
			mFocusBmp.recycle();
			mFocusBmp =null;
		}
		
//		mRenderedCount = 0;
//		mIsRenderedEnough = false;
	}
	@Override
	public void onSurfaceCreated(GL10 gl, EGLConfig arg1) {
        gl.glEnable(GL10.GL_TEXTURE_2D);
        gl.glShadeModel(GL10.GL_SMOOTH);
        gl.glClearColor(0.0f, 0.0f, 0.0f, 1.0f);
        gl.glClearDepthf(1.0f);  
        gl.glEnable(GL10.GL_DEPTH_TEST);
        gl.glDepthFunc(GL10.GL_LEQUAL);
        gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
	}

	@Override
	public void onSurfaceChanged(GL10 gl, int width, int height) {
		gl.glViewport(0, 0, width, height);
		gl.glMatrixMode(GL10.GL_PROJECTION);
		gl.glLoadIdentity();
		gl.glOrthof(-width / 2, width / 2, -height / 2, height / 2, -10, 1000);
		gl.glMatrixMode(GL10.GL_MODELVIEW);
		gl.glLoadIdentity();

		mRect.left = -width/2;
		mRect.right = width/2;
		mRect.top = height/2;
		mRect.bottom = -height/2;
	}

	@Override
	public void onDrawFrame(GL10 gl) {
		if( isPause )
		{
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return;
		}
		long now = System.currentTimeMillis();
	
		//系统忙，暂停绘图
		if (mList != null) {
			while (0 < mReleaseCPUTime){
				if (now <= mReleaseCPUTime)
					mReleaseCPUTime = now;
				if (now < mReleaseCPUTime + mReleaseCPUDura){
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}else{
					mReleaseCPUTime = 0;
					break;
				}
				now = System.currentTimeMillis();
			}
		}

		synchronized (syncObj) 
		{			
			//绘图
			mRedraw = false;
			drawAll(gl, now);

			//优化内存管理
			refreshItemPicture(gl, now);
		}
	}

	public boolean onKey(int keyCode) {
		long now = System.currentTimeMillis();
		boolean orientKey = false;
		float maxSpeed = 1.0f;
		synchronized (syncObj) {
			switch (keyCode) {
			case KeyEvent.KEYCODE_DPAD_LEFT:
				orientKey = true;
				maxSpeed = 2.0f;
				break;
			case KeyEvent.KEYCODE_DPAD_RIGHT:
				orientKey = true;
				maxSpeed = 2.0f;
				break;
			case KeyEvent.KEYCODE_DPAD_UP:
			case 92://KeyEvent.KEYCODE_PAGE_UP
				orientKey = true;
				maxSpeed = 4.0f;
				break;
			case KeyEvent.KEYCODE_DPAD_DOWN:
			case 93://KeyEvent.KEYCODE_PAGE_DOWN:
				orientKey = true;
				maxSpeed = 4.0f;
				break;
			default:
				break;
			}	
	
			if (mKeyCode == keyCode) {
				if (now < mKeyMs + 200 && orientKey == true) {
					mSpeed = mSpeed * 1.5f;
					if (maxSpeed < mSpeed)
						mSpeed = maxSpeed;
				} else
					mSpeed = 1.0f;
			} else {
				mSpeed = 1.0f;
			}
			mKeyCode = keyCode;
			mKeyMs = now;
			mLastMs = 0;
	
			if (orientKey == true)
				mMoving = true;
		}
		return false;
	}

	public boolean onTouch(MotionEvent event) {
	switch (event.getAction()) {
	case MotionEvent.ACTION_UP:
	case MotionEvent.ACTION_DOWN:
         if (0 < mColNum && 0 < mRowNum){
	         int uw = (mRect.right-mRect.left)/mColNum;
	         int uh = (mRect.top - mRect.bottom)/mRowNum;
	         int row = (int) (event.getY()/(uh+1));
	         int col = (int) (event.getX()/(uw+1));
	         int idx = (mStartRow+row) * mColNum + col;
	
	         if (0 <= idx && idx < mList.size()){
	        	 Rect rect = getItemRect(idx);
	        	 if (rect.left <= event.getX() && event.getX() < rect.right &&
	        	     rect.top <= event.getY() && event.getY() < rect.bottom ){
	        		 if (idx != mFocusIdx){
						setFocusIndex(idx);
						if(mFocusChangeHandler != null ){
							Message msg  = new Message();
							msg.what = mFocusChangeType;
							msg.arg1 = mFocusIdx;
							mFocusChangeHandler.sendMessage(msg);
						}
	        		}else{
						if(mFocusChangeHandler != null ){
							Message msg  = new Message();
							msg.what = mFocusChangeType;
							msg.arg1 = -1;
							mFocusChangeHandler.sendMessage(msg);
						}
	        		}
					return true;
	        	 }
	         }
         }
         break;
        }
		return false;
	}

	public void drawAll(GL10 gl, long now) {
		Rect clipRect;
		int row, col, idx, picWidth, picHeight;
		float x, y, z, uw, uh;
		float minx, maxx, miny, maxy;
		int beginRow, endRow;

		mGl10 = gl;
		gl.glClearColor(0, 0, 0, 0);  
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();  

		uh = (mRect.top - mRect.bottom)/mRowNum;//GL里面top>bottom
		uw = (mRect.right - mRect.left) / mColNum;
	  	clipRect = mRect;

		if(mList== null || mList.size() <= 0)
			return;

		int cnt = 0;
		if (mLastMs == 0)
			mLastMs = now - 50;
		if (mLastMs + 100 < now)
			mLastMs = now - 100;
		if (now < mLastMs)
			mLastMs = now;
		if (now < mKeyMs)
			mKeyMs = now;
		if (mKeyMs + 200 < now && 1.0f < mSpeed)
			mSpeed = 1.0f;

		if (mMoving){
			mMoving = MoveTo( now );
			if (mMoving)
				mRedraw = true;
		}else{
			//mFocusStartCol = mFocusIdx % mColNum;
			//mFocusStartRow = (mFocusIdx - mStartRow*mColNum) / mColNum;
			//mFocusEndCol = mFocusStartCol;
			//mFocusEndRow = mFocusStartRow;
			mOffset = 0;
			//mLastMs = 0;
		}


		if (mStartRow < mNextStartRow){
			beginRow = 0;
			endRow = mRowNum + 1;
		}else if(mNextStartRow < mStartRow){
			beginRow = -1;
			endRow = mRowNum;
		} else {
			beginRow = 0;
			endRow = mRowNum;
		}
		//?????
		for (row = beginRow; row < endRow; row++) {
			for (col = 0; col < mColNum; col++) {
				idx = (mStartRow + row) * mColNum + col;
				if (idx < 0)
					continue;
				if (mList.size() <= idx)
					break;

				if (mStartRow < mNextStartRow)
					y = mRect.top - (row + 0.5f - mOffset) * uh;
				else if (mNextStartRow < mStartRow)
					y = mRect.top - (row + 0.5f + mOffset) * uh;
				else
					y = mRect.top - (row + 0.5f) * uh;

				x = mRect.left + (col + 0.5f) * uw;
				z = mZ;

				minx = (x - uw/2) > clipRect.left   ? (x - uw/2) : clipRect.left;
				miny = (y - uh/2) > clipRect.bottom ? (y - uh/2) : clipRect.bottom;
				maxx = (x + uw/2) < clipRect.right  ? (x + uw/2) : clipRect.right;
				maxy = (y + uh/2) < clipRect.top    ? (y + uh/2) : clipRect.top;

				if (minx < maxx && miny < maxy) {
					if (drawItem(gl, idx, x, y, z, now)) {
						mRedraw = true;
//						mRenderedCount ++;
//						Log.d("zouplzoupl", "3D MatrixRender drawAll() mRenderedCount = "+mRenderedCount);
//						if (mRenderedCount >= 10) {
//							mIsRenderedEnough = true;
//						} else {
//							mIsRenderedEnough = false;
//						}
					}
					cnt++;
				}
			}
		}

		idx = getFocusIndex();
		if(mFocusEnable && 0 <= idx && idx < mList.size()){
			picWidth = mList.get(idx).getPicWidth();
			picHeight = mList.get(idx).getPicHeight();
			if(mFocusPic == null && mFocusBmp != null) {
				mFocusPic = new IconItem();
              int width = mList.get(idx).getPicWidth();
              int height = mList.get(idx).getPicHeight();
//                width += LayoutUtil.getWidth(52) + width / 100 + Math.abs(width - height) / 100;
//                height += LayoutUtil.getHeight(62) + height / 100 + Math.abs(height - width) / 100;
				/**没有引进LayoutUtil，故直接写为52、62   zoupl*/
				width += 25 + width / 100 + Math.abs(width - height) / 100;
				height += 25 + height / 100 + Math.abs(height - width) / 100;

                height += mList.get(idx).getTextHeight();
//                width = LayoutUtil.getLayoutWidth(width);
//                height = LayoutUtil.getLayoutHeight(height);
				/**没有引进LayoutUtil，故只是使用width与height  zoupl*/
				mFocusPic.setSize(width , height);
                Bitmap bitmap = Bitmap.createBitmap(width, height, Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                Drawable drawable = null;
                if (mFocusBmp != null && !mFocusBmp.isRecycled()) {
                    byte[] chunk = mFocusBmp.getNinePatchChunk();
                    if (NinePatch.isNinePatchChunk(chunk)) {
                        drawable = new NinePatchDrawable(mFocusBmp, chunk, new Rect(), null);
                    } else {
                        drawable = new BitmapDrawable(mFocusBmp);
                    }
                }
                if(drawable != null){
                    drawable.setBounds(0, 0, width, height);
                    drawable.draw(canvas);
                }
				mFocusPic.setTexture(gl, bitmap);
			}
			if(mFocusPic != null){
				x = ((mRect.left + (mFocusStartCol + 0.5f + (mFocusEndCol - mFocusStartCol)*mOffset)*uw) - 0);
				y = ((mRect.top - (mFocusStartRow + 0.5f + (mFocusEndRow - mFocusStartRow)*mOffset)*uh) + 0);
				z = (mZ + 15);
//				y += (mUnitHeight - picHeight)/2 + 13;

				gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
				gl.glEnable(GL10.GL_BLEND);
				gl.glPushMatrix();
			    gl.glTranslatef(x, y, z);
		    	mFocusPic.draw(gl);
			    gl.glPopMatrix();
		        gl.glDisable(GL10.GL_BLEND);
			}
	    }
		mLastMs = now;
	}

	public void onTimer(long now){
		IconItem item;
		int cnt, n, idx = getFocusIndex();

		synchronized (syncObj) 
		{
			if(mList== null || mList.size() <= 0)
				return;
	
			//背后载入图片，但每次要限制个数，以免占用CPU过多
			cnt = 0;
			for (n=0; n<2 && cnt<2; n++){
				for( n=idx; 0<=n && idx-12<=n && cnt<1; n--){
					item = mList.get(n);
					if ((mKeyMs + 5000 < now) ||
						(mKeyMs + 1000 < now && idx - 12 < n)){
						if (item.loadBitmap(now)){
							cnt++;
							break;
						}
					}
				}
	
				for( n=idx+1; n<mList.size() && n<idx+12 && cnt<1; n++){
					item = mList.get(n);
					if ((mKeyMs + 5000 < now) ||
						(mKeyMs + 1000 < now && n < idx + 12)){
						if (item.loadBitmap(now)){
							cnt++;
							break;
						}
					}
				}
			}
		}
	}

	//优化内存管理
	private void refreshItemPicture(GL10 gl, long now){
		IconItem item;
		int cnt, n, idx = getFocusIndex();

		if(mList== null || mList.size() <= 0 || mTextureNum < 700)
			return;

		//释放离视野范围太远的纹理
		cnt = 0;
		for (n=0; n<idx-24; n++){
			if (0 <= n && n < mList.size()){
				item = mList.get(n);
				if (item.releaseTexture(gl)){
					mTextureNum--;
					cnt++;
				}
			}
		}
		cnt = 0;
		for (n=idx+24; n<mList.size(); n++){
			if (0 <= n && n < mList.size()){
				item = mList.get(n);
				if (item.releaseTexture(gl)){
					mTextureNum--;
					cnt++;
				}
			}
		}
	}


	private boolean MoveTo(long now){
		float dura = 400.0f;
		int startRow = mStartRow;
		int fcsIdx = mFocusIdx;
		int i, num = (int)mOffset;
		float speed = mSpeed;
		boolean moving = true;
		
		switch (mKeyCode) {
		case KeyEvent.KEYCODE_DPAD_LEFT:
			if (1.5f < speed)//控制焦点移动速度不能太快
				speed = 1.5f;
			if ((0 < mStartRow) && 
				(mFocusIdx == mStartRow*mColNum || mFocusIdx == (mStartRow+1)*mColNum)){
				startRow = mStartRow - 1;
				dura *= speed;
			}else{
				dura /= 2;
			}
			if (0 < mFocusIdx)
				fcsIdx = mFocusIdx - 1;
			break;
		case KeyEvent.KEYCODE_DPAD_RIGHT:
			if (1.5f < speed)//控制焦点移动速度不能太快
				speed = 1.5f;
			if ((mFocusIdx % mColNum) == mColNum-1 && 
				(mStartRow+2)*mColNum <= mFocusIdx + 1 &&
				(mStartRow + 1) < getTotalRowNum()){
				startRow = mStartRow + 1;
				if( startRow + mRowNum >= getTotalRowNum())
					startRow = getTotalRowNum() - mRowNum;
                if(startRow < 0){
                    startRow = 0;
                }
				dura *= speed;
			}else{
				dura /= 2;
			}
			if (mFocusIdx + 1 < mList.size() )
				fcsIdx = mFocusIdx + 1;
			break;
		case KeyEvent.KEYCODE_DPAD_UP:
		case 92://KeyEvent.KEYCODE_PAGE_UP
			if (num <= 0)
				num = 1;
			if (0 < mStartRow && mFocusIdx < (mStartRow+2)*mColNum){
				startRow = mStartRow - num;
				if (startRow < 0)
					startRow = 0;
			}
			for (i=0; i<num; i++){
				if (fcsIdx < mColNum)
					break;
				fcsIdx -= mColNum;
			}
			if (fcsIdx < 0){
				fcsIdx = 0;
				dura /= 2;
			}
			break;
		case KeyEvent.KEYCODE_DPAD_DOWN:
		case 93://KeyEvent.KEYCODE_PAGE_DOWN:
			if (num <= 0)
				num = 1;
			if (mStartRow+1 < getTotalRowNum()){
				if ((mStartRow+1)*mColNum <= mFocusIdx){
					startRow = mStartRow + num;
					if( startRow + mRowNum >= getTotalRowNum())
						startRow = getTotalRowNum() - mRowNum;
                    if(startRow < 0){
                        startRow = 0;
                    }
//					if (getTotalRowNum()-1 <= startRow)
//						startRow = getTotalRowNum() - 1;
				}
				if (getFocusRow()+1 < getTotalRowNum()){
					fcsIdx = mFocusIdx + mColNum * num;
					if (mList.size()-1 < fcsIdx){
						fcsIdx = mList.size() - 1;
						dura /= 2;
					}
				}
			}
			break;
		case KeyEvent.KEYCODE_DPAD_CENTER:
			break;
		default:
			break;
		}
		mOffset += (now - mLastMs) / dura * speed;
		num = (int)mOffset;		
		mNextStartRow = startRow;
		mFocusStartCol = mFocusIdx % mColNum;
		mFocusStartRow = (mFocusIdx - mStartRow*mColNum) / mColNum;
		mFocusEndCol = fcsIdx % mColNum;
		mFocusEndRow = (fcsIdx - startRow*mColNum) / mColNum;

//	Log.e("move", "offset="+mOffset + " startRow="+mStartRow + 
//				  " start="+mFocusStartRow + ":"+mFocusStartCol + 
//				  " end="+mFocusEndRow + ":"+mFocusEndCol + 
//				  " fcs="+mFocusIdx + "->"+fcsIdx +
//				  " speed="+mSpeed +
//				  " cycle="+(now-mLastMs));

		if (1.0f <= mOffset) {
			mLastFocusIdx = mFocusIdx;
			mFocusIdx = fcsIdx;
			if(mFocusChangeHandler != null ){
				Message msg  = new Message();
				msg.what = mFocusChangeType;
				msg.arg1 = mFocusIdx;
				mFocusChangeHandler.sendMessage(msg);
			}
			mStartRow = mNextStartRow;
			mFocusStartCol = mFocusIdx % mColNum;
			mFocusStartRow = (mFocusIdx - mStartRow*mColNum) / mColNum;
			mFocusEndCol = mFocusStartCol;
			mFocusEndRow = mFocusStartRow;

			if (mSpeed <= 1.2f) {
				mOffset = 0;
				mLastMs = 0;
				mKeyCode = -1;
				moving = false;
			}else {
				mOffset -= (int)mOffset;
				//mLastMs = now;//0;
				//mLastMs = now - (long)((mOffset-(int)mOffset)*dura);
				//mOffset = 0;//now - (mOffset-(int)mOffset)*dura;
			}
		}
		if (mOnItemSelectedListener != null) {
			mOnItemSelectedListener.onItemSelected(mFocusIdx);
		}
		return moving;
	}
	
	//回执某一个Item
	private boolean drawItem(GL10 gl, int idx, float x, float y, float z, long now) {
		IconItem item;
		boolean needScroll = false;
		boolean needRedraw = false;
		int num0, num1;

		if (idx <0 || mList.size() <= idx)
			return false;

		if (mDrawBmp == null)
			mDrawBmp = Bitmap.createBitmap(mUnitWidth, mUnitHeight, Config.ARGB_8888);

		item = mList.get(idx);
		if (item.isNeedScroll() && idx == getFocusIndex() && mFocusEnable == true )
			needScroll = true;

		boolean busy =  now < mKeyMs + 1000;
		//只有在空闲时才载入新图片
		if (busy == false){
			//上一个焦点如果有滚动字幕，恢复原点位置
			if (mLastFocusIdx == idx && item.getScrollPos() != 0){
				item.setScrollPos(0);
				if(item.releaseTexture(gl))
					mTextureNum--;
				mLastFocusIdx = -1;
			}else if (needScroll) {
				//滚动字幕，必须更新纹理
				item.setScrollPos(needScroll ? now:0);
				if(item.releaseTexture(gl))
					mTextureNum--;
			}
		}

		num0 = item.isTextureValid() ? 1 : 0;
		if (mDrawBmp != null)
			needRedraw = item.loadTexture(gl, mDefaultBmp, mDrawBmp, true);
		num1 = item.isTextureValid() ? 1 : 0;
		mTextureNum += num1 - num0;

		gl.glPushMatrix();
		gl.glTranslatef(x, y, z);
		item.draw(gl);
		gl.glPopMatrix();

		return needScroll||needRedraw;
	}

	private Rect getItemRect(int idx) {
		int x, y, uw, uh, w, h, row, col;
		Rect rect;
		IconItem item;

		if (idx < 0 || mList.size() <= idx || mColNum <= 0 || mRowNum <= 0)
			return new Rect(0,0,0,0);

		item = mList.get(idx);
		uw = (mRect.right - mRect.left) / mColNum;
		uh = (mRect.top - mRect.bottom) / mRowNum;
		w = item.getWidth();
		h = item.getHeight();

		row = idx / mColNum - mStartRow;
		col = idx % mColNum;

		x = col * uw;
		y = (int)((row - mOffset) * uh);
		x += (uw - w) / 2;
		y += (uh - h) / 2;

		rect = new Rect(x, y, x+w, y+h);
		return rect;
	}

	public void setFocusBitmap(Bitmap bitmap){
		mFocusBmp = bitmap;
//		mFocusBmp = IconItemBase.createBitmap(IconItemBase.PicWidth, IconItemBase.PicHeight + IconItemBase.TxtHeight, 1);
		mRedraw = true;
	}

	
	public void setDefaultBitmap(Bitmap bitmap){
		mDefaultBmp = bitmap;
//		mDefaultBmp = IconItemBase.createBitmap(IconItemBase.PicWidth, IconItemBase.PicHeight + IconItemBase.TxtHeight, 0);
		mRedraw = true;
	}
	
	public void setFocusStatus(boolean enable){
		mLastFocusIdx = mFocusIdx;
		mFocusEnable = enable;
		mRedraw = true;
	}
	
	public boolean getFocusStatus(){
		return mFocusEnable;
	}
	
	public void setFocusIndex(int idx) {
		mLastFocusIdx = mFocusIdx;
		mFocusIdx = idx;
		mFocusStartCol = mFocusIdx % mColNum;
		mFocusStartRow = (mFocusIdx - mStartRow*mColNum) / mColNum;
		mFocusEndCol = mFocusStartCol;
		mFocusEndRow = mFocusStartRow;
		mRedraw = true;
	}

	public int getFocusIndex() {
		synchronized (syncObj) {
			return mFocusIdx;
		}
	}

	public void setFocusPos(int pos) {
		mLastFocusIdx = mFocusIdx;
		mFocusIdx = mStartRow*mColNum + pos;
		mRedraw = true;
	}

	public int getFocusPos() {
		return mFocusIdx - mStartRow*mColNum;
	}

	public void setFocusChangeHandler(Handler handler, int type){
		mFocusChangeHandler = handler;
		mFocusChangeType = type;
	}

	public void setViewRowNum(int rowNum){
		mRowNum = rowNum;
		mRedraw = true;
	}

	public void setViewColNum(int colNum) {
		mColNum = colNum;
		mRedraw = true;
	}
	
	public int getViewRowNum(){
		return mRowNum;
	}

	public int getViewColNum() {
		return mColNum;
	}

	public void setUnitSize(int width, int height){
		mUnitWidth = width;
		mUnitHeight = height;
		if(mDrawBmp != null)
			mDrawBmp.recycle();
		mDrawBmp = Bitmap.createBitmap(mUnitWidth, mUnitHeight, Config.ARGB_8888);
//		if(mDefaultBmp == null || mDefaultBmp.isRecycled())
//			mDefaultBmp = IconItemBase.createBitmap(mUnitWidth, mUnitHeight, 0);
//		if(mFocusBmp == null || mFocusBmp.isRecycled())
//			mFocusBmp = IconItemBase.createBitmap(mUnitWidth, mUnitHeight, 1);
		mRedraw = true;
	}
	
	public void setStartRow(int row){
		mStartRow = row;
		mRedraw = true;
	}

	public int getStartRow(){
		return mStartRow;
	}

	private int getFocusRow() {
		return mFocusIdx / mColNum;
	}
	
	private int getTotalRowNum(){
		return (mList.size() + mColNum -1) / mColNum;
	}

	public void addItemList(ArrayList<IconItem> list) {

		if(list == null || list.size() <= 0)
			return;
		synchronized (syncObj) {

			if (mList == null) {
				mList = new ArrayList<IconItem>();
				initRender();
			}
			mList.addAll(list);
			mRedraw = true;
		}

	}
	public void addItem(IconItem item) {
		synchronized (syncObj) {
			if (mList == null)
			{
				mList = new ArrayList<IconItem>();
				initRender();
			}
			mList.add(item);
			mRedraw = true;
		}
	}
	
	public void clearItemList(){
		synchronized (syncObj) {
			mList = new ArrayList<IconItem>();
			initRender();
			mRedraw = true;
		}
	}
	
	public ArrayList<IconItem> getItemList(){
		synchronized (syncObj) {
			return mList;
		}
	}
	
	public int getItemListSize(){
		synchronized (syncObj) {
			if(mList == null)
				return 0;
			return mList.size();
		}
	}

	private void initRender(){
		mStartRow = 0;
		mFocusStartRow = 0;
		mFocusStartCol = 0;
		mFocusEndRow = 0;
		mFocusEndCol = 0;
		mFocusIdx = 0;
	}

	public void setPause( boolean pause){
		isPause = pause;
	}

	public void setReleaseCPU(long startTimeMs, long timeout){
		mReleaseCPUTime = startTimeMs;
		mReleaseCPUDura = timeout;
	}
}
