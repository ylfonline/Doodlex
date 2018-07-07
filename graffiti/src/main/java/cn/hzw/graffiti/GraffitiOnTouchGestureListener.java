package cn.hzw.graffiti;

import android.animation.ValueAnimator;
import android.graphics.Path;
import android.graphics.PointF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import java.util.List;

import cn.forward.androids.TouchGestureDetector;
import cn.hzw.graffiti.core.IGraffitiItem;
import cn.hzw.graffiti.core.IGraffitiSelectableItem;

import static cn.hzw.graffiti.util.DrawUtil.computeAngle;

/**
 * GraffitiView的涂鸦手势监听
 * Created on 30/06/2018.
 */

public class GraffitiOnTouchGestureListener extends TouchGestureDetector.OnTouchGestureListener {
    private static final float VALUE = 1f;

    // 触摸的相关信息
    private float mTouchX, mTouchY;
    private float mLastTouchX, mLastTouchY;
    private float mTouchDownX, mTouchDownY;

    // 缩放相关
    private Float mLastFocusX;
    private Float mLastFocusY;
    private float mTouchCentreX, mTouchCentreY;


    private float mSelectedItemX, mSelectedItemY;
    private float mRotateTextDiff; // 开始旋转图片时的差值（当前图片与触摸点的角度）

    private Path mCurrPath; // 当前手写的路径
    private GraffitiPath mCurrGraffitiPath;
    private CopyLocation mCopyLocation;

    private GraffitiView mGraffiti;

    // 动画相关
    private ValueAnimator mScaleAnimator;
    private float mAnimTransX, mAnimTranY;

    private IGraffitiSelectableItem mSelectedItem; // 当前选中的item
    private ISelectionListener mListener;

    public GraffitiOnTouchGestureListener(GraffitiView graffiti, ISelectionListener listener) {
        mGraffiti = graffiti;
        mCopyLocation = GraffitiPen.COPY.getCopyLocation();
        mCopyLocation.reset();
        mGraffiti.addGraffitiViewListener(new GraffitiView.GraffitiViewListener() {
            @Override
            public void onActionOccur(int action, Object obj) {
                if (action == GraffitiView.ACTION_ROTATION) {
                    mCopyLocation.rotatePosition(mGraffiti, (int) obj);
                }
            }
        });
        mListener = listener;
    }

    public void setSelectedItem(IGraffitiSelectableItem selectedItem) {
        if (mSelectedItem != null) {
            mSelectedItem.setSelected(false);
        }
        if (selectedItem != null) {
            selectedItem.setSelected(true);
        }
        this.mSelectedItem = selectedItem;
    }

    public IGraffitiSelectableItem getSelectedItem() {
        return mSelectedItem;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mTouchX = mTouchDownX = e.getX();
        mTouchY = mTouchDownY = e.getY();
        mGraffiti.enableAmplifier(false); // 关闭放大镜
        return true;
    }

    /**
     * 开始滚动
     *
     * @param event
     */
    @Override
    public void onScrollBegin(MotionEvent event) {
        mLastTouchX = mTouchX = event.getX();
        mLastTouchY = mTouchY = event.getY();

        if (mGraffiti.getPen().isSelectable()) {
            // 判断是否点中选择区域
            if (mSelectedItem != null) {
                PointF xy = mSelectedItem.getLocation();
                mSelectedItemX = xy.x;
                mSelectedItemY = xy.y;
                // 旋转
                if (mSelectedItem.isCanRotate(mGraffiti.toX(mTouchX), mGraffiti.toY(mTouchY))) {
                    mSelectedItem.setIsRotating(true);
                    mRotateTextDiff = mSelectedItem.getItemRotate() -
                            computeAngle(xy.x, xy.y, mGraffiti.toX(mTouchX), mGraffiti.toY(mTouchY));
                }
            }
        } else {
            mGraffiti.enableAmplifier(true); // 涂鸦时开启放大镜
            // 点击copy
            if (mGraffiti.getPen() == GraffitiPen.COPY && mCopyLocation.isInIt(mGraffiti.toX(mTouchX), mGraffiti.toY(mTouchY), mGraffiti.getSize())) {
                mCopyLocation.setRelocating(true);
                mCopyLocation.setCopying(false);
            } else {
                if (mGraffiti.getPen() == GraffitiPen.COPY) {
                    mCopyLocation.setRelocating(false);
                    if (!mCopyLocation.isCopying()) {
                        mCopyLocation.setCopying(true);
                        mCopyLocation.setStartPosition(mGraffiti.toX(mTouchX), mGraffiti.toY(mTouchY));
//                        mCurrGraffitiPath.updateCopy(mGraffiti.toX(mTouchX), mGraffiti.toY(mTouchY), mCopyLocation.getX(), mCopyLocation.getY());
                    }
                }

                // 初始化绘制
                mCurrPath = new Path();
                mCurrPath.moveTo(mGraffiti.toX(mTouchX), mGraffiti.toY(mTouchY));
                if (mGraffiti.getShape() == GraffitiShape.HAND_WRITE) { // 手写
                    mCurrGraffitiPath = GraffitiPath.toPath(mGraffiti, mCurrPath);
                } else {  // 画图形
                    mCurrGraffitiPath = GraffitiPath.toShape(mGraffiti,
                            mGraffiti.toX(mTouchDownX), mGraffiti.toY(mTouchDownY), mGraffiti.toX(mTouchX), mGraffiti.toY(mTouchY));
                }
                mCurrGraffitiPath.setDrawOptimize(false);
                mGraffiti.addItem(mCurrGraffitiPath);
            }
        }
        mGraffiti.invalidate();
    }

    @Override
    public void onScrollEnd(MotionEvent e) {
        mLastTouchX = mTouchX;
        mLastTouchY = mTouchY;
        mTouchX = e.getX();
        mTouchY = e.getY();

        if (mGraffiti.getPen().isSelectable()) {
            mSelectedItem.setIsRotating(false);
        } else {
            if (mCurrGraffitiPath != null) {
                mCurrGraffitiPath.setDrawOptimize(true);
                mGraffiti.invalidate(mCurrGraffitiPath);
                mCurrGraffitiPath = null;
            }
        }
        mGraffiti.enableAmplifier(false); // 关闭放大镜
        mGraffiti.invalidate();
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        mLastTouchX = mTouchX;
        mLastTouchY = mTouchY;
        mTouchX = e2.getX();
        mTouchY = e2.getY();

        if (mGraffiti.getPen().isSelectable()) { //画笔是否是可选择的
            if (mSelectedItem != null) {
                if (mSelectedItem.isRotating()) { // 旋转item
                    PointF xy = mSelectedItem.getLocation();
                    mSelectedItem.setItemRotate(mRotateTextDiff + computeAngle(
                            xy.x, xy.y, mGraffiti.toX(mTouchX), mGraffiti.toY(mTouchY)
                    ));
                } else { // 移动item
                    mSelectedItem.setLocation(
                            mSelectedItemX + mGraffiti.toX(mTouchX) - mGraffiti.toX(mTouchDownX),
                            mSelectedItemY + mGraffiti.toY(mTouchY) - mGraffiti.toY(mTouchDownY));
                }
            }
        } else {
            if (mGraffiti.getPen() == GraffitiPen.COPY && mCopyLocation.isRelocating()) {
                // 正在定位location
                mCopyLocation.updateLocation(mGraffiti.toX(mTouchX), mGraffiti.toY(mTouchY));
            } else {
                if (mGraffiti.getPen() == GraffitiPen.COPY) {
                    mCopyLocation.updateLocation(mCopyLocation.getCopyStartX() + mGraffiti.toX(mTouchX) - mCopyLocation.getTouchStartX(),
                            mCopyLocation.getCopyStartY() + mGraffiti.toY(mTouchY) - mCopyLocation.getTouchStartY());
                }
                if (mGraffiti.getShape() == GraffitiShape.HAND_WRITE) { // 手写
                    mCurrPath.quadTo(
                            mGraffiti.toX(mLastTouchX),
                            mGraffiti.toY(mLastTouchY),
                            mGraffiti.toX((mTouchX + mLastTouchX) / 2),
                            mGraffiti.toY((mTouchY + mLastTouchY) / 2));
                    mCurrGraffitiPath.updatePath(mCurrPath);
                } else {  // 画图形
                    mCurrGraffitiPath.updateXY(mGraffiti.toX(mTouchDownX), mGraffiti.toY(mTouchDownY), mGraffiti.toX(mTouchX), mGraffiti.toY(mTouchY));
                }
            }
        }
        mGraffiti.invalidate();
        return true;
    }


    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        mLastTouchX = mTouchX;
        mLastTouchY = mTouchY;
        mTouchX = e.getX();
        mTouchY = e.getY();

