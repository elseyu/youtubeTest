package com.sunniwell.player.youtube.widget.matrix;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.opengl.GLSurfaceView;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.util.Log;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;

import com.sunniwell.player.youtube.R;

import java.util.ArrayList;


public class MatrixView extends GLSurfaceView {
    private static final String TAG = "MatrixView";
	private static final int DEAD = 0;
	private static final int RUNNING = 1;
	private static final int PAUSE = 2;

	private MatrixRenderer mRender = null;

	private int mStatus = DEAD;
	private long mRedrawStartTime = 0;
	private static final int REDRAW_DURATION = 300000;
	private GestureDetector mGestureDetector;
	private MatrixViewGesture mMatrixViewGesture;
	private ScrollRunnable mScrollRunnable;
	private boolean mMoveRunning = true;
	private int mScrollY = 0;
	private int mScrollDirect = 0;
	public MatrixView(Context context){
		super(context);
		init(context);
	}

	public MatrixView(Context context, AttributeSet attrs){
		super(context, attrs);
		init(context);
	}

	public MatrixRenderer getRender() {
		return mRender;
	}


	private void init(Context context){
        getHolder().addCallback(this);
        mScrollRunnable = new ScrollRunnable();
        new Thread(mScrollRunnable).start();

        mMatrixViewGesture = new MatrixViewGesture(this);
        mGestureDetector = new GestureDetector(mMatrixViewGesture);

		setEGLConfigChooser(8, 8, 8, 8, 16, 0);
		mRender = new MatrixRenderer();
		setRenderer(mRender);
		setZOrderOnTop(true);
		setRenderMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
		mStatus = PAUSE;
		mRedrawStartTime = System.currentTimeMillis();
		mHandler.sendEmptyMessage(0);
	}

	public void onDestroy()
	{
        setFocusStatus(false);
        mStatus = DEAD;
        if(!mScrollRunnable.isFinish()){
            mScrollRunnable.broken();
        }
        mMoveRunning = false;
		mRender.onDestroy();
	}
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {
		super.surfaceChanged(holder, format, w, h);
		mStatus = RUNNING;
        Log.d(TAG, "surfaceChanged ");
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		super.surfaceCreated(holder);
        holder.setFormat(PixelFormat.TRANSLUCENT);
		mStatus = RUNNING;
        mHandler.sendEmptyMessage(0);
        Log.d(TAG, "surfaceCreated ");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		mStatus = DEAD;
		super.surfaceDestroyed(holder);
        Log.d(TAG, "surfaceDestroyed ");
	}

	Handler mHandler = new Handler(Looper.getMainLooper()) {
		@Override

		public void handleMessage(Message msg) {
            if(mStatus == DEAD)
                return;
			long now = System.currentTimeMillis();
			try{
				if( mStatus == RUNNING && mRender != null && (mRender.mRedraw
						|| mRedrawStartTime + REDRAW_DURATION > now) ) {
					requestRender();
//					mRedrawStartTime = now;
				}
				if (mRender != null)
					mRender.onTimer(now);
//				Thread.sleep(1);
			}catch(Exception e) {
				e.printStackTrace();
			}
			mHandler.sendEmptyMessageDelayed(0, 25);
		}
	};

	public boolean onKey(int keyCode) {
		boolean res = true;
		switch(keyCode){
		case KeyEvent.KEYCODE_DPAD_LEFT:
		case KeyEvent.KEYCODE_DPAD_RIGHT:
		case KeyEvent.KEYCODE_DPAD_UP:
		case KeyEvent.KEYCODE_DPAD_DOWN:
		case KeyEvent.KEYCODE_DPAD_CENTER:
		case 92:	//KeyEvent.KEYCODE_PAGE_UP:
		case 93:	//KeyEvent.KEYCODE_PAGE_DOWN:
			mRedrawStartTime = System.currentTimeMillis();
			mRender.onKey(keyCode);
			onResume();
			break;
		default:
            res = false;
			break;
		}
		return res;
	}
    public boolean onDown(MotionEvent event){
        if(!mScrollRunnable.isFinish()){
            mScrollRunnable.broken();
        }
        mScrollY = 0;
        mScrollDirect = 0;
        return false;
    }

