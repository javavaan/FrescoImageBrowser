package com.example.imagebrowserdemo

import android.animation.Animator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.SystemClock
import android.support.v4.view.ViewCompat
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.LinearInterpolator
import android.widget.LinearLayout
import android.widget.OverScroller
import android.widget.Toast
import com.example.imagebrowserdemo.util.ImageBrowserUtil
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.drawee.view.SimpleDraweeView
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequest

// 滑动距离大于200就关闭
private const val CLOSE_DISTANCE = 300
/**对应的gif缩放大小*/
private const val GIF_MIN_SCALE = 0.5F
private const val GIF_MAX_SCALE = 2F

/**
 * 平移使用scroll
 * 包含惯性滑动
 */
class ImageBrowserItemView : SimpleDraweeView, GestureDetector.OnDoubleTapListener, GestureDetector.OnGestureListener {


    // 图片最大显示区域的大小
    private var screenWidth = ImageBrowserUtil.getScreenWidth()
    private var screenHeight = ImageBrowserUtil.getScreenHeight() - ImageBrowserUtil.getStatusBarHeight()

    private var statusBarHeight = ImageBrowserUtil.getStatusBarHeight()

    // 图片y轴中心
    private var imageCenterY = (ImageBrowserUtil.getScreenHeight() + statusBarHeight) / 2F


    // gif 图片当前的缩放倍率
    var imgCurrentScale = 1F
    var imgCurrentScaleTemp = 1F

    // 当前背景透明度
    private var currentAlpha = 1F

    // 图片原始宽高
    private var imageWidth = screenWidth
    private var imageHeight = screenHeight

    // 图片显示宽高
    private var displayWidth = screenWidth
    private var displayHeight = screenHeight

    // 用于回坑动画的rect
    private var currentRect: Rect? = null


    // 触摸事件监听
    private var listener: IImageActionListener? = null

    private var gestureDetector: GestureDetector? = null

    private var velocityTracker = VelocityTracker.obtain()


    private var startX = 0F;
    private var startY = 0F;
    // x y 方向滑动距离
    private var disX = 0F
    private var disY = 0F

    // 上一次的滑动距离
    private var lastDisX = 0F
    private var lastDisY = 0F

    // 为了保持图片跟手 需设置的偏移x y
    private var extraX = 0F
    private var extraY = 0F

    // 是否在正在单指拖动
    private var draging = false

    // 是否正在处理双指缩放
    private var doubleFingerTouch = false

    // 上一次两指之间的距离
    private var disBetweenFingersPre = 0F


    // 当开始触摸的时候 图片缩放比例 在退出,返回原位动画用到
    private var imgScaleDown = 1F
    /**
     * 由于事件传递的当触摸事件需要父控件viewpager拦截是 会产生跳动的问题
     * 因为viewpager记录了点击事件的位置导致移动距离过大，
     * 如果需要父控件viewpager拦截那么需要重新设置他的lastMotion的位置防止跳跃
     */
    private var gifResetParentLastMotion = true

    // 放大状态下 图片水平方向的滑动距离
    private var scaleStateMovedX = 0
    private var scaleStateMovedY = 0

    // 在放大状态的拖拽
    private var scaleStateDraging = false

    private var scaleStateDragingBefore = false

    /**用于fling手势*/
    private var flingHelper = FlingHelper(context, this)

    private var flingEnable = true

    // 手指按下时scrollx和scrolly
    private var actionDownScrollX = 0
    private var actionDownScrollY = 0

    // gif 对应的Animatable
    private var gifAnimatable: Animatable? = null


    constructor(context: Context) : super(context) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context)
    }

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init(context)
    }

    override fun onDetachedFromWindow() {
        velocityTracker.recycle()
        super.onDetachedFromWindow()
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        val gestureEvent = gestureDetector?.onTouchEvent(event)
        val dragEvent = handleDragEvent(event)
        val result = (gestureEvent ?: false) || dragEvent
        return result
    }

    private fun init(context: Context) {
        // 手势监听
        gestureDetector = GestureDetector(context, this)
        gestureDetector?.setOnDoubleTapListener(this)

        // 惯性滑动
        flingHelper.scrollChangeCallback = { dx, dy ->
            if (flingEnable) {
                this@ImageBrowserItemView.scrollBy(-dx, -dy)
            }
        }
    }

    /**
     * 设置监听器，用于外部获取当前图片的手势状态
     */
    fun setListener(listener: IImageActionListener?) {
        this.listener = listener
    }

    /**
     * 设置图片url
     * @param url 图片url
     */
    fun setImageUrl(url: String?) {

        var controllerListener = object : BaseControllerListener<ImageInfo>() {
            override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, anim: Animatable?) {
                // 不自动播放动画，只有在显示的时候才执行动画
                gifAnimatable = anim
                setImageWidthAndHeight(imageInfo)
            }
        }
        val controller = Fresco.newDraweeControllerBuilder()
            .setAutoPlayAnimations(false)
                // 可以先设置缩略图 再设置原图
