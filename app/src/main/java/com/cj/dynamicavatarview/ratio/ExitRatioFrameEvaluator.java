package com.cj.dynamicavatarview.ratio;

import android.animation.TypeEvaluator;
import android.content.Context;
import android.util.TypedValue;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by chenj on 2016/10/19.
 */
public class ExitRatioFrameEvaluator implements TypeEvaluator {
    public static final int OFFSET_DISTANCE = 80;

    private Context mContext;

    private int mOffsetDistance;

    public ExitRatioFrameEvaluator(Context context){
        this.mContext = context;
        mOffsetDistance = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,OFFSET_DISTANCE,mContext.getResources().getDisplayMetrics());
    }
    @Override
    public Object evaluate(float fraction, Object startValue, Object endValue) {
        List<RatioFrame> startRatioFrameList = (List<RatioFrame>) startValue;
        List<RatioFrame> endRatioFrameList = (List<RatioFrame>) endValue;
        List<RatioFrame> ratioFrameList = new ArrayList<>();
        for (int i = 0; i < startRatioFrameList.size(); i++) {
            RatioFrame startRatioFrame = startRatioFrameList.get(i);
            RatioFrame endRatioFrame = endRatioFrameList.get(i);

            double t = ( -2 * Math.pow(fraction,2) + 2 * fraction);//倾斜变化率
            //System.out.println("--------------------------t:"+t);
            //System.out.println("--------------------------fraction:"+fraction);

            int temp = (int)((mOffsetDistance) * t);
            double rightAngle = Math.PI / 2;
            //System.out.println("--------------------------rightAngle:"+rightAngle);
            //System.out.println("--------------------------startRatioFrame.mAngle:"+startRatioFrame.mAngle);
            int moveX = 0,moveY = 0;

            //让气泡上、下、左、右平移，形成弧度的平移路线
//            if(startRatioFrame.mAngle >0 && startRatioFrame.mAngle <= rightAngle){//(0 < angle <= 90)上移
//                moveX = (int)(temp * Math.abs(Math.cos(startRatioFrame.mAngle)));
//                moveY = (int)(temp * Math.abs(Math.sin(startRatioFrame.mAngle)));
//            }else if(startRatioFrame.mAngle > rightAngle && startRatioFrame.mAngle <= rightAngle * 2){//(90 < angle <= 180)右移
//                moveX = (int)(-temp * Math.abs(Math.cos(startRatioFrame.mAngle)));
//                moveY = (int)(temp * Math.abs(Math.sin(startRatioFrame.mAngle)));
//            }else if(startRatioFrame.mAngle > rightAngle * 2 && startRatioFrame.mAngle <= rightAngle * 3){//(180 < angle <= 2700)下移
//                moveX = (int)(-temp * Math.abs(Math.cos(startRatioFrame.mAngle)));
//                moveY = (int)(-temp * Math.abs(Math.sin(startRatioFrame.mAngle)));
//            }else if(startRatioFrame.mAngle > rightAngle * 3 && startRatioFrame.mAngle <= rightAngle * 4 || startRatioFrame.mAngle == 0){//(270 < angle <= 360 或者 angle == 0) 左移
//                moveX = (int)(temp * Math.abs(Math.cos(startRatioFrame.mAngle)));
//                moveY = (int)(-temp * Math.abs(Math.sin(startRatioFrame.mAngle)));
//            }
            //简化为
            moveX = (int)(temp * Math.cos(startRatioFrame.mAngle));
            moveY = (int)(temp * Math.sin(startRatioFrame.mAngle));

            int left = (int)(startRatioFrame.mLeft - ((startRatioFrame.mLeft - endRatioFrame.mLeft) * fraction) - moveX);
            int top = (int)(startRatioFrame.mTop - ((startRatioFrame.mTop - endRatioFrame.mTop) * fraction) - moveY);
            int right = (int)(startRatioFrame.mRight - ((startRatioFrame.mRight - endRatioFrame.mRight) * fraction) - moveX);
            int bottom = (int)(startRatioFrame.mBottom - ((startRatioFrame.mBottom - endRatioFrame.mBottom) * fraction) - moveY);
            ratioFrameList.add(new RatioFrame(left,top,right,bottom));
        }
        return ratioFrameList;
    }
}