    public boolean onSingleTapUp(MotionEvent event){
        if(!mScrollRunnable.isFinish()){
            mScrollRunnable.broken();
        }
        mScrollY = 0;
        mScrollDirect = 0;
        return mRender.onTouch(event);
    }
    public boolean onScroll(float deltaY){
        if(!mScrollRunnable.isFinish()){
            mScrollRunnable.broken();
        }

        if(deltaY > 0){
            if(deltaY - mScrollY > mRender.mUnitHeight / 2){
                mScrollY += mRender.mUnitHeight;
//                mRender.setFocusPos(0);
                onKey(KeyEvent.KEYCODE_DPAD_UP);
                mScrollDirect = KeyEvent.KEYCODE_DPAD_UP;
            }else if(deltaY - mScrollY < - mRender.mUnitHeight / 2){
                mScrollY -= mRender.mUnitHeight;
//                mRender.setFocusPos(7);
                onKey(KeyEvent.KEYCODE_DPAD_DOWN);
                mScrollDirect = KeyEvent.KEYCODE_DPAD_DOWN;
            }
        }else if(deltaY < 0){
            if(deltaY + mScrollY < -mRender.mUnitHeight / 2){
                mScrollY += mRender.mUnitHeight;
//                mRender.setFocusPos(7);
                onKey(KeyEvent.KEYCODE_DPAD_DOWN);
                mScrollDirect = KeyEvent.KEYCODE_DPAD_DOWN;
            }else if(deltaY + mScrollY > mRender.mUnitHeight / 2){
                mScrollY -= mRender.mUnitHeight;
//                mRender.setFocusPos(0);
                onKey(KeyEvent.KEYCODE_DPAD_UP);
                mScrollDirect = KeyEvent.KEYCODE_DPAD_UP;
            }
        }
        return false;
    }

    @Override
	public boolean onGenericMotionEvent(MotionEvent event) {
		// TODO Auto-generated method stub
    	if( event.getAction() == MotionEvent.ACTION_SCROLL ){

    		if(!mScrollRunnable.isFinish()){
                mScrollRunnable.broken();
            }
            mScrollY = 0;
            mScrollDirect = 0;

            float vscroll = event.getAxisValue(MotionEvent.AXIS_VSCROLL);
	    	if(vscroll<0.0f){
	    		for(int i=0; i>vscroll; i--){
	    			onKey(KeyEvent.KEYCODE_DPAD_DOWN);
	    		}
	    	}else{
	    		for(int i=0; i<vscroll; i++){
	    			onKey(KeyEvent.KEYCODE_DPAD_UP);
	    		}
	    	}
    	}
		return super.onGenericMotionEvent(event);
//		if(0!=(event.getSource()&InputDevice.SOURCE_CLASS_POINTER)){
//		　　　　switch(event.getAction()){
//		　　　　　　case MotionEvent.ACTION_SCROLL:
//		　　　　　　　　if(event.getAxisValue(MotionEvent.AXIS_VSCROLL)<0.0f)
//		          　　　　selectNext()
//		　　　　　　　　else
//		          　　　　selectPrev();
//		　　　　　　　　return true;
//		　　　　　　}
//		　　　　}
//		　　returnsuper.onGenericMotionEvent(event);
	}

	public boolean onFling(float velocityY){
        if(!mScrollRunnable.isFinish()){
            mScrollRunnable.broken();
        }
        mScrollY = 0;
        if(velocityY > 0){
            if(mScrollDirect != 0 && mScrollDirect != KeyEvent.KEYCODE_DPAD_UP){
                mScrollDirect = 0;
                return false;
            }
//            mRender.setFocusPos(0);
            mScrollRunnable.fling(KeyEvent.KEYCODE_DPAD_UP, (int)velocityY / 4);
        }else if(velocityY < 0){
            if(mScrollDirect != 0 && mScrollDirect != KeyEvent.KEYCODE_DPAD_DOWN){
                mScrollDirect = 0;
                return false;
            }
//            mRender.setFocusPos(7);
            mScrollRunnable.fling(KeyEvent.KEYCODE_DPAD_DOWN, (int)-velocityY / 4);
        }
        return false;
    }
	public boolean onTouch(MotionEvent event) {
		return mGestureDetector.onTouchEvent(event);
	}

	public void setViewRowNum(int rowNum){
		mRender.setViewRowNum(rowNum);
	}

	public int getViewRowNum(){
		return mRender.getViewRowNum();
	}

	public void setViewColNum(int colNum) {
		mRender.setViewColNum(colNum);
	}

	public int getViewColNum(){
		return mRender.getViewColNum();
	}
	
    public void setFocusBitmap(Bitmap bitmap, int zIndex, int width, int height, boolean anim){
        mRender.setFocusBitmap(bitmap, zIndex, width, height, anim);
    }

	public void setUnitSize(int width, int height){
		/**没有引进LayoutUtil，故直接使用width与height zpl*/
		/*width = LayoutUtil.getWidth(width);
    	height = LayoutUtil.getHeight(height);*/
		mRender.setUnitSize(width, height);
        mRender.setFocusBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.item_focus));
	}

	public void setStartRow(int row){
		mRender.setStartRow(row);
	}

	public int getStartRow(){
		return mRender.getStartRow();
	}

	public void setFocusBitmap(Bitmap bitmap){
		mRender.setFocusBitmap(bitmap);
	}

	public void setDefaultBitmap(Bitmap bitmap){
		mRender.setDefaultBitmap(bitmap);
	}


	public void setFocusStatus(boolean enable){
		mRedrawStartTime = System.currentTimeMillis();
		if(enable == true){
			requestFocus();
		} else if(enable == false){
			clearFocus();
		}
		mRender.setFocusStatus(enable);
		invalidate();
	}


	public boolean getFocusStatus(){
		return mRender.getFocusStatus();
	}

	//get the focous in matrix
	public int getFocusIndex(){
		return mRender.getFocusIndex();
	}

	public void setFocusIndex( int idx ){
		mRedrawStartTime = System.currentTimeMillis();
		mRender.setFocusIndex( idx );
	}

	public void setFocusChangeHandler(Handler handler, int type){
		mRender.setFocusChangeHandler(handler, type);
	}

	public void addItemList(ArrayList<IconItem> itemList){
		mRedrawStartTime = System.currentTimeMillis();
		mRender.addItemList(itemList);
	}
	public void addItem(IconItem item){
		mRedrawStartTime = System.currentTimeMillis();
		mRender.addItem(item);
	}
	public void clearItemList(){
		mRedrawStartTime = System.currentTimeMillis();
		mRender.clearItemList();
	}
	public ArrayList<IconItem> getItemList(){
		return mRender.getItemList();
	}
	public int getItemSize(){
		return mRender.getItemListSize();
	}

	public void setReleaseCPU(long startTimeMs, long timeout){
		mRender.setReleaseCPU(startTimeMs,timeout);
	}
	public void setPause( boolean pause){
		if( !pause )
			mRedrawStartTime = System.currentTimeMillis();
		mRender.setPause(pause);
	}

    private class ScrollRunnable implements Runnable {
        private int keyCode = 0;
        private long millis = 0;
        private long startMillis = 0;
        private boolean broken = false;
        private boolean isfinish = false;
        @Override
        public void run() {
            //To change body of implemented methods use File | Settings | File Templates.
            while(mMoveRunning){
                startMillis = System.currentTimeMillis();
                while(System.currentTimeMillis() - startMillis < millis && !broken){
                    onKey(keyCode);
                    threadDelay(100);
                }
                isfinish = true;
                startMillis = 0;
                millis = 0;
                threadDelay(10);
            }
        }

        public void fling(int keyCode, long millis){
            this.keyCode = keyCode;
            this.millis = millis;
            broken = false;
            isfinish = false;
            for(int i = 0; i < 4; ++i){
                onKey(keyCode);
            }
        }

        public void broken(){
            this.broken = true;
        }

        public boolean isFinish(){
            return this.isfinish;
        }
    };

    private void threadDelay(long millis){
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
    }

}
