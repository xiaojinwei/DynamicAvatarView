package com.cj.dynamicavatarview.ratio;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Handler;
import android.os.Message;
import android.util.AttributeSet;
import android.widget.ImageView;

/**
 * Created by cj_28 on 2016/10/15.
 */
public class DynamicAvatarView extends ImageView {

    private static final ScaleType SCALE_TYPE = ScaleType.CENTER_CROP;

    private static final int COLORDRAWABLE_DIMENSION = 1;
    private static final Bitmap.Config BITMAP_CONFIG = Bitmap.Config.ARGB_8888;

    private Bitmap mBitmap;
    private BitmapShader mBitmapShader;

    private final Matrix mShaderMatrix = new Matrix();
    private final Paint mBitmapPaint = new Paint();
    private final Paint mBorderPaint = new Paint();
    private final Paint mOuterPaint = new Paint();
    private final Paint mInnerPaint = new Paint();

    private static final int DEFAULT_BORDER_WIDTH = 3;
    private static final int DEFAULT_BORDER_COLOR = Color.WHITE;
    private static final int OUTER_PAINT_COLOR = Color.parseColor("#55FFFFFF");
    private static final int INNER_PAINT_COLOR = Color.parseColor("#66FFFFFF");
    private static final int OUTER_PAINT_COLOR_CENTER = Color.parseColor("#22FFFFFF");
    private static final int INNER_PAINT_COLOR_CENTER = Color.parseColor("#33FFFFFF");

    private int mBorderColor = DEFAULT_BORDER_COLOR;
    private int mBorderWidth = DEFAULT_BORDER_WIDTH;

    private int mOuterPaintColor = OUTER_PAINT_COLOR;
    private int mInnerPaintColor = INNER_PAINT_COLOR;

    private int mBitmapWidth;
    private int mBitmapHeight;

    private final RectF mDrawableRect = new RectF();
    private final RectF mBorderRect = new RectF();

    private float mDrawableRadius;//显示的图片
    private float mBorderRadius;//..//显示的图片上的边框
    private float mOuterRadius;//外层动画
    private float mInnerRadius;//..//内层动画

    private float mRealDrawableRadius;//这是View没有被缩放之前的mDrawableRadius的半径
    private float mRealBorderRadius;//..

    private boolean mReady;
    private boolean mSetupPending;

    private float mChangeRateBorder;//记录外圆执行动画时半径变化率
    private float mChangeRateOuter;//记录内圆执行动画时半径变化率
    private float mChangeRateInner;//记录图片边框执行动画时半径变化率
    private float mChangeRange;//变化范围，View半径的1/6

    private boolean mIsFirstEnterAnim;//用来标识是否第一次进入动画
    //private boolean mIsFirstLayout;//

    private int mPictureRadius;

    private int mAnimFlashTime = 30;//

    /**
     * 返回显示的图片半径
     * @return
     */
    public int getPictureRadius(){
        return mPictureRadius;
    }

    //*******执行动画*******//
    //外圆执行动画时半径变化率
    private float mRateOuter[] = {
            -1,-1,-1,-1,-1,-1,-1,-1,-1,-1,
            0,0.1f,0.2f,0.3f,0.4f,0.5f,0.6f,0.7f,0.8f,0.9f,1.0f
            ,0.92f,0.88f,0.85f,0.82f,0.76f,0.72f,0.68f,0.60f,0.54f,0.48f,
            0.40f,0.33f,0.28f,0.20f};
    //内圆执行动画时半径变化率
    private float mRateInner[] = {
            -1,-1,-1,-1,-1,
            0,0.1f,0.2f,0.3f,0.4f,0.5f,0.6f,0.7f,0.8f,0.9f,1.0f
            ,0.92f,0.88f,0.84f,0.80f,0.72f,0.67f,0.60f,0.54f,0.48f,-1f
            ,-1f,-1f,-1f,-1f,-1f,-1f,-1f,-1f,-1f};
    //图片边框执行动画时半径变化率
    private float mRateBorder[] = {
            0,0.1f,0.2f,0.3f,0.4f,0.5f,0.6f,0.7f,0.8f,0.9f,1.0f
            ,0.92f,0.90f,0.84f,0.78f,0.72f,0.64f,0.58f,-1f,-1f,-1f
            ,-1f,-1f,-1f,-1f,-1f,-1f,-1f,-1f,-1f,-1f
            ,-1f,-1f,-1f,-1f};

