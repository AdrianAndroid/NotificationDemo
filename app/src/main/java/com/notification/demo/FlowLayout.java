package com.notification.demo;


import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import java.util.ArrayList;
import java.util.List;

/**
 * 流式布局标签控件
 *
 * 本文作者：谷哥的小弟
 * 博客地址：http://blog.csdn.net/lfdfhl
 */
public class FlowLayout extends ViewGroup {

    /**
     * Special value for the child view spacing.
     * SPACING_AUTO means that the actual spacing is calculated according to the size of the
     * container and the number of the child views, so that the child views are placed evenly in
     * the container.
     */
    public static final int SPACING_AUTO = -65536;

    /**
     * Special value for the horizontal spacing of the child views in the last row
     * SPACING_ALIGN means that the horizontal spacing of the child views in the last row keeps
     * the same with the spacing used in the row above. If there is only one row, this value is
     * ignored and the spacing will be calculated according to childSpacing.
     */
    public static final int SPACING_ALIGN = -65537;

    private static final int SPACING_UNDEFINED = -65538;

    private static final boolean DEFAULT_FLOW = true;
    private static final int DEFAULT_CHILD_SPACING = 0;
    private static final int DEFAULT_CHILD_SPACING_FOR_LAST_ROW = SPACING_UNDEFINED;
    private static final float DEFAULT_ROW_SPACING = 0;
    private static final boolean DEFAULT_RTL = false;

    private boolean mFlow = DEFAULT_FLOW;
    private int mChildSpacing = DEFAULT_CHILD_SPACING;
    private int mChildSpacingForLastRow = DEFAULT_CHILD_SPACING_FOR_LAST_ROW;
    private float mRowSpacing = DEFAULT_ROW_SPACING;
    private float mAdjustedRowSpacing = DEFAULT_ROW_SPACING;
    private boolean mRtl = DEFAULT_RTL;

    private final List<Float> mHorizontalSpacingForRow = new ArrayList<>();
    private final List<Integer> mHeightForRow = new ArrayList<>();
    private final List<Integer> mChildNumForRow = new ArrayList<>();
    private LabelClicked mListenter;

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        super(context, attrs);

        TypedArray a = context.getTheme().obtainStyledAttributes(
                attrs, R.styleable.FlowLayout, 0, 0);
        try {
            mFlow = a.getBoolean(R.styleable.FlowLayout_flow, DEFAULT_FLOW);
            try {
                mChildSpacing = a.getInt(R.styleable.FlowLayout_childSpacing, DEFAULT_CHILD_SPACING);
            } catch (NumberFormatException e) {
                mChildSpacing = a.getDimensionPixelSize(R.styleable.FlowLayout_childSpacing, (int) dpToPx(DEFAULT_CHILD_SPACING));
            }
            try {
                mChildSpacingForLastRow = a.getInt(R.styleable.FlowLayout_childSpacingForLastRow, SPACING_UNDEFINED);
            } catch (NumberFormatException e) {
                mChildSpacingForLastRow = a.getDimensionPixelSize(R.styleable.FlowLayout_childSpacingForLastRow, (int) dpToPx(DEFAULT_CHILD_SPACING));
            }
            try {
                mRowSpacing = a.getInt(R.styleable.FlowLayout_rowSpacing, 0);
            } catch (NumberFormatException e) {
                mRowSpacing = a.getDimension(R.styleable.FlowLayout_rowSpacing, dpToPx(DEFAULT_ROW_SPACING));
            }
            mRtl = a.getBoolean(R.styleable.FlowLayout_rtl, DEFAULT_RTL);
        } finally {
            a.recycle();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        mHorizontalSpacingForRow.clear();
        mChildNumForRow.clear();
        mHeightForRow.clear();

        int measuredHeight = 0, measuredWidth = 0, childCount = getChildCount();
        int rowWidth = 0, maxChildHeightInRow = 0, childNumInRow = 0;
        int rowSize = widthSize - getPaddingLeft() - getPaddingRight();
        boolean allowFlow = widthMode != MeasureSpec.UNSPECIFIED && mFlow;
        int childSpacing = mChildSpacing == SPACING_AUTO && widthMode == MeasureSpec.UNSPECIFIED
                ? 0 : mChildSpacing;
        float tmpSpacing = childSpacing == SPACING_AUTO ? 0 : childSpacing;

        for (int i = 0; i < childCount; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) {
                continue;
            }

            LayoutParams childParams = child.getLayoutParams();
            int horizontalMargin = 0, verticalMargin = 0;
            if (childParams instanceof MarginLayoutParams) {
                measureChildWithMargins(child, widthMeasureSpec, 0, heightMeasureSpec, measuredHeight);
                MarginLayoutParams marginParams = (MarginLayoutParams) childParams;
                horizontalMargin = marginParams.leftMargin + marginParams.rightMargin;
                verticalMargin = marginParams.topMargin + marginParams.bottomMargin;
            } else {
                measureChild(child, widthMeasureSpec, heightMeasureSpec);
            }

            int childWidth = child.getMeasuredWidth() + horizontalMargin;
            int childHeight = child.getMeasuredHeight() + verticalMargin;
            // Need flow to next row
            if (allowFlow && rowWidth + childWidth > rowSize) {
                // Save parameters for current row
                mHorizontalSpacingForRow.add(getSpacingForRow(childSpacing, rowSize, rowWidth, childNumInRow));
                mChildNumForRow.add(childNumInRow);
                mHeightForRow.add(maxChildHeightInRow);
                measuredHeight += maxChildHeightInRow;
                measuredWidth = max(measuredWidth, rowWidth);

                // Place the child view to next row
                childNumInRow = 1;
                rowWidth = childWidth + (int) tmpSpacing;
                maxChildHeightInRow = childHeight;
            } else {
                childNumInRow++;
                rowWidth += childWidth + tmpSpacing;
                maxChildHeightInRow = max(maxChildHeightInRow, childHeight);
            }
        }

