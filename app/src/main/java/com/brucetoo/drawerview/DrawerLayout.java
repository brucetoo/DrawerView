package com.brucetoo.drawerview;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.os.Build;
import android.support.annotation.ColorInt;
import android.support.annotation.IdRes;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.ViewDragHelper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;


/**
 * Created by Bruce Too
 * On 6/27/16.
 * At 15:22
 */
public class DrawerLayout extends ViewGroup implements View.OnClickListener {

    private static final String TAG = DrawerLayout.class.getSimpleName();

    private static final int DEFAULT_MAX_WIDTH = 500;
    private static final int DEFAULT_MASK_COLOR = 0x77000000;

    private ViewDragHelper mViewDragHelper;

    private float mInitMotionX;
    private float mInitMotionY;

    private int mDragMaxWidth;
    @IdRes
    private int mDragViewResId;
    @IdRes
    private int mContentViewResId;
    private View mDragView;
    private View mContentView;
    private View mMaskView;
    @ColorInt
    private int mMaskColor;

    private int mDragViewLeft;
    private int mScreenWidth;
    private float mDragRatio;
    private View mReleaseView;
    private DragRatioListener mDragRatioListener;
    private boolean mIsMaskEnable;

    public DrawerLayout(Context context) {
        this(context, null);
    }

    public DrawerLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public DrawerLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public DrawerLayout(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.DrawerLayout);
        if (array != null) {
            mDragMaxWidth = array.getDimensionPixelSize(R.styleable.DrawerLayout_dragMaxWidth, DEFAULT_MAX_WIDTH);
            mDragViewResId = array.getResourceId(R.styleable.DrawerLayout_dragView, -1);
            mContentViewResId = array.getResourceId(R.styleable.DrawerLayout_contentView, -1);
            mMaskColor = array.getColor(R.styleable.DrawerLayout_maskColor, DEFAULT_MASK_COLOR);
            mIsMaskEnable = array.getBoolean(R.styleable.DrawerLayout_maskEnable, true);//default enable
            array.recycle();
        }

        mMaskView = new View(context);
        mMaskView.setOnClickListener(this);
        mMaskView.setBackgroundColor(mMaskColor);
        mMaskView.setVisibility(GONE);

        //实例化ViewDragHelper
        mViewDragHelper = ViewDragHelper.create(this, 1.0f, new DragHelperCallBack());
        //设置只能在右边距滑动时处理事件
        mViewDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_RIGHT);

        mDragViewLeft = mScreenWidth = context.getResources().getDisplayMetrics().widthPixels;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        if (mDragViewResId != -1) {
            mDragView = findViewById(mDragViewResId);
//            ViewGroup.LayoutParams params = mDragView.getLayoutParams();
//            params.width = mDragMaxWidth;
//            params.height = getMeasuredHeight();
//            mDragView.setLayoutParams(params);
        }
        if (mContentViewResId != -1) {
            mContentView = findViewById(mContentViewResId);
        }

        if (mMaskView != null) {
            addView(mMaskView);
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
//        Log.i(TAG, "onMeasure happened!");
//        measureChildren(widthMeasureSpec, heightMeasureSpec);

        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);

        final int childCount = getChildCount() - (mMaskView == null ? 0 : 1);//减去maskView

        if (childCount != 2) {//除了maskView,直接的子view只能有2个
            throw new IllegalArgumentException("Drawer layout must have exactly 2 children!");
        }

        if (mDragMaxWidth > mScreenWidth) {//不允许最大的拖动距离大于屏幕的宽度
            throw new IllegalArgumentException("Drawer width can't be great than screen width!");
        }


        //TODO 在有padding margin值的时候可能宽高度不对

        //处理dragView
        ViewGroup.LayoutParams dragParams = mDragView.getLayoutParams();
        int mDragViewWidthSpec;
        int mMaskViewWidthSpec;
        if (dragParams.width == LayoutParams.WRAP_CONTENT) {
            mDragViewWidthSpec = MeasureSpec.makeMeasureSpec(mDragMaxWidth, MeasureSpec.AT_MOST);
            mMaskViewWidthSpec = MeasureSpec.makeMeasureSpec(maxWidth - mDragMaxWidth, MeasureSpec.AT_MOST);
        } else if (dragParams.width == LayoutParams.MATCH_PARENT) {
            mDragViewWidthSpec = MeasureSpec.makeMeasureSpec(mDragMaxWidth, MeasureSpec.EXACTLY);
            mMaskViewWidthSpec = MeasureSpec.makeMeasureSpec(maxWidth - mDragMaxWidth, MeasureSpec.EXACTLY);
        } else {
            mDragMaxWidth = dragParams.width;
            mDragViewWidthSpec = MeasureSpec.makeMeasureSpec(dragParams.width, MeasureSpec.EXACTLY);
            mMaskViewWidthSpec = MeasureSpec.makeMeasureSpec(maxWidth - dragParams.width, MeasureSpec.EXACTLY);
        }
        mDragView.measure(mDragViewWidthSpec, heightMeasureSpec);

        //处理ContentView
        LayoutParams contentParams = mContentView.getLayoutParams();
        //强制contentView的宽高都是 MATCH_PARENT
        mContentView.measure(widthMeasureSpec, heightMeasureSpec);
        if (contentParams.width != LayoutParams.MATCH_PARENT && contentParams.height == LayoutParams.MATCH_PARENT) {
            throw new IllegalArgumentException("Content View width/height must be MATCH_PARENT");
        }

        //处理maskView
        mMaskView.measure(mMaskViewWidthSpec, heightMeasureSpec);

        setMeasuredDimension(maxWidth, maxHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        mContentView.layout(0, 0, r, b);
        mDragView.layout(mDragViewLeft, 0, mDragViewLeft + mDragMaxWidth, b);
        mMaskView.layout(0, 0, mDragViewLeft, b);
//        Log.i(TAG, "onLayout->mDragViewLeft->" + mDragViewLeft);
        bringChildToFront(mDragView);//强制bring到前面
    }

    //ViewDragHelper全权处理触摸事件
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = MotionEventCompat.getActionMasked(ev);
        if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
            mViewDragHelper.cancel();
            return false;
        }

        float moveX = ev.getX();
        float moveY = ev.getY();
        switch (action) {
            case MotionEvent.ACTION_DOWN:
                mInitMotionX = moveX;
                mInitMotionY = moveY;
                break;
            case MotionEvent.ACTION_MOVE:
                float adx = Math.abs(moveX - mInitMotionX);
                float ady = Math.abs(moveY - mInitMotionY);
                int touchSlop = mViewDragHelper.getTouchSlop();
                //必须是横向滚动 其他情况无效
                if (ady > adx && adx > touchSlop) {
                    mViewDragHelper.cancel();
                    return false;
                }
                break;
        }

        return mViewDragHelper.shouldInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mViewDragHelper.processTouchEvent(event);
        float moveX = event.getX();
        float moveY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mInitMotionX = moveX;
                mInitMotionY = moveY;
                break;
            case MotionEvent.ACTION_UP:
                if (mDragViewLeft > (getWidth() - mDragMaxWidth / 2)) {
                    closeDrawer();
                } else {
                    if (mReleaseView != null && mReleaseView == mDragView) {
                        mReleaseView = null;
                        Log.i(TAG, "onTouchEvent openDrawer");
                        openDrawer();
                    }
                }
                break;
        }

        return true;
    }

    @Override
    public void computeScroll() {
        /**
         * viewDragHelper响应{@link android.widget.Scroller Scroller}
         */
        if (mViewDragHelper.continueSettling(true)) {
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mMaskView) {
            if (isDrawerOpen()) {
                closeDrawer();
            }
        }
    }


    private class DragHelperCallBack extends ViewDragHelper.Callback {

        @Override
        public boolean tryCaptureView(View child, int pointerId) {
            return mDragView == child;
        }

        @Override
        public void onEdgeDragStarted(int edgeFlags, int pointerId) {
            //只监听右边距向内滑动
            if (edgeFlags == ViewDragHelper.EDGE_RIGHT) {
                Log.i(TAG, "onEdgeDragStarted:Left edge drag start");
                mMaskView.setVisibility(VISIBLE);
                if (!mIsMaskEnable) {//不允许mask渐变动画的时候,alpha设置0 可点击
                    mMaskView.setAlpha(0);
                }
                mViewDragHelper.captureChildView(mDragView, pointerId);
            }
        }


        @Override
        public int clampViewPositionHorizontal(View child, int left, int dx) {
            if (child == mDragView) {
//                Log.i(TAG, "clampViewPositionHorizontal");
                float rectLeft = getWidth() - mDragMaxWidth;
                float rectRight = getWidth();
                //保证mDragView只能在 getWidth() - mDragMaxWidth 到 getWidth的范围内滚动
                //滚动边界控制
                return (int) Math.min(Math.max(left, rectLeft), rectRight);
            } else {
                return left;
            }
        }

        @Override
        public int getViewHorizontalDragRange(View child) {
            return mDragMaxWidth;
        }

        @Override
        public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {

            if (changedView == mDragView) {
                mDragViewLeft = left;
                mDragRatio = (float) (getWidth() - left) / mDragMaxWidth;
                if (null != mDragRatioListener) {
                    mDragRatioListener.onDragRatioChange(mDragRatio, mDragView);
                }

                if (mIsMaskEnable) {
                    if (mDragRatio == 0) {
                        mMaskView.setVisibility(GONE);
                    } else if (mDragRatio == 1) {
                        mMaskView.setVisibility(VISIBLE);
                    }
                    mMaskView.setAlpha(mDragRatio);
                }
//                Log.i(TAG, "onViewPositionChanged left->" + left);
//                Log.i(TAG, "onViewPositionChanged width->" + getWidth());
//                Log.i(TAG, "onViewPositionChanged mDragMaxWidth->" + mDragMaxWidth);
//                Log.i(TAG, "onViewPositionChanged ratio->" + mDragRatio);
                requestLayout();
            }
        }

        @Override
        public void onViewReleased(View releasedChild, float xvel, float yvel) {
            mReleaseView = releasedChild;
            if (releasedChild == mDragView) {
                Log.i(TAG, "onViewReleased");
                //  速率  左 -> 右 正
                //  速率  右 -> 左 负
                if (xvel < 0 || (xvel == 0 && mDragRatio > 0.5f)) {
                    mViewDragHelper.settleCapturedViewAt(getWidth() - mDragMaxWidth, 0);
                } else {
                    mViewDragHelper.settleCapturedViewAt(getWidth(), 0);
                }
                invalidate();
            }
        }

        @Override
        public void onViewDragStateChanged(int state) {
            super.onViewDragStateChanged(state);
        }

        @Override
        public void onViewCaptured(View capturedChild, int activePointerId) {
            super.onViewCaptured(capturedChild, activePointerId);
        }
    }

    /**
     * 设置mask背景的颜色
     *
     * @param maskColor 颜色id
     * @see DrawerLayout#mMaskColor
     */
    public void setMaskColor(@ColorInt int maskColor) {
        this.mMaskColor = maskColor;
        if (mMaskView != null) {
            mMaskView.setBackgroundColor(maskColor);
        }
    }

    /**
     * 是否允许mask渐变
     *
     * @param enable 默认true
     * @see DrawerLayout#mIsMaskEnable
     */
    public void setMaskEnable(boolean enable) {
        this.mIsMaskEnable = enable;
    }

    /**
     * 动态刷新{@link DrawerLayout#mDragMaxWidth}可拖动的最大宽度
     * NOTE:不能大于屏幕宽度
     * 如果{@link DrawerLayout#mDragView}的布局参数{@link android.view.ViewGroup.LayoutParams#width}
     * 的值等于一个确切的值,则配置的{@link DrawerLayout#mDragMaxWidth}无效
     *
     * @param dragMaxWidth 拖动的最大宽度(距离)
     * @see DrawerLayout#onMeasure(int, int)
     */
    public void setDragMaxWidth(int dragMaxWidth) {
        this.mDragMaxWidth = dragMaxWidth;
        requestLayout();
    }

    /**
     * 开启{@link DrawerLayout#mDragView}
     */
    public void openDrawer() {
        mMaskView.setVisibility(VISIBLE);
        if (!mIsMaskEnable) {//不允许mask渐变动画的时候,alpha设置0 可点击
            mMaskView.setAlpha(0);
        }
        smoothSlideToEdge(false);
    }

    /**
     * 关闭{@link DrawerLayout#mDragView}
     */
    public void closeDrawer() {
        smoothSlideToEdge(true);
    }

    /**
     * 判断{@link DrawerLayout#mDragView}是否显示
     */
    public boolean isDrawerOpen() {
        return mDragViewLeft < (getWidth() - mDragMaxWidth / 2);
    }

    /**
     * 设置开启{@link DrawerLayout#mDragView}过程中拖动的比例{@link DrawerLayout#mDragRatio}
     * {@link DrawerLayout.DragRatioListener}
     */
    public void setDragRatioListener(DragRatioListener listener) {
        this.mDragRatioListener = listener;
    }

    /**
     * {@link DrawerLayout#mDragView}执行slide动画到 toEdge
     * 主要是调用{@link ViewDragHelper#smoothSlideViewTo(View, int, int)}方法来滚动view
     *
     * @param toEdge 是否滚到边界
     */
    private void smoothSlideToEdge(boolean toEdge) {
        //根据实际情况考虑padding值是否算在内
//        final int leftBound = getPaddingLeft();
        int offset = 0;
        if (!toEdge) {
            offset = getWidth() - mDragMaxWidth;
        } else {
            offset = getWidth();
        }

        /**
         * 如果继续执行{@link ViewDragHelper#continueSettling(boolean)}
         * 在下一帧来之前手动invalidate一次
         * */
        if (mViewDragHelper.smoothSlideViewTo(mDragView, offset, mDragView.getTop())) {
            ViewCompat.postInvalidateOnAnimation(this);//否则可能会失真
        }
    }


    /**
     * 拖拽比例的监听
     */
    public interface DragRatioListener {
        /**
         * 拖拽比例变化回调
         *
         * @param ratio    拖拽比
         * @param dragView 监听的view
         */
        void onDragRatioChange(float ratio, View dragView);
    }
}
