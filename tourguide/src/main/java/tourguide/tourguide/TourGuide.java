package tourguide.tourguide;

import android.app.Activity;
import android.graphics.Point;
import android.os.Build;
import androidx.core.view.ViewCompat;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;
import android.widget.TextView;

/**
 * Created by tanjunrong on 2/10/15.
 */
public class TourGuide {
    /**
     * This describes the animation techniques
     * */
    public enum Technique {
        Click, HorizontalLeft, HorizontalRight, VerticalUpward, VerticalDownward
    }

    /**
     * This describes the allowable motion, for example if you want the users to learn about clicking, but want to stop them from swiping, then use ClickOnly
     */
    public enum MotionType {
        AllowAll, ClickOnly, SwipeOnly
    }
    protected Technique mTechnique;
    protected View mHighlightedView;
    private Activity mActivity;
    protected MotionType mMotionType;
    protected FrameLayoutWithHole mFrameLayout;
    private View mToolTipViewGroup;
    public ToolTip mToolTip;
    public Pointer mPointer;
    public Overlay mOverlay;

    /*************
     *
     * Public API
     *
     *************/

    /* Static builder */
    public static TourGuide init(Activity activity){
        return new TourGuide(activity);
    }

    /* Constructor */
    public TourGuide(Activity activity){
        mActivity = activity;
    }

    /**
     * Setter for the animation to be used
     * @param technique Animation to be used
     * @return return TourGuide instance for chaining purpose
     */
    public TourGuide with(Technique technique) {
        mTechnique = technique;
        return this;
    }

    /**
     * Sets which motion type is motionType
     * @param motionType
     * @return return TourGuide instance for chaining purpose
     */
    public TourGuide motionType(MotionType motionType){
        mMotionType = motionType;
        return this;
    }

    /**
     * Sets the targeted view for TourGuide to play on
     * @param targetView the view in which the tutorial button will be placed on top of
     * @return return TourGuide instance for chaining purpose
     */
    public TourGuide playOn(View targetView){
        mHighlightedView = targetView;
        setupView();
        return this;
    }

    /**
     * Sets the overlay
     * @param overlay this overlay object should contain the attributes of the overlay, such as background color, animation, Style, etc
     * @return return TourGuide instance for chaining purpose
     */
    public TourGuide setOverlay(Overlay overlay){
        mOverlay = overlay;
        return this;
    }
    /**
     * Set the toolTip
     * @param toolTip this toolTip object should contain the attributes of the ToolTip, such as, the title text, and the description text, background color, etc
     * @return return TourGuide instance for chaining purpose
     */
    public TourGuide setToolTip(ToolTip toolTip){
        mToolTip = toolTip;
        return this;
    }
    /**
     * Set the Pointer
     * @param pointer this pointer object should contain the attributes of the Pointer, such as the pointer color, pointer gravity, etc, refer to @Link{pointer}
     * @return return TourGuide instance for chaining purpose
     */
    public TourGuide setPointer(Pointer pointer){
        mPointer = pointer;
        return this;
    }
    /**
     * Clean up the tutorial that is added to the activity
     */
     public void cleanUp(){
         mFrameLayout.cleanUp();
         if (mToolTipViewGroup!=null) {
             ((ViewGroup) mActivity.getWindow().getDecorView()).removeView(mToolTipViewGroup);
         }
    }

    /**
     *
     * @return FrameLayoutWithHole that is used as overlay
     */
    public FrameLayoutWithHole getOverlay(){
        return mFrameLayout;
    }
    /**
     *
     * @return the ToolTip container View
     */
    public View getToolTip(){
        return mToolTipViewGroup;
    }
    /******
     *
     * Private methods
     *
     *******/
    //TODO: move into Pointer
    private int getXBasedOnGravity(int width){
        int [] pos = new int[2];
        mHighlightedView.getLocationOnScreen(pos);
        int x = pos[0];
        if((mPointer.mGravity & Gravity.RIGHT) == Gravity.RIGHT){
            return x+mHighlightedView.getWidth()-width;
        } else if ((mPointer.mGravity & Gravity.LEFT) == Gravity.LEFT) {
            return x;
        } else { // this is center
            return x+mHighlightedView.getWidth()/2-width/2;
        }
    }
    //TODO: move into Pointer
    private int getYBasedOnGravity(int height){
        int [] pos = new int[2];
        mHighlightedView.getLocationInWindow(pos);
        int y = pos[1];
        if((mPointer.mGravity & Gravity.BOTTOM) == Gravity.BOTTOM){
            return y+mHighlightedView.getHeight()-height;
        } else if ((mPointer.mGravity & Gravity.TOP) == Gravity.TOP) {
            return y;
        }else { // this is center
            return y+mHighlightedView.getHeight()/2-height/2;
        }
    }

