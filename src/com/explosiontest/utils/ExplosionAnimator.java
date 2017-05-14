 package com.explosiontest.utils;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Interpolator;

import java.util.Random;

//粒子碎裂爆炸动画效果
public class ExplosionAnimator extends ValueAnimator {

	//声明动画执行时长
    static long DEFAULT_DURATION = 0x400;
    //声明动画插值器
    private static final Interpolator DEFAULT_INTERPOLATOR = new AccelerateInterpolator(0.6f);
    
    private static final float END_VALUE = 1.4f;
    //声明
    private static final float X = Utils.dp2Px(5);
    private static final float Y = Utils.dp2Px(20);
    private static final float V = Utils.dp2Px(2);
    private static final float W = Utils.dp2Px(1);
    //声明画笔对象
    private Paint mPaint;
    //声明粒子集合
    private Particle[] mParticles;
    //声明矩形对象
    private Rect mRect;
    //声明一个view容器
    private View mContainer;

    public ExplosionAnimator(View container, Bitmap bitmap, Rect bound) {
        mPaint = new Paint();
        mRect = new Rect(bound);
        int partLen = 15;
        //粒子个数为partLen*partLen
        mParticles = new Particle[partLen * partLen];
        //random：按时间随机数
        Random random = new Random(System.currentTimeMillis());
        //bitmap:将Bitmap等分成了17*17个矩阵块
        int w = bitmap.getWidth() / (partLen + 2);
        int h = bitmap.getHeight() / (partLen + 2);
        for (int i = 0; i < partLen; i++) {
            for (int j = 0; j < partLen; j++) {
            	//封装粒子对象数组
            	//bitmap.getPixel获取图片（即icon图片）的像素点((j + 1) * w, (i + 1) * h)的色值
                mParticles[(i * partLen) + j] = generateParticle(bitmap.getPixel((j + 1) * w, (i + 1) * h), random);
            }
        }
        mContainer = container;
        
        //对应ValueAnimator.ofFloat函数,粒子在DEFAULT_DURATION时间内按DEFAULT_INTERPOLATOR插值器由0-END_VALUE进行变化
        setFloatValues(0f, END_VALUE);
        setInterpolator(DEFAULT_INTERPOLATOR);
        setDuration(DEFAULT_DURATION);
    }

    //
    private Particle generateParticle(int color, Random random) {
        Particle particle = new Particle();
        particle.color = color;
        particle.radius = V;
        //nextFloat()返回一个随机数在 [0.0 , 1.0) 之间均匀分布的 float 值
        if (random.nextFloat() < 0.2f) {
            particle.baseRadius = V + ((X - V) * random.nextFloat());
        } else {
            particle.baseRadius = W + ((V - W) * random.nextFloat());
        }
        float nextFloat = random.nextFloat();
        particle.top = mRect.height() * ((0.18f * random.nextFloat()) + 0.2f);
        particle.top = nextFloat < 0.2f ? particle.top : particle.top + ((particle.top * 0.2f) * random.nextFloat());
        particle.bottom = (mRect.height() * (random.nextFloat() - 0.5f)) * 1.8f;
        float f = nextFloat < 0.2f ? particle.bottom : nextFloat < 0.8f ? particle.bottom * 0.6f : particle.bottom * 0.3f;
        particle.bottom = f;
        particle.mag = 4.0f * particle.top / particle.bottom;
        particle.neg = (-particle.mag) / particle.bottom;
        f = mRect.centerX() + (Y * (random.nextFloat() - 0.5f));
        particle.baseCx = f;
        particle.cx = f;
        f = mRect.centerY() + (Y * (random.nextFloat() - 0.5f));
        particle.baseCy = f;
        particle.cy = f;
        particle.life = END_VALUE / 10 * random.nextFloat();
        particle.overflow = 0.4f * random.nextFloat();
        particle.alpha = 1f;
        return particle;
    }

    public boolean draw(Canvas canvas) {
    	/* 
         * 该属性动画只是起到一个计时作用，与爆炸效果没有其他联系。若动画时间还没结束，则依次计算每个粒子当前的各属性值（坐标，半径），然后依次绘制每个粒子 
         * （  canvas.drawCircle(particle.cx, particle.cy, particle.radius, mPaint);  ）， 
         * 然后调用mContainer.invalidate()，调用该函数后，会自动调用mContainer(即 ExplosionField对象)的onDraw(Canvas canvas) 方法，该方法内部 
         * 又调用了explosion（即 ExplosionAnimator对象）的draw方法（即 下面的方法），所以又会继续绘制。所以ExplosionAnimator的draw()和ExplosionField的onDraw 
         * 反复相互调用，直到动画时间终止为止。 这种相互调用方式跟scroller和computeScroll(),onDraw()之间的使用方式很像 
         */ 
        if (!isStarted()) {
            return false;
        }
        for (Particle particle : mParticles) {
            particle.advance((float) getAnimatedValue());
            if (particle.alpha > 0f) {
                mPaint.setColor(particle.color);
                mPaint.setAlpha((int) (Color.alpha(particle.color) * particle.alpha));
                canvas.drawCircle(particle.cx, particle.cy, particle.radius, mPaint);
            }
        }
        mContainer.invalidate();
        return true;
    }

    @Override
    public void start() {
        super.start();
        mContainer.invalidate(mRect);
    }

    //定义一个粒子类型，对粒子的透明度、颜色、圆心x/y坐标、圆半径、起点x/y坐标、初始圆半径、顶部距离、底部距离、生命周期等参数进行的初始化
    private class Particle {
        float alpha;
        int color;
        float cx;//粒子当前中心x坐标  
        float cy;//粒子当前中心y坐标  
        float radius;  
        float baseCx;//粒子原始x坐标  
        float baseCy;//粒子原始y坐标  
        float baseRadius;//粒子原始半径  
        float top;
        float bottom;
        float mag;
        float neg;
        float life;
        float overflow;

        /** 
         * 计算爆炸后该粒子的圆心坐标和半径 
         */ 
        public void advance(float factor) {
            float f = 0f;
            float normalization = factor / END_VALUE;
            if (normalization < life || normalization > 1f - overflow) {
                alpha = 0f;
                return;
            }
            normalization = (normalization - life) / (1f - life - overflow);
            float f2 = normalization * END_VALUE;
            if (normalization >= 0.7f) {
                f = (normalization - 0.7f) / 0.3f;
            }
            alpha = 1f - f;
            f = bottom * f2;
            cx = baseCx + f;
            cy = (float) (baseCy - this.neg * Math.pow(f, 2.0)) - f * mag;
            radius = V + (baseRadius - V) * f2;
        }
    }
}
