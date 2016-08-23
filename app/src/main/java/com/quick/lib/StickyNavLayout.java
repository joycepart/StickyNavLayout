package com.quick.lib;

import android.content.Context;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.OverScroller;
import android.widget.ScrollView;

public class StickyNavLayout extends LinearLayout {

    private View mTop;
    private View mNav;
    private ViewPager mViewPager;
    private int mTopViewHeight;
    OverScroller mScroller;
    private boolean isTopHidden = false;
    private ScrollView mInnerScrollView;
    private VelocityTracker mVelocityTracker;
    private int mMaximumVelocity, mMinimumVelocity;
    float lastX=0;
    float lastY = 0;
    float dy = 0;
    float dx=0;
    private int mTouchSlop;
    boolean onIntercept;
    public StickyNavLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(LinearLayout.VERTICAL);
        mScroller = new OverScroller(context);
        mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();//获得能够进行手势滑动的距离

        mMaximumVelocity = ViewConfiguration.get(context)
                .getScaledMaximumFlingVelocity();
        mMinimumVelocity = ViewConfiguration.get(context)
                .getScaledMinimumFlingVelocity();
    }

    private void getCurrentScrollView() {
        int currentItem = mViewPager.getCurrentItem();
        PagerAdapter a = mViewPager.getAdapter();
        if (a instanceof FragmentPagerAdapter) {
            FragmentPagerAdapter fadapter = (FragmentPagerAdapter) a;
            Fragment item = (Fragment) fadapter.instantiateItem(mViewPager,
                    currentItem);
            mInnerScrollView = (ScrollView) (item.getView()
                    .findViewById(R.id.id_stickynavlayout_innerscrollview));
        } else if (a instanceof FragmentStatePagerAdapter) {
            FragmentStatePagerAdapter fsAdapter = (FragmentStatePagerAdapter) a;
            Fragment item = (Fragment) fsAdapter.instantiateItem(mViewPager,
                    currentItem);
            mInnerScrollView = (ScrollView) (item.getView()
                    .findViewById(R.id.id_stickynavlayout_innerscrollview));
        }

    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        mTop = findViewById(R.id.id_stickynavlayout_topview);
        mNav = findViewById(R.id.id_stickynavlayout_indicator);
        View view = findViewById(R.id.id_stickynavlayout_viewpager);
        if (!(view instanceof ViewPager)) {
            throw new RuntimeException(
                    "id_stickynavlayout_viewpager show used by ViewPager !");
        }
        mViewPager = (ViewPager) view;

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        ViewGroup.LayoutParams params = mViewPager.getLayoutParams();
        /**因为StickyNavLayout是match_parent,所以getMeasuredHeight()等于屏幕高度-状态栏高度
         *
         */
        params.height = getMeasuredHeight() - mNav.getMeasuredHeight();
        mTopViewHeight = mTop.getMeasuredHeight();
    }

    /**
     * 在此处理什么时候需要拦截，什么时候不需要拦截
     */
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        float x = ev.getX();
        float y = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                lastX=x;
                lastY = y;
                break;
            case MotionEvent.ACTION_MOVE:
                dx=x-lastX;
                dy = y - lastY;
                lastX = x;
                lastY = y;
                getCurrentScrollView();
                if(onIntercept){
                    //如果是水平滑动
                    if(Math.abs(dx)>Math.abs(dy)){
                      onIntercept =false;
                    }else{
                        // 如果topView隐藏，且上滑动时，则改变当前事件为ACTION_DOWN
                        if (isTopHidden && dy < 0) {
                            onIntercept = false;
                            ev.setAction(MotionEvent.ACTION_DOWN); //手动调用分发事件
                            dispatchTouchEvent(ev);//手动去发出事件
                        }
                    }
                }else{
                    //竖直滑动
                    if(Math.abs(dy)>Math.abs(dx)){
                        // 如果topView没有隐藏
                        // 或sc的listView在顶部 && topView隐藏 && 下拉，则拦截
                        if(!isTopHidden||(isTopHidden&&dy > 0 && mInnerScrollView.getScrollY() == 0 )){
                            onIntercept=true;
                            ev.setAction(MotionEvent.ACTION_DOWN);
                            dispatchTouchEvent(ev);
                        }
                    }
                }
                break;
        }
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        int action = ev.getAction();
        switch (action) {
            case MotionEvent.ACTION_MOVE:
                if (onIntercept) {
                    return true;
                }
        }
        return super.onInterceptTouchEvent(ev);
    }


    @Override
    public boolean onTouchEvent(MotionEvent event) {
        initVelocityTrackerIfNotExists();
        mVelocityTracker.addMovement(event);
        int action = event.getAction();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_MOVE:
                scrollBy(0, (int) -dy);
                break;
            case MotionEvent.ACTION_CANCEL:
                recycleVelocityTracker();
                if (!mScroller.isFinished()) {
                    mScroller.abortAnimation();
                }
                break;
            case MotionEvent.ACTION_UP:
                mVelocityTracker.computeCurrentVelocity(1000, mMaximumVelocity);//1秒的单位时间内运动了多少个像素
                int velocityY = (int) mVelocityTracker.getYVelocity();//拿到y方向速率
                if (Math.abs(velocityY) > mMinimumVelocity) {
                    fling(-velocityY);
                }
                recycleVelocityTracker();
                break;
        }

        return true;
    }

    public void fling(int velocityY) {
        /*
		fling(int startX, int startY, int velocityX, int velocityY,int minX, int maxX, int minY, int maxY)
		*/
        mScroller.fling(0, getScrollY(), 0, velocityY, 0, 0, 0, mTopViewHeight);
        invalidate();
    }

    @Override
    public void scrollTo(int x, int y) {
        super.scrollTo(x, y);
        if (getScrollY() > mTopViewHeight) {
            setScrollY(mTopViewHeight);
        }
        if (getScrollY() < 0) {
            setScrollY(0);
        }
        isTopHidden = (getScrollY() == mTopViewHeight);
    }

    @Override
    public void computeScroll() {
        super.computeScroll();
        if (mScroller.computeScrollOffset()) {
            scrollTo(0, mScroller.getCurrY());
            invalidate();
        }
    }

    private void initVelocityTrackerIfNotExists() {
        if (mVelocityTracker == null) {
            mVelocityTracker = VelocityTracker.obtain();
        }
    }

    private void recycleVelocityTracker() {
        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }
}
