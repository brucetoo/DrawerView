package com.brucetoo.drawerview;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
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

        //Init ViewDragHelper
        mViewDragHelper = ViewDragHelper.create(this, 1.0f, new DragHelperCallBack());
        //Only care about swipe from EDGE_RIGHT
        mViewDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_RIGHT);

        //left bound of dragView default equals screen width
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
//        measureChildren(widthMeasureSpec, heightMeasureSpec);//ignore this,measure children separately

        //get the extra width and height with spec
        int maxWidth = MeasureSpec.getSize(widthMeasureSpec);
        int maxHeight = MeasureSpec.getSize(heightMeasureSpec);

        final int childCount = getChildCount() - (mMaskView == null ? 0 : 1);//except maskView

        if (childCount != 2) {//only have 2 children except maskView
            throw new IllegalArgumentException("Drawer layout must have exactly 2 children!");
        }

        if (mDragMaxWidth > mScreenWidth) {//dragMaxWidth can't be greater than screen width
            throw new IllegalArgumentException("Drawer width can't be greater than screen width!");
        }


        //TODO if there are padding or margin values in this ViewGroup,may has some problems

        /**
         handle {@link mDragView} with layoutParams.
         NOTE:if {@link mDragView} has exactly width,{@link mDragMaxWidth} is not available
         */
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
        //measure {@link mDragView}
        mDragView.measure(mDragViewWidthSpec, heightMeasureSpec);

        /**
         * handle {@link mContentView}
         * NOTE: the height and width must be MATCH_PARENT
         */
        LayoutParams contentParams = mContentView.getLayoutParams();
        mContentView.measure(widthMeasureSpec, heightMeasureSpec);
        if (contentParams.width != LayoutParams.MATCH_PARENT && contentParams.height == LayoutParams.MATCH_PARENT) {
            throw new IllegalArgumentException("Content View width/height must be MATCH_PARENT");
        }

        /**
         * handle {@link mMaskView}
         */
        mMaskView.measure(mMaskViewWidthSpec, heightMeasureSpec);

        setMeasuredDimension(maxWidth, maxHeight);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {

        mContentView.layout(0, 0, r, b);
        mDragView.layout(mDragViewLeft, 0, mDragViewLeft + mDragMaxWidth, b);
        mMaskView.layout(0, 0, mDragViewLeft, b);
//        Log.i(TAG, "onLayout->mDragViewLeft->" + mDragViewLeft);
        bringChildToFront(mDragView);//force bring dragView to front in case the wrong z value.
    }

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
                //we only care about horizontal scroll
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
        //let ViewDragHelper to handle touch event
        mViewDragHelper.processTouchEvent(event);
        float moveX = event.getX();
        float moveY = event.getY();
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mInitMotionX = moveX;
                mInitMotionY = moveY;
                break;
            case MotionEvent.ACTION_UP:
                //set open or close dragView according to mDragViewLeft when we ACTION_UP
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
         * viewDragHelper response {@link android.widget.Scroller Scroller}
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
            //only handle swipe right to left at right edge
            if (edgeFlags == ViewDragHelper.EDGE_RIGHT) {
                Log.i(TAG, "onEdgeDragStarted:Left edge drag start");
                mMaskView.setVisibility(VISIBLE);
                if (!mIsMaskEnable) {//when disable mask animation,we need alpha==0 so it can be triggered
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
                //mDragView's drag bounds,left range must in rectLeft < left < rectRight
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
                //  horizontal x velocity:  left -> right  >0
                //  horizontal x velocity:  right -> left  <0
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
     * set the color of {@link DrawerLayout#mMaskView}
     *
     * @param maskColor color
     * @see DrawerLayout#mMaskColor
     */
    public void setMaskColor(@ColorInt int maskColor) {
        this.mMaskColor = maskColor;
        if (mMaskView != null) {
            mMaskView.setBackgroundColor(maskColor);
        }
    }

    /**
     * set enable mask animation or not
     *
     * @param enable default is true
     * @see DrawerLayout#mIsMaskEnable
     */
    public void setMaskEnable(boolean enable) {
        this.mIsMaskEnable = enable;
    }

    /**
     * set the max value of {@link DrawerLayout#mDragMaxWidth}
     * NOTE:dragMaxWidth can't be greater than screen width
     * When {@link DrawerLayout#mDragView} has extra width,
     * {@link DrawerLayout#mDragMaxWidth} will be invalid
     *
     * @param dragMaxWidth max width can be dragged
     * @see DrawerLayout#onMeasure(int, int)
     */
    public void setDragMaxWidth(int dragMaxWidth) {
        this.mDragMaxWidth = dragMaxWidth;
        requestLayout();
    }

    /**
     * Open {@link DrawerLayout#mDragView}
     */
    public void openDrawer() {
        mMaskView.setVisibility(VISIBLE);
        if (!mIsMaskEnable) {
            mMaskView.setAlpha(0);
        }
        smoothSlideToEdge(false);
    }

    /**
     * Close {@link DrawerLayout#mDragView}
     */
    public void closeDrawer() {
        smoothSlideToEdge(true);
    }

    /**
     * get if {@link DrawerLayout#mDragView} is opened
     */
    public boolean isDrawerOpen() {
        return mDragViewLeft < (getWidth() - mDragMaxWidth / 2);
    }

    /**
     * set {@link DrawerLayout#mDragView} drag listener with {@link DrawerLayout#mDragRatio} callback
     *
     * @see DrawerLayout.DragRatioListener
     */
    public void setDragRatioListener(DragRatioListener listener) {
        this.mDragRatioListener = listener;
    }

    /**
     * smooth move {@link DrawerLayout#mDragView} to exact value with duration
     *
     * @param toWidth  new width of {@link DrawerLayout#mDragView}
     * @param duration duration about animation
     */
    public void smoothSlideTo(final int toWidth, long duration) {
        if (toWidth != mDragViewLeft) {

            ValueAnimator dragAnim = ValueAnimator.ofInt(mDragMaxWidth, toWidth);
            dragAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    int value = (int) animation.getAnimatedValue();
                    mDragView.getLayoutParams().width = value;
                    mDragView.requestLayout();

                    mDragMaxWidth = value;
                    mDragViewLeft = mScreenWidth - value;
                    requestLayout();
                }
            });
            dragAnim.setDuration(duration);
            dragAnim.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    mDragMaxWidth = mScreenWidth - toWidth;
                }
            });
            dragAnim.start();
        }
    }

    /**
     * smooth slide {@link DrawerLayout#mDragView} to edge
     * drive by {@link ViewDragHelper#smoothSlideViewTo(View, int, int)} method
     *
     * @param toEdge slide to edge?
     */
    private void smoothSlideToEdge(boolean toEdge) {
        //TODO maybe padding values need be considered
//        final int leftBound = getPaddingLeft();
        int offset = 0;
        if (!toEdge) {
            offset = getWidth() - mDragMaxWidth;
        } else {
            offset = getWidth();
        }

        if (mViewDragHelper.smoothSlideViewTo(mDragView, offset, mDragView.getTop())) {
            //force invalidate before next frame comes.
            ViewCompat.postInvalidateOnAnimation(this);
        }
    }


    /**
     * the listener when drag happened
     */
    public interface DragRatioListener {
        /**
         * callback of ratio changed when drag
         *
         * @param ratio    drag ration
         * @param dragView captured/dragged view
         */
        void onDragRatioChange(float ratio, View dragView);
    }
}