        if (mGraffiti.getPen().isSelectable()) {
            boolean found = false;
            IGraffitiSelectableItem item;
            List<IGraffitiItem> items = mGraffiti.getAllItem();
            for (int i = items.size() - 1; i >= 0; i--) {
                IGraffitiItem elem = items.get(i);
                if (!(elem instanceof IGraffitiSelectableItem)) {
                    continue;
                }
                item = (IGraffitiSelectableItem) elem;

                if (item.isInIt(mGraffiti.toX(mTouchX), mGraffiti.toY(mTouchY))) {
                    found = true;
                    setSelectedItem(item);
                    PointF xy = item.getLocation();
                    mSelectedItemX = xy.x;
                    mSelectedItemY = xy.y;
                    mListener.onSelectedItem(mSelectedItem, true);
                    break;
                }
            }
            if (!found) {
                if (mSelectedItem != null) { // 取消选定
                    IGraffitiSelectableItem old = mSelectedItem;
                    setSelectedItem(null);
                    mListener.onSelectedItem(old, false);
                } else {
                    mListener.onCreateSelectableItem(mGraffiti.toX(mTouchX), mGraffiti.toY(mTouchY));
                }
            }
        } else {
            // 模拟一次滑动
            onScrollBegin(e);
            onScroll(e, e, 0, 0);
            onScrollEnd(e);
        }
        mGraffiti.invalidate();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mLastFocusX = null;
        mLastFocusY = null;
        mGraffiti.enableAmplifier(false);
        return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
        // 屏幕上的焦点
        mTouchCentreX = detector.getFocusX();
        mTouchCentreY = detector.getFocusY();

        if (mLastFocusX != null && mLastFocusY != null) { // 焦点改变
            final float dx = mTouchCentreX - mLastFocusX;
            final float dy = mTouchCentreY - mLastFocusY;
            // 移动图片
            if (Math.abs(dx) > 1 || Math.abs(dy) > 1) {
                mGraffiti.setTransX(mGraffiti.getTransX() + dx);
                mGraffiti.setTransY(mGraffiti.getTransY() + dy);
            }
        }

        if (detector.getScaleFactor() > 0.1f) {
            // 缩放图片
            float scale = mGraffiti.getScale() * detector.getScaleFactor();
            mGraffiti.setScale(scale, mGraffiti.toX(mTouchCentreX), mGraffiti.toY(mTouchCentreY));
        }

        mLastFocusX = mTouchCentreX;
        mLastFocusY = mTouchCentreY;

        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        if (mGraffiti.getScale() < 1) {
            if (mScaleAnimator == null) {
                mScaleAnimator = new ValueAnimator();
                mScaleAnimator.setDuration(100);
                mScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float value = (float) animation.getAnimatedValue();
                        float fraction = animation.getAnimatedFraction();
                        mGraffiti.setScale(value, mGraffiti.toX(mTouchCentreX), mGraffiti.toY(mTouchCentreY));
                        mGraffiti.setTrans(mAnimTransX * (1 - fraction), mAnimTranY * (1 - fraction));
                    }
                });
            }
            mScaleAnimator.cancel();
            mAnimTransX = mGraffiti.getTransX();
            mAnimTranY = mGraffiti.getTransY();
            mScaleAnimator.setFloatValues(mGraffiti.getScale(), 1);
            mScaleAnimator.start();
        }
    }

    public void setGraffitiListener(ISelectionListener graffitiListener) {
        mListener = graffitiListener;
    }

    public ISelectionListener getGraffitiListener() {
        return mListener;
    }

    public interface ISelectionListener {
        /**
         * 选中
         *
         * @param selected 是否选中，false表示从选中变成不选中
         */
        void onSelectedItem(IGraffitiSelectableItem selectableItem, boolean selected);

        /**
         * 新建一个可选的item
         *
         * @param x
         * @param y
         */
        void onCreateSelectableItem(float x, float y);
    }

}