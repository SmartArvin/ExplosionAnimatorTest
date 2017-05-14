package com.explosiontest.utils;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;


public class ExplosionField extends View {

	//声明一个ExplosionAnimator类型的数组
    private List<ExplosionAnimator> mExplosions = new ArrayList<>();
    private int[] mExpandInset = new int[2];

    public ExplosionField(Context context) {
        super(context);
        init();
    }

    public ExplosionField(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ExplosionField(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
    	//默认将数组mExpandInset的值初始化为32dp对应的px.
        Arrays.fill(mExpandInset, Utils.dp2Px(32));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        for (ExplosionAnimator explosion : mExplosions) {
            explosion.draw(canvas);
        }
    }

    public void expandExplosionBound(int dx, int dy) {
        mExpandInset[0] = dx;
        mExpandInset[1] = dy;
    }

    public void explode(Bitmap bitmap, Rect bound, long startDelay, long duration) {
    	//实例化一个ExplosionAnimator对象,传入三个参数
    	//View 当前view, Bitmap：view内容对应的bitmap对象, Rect：view的矩形可见区域
        final ExplosionAnimator explosion = new ExplosionAnimator(this, bitmap, bound);
        explosion.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
            	//当动画执行结束后，从动画集合中remove掉
                mExplosions.remove(animation);
            }
        });
        explosion.setStartDelay(startDelay);
        explosion.setDuration(duration);
        //动画未开始执行前将该ExplosionAnimator动画添加进mExplosions动画列表中
        mExplosions.add(explosion);
        explosion.start();//启动动画
    }

    public void explode(final View view) {
    	//先实例化一个Rect对象
        Rect r = new Rect();
        //获取view在屏幕中的可视区域
        view.getGlobalVisibleRect(r);
        //获取view在屏幕中的绝对坐标
        int[] location = new int[2];
        getLocationOnScreen(location);
        //调整Rect的位置
        r.offset(-location[0], -location[1]);
        r.inset(-mExpandInset[0], -mExpandInset[1]);
        //定义抖动动画效果 start
        int startDelay = 100;
        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f).setDuration(150);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            Random random = new Random();

            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                view.setTranslationX((random.nextFloat() - 0.5f) * view.getWidth() * 0.05f);
                view.setTranslationY((random.nextFloat() - 0.5f) * view.getHeight() * 0.05f);

            }
        });
        animator.start();
        //定义抖动动画效果 end
        
        //抖动动画结束后隐藏view内容
        view.animate().setDuration(150).setStartDelay(startDelay).scaleX(0f).scaleY(0f).alpha(0f).start();
        //接下来开始实现炸裂动画效果
        explode(Utils.createBitmapFromView(view), r, startDelay, ExplosionAnimator.DEFAULT_DURATION);
    }

    public void clear() {
        mExplosions.clear();
        invalidate();
    }

    public static ExplosionField attach2Window(Activity activity) {
    	//获取Activity的根布局
        ViewGroup rootView = (ViewGroup) activity.findViewById(Window.ID_ANDROID_CONTENT);
        //创建ExplosionField对象
        ExplosionField explosionField = new ExplosionField(activity);
        //设置ViewGroup.LayoutParams.MATCH_PARENT可以将炸裂区域设置为整个activity
        rootView.addView(explosionField, new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        return explosionField;
    }

}
