package cn.hzw.doodle;

import android.animation.ValueAnimator;
import android.graphics.Path;
import android.graphics.PointF;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import java.util.List;

import cn.forward.androids.TouchGestureDetector;
import cn.hzw.doodle.core.IDoodle;
import cn.hzw.doodle.core.IDoodleItem;
import cn.hzw.doodle.core.IDoodleSelectableItem;

import static cn.hzw.doodle.util.DrawUtil.computeAngle;

/**
 * DoodleView的涂鸦手势监听
 * Created on 30/06/2018.
 */

public class DoodleOnTouchGestureListener extends TouchGestureDetector.OnTouchGestureListener {
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
    private DoodlePath mCurrDoodlePath;
    private CopyLocation mCopyLocation;

    private DoodleView mDoodle;

    // 动画相关
    private ValueAnimator mScaleAnimator;
    private float mScaleAnimTransX, mScaleAnimTranY;
    private ValueAnimator mTranslateAnimator;
    private float mTransAnimOldY, mTransAnimY;

    private IDoodleSelectableItem mSelectedItem; // 当前选中的item
    private ISelectionListener mSelectionListener;

    public DoodleOnTouchGestureListener(DoodleView doodle, ISelectionListener listener) {
        mDoodle = doodle;
        mCopyLocation = DoodlePen.COPY.getCopyLocation();
        mCopyLocation.reset();
        mCopyLocation.updateLocation(doodle.getBitmap().getWidth() / 2, doodle.getBitmap().getHeight() / 2);
        mSelectionListener = listener;
    }

    public void setSelectedItem(IDoodleSelectableItem selectedItem) {
        IDoodleSelectableItem old = mSelectedItem;
        mSelectedItem = selectedItem;

        if (old != null) { // 取消选定
            old.setSelected(false);
            if (mSelectionListener != null) {
                mSelectionListener.onSelectedItem(mDoodle, old, false);
            }
        }
        if (mSelectedItem != null) {
            mSelectedItem.setSelected(true);
            if (mSelectionListener != null) {
                mSelectionListener.onSelectedItem(mDoodle, mSelectedItem, true);
            }
        }

    }

    public IDoodleSelectableItem getSelectedItem() {
        return mSelectedItem;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        mTouchX = mTouchDownX = e.getX();
        mTouchY = mTouchDownY = e.getY();
        mDoodle.enableZoomer(false); // 关闭放大镜
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

        if (mDoodle.getPen().isSelectable()) {
            // 判断是否点中选择区域
            if (mSelectedItem != null) {
                PointF xy = mSelectedItem.getLocation();
                mSelectedItemX = xy.x;
                mSelectedItemY = xy.y;
                if (mSelectedItem instanceof DoodleRotatableItemBase
                        && (((DoodleRotatableItemBase) mSelectedItem).canRotate(mDoodle.toX(mTouchX), mDoodle.toY(mTouchY)))) {
                    ((DoodleRotatableItemBase) mSelectedItem).setIsRotating(true);
                    mRotateTextDiff = mSelectedItem.getItemRotate() -
                            computeAngle(xy.x, xy.y, mDoodle.toX(mTouchX), mDoodle.toY(mTouchY));
                }
            }
        } else {
            mDoodle.enableZoomer(true); // 涂鸦时开启放大镜
            // 点击copy
            if (mDoodle.getPen() == DoodlePen.COPY && mCopyLocation.contains(mDoodle.toX(mTouchX), mDoodle.toY(mTouchY), mDoodle.getSize())) {
                mCopyLocation.setRelocating(true);
                mCopyLocation.setCopying(false);
            } else {
                if (mDoodle.getPen() == DoodlePen.COPY) {
                    mCopyLocation.setRelocating(false);
                    if (!mCopyLocation.isCopying()) {
                        mCopyLocation.setCopying(true);
                        mCopyLocation.setStartPosition(mDoodle.toX(mTouchX), mDoodle.toY(mTouchY));
                    }
                }

                // 初始化绘制
                mCurrPath = new Path();
                mCurrPath.moveTo(mDoodle.toX(mTouchX), mDoodle.toY(mTouchY));
                if (mDoodle.getShape() == DoodleShape.HAND_WRITE) { // 手写
                    mCurrDoodlePath = DoodlePath.toPath(mDoodle, mCurrPath);
                } else {  // 画图形
                    mCurrDoodlePath = DoodlePath.toShape(mDoodle,
                            mDoodle.toX(mTouchDownX), mDoodle.toY(mTouchDownY), mDoodle.toX(mTouchX), mDoodle.toY(mTouchY));
                }
//                mCurrDoodlePath.setDrawOptimize(false);
                mDoodle.addItem(mCurrDoodlePath);
            }
        }
        mDoodle.invalidate();
    }