        // Measure remaining child views in the last row
        if (mChildSpacingForLastRow == SPACING_ALIGN) {
            // For SPACING_ALIGN, use the same spacing from the row above if there is more than one
            // row.
            if (mHorizontalSpacingForRow.size() >= 1) {
                mHorizontalSpacingForRow.add(
                        mHorizontalSpacingForRow.get(mHorizontalSpacingForRow.size() - 1));
            } else {
                mHorizontalSpacingForRow.add(getSpacingForRow(childSpacing, rowSize, rowWidth, childNumInRow));
            }
        } else if (mChildSpacingForLastRow != SPACING_UNDEFINED) {
            // For SPACING_AUTO and specific DP values, apply them to the spacing strategy.
            mHorizontalSpacingForRow.add(getSpacingForRow(mChildSpacingForLastRow, rowSize, rowWidth, childNumInRow));
        } else {
            // For SPACING_UNDEFINED, apply childSpacing to the spacing strategy for the last row.
            mHorizontalSpacingForRow.add(getSpacingForRow(childSpacing, rowSize, rowWidth, childNumInRow));
        }

        mChildNumForRow.add(childNumInRow);
        mHeightForRow.add(maxChildHeightInRow);
        measuredHeight += maxChildHeightInRow;
        measuredWidth = max(measuredWidth, rowWidth);

        if (childSpacing == SPACING_AUTO) {
            measuredWidth = widthSize;
        } else if (widthMode == MeasureSpec.UNSPECIFIED) {
            measuredWidth = measuredWidth + getPaddingLeft() + getPaddingRight();
        } else {
            measuredWidth = min(measuredWidth + getPaddingLeft() + getPaddingRight(), widthSize);
        }

        measuredHeight += getPaddingTop() + getPaddingBottom();
        int rowNum = mHorizontalSpacingForRow.size();
        float rowSpacing = mRowSpacing == SPACING_AUTO && heightMode == MeasureSpec.UNSPECIFIED
                ? 0 : mRowSpacing;
        if (rowSpacing == SPACING_AUTO) {
            if (rowNum > 1) {
                mAdjustedRowSpacing = (heightSize - measuredHeight) / (rowNum - 1);
            } else {
                mAdjustedRowSpacing = 0;
            }
            measuredHeight = heightSize;
        } else {
            mAdjustedRowSpacing = rowSpacing;
            if (heightMode == MeasureSpec.UNSPECIFIED) {
                measuredHeight = (int) (measuredHeight + mAdjustedRowSpacing * (rowNum - 1));
            } else {
                measuredHeight = min(
                        (int) (measuredHeight + mAdjustedRowSpacing * (rowNum - 1)), heightSize);
            }
        }