    //private int mColor[] = {0x55FFFFFF,0x44FFFFFF,0x33FFFFFF,0x22FFFFFF,0x11FFFFFF,0x00FFFFFF,0x00FFFFFF,0x00FFFFFF,0x00FFFFFF,0x00FFFFFF};
    private int mRateIndex;//动画变化率的索引

    //*******进入动画*******//
    //外圆执行动画时半径变化率
    private float mRateOuterEnter[] = {
            -2,-2,//外圆要缩小2个mChangeRange才会完全隐藏
            -2,-2,-2,-2,-2,
            -0.8f,-0.6f,-0.4f,-0.2f,-0.1f,
            0,0.1f,0.2f,0.3f,0.4f,0.5f
            ,0.44f,0.4f,0.35f,0.3f,0.25f
            ,0.20f,0.15f,0.1f,0.0f};
    //内圆执行动画时半径变化率
    private float mRateInnerEnter[] = {
            -1,-1,//外圆要缩小1个mChangeRange才会完全隐藏
            -0.8f,-0.6f,-0.4f,-0.2f,-0.1f,
            0,0.1f,0.2f,0.3f,0.35f,0.4f
            ,0.45f,0.5f,0.45f,0.4f,0.35f
            ,0.3f,0.25f,0.2f,0.15f,0.1f
            ,0.05f,0f,0f,0f};
    private int mRateIndexEnter;//进入动画变化率的索引

    //*******退出动画*******//
    //外圆执行动画时半径变化率
    private float mRateOuterExit[] = {

            0.0f,-0.2f,-0.4f,-0.6f,-0.8f
            ,-1f,-1.2f,-1.4f,-1.6f,-1.8f
            ,-2f};
    //内圆执行动画时半径变化率
    private float mRateInnerExit[] = {

            0.0f,-0.1f,-0.2f,-0.3f,-0.4f
            ,-0.5f,-0.6f,-0.7f,-0.8f,-0.9f
            ,-1f};
    private int mRateIndexExit;//进入动画变化率的索引


    /**
     * 按住执行动画
     */
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int index = mRateIndex ++;
            mChangeRateBorder = mRateBorder[(index)% mRateBorder.length];
            setPaintCorlor(mBorderPaint,mChangeRateBorder,DEFAULT_BORDER_COLOR);
            setPaintAlpha(mBorderPaint,(index)% mRateBorder.length,mRateBorder);
            mChangeRateOuter = mRateOuter[(index) % mRateOuter.length];
            setPaintCorlor(mOuterPaint,mChangeRateOuter,mOuterPaintColor);
            setPaintAlpha(mOuterPaint,(index) % mRateOuter.length,mRateOuter);
            mChangeRateInner = mRateInner[(index) % mRateInner.length];
            setPaintCorlor(mInnerPaint,mChangeRateInner,mInnerPaintColor);
            setPaintAlpha(mInnerPaint,(index) % mRateInner.length,mRateInner);

