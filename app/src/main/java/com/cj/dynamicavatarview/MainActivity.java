package com.cj.dynamicavatarview;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.cj.dynamicavatarview.ratio.DynamicAvatarView;
import com.cj.dynamicavatarview.ratio.RatioLayout;

public class MainActivity extends AppCompatActivity {

    private DynamicAvatarView mDynamic;
    private RatioLayout mRatio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRatio = (RatioLayout) findViewById(R.id.ratio);

        mDynamic = (DynamicAvatarView) findViewById(R.id.dynamic);


        final String[] texts = {"高低贵贱(0)", "活动方案的(0)", "IT发哈(0)", "发疯(0)", "额头和(0)", "法国热啊法国热啊(0)", "防护(0)", "夫人(0)", "德娃(0)"};
        final boolean[] bgs = { false, true, false, true, false, true, false, true,false};

        mRatio.addText(texts);//,"防护","夫人","德娃"
        //mRatio.setBubbleEnterFinishListener();
        mRatio.changeTextBackground(bgs);

        mRatio.setInnerCenterListener(new RatioLayout.InnerCenterListener() {
            @Override
            public void innerCenterHominged(int position, String text) {
                texts[position] = addNumber(texts[position]);
                mRatio.changeText(texts);
            }

            @Override
            public void innerCenter(int position, String text) {
                if(position % 2 == 0){
                    mRatio.setPlayLoveXin(true);
                }else{
                    mRatio.setPlayLoveXin(false);
                }
            }
        });
    }

    private String addNumber(String text) {
        int start = text.indexOf("(");
        int end = text.indexOf(")");
        int number = Integer.parseInt(text.substring(start + 1, end));
        number++;

        return text.substring(0, start) + "(" + number + ")";
    }

    public void exit(View view) {
        //mDynamic.exitAnim();
        mRatio.exitBubble();
    }

    public void enter(View view) {
        //mDynamic.enterAnim();
        mRatio.enterBubble();
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("-------------------onResume:");
    }

    @Override
    protected void onStart() {
        super.onStart();
        System.out.println("-------------------onStart:");
    }

    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("-------------------onPause:");
    }

    @Override
    protected void onStop() {
        super.onStop();
        System.out.println("-------------------onStop:");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        System.out.println("-------------------onRestart:");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRatio.destry();
        System.out.println("-------------------onDestroy:");
    }
}