        measuredWidth = widthMode == MeasureSpec.EXACTLY ? widthSize : measuredWidth;
        measuredHeight = heightMode == MeasureSpec.EXACTLY ? heightSize : measuredHeight;
        setMeasuredDimension(measuredWidth, measuredHeight);
    }

    private void test() {
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int paddingLeft = getPaddingLeft();
        int paddingRight = getPaddingRight();
        int paddingTop = getPaddingTop();
        int x = mRtl ? (getWidth() - paddingRight) : paddingLeft;
        int y = paddingTop;

        int rowCount = mChildNumForRow.size(), childIdx = 0;
        for (int row = 0; row < rowCount; row++) {
            int childNum = mChildNumForRow.get(row);
            int rowHeight = mHeightForRow.get(row);
            float spacing = mHorizontalSpacingForRow.get(row);
            for (int i = 0; i < childNum; i++) {
                View child = getChildAt(childIdx++);
                if (child.getVisibility() == GONE) {
                    continue;
                }

                LayoutParams childParams = child.getLayoutParams();
                int marginLeft = 0, marginTop = 0, marginRight = 0;
                if (childParams instanceof MarginLayoutParams) {
                    MarginLayoutParams marginParams = (MarginLayoutParams) childParams;
                    marginLeft = marginParams.leftMargin;
                    marginRight = marginParams.rightMargin;
                    marginTop = marginParams.topMargin;
                }

                int childWidth = child.getMeasuredWidth();
                int childHeight = child.getMeasuredHeight();
                if (mRtl) {
                    child.layout(x - marginRight - childWidth, y + marginTop,
                            x - marginRight, y + marginTop + childHeight);
                    x -= childWidth + spacing + marginLeft + marginRight;
                } else {
                    child.layout(x + marginLeft, y + marginTop,
                            x + marginLeft + childWidth, y + marginTop + childHeight);
                    x += childWidth + spacing + marginLeft + marginRight;
                }
            }
            x = mRtl ? (getWidth() - paddingRight) : paddingLeft;
            y += rowHeight + mAdjustedRowSpacing;
        }
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    /**
     * Returns whether to allow child views flow to next row when there is no enough space.
     *
     * @return Whether to flow child views to next row when there is no enough space.
     */
    public boolean isFlow() {
        return mFlow;
    }

    /**
     * Sets whether to allow child views flow to next row when there is no enough space.
     *
     * @param flow true to allow flow. false to restrict all child views in one row.
     */
    public void setFlow(boolean flow) {
        mFlow = flow;
        requestLayout();
    }

    /**
     * Returns the horizontal spacing between child views.
     *
     * @return The spacing, either {@link FlowLayout#SPACING_AUTO}, or a fixed size in pixels.
     */
    public int getChildSpacing() {
        return mChildSpacing;
    }

    /**
     * Sets the horizontal spacing between child views.
     *
     * @param childSpacing The spacing, either {@link FlowLayout#SPACING_AUTO}, or a fixed size in
     *                     pixels.
     */
    public void setChildSpacing(int childSpacing) {
        mChildSpacing = childSpacing;
        requestLayout();
    }

    /**
     * Returns the horizontal spacing between child views of the last row.
     *
     * @return The spacing, either {@link FlowLayout#SPACING_AUTO},
     * {@link FlowLayout#SPACING_ALIGN}, or a fixed size in pixels
     */
    public int getChildSpacingForLastRow() {
        return mChildSpacingForLastRow;
    }

    /**
     * Sets the horizontal spacing between child views of the last row.
     *
     * @param childSpacingForLastRow The spacing, either {@link FlowLayout#SPACING_AUTO},
     *                               {@link FlowLayout#SPACING_ALIGN}, or a fixed size in pixels
     */
    public void setChildSpacingForLastRow(int childSpacingForLastRow) {
        mChildSpacingForLastRow = childSpacingForLastRow;
        requestLayout();
    }

    /**
     * Returns the vertical spacing between rows.
     *
     * @return The spacing, either {@link FlowLayout#SPACING_AUTO}, or a fixed size in pixels.
     */
    public float getRowSpacing() {
        return mRowSpacing;
    }

    /**
     * Sets the vertical spacing between rows in pixels. Use SPACING_AUTO to evenly place all rows
     * in vertical.
     *
     * @param rowSpacing The spacing, either {@link FlowLayout#SPACING_AUTO}, or a fixed size in
     *                   pixels.
     */
    public void setRowSpacing(float rowSpacing) {
        mRowSpacing = rowSpacing;
        requestLayout();
    }

    private int max(int a, int b) {
        return Math.max(a, b);
    }

    private int min(int a, int b) {
        return Math.min(a, b);
    }

    private float getSpacingForRow(int spacingAttribute, int rowSize, int usedSize, int childNum) {
        float spacing;
        if (spacingAttribute == SPACING_AUTO) {
            if (childNum > 1) {
                spacing = (rowSize - usedSize) / (childNum - 1);
            } else {
                spacing = 0;
            }
        } else {
            spacing = spacingAttribute;
        }
        return spacing;
    }

    private float dpToPx(float dp) {
        return TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, dp, getResources().getDisplayMetrics());
    }

    public void setLabelClickedListener(LabelClicked listener) {
        this.mListenter = listener;
    }

    public interface LabelClicked {

        void onLabelClick(String label);
    }
}

