package com.ch.xseekbar;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ClipDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.v4.util.Pools;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.ViewParent;

import java.util.ArrayList;
import java.util.List;


/**
 * @author chen
 * @date 2019/4/23 下午4:37
 * email chenxiuyi@haier.com
 * <p>
 * 自定义SeekBar,可作为ProgressBar使用
 * 1.可设置显示方向{@link Direction#VERTICAL}:垂直方向 {@link Direction#HORIZONTAL}:水平方向
 * 2.进度值指示器
 * 3.显示刻度
 * 4.显示刻度说明
 * 5.进度、背景、刻度、滑块、指示器自定义{@link Drawable}
 * 6.移除了ProgressBar的secondProgress
 * <p>
 * 配置属性
 * @attr R.styleable#XSeekBar_minWidth 最小宽度,默认6px,如果progressDrawable未设置Size属性则使用该值作为宽度
 * @attr R.styleable#XSeekBar_minHeight 最小高度,默认6px,如果progressDrawable为设置Size属性则使用该值作为高度
 * @attr R.styleable#XSeekBar_maxWidth 最大宽度,默认100px
 * @attr R.styleable#XSeekBar_maxHeight 最大高度,默认100px
 * @attr R.styleable#XSeekBar_thumb 拖动触点Drawable
 * @attr R.styleable#XSeekBar_thumbTintMode
 * @attr R.styleable#XSeekBar_thumbTint
 * @attr R.styleable#XSeekBar_thumbOffset 触点偏移，作用于两端，默认0
 * @attr R.styleable#XSeekBar_tickMark 刻度Drawable
 * @attr R.styleable#XSeekBar_tickMarkTintMode
 * @attr R.styleable#XSeekBar_tickMarkTint
 * @attr R.styleable#XSeekBar_min 进度最小值 默认0
 * @attr R.styleable#XSeekBar_max 进度最大值 默认100
 * @attr R.styleable#XSeekBar_progress 当前进度值
 * @attr R.styleable#XSeekBar_progressDrawable 进度Drawable
 * @attr R.styleable#XSeekBar_orientation 进度条绘制方向水平/垂直 默认水平
 * @attr R.styleable#XSeekBar_indicatorPos 指示器相对于进度条的位置需依据orientation的定义
 * @attr R.styleable#XSeekBar_indicator 指示器Drawable
 * @attr R.styleable#XSeekBar_indicatorTintMode
 * @attr R.styleable#XSeekBar_indicatorTint
 * @attr R.styleable#XSeekBar_indicatorOffset 指示器相对于触点的偏移，偏移方向依赖于indicatorPos的定义，默认0
 * <p>
 * progressDrawable实例
 * <layer-list>
 * // 进度槽背景Drawable
 * <item android:id="@android:id/background">
 * <shape>
 * <solid android:color="#ffd1d1d1" />
 * <corners android:radius="10px" />
 * </shape>
 * </item>
 * // 进度背景Drawable
 * <item android:id="@android:id/progress">
 * <clip>
 * <shape>
 * <solid android:color="#ff00cc00" />
 * <corners android:radius="10px" />
 * </shape>
 * </clip>
 * </item>
 * </layer-list>
 * @see #setThumb(Drawable)
 * @see #setMin(int)
 * @see #setMax(int)
 * @see #setProgress(int)
 * @see #setProgressDrawable(Drawable)
 * @see #setIndicatorDrawable(Drawable)
 */
public class XSeekBar extends View {
    /**
     * 进度变换使用Drawable的setLevel实现
     * Drawable的level最大值为10000
     */
    private static final int MAX_LEVEL = 10000;

    int mMinWidth;
    int mMaxWidth;
    int mMinHeight;
    int mMaxHeight;

    private int mProgress;
    private int mMin;
    private int mMax;
    private int mDirection;
    private int mIndicatorPos;
    /**
     * 抽象比例
     * 将mMin-mMax映射到[0-10000]
     */
    private float mVisualProgress;

    private Drawable mThumbDrawable;
    private ColorStateList mThumbTintList = null;
    private PorterDuff.Mode mThumbTintMode = null;

    private Drawable mTickMarkDrawable;
    private ColorStateList mTickMarkTintList = null;
    private PorterDuff.Mode mTickMarkTintMode = null;

    private Drawable mIndicatorDrawable;
    private ColorStateList mIndicatorTintList = null;
    private PorterDuff.Mode mIndicatorTintMode = null;
    /**
     * 指示器相对于{@link #mThumbDrawable}的偏移
     */
    private int mIndicatorOffset;
    /**
     * 是否显示指示器
     */
    private boolean mIsShowIndicator;

    /**
     * 进度Drawable 推荐使用LayerDrawable
     * 使用{@link android.R.id#progress}定义进度
     * 使用{@link android.R.id#background}定义背景
     */
    private Drawable mProgressDrawable;
    private ColorStateList mProgressTintList = null;
    private PorterDuff.Mode mProgressTintMode = null;
    private PorterDuff.Mode mProgressBackgroundTintMode = null;
    private ColorStateList mProgressBackgroundTintList;

    private int mThumbOffset;

    private long mUiThreadId;

    private boolean mIsDragging;

    private boolean mRefreshIsPosted;

    private boolean mIsAttachedToWindow;

    private RefreshProgressRunnable mProgressRefreshRunnable;
    /**
     * 进度列表，子线程刷新进度使用
     */
    private List<RefreshData> mProgressListData = new ArrayList();

    private OnSeekBarChangeListener mOnSeekBarChangeListener;
    /**
     * 指示器内容Provider
     */
    private IndicatorContentProvider mIndicatorContentProvider;
    /**
     * 指示器内容画笔
     */
    private Paint mIndicatorContentPaint;

    private int mScaledTouchSlop;
    /**
     * 用户是否可拖动改变进度值
     */
    private boolean mIsUserSeekable = false;

    private float mTouchDownX;

    public XSeekBar(Context context) {
        super(context);
    }

    public XSeekBar(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public XSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, 0);
    }

    public XSeekBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

        this.mUiThreadId = Thread.currentThread().getId();
        initParam();

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.XSeekBar, defStyleAttr, defStyleRes);

        // 方向
        mDirection = a.getInt(R.styleable.XSeekBar_orientation, Direction.HORIZONTAL);
        mIndicatorPos = a.getInt(R.styleable.XSeekBar_indicatorPos, mDirection == Direction.HORIZONTAL ? IndicatorPosition.TOP : IndicatorPosition.LEFT);
        // 判断方向与指示器位置的匹配关系 水平只能上/下  垂直只能左/右
        if (mDirection == Direction.HORIZONTAL) {
            if (mIndicatorPos == IndicatorPosition.LEFT || mIndicatorPos == IndicatorPosition.RIGHT) {
                throw new IllegalArgumentException("水平方向指示器只能使用TOP或者BOTTOM作为方向");
            }
        } else {
            if (mIndicatorPos == IndicatorPosition.BOTTOM || mIndicatorPos == IndicatorPosition.TOP) {
                throw new IllegalArgumentException("垂直方向指示器只能使用LEFT或者RIGHT作为方向");
            }
        }

        // 触点Drawable
        final Drawable thumbDrawable = a.getDrawable(R.styleable.XSeekBar_thumb);
        if (a.hasValue(R.styleable.XSeekBar_thumbTintMode)) {
            mThumbTintMode = parseTintMode(a.getInt(R.styleable.XSeekBar_thumbTintMode, -1), mThumbTintMode);
        }

        if (a.hasValue(R.styleable.XSeekBar_thumbTint)) {
            mThumbTintList = a.getColorStateList(R.styleable.XSeekBar_thumbTint);
        }
        setThumb(thumbDrawable);

        // 触点偏移
        final int thumbOffset = a.getDimensionPixelOffset(R.styleable.XSeekBar_thumbOffset, getThumbOffset());
        setThumbOffset(thumbOffset);

        // 刻度线Drawable
        final Drawable tickMark = a.getDrawable(R.styleable.XSeekBar_tickMark);
        if (a.hasValue(R.styleable.XSeekBar_tickMarkTintMode)) {
            mTickMarkTintMode = parseTintMode(a.getInt(R.styleable.XSeekBar_tickMarkTintMode, -1), mTickMarkTintMode);
        }

        if (a.hasValue(R.styleable.XSeekBar_tickMarkTint)) {
            mTickMarkTintList = a.getColorStateList(R.styleable.XSeekBar_tickMarkTint);
        }
        setTickMark(tickMark);

        // 进度 Drawable
        final Drawable progressDrawable = a.getDrawable(R.styleable.XSeekBar_progressDrawable);
        if (progressDrawable != null) {
            if (needsTileify(progressDrawable)) {
                setProgressDrawableTiled(progressDrawable);
            } else {
                setProgressDrawable(progressDrawable);
            }
        }
        if (a.hasValue(R.styleable.XSeekBar_progressTintMode)) {
            this.mProgressTintMode = parseTintMode(a.getInt(R.styleable.XSeekBar_progressTintMode, -1), null);
        }
        if (a.hasValue(R.styleable.XSeekBar_progressTint)) {
            this.mProgressTintList = a.getColorStateList(R.styleable.XSeekBar_progressTint);
        }
        if (a.hasValue(R.styleable.XSeekBar_progressBackgroundTintMode)) {
            this.mProgressBackgroundTintMode = parseTintMode(a.getInt(R.styleable.XSeekBar_progressBackgroundTintMode, -1), null);
        }
        if (a.hasValue(R.styleable.XSeekBar_progressBackgroundTint)) {
            this.mProgressBackgroundTintList = a.getColorStateList(R.styleable.XSeekBar_progressBackgroundTint);
        }

        // 指示器Drawable
        final Drawable indicatorDrawable = a.getDrawable(R.styleable.XSeekBar_indicator);
        if (indicatorDrawable != null) {
            setIndicatorDrawable(indicatorDrawable);
        }
        if (a.hasValue(R.styleable.XSeekBar_indicatorTintMode)) {
            this.mIndicatorTintMode = parseTintMode(a.getInt(R.styleable.XSeekBar_indicatorTintMode, -1), null);
        }
        if (a.hasValue(R.styleable.XSeekBar_indicatorTint)) {
            this.mIndicatorTintList = a.getColorStateList(R.styleable.XSeekBar_indicatorTint);
        }

        mMinWidth = a.getDimensionPixelOffset(R.styleable.XSeekBar_minWidth, mMinWidth);
        mMaxWidth = a.getDimensionPixelOffset(R.styleable.XSeekBar_maxWidth, mMaxWidth);
        mMinHeight = a.getDimensionPixelOffset(R.styleable.XSeekBar_minHeight, mMinHeight);
        mMaxHeight = a.getDimensionPixelOffset(R.styleable.XSeekBar_maxHeight, mMaxHeight);

        setMin(a.getInt(R.styleable.XSeekBar_min, mMin));
        setMax(a.getInt(R.styleable.XSeekBar_max, mMax));

        setProgress(a.getInt(R.styleable.XSeekBar_progress, mProgress));

        a.recycle();

        applyThumbTint();
        applyTickMarkTint();
        applyProgressTints();
        applyIndicatorTint();
    }

    /**
     * 判断ProgressDrawable是否是{@link BitmapDrawable}类型
     * 或者{@link LayerDrawable} 和 {@link StateListDrawable}中是否包含有{@link BitmapDrawable}类型
     * 的Drawable
     *
     * @param dr
     * @return
     */
    private boolean needsTileify(Drawable dr) {
        if (dr instanceof LayerDrawable) {
            final LayerDrawable orig = (LayerDrawable) dr;
            final int N = orig.getNumberOfLayers();
            for (int i = 0; i < N; i++) {
                if (needsTileify(orig.getDrawable(i)) || mDirection == Direction.VERTICAL) {
                    return true;
                }
            }
            return false;
        }

        if (dr instanceof StateListDrawable) {
            return false;
        }

        if (dr instanceof BitmapDrawable) {
            return true;
        }

        return false;
    }

    /**
     * 初始化默认参数
     */
    private void initParam() {
        mMin = 0;
        mMax = 100;
        mProgress = 0;
        mMinWidth = 6;
        mMaxWidth = 100;
        mMinHeight = 6;
        mMaxHeight = 100;
        mDirection = Direction.HORIZONTAL;
        mIndicatorPos = IndicatorPosition.TOP;
        mIsUserSeekable = true;
        mScaledTouchSlop = ViewConfiguration.get(getContext()).getScaledTouchSlop();
        mIndicatorOffset = 0;
        // 指示器内容
        mIndicatorContentPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    }

    /**
     * 设置当前进度，可在子线程调用
     *
     * @param progress
     */
    public synchronized void setProgress(int progress) {
        setProgressInternal(progress, false, false);
    }

    /**
     * 在当前进度增加progressBy值
     * @param progressBy 正数是增加  负数是减小
     */
    public synchronized void setProgressBy(int progressBy) {
        setProgressInternal(this.mProgress + progressBy, false,false);
    }

    /**
     * 显示指示器
     */
    public void showIndicator() {
        this.mIsShowIndicator = true;
        if (this.mIndicatorDrawable != null) {
            this.invalidate(this.mIndicatorDrawable.getBounds());
        }
    }

    /**
     * 隐藏指示器
     */
    public void hidIndicator() {
        this.mIsShowIndicator = false;
        if (this.mIndicatorDrawable != null) {
            this.invalidate(this.mIndicatorDrawable.getBounds());
        }
    }

    /**
     * 指示器是否显示
     *
     * @return
     */
    public boolean isIndicatorShow() {
        return this.mIsShowIndicator;
    }

    /**
     * 修改当前进度值
     *
     * @param progress 进度值
     * @param fromUser 是否来自与用户操作
     * @param animate  是否需要动画
     * @return 进度是否修改，如果当前进度与设置的进度相同返回false
     */
    private synchronized boolean setProgressInternal(int progress, boolean fromUser, boolean animate) {
        if (progress > mMax) {
            progress = mMax;
        }
        if (progress < mMin) {
            progress = mMin;
        }

        if (progress == mProgress) {
            return false;
        }

        mProgress = progress;
        refreshProgress(mProgress, fromUser, animate);
        return true;
    }

    /**
     * 刷新进度，区分主线程和子线程
     *
     * @param progress
     * @param fromUser
     * @param animate
     */
    private synchronized void refreshProgress(int progress, boolean fromUser, boolean animate) {
        // 主线程处理
        if (mUiThreadId == Thread.currentThread().getId()) {
            doRefreshProgress(progress, fromUser, true, animate);
        }
        // 子线程处理
        else {
            if (mProgressRefreshRunnable == null) {
                mProgressRefreshRunnable = new RefreshProgressRunnable();
            }

            if (mIsAttachedToWindow && !mRefreshIsPosted) {
                post(mProgressRefreshRunnable);
                mRefreshIsPosted = true;
            }
        }
    }

    /**
     * 刷新进度
     *
     * @param progress
     * @param fromUser
     * @param callBackToApp
     * @param animate       TODO 未对动画做处理
     */
    private synchronized void doRefreshProgress(int progress, boolean fromUser, boolean callBackToApp, boolean animate) {
        int range = mMax - mMin;
        final float scale = range > 0 ? (progress - mMin) / (float) range : 0;
        setVisualProgress(scale);

        if (callBackToApp) {
            onProgressRefresh(scale, fromUser, progress);
        }
    }

    /**
     * 供子类实现的方法，进度变化会调用该方法
     *
     * @param scale
     * @param fromUser
     * @param progress
     */
    protected void onProgressRefresh(float scale, boolean fromUser, int progress) {
        if (this.mOnSeekBarChangeListener != null) {
            this.mOnSeekBarChangeListener.onProgressChanged(this, progress, fromUser);
        }
    }

    /**
     * 设置比例进度，从{@link #mMin}到{@link #mMax}的进度值最终需要映射到{@link Drawable#setLevel(int)}
     * 方法参数中，{@link Drawable}的Level取值是从[0-10000]，progress设置{@link #mMax}到10000的比例值
     *
     * @param progress [0...1]的比例值
     */
    private void setVisualProgress(float progress) {
        mVisualProgress = progress;

        // 修改进度值
        Drawable d = mProgressDrawable;
        if (d instanceof LayerDrawable) {
            d = ((LayerDrawable) d).findDrawableByLayerId(android.R.id.progress);
            if (d == null) {
                d = mProgressDrawable;
            }
        }
        if (d != null) {
            final int level = (int) (progress * MAX_LEVEL);
            d.setLevel(level);
        }

        // 更新触点位置
        if (mThumbDrawable != null) {
            updateThumbPos(getWidth(), getHeight(), mThumbDrawable, progress, 0);
        }

        // 更新指示器位置
        if (mIndicatorDrawable != null) {
            updateIndicatorPos(getWidth(), getHeight(), mIndicatorDrawable, progress);
        }

        invalidate();

        onVisualProgressChanged(progress);
    }

    /**
     * 供子类实现的方法，当
     *
     * @param progress
     */
    protected void onVisualProgressChanged(float progress) {
        // Stub method
    }

    /**
     * 设置拖动触点Drawable
     * 调用此方法会直接将{@link #mThumbOffset}设置为Drawable的宽度的1/3
     *
     * @param thumbDrawable 拖动触点Drawable 支持tint
     * @see #setThumbOffset(int)
     */
    public void setThumb(Drawable thumbDrawable) {
        final boolean needUpdate;
        if (mThumbDrawable != null && mThumbDrawable != thumbDrawable) {
            mThumbDrawable.setCallback(null);
            needUpdate = true;
        } else {
            needUpdate = false;
        }

        if (thumbDrawable != null) {
            thumbDrawable.setCallback(this);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && canResolveLayoutDirection()) {
                thumbDrawable.setLayoutDirection(getLayoutDirection());
            }

            // 默认mThumbOffset是ThumbDrawable宽度的1/2
            mThumbOffset = thumbDrawable.getIntrinsicWidth() / 2;

            if (needUpdate && (thumbDrawable.getIntrinsicHeight() != mThumbDrawable.getIntrinsicHeight() || thumbDrawable.getIntrinsicWidth() != mThumbDrawable.getIntrinsicWidth())) {
                requestLayout();
            }
        }

        mThumbDrawable = thumbDrawable;

        applyThumbTint();
        invalidate();

        if (needUpdate) {
            if (thumbDrawable != null && thumbDrawable.isStateful()) {
                int[] state = getDrawableState();
                thumbDrawable.setState(state);
            }
        }
    }

    /**
     * ProgressDrawable进行ClipDrawable处理后设置进度Drawable
     *
     * @param d
     */
    public void setProgressDrawableTiled(Drawable d) {
        if (d != null) {
            d = tileify(d, false);
        }

        setProgressDrawable(d);
    }

    /**
     * 设置指示器Drawable
     *
     * @param d
     */
    public void setIndicatorDrawable(Drawable d) {
        if (this.mIndicatorDrawable != d) {
            if (mIndicatorDrawable != null) {
                this.mIndicatorDrawable.setCallback(null);
            }

            mIndicatorDrawable = d;

            if (d != null) {
                d.setCallback(this);
                if (d.isStateful()) {
                    d.setState(getDrawableState());
                }
                boolean needRequestLayout = false;
                if (d.getIntrinsicWidth() > mMaxWidth || d.getIntrinsicWidth() > mMaxHeight) {
                    needRequestLayout = true;
                }
                if (d.getIntrinsicHeight() > mMaxWidth || d.getIntrinsicHeight() > mMaxHeight) {
                    needRequestLayout = true;
                }

                if (needRequestLayout) {
                    requestLayout();
                }

                invalidate();
            }
        }
    }

    /**
     * 设置进度Drawable
     * 推荐使用LayerDrawable
     * 并通过android.R.id.progress定义进度
     * android.R.id.background定义进度槽
     *
     * @param d
     */
    public void setProgressDrawable(Drawable d) {
        if (mProgressDrawable != d) {
            if (mProgressDrawable != null) {
                mProgressDrawable.setCallback(null);
                unscheduleDrawable(mProgressDrawable);
            }

            mProgressDrawable = d;

            if (d != null) {
                d.setCallback(this);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    d.setLayoutDirection(getLayoutDirection());
                }
                if (d.isStateful()) {
                    d.setState(getDrawableState());
                }

                int dHeight = d.getMinimumHeight();
                int dWidth = d.getMinimumWidth();

                boolean needRequestLayout = false;
                if (mMaxHeight < dHeight) {
                    mMaxHeight = dHeight;
                    needRequestLayout = true;
                }
                if (mMaxWidth < dWidth) {
                    mMaxWidth = dWidth;
                    needRequestLayout = true;
                }
                if (needRequestLayout) {
                    requestLayout();
                }

                applyProgressTints();
            }

            updateDrawableBounds(getWidth(), getHeight());
            updateDrawableState();

            doRefreshProgress(mProgress, false, false, false);
        }
    }

    /**
     * 获取用户是否可拖动改变进度值
     *
     * @return
     */
    public boolean getUserSeekable() {
        return this.mIsUserSeekable;
    }

    /**
     * 设置用户是否可拖动改变进度值
     *
     * @param seekbale
     */
    public void setUserSeekable(boolean seekbale) {
        this.mIsUserSeekable = seekbale;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!mIsUserSeekable || !isEnabled()) {
            return false;
        }

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (isInScrollingContainer()) {
                    mTouchDownX = event.getX();
                } else {
                    startDrag(event);
                }
                break;
            case MotionEvent.ACTION_MOVE:
                if (mIsDragging) {
                    trackTouchEvent(event);
                } else {
                    final float x = event.getX();
                    if (Math.abs(x - mTouchDownX) > mScaledTouchSlop) {
                        startDrag(event);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                if (mIsDragging) {
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                    setPressed(false);
                } else {
                    onStartTrackingTouch();
                    trackTouchEvent(event);
                    onStopTrackingTouch();
                }

                invalidate();
                break;
            case MotionEvent.ACTION_CANCEL:
                if (mIsDragging) {
                    onStopTrackingTouch();
                    setPressed(false);
                }
                invalidate();
                break;
            default:
        }
        return true;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        updateDrawableBounds(w, h);
        if (mThumbDrawable != null) {
            updateThumbPos(w, h, mThumbDrawable, mVisualProgress, 0);
        }

        if (mIndicatorDrawable != null) {
            updateIndicatorPos(w, h, mIndicatorDrawable, mVisualProgress);
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        // 绘制进度和背景
        drawTrack(canvas);
        // 绘制触点
        drawThumb(canvas);
        // 绘制指示器
        drawIndicator(canvas);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mIsAttachedToWindow = true;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mIsAttachedToWindow = false;
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        updateDrawableState();
    }

    @Override
    public void drawableHotspotChanged(float x, float y) {
        super.drawableHotspotChanged(x, y);

        if (mProgressDrawable != null) {
            mProgressDrawable.setHotspot(x, y);
        }
    }

    /**
     * 计算XSeekBar的尺寸
     *
     * @param widthMeasureSpec
     * @param heightMeasureSpec
     */
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int dw = 0;
        int dh = 0;
        int maxDrawableWidth, maxDrawableHeight;
        int indicatorWidth = 0, indicatorHeight = 0;
        int indicatorOffset = 0;

        final Drawable progressD = mProgressDrawable;
        final Drawable thumbD = mThumbDrawable;
        final Drawable indicatorD = mIndicatorDrawable;

        if (indicatorD != null) {
            indicatorWidth = indicatorD.getIntrinsicWidth();
            indicatorHeight = indicatorD.getIntrinsicHeight();

            indicatorWidth = indicatorWidth < 0 ? 0 : indicatorWidth;
            indicatorHeight = indicatorHeight < 0 ? 0 : indicatorHeight;
            indicatorOffset = mIndicatorOffset;
        }

        // 触点Drawable和进度Drawable中的最大宽高
        maxDrawableWidth = Math.max(progressD.getIntrinsicWidth(), thumbD.getIntrinsicWidth());
        maxDrawableHeight = Math.max(progressD.getIntrinsicHeight(), thumbD.getIntrinsicHeight());

        // 水平方向
        if (mDirection == Direction.HORIZONTAL) {
            dw = Math.max(mMinWidth, Math.min(mMaxWidth, maxDrawableWidth));
            dh = Math.max(mMinHeight, Math.min(mMaxHeight, maxDrawableHeight + indicatorOffset + indicatorHeight));
        }

        // 垂直方向
        if (mDirection == Direction.VERTICAL) {
            dw = Math.max(mMinWidth, Math.min(mMaxWidth, maxDrawableWidth + indicatorOffset + indicatorWidth));
            dh = Math.max(mMinHeight, Math.min(mMaxHeight, maxDrawableHeight));
        }

        updateDrawableState();

        dw += getPaddingLeft() + getPaddingRight();
        dh += getPaddingTop() + getPaddingBottom();

        final int measuredWidth = resolveSizeAndState(dw, widthMeasureSpec, 0);
        final int measuredHeight = resolveSizeAndState(dh, heightMeasureSpec, 0);
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    /**
     * 更新ProgressDrawable的状态
     */
    private void updateDrawableState() {
        final int[] state = getDrawableState();

        if (mProgressDrawable != null && mProgressDrawable.isStateful()) {
            if (mProgressDrawable.setState(state)) {
                invalidate();
            }
        }
    }

    /**
     * 绘制进度和背景
     * canvas需偏移出padding和指示器的宽/高以及{@link #mIndicatorOffset}
     * 如果触点的宽/高大于进度的宽/高，还需进一步偏移(指示器宽/高 - 进度宽/高) / 2 + 进度宽/高 / 2
     *
     * @param canvas
     */
    void drawTrack(Canvas canvas) {
        final Drawable d = mProgressDrawable;
        // 是否存在指示器
        final boolean hasIndicator = this.mIndicatorDrawable != null;

        if (d != null) {
            // 如果ProgressDrawable没有设置size属性则使用minXXX属性
            int progressDWidth = mProgressDrawable.getIntrinsicWidth() < 0 ? mMinWidth : mProgressDrawable.getIntrinsicWidth();
            int progressDHeight = mProgressDrawable.getIntrinsicHeight() < 0 ? mMinHeight : mProgressDrawable.getIntrinsicHeight();

            // 是否触点的宽/高大于进度的宽/高,如果是的话进度还要进一步的偏移
            final boolean isThumbWidthBigger = progressDWidth < mThumbDrawable.getIntrinsicWidth();
            final boolean isThumbHeightBigger = progressDHeight < mThumbDrawable.getIntrinsicHeight();
            int vSpace = 0, hSpace = 0;
            if (isThumbHeightBigger) {
                vSpace = (mThumbDrawable.getIntrinsicHeight() - progressDHeight) / 2;
            }
            if (isThumbWidthBigger) {
                hSpace = (mThumbDrawable.getIntrinsicWidth() - progressDWidth) / 2;
            }

            final int saveCount = canvas.save();
            // 指示器在进度上方
            if (mDirection == Direction.HORIZONTAL && hasIndicator && mIndicatorPos == IndicatorPosition.TOP) {
                canvas.translate(getPaddingLeft(), getPaddingTop() + mIndicatorDrawable.getIntrinsicHeight() + mIndicatorOffset + vSpace);
            }
            // 指示器在进度左侧
            else if (mDirection == Direction.VERTICAL && hasIndicator && mIndicatorPos == IndicatorPosition.LEFT) {
                canvas.translate(getPaddingLeft() + mIndicatorDrawable.getIntrinsicWidth() + mIndicatorOffset + hSpace, getPaddingTop());
            }
            // 水平方向，但是没有指示器
            else if (mDirection == Direction.HORIZONTAL) {
                canvas.translate(getPaddingLeft(), getPaddingTop() + vSpace);
            }
            // 垂直方向，没有指示器
            else {
                canvas.translate(getPaddingLeft() + hSpace, getPaddingTop());
            }

            d.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    /**
     * 绘制触点
     * canvas需偏移出padding和指示器的宽/高以及{@link #mIndicatorOffset}
     *
     * @param canvas
     */
    void drawThumb(Canvas canvas) {
        if (mThumbDrawable != null) {
            // 是否存在指示器
            final boolean hasIndicator = this.mIndicatorDrawable != null;
            final int thumbWidth = mThumbDrawable.getIntrinsicWidth();
            final int thumbHeight = mThumbDrawable.getIntrinsicHeight();

            final int saveCount = canvas.save();
            // 指示器在进度上方
            if (mDirection == Direction.HORIZONTAL && hasIndicator && mIndicatorPos == IndicatorPosition.TOP) {
                canvas.translate(getPaddingLeft() - thumbWidth / 2, getPaddingTop() + mIndicatorDrawable.getIntrinsicHeight() + mIndicatorOffset);
            }
            // 指示器在进度左侧
            else if (mDirection == Direction.VERTICAL && hasIndicator && mIndicatorPos == IndicatorPosition.LEFT) {
                canvas.translate(getPaddingLeft() + mIndicatorDrawable.getIntrinsicWidth() + mIndicatorOffset, getPaddingTop() - thumbHeight / 2);
            }
            // 水平方向，但是没有指示器
            else if (mDirection == Direction.HORIZONTAL) {
                canvas.translate(getPaddingLeft() - thumbWidth / 2, getPaddingTop());
            }
            // 垂直方向，没有指示器
            else {
                canvas.translate(getPaddingLeft(), getPaddingTop() - thumbHeight / 2);
            }

            mThumbDrawable.draw(canvas);
            canvas.restoreToCount(saveCount);
        }
    }

    /**
     * 绘制指示器和内容
     * canvas需偏移出padding,如果padding或者{@link #mIndicatorOffset}的值小于指示器宽/高的1/2有可能导致
     * 指示器移除XSeekBar的边界
     *
     * @param canvas
     */
    void drawIndicator(Canvas canvas) {
        if (mIndicatorDrawable != null) {
            final int saveCount = canvas.save();
            final int indicatorWidth = mIndicatorDrawable.getIntrinsicWidth();
            final int indicatorHeight = mIndicatorDrawable.getIntrinsicHeight();
            if (mDirection == Direction.HORIZONTAL) {
                canvas.translate(-indicatorWidth / 2, getPaddingTop());
            } else {
                canvas.translate(getPaddingLeft(), -(indicatorHeight / 2));
            }

            mIndicatorDrawable.draw(canvas);

            // 指示器内容
            if (mIndicatorContentProvider != null) {
                IndicatorFontInfo indicatorFontInfo = mIndicatorContentProvider.getIndicatorContent(mProgress, indicatorWidth, indicatorHeight);
                if (indicatorFontInfo != null) {
                    String content = indicatorFontInfo.getText();
                    if (!TextUtils.isEmpty(content)) {
                        if (mIndicatorContentPaint.getTextSize() != indicatorFontInfo.getTextSize()) {
                            mIndicatorContentPaint.setTextSize(indicatorFontInfo.getTextSize());
                        }
                        if (mIndicatorContentPaint.getColor() != indicatorFontInfo.getTextColor()) {
                            mIndicatorContentPaint.setColor(indicatorFontInfo.getTextColor());
                        }
                        if (indicatorFontInfo.isBold() && mIndicatorContentPaint.getTypeface() != null && !mIndicatorContentPaint.getTypeface().isBold()) {
                            Typeface font = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD);
                            mIndicatorContentPaint.setTypeface(font);
                        }
                        Rect indicatorBounds = mIndicatorDrawable.getBounds();
                        canvas.drawText(content, indicatorBounds.left + indicatorFontInfo.getOffsetX(), indicatorBounds.top + indicatorFontInfo.getOffsetY() + indicatorFontInfo.getTextSize(), mIndicatorContentPaint);
                    }
                }
            }
            canvas.restoreToCount(saveCount);
        }
    }

    /**
     * 更新ProgressDrawable的边界
     * 绘制Progress的时候Canvas已偏移出padding和指示器的宽/高，因此起点是0,0点
     *
     * @param w XSeekBar宽度
     * @param h XSeekBar高度
     */
    private void updateDrawableBounds(int w, int h) {
        w -= getPaddingRight() + getPaddingLeft();
        h -= getPaddingTop() + getPaddingBottom();

        int right = w;
        int bottom = h;
        int top = 0;
        int left = 0;

        if (mProgressDrawable != null) {
            int dW = mProgressDrawable.getIntrinsicWidth();
            int dH = mProgressDrawable.getIntrinsicHeight();
            // drawable如果未设置宽度和高度会返回-1,此时将使用mMinXXX值
            dW = dW <= 0 ? mMinWidth : dW;
            dH = dH <= 0 ? mMinHeight : dH;
            if (mDirection == Direction.HORIZONTAL) {
                mProgressDrawable.setBounds(0, 0, right, dH);
            } else {
                mProgressDrawable.setBounds(0, 0, dW, bottom);
            }
        }
    }

    /**
     * 更新触点位置,默认触点的中心点与进度两端对齐,如果没有设置正确的{@link #mThumbOffset}或者padding大于触点
     * 尺寸的1/2有可能导致触点溢出边界
     * 触点Canvas 已偏移出padding和可能的指示器宽/高，因此触点坐标还是以0,0点为起点
     *
     * @param w      XSeekBar的宽度
     * @param h      XSeekBar的高度
     * @param thumb  触点Drawable
     * @param scale  进度比例
     * @param offset 触点偏移 根据{@link #mDirection}
     *               如果是{@link Direction#HORIZONTAL}则offset表示垂直偏移
     *               如果是{@link Direction#VERTICAL}则offset表示水平偏移
     */
    private void updateThumbPos(int w, int h, Drawable thumb, float scale, int offset) {
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int paddingBottom = getPaddingBottom();

        int thumbWidth = this.mThumbDrawable.getIntrinsicWidth();
        int thumbHeight = this.mThumbDrawable.getIntrinsicHeight();

        int available = 0;

        int left = 0, right = 0, top = 0, bottom = 0;

        // 水平方向
        if (mDirection == Direction.HORIZONTAL) {
            available = w - paddingLeft - paddingRight;

            // 触点左侧位置
            final int thumbPos = (int) (mThumbOffset + scale * available - thumbWidth / 2);

            top = offset;
            bottom = offset + thumbHeight;
            left = thumbPos;
            right = left + thumbWidth;

            final Drawable background = getBackground();
            if (background != null) {
                final int offsetX = paddingLeft - mThumbOffset;
                final int offsetY = paddingTop;
                background.setHotspotBounds(left + offsetX, top + offsetY,
                        right + offsetX, bottom + offsetY);
            }
        }

        // 垂直方向
        if (mDirection == Direction.VERTICAL) {
            available = h - paddingTop - paddingBottom;

            // 触点顶部位置
            final int thumbPos = (int) (mThumbOffset + scale * available - thumbHeight / 2);

            top = thumbPos;
            bottom = top + thumbHeight;
            left = offset;
            right = offset + thumbWidth;

            final Drawable background = getBackground();
            if (background != null) {
                final int offsetX = paddingLeft;
                final int offsetY = paddingTop - mThumbOffset;
                background.setHotspotBounds(left + offsetX, top + offsetY,
                        right + offsetX, bottom + offsetY);
            }
        }

        thumb.setBounds(left, top, right, bottom);
    }

    /**
     * 更新指示器位置
     * canvas已偏移出相应的padding，因此此处还是以0,0点为起点
     *
     * @param w         View宽度
     * @param h         View高度
     * @param indicator 指示器Drawable
     * @param scale     进度比例
     * @see #mDirection
     * @see #mIndicatorPos
     */
    private void updateIndicatorPos(int w, int h, Drawable indicator, float scale) {
        if (indicator != null) {
            int paddingLeft = getPaddingLeft();
            int paddingRight = getPaddingRight();
            int paddingTop = getPaddingTop();
            int paddingBottom = getPaddingBottom();
            int available = 0;

            final Rect rect = new Rect(0, 0, 0, 0);
            final int width = indicator.getIntrinsicWidth();
            final int height = indicator.getIntrinsicHeight();

            // 水平方向
            if (mDirection == Direction.HORIZONTAL) {
                available = w - paddingLeft - paddingRight;

                final int left = (int) (mThumbOffset + scale * available);

                rect.left = left;
                rect.right = rect.left + width;
                // 指示器在进度上方
                if (mIndicatorPos == IndicatorPosition.TOP) {
                    rect.top = 0;
                    rect.bottom = rect.top + height;
                }
                // 指示器在进度下方
                else {
                    int progressDHeight = mProgressDrawable.getIntrinsicHeight() < 0 ? mMinHeight : mProgressDrawable.getIntrinsicHeight();
                    rect.top = Math.max(progressDHeight, mThumbDrawable.getIntrinsicHeight()) + mIndicatorOffset;
                    rect.bottom = rect.top + height;
                }
            }

            // 垂直方向
            if (mDirection == Direction.VERTICAL) {
                available = h - paddingTop - paddingBottom;

                final int top = (int) (mThumbOffset + scale * available);

                rect.top = top;
                rect.bottom = rect.top + height;
                // 指示器在进度左侧
                if (mIndicatorPos == IndicatorPosition.LEFT) {
                    rect.left = 0;
                    rect.right = rect.left + width;
                }
                // 指示器在进度右侧
                else {
                    int progressDWidth = mProgressDrawable.getIntrinsicWidth() < 0 ? mMinWidth : mProgressDrawable.getIntrinsicWidth();
                    rect.left = Math.max(progressDWidth, mThumbDrawable.getIntrinsicWidth()) + mIndicatorOffset;
                    rect.right = rect.left + width;
                }
            }

            indicator.setBounds(rect);
        }

    }

    /**
     * 获取进度监听器
     *
     * @return
     */
    public OnSeekBarChangeListener getOnSeekBarChangeListener() {
        return this.mOnSeekBarChangeListener;
    }

    /**
     * 设置进度变化的监听，另外可监听用户开始和接收触摸的手势动作
     *
     * @param l 监听器
     * @see OnSeekBarChangeListener
     */
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener l) {
        mOnSeekBarChangeListener = l;
    }

    /**
     * 设置指示器内容提供者
     *
     * @param provider
     */
    public void setIndicatorContentProvider(IndicatorContentProvider provider) {
        this.mIndicatorContentProvider = provider;
    }

    /**
     * 手指开始触摸XSeekBar
     *
     * @param event
     */
    private void startDrag(MotionEvent event) {
        setPressed(true);

        if (mThumbDrawable != null) {
            invalidate(mThumbDrawable.getBounds());
        }

        onStartTrackingTouch();
        trackTouchEvent(event);
        attemptClaimDrag();
    }

    /**
     * 用户手指开始触摸XSeekBar的回调方法
     */
    void onStartTrackingTouch() {
        mIsDragging = true;
        if (this.mOnSeekBarChangeListener != null) {
            this.mOnSeekBarChangeListener.onStartTrackingTouch(this);
        }
    }

    /**
     * 用户手指结束触摸XSeekBar的回调方法
     */
    void onStopTrackingTouch() {
        mIsDragging = false;
        if (this.mOnSeekBarChangeListener != null) {
            this.mOnSeekBarChangeListener.onStopTrackingTouch(this);
        }
    }

    /**
     * 声明该XSeekBar处理所有的触摸事件
     * 禁止父View拦截触摸事件
     */
    private void attemptClaimDrag() {
        if (getParent() != null) {
            getParent().requestDisallowInterceptTouchEvent(true);
        }
    }

    /**
     * 手指滑动事件处理
     *
     * @param event
     */
    private void trackTouchEvent(MotionEvent event) {
        final int paddingLeft = getPaddingLeft();
        final int paddingRight = getPaddingRight();
        final int paddingTop = getPaddingTop();
        final int paddingBottom = getPaddingBottom();

        final int x = Math.round(event.getX());
        final int y = Math.round(event.getY());
        final int width = getWidth();
        final int height = getHeight();

        final int range = mMax - mMin;

        float scale;
        int available;
        float progress = 0.0f;

        // 水平方向
        if (mDirection == Direction.HORIZONTAL) {
            available = width - paddingLeft - paddingRight;
            if (x < paddingLeft) {
                scale = 0.0f;
            } else if (x > width - paddingRight) {
                scale = 1.0f;
            } else {
                scale = (x - paddingLeft) / (float) available;
            }

            progress = scale * range;
        }

        // 垂直方向
        if (mDirection == Direction.VERTICAL) {
            available = height - paddingTop - paddingBottom;
            if (y < paddingTop) {
                scale = 0.0f;
            } else if (y > height - paddingBottom) {
                scale = 1.0f;
            } else {
                scale = (y - paddingTop) / (float) available;
            }

            progress = scale * range;
        }

        setHotspot(x, y);
        setProgressInternal(Math.round(progress), true, false);
    }

    /**
     * 设置背景Drawable的波纹显示位置
     *
     * @param x
     * @param y
     */
    private void setHotspot(float x, float y) {
        final Drawable bg = getBackground();
        if (bg != null) {
            bg.setHotspot(x, y);
        }
    }

    /**
     * 判断XSeekBar的父容器是否是一个可滚动的ViewGroup
     * 该方法来自与View，但因为该方法不可见所以在此处在此定义
     *
     * @return
     */
    private boolean isInScrollingContainer() {
        ViewParent p = getParent();
        while (p != null && p instanceof ViewGroup) {
            if (((ViewGroup) p).shouldDelayChildPressedState()) {
                return true;
            }
            p = p.getParent();
        }
        return false;
    }

    /**
     * XSeekBar推荐使用{@link ClipDrawable}作为ProgressDrawable
     * 如果用户设置的ProgressDrawable 并非ClipDrawable则需进行处理
     * 如果用户设置的ProgressDrawable是 {@link BitmapDrawable}类型
     * 会将该Drawable的tintModeXY设置为{@link Shader.TileMode#REPEAT}
     *
     * @param drawable 待处理的Drawable
     * @param clip     true:返回ClipDrawable对象 false:返回drawable原始类型
     * @return 返回经过处理的Drawable
     * @see ClipDrawable
     */
    private Drawable tileify(Drawable drawable, boolean clip) {
        if (drawable instanceof LayerDrawable) {
            final LayerDrawable orig = (LayerDrawable) drawable;
            final int N = orig.getNumberOfLayers();
            final Drawable[] outDrawables = new Drawable[N];

            for (int i = 0; i < N; i++) {
                final int id = orig.getId(i);
                outDrawables[i] = tileify(orig.getDrawable(i), id == android.R.id.progress);
            }

            final LayerDrawable clone = new LayerDrawable(outDrawables);
            for (int i = 0; i < N; i++) {
                clone.setId(i, orig.getId(i));
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    clone.setLayerGravity(i, orig.getLayerGravity(i));
                    clone.setLayerWidth(i, orig.getLayerWidth(i));
                    clone.setLayerHeight(i, orig.getLayerHeight(i));
                    clone.setLayerInsetLeft(i, orig.getLayerInsetLeft(i));
                    clone.setLayerInsetRight(i, orig.getLayerInsetRight(i));
                    clone.setLayerInsetTop(i, orig.getLayerInsetTop(i));
                    clone.setLayerInsetBottom(i, orig.getLayerInsetBottom(i));
                    clone.setLayerInsetStart(i, orig.getLayerInsetStart(i));
                    clone.setLayerInsetEnd(i, orig.getLayerInsetEnd(i));
                    clone.setLayoutDirection(orig.getLayoutDirection());
                }
            }

            return clone;
        }

        if (drawable instanceof StateListDrawable) {
            return drawable;
        }

        if (drawable instanceof BitmapDrawable) {
            final Drawable.ConstantState cs = drawable.getConstantState();
            final BitmapDrawable clone = (BitmapDrawable) cs.newDrawable(getResources());
            if (mDirection == Direction.HORIZONTAL) {
                clone.setTileModeXY(Shader.TileMode.REPEAT, Shader.TileMode.CLAMP);
            } else {
                clone.setTileModeXY(Shader.TileMode.CLAMP, Shader.TileMode.REPEAT);
            }

            if (clip) {
                if (mDirection == Direction.HORIZONTAL) {
                    return new ClipDrawable(clone, Gravity.LEFT, ClipDrawable.HORIZONTAL);
                } else {
                    return new ClipDrawable(clone, Gravity.TOP, ClipDrawable.VERTICAL);
                }
            } else {
                return clone;
            }
        }

        if (clip && mDirection == Direction.VERTICAL) {
            if (drawable instanceof ClipDrawable && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                return new ClipDrawable(((ClipDrawable) drawable).getDrawable(), Gravity.TOP, ClipDrawable.VERTICAL);
            } else {
                return new ClipDrawable(drawable, Gravity.TOP, ClipDrawable.VERTICAL);
            }
        } else {
            return drawable;
        }
    }

    /**
     * 设置触点偏移
     *
     * @return
     */
    public int getThumbOffset() {
        return this.mThumbOffset;
    }

    /**
     * 设置拖动触点相对于两端的偏移
     *
     * @param thumbOffset
     */
    public void setThumbOffset(int thumbOffset) {
        this.mThumbOffset = thumbOffset;
        invalidate();
    }

    /**
     * 设置最小值
     *
     * @param min
     */
    public synchronized void setMin(int min) {
        if (min > mMax) {
            min = mMax;
        }
        if (min != mMin) {
            mMin = min;
            postInvalidate();

            if (mProgress < min) {
                mProgress = min;
            }
            refreshProgress(mProgress, false, false);
        } else {
            mMin = min;
        }
    }

    /**
     * 设置最大值
     *
     * @param max
     */
    public synchronized void setMax(int max) {
        if (max < mMin) {
            max = mMin;
        }
        if (max != mMax) {
            mMax = max;
            postInvalidate();

            if (mProgress > max) {
                mProgress = max;
            }
            refreshProgress(mProgress, false, false);
        } else {
            mMax = max;
        }
    }

    /**
     * 设置刻度线Drawable
     *
     * @param tickMarkDrawable
     */
    public void setTickMark(Drawable tickMarkDrawable) {
        if (mTickMarkDrawable != null) {
            mTickMarkDrawable.setCallback(null);
        }

        mTickMarkDrawable = tickMarkDrawable;

        if (tickMarkDrawable != null) {
            tickMarkDrawable.setCallback(this);
            if (canResolveLayoutDirection() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                tickMarkDrawable.setLayoutDirection(getLayoutDirection());
            }
            applyTickMarkTint();
        }

        invalidate();
    }

    /**
     * 将布局文件中的tintMode值转为 {@link PorterDuff.Mode}
     * 因该方法是Drawable的内部方法，因此需提取出来
     */
    private PorterDuff.Mode parseTintMode(int value, PorterDuff.Mode defaultMode) {
        switch (value) {
            case 3:
                return PorterDuff.Mode.SRC_OVER;
            case 5:
                return PorterDuff.Mode.SRC_IN;
            case 9:
                return PorterDuff.Mode.SRC_ATOP;
            case 14:
                return PorterDuff.Mode.MULTIPLY;
            case 15:
                return PorterDuff.Mode.SCREEN;
            case 16:
                return PorterDuff.Mode.ADD;
            default:
                return defaultMode;
        }
    }

    /**
     * 配置进度和背景Drawable tint及State
     */
    private void applyProgressTints() {
        if (mProgressDrawable != null && (mProgressTintMode != null || mProgressTintList != null)) {
            // 进度
            final Drawable progressD = getTargetDrawable(android.R.id.progress);
            if (progressD != null) {
                progressD.setTintMode(mProgressTintMode);
                progressD.setTintList(mProgressTintList);
                if (progressD.isStateful()) {
                    progressD.setState(getDrawableState());
                }
            }

            // 背景
            final Drawable backgroundD = getTargetDrawable(android.R.id.background);
            if (backgroundD != null) {
                backgroundD.setTintMode(mProgressBackgroundTintMode);
                backgroundD.setTintList(mProgressBackgroundTintList);
                if (backgroundD.isStateful()) {
                    backgroundD.setState(getDrawableState());
                }
            }
        }
    }

    /**
     * 从{@link #mProgressDrawable} 中读取指定layerId对应的Drawable
     *
     * @param id layerId
     * @return id 对应的layerId Drawable
     * 如果{@link #mProgressDrawable}不是{@link LayerDrawable}类型，则直接返回{@link #mProgressDrawable}
     */
    private Drawable getTargetDrawable(int id) {
        Drawable layer = null;

        final Drawable d = mProgressDrawable;
        if (d != null) {
            mProgressDrawable = d.mutate();

            if (d instanceof LayerDrawable) {
                layer = ((LayerDrawable) d).findDrawableByLayerId(id);
            } else {
                return d;
            }
        }

        return layer;
    }

    /**
     * 配置触点Drawable tint及State
     */
    private void applyThumbTint() {
        if (mThumbDrawable != null && (mThumbTintList != null || mThumbTintMode != null)) {
            mThumbDrawable = mThumbDrawable.mutate();

            mThumbDrawable.setTintList(mThumbTintList);

            mThumbDrawable.setTintMode(mThumbTintMode);

            if (mThumbDrawable.isStateful()) {
                mThumbDrawable.setState(getDrawableState());
            }
        }
    }

    /**
     * 配置指示器Drawable tint及State
     */
    private void applyIndicatorTint() {
        if (mIndicatorDrawable != null && (mIndicatorTintList != null || mIndicatorTintMode != null)) {
            mIndicatorDrawable = mIndicatorDrawable.mutate();

            mIndicatorDrawable.setTintList(mIndicatorTintList);

            mIndicatorDrawable.setTintMode(mIndicatorTintMode);

            if (mIndicatorDrawable.isStateful()) {
                mIndicatorDrawable.setState(getDrawableState());
            }
        }
    }

    /**
     * 配置刻度线Drawable tint及State
     */
    private void applyTickMarkTint() {
        if (mTickMarkDrawable != null && (mTickMarkTintMode != null || mTickMarkTintList != null)) {

            mTickMarkDrawable.setTintList(mTickMarkTintList);

            mTickMarkDrawable.setTintMode(mThumbTintMode);

            if (mTickMarkDrawable.isStateful()) {
                mTickMarkDrawable.setState(getDrawableState());
            }
        }
    }

    /**
     * 进度变化的监听
     */
    public interface OnSeekBarChangeListener {

        /**
         * 进度变化通知
         *
         * @param seekBar
         * @param progress 当前进度值min-max
         * @param fromUser 是否是用户触摸导致进度变化
         */
        void onProgressChanged(XSeekBar seekBar, int progress, boolean fromUser);

        /**
         * 用户开始触摸进度的通知
         *
         * @param seekBar
         */
        void onStartTrackingTouch(XSeekBar seekBar);

        /**
         * 用户结束触摸进度的通知
         *
         * @param seekBar
         */
        void onStopTrackingTouch(XSeekBar seekBar);
    }

    /**
     * 指示器内容Provider
     */
    public interface IndicatorContentProvider {
        /**
         * 获取指示器内容
         *
         * @param progress        当前进度
         * @param indicatorWidth  指示器Drawable的宽度
         * @param indicatorHeight 指示器Drawable的高度
         * @return
         */
        IndicatorFontInfo getIndicatorContent(int progress, int indicatorWidth, int indicatorHeight);
    }

    /**
     * 显示方向定义
     */
    public static final class Direction {
        /**
         * 垂直显示
         */
        public static int VERTICAL = 2;
        /**
         * 水平显示
         */
        public static int HORIZONTAL = 1;
    }

    /**
     * 定义指示器显示位置
     * 根据{@link Direction}判断
     *
     * @see Direction#HORIZONTAL 上/下
     * @see Direction#VERTICAL 左/右
     */
    public static final class IndicatorPosition {
        public static final int TOP = 2;
        public static final int BOTTOM = 4;

        public static final int LEFT = 1;
        public static final int RIGHT = 3;
    }

    /**
     * 指示器文字信息配置
     * 包括字体颜色、字体大小、文字偏移
     * 系统会频繁创建该对象，为避免内存泄漏，使用对象池
     *
     * @see Pools.SynchronizedPool
     */
    public static final class IndicatorFontInfo {
        private static final int MAX_POOL = 10;
        private static final Pools.SynchronizedPool<IndicatorFontInfo> pool = new Pools.SynchronizedPool<>(MAX_POOL);
        private String text;
        private int textColor;
        private int textSize;
        /**
         * 是否文字加粗
         */
        private boolean bold;
        /**
         * 文字左上角相对于指示器(0,0)点偏移
         */
        private int offsetX;
        private int offsetY;

        /**
         * 私有化构造方法，避免随意创建对象
         */
        private IndicatorFontInfo() {
            // 使用默认值初始化
            this("", Color.BLACK, 18, false, 0, 0);
        }

        private IndicatorFontInfo(String text, int textColor, int textSize, boolean bold, int offsetX, int offsetY) {
            this.text = text;
            this.textColor = textColor;
            this.textSize = textSize;
            this.offsetX = offsetX;
            this.offsetY = offsetY;
            this.bold = bold;
        }

        /**
         * 唯一实例化方法，为避免内存泄漏，必须使用该入口实例化
         *
         * @param text
         * @param textColor
         * @param textSize
         * @param offsetX
         * @param offsetY
         * @return
         */
        public static IndicatorFontInfo acquire(String text, int textColor, int textSize, boolean bold, int offsetX, int offsetY) {
            IndicatorFontInfo info = pool.acquire();
            if (info == null) {
                info = new IndicatorFontInfo(text, textColor, textSize, bold, offsetX, offsetY);
            }
            return info;
        }

        public void recycle() {
            pool.release(this);
        }

        public String getText() {
            return text == null ? "" : text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public int getTextColor() {
            return textColor;
        }

        public void setTextColor(int textColor) {
            this.textColor = textColor;
        }

        public int getTextSize() {
            return textSize;
        }

        public void setTextSize(int textSize) {
            this.textSize = textSize;
        }

        public int getOffsetX() {
            return offsetX;
        }

        public void setOffsetX(int offsetX) {
            this.offsetX = offsetX;
        }

        public int getOffsetY() {
            return offsetY;
        }

        public void setOffsetY(int offsetY) {
            this.offsetY = offsetY;
        }

        public boolean isBold() {
            return this.bold;
        }

        public void setBold(boolean bold) {
            this.bold = bold;
        }
    }

    /**
     * 进度刷新bean，在子线程中刷新进度使用，使用对象池保存
     */
    private static class RefreshData {
        private static final int MAX_POOL_SIZE = 24;
        private static final Pools.SynchronizedPool<RefreshData> pool = new Pools.SynchronizedPool<>(MAX_POOL_SIZE);

        public int id;
        public int progress;
        public boolean fromUser;
        public boolean animate;

        public static RefreshData obtain(int id, int progress, boolean fromUser, boolean animate) {
            RefreshData obj = pool.acquire();
            if (obj == null) {
                obj = new RefreshData();
            }

            obj.id = id;
            obj.progress = progress;
            obj.fromUser = fromUser;
            obj.animate = animate;

            return obj;
        }

        public void recycle() {
            pool.release(this);
        }
    }

    /**
     * 在子线程刷新进度
     */
    private class RefreshProgressRunnable implements Runnable {

        @Override
        public void run() {
            synchronized (XSeekBar.this) {
                final int size = mProgressListData.size();
                for (int i = 0; i < size; i++) {
                    final RefreshData rd = mProgressListData.get(i);
                    doRefreshProgress(rd.progress, rd.fromUser, true, rd.animate);
                    rd.recycle();
                }
                mProgressListData.clear();
                mRefreshIsPosted = false;
            }
        }
    }
}
