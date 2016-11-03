package com.cj.dynamicavatarview.ratio;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.LinearInterpolator;
import android.view.animation.ScaleAnimation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import com.cj.dynamicavatarview.R;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Created by chenj on 2016/10/12.
 */
public class RatioLayout extends ViewGroup {

    private static final int OPEN_BUBBLE_TIME = 800;//展开气泡的时间
    private static final int CLOSE_BUBBLE_TIME = 800;//关闭气泡的时间

    private static final int BUBBLE_ENTER_CENTER_SCALE_TIME = 200;//气泡进入中心后，松开缩放的时间
    private static final int BUBBLE_TRANSLATION_HOMING_TIME = 400;//气泡被移动后归位的时间

    private static final int LOVE_XIN_NUMBER = 5;//爱心的数量

    private int mWidth;//容器的宽
    private int mHeight;//容器的高
    private int mMinSize;//宽和高中最小值

    private int mMinBubbleHeight;//最小气泡的高
    private int mMinBubbleWidth;//最小气泡的宽
    private int mMaxBubbleHeight;//最大气泡的高
    private int mMaxBubbleWidth;//最大气泡的宽

    private DynamicAvatarView mImageView;

    //private boolean mIsFinishFirst;

    private List<BubbleView> mTextViews = new ArrayList<>();
    private List<String> mTexts = new ArrayList<>();

    //圆的中心坐标
    private RatioFrame mRatioFrameCenter = null;
    //每个气泡的布局坐标
    private List<RatioFrame> mRatioFrameList = new ArrayList<>();
    //当前气泡显示的位置
    private List<RatioFrame> mCurrentRatioFrameList;
    //ValueAnimator不会针对某个View做动画，它只是一个值对象到另一个值对象的平缓变化的过程
    private ValueAnimator mAnimatorEnetr;
    private ValueAnimator mAnimatorExit;

    private boolean mIsBubbleOpen;//气泡是否展开，只有展开才会触发触摸事件
    private boolean mIsBubbleHoming;//气泡是否在归位的途中，只有归位之后才会触发触摸事件，不然会导致再次按下时，暂停补了浮动动画
    //private List<AnimatorSet> mAnimatorSetList = new ArrayList<>();
    private Map<Integer,AnimatorSet> mAnimatorSetList = new HashMap<>();

    private boolean mIsHaveBubbleDown ;//是否有气泡被按下，依次只能按下一个气泡

    private boolean mIsEnter;//是否进入气泡

    private List<ImageView> mLoveXinList;

    private boolean mIsPlayLoveXin;//是否在播放爱心动画，播放时不可以触发了
    private TextView mAddOneText;

    private boolean mIsPlayAnimLoveXin;//气泡进入中心点后是否执行爱心动画

    public RatioLayout(Context context) {
        this(context,null);
    }

    public RatioLayout(Context context, AttributeSet attrs) {
        this(context, attrs,0);
    }

    public RatioLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mLoveXinList = new ArrayList<>();
        for(int i=0;i<LOVE_XIN_NUMBER;i++){
            ImageView imageView = new ImageView(context);
            imageView.setImageResource(R.mipmap.xin);
            mLoveXinList.add(imageView);
            addView(imageView);
            imageView.setVisibility(View.GONE);
        }

    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int minSize = Math.min(widthSize,heightSize);
        //取最小值当作显示区域
        mMinSize = minSize;
        //初始化气泡的最大最小值
        mMinBubbleWidth = mMinBubbleHeight = minSize / 7;
        mMaxBubbleHeight = mMaxBubbleWidth = minSize / 6;

        mWidth = widthSize;
        mHeight = heightSize;

        //因为每个气泡的大小是不一样的，所以只记下中心点，然后可在RatioFrameEvaluator中计算每个气泡在中心的布局坐标
        mRatioFrameCenter = new RatioFrame(mWidth / 2,mHeight / 2,mWidth / 2,mHeight / 2);

        int childCount = getChildCount();
        //mTextViews.clear();

        for(int i=0;i<childCount;i++){
            View child = getChildAt(i);
            if(child instanceof DynamicAvatarView){
                mImageView = (DynamicAvatarView) child;
                mImageView.measure(MeasureSpec.makeMeasureSpec(widthSize / 2 , widthMode),MeasureSpec.makeMeasureSpec(heightSize / 2 , heightMode));
                // System.out.println("-------------------------DynamicAvatarView");
            }
            if(child instanceof BubbleView){
                BubbleView textView = (BubbleView) child;
                //mTextViews.add(textView);
                int textLength = textView.getText().toString().length();
                int bubbleSize = (int)(mMinBubbleWidth + (mMaxBubbleWidth - mMinBubbleWidth) * (textLength / 10.0f));
                //给TextView设置大小
                textView.measure(MeasureSpec.makeMeasureSpec(bubbleSize , widthMode),MeasureSpec.makeMeasureSpec(bubbleSize , heightMode));
                // System.out.println("-------------------------textView:"+textView.getText().toString());
            }
        }