    @Override
    public void onScrollEnd(MotionEvent e) {
        mLastTouchX = mTouchX;
        mLastTouchY = mTouchY;
        mTouchX = e.getX();
        mTouchY = e.getY();

        if (mDoodle.getPen().isSelectable()) {
            if (mSelectedItem instanceof DoodleRotatableItemBase) {
                ((DoodleRotatableItemBase) mSelectedItem).setIsRotating(false);
            }
        } else {
            if (mCurrDoodlePath != null) {
                /*mCurrDoodlePath.setDrawOptimize(true);
                mDoodle.invalidate(mCurrDoodlePath);*/
                mCurrDoodlePath = null;
            }
        }
        mDoodle.enableZoomer(false); // 关闭放大镜
        mDoodle.invalidate();
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        mLastTouchX = mTouchX;
        mLastTouchY = mTouchY;
        mTouchX = e2.getX();
        mTouchY = e2.getY();

        if (mDoodle.getPen().isSelectable()) { //画笔是否是可选择的
            if (mSelectedItem != null) {
                if ((mSelectedItem instanceof DoodleRotatableItemBase) && (((DoodleRotatableItemBase) mSelectedItem).isRotating())) { // 旋转item
                    PointF xy = mSelectedItem.getLocation();
                    mSelectedItem.setItemRotate(mRotateTextDiff + computeAngle(
                            xy.x, xy.y, mDoodle.toX(mTouchX), mDoodle.toY(mTouchY)
                    ));
                } else { // 移动item
                    mSelectedItem.setLocation(
                            mSelectedItemX + mDoodle.toX(mTouchX) - mDoodle.toX(mTouchDownX),
                            mSelectedItemY + mDoodle.toY(mTouchY) - mDoodle.toY(mTouchDownY));
                }
            }
        } else {
            if (mDoodle.getPen() == DoodlePen.COPY && mCopyLocation.isRelocating()) {
                // 正在定位location
                mCopyLocation.updateLocation(mDoodle.toX(mTouchX), mDoodle.toY(mTouchY));
            } else {
                if (mDoodle.getPen() == DoodlePen.COPY) {
                    mCopyLocation.updateLocation(mCopyLocation.getCopyStartX() + mDoodle.toX(mTouchX) - mCopyLocation.getTouchStartX(),
                            mCopyLocation.getCopyStartY() + mDoodle.toY(mTouchY) - mCopyLocation.getTouchStartY());
                }
                if (mDoodle.getShape() == DoodleShape.HAND_WRITE) { // 手写
                    mCurrPath.quadTo(
                            mDoodle.toX(mLastTouchX),
                            mDoodle.toY(mLastTouchY),
                            mDoodle.toX((mTouchX + mLastTouchX) / 2),
                            mDoodle.toY((mTouchY + mLastTouchY) / 2));
                    mCurrDoodlePath.updatePath(mCurrPath);
                } else {  // 画图形
                    mCurrDoodlePath.updateXY(mDoodle.toX(mTouchDownX), mDoodle.toY(mTouchDownY), mDoodle.toX(mTouchX), mDoodle.toY(mTouchY));
                }
            }
        }
        mDoodle.invalidate();
        return true;
    }


    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        mLastTouchX = mTouchX;
        mLastTouchY = mTouchY;
        mTouchX = e.getX();
        mTouchY = e.getY();