            //System.out.println("---------mChangeRate:"+mChangeRateBorder);
            invalidate();
            mHandler.removeCallbacksAndMessages(null);
            mHandler.sendEmptyMessageDelayed(0,mAnimFlashTime);
        }
    };

    private Handler mHandlerEnter = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int index = mRateIndexEnter ++;
            if(index >= mRateOuterEnter.length) {
                mRateIndexEnter = 0;
                mHandlerEnter.removeCallbacksAndMessages(null);
                //调用进入完成监听
                if(mEnterFinishListener != null){
                    if(mIsFirstEnterAnim){
                        mEnterFinishListener.firstEnterFinish();
                        mIsFirstEnterAnim = false;
                    }
                    mEnterFinishListener.enterFinish();
                }
                return;
            }

            mChangeRateOuter = mRateOuterEnter[(index) % mRateOuterEnter.length];

            mChangeRateInner = mRateInnerEnter[(index) % mRateInnerEnter.length];

            invalidate();
            mHandlerEnter.removeCallbacksAndMessages(null);
            mHandlerEnter.sendEmptyMessageDelayed(0,20);
        }
    };

    private Handler mHandlerExit = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int index = mRateIndexExit ++;
            if(index >= mRateOuterExit.length) {
                mRateIndexExit = 0;
                mHandlerExit.removeCallbacksAndMessages(null);
                return;
            }

            mChangeRateOuter = mRateOuterExit[(index) % mRateOuterExit.length];

            mChangeRateInner = mRateInnerExit[(index) % mRateInnerExit.length];

            invalidate();
            mHandlerExit.removeCallbacksAndMessages(null);
            mHandlerExit.sendEmptyMessageDelayed(0,20);
        }
    };

    /**
     * 设置outer和inner的画笔颜色
     * @param paint
     * @param rate
     * @param color
     */
    private void setPaintCorlor(Paint paint,float rate,int color){
        if(rate < 0){
            paint.setColor(Color.TRANSPARENT);
        }else{
            paint.setColor(color);
        }
    }

    /**
     * 设置透明度
     * @param paint
     * @param index
     * @param rate
     */
    private void setPaintAlpha(Paint paint,int index,float[] rate){
        int pre = index -1;
        if(pre >= 0 ){
            if(rate[pre] > rate[index] && rate[index] > 0){
                int color = paint.getColor();

                int colorTransparent = color & 0xff000000;
                int colorValue = color & 0x00ffffff;
                colorTransparent = colorTransparent >>> 7;
                paint.setColor((int)(rate[index] * colorTransparent) << 7 | colorValue);

            }
        }
    }

    public DynamicAvatarView(Context context) {
        this(context,null);
    }

    public DynamicAvatarView(Context context, AttributeSet attrs) {
        this(context, attrs,0);

    }

    public DynamicAvatarView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        super.setScaleType(SCALE_TYPE);

        //可以执行了
        mReady = true;

        if (mSetupPending) {
            setup();
            mSetupPending = false;
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        int size = Math.min(widthSize, heightSize);

        super.onMeasure(MeasureSpec.makeMeasureSpec(size,widthMode), MeasureSpec.makeMeasureSpec(size,heightMode));
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {

        super.onLayout(changed, left, top, right, bottom);
        //if(!mIsFirstLayout ){
            mIsFirstEnterAnim = true;
            enterAnim();
           // mIsFirstLayout = true;
        //}

    }

    @Override
    protected void onDraw(Canvas canvas) {

        if (getDrawable() == null) {
            return;
        }

        //画动画的图形
        canvas.drawCircle(getWidth() / 2,getHeight() / 2,getChangeRadiusOuter(mOuterRadius),mOuterPaint);
        canvas.drawCircle(getWidth() / 2,getHeight() / 2,getChangeRadiusInner(mInnerRadius),mInnerPaint);

        canvas.drawCircle(getWidth() / 2, getHeight() / 2, mDrawableRadius, mBitmapPaint);
        canvas.drawCircle(getWidth() / 2, getHeight() / 2, getChangeRadiusBorder(mBorderRadius), mBorderPaint);
    }

    private float getChangeRadiusBorder(float radius){

        return mChangeRateBorder * mChangeRange + radius;

    }
    private float getChangeRadiusOuter(float radius){

        return mChangeRateOuter * mChangeRange + radius;

    }
    private float getChangeRadiusInner(float radius){

        return mChangeRateInner * mChangeRange + radius;

    }

    private void initAnimColor(){
        mOuterPaint.setStyle(Paint.Style.FILL);
        mOuterPaint.setAntiAlias(true);
        mOuterPaint.setColor(mOuterPaintColor);
        mInnerPaint.setStyle(Paint.Style.FILL);
        mInnerPaint.setAntiAlias(true);
        mInnerPaint.setColor(mInnerPaintColor);
        //图片边框（默认白色）
        mBorderPaint.setColor(mBorderColor);

        mOuterRadius = mRealBorderRadius / 6 * 5;
        mInnerRadius = mRealBorderRadius / 6 * 4;
        mChangeRange = mRealBorderRadius / 6;
        mRateIndex = 0;
    }

    public void enterCenter(){
        mAnimFlashTime = 10;
        mOuterPaintColor = OUTER_PAINT_COLOR_CENTER;
        mInnerPaintColor = INNER_PAINT_COLOR_CENTER;
    }

    public void exitCenter(){
        mAnimFlashTime = 30;
        mOuterPaintColor = OUTER_PAINT_COLOR;
        mInnerPaintColor = INNER_PAINT_COLOR;
    }

    /**
     * 开始动画
     */
    public void startAnim(){
        mHandler.sendEmptyMessageDelayed(0,30);

    }
    /**
     * 停止动画
     */
    public void stopAnim(){
        mHandler.removeCallbacksAndMessages(null);
        mChangeRateBorder = 0;
        mChangeRateOuter = 0;
        mChangeRateInner = 0;
//        mBorderPaint.setColor(DEFAULT_BORDER_COLOR);
//        mOuterPaint.setColor(mOuterPaintColor);
//        mInnerPaint.setColor(mInnerPaintColor);
//        mRateIndex = 0;
        initAnimColor();

        //invalidate();
        enterAnim();
    }
    /**
     * 进入动画
     */
    public void enterAnim(){
        mHandlerEnter.sendEmptyMessage(0);
    }

    /**
     * 退出动画
     */
    public void exitAnim(){
        mHandlerExit.sendEmptyMessage(0);
        //退出之后，下次进入动画时，又是第一次
        mIsFirstEnterAnim  = true;
    }

    /**
     * 设置外圆动画的颜色
     * @param outerPaintColor
     */
    public void setOuterPaintColor(int outerPaintColor) {
        if (outerPaintColor == mOuterPaintColor) {
            return;
        }
        mOuterPaintColor = outerPaintColor;
        mOuterPaint.setColor(mOuterPaintColor);
        invalidate();
    }

    /**
     * 设置内圆动画的颜色
     * @param innerPaintColor
     */
    public void setInnerPaintColor(int innerPaintColor) {
        if (innerPaintColor == mInnerPaintColor) {
            return;
        }
        mInnerPaintColor = innerPaintColor;
        mInnerPaint.setColor(mInnerPaintColor);
        invalidate();
    }

    /**
     * 设置图片边框的颜色
     * @param borderColor
     */
    public void setBorderColor(int borderColor) {
        if (borderColor == mBorderColor) {
            return;
        }

        mBorderColor = borderColor;
        mBorderPaint.setColor(mBorderColor);
        invalidate();
    }

    /**
     * 设置图片边框的宽度
     * @param borderWidth
     */
    public void setBorderWidth(int borderWidth) {
        if (borderWidth == mBorderWidth) {
            return;
        }

        mBorderWidth = borderWidth;
        setup();
    }

    @Override
    public ScaleType getScaleType() {
        return SCALE_TYPE;
    }

    @Override
    public void setScaleType(ScaleType scaleType) {
        if (scaleType != SCALE_TYPE) {
            throw new IllegalArgumentException(String.format("ScaleType %s not supported.", scaleType));
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        //只有在此方法中调用setup，setup中的getWidth方法得到的值才不会是0，
        setup();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        mBitmap = getMaxSquareCenter(bm);
        setup();
    }

    /**
     * mxl中设置src就会走此方法
     * @param drawable
     */
    @Override
    public void setImageDrawable(Drawable drawable) {
        super.setImageDrawable(drawable);
        mBitmap = getBitmapFromDrawable(drawable);
        setup();
    }

    @Override
    public void setImageResource(int resId) {
        super.setImageResource(resId);
        mBitmap = getBitmapFromDrawable(getDrawable());
        setup();
    }

    /**
     * 从bitmap中间裁剪出最大的正方形
     * @param bitmap
     * @return
     */
    private Bitmap getMaxSquareCenter(Bitmap bitmap){
        int w = bitmap.getWidth(); // 得到图片的宽，高
        int h = bitmap.getHeight();
        int cropWidth = w >= h ? h : w;// 裁切后所取的正方形区域边长
        return Bitmap.createBitmap(bitmap, (w - cropWidth)/2 , (h - cropWidth)/2, cropWidth, cropWidth, null, false);
    }

    private Bitmap getBitmapFromDrawable(Drawable drawable) {

        if (drawable == null) {
            return null;
        }

        if (drawable instanceof BitmapDrawable) {
            //从bitmap中间裁剪出最大的正方形
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            return getMaxSquareCenter(bitmap);
            //return ((BitmapDrawable) drawable).getBitmap();
        }

        try {
            Bitmap bitmap;

            if (drawable instanceof ColorDrawable) {
                bitmap = Bitmap.createBitmap(COLORDRAWABLE_DIMENSION, COLORDRAWABLE_DIMENSION, BITMAP_CONFIG);
            } else {
                int min = Math.min(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
                bitmap = Bitmap.createBitmap(min, min, BITMAP_CONFIG);
            }

            Canvas canvas = new Canvas(bitmap);
            int left,top,right,buttom;
            int width = canvas.getWidth();
            int height = canvas.getHeight();
            int abs = Math.abs(width - height);
            if(width <= height){
                left = 0;
                top = (height - abs) / 2;
                right = width;
                buttom = height - top;
            }else{
                left = (width - abs) / 2;
                top = 0;
                right = width - left;
                buttom = height;
            }
            //drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.setBounds(left, top, right, buttom);
            drawable.draw(canvas);
            return bitmap;
        } catch (OutOfMemoryError e) {
            return null;
        }
    }

    private void setup() {
        //只有执行过构造函数之后，所有的成员才被初始化完毕
       if (!mReady) {
            mSetupPending = true;
            return;
        }

        if (mBitmap == null) {
            return;
        }

        mBitmapShader = new BitmapShader(mBitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);

        mBitmapPaint.setAntiAlias(true);
        mBitmapPaint.setShader(mBitmapShader);

        mBorderPaint.setStyle(Paint.Style.STROKE);
        mBorderPaint.setAntiAlias(true);
        mBorderPaint.setColor(mBorderColor);
        mBorderPaint.setStrokeWidth(mBorderWidth);

        mBitmapHeight = mBitmap.getHeight();
        mBitmapWidth = mBitmap.getWidth();
        //图片边框设置的范围
        mBorderRect.set(0, 0, getWidth(), getHeight());
        mBorderRadius = Math.min((mBorderRect.height() - mBorderWidth) / 2, (mBorderRect.width() - mBorderWidth) / 2) / 2;
        mRealBorderRadius = 2 * mBorderRadius;
        //图片显示的范围
        mDrawableRect.set(mBorderWidth, mBorderWidth, mBorderRect.width() - mBorderWidth, mBorderRect.height() - mBorderWidth);
        //让图片显示的范围是控件大小的一半
        mDrawableRadius = Math.min(mDrawableRect.height() / 2, mDrawableRect.width() / 2) / 2;
        mRealDrawableRadius = 2 * mDrawableRadius;
        mPictureRadius = (int)mDrawableRadius;

        updateShaderMatrix();
        initAnimColor();
        invalidate();
    }

    private void updateShaderMatrix() {
        float scale;
        float dx = 0;
        float dy = 0;

        mShaderMatrix.set(null);

        if (mBitmapWidth * mDrawableRect.height() > mDrawableRect.width() * mBitmapHeight) {
            scale = mDrawableRect.height() / (float) mBitmapHeight / 2;  //将图片缩放在正中间
            dx = (mDrawableRect.width() - mBitmapWidth * scale) * 0.5f;
        } else {
            scale = mDrawableRect.width() / (float) mBitmapWidth / 2;
            dy = (mDrawableRect.height() - mBitmapHeight * scale) * 0.5f;
        }

        mShaderMatrix.setScale(scale , scale );
        mShaderMatrix.postTranslate((int) (dx + 0.5f) + mBorderWidth + mDrawableRadius, (int) (dy + 0.5f) + mBorderWidth);

        mBitmapShader.setLocalMatrix(mShaderMatrix);
    }

    /**
     * 图片外围圈进入显示完毕监听
     */
    public interface EnterFinishListener{
        void enterFinish();
        void firstEnterFinish();
    }

    private EnterFinishListener mEnterFinishListener;

    /**
     * 设置图片外围圈进入显示完毕监听
     * @param listener
     */
    public void setEnterFinishListener(EnterFinishListener listener){
        this.mEnterFinishListener = listener;
    }

}
