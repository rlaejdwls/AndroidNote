package com.example.customviewexample.task.spannable.gridview.manager

import android.content.Context
import android.graphics.PointF
import android.graphics.Rect
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearSmoothScroller
import androidx.recyclerview.widget.RecyclerView
import com.example.customviewexample.task.spannable.gridview.exception.InvalidMaxSpansException
import com.example.customviewexample.task.spannable.gridview.exception.InvalidSpanSizeException
import com.example.customviewexample.task.spannable.gridview.extensions.intersects
import com.example.customviewexample.task.spannable.gridview.extensions.isAdjacentTo

/**
 * Created by Hwang on 2018-10-17.
 *
 * Description : https://github.com/Arasthel/SpannedGridLayoutManager
 */
open class SpannedGridLayoutManager(val orientation: Orientation,
                                    val spans: Int) : RecyclerView.LayoutManager() {

    //==============================================================================================
    //  ~ Orientation & Direction enums
    //==============================================================================================

    /**
     * Orientations to layout and scroll views
     * <li>VERTICAL</li>
     * <li>HORIZONTAL</li>
     */
    enum class Orientation {
        VERTICAL, HORIZONTAL
    }

    /**
     * Direction of scroll for layouting process
     * <li>START</li>
     * <li>END</li>
     */
    enum class Direction {
        START, END
    }

    //==============================================================================================
    //  ~ Properties
    //==============================================================================================

    /**
     * Current scroll amount
     */
    var scroll = 0

    /**
     * Helper get free rects to place views
     */
    lateinit var rectsHelper: RectsHelper

    /**
     * First visible position in layout - changes with recycling
     */
    val firstVisiblePosition: Int get() {
        if (childCount == 0) { return 0 }
        return getPosition(getChildAt(0)!!)
    }

    /**
     * Last visible position in layout - changes with recycling
     */
    val lastVisiblePosition: Int get() {
        if (childCount == 0) { return 0 }
        return getPosition(getChildAt(childCount-1)!!)
    }

    /**
     * Start of the layout. Should be [getPaddingEndForOrientation] + first visible item top
     */
    var layoutStart = 0
    /**
     * End of the layout. Should be [layoutStart] + last visible item bottom + [getPaddingEndForOrientation]
     */
    var layoutEnd = 0

    /**
     * Total length of the layout depending on current orientation
     */
    val size: Int get() = if (orientation == Orientation.VERTICAL) height else width

    /**
     * Cache of rects for layouted views
     */
    val childFrames = mutableMapOf<Int, Rect>()

    /**
     * Temporary variable to store wanted scroll by [scrollToPosition]
     */
    var pendingScrollToPosition: Int? = null

    /**
     * Whether item order will be kept along re-creations of this LayoutManager with different
     * configurations of not, so . Default is false. Only set to true if this condition is met.
     * Otherwise, scroll bugs will happen.
     */
    var itemOrderIsStable = false

    //==============================================================================================
    //  ~ Initializer
    //==============================================================================================

    init {
        if (spans < 1) {
            throw InvalidMaxSpansException(spans)
        }
    }

    //==============================================================================================
    //  ~ Override parent
    //==============================================================================================

    override fun generateDefaultLayoutParams(): RecyclerView.LayoutParams {
        return RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
        )
    }

    //==============================================================================================
    //  ~ View layouting methods
    //==============================================================================================

    override fun onLayoutChildren(recycler: RecyclerView.Recycler, state: RecyclerView.State) {

        rectsHelper = RectsHelper(this, orientation)

        layoutStart = getPaddingStartForOrientation()
        layoutEnd = getPaddingEndForOrientation()

        // Clear cache, since layout may change
        childFrames.clear()

        // If there were any views, detach them so they can be recycled
        detachAndScrapAttachedViews(recycler)

        val pendingScrollToPosition = pendingScrollToPosition
        if (pendingScrollToPosition != null && pendingScrollToPosition >= spans) {

            scroll = 0

            var lastAddedView: View? = null
            var position = 0
            // Keep adding views until reaching the one needed
            for (i in 0 until pendingScrollToPosition) {
                if (lastAddedView != null) {
                    // Recycle views to reduce RAM usage
                    updateEdgesWithRemovedChild(lastAddedView, Direction.START)
                    removeAndRecycleView(lastAddedView, recycler)
                }
                lastAddedView = makeView(position, Direction.END, recycler)
                updateEdgesWithNewChild(lastAddedView)
                position++
            }

            val view = makeView(pendingScrollToPosition, Direction.END, recycler)
            val offset = if (orientation == Orientation.VERTICAL)
                view.top - getTopDecorationHeight(view) else
                view.left - getLeftDecorationWidth(view)
            removeAndRecycleView(view, recycler)

            layoutStart = offset
            scrollBy(-offset, state)
            fillAfter(pendingScrollToPosition, recycler, state, size)

            recycleChildrenOutOfBounds(Direction.END, recycler)

            this.pendingScrollToPosition = null
        } else {
            // Fill from start to visible end
            fillGap(Direction.END, recycler, state)

            recycleChildrenOutOfBounds(Direction.END, recycler)
        }
    }

    override fun onLayoutCompleted(state: RecyclerView.State) {
        super.onLayoutCompleted(state)

        // Check if after changes in layout we aren't out of its bounds
        val overScroll = scroll + size - layoutEnd - getPaddingEndForOrientation()
        val allItemsInScreen = firstVisiblePosition == 0 && lastVisiblePosition == state.itemCount - 1
        if (!allItemsInScreen && overScroll > 0) {
            // If we are, fix it
            scrollBy(overScroll, state)
        }
    }

    /**
     * Measure child view using [RectsHelper]
     */
    protected fun measureChild(position: Int, view: View) {

        val freeRectsHelper = this.rectsHelper

        val itemWidth = freeRectsHelper.itemSize
        val itemHeight = freeRectsHelper.itemSize

        if (view.layoutParams !is SpanLayoutParams) {
            throw TypeCastException("View LayoutParams must be of type '${SpanLayoutParams::class.java.name}'")
        }

        val layoutParams = (view.layoutParams as SpanLayoutParams)

        val spanSize = layoutParams.spanSize

        val usedSpan = if (orientation == Orientation.HORIZONTAL) spanSize.height else spanSize.width

        if (usedSpan > this.spans || usedSpan < 1) {
            throw InvalidSpanSizeException(errorSize = usedSpan, maxSpanSize = spans)
        }

        // This rect contains just the row and column number - i.e.: [0, 0, 1, 1]
        val rect = freeRectsHelper.findRect(position, spanSize)

        // Multiply the rect for item width and height to get positions
        val left = rect.left * itemWidth
        val right = rect.right * itemWidth
        val top = rect.top * itemHeight
        val bottom = rect.bottom * itemHeight

        var insetsRect = Rect()
        calculateItemDecorationsForChild(view, insetsRect)

        // Measure child
        val width = right - left - insetsRect.left - insetsRect.right
        val height = bottom - top - insetsRect.top - insetsRect.bottom
        layoutParams.width = width
        layoutParams.height = height
        measureChildWithMargins(view, width, height)

        // Remove free space from the helper
        freeRectsHelper.pushRect(position, rect)

        // Cache rect
        childFrames[position] = Rect(left, top, right, bottom)
    }

    /**
     * Layout child once it's measured and its position cached
     */
    protected fun layoutChild(position: Int, view: View) {
        val frame = childFrames[position]

        if (frame != null) {
            val scroll = this.scroll

            val startPadding = getPaddingStartForOrientation()

            if (orientation == Orientation.VERTICAL) {
                layoutDecorated(view,
                        frame.left + paddingLeft,
                        frame.top - scroll + startPadding,
                        frame.right + paddingLeft,
                        frame.bottom - scroll + startPadding)
            } else {
                layoutDecorated(view,
                        frame.left - scroll + startPadding,
                        frame.top + paddingTop,
                        frame.right - scroll + startPadding,
                        frame.bottom + paddingTop)
            }
        }

        // A new child was layouted, layout edges change
        updateEdgesWithNewChild(view)
    }

    /**
     * Ask the recycler for a view, measure and layout it and add it to the layout
     */
    protected fun makeAndAddView(position: Int, direction: Direction, recycler: RecyclerView.Recycler): View {
        val view = recycler.getViewForPosition(position)
        measureChild(position, view)
        layoutChild(position, view)

        if (direction == Direction.END) {
            addView(view)
        } else {
            addView(view, 0)
        }

        return view
    }

    protected fun makeView(position: Int, direction: Direction, recycler: RecyclerView.Recycler): View {
        val view = recycler.getViewForPosition(position)
        measureChild(position, view)
        layoutChild(position, view)

        return view
    }

    /**
     * A new view was added, update layout edges if needed
     */
    protected fun updateEdgesWithNewChild(view: View) {
        val childStart = getChildStart(view) + scroll + getPaddingStartForOrientation()

        if (childStart < layoutStart) {
            layoutStart = childStart
        }

        val childEnd = getChildEnd(view) + scroll + getPaddingStartForOrientation()

        if (childEnd > layoutEnd) {
            layoutEnd = childEnd
        }
    }

    //==============================================================================================
    //  ~ Recycling methods
    //==============================================================================================

    /**
     * Recycle any views that are out of bounds
     */
    protected fun recycleChildrenOutOfBounds(direction: Direction, recycler: RecyclerView.Recycler) {
        if (direction == Direction.END) {
            recycleChildrenFromStart(direction, recycler)
        } else {
            recycleChildrenFromEnd(direction, recycler)
        }
    }

    /**
     * Recycle views from start to first visible item
     */
    protected fun recycleChildrenFromStart(direction: Direction, recycler: RecyclerView.Recycler) {
        val childCount = childCount
        val start = getPaddingStartForOrientation()

        var detachedCount = 0

        for (i in 0 until childCount) {
            val child = getChildAt(i)!!
            val childEnd = getChildEnd(child)

            if (childEnd >= start) {
                break
            }

            detachedCount++
        }

        while (detachedCount-- > 0) {
            val child = getChildAt(0)!!
            removeAndRecycleView(child, recycler)
            updateEdgesWithRemovedChild(child, direction)
        }
    }

    /**
     * Recycle views from end to last visible item
     */
    protected fun recycleChildrenFromEnd(direction: Direction, recycler: RecyclerView.Recycler) {
        val childCount = childCount
        val end = size + getPaddingEndForOrientation()

        var firstDetachedPos = 0
        var detachedCount = 0

        for (i in (0 until childCount).reversed()) {
            val child = getChildAt(i)!!
            val childStart = getChildStart(child)

            if (childStart <= end) {
                break
            }

            firstDetachedPos = i
            detachedCount++
        }

        while (detachedCount-- > 0) {
            val child = getChildAt(firstDetachedPos)!!
            removeAndRecycleViewAt(firstDetachedPos, recycler)
            updateEdgesWithRemovedChild(child, direction)
        }
    }

    /**
     * Update layout edges when views are recycled
     */
    protected fun updateEdgesWithRemovedChild(view: View, direction: Direction) {
        val childStart = getChildStart(view) + scroll
        val childEnd = getChildEnd(view) + scroll

        if (direction == Direction.END) { // Removed from start
            layoutStart = getPaddingStartForOrientation() + childEnd
        } else if (direction == Direction.START) { // Removed from end
            layoutEnd = getPaddingStartForOrientation() + childStart
        }
    }

    //==============================================================================================
    //  ~ Scroll methods
    //==============================================================================================

    override fun computeVerticalScrollOffset(state: RecyclerView.State): Int {
        if (childCount == 0) {
            return 0
        }

        return firstVisiblePosition
    }

    override fun computeVerticalScrollExtent(state: RecyclerView.State): Int {
        return childCount
    }

    override fun computeVerticalScrollRange(state: RecyclerView.State): Int {
        return state.itemCount
    }

    override fun canScrollVertically(): Boolean {
        return orientation == Orientation.VERTICAL
    }

    override fun canScrollHorizontally(): Boolean {
        return orientation == Orientation.HORIZONTAL
    }

    override fun scrollHorizontallyBy(dx: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        return scrollBy(dx, recycler, state)
    }

    override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        return scrollBy(dy, recycler, state)
    }

    protected fun scrollBy(delta: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State): Int {
        var delta = delta

        // If there are no view or no movement, return
        if (delta == 0) {
            return 0
        }

        val canScrollBackwards = (firstVisiblePosition) >= 0 &&
                0 < scroll &&
                delta < 0

        val canScrollForward = (firstVisiblePosition + childCount) <= state.itemCount &&
                layoutEnd + getPaddingEndForOrientation() > (scroll + size) &&
                delta > 0

        // If can't scroll forward or backwards, return
        if (!(canScrollBackwards || canScrollForward)) {
            return 0
        }

        scrollBy(-delta, state)

        val direction = if (delta > 0) Direction.END else Direction.START

        recycleChildrenOutOfBounds(direction, recycler)

        fillGap(direction, recycler, state)

        return delta
    }

    /**
     * Scrolls distance based on orientation. Corrects distance if out of bounds.
     */
    protected fun scrollBy(distance: Int, state: RecyclerView.State) {
        var distance = distance

        val paddingEndLayout = getPaddingEndForOrientation()

        val start = 0
        val end = layoutEnd + paddingEndLayout

        scroll -= distance

        // Correct scroll if was out of bounds at start
        if (scroll < start) {
            distance += scroll
            scroll = start
        }

        // Correct scroll if it would make the layout scroll out of bounds at the end
        if (scroll + size > end && (firstVisiblePosition + childCount + spans) >= state.itemCount) {
            distance -= (end - scroll - size)
            scroll = end - size
        }

        if (orientation == Orientation.VERTICAL) {
            offsetChildrenVertical(distance)
        } else{
            offsetChildrenHorizontal(distance)
        }
    }

    /**
     * Checks if more views can be added between the current scroll and a limit
     */
    protected fun canAddMoreViews(direction: Direction, limit: Int): Boolean {
        return if (direction == Direction.START) {
            firstVisiblePosition > 0 && limit < layoutStart
        } else {
            limit > layoutEnd
        }
    }

    override fun scrollToPosition(position: Int) {
        pendingScrollToPosition = position

        requestLayout()
    }

    override fun smoothScrollToPosition(recyclerView: RecyclerView, state: RecyclerView.State, position: Int) {
        val smoothScroller = object: LinearSmoothScroller(recyclerView.context) {

            override fun computeScrollVectorForPosition(targetPosition: Int): PointF? {
                if (childCount == 0) {
                    return null
                }

                val direction = if (targetPosition < firstVisiblePosition) -1 else 1
                return PointF(0f, direction.toFloat())
            }

            override fun getVerticalSnapPreference(): Int {
                return LinearSmoothScroller.SNAP_TO_START
            }
        }

        smoothScroller.targetPosition = position
        startSmoothScroll(smoothScroller)
    }

    /**
     * Fills gaps on the layout, on directions [Direction.START] or [Direction.END]
     */
    protected fun fillGap(direction: Direction, recycler: RecyclerView.Recycler, state: RecyclerView.State) {
        val firstPosition = firstVisiblePosition

        if (direction == Direction.END) {
            fillAfter(firstPosition + childCount, recycler, state, size)
        } else {
            fillBefore(firstPosition - 1, recycler, 0)
        }
    }

    /**
     * Fill gaps before the given position
     * @param position Item position in adapter
     * @param recycler Recycler
     * @param extraSpace Extra space to add to current [scroll] and use as limit for filling the gaps
     */
    protected fun fillBefore(position: Int, recycler: RecyclerView.Recycler, extraSpace: Int) {
        var position = position
        val limit = getPaddingStartForOrientation() + scroll + extraSpace

        var startOfLine: Int? = null
        val isInIncompleteLine: (Int) -> Boolean = {
            val start = if (orientation == Orientation.VERTICAL) childFrames[it]?.top else childFrames[it]?.left
            start == startOfLine
        }
        while ((canAddMoreViews(Direction.START, limit) || isInIncompleteLine(position)) && position >= 0) {
            makeAndAddView(position, Direction.START, recycler)
            startOfLine = if (orientation == Orientation.VERTICAL) childFrames[position]?.top else childFrames[position]?.left
            position--
        }
    }

    /**
     * Fill gaps after the given position
     * @param position Item position in adapter
     * @param recycler Recycler
     * @param extraSpace Extra space to add to current [scroll] and use as limit for filling the gaps
     */
    protected fun fillAfter(position: Int, recycler: RecyclerView.Recycler, state: RecyclerView.State, extraSpace: Int) {
        var position = position
        val itemAtPosition = findViewByPosition(position - 1)
        val decorTop = if (itemAtPosition != null) getTopDecorationHeight(itemAtPosition) else 0
        val limit = getPaddingStartForOrientation() + scroll + size + extraSpace + decorTop

        while (canAddMoreViews(Direction.END, limit) && position < state.itemCount) {
            makeAndAddView(position, Direction.END, recycler)
            position++
        }
    }

    //==============================================================================================
    //  ~ Decorated position and sizes
    //==============================================================================================

    override fun getDecoratedMeasuredWidth(child: View): Int {
        val position = getPosition(child)
        return childFrames[position]!!.width()
    }

    override fun getDecoratedMeasuredHeight(child: View): Int {
        val position = getPosition(child)
        return childFrames[position]!!.height()
    }

    override fun getDecoratedTop(child: View): Int {
        val position = getPosition(child)
        val decoration = getTopDecorationHeight(child)
        var top = childFrames[position]!!.top + decoration

        if (orientation == Orientation.VERTICAL) {
            top -= scroll
        }

        return top
    }

    override fun getDecoratedRight(child: View): Int {
        val position = getPosition(child)
        val decoration = getLeftDecorationWidth(child) + getRightDecorationWidth(child)
        var right = childFrames[position]!!.right + decoration

        if (orientation == Orientation.HORIZONTAL) {
            right -= scroll - getPaddingStartForOrientation()
        }

        return right
    }

    override fun getDecoratedLeft(child: View): Int {
        val position = getPosition(child)
        val decoration = getLeftDecorationWidth(child)
        var left = childFrames[position]!!.left + decoration

        if (orientation == Orientation.HORIZONTAL) {
            left -= scroll
        }

        return left
    }

    override fun getDecoratedBottom(child: View): Int {
        val position = getPosition(child)
        val decoration = getTopDecorationHeight(child) + getBottomDecorationHeight(child)
        var bottom = childFrames[position]!!.bottom + decoration

        if (orientation == Orientation.VERTICAL) {
            bottom -= scroll - getPaddingStartForOrientation()
        }
        return bottom
    }

    //==============================================================================================
    //  ~ Orientation Utils
    //==============================================================================================

    protected fun getPaddingStartForOrientation(): Int {
        if (orientation == Orientation.VERTICAL) {
            return paddingTop
        } else {
            return paddingLeft
        }
    }

    protected fun getPaddingEndForOrientation(): Int {
        if (orientation == Orientation.VERTICAL) {
            return paddingBottom
        } else {
            return paddingRight
        }
    }

    protected fun getChildStart(child: View): Int {
        if (orientation == Orientation.VERTICAL) {
            return getDecoratedTop(child)
        } else {
            return getDecoratedLeft(child)
        }
    }

    protected fun getChildEnd(child: View): Int {
        if (orientation == Orientation.VERTICAL) {
            return getDecoratedBottom(child)
        } else {
            return getDecoratedRight(child)
        }
    }

    protected fun getChildSize(child: View): Int {
        if (orientation == Orientation.VERTICAL) {
            return getDecoratedMeasuredWidth(child)
        } else {
            return getDecoratedMeasuredHeight(child)
        }
    }

    //==============================================================================================
    //  ~ Save & Restore State
    //==============================================================================================

    override fun onSaveInstanceState(): Parcelable? {
        return if (itemOrderIsStable && childCount > 0) {
            logMessage("Saving first visible position: $firstVisiblePosition")
            SavedState(firstVisiblePosition)
        } else {
            null
        }
    }

    override fun onRestoreInstanceState(state: Parcelable) {
        logMessage("Restoring state")
        val savedState = state as? SavedState
        if (savedState != null) {
            val firstVisibleItem = savedState.firstVisibleItem
            scrollToPosition(firstVisibleItem)
        }
    }

    companion object {
        const val TAG = "SpannedGridLayoutMan"
        const val DEBUG = false

        fun logMessage(message: String) {
            if (DEBUG) Log.d(TAG, message)
        }
    }

    class SavedState(val firstVisibleItem: Int): Parcelable {

        companion object {

            @JvmField val CREATOR = object: Parcelable.Creator<SavedState> {

                override fun createFromParcel(source: Parcel): SavedState {
                    val state = SavedState(source.readInt())
                    return state
                }

                override fun newArray(size: Int): Array<SavedState?> {
                    return arrayOfNulls(size)
                }
            }
        }

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.writeInt(firstVisibleItem)
        }

        override fun describeContents(): Int {
            return 0
        }

    }

}

/**
 * A helper to find free rects in the current layout.
 */
open class RectsHelper(val layoutManager: SpannedGridLayoutManager,
                       val orientation: SpannedGridLayoutManager.Orientation) {

    /**
     * Comparator to sort free rects by position, based on orientation
     */
    val rectComparator = Comparator<Rect> { rect1, rect2 ->
        when (orientation) {
            SpannedGridLayoutManager.Orientation.VERTICAL -> {
                if (rect1.top == rect2.top) {
                    if (rect1.left < rect2.left) { -1 } else { 1 }
                } else {
                    if (rect1.top < rect2.top) { -1 } else { 1 }
                }
            }
            SpannedGridLayoutManager.Orientation.HORIZONTAL -> {
                if (rect1.left == rect2.left) {
                    if (rect1.top < rect2.top) { -1 } else { 1 }
                } else {
                    if (rect1.left < rect2.left) { -1 } else { 1 }
                }
            }
        }

    }

    /**
     * Cache of rects that are already used
     */
    val rectsCache = mutableMapOf<Int, Rect>()

    /**
     * List of rects that are still free
     */
    val freeRects = mutableListOf<Rect>()

    /**
     * Free space to divide in spans
     */
    val size: Int get() {
        if (orientation == SpannedGridLayoutManager.Orientation.VERTICAL) {
            return layoutManager.width - layoutManager.paddingLeft - layoutManager.paddingRight
        } else {
            return layoutManager.height - layoutManager.paddingTop - layoutManager.paddingBottom
        }
    }

    /**
     * Space occupied by each span
     */
    val itemSize: Int get() = size / layoutManager.spans

    /**
     * Start row/column for free rects
     */
    val start: Int get() {
        if (orientation == SpannedGridLayoutManager.Orientation.VERTICAL) {
            return freeRects[0].top * itemSize
        } else {
            return freeRects[0].left * itemSize
        }
    }

    /**
     * End row/column for free rects
     */
    val end: Int get() {
        if (orientation == SpannedGridLayoutManager.Orientation.VERTICAL) {
            return (freeRects.last().top + 1) * itemSize
        } else {
            return (freeRects.last().left + 1) * itemSize
        }
    }

    init {
        val initialFreeRect: Rect
        // There will always be a free rect that goes to Int.MAX_VALUE
        if (orientation == SpannedGridLayoutManager.Orientation.VERTICAL) {
            initialFreeRect = Rect(0, 0, layoutManager.spans, Int.MAX_VALUE)
        } else {
            initialFreeRect = Rect(0, 0, Int.MAX_VALUE, layoutManager.spans)
        }
        freeRects.add(initialFreeRect)
    }

    /**
     * Get a free rect for the given span and item position
     */
    fun findRect(position: Int, spanSize: SpanSize): Rect {
        val rect = rectsCache[position] ?: findRectForSpanSize(spanSize)
        return rect
    }

    /**
     * Find a valid free rect for the given span size
     */
    protected fun findRectForSpanSize(spanSize: SpanSize): Rect {
        val lane = freeRects.first {
            val itemRect = Rect(it.left, it.top, it.left+spanSize.width, it.top + spanSize.height)
            it.contains(itemRect)
        }

        return Rect(lane.left, lane.top, lane.left+spanSize.width, lane.top + spanSize.height)
    }

    /**
     * Push this rect for the given position, subtract it from [freeRects]
     */
    fun pushRect(position: Int, rect: Rect) {
        rectsCache[position] = rect
        subtract(rect)
    }

    /**
     * Remove this rect from the [freeRects], merge and reorder new free rects
     */
    protected fun subtract(subtractedRect: Rect) {
        val interestingRects = freeRects.filter { it.isAdjacentTo(subtractedRect) || it.intersects(subtractedRect) }

        val possibleNewRects = mutableListOf<Rect>()
        val adjacentRects = mutableListOf<Rect>()

        for (free in interestingRects) {
            if (free.isAdjacentTo(subtractedRect) && !subtractedRect.contains(free)) {
                adjacentRects.add(free)
            } else {
                freeRects.remove(free)

                if (free.left < subtractedRect.left) { // Left
                    possibleNewRects.add(Rect(free.left, free.top, subtractedRect.left, free.bottom))
                }

                if (free.right > subtractedRect.right) { // Right
                    possibleNewRects.add(Rect(subtractedRect.right, free.top, free.right, free.bottom))
                }

                if (free.top < subtractedRect.top) { // Top
                    possibleNewRects.add(Rect(free.left, free.top, free.right, subtractedRect.top))
                }

                if (free.bottom > subtractedRect.bottom) { // Bottom
                    possibleNewRects.add(Rect(free.left, subtractedRect.bottom, free.right, free.bottom))
                }
            }
        }

        for (rect in possibleNewRects) {
            val isAdjacent = adjacentRects.firstOrNull { it != rect && it.contains(rect) } != null
            if (isAdjacent) continue

            val isContained = possibleNewRects.firstOrNull { it != rect && it.contains(rect) } != null
            if (isContained) continue

            freeRects.add(rect)
        }

        freeRects.sortWith(rectComparator)
    }
}

/**
 * Helper to store width and height spans
 */
class SpanSize(val width: Int, val height: Int)

class SpanLayoutParams: RecyclerView.LayoutParams {

    var spanSize = SpanSize(width = 0, height = 0)

    constructor(spanSize: SpanSize): super(0, 0) {
        this.spanSize = spanSize
    }

    constructor(context: Context, attrs: AttributeSet): super(context, attrs)
    constructor(width: Int, height: Int): super(width, height)
    constructor(source: ViewGroup.MarginLayoutParams): super(source)
    constructor(source: ViewGroup.LayoutParams): super(source)
    constructor(source: RecyclerView.LayoutParams): super(source)
}