        if (mDoodle.getPen().isSelectable()) {
            boolean found = false;
            IDoodleSelectableItem item;
            List<IDoodleItem> items = mDoodle.getAllItem();
            for (int i = items.size() - 1; i >= 0; i--) {
                IDoodleItem elem = items.get(i);
                if (!(elem instanceof IDoodleSelectableItem)) {
                    continue;
                }
                item = (IDoodleSelectableItem) elem;

                if (item.contains(mDoodle.toX(mTouchX), mDoodle.toY(mTouchY))) {
                    found = true;
                    setSelectedItem(item);
                    PointF xy = item.getLocation();
                    mSelectedItemX = xy.x;
                    mSelectedItemY = xy.y;
                    break;
                }
            }
            if (!found) {
                if (mSelectedItem != null) { // 取消选定
                    IDoodleSelectableItem old = mSelectedItem;
                    setSelectedItem(null);
                    if (mSelectionListener != null) {
                        mSelectionListener.onSelectedItem(mDoodle, old, false);
                    }
                } else {
                    if (mSelectionListener != null) {
                        mSelectionListener.onCreateSelectableItem(mDoodle, mDoodle.toX(mTouchX), mDoodle.toY(mTouchY));
                    }
                }
            }
        } else {
            // 模拟一次滑动
            onScrollBegin(e);
            onScroll(e, e, 0, 0);
            onScrollEnd(e);
        }
        mDoodle.invalidate();
        return true;
    }

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
        mLastFocusX = null;
        mLastFocusY = null;
        mDoodle.enableZoomer(false);
        return true;
    }

    private float pendingX, pendingY, pendingScale = 1;

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
                mDoodle.setDoodleTranslationX(mDoodle.getDoodleTranslationX() + dx + pendingX);
                mDoodle.setDoodleTranslationY(mDoodle.getDoodleTranslationY() + dy + pendingY);
                pendingX = pendingY = 0;
            } else {
                pendingX += dx;
                pendingY += dy;
            }
        }

        if (Math.abs(1 - detector.getScaleFactor()) > 0.005f) {
            // 缩放图片
            float scale = mDoodle.getDoodleScale() * detector.getScaleFactor() * pendingScale;
            mDoodle.setDoodleScale(scale, mDoodle.toX(mTouchCentreX), mDoodle.toY(mTouchCentreY));
            pendingScale = 1;
        } else {
            pendingScale *= detector.getScaleFactor();
        }

        mLastFocusX = mTouchCentreX;
        mLastFocusY = mTouchCentreY;

        return true;
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
        if (mDoodle.getDoodleScale() < 1) { //
            if (mScaleAnimator == null) {
                mScaleAnimator = new ValueAnimator();
                mScaleAnimator.setDuration(100);
                mScaleAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float value = (float) animation.getAnimatedValue();
                        float fraction = animation.getAnimatedFraction();
                        mDoodle.setDoodleScale(value, mDoodle.toX(mTouchCentreX), mDoodle.toY(mTouchCentreY));
                        mDoodle.setDoodleTranslation(mScaleAnimTransX * (1 - fraction), mScaleAnimTranY * (1 - fraction));
                    }
                });
            }
            mScaleAnimator.cancel();
            mScaleAnimTransX = mDoodle.getDoodleTranslationX();
            mScaleAnimTranY = mDoodle.getDoodleTranslationY();
            mScaleAnimator.setFloatValues(mDoodle.getDoodleScale(), 1);
            mScaleAnimator.start();
        } else { //
            limitBound(true);
        }
    }

    /**
     * 限定边界
     *
     * @param anim 动画效果
     */
    public void limitBound(boolean anim) {
        if (mDoodle.getDoodleRotation() % 90 != 0) { // 只处理0,90,180,270
            return;
        }

        final float oldX = mDoodle.getDoodleTranslationX(), oldY = mDoodle.getDoodleTranslationY();
        RectF bound = mDoodle.getDoodleBound();
        float x = mDoodle.getDoodleTranslationX(), y = mDoodle.getDoodleTranslationY();
        float width = mDoodle.getCenterWidth() * mDoodle.getRotateScale(), height = mDoodle.getCenterHeight() * mDoodle.getRotateScale();

        // 上下都在屏幕内
        if (bound.height() <= mDoodle.getHeight()) {
            if (mDoodle.getDoodleRotation() == 0 || mDoodle.getDoodleRotation() == 180) {
                y = (height - height * mDoodle.getDoodleScale()) / 2;
            } else {
                x = (width - width * mDoodle.getDoodleScale()) / 2;
            }
        } else {
            float heightDiffTop = bound.top;
            // 只有上在屏幕内
            if (bound.top > 0 && bound.bottom >= mDoodle.getHeight()) {
                if (mDoodle.getDoodleRotation() == 0 || mDoodle.getDoodleRotation() == 180) {
                    if (mDoodle.getDoodleRotation() == 0) {
                        y = y - heightDiffTop;
                    } else {
                        y = y + heightDiffTop;
                    }
                } else {
                    if (mDoodle.getDoodleRotation() == 90) {
                        x = x - heightDiffTop;
                    } else {
                        x = x + heightDiffTop;
                    }
                }
            } else if (bound.bottom < mDoodle.getHeight() && bound.top <= 0) { // 只有下在屏幕内
                float heightDiffBottom = mDoodle.getHeight() - bound.bottom;
                if (mDoodle.getDoodleRotation() == 0 || mDoodle.getDoodleRotation() == 180) {
                    if (mDoodle.getDoodleRotation() == 0) {
                        y = y + heightDiffBottom;
                    } else {
                        y = y - heightDiffBottom;
                    }
                } else {
                    if (mDoodle.getDoodleRotation() == 90) {
                        x = x + heightDiffBottom;
                    } else {
                        x = x - heightDiffBottom;
                    }
                }
            }
        }

        // 左右都在屏幕内
        if (bound.width() <= mDoodle.getWidth()) {
            if (mDoodle.getDoodleRotation() == 0 || mDoodle.getDoodleRotation() == 180) {
                x = (width - width * mDoodle.getDoodleScale()) / 2;
            } else {
                y = (height - height * mDoodle.getDoodleScale()) / 2;
            }
        } else {
            float widthDiffLeft = bound.left;
            // 只有左在屏幕内
            if (bound.left > 0 && bound.right >= mDoodle.getWidth()) {
                if (mDoodle.getDoodleRotation() == 0 || mDoodle.getDoodleRotation() == 180) {
                    if (mDoodle.getDoodleRotation() == 0) {
                        x = x - widthDiffLeft;
                    } else {
                        x = x + widthDiffLeft;
                    }
                } else {
                    if (mDoodle.getDoodleRotation() == 90) {
                        y = y + widthDiffLeft;
                    } else {
                        y = y - widthDiffLeft;
                    }
                }
            } else if (bound.right < mDoodle.getWidth() && bound.left <= 0) { // 只有右在屏幕内
                float widthDiffRight = mDoodle.getWidth() - bound.right;
                if (mDoodle.getDoodleRotation() == 0 || mDoodle.getDoodleRotation() == 180) {
                    if (mDoodle.getDoodleRotation() == 0) {
                        x = x + widthDiffRight;
                    } else {
                        x = x - widthDiffRight;
                    }
                } else {
                    if (mDoodle.getDoodleRotation() == 90) {
                        y = y - widthDiffRight;
                    } else {
                        y = y + widthDiffRight;
                    }
                }
            }
        }
        if (anim) {
            if (mTranslateAnimator == null) {
                mTranslateAnimator = new ValueAnimator();
                mTranslateAnimator.setDuration(100);
                mTranslateAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        float value = (float) animation.getAnimatedValue();
                        float fraction = animation.getAnimatedFraction();
                        mDoodle.setDoodleTranslation(value, mTransAnimOldY + (mTransAnimY - mTransAnimOldY) * fraction);
                    }
                });
            }
            mTranslateAnimator.setFloatValues(oldX, x);
            mTransAnimOldY = oldY;
            mTransAnimY = y;
            mTranslateAnimator.start();
        } else {
            mDoodle.setDoodleTranslation(x, y);
        }
    }

    public void setSelectionListener(ISelectionListener doodleListener) {
        mSelectionListener = doodleListener;
    }

    public ISelectionListener getSelectionListener() {
        return mSelectionListener;
    }

    public interface ISelectionListener {
        /**
         * called when the item(such as text, texture) is selected/unselected.
         * item（如文字，贴图）被选中或取消选中时回调
         *
         * @param selected 是否选中，false表示从选中变成不选中
         */
        void onSelectedItem(IDoodle doodle, IDoodleSelectableItem selectableItem, boolean selected);

        /**
         * called when you click the view to create a item(such as text, texture).
         * 点击View中的某个点创建可选择的item（如文字，贴图）时回调
         *
         * @param x
         * @param y
         */
        void onCreateSelectableItem(IDoodle doodle, float x, float y);
    }

}