//            .setLowResImageRequest()
            .setImageRequest(ImageRequest.fromUri(Uri.parse(url)))
            .setOldController(this.getController())
            .setControllerListener(controllerListener)
            .build()
        val hierarchy = this.getHierarchy()
        // 此种缩放模式可保证imageviwe的大小超过父view
        hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.FOCUS_CROP)
        this.controller = controller

    }

    /**
     * 设置图片宽高
     */
    private fun setImageWidthAndHeight(imageInfo: ImageInfo?) {

        if (imageInfo?.width ?: 0 > 0) {
            imageWidth = imageInfo?.width!!
        }
        if (imageInfo?.height ?: 0 > 0) {
            imageHeight = imageInfo?.height!!
        }
        if ((screenWidth.toFloat() / imageWidth) <= (screenHeight.toFloat() / imageHeight)) {
            //说明宽度填满
            displayWidth = screenWidth
            displayHeight = ((screenWidth.toFloat() / imageWidth) * imageHeight).toInt()
        } else {
            // 说明高度填满
            displayWidth = (imageWidth * (screenHeight.toFloat() / imageHeight)).toInt()
            displayHeight = screenHeight
        }
        val lp = this.layoutParams as? LinearLayout.LayoutParams
        lp?.height = displayHeight
        lp?.width = displayWidth

        this.layoutParams = lp

    }


    /**
     * 初始滑动，双指缩放手势
     */
    private fun handleDragEvent(event: MotionEvent?): Boolean {
        if (event == null) {
            return false
        }
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                velocityTracker?.clear()
                velocityTracker?.addMovement(event)
                getViewPager()?.requestDisallowInterceptTouchEvent(true)
                getViewPager().onInterceptTouchEventFlag = false

                // 初始化或者设置一些参数 用于手势滑动
                startX = event.rawX
                startY = event.rawY
                imgScaleDown = imgCurrentScale
                imgCurrentScaleTemp = imgCurrentScale
                gifResetParentLastMotion = true
                draging = false
                lastDisX = 0F
                lastDisY = 0F
                actionDownScrollX = this.scrollX
                actionDownScrollY = this.scrollY
                scaleStateMovedX = this.scrollX
                scaleStateMovedY = this.scrollY
                flingEnable = true
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN -> {
                // 第二个手指放下要处理双指缩放
                disBetweenFingersPre = getDistance(event)
                if (!draging) {
                    doubleFingerTouch = true
                }
                draging = false
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                velocityTracker?.addMovement(event)


                if (startX <= 0.1F) {
                    startX = event.rawX
                    startY = event.rawY
                }
                disX = event.rawX - startX
                disY = event.rawY - startY
                //一根手指滑动 包括下拉关闭 和 图片放大状态的拖动
                if (event.pointerCount == 1) {
                    // 上下滑动的手势
                    if ((disY > 0 && Math.abs(disY) > Math.abs(disX)) || draging || imgCurrentScale > 1) {
                        if (imgCurrentScale > 1) {
                            // 当图片处于放大状态下
                            val verticalTop = disY > 0 && (Math.abs(disY) > Math.abs(disX) && getImgRect().top >= 0)
                            if ((draging || verticalTop) && scaleStateDraging == false) {

                            } else {
                                handleScaleStateMove(event)
                                return true
                            }
                        }
                        draging = true
                        // 缩放 start
                        var scale = 1F
                        scale = (1 - Math.abs(disY) / (screenHeight * 0.6F)) * imgCurrentScaleTemp / imgCurrentScale
                        if (((1 - Math.abs(disY) / (screenHeight * 0.6F)) * imgCurrentScaleTemp) < 0.3) {
                            scale = 0.3F / imgCurrentScale
                        }
                        imgCurrentScale = imgCurrentScale * scale
                        lastDisY = disY

                        val imgChangeScale = imgCurrentScale * 1F
//
                        setImageWidthHeightByScale(imgChangeScale)
                        // 缩放 end

                        // 平移start
                        extraX = (1F - imgCurrentScale / imgCurrentScaleTemp) * (startX - screenWidth / 2)
                        extraY = (1F - imgCurrentScale / imgCurrentScaleTemp) * (startY - screenHeight / 2)
                        this.scrollTo(
                            (actionDownScrollX * (imgCurrentScale / imgCurrentScaleTemp) - disX - extraX).toInt(),
                            (actionDownScrollY * (imgCurrentScale / imgCurrentScaleTemp) - disY - extraY).toInt()
                        )
                        // 平移end
                        // 设置viewpager背景透明度
                        currentAlpha = 1F - disY * 1.5F / screenHeight
                        if (currentAlpha > 1) {
                            currentAlpha = 1F
                        }
                        setBgAlpha(currentAlpha)
                        return true
                    } else {
                        // 交geiviewpager处理
                        viewpagerHandleDrag()
                        return false
                    }
                } else if (doubleFingerTouch) {
                    // 处理双指缩放
                    var scale = 1F + (getDistance(event) - disBetweenFingersPre) / 600
                    if (disBetweenFingersPre <= 0) {
                        scale = 1F
                    }
                    if (imgCurrentScale * scale < GIF_MIN_SCALE) {
                        scale = 1F
                    } else if (imgCurrentScale * scale > GIF_MAX_SCALE) {
                        scale = 1F
                    }
                    imgCurrentScale = imgCurrentScale * scale
                    imgCurrentScaleTemp = imgCurrentScale
                    setImageWidthHeightByScale(imgCurrentScale)
                    disBetweenFingersPre = getDistance(event)
                    return true
                } else {
                    viewpagerHandleDrag()
                    return false
                }
            }
            MotionEvent.ACTION_POINTER_UP -> {

                getViewPager().onInterceptTouchEventFlag = false
                startX = -1F
                startY = -1F
                imgScaleDown = imgCurrentScale
                if (imgCurrentScale < 1) {
                    doubleFingersDragEndBack()
                }
                return true
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                velocityTracker?.computeCurrentVelocity(1000)
                if (scaleStateDraging) {
                    // 处理放大状态下的惯性滚动
                    onMoveDragEnd(velocityTracker.xVelocity, velocityTracker.yVelocity)
                }
                getViewPager().onInterceptTouchEventFlag = false
                scaleStateDraging = false
                lastDisX = 0F
                lastDisY = 0F
                scaleStateDragingBefore = false

                if (draging) {
                    if (disY > CLOSE_DISTANCE) {
                        imgAnimClose()
                    } else {
                        imgAnimToBack()
                    }
                    return true
                }
                draging = false
            }

        }
        return false
    }

    /**
     * viewpager处理滑动
     */
    private fun viewpagerHandleDrag(){
        getViewPager()?.requestDisallowInterceptTouchEvent(false)
        getViewPager().onInterceptTouchEventFlag = true
    }

    /**
     * 根据缩放倍率设置图片宽高
     */
    private fun setImageWidthHeightByScale(scale: Float) {
        val lp = this.layoutParams
        lp.width = (displayWidth * scale).toInt()
        lp.height = (displayHeight * scale).toInt()
        this.layoutParams = lp
    }

    /**
     * 设置背景透明度
     */
    private fun setBgAlpha(alpha: Float) {
        getViewPager().background.alpha = (alpha * 255).toInt()
    }

    private fun getViewPager(): BigImageBrowserViewPager {
        var parentView = this.parent
        while (null != parentView && parentView !is BigImageBrowserViewPager) {
            parentView = parentView.parent
        }
        return parentView as BigImageBrowserViewPager
    }

    /**
     * 处理放大状态下的move
     */
    private fun handleScaleStateMove(event: MotionEvent) {
        scaleStateDraging = true
        if (lastDisX == 0F) {
            lastDisX = disX
            lastDisY = disY
            getViewPager()?.onInterceptTouchEventFlag = false
            getViewPager()?.requestDisallowInterceptTouchEvent(true)
            return
        }
        val movedX = lastDisX - disX
        val movedY = lastDisY - disY
        val rect = getImgRect()
        //水平方向移动
        if (movedX < 0) {
            // 向右移动
            if (rect.left < 0) {
                // 可以向右移动
                this.scrollBy(movedX.toInt(), 0)
            } else {
                handleViewpagerTouch(event, disX)
                return
            }
        } else {
            // 向左移动
            if (rect.right > screenWidth) {
                // 可以向左滑动
                this.scrollBy(movedX.toInt(), 0)
            } else {
                handleViewpagerTouch(event, disX)
                return
            }
        }
        // 竖直方向移动
        if (movedY < 0) {
            // 向下移动
            if (rect.top < 0) {
                this.scrollBy(0, movedY.toInt())
            }

        } else {
            // 向上移动
            if (rect.bottom > ImageBrowserUtil.getScreenHeight()) {
                this.scrollBy(0, movedY.toInt())
            }
        }
        scaleStateMovedX = this.scrollX
        scaleStateMovedY = this.scrollY
        lastDisX = disX
        lastDisY = disY
    }

    private fun onMoveDragEnd(xVelocity: Float, yVelocity: Float) {
        /**利用flingHelper进行fling动画*/
        val drawableRect = getImgRect()

        val minX: Int
        val maxX: Int
        val minY: Int
        val maxY: Int

        if (drawableRect.width() < screenWidth) {
            minX = (screenWidth - drawableRect.width()) / 2
            maxX = minX
        } else {
            minX = screenWidth - drawableRect.width()
            maxX = 0
        }

        if (drawableRect.height() < screenHeight) {
            minY = (screenHeight - drawableRect.height()) / 2
            maxY = minY
        } else {
            minY = screenHeight - drawableRect.height()
            maxY = 0
        }

        var overX = 115
        var overY = 115

        if (drawableRect.left >= 0 && drawableRect.right <= this.width) {
            overX = 0
        }

        if (drawableRect.top >= 0 && drawableRect.bottom <= this.height) {
            overY = 0
        }

        flingHelper.fling(
            drawableRect.left, drawableRect.top,
            xVelocity.toInt(), yVelocity.toInt(),
            minX, maxX, minY, maxY,
            overX, overY
        )

    }


    /*获取两指之间的距离*/
    private fun getDistance(event: MotionEvent): Float {
        val x = event.getX(1) - event.getX(0);
        val y = event.getY(1) - event.getY(0);
        val distance = Math.sqrt((x * x + y * y).toDouble());//两点间的距离
        return distance.toFloat();
    }

    /**
     * 获取图片显示的rect
     */
    private fun getImgRect(): Rect {
        val rect = Rect()
        val arr = IntArray(2)
        this.getLocationInWindow(arr)

        rect.left = (((screenWidth - imgCurrentScale * displayWidth) / 2) - this.scrollX).toInt()
        rect.top = (((screenHeight - imgCurrentScale * displayHeight) / 2) - this.scrollY).toInt()
        rect.right = (rect.left + imgCurrentScale * displayWidth).toInt()
        rect.bottom = (rect.top + imgCurrentScale * displayHeight).toInt()
        return rect
    }

    /**
     * 把滑动事件交给viewpager处理 解决viewpager跳动问题
     */
    private fun handleViewpagerTouch(event: MotionEvent, disX: Float) {
        flingEnable = false
        val parentViewPager = getViewPager()
        parentViewPager.onInterceptTouchEventFlag = true
        parentViewPager?.requestDisallowInterceptTouchEvent(false)

        /**一次完整的触摸事件只需要设置一次*/
        if (gifResetParentLastMotion) {
            gifResetParentLastMotion = false
            parentViewPager.setLastMotionX(event.rawX)
        }
    }

    /**
     * 双指缩放松开后 回到原始大小
     */
    private fun doubleFingersDragEndBack() {

        val scrollXTemp = this.scrollX
        val scrollYTemp = this.scrollY
        val animator = ValueAnimator.ofFloat(imgCurrentScale, 1F)
        animator.duration = 100
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener {
            val currentValue = it.animatedValue as Float
            val animPercent = (currentValue - imgCurrentScale) / (1F - imgCurrentScale)
            val x = scrollXTemp * (1 - animPercent)
            val y = scrollYTemp * (1 - animPercent)
            this.scrollTo(x.toInt(), y.toInt())
            setImageWidthHeightByScale(currentValue)
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                resetState()
            }

            override fun onAnimationCancel(animation: Animator?) {
                resetState()
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        })
        animator.start()
    }

    /**
     * 如果拖拽距离小于设定值，则返回原处
     */
    private fun imgAnimToBack() {
        var currentValue = 0F
        var animPercent = 0F
        val scrollYTemp = this.scrollY
        val scrollXTemp = this.scrollX
        val animator = ValueAnimator.ofFloat(imgCurrentScale, imgScaleDown)
        animator.duration = 300
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener {
            currentValue = it.animatedValue as Float
            setImageWidthHeightByScale(currentValue)
            animPercent = (currentValue - imgCurrentScale) / (imgScaleDown - imgCurrentScale)
            val scrollToX = scrollXTemp + (scaleStateMovedX - scrollXTemp) * animPercent
            val scrollToY = scrollYTemp + (scaleStateMovedY - scrollYTemp) * animPercent
            this.scrollTo(scrollToX.toInt(), scrollToY.toInt())
            setBgAlpha(currentAlpha + (1F - currentAlpha) * animPercent)
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                imgCurrentScale = imgScaleDown
            }

            override fun onAnimationCancel(animation: Animator?) {
                imgCurrentScale = imgScaleDown
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        })
        animator.start()
    }

    /**
     * 关闭动画
     * 外部可直接调用该方法关闭view
     */
    fun imgAnimClose() {
        if (currentRect != null) {
            imgZoomCloseAnim(currentRect!!)
        } else {
            imgFadeCloseAnim()
        }
    }

    /**
     * 缩放退场
     */
    private fun imgZoomCloseAnim(tarRect: Rect) {
        var currentValue = 0F
        var animPercent = 0F
        val gifCurrentScaleTemp = imgCurrentScale
        val targetScale = tarRect.width().toFloat() / screenWidth
        val scrollXTemp = this.scrollX
        val scrollYTemp = this.scrollY

        val targetTransY = (imageCenterY - displayHeight * targetScale / 2) - tarRect.top
        val targetScrollX = screenWidth * (1F - targetScale) / 2 - tarRect.left

        val animator = ValueAnimator.ofFloat(imgCurrentScale, targetScale)
        animator.addUpdateListener {
            currentValue = it.animatedValue as Float
            animPercent = (gifCurrentScaleTemp - currentValue) / (gifCurrentScaleTemp - targetScale)
            setImageWidthHeightByScale(currentValue)

            val x = scrollXTemp + (targetScrollX - scrollXTemp) * animPercent
            val y = scrollYTemp + (targetTransY - scrollYTemp) * animPercent
            this.scrollTo(x.toInt(), y.toInt())

            setBgAlpha(currentAlpha * (1F - animPercent))
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                listener?.onDragEnd()
            }

            override fun onAnimationCancel(animation: Animator?) {
                listener?.onDragEnd()
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        })
        animator.duration = 260
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    /**
     * 透明度渐变退场
     */
    private fun imgFadeCloseAnim() {
        val animator = ValueAnimator.ofFloat(0F, 1F)
        animator.addUpdateListener {
            val currentValue = it.animatedValue as Float
            setBgAlpha(currentAlpha * (1F - currentValue))
            getViewPager().alpha = 1F - currentValue
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                listener?.onDragEnd()
            }

            override fun onAnimationCancel(animation: Animator?) {
                listener?.onDragEnd()
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        })
        animator.duration = 260
        animator.interpolator = LinearInterpolator()
        animator.start()
    }

    /**
     * 重置状态到没有触摸事件的时候
     */
    private fun resetState() {
        setBgAlpha(1F)
        currentAlpha = 1F
        imgCurrentScale = 1F
        imgCurrentScaleTemp = 1F
        startX = 0F
        startY = 0F
        disX = 0F
        disY = 0F
        scaleStateMovedX = 0
        scaleStateMovedY = 0
    }


    // 处理双击手势 start
    // 当第二下down触发
    override fun onDoubleTap(e: MotionEvent?): Boolean {
        if (imgCurrentScale > 1F) {
            doubleClapScale(1F)
        } else if (imgCurrentScale == 1F) {
            doubleClapScale(GIF_MAX_SCALE)
        }
        return true
    }

    // 第二下down和up都触发
    override fun onDoubleTapEvent(e: MotionEvent?): Boolean {
        return false
    }

    override fun onSingleTapConfirmed(e: MotionEvent?): Boolean {
        listener?.singleTapCallback()
        return true
    }
    // 处理双击手势 end

    override fun onShowPress(e: MotionEvent?) {

    }

    override fun onSingleTapUp(e: MotionEvent?): Boolean {
        return false

    }

    override fun onDown(e: MotionEvent?): Boolean {
        return false
    }

    override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
        return false
    }

    override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent?) {
        listener?.longPressCallback()
    }

    /**
     * 双击缩小或放大
     */
    private fun doubleClapScale(targetScale: Float) {
        var currentValue = 0F
        var animPercent = 0F
        val scrollYTemp = this.scrollY
        val scrollXTemp = this.scrollX
        val animator = ValueAnimator.ofFloat(imgCurrentScale, targetScale)
        animator.duration = 200
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener {
            currentValue = it.animatedValue as Float
            setImageWidthHeightByScale(currentValue)
            animPercent = (currentValue - imgCurrentScale) / (targetScale - imgCurrentScale)
            val x = scrollXTemp * (1 - animPercent)
            val y = scrollYTemp * (1 - animPercent)
            this.scrollTo(x.toInt(), y.toInt())
        }
        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                if (targetScale == 1F) {
                    resetState()
                }
                imgCurrentScale = targetScale
            }

            override fun onAnimationCancel(animation: Animator?) {
                if (targetScale == 1F) {
                    resetState()
                }
                imgCurrentScale = targetScale
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        })
        animator.start()
    }


    /**
     * 设置当前rect
     */
    fun setCurrentRect(rect: Rect?) {
        currentRect = rect
    }

    /**
     * 外面调用 当view被滑走之后 归位
     */
    fun resetViewState() {
        this.scrollTo(0, 0)
        val lp = this.layoutParams
        lp.height = displayHeight
        lp.width = displayWidth
        this.layoutParams = lp

        resetState()
    }

    /**
     * 播放gif
     */
    fun startGifAnim() {
        gifAnimatable?.start()
    }

    /**
     * 停止gif
     */
    fun stopGifAnim() {
        gifAnimatable?.stop()
    }

}

/**
 * fling手势帮助类
 */
private class FlingHelper : Runnable {
    /**变化回调*/
    var scrollChangeCallback: ((dx: Int, dy: Int) -> Unit)? = null

    /**用于手势滑动*/
    private var overScroller: OverScroller

    /**对应的view*/
    private var attachedView: View

    private var preScrollX: Int = 0
    private var preScrollY: Int = 0

    constructor(context: Context, attachedView: View) {
        this.overScroller = OverScroller(context)
        this.attachedView = attachedView
    }

    override fun run() {
        if (overScroller.isFinished) {
            return
        }

        // 如果返回true表示动画还未结束
        if (overScroller.computeScrollOffset()) {
            val curScrollX = overScroller.currX
            val curScrollY = overScroller.currY

            scrollChangeCallback?.invoke(curScrollX - preScrollX, curScrollY - preScrollY)

            preScrollX = curScrollX
            preScrollY = curScrollY

            ViewCompat.postOnAnimation(attachedView, this)
        }
    }

    fun fling(
        startX: Int, startY: Int,
        velocityX: Int, velocityY: Int,
        minX: Int, maxX: Int,
        minY: Int, maxY: Int,
        overX: Int = 0, overY: Int = 0
    ) {
        preScrollX = startX
        preScrollY = startY

        overScroller.fling(
            startX,
            startY,
            (velocityX / 1.2).toInt(),
            (velocityY / 1.2).toInt(),
            minX,
            maxX,
            minY,
            maxY,
            overX,
            overY
        )

        ViewCompat.postOnAnimation(attachedView, this)
    }

    /**
     * 停止
     */
    fun stop() {
        overScroller.forceFinished(true)

        preScrollX = 0
        preScrollY = 0
    }

    fun isFinished(): Boolean {
        return overScroller.isFinished
    }


}