        //爱心View
        for (int i = 0; i < mLoveXinList.size(); i++) {
            ImageView imageView = mLoveXinList.get(i);
            imageView.measure(MeasureSpec.makeMeasureSpec(mMinSize / 4 / 2 , MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(mMinSize / 4 / 2, MeasureSpec.EXACTLY));
            //将mLoveXin显示在Group的所有子视图之上
            //bringChildToFront(mLoveXin);
            imageView.bringToFront();
        }

        //计算外圆的气泡的布局位置
        calculateRatioFrame(mTextViews);

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

    }


    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        if(mImageView == null) return;

        int width = mImageView.getMeasuredWidth();
        int height = mImageView.getMeasuredHeight();
        int left = mWidth / 2 - width / 2;
        int top = mHeight / 2 - height / 2;
        int right = mWidth / 2 + width / 2;
        int bottom = mHeight / 2 + height / 2;

        mImageView.layout(left,top,right,bottom);

        for (int i = 0; i < mLoveXinList.size(); i++) {
            ImageView imageView = mLoveXinList.get(i);
            left = mWidth / 2 + width / 4 - imageView.getMeasuredWidth() / 2;
            bottom = mHeight / 2 + height / 3;
            top = bottom - imageView.getMeasuredHeight();
            right = left + imageView.getMeasuredWidth();

//            System.out.println("-------------------------left:"+left);
//            System.out.println("-------------------------top:"+top);
//            System.out.println("-------------------------right:"+right);
//            System.out.println("-------------------------bottom:"+bottom);

            imageView.layout(left,top,right,bottom);
        }


        for (int i = 0; i < mTextViews.size(); i++) {

            TextView textView = mTextViews.get(i);

            //RatioFrame ratioFrame = mRatioFrameList.get(i);
            if(mCurrentRatioFrameList != null){
                RatioFrame ratioFrame = mCurrentRatioFrameList.get(i);
                textView.layout(ratioFrame.mLeft,ratioFrame.mTop,ratioFrame.mRight,ratioFrame.mBottom);
            }

        }
        //初始化气泡进入动画
        setBubbleEnterFinishListener();
        //初始化气泡退出动画
        setBubbleExitFinish();

        //System.out.println("-------------------mRatioFrameList:"+ mRatioFrameList);
    }

    /**
     * 计算气泡的布局位置
     * @param textViews
     */
    private void calculateRatioFrame(List<BubbleView> textViews){
        if(textViews.size() == 0) return;
        mRatioFrameList.clear();

        double angle = 0;
        double grad = Math.PI * 2 / textViews.size();//梯度，每个TextView之间的角度 (Math.PI 是数学中的90°)
        double rightAngle = Math.PI / 2;

        int cx = mWidth / 2;//容器中心x坐标
        int cy = mHeight / 2;//容器中心y坐标
        //System.out.println("----------------cx:"+cx);
        //System.out.println("----------------cy:"+cy);
        int radius = mMinSize / 2 / 2 / 2 + mMinSize / 2 / 2 ;//动态气泡的组成圆的半径
        //System.out.println("-----------------radius:"+radius);
        int left = 0;
        int top = 0;
        int right = 0;
        int bottom = 0;
        int a = 0,b = 0;
        //int r = mMinSize / 6 / 2;//气泡半径
        for (int i = 0; i < textViews.size(); i++) {
            int r = textViews.get(i).getMeasuredWidth() / 2;//计算得来//固定死的mMinSize / 6 / 2;//气泡半径
            if(angle >= 0 && angle < rightAngle){  //0 - 90
                //保持角度在 0 - 90
                a = (int)(radius * Math.sin(Math.abs(angle % rightAngle)));
                b = (int)(radius * Math.cos(Math.abs(angle % rightAngle)));

                left = cx + a - r;
                top = cy - b - r;
                right = left + 2 * r;
                bottom = top + 2 * r;
                //System.out.println("-----------0 - 90---1--a:"+a);
                //System.out.println("-----------0 - 90---1--b:"+b);

            }else if(angle >= rightAngle && angle < rightAngle * 2){ // 90 - 180
                a = (int)(radius * Math.sin(Math.abs(angle % rightAngle)));
                b = (int)(radius * Math.cos(Math.abs(angle % rightAngle)));
                left = cx + b - r;
                top = cy + a - r;
                right = left + 2 * r;
                bottom = top + 2 * r;
                //System.out.println("-----------90 - 180---1--a:"+a);
                //System.out.println("-----------90 - 180---1--b:"+b);

            }else if(angle >= rightAngle * 2 && angle < rightAngle * 3){ // 180 - 270
                a = (int)(radius * Math.sin(Math.abs(angle % rightAngle)));
                b = (int)(radius * Math.cos(Math.abs(angle % rightAngle)));
                left = cx - a - r;
                top = cy + b - r;
                right = left + 2 * r;
                bottom = top + 2 * r;
                //System.out.println("-----------180 - 270---1--a:"+a);
                //System.out.println("-----------180 - 270---1--b:"+b);
            }else if(angle >= rightAngle * 3 && angle < rightAngle * 4){ //270 - 360
                a = (int)(radius * Math.sin(Math.abs(angle % rightAngle)));
                b = (int)(radius * Math.cos(Math.abs(angle % rightAngle)));
                left = cx - b - r;
                top = cy - a - r;
                right = left + 2 * r;
                bottom = top + 2 * r;
                //System.out.println("-----------270 - 360---1--a:"+a);
                // System.out.println("-----------270 - 360---1--b:"+b);
            }

            //System.out.println("--------------1--left:"+left);
            //System.out.println("--------------1--top:"+top);
            //System.out.println("--------------1--right:"+right);
            //System.out.println("--------------1--bottom:"+bottom);

            mRatioFrameList.add(new RatioFrame(left,  top, right,bottom,angle));
            //角度再加一个梯度值
            angle += grad;

        }
    }


//    public void setBubbleBackground(){
//        //view.setBackgroundResource(R.drawable.textview_shape_blue);
//    }

    private TranslateAnimation getTranslateAnimation(int time,int x,int y){
        TranslateAnimation translateAnimation = new TranslateAnimation(0, x, 0, y);
        translateAnimation.setDuration(time);
        translateAnimation.setRepeatCount(0);
        translateAnimation.setRepeatMode(Animation.RESTART);
        return translateAnimation;
    }

    private ScaleAnimation getScaleAnimation(int time){
        ScaleAnimation scaleAnimation = new ScaleAnimation(0.3f, 1, 0.3f, 1, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scaleAnimation.setDuration(time);
        scaleAnimation.setRepeatCount(0);
        scaleAnimation.setRepeatMode(Animation.RESTART);
        return scaleAnimation;
    }

    private AlphaAnimation getAlphaAnimation(int time,float toAlpha){
        AlphaAnimation alphaAnimation = new AlphaAnimation(0.99f, toAlpha);
        // 设置动画的时长，单位毫秒
        alphaAnimation.setDuration(time);
        // 设置动画循环次数，这里设置成了无限循环
        alphaAnimation.setRepeatCount(0);
        alphaAnimation.setRepeatMode(Animation.RESTART);
        return alphaAnimation;
    }

    /**
     * 播放爱心动画
     */
    public void playCenterLoveXin(){
        mIsPlayLoveXin = true;
        for (int i = 0; i < mLoveXinList.size(); i++) {
            ImageView imageView = mLoveXinList.get(i);
            playCenterLoveXin(imageView,i*200 ,i);
        }

    }

    /**
     * 给执行的View设置组合动画
     * @param view 要播放的View
     * @param time 延时多久播放
     */
    private void playCenterLoveXin(final View view ,int time,final int position){
        postDelayed(new Runnable() {
            @Override
            public void run() {

                view.setVisibility(View.VISIBLE);
                AnimationSet mAnimationSet = new AnimationSet(false);
                mAnimationSet.addAnimation(getTranslateAnimation(500,0,px2dp(-view.getHeight() / 5 * 3)));
                mAnimationSet.addAnimation(getScaleAnimation(500));
                mAnimationSet.addAnimation(getAlphaAnimation(500,0.0f));
                mAnimationSet.setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        view.setVisibility(View.GONE);
                        //播放到最后一个时，mIsPlayLoveXin播放动画置为false
                        if(position == mLoveXinList.size()-1){
                            mIsPlayLoveXin = false;
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                view.setAnimation(mAnimationSet);
                mAnimationSet.start();
            }
        }, time);
    }

    /**
     * 创建+1的View并设置动画
     * @param viewLocation
     */
    private void addOneAnim(View viewLocation){
        if(mAddOneText == null) {
            mAddOneText = new TextView(getContext());
            mAddOneText.setText("+1");
            mAddOneText.setTextSize(16);
            mAddOneText.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD));
            mAddOneText.getPaint().setFakeBoldText(true);
            mAddOneText.setTextColor(Color.BLACK);
            mAddOneText.setBackgroundColor(Color.TRANSPARENT);
            LayoutParams layoutParams = new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.WRAP_CONTENT);
            mAddOneText.setLayoutParams(layoutParams);

            addView(mAddOneText);
            mAddOneText.bringToFront();
        }

        mAddOneText.measure(0,0);
        int left = viewLocation.getLeft() + viewLocation.getWidth() / 5 * 3;
        int bottom = viewLocation.getBottom() - viewLocation.getHeight() / 5;
        int top = bottom - mAddOneText.getMeasuredHeight() ;
        int right = left + mAddOneText.getMeasuredWidth();

        mAddOneText.layout(left,top,right,bottom);

        setAddOneAnim(mAddOneText);
    }

    /**
     * 设置动画
     * @param view
     */
    private void setAddOneAnim(final View view) {
        AnimationSet mAnimationSet = new AnimationSet(false);
        mAnimationSet.addAnimation(getTranslateAnimation(1000,px2dp(5),px2dp(-view.getHeight() / 5 * 4)));
        mAnimationSet.addAnimation(getScaleAnimation(1000));
        mAnimationSet.addAnimation(getAlphaAnimation(1000,0.4f));
        mAnimationSet.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
                view.setVisibility(View.VISIBLE);
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                view.setVisibility(View.GONE);

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        view.setAnimation(mAnimationSet);
        mAnimationSet.start();
    }

    /**
     * 设置气泡的文本
     * @param texts
     */
    public void addText(String... texts){
        mTexts.clear();
        mTexts.addAll(Arrays.asList(texts));
        addTextViews();
        requestLayout();
    }

    /**
     * 设置背景
     * @param background
     */
    public void changeTextBackground(boolean... background){

        for (int i = 0; i < background.length; i++) {
            if(background[i]){
                mTextViews.get(i).setBackgroundResource(R.drawable.textview_shape_blue);
            }else{
                mTextViews.get(i).setBackgroundResource(R.drawable.textview_shape);
            }
        }
    }

    /**
     * 改变所有气泡上的值
     * @param texts
     */
    public void changeText(String... texts){
        for (int i = 0; i < mTextViews.size(); i++) {
            TextView textView = mTextViews.get(i);
            textView.setText(getFormatText(texts[i]));
        }
    }

    /**
     * 是否执行爱心动画
     * @param isPlay
     */
    public void setPlayLoveXin(boolean isPlay){
        mIsPlayAnimLoveXin = isPlay;
    }

    /**
     * 根据文本创建TextView
     */
    private void addTextViews(){
        mTextViews.clear();
        for (int i = 0; i < mTexts.size(); i++) {
            String text = mTexts.get(i);
            BubbleView textView = new BubbleView(getContext());
            textView.setText(getFormatText(text));
            textView.setTextColor(Color.WHITE);
            textView.setBackgroundResource(R.drawable.textview_shape);
            textView.setGravity(Gravity.CENTER);
            textView.setTextSize(14);

            setBubbleTouch(i,textView);

            mTextViews.add(textView);
            addView(textView);
        }
    }

    /**
     * 按照指定的格式格式化文本
     * @param text
     * @return
     */
    private String getFormatText(String text){
        StringBuilder sb = new StringBuilder();
        int index = text.indexOf("(");
        String temp = null;
        if(index != -1) {
            temp = text.substring(0, index);
        }else{
            temp = text;
        }
        int length = temp.length();
        if(length <= 3){
            sb.append(temp).append("\n").append(text.substring(index));
        }else if(length <= 5){
            sb.append(temp.substring(0,2)).append("\n").append(temp.substring(2,length)).append("\n").append(text.substring(index));
        }else{
            sb.append(temp.substring(0,2)).append("\n").append(temp.substring(2,5)).append(".").append("\n").append(text.substring(index));

        }
        return sb.toString();
    }

    /**
     * 设置View气泡的触摸事件，让气泡跟着手指移动
     * @param i
     * @param textView
     */
    private void setBubbleTouch(int i,final TextView textView){
        final int touchPoition = i;
        textView.setOnTouchListener(new OnTouchListener() {
            int mStartX = 0;
            int mStartY = 0;
            @Override
            public boolean onTouch(View view, MotionEvent event) {

                //如果气泡没有展开就不会执行触摸事件,在归位途中也不触发，播放爱心动画时也不可触摸
                if(!mIsBubbleOpen || mIsBubbleHoming || mIsPlayLoveXin) return false;

                switch (event.getActionMasked()){
                    case MotionEvent.ACTION_POINTER_DOWN:
                    case MotionEvent.ACTION_POINTER_UP:

                        return false;
                    case MotionEvent.ACTION_DOWN:
                        if(!mIsHaveBubbleDown) {
                            mIsHaveBubbleDown = true;

                            if (mImageView != null) {
                                mImageView.startAnim();
                            }
                            mStartX = (int) event.getRawX();
                            mStartY = (int) event.getRawY();

                            //暂停动画
                            stopBubbleFolat(touchPoition, view);
                        }else {
                            return false;
                        }
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        if(mIsHaveBubbleDown) {
                            mIsHaveBubbleDown = false;

                            if (mImageView != null) {
                                mImageView.stopAnim();
                            }

                            //气泡归位,从当前位置回归到原来位置
                            RatioFrame currentLocation = new RatioFrame(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
                            boolean inPictureCenter = isInPictureCenter(touchPoition, view, currentLocation, mRatioFrameList.get(touchPoition));
                            if (!inPictureCenter) {
                                homingBubbleView(false, touchPoition, view, currentLocation, mRatioFrameList.get(touchPoition));
                                //打开气泡浮动,
                                //startBubbleFloat(touchPoition);
                            }
                            //如果在这里执行startBubbleFloat(touchPoition,view);如果在中心点，就会导致将缩放的动画覆盖掉，从而不会归为，导致停在中心点了

                        }
                        break;
                    case MotionEvent.ACTION_MOVE:
                        int endX = (int) event.getRawX();
                        int endY = (int) event.getRawY();
                        int offsetX = endX - mStartX;
                        int offsetY = endY - mStartY;
                        int left = textView.getLeft() + offsetX;
                        int top = textView.getTop() + offsetY;
                        int right = textView.getRight() + offsetX;
                        int bottom = textView.getBottom() + offsetY;
                        textView.layout(left,top,right,bottom);

                        boolean inPictureCenter = isInPictureCenter(view);
                        if(inPictureCenter){
                            mImageView.enterCenter();
                        }else{
                            mImageView.exitCenter();
                        }

                        mStartX = endX;
                        mStartY = endY;
                        break;
                }
                return true;
            }
        });
    }

    /**
     * 判断气泡中心点是否在图片内部
     * @param view  当前移动到的位置的view
     * @return
     */
    private boolean isInPictureCenter(View view){
        //气泡归位,从当前位置回归到原来位置
        RatioFrame current = new RatioFrame(view.getLeft(), view.getTop(), view.getRight(), view.getBottom());
        RatioPoint centerPoint = new RatioPoint(mWidth/2,mHeight/2);
        RatioPoint currentPoint = new RatioPoint(current.mLeft + ((current.mRight - current.mLeft) / 2),current.mTop + ((current.mBottom - current.mTop) / 2));
        int x = Math.abs(centerPoint.x - currentPoint.x);
        int y = Math.abs(centerPoint.y - currentPoint.y);
        //通过勾股定理计算两点之间的距离
        int edge = (int)Math.sqrt(Math.pow(x,2) + Math.pow(y,2));
        int pictureRadius = mImageView.getPictureRadius();
        //然后和内部图片的半斤比较，小于pictureRadius，就说明在内部
        if(pictureRadius > edge){//进入到内部

            return true;
        }
        return false;
    }

    /**
     * 判断气泡中心点是否在图片内部
     * @param view
     * @param current  当前移动到的位置
     * @param endRatioFrame  如果在中间，该值用于复位到原本位置
     * @return
     */
    private boolean isInPictureCenter(int position,View view,RatioFrame current,RatioFrame endRatioFrame){
        RatioPoint centerPoint = new RatioPoint(mWidth/2,mHeight/2);
        RatioPoint currentPoint = new RatioPoint(current.mLeft + ((current.mRight - current.mLeft) / 2),current.mTop + ((current.mBottom - current.mTop) / 2));
        int x = Math.abs(centerPoint.x - currentPoint.x);
        int y = Math.abs(centerPoint.y - currentPoint.y);
        //通过勾股定理计算两点之间的距离
        int edge = (int)Math.sqrt(Math.pow(x,2) + Math.pow(y,2));
        int pictureRadius = mImageView.getPictureRadius();
        //然后和内部图片的半斤比较，小于pictureRadius，就说明在内部
        if(pictureRadius > edge){//进入到内部
            //System.out.println("---------------------进入到内部");

            //防止有些人手速快（哈哈），将一个气泡移动到中心是，还在缩放了，就移动了第二个气泡，导致出现bug
            mIsPlayLoveXin = true;

            if(mInnerCenterListener != null){
                mInnerCenterListener.innerCenter(position,((TextView)view).getText().toString());
            }

            //说明到中心了，执行操作加1
            reveseScaleView(position ,view,current,endRatioFrame);
            return true;
        }
        return false;
    }

    /**
     * 缩放图片（补间动画）
     * @param view
     * @param current  缩放后用于平移的起点
     * @param endRatioFrame 缩放后用于平移的终点
     */
    public void reveseScaleView(final int position , final View view, final RatioFrame current, final Object endRatioFrame) {
        // 以view中心为缩放点，由初始状态缩小到看不间在返回到看见
        ScaleAnimation animation = new ScaleAnimation(
                1.0f, 0.0f,//一点点变小直到看不见为止
                1.0f, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f//中间缩放
        );
        animation.setDuration(BUBBLE_ENTER_CENTER_SCALE_TIME);
        animation.setRepeatMode(Animation.REVERSE);
        animation.setRepeatCount(1);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {

                homingBubbleView(true,position,view, current, endRatioFrame);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        view.startAnimation(animation);
    }

   /* *//**
     * 让气泡浮动起来
     * @param textViews
     *//*
    private void startBubbleFloat(List<TextView> textViews){

        mTranslateAnimationList.clear();

        for (int i = 0; i < textViews.size(); i++) {
            final AnimPoint animPoint = new AnimPoint(0,10,0,0);
            TranslateAnimation mTranslateAnimation = new TranslateAnimation(animPoint.fromX, animPoint.toX, animPoint.fromY, animPoint.toY);

            mTranslateAnimation.setDuration(1000);

            mTranslateAnimation.setRepeatCount(Animation.INFINITE);

            mTranslateAnimation.setRepeatMode(Animation.REVERSE);

            mTranslateAnimation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    animPoint.fromX = getRandom();
                    animPoint.toX = getRandom();
                    animPoint.fromY = getRandom();
                    animPoint.toY = getRandom();


                    System.out.println("------------------------animPoint:"+animPoint);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });

            textViews.get(i).setAnimation(mTranslateAnimation);

            mTranslateAnimation.start();

            mTranslateAnimationList.add(mTranslateAnimation);
        }

    }

    private int getRandom(){
        Random random = new Random(10);
        return random.nextInt();
    }

    *//**
     * 让气泡浮动起来
     * @param view
     *//*
    private void startBubbleFloat(int position,View view){

        TranslateAnimation translateAnimation = mTranslateAnimationList.get(position);
        view.setAnimation(translateAnimation);

        translateAnimation.start();
    }


    *//**
     * 清除动画
     * @param view
     *//*
    private void stopBubbleFolat(View view){
        view.clearAnimation();
    }

    *//**
     * 停止气泡浮动
     *//*
    public void stopBubbleFloat(){
        if(mTranslateAnimationList != null){
            for (int i = 0; i < mTranslateAnimationList.size(); i++) {
                mTranslateAnimationList.get(i).cancel();
            }
        }
    }*/

    /**
     * 产生（-8到-4）和（4-8）随机数
     * @return
     */
    private int getRandom(){
        Random random = new Random();
        boolean b = random.nextBoolean();
        if(b){
            return random.nextInt(6) + 6;
        }
        return (random.nextInt(6) + 6) * -1;
    }

    /**
     * 将px转dp
     * @return
     */
    private int getRandomDp(){
        return px2dp(getRandom());
    }

    /**
     * px转dp
     * @param px
     * @return
     */
    private int px2dp(int px){
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,px,getContext().getResources().getDisplayMetrics());
    }

    /**
     * 产生（3000 - 5000）随机数
     * @return
     */
    private int getRandomTime(){
        Random random = new Random();
        return (random.nextInt(2000) + 3000);
    }

    public List<BubbleView> getTextViews(){
        return mTextViews;
    }

    /**
     * 让气泡浮动起来
     * @param textViews
     */
    public void startBubbleFloat(List<BubbleView> textViews){
        mAnimatorSetList.clear();
        for (int i = 0; i < textViews.size(); i++) {
            TextView textView = textViews.get(i);
            //setAnimFloat(textView);

            mAnimatorSetList.put(i,setAnimFloat(textView));

        }

    }

    /**
     * 给指定的View设置浮动效果
     * @param view
     * @return
     */
    private AnimatorSet setAnimFloat(View view ){
        List<Animator> animators = new ArrayList<>();
        //int random = getRandom() ;
        ObjectAnimator translationXAnim = ObjectAnimator.ofFloat(view, "translationX", 0f,getRandomDp(),getRandomDp() , 0);
        translationXAnim.setDuration(getRandomTime());
        translationXAnim.setRepeatCount(ValueAnimator.INFINITE);//无限循环
        translationXAnim.setRepeatMode(ValueAnimator.INFINITE);//
        translationXAnim.setInterpolator(new LinearInterpolator());
        translationXAnim.start();
        animators.add(translationXAnim);
        //random = getRandom();
        ObjectAnimator translationYAnim = ObjectAnimator.ofFloat(view, "translationY", 0f,getRandomDp(),getRandomDp() , 0);
        translationYAnim.setDuration(getRandomTime());
        translationYAnim.setRepeatCount(ValueAnimator.INFINITE);
        translationYAnim.setRepeatMode(ValueAnimator.INFINITE);
        translationXAnim.setInterpolator(new LinearInterpolator());
        translationYAnim.start();
        animators.add(translationYAnim);

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(animators);
        //animatorSet.setStartDelay(delay);
        animatorSet.start();
        return animatorSet;
    }

    /**
     * 让指定的气泡浮动起来
     *
     *
     */
    private void startBubbleFloat(int position,View view ){
        //AnimatorSet animatorSet = mAnimatorSetList.get(position);
        //animatorSet.start();
        //mAnimatorSetList.remove(position);
        //AnimatorSet animatorSet = setAnimFloat(view);
        //mAnimatorSetList.put(position ,animatorSet);


        AnimatorSet animatorSet = mAnimatorSetList.get(position);
        for (Animator animator : animatorSet.getChildAnimations()) {
            animator.start();
        }

    }

    /**
     * 暂停浮动
     *
     */
    private void stopBubbleFolat(int position,View view){
        final AnimatorSet animatorSet = mAnimatorSetList.get(position);
        for (Animator animator : animatorSet.getChildAnimations()) {
            //执行到动画最后，恢复到初始位置，不然重新开始浮动的时候，会有一个闪烁的bug
            if(animator.isRunning()) {
                animator.end();
                animator.cancel();
            }
        }
        postDelayed(new Runnable() {
            @Override
            public void run() {
                //需要执行两次，经过测试，循环一次只能结束一个
                for (Animator animator : animatorSet.getChildAnimations()) {
                    if(animator.isRunning()) {
                        animator.end();
                        animator.cancel();
                    }
                }
            }
        },50);

        //animatorSet.end();
        //animatorSet.pause();
        //animatorSet.cancel();
        //mAnimatorSetList.remove(position);
    }

    /**
     * 停止浮动
     */
    public void stopBubbleFloat(){
        for (int i = 0; i < mAnimatorSetList.size(); i++) {
            AnimatorSet animatorSet = mAnimatorSetList.get(i);
            if(animatorSet != null) {
                //animatorSet.pause();
                //mAnimatorSetList.remove(i);//不能再此移除
                for (Animator animator : animatorSet.getChildAnimations()) {
                    if(animator != null) {
                        if(animator.isRunning()) {
                            animator.end();
                            animator.cancel();
                        }
                    }
                }
                //需要执行两次，经过测试，循环一次只能结束一个

            }
            //animatorSet = null;
        }

        //需要延时重新执行一次，不知道为什么？？？
        postDelayed(new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < mAnimatorSetList.size(); i++) {
                    AnimatorSet animatorSet = mAnimatorSetList.get(i);
                    if(animatorSet != null) {
                        //animatorSet.pause();
                        //mAnimatorSetList.remove(i);//不能再此移除
                        for (Animator animator : animatorSet.getChildAnimations()) {
                            if(animator != null) {
                                if(animator.isRunning()) {
                                    animator.end();
                                    animator.cancel();
                                }
                            }
                        }
                        //需要执行两次，经过测试，循环一次只能结束一个

                    }
                    //animatorSet = null;
                }
            }
        },50);

    }

    /**
     * 气泡由小到大缩放
     * @param textViews
     */
    private void scaleSmallToLarge(List<BubbleView> textViews){
        // 以view中心为缩放点，由初始状态缩小到看不间
        ScaleAnimation animation = new ScaleAnimation(
                0.0f, 1.0f,//一点点变小知道看不见为止
                0.0f, 1.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f//中间缩放
        );
        animation.setDuration(OPEN_BUBBLE_TIME);//要和平移的时间一致
        //animation.setRepeatMode(Animation.REVERSE);
        //animation.setRepeatCount(1);
        for (int i = 0; i < textViews.size(); i++) {
//            //先显示
//            textViews.get(i).setVisibility(View.VISIBLE);
            //再执行动画
            textViews.get(i).startAnimation(animation);
        }

    }

    /**
     * 气泡由大到小缩放
     * @param textViews
     */
    private void scaleLargeToSmall(final List<BubbleView> textViews){
        // 以view中心为缩放点，由初始状态缩小到看不间
        ScaleAnimation animation = new ScaleAnimation(
                1.0f, 0.0f,//一点点变小知道看不见为止
                1.0f, 0.0f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f//中间缩放
        );
        animation.setDuration(CLOSE_BUBBLE_TIME);//要和平移的时间一致
        //animation.setRepeatMode(Animation.REVERSE);
        //animation.setRepeatCount(1);
        animation.setFillAfter(true);
//        animation.setAnimationListener(new Animation.AnimationListener() {
//            @Override
//            public void onAnimationStart(Animation animation) {
//
//            }
//
//            @Override
//            public void onAnimationEnd(Animation animation) {
//                for (int i = 0; i < textViews.size(); i++) {
//                    //隐藏，不响应触摸事件
//                    textViews.get(i).setVisibility(View.INVISIBLE);
//                }
//            }
//
//            @Override
//            public void onAnimationRepeat(Animation animation) {
//
//            }
//        });
        for (int i = 0; i < textViews.size(); i++) {
            textViews.get(i).startAnimation(animation);
        }

    }


    /**
     * 平移View(气泡),就是将该View重新布局
     * @param view
     * @param startRatioFrame
     * @param endRatioFrame
     */
    private void homingBubbleView(final boolean isCenter,final int position ,final View view,Object startRatioFrame,Object endRatioFrame){
        ValueAnimator mAnimatorHoming = ValueAnimator.ofObject(new HomingRatioFrameEvaluator(),startRatioFrame, endRatioFrame);
        mAnimatorHoming.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                RatioFrame currentRatioFrame = (RatioFrame) animation.getAnimatedValue();
                //重新布局
                view.layout(currentRatioFrame.mLeft,currentRatioFrame.mTop,currentRatioFrame.mRight,currentRatioFrame.mBottom);
            }
        });
        mAnimatorHoming.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animation) {
                //在归位途中
                mIsBubbleHoming = true;
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                //打开气泡浮动
                startBubbleFloat(position,view);
                //已归位
                mIsBubbleHoming = false;
                //如果是在中心撒手的，说明操作添加1
                if(isCenter) {
                    if(mIsPlayAnimLoveXin) {
                        view.setBackgroundResource(R.drawable.textview_shape_blue);
                        //播放动画
                        playCenterLoveXin();
                        addOneAnim(view);
                    }else{
                        //在刚进入中心时，置为true，防止执行缩放时，再次触摸，这里不执行动画，所以置为false
                        mIsPlayLoveXin = false;
                    }

                    if (mInnerCenterListener != null) {
                        if (view instanceof TextView) {

                            mInnerCenterListener.innerCenterHominged(position, ((TextView) view).getText().toString());
                        }
                    }
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {

            }

            @Override
            public void onAnimationRepeat(Animator animation) {

            }
        });
        mAnimatorHoming.setDuration(BUBBLE_TRANSLATION_HOMING_TIME);
        mAnimatorHoming.start();
    }


    /**
     * 设置气泡进入的一个监听
     */
    private void setBubbleEnterFinishListener(){

        if(mImageView != null){
            mImageView.setEnterFinishListener(mEnterFinishListener);
        }

    }

    /**
     * 设置气泡退出的一个监听
     */
    private void setBubbleExitFinish(){

        mAnimatorExit = ValueAnimator.ofObject(new ExitRatioFrameEvaluator(getContext()),mRatioFrameList, getRatioFrameCenterList(mRatioFrameCenter,mRatioFrameList));
        mAnimatorExit.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mCurrentRatioFrameList = (List<RatioFrame>) animation.getAnimatedValue();
                requestLayout();
            }
        });
        mAnimatorExit.setDuration(CLOSE_BUBBLE_TIME);
        //mAnimatorExit.start();

    }

    /**
     * 根据中心点，和边缘气泡的集合，计算边缘气泡到中心点的一个坐标
     * @param center
     * @param edgeList
     * @return
     */
    private List<RatioFrame> getRatioFrameCenterList(RatioFrame center ,List<RatioFrame> edgeList){
        List<RatioFrame> centerList = new ArrayList<>();
        int left = 0;
        int right = 0 ;
        int top = 0 ;
        int bottom = 0;
        for (int i = 0; i < edgeList.size(); i++) {
            RatioFrame ratioFrame = edgeList.get(i);

            left = center.mLeft - (ratioFrame.mRight - ratioFrame.mLeft) / 2;
            right = center.mRight + (ratioFrame.mRight - ratioFrame.mLeft) / 2;
            top = center.mTop - (ratioFrame.mBottom - ratioFrame.mTop) / 2;
            bottom = center.mBottom + (ratioFrame.mBottom - ratioFrame.mTop) / 2;

            centerList.add(new RatioFrame(left,top,right,bottom));
        }

        return centerList;
    }


    /**
     * 进入气泡
     */
    public void enterBubble(){
        if(mIsEnter) return ;

        mIsEnter = true;

//        if(mAnimatorEnetr != null){
//
//            mAnimatorEnetr.start();
//        }

        //进入动画时，它会自动回调mAnimatorEnetr.start();
        if(mImageView != null){
            mImageView.enterAnim();
        }
    }

    /**
     * 退出气泡
     */
    public void exitBubble(){
        //只有展开才可以退出
        if(!mIsBubbleOpen) return ;

        if(mAnimatorExit != null){

            mAnimatorExit.start();

            //气泡展开的标识设为false
            mIsBubbleOpen = false;

            mIsEnter = false;

            //同时气泡有大变小
            scaleLargeToSmall(mTextViews);

            stopBubbleFloat();
        }

        if(mImageView != null){
            mImageView.exitAnim();
        }
    }

    /**
     * 销毁
     */
    public void destry(){
        mTextViews.clear();
        mRatioFrameList.clear();
        mAnimatorSetList.clear();
    }

    /**
     * 中间圆外围圈进入完成监听
     */
    private DynamicAvatarView.EnterFinishListener mEnterFinishListener = new DynamicAvatarView.EnterFinishListener() {
        @Override
        public void enterFinish() {

        }

        @Override
        public void firstEnterFinish() {
            //mAnimatorEnetr = ValueAnimator.ofObject(new EnterRatioFrameEvaluator(), mRatioFrameCenter,mRatioFrameList);
            mAnimatorEnetr = ValueAnimator.ofObject(new EnterRatioFrameEvaluator(getContext()), getRatioFrameCenterList(mRatioFrameCenter,mRatioFrameList),mRatioFrameList);
            mAnimatorEnetr.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    mCurrentRatioFrameList = (List<RatioFrame>) animation.getAnimatedValue();
                    requestLayout();
                }
            });
            mAnimatorEnetr.setDuration(OPEN_BUBBLE_TIME);
            mAnimatorEnetr.addListener(new Animator.AnimatorListener() {
                @Override
                public void onAnimationStart(Animator animation) {

                }

                @Override
                public void onAnimationEnd(Animator animation) {
                    //防止锁屏后，解屏后会重走onMeasure，onLayout方法，此监听会重新设置，会再次的startBubbleFloat，之前的浮动动画还在，就会导致按下不会停止浮动，
                    //所以要先停止浮动，在开始浮动
                    stopBubbleFloat();
                    startBubbleFloat(mTextViews);
                    //气泡展开的标识设为true
                    mIsBubbleOpen = true;
                }

                @Override
                public void onAnimationCancel(Animator animation) {

                }

                @Override
                public void onAnimationRepeat(Animator animation) {

                }
            });
            mAnimatorEnetr.start();

            //同时缩放气泡
            scaleSmallToLarge(mTextViews);

        }
    };


    public interface InnerCenterListener{
        void innerCenterHominged(int position, String text);
        void innerCenter(int position, String text);
    }

    private InnerCenterListener mInnerCenterListener;

    public void setInnerCenterListener(InnerCenterListener listener){
        this.mInnerCenterListener = listener;
    }

}

