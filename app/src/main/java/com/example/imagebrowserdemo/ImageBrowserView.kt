package com.example.imagebrowserdemo

import android.animation.Animator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.support.v4.view.ViewPager
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageView
import android.widget.RelativeLayout
import com.example.imagebrowserdemo.util.ImageBrowserUtil

private const val ENTER_ANIM_DURATION = 200L

class ImageBrowserView : RelativeLayout {
    private var viewPager: ViewPager? = null
    private var adapter: ImageBrowserPagerAdapter? = null

    private var screenWidth = ImageBrowserUtil.getScreenWidth()
    private var screenHeight = ImageBrowserUtil.getScreenHeight() - ImageBrowserUtil.getStatusBarHeight()

    // 用于入场动画的view
    private var enterImage: ImageView? = null
    private var imageDatas = arrayListOf<String>()

    private var listener: IImageActionListener? = null

    // 入场时position
    private var enterPos = 0

    constructor(context: Context?) : super(context) {
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    }

    private fun init() {
        viewPager = BigImageBrowserViewPager(context)
        viewPager?.pageMargin = ImageBrowserUtil.dp2px(20F)
        enterImage = ImageView(context)
        val layoutParams =
            RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        val layoutParams2 =
            RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        addView(viewPager, layoutParams)
        addView(enterImage, layoutParams2)
        enterImage?.visibility = View.INVISIBLE
        viewPager?.setBackgroundColor(Color.parseColor("#000000"))
        viewPager?.background?.alpha = 0

        viewPager?.setOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {
                stopGifPlay()
                if (state == ViewPager.SCROLL_STATE_IDLE) {
                    resetPreViewState()
                }
            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
            }

            override fun onPageSelected(position: Int) {
                val view = viewPager?.findViewWithTag<ImageBrowserItemParent>(position)
                view?.getImage()?.startGifAnim()
                if (position != 0) {
                    view?.getImage()?.setCurrentRect(null)
                }
                listener?.pageSelectedCallback(position, view?.getImage())
            }

        })
    }

    /**
     * @param datas 所有图片的地址
     * @param listener 图片手势的监听
     * @param rect 图片的原始位置
     * @param bitmap 图片的bitmap
     * @param enterPos 点击第几张图片进入
     */
    fun initData(
        datas: ArrayList<String>?,
        rect: Rect?,
        bitmap: Bitmap?,
        enterPos: Int,
        listener: IImageActionListener
    ) {
        this.listener = listener
        this.enterPos = enterPos
        init()
        imageDatas.clear()
        if (datas != null) {
            imageDatas.addAll(datas)
        }
        adapter = ImageBrowserPagerAdapter(context, datas, listener)
        enterImage?.setImageBitmap(bitmap)
        enterAnim(rect)
    }

    /**
     * 入场动画
     */
    private fun enterAnim(rect: Rect?) {
        if (rect == null) {
            enterFageAnim()
        } else {
            enterZoomAnim(rect)
        }

    }

    /**
     * 缩放入场动画
     */
    private fun enterZoomAnim(rect: Rect?) {
        enterImage?.visibility = View.VISIBLE
        val width = rect?.width()
        val height = rect?.height()
        val lp = enterImage?.layoutParams
        lp?.width = width
        lp?.height = height
        // enterImage最终的目标宽度
        var targehtWidth = 0
        enterImage?.layoutParams = lp

        val currentTransY = rect?.top?.toFloat()!! - ImageBrowserUtil.getStatusBarHeight()

        enterImage?.translationX = rect?.left?.toFloat()!!
        enterImage?.translationY = currentTransY

        if ((screenWidth.toFloat() / width!!) <= (screenHeight.toFloat() / height!!)) {
            //说明宽度填满
            targehtWidth = screenWidth
        } else {
            // 说明高度填满
            targehtWidth = (width * (screenHeight.toFloat() / height)).toInt()
        }
        val targetTransY =
            (ImageBrowserUtil.getStatusBarHeight() + ImageBrowserUtil.getScreenHeight()) / 2 - (targehtWidth * height / width) / 2 - ImageBrowserUtil.getStatusBarHeight()

        var currentValue = 0
        var animPercent = 0F
        val animator = ValueAnimator.ofInt(width, targehtWidth)
        animator.duration = ENTER_ANIM_DURATION
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener {
            currentValue = it.animatedValue as Int
            animPercent = (currentValue - width).toFloat() / (targehtWidth - width)
            val lp = enterImage?.layoutParams
            lp?.width = currentValue
            lp?.height = currentValue * height / width
            enterImage?.layoutParams = lp
            enterImage?.translationX = (1F - animPercent) * (rect.left)
            enterImage?.translationY = currentTransY + animPercent * (targetTransY - currentTransY)
            viewPager?.background?.alpha = (animPercent * 255).toInt()
        }

        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
                showViewpager()
                postDelayed({
                    viewPager?.findViewWithTag<ImageBrowserItemParent>(enterPos)?.getImage()?.setCurrentRect(rect)
                    viewPager?.findViewWithTag<ImageBrowserItemParent>(enterPos)?.getImage()?.startGifAnim()
                }, 100)
            }

            override fun onAnimationCancel(animation: Animator?) {
                showViewpager()
                postDelayed({
                    viewPager?.findViewWithTag<ImageBrowserItemParent>(enterPos)?.getImage()?.setCurrentRect(rect)
                    viewPager?.findViewWithTag<ImageBrowserItemParent>(enterPos)?.getImage()?.startGifAnim()
                }, 100)
            }

            override fun onAnimationStart(animation: Animator?) {
            }

        })

        animator.start()
    }

    /**
     * 透明度渐变入场动画
     */
    private fun enterFageAnim() {
        var currentValue = 0F
        val animator = ValueAnimator.ofFloat(0F, 1F)
        animator.duration = ENTER_ANIM_DURATION
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.addUpdateListener {
            currentValue = it.animatedValue as Float

            viewPager?.background?.alpha = (currentValue * 255).toInt()
            viewPager?.alpha = currentValue
        }

        animator.addListener(object : Animator.AnimatorListener {
            override fun onAnimationRepeat(animation: Animator?) {

            }

            override fun onAnimationEnd(animation: Animator?) {
            }

            override fun onAnimationCancel(animation: Animator?) {
            }

            override fun onAnimationStart(animation: Animator?) {
                showViewpager()
            }

        })

        animator.start()
    }

    private fun showViewpager() {
        enterImage?.visibility = View.GONE
        viewPager?.adapter = adapter
    }

    /**
     * 重置页面状态
     */
    private fun resetPreViewState() {
        val currPos = viewPager?.currentItem ?: return
        val prePos = currPos - 1
        val behind = currPos + 1
        viewPager?.findViewWithTag<ImageBrowserItemParent>(prePos)?.resetImageView()
        viewPager?.findViewWithTag<ImageBrowserItemParent>(behind)?.resetImageView()
        viewPager?.findViewWithTag<ImageBrowserItemParent>(currPos)?.getImage()?.startGifAnim()
    }

    /**
     * 滑动的时候停止gif播放
     */
    private fun stopGifPlay(){
        val currPos = viewPager?.currentItem ?: return
        val prePos = currPos - 1
        val behind = currPos + 1
        viewPager?.findViewWithTag<ImageBrowserItemParent>(prePos)?.getImage()?.stopGifAnim()
        viewPager?.findViewWithTag<ImageBrowserItemParent>(behind)?.getImage()?.stopGifAnim()
        viewPager?.findViewWithTag<ImageBrowserItemParent>(currPos)?.getImage()?.stopGifAnim()
    }

    /**
     * 当应用切到后台再切换回来需要调用此方法播放gif
     */
    fun startGifPlay(){
        val currPos = viewPager?.currentItem ?: return
        viewPager?.findViewWithTag<ImageBrowserItemParent>(currPos)?.getImage()?.startGifAnim()
    }
}