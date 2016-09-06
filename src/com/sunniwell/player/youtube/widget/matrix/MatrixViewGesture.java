package com.sunniwell.player.youtube.widget.matrix;
import android.view.GestureDetector;
import android.view.MotionEvent;

/**
 * Created with IntelliJ IDEA.
 * User: husl
 * Date: 13-6-21
 * Time: 下午4:31
 * To change this template use File | Settings | File Templates.
 */
public class MatrixViewGesture implements GestureDetector.OnGestureListener{
    private MatrixView mMatrixView = null;

    public MatrixViewGesture(MatrixView matrixView){
        mMatrixView = matrixView;
    }

	// 鼠标按下的时候，会产生onDown。由一个ACTION_DOWN产生。
    @Override
    public boolean onDown(MotionEvent e) {
        if(mMatrixView != null){
            return mMatrixView.onDown(e);
        }
        return false;
    }

	//点击了触摸屏，但是没有移动和弹起的动作。onShowPress和onDown的区别在于
	//onDown是，一旦触摸屏按下，就马上产生onDown事件，但是onShowPress是onDown事件产生后，
    //一段时间内，如果没有移动鼠标和弹起事件，就认为是onShowPress事件
    @Override
    public void onShowPress(MotionEvent e) {

    }

    // 轻击触摸屏后，弹起。如果这个过程中产生了onLongPress、onScroll和onFling事件，就不会
    // 产生onSingleTapUp事件。
    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        if(mMatrixView != null){
            return mMatrixView.onSingleTapUp(e);
        }
        return false;
    }
    // 滚动事件，当在触摸屏上迅速的移动，会产生onScroll。由ACTION_MOVE产生
    // e1：第1个ACTION_DOWN MotionEvent
    // e2：最后一个ACTION_MOVE MotionEvent 
	// distanceX：距离上次产生onScroll事件后，X抽移动的距离
	// distanceY：距离上次产生onScroll事件后，Y抽移动的距离
    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        if(mMatrixView != null){
            return mMatrixView.onScroll(e2.getY() - e1.getY());
        }
        return false;
    }
    
	// 用户长按触摸屏，由多个MotionEvent ACTION_DOWN触发
    @Override
    public void onLongPress(MotionEvent e) {

    }
    
	// 用户按下触摸屏、快速移动后松开,这个时候，你的手指运动是有加速度的。
	// 由1个MotionEvent ACTION_DOWN, 
	// 多个ACTION_MOVE, 1个ACTION_UP触发 
	// e1：第1个ACTION_DOWN MotionEvent 
	// e2：最后一个ACTION_MOVE MotionEvent 
	// velocityX：X轴上的移动速度，像素/秒 
	// velocityY：Y轴上的移动速度，像素/秒
    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
        if(mMatrixView != null){
            mMatrixView.onFling(velocityY);
        }
        return false;
    }
}
