
##DEMO EFFECT
![DEMO](./demo.gif)


##ViewDragHelper使用总结

1.实例化ViewDragHelper

```java

        //实例化ViewDragHelper,1.0代表灵敏度,越高越灵敏
        mViewDragHelper = ViewDragHelper.create(this, 1.0f, new DragHelperCallBack());
        
        //设置处理哪边边界的滑动事件..可以不自定义
        mViewDragHelper.setEdgeTrackingEnabled(ViewDragHelper.EDGE_RIGHT);
```

2.在 ```onInterceptTouchEvent()``` 让```ViewDragHelper```处理事件截断

```java

          @Override
           public boolean onInterceptTouchEvent(MotionEvent ev) {
               final int action = MotionEventCompat.getActionMasked(ev);
               if (action == MotionEvent.ACTION_CANCEL || action == MotionEvent.ACTION_UP) {
                   mViewDragHelper.cancel();
                   return false;
               }
       
               //中间处理自己的逻辑,也可自己判断是否要截断事件
       
               return mViewDragHelper.shouldInterceptTouchEvent(ev);//这个是必须条件,也可加上自己的条件
           }

```

3.在```onTouchEvent()```使```ViewDragHelper```处理touch事件

```java

    @Override
       public boolean onTouchEvent(MotionEvent event) {
           //让ViewragHelper处理touch事件
           mViewDragHelper.processTouchEvent(event);
           //可以自定义自己对event的处理
   
           return true;
       }
```

4.在CallBack中处理自己想处理的所有

```java
     
     private class DragHelperCallBack extends ViewDragHelper.Callback {
     
             @Override
             public boolean tryCaptureView(View child, int pointerId) {
                 //返回需要被监听的view
                 return mDragView == child;
             }
             
             @Override
              public void onViewCaptured(View capturedChild, int activePointerId) {
                   super.onViewCaptured(capturedChild, activePointerId);
                   //当被监听的view设置成功后,处理view的一些参数等 whatever you want
              }
     
             @Override
             public void onEdgeDragStarted(int edgeFlags, int pointerId) {
                 //根据不同的边界的滚动触发 可以选择capture不同的view
                 if (edgeFlags == ViewDragHelper.EDGE_RIGHT) {
                     mViewDragHelper.captureChildView(mDragView, pointerId);
                 }
             }
             
     
             @Override
             public int clampViewPositionHorizontal(View child, int left, int dx) {
                //当需要处理水平滚动的时候,该函数往往用来确定child滚动的区域 
                //同理clampViewPositionVertical 处理垂直滚动的区域
                 if (child == mDragView) {
                     float rectLeft = getWidth() - mDragMaxWidth;
                     float rightRight = getWidth();
                     //保证mDragView只能在 getWidth() - mDragMaxWidth 到 getWidth的范围内滚动
                     //下面的方法在控制边界代码比较高效
                     return (int) Math.min(Math.max(left, rectLeft), rightRight);
                 } else {
                     return left;
                 }
             }
     
             @Override
             public int getViewHorizontalDragRange(View child) {
             //水平滚动最大的距离
                 return mDragMaxWidth;
             }
             
              @Override
              public int getViewVerticalDragRange(View child) {
                  //垂直滚动最大的距离
                 return mDragMaxHeight;
              }
     
             @Override
             public void onViewPositionChanged(View changedView, int left, int top, int dx, int dy) {
     
                 /**
                 该回调会在view位置发生变化时回调、
                 可以通过changedView不同值来处理不同的逻辑
                 往往会调用requestLayout();来刷新布局
                 
                 处理水平滚动时 往往使用 left来判断位置
                 处理垂直滚动式 往往使用 top来判断位置
                 可以在此计算拖拽的比例
                 */
             }
     
             @Override
             public void onViewReleased(View releasedChild, float xvel, float yvel) {
                  //  xvel速率  左 -> 右 正
                  //  xvel速率  右 -> 左 负
                 /**
                   当被Capture的视图释放的时候回调
                   通常会调用  mViewDragHelper.settleCapturedViewAt(getWidth(), 0);
                   方法来将被释放的view动画移动到最终的left,top点
                 */
             }
     
             @Override
             public void onViewDragStateChanged(int state) {
                 super.onViewDragStateChanged(state);
                 //拖拽的状态变化时回调
             }
     
         }
  
```

5.通常重载 ```ViewGroup``` 的 ```onComputeScroll()```方法

```java
  
      @Override
      public void computeScroll() {
          /**
           * viewDragHelper响应{@link android.widget.Scroller Scroller}
           */
          if (mViewDragHelper.continueSettling(true)) {//继续执行释放逻辑
              //在下一帧来之前手动Invalidate一下,避免卡帧
              ViewCompat.postInvalidateOnAnimation(this);
          }
      }

```

6.手动smooth滚动View到指定位置的操作

```java
   
     /**
          * 
          * 主要是调用{@link ViewDragHelper#smoothSlideViewTo(View, int, int)}方法来滚动view
          */
         private void smoothSlideTo() {
            
             /**
              * 如果继续执行{@link ViewDragHelper#continueSettling(boolean)}
              * 在下一帧来之前手动invalidate一次
              * */
             if (mViewDragHelper.smoothSlideViewTo(mDragView, offset, mDragView.getTop())) {
                 ViewCompat.postInvalidateOnAnimation(this);
             }
         }
  
```