    protected void setupView(){
        // TourGuide can only be setup after all the views is ready and obtain it's position/measurement
        // so when this is the 1st time TourGuide is being added,
        // else block will be executed, and ViewTreeObserver will make TourGuide setup process to be delayed until everything is ready
        // when this is run the 2nd or more times, if block will be executed
        if (ViewCompat.isAttachedToWindow(mHighlightedView)){
            startView();
        } else {
            final ViewTreeObserver viewTreeObserver = mHighlightedView.getViewTreeObserver();
            viewTreeObserver.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        //noinspection deprecation
                        mHighlightedView.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    } else {
                        mHighlightedView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }
                    startView();
                }
            });
        }
    }

    private void startView(){
        /* Initialize a frame layout with a hole */
        mFrameLayout = new FrameLayoutWithHole(mActivity, mHighlightedView, mMotionType, mOverlay);

        // We add a finish button if there exist one, can also be used to add other custom views
        if (mFrameLayout.getFinishButton() != null) {

            View finishView = mFrameLayout.getFinishButton();
            FrameLayout.LayoutParams layoutParams=new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            layoutParams.gravity=Gravity.CENTER_HORIZONTAL|Gravity.BOTTOM;
            mFrameLayout.addView(finishView);
        }

        /* handle click disable */
        handleDisableClicking(mFrameLayout);

        setupFrameLayout();
        /* setup tooltip view */
        setupToolTip();
    }

    private void handleDisableClicking(FrameLayoutWithHole frameLayoutWithHole){
        // 1. if user provides an overlay listener, use that as 1st priority
        if (mOverlay != null && mOverlay.mOnClickListener!=null) {
            frameLayoutWithHole.setClickable(true);
            frameLayoutWithHole.setOnClickListener(mOverlay.mOnClickListener);
        }
        // 2. if overlay listener is not provided, check if it's disabled
        else if (mOverlay != null && mOverlay.mDisableClick) {
            Log.w("tourguide", "Overlay's default OnClickListener is null, it will proceed to next tourguide when it is clicked");
            frameLayoutWithHole.setViewHole(mHighlightedView);
            frameLayoutWithHole.setSoundEffectsEnabled(false);
            frameLayoutWithHole.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) {} // do nothing, disabled.
            });
        }
    }

    private void setupToolTip(){
        final FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);

        if (mToolTip != null) {
            /* inflate and get views */
            ViewGroup parent = (ViewGroup) mActivity.getWindow().getDecorView();
            LayoutInflater layoutInflater = mActivity.getLayoutInflater();

            if (mToolTip.getCustomView() == null) {
                mToolTipViewGroup = layoutInflater.inflate(R.layout.tour_guide_tooltip, null);
                View toolTipContainer = mToolTipViewGroup.findViewById(R.id.tour_tooltip_container);
                TextView toolTipTitleTV = (TextView) mToolTipViewGroup.findViewById(R.id.title);
                TextView toolTipDescriptionTV = (TextView) mToolTipViewGroup.findViewById(R.id.description);

                /* set tooltip attributes */
                toolTipContainer.setBackgroundColor(mToolTip.mBackgroundColor);

                if (mToolTip.mTitle == null || mToolTip.mTitle.isEmpty()) {
                    toolTipTitleTV.setVisibility(View.GONE);
                } else {
                    toolTipTitleTV.setVisibility(View.VISIBLE);
                    toolTipTitleTV.setText(mToolTip.mTitle);
                }

                if (mToolTip.mDescription == null || mToolTip.mDescription.isEmpty()) {
                    toolTipDescriptionTV.setVisibility(View.GONE);
                } else {
                    toolTipDescriptionTV.setVisibility(View.VISIBLE);
                    toolTipDescriptionTV.setText(mToolTip.mDescription);
                }
            } else {
                mToolTipViewGroup = mToolTip.getCustomView();
            }

            mToolTipViewGroup.startAnimation(mToolTip.mEnterAnimation);

            /* add setShadow if it's turned on */
            if (mToolTip.mShadow) {
                mToolTipViewGroup.setBackgroundDrawable(mActivity.getResources().getDrawable(R.drawable.drop_shadow));
            }

            /* position and size calculation */
            int [] pos = new int[2];
            mHighlightedView.getLocationOnScreen(pos);
            int targetViewX = pos[0];
            targetViewX = targetViewX + mHighlightedView.getMeasuredWidth()/2;  // align tooltip with center of target view
            final int targetViewY = pos[1];

            // get measured size of tooltip
            mToolTipViewGroup.measure(FrameLayout.LayoutParams.WRAP_CONTENT, FrameLayout.LayoutParams.WRAP_CONTENT);
            int toolTipMeasuredWidth = mToolTipViewGroup.getMeasuredWidth();
            int toolTipMeasuredHeight = mToolTipViewGroup.getMeasuredHeight();

            Point resultPoint = new Point(); // this holds the final position of tooltip
            float density = mActivity.getResources().getDisplayMetrics().density;

            final float adjustment;
            if (mToolTip.needsAdjustment()) {
                adjustment = 10 * density; //adjustment is that little overlapping area of tooltip and targeted button
            } else {
                adjustment = 0;
            }

            // calculate x position, based on gravity, tooltipMeasuredWidth, parent max width, x position of target view, adjustment
            if (toolTipMeasuredWidth > parent.getWidth()){
                resultPoint.x = getXForTooTip(mToolTip.mGravity, parent.getWidth(), targetViewX, adjustment);
            } else {
                resultPoint.x = getXForTooTip(mToolTip.mGravity, toolTipMeasuredWidth, targetViewX, adjustment);
            }

            resultPoint.y = getYForTooTip(mToolTip.mGravity, toolTipMeasuredHeight, targetViewY, adjustment);

            // add view to parent
//            ((ViewGroup) mActivity.getWindow().getDecorView().findViewById(android.R.id.content)).addView(mToolTipViewGroup, layoutParams);
            parent.addView(mToolTipViewGroup, layoutParams);

            // 1. width < screen check
            if (toolTipMeasuredWidth > parent.getWidth()){
                mToolTipViewGroup.getLayoutParams().width = parent.getWidth();
                toolTipMeasuredWidth = parent.getWidth();
            }
            // 2. x left boundary check
            if (resultPoint.x < 0){
                mToolTipViewGroup.getLayoutParams().width = toolTipMeasuredWidth + resultPoint.x; //since point.x is negative, use plus
                resultPoint.x = 0;
            }
            // 3. x right boundary check
            int tempRightX = resultPoint.x + toolTipMeasuredWidth;
            if ( tempRightX > parent.getWidth()){
                mToolTipViewGroup.getLayoutParams().width = parent.getWidth() - resultPoint.x; //since point.x is negative, use plus
            }

            // pass toolTip onClickListener into toolTipViewGroup
            if (mToolTip.mOnClickListener!=null) {
                mToolTipViewGroup.setOnClickListener(mToolTip.mOnClickListener);
            }

            // TODO: no boundary check for height yet, this is a unlikely case though
            // height boundary can be fixed by user changing the gravity to the other size, since there are plenty of space vertically compared to horizontally

            // this needs an viewTreeObserver, that's because TextView measurement of it's vertical height is not accurate (didn't take into account of multiple lines yet) before it's rendered
            // re-calculate height again once it's rendered
            mToolTipViewGroup.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    // make sure this only run once
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                        //noinspection deprecation
                        mToolTipViewGroup.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                    } else {
                        mToolTipViewGroup.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    }

                    int fixedY;
                    int toolTipHeightAfterLayouted = mToolTipViewGroup.getHeight();
                    fixedY = getYForTooTip(mToolTip.mGravity, toolTipHeightAfterLayouted, targetViewY, adjustment);
                    layoutParams.setMargins((int) mToolTipViewGroup.getX(), fixedY, 0, 0);
                }
            });

            // set the position using setMargins on the left and top
            layoutParams.setMargins(resultPoint.x, resultPoint.y, 0, 0);
        }

    }

    private int getXForTooTip(int gravity, int toolTipMeasuredWidth, int targetViewX, float adjustment){
        int x;
        if ((gravity & Gravity.LEFT) == Gravity.LEFT){
            x = targetViewX - toolTipMeasuredWidth + (int)adjustment;
        } else if ((gravity & Gravity.RIGHT) == Gravity.RIGHT) {
            x = targetViewX + mHighlightedView.getWidth() - (int)adjustment;
        } else {
            x = targetViewX + mHighlightedView.getWidth() / 2 - toolTipMeasuredWidth / 2;
        }
        return x;
    }
    private int getYForTooTip(int gravity, int toolTipMeasuredHeight, int targetViewY, float adjustment){
        int y;
        if ((gravity & Gravity.TOP) == Gravity.TOP) {

            if (((gravity & Gravity.LEFT) == Gravity.LEFT) || ((gravity & Gravity.RIGHT) == Gravity.RIGHT)) {
                y =  targetViewY - toolTipMeasuredHeight + (int)adjustment;
            } else {
                y =  targetViewY - toolTipMeasuredHeight - (int)adjustment;
            }
        } else { // this is center
            if (((gravity & Gravity.LEFT) == Gravity.LEFT) || ((gravity & Gravity.RIGHT) == Gravity.RIGHT)) {
                y =  targetViewY + mHighlightedView.getHeight() - (int) adjustment;
            } else {
                y =  targetViewY + mHighlightedView.getHeight() + (int) adjustment;
            }
        }
        return y;
    }

    private void setupFrameLayout(){
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT);
        ViewGroup contentArea = (ViewGroup) mActivity.getWindow().getDecorView().findViewById(android.R.id.content);
        int [] pos = new int[2];
        contentArea.getLocationOnScreen(pos);
        // frameLayoutWithHole's coordinates are calculated taking full screen height into account
        // but we're adding it to the content area only, so we need to offset it to the same Y value of contentArea

        layoutParams.setMargins(0,-pos[1],0,0);
        contentArea.addView(mFrameLayout, layoutParams);
    }
}