/**
 * 气泡布局坐标
 */
class RatioFrame{
    public int mLeft ;
    public int mTop ;
    public int mRight ;
    public int mBottom ;

    public double mAngle;

    @Override
    public String toString() {
        return "RatioFrame{" +
                "mLeft=" + mLeft +
                ", mTop=" + mTop +
                ", mRight=" + mRight +
                ", mBottom=" + mBottom +
                '}';
    }

    public RatioFrame(int left, int top, int right ,int bottom) {
        mLeft = left;
        mRight = right;
        mTop = top;
        mBottom = bottom;

    }

    public RatioFrame(int left, int top, int right ,int bottom, double angle) {
        mTop = top;
        mLeft = left;
        mRight = right;
        mBottom = bottom;
        mAngle = angle;
    }
}

/**
 * 点坐标
 */
class RatioPoint{
    public int x;
    public int y;

    public RatioPoint(int x, int y) {
        this.x = x;
        this.y = y;
    }
}

///**
// * 浮动的坐标
// */
//class AnimPoint{
//    public int fromX;
//    public int toX ;
//    public int fromY ;
//    public int toY ;
//
//    public AnimPoint(int fromX,int toX,int fromY,int toY){
//        this.fromX = fromX;
//        this.toX = toX;
//        this.fromY = fromY;
//    }
//
//    @Override
//    public String toString() {
//        return "AnimPoint{" +
//                "fromX=" + fromX +
//                ", toX=" + toX +
//                ", fromY=" + fromY +
//                ", toY=" + toY +
//                '}';
//    }
//}