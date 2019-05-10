package com.example.imagebrowserdemo

import android.content.Context
import android.support.v4.view.PagerAdapter
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.RelativeLayout

class ImageBrowserPagerAdapter : PagerAdapter {
    private var datas = arrayListOf<String>()
    private var context: Context? = null
    private var listener:IImageActionListener?=null


    constructor(context:Context,datas:ArrayList<String>?,listener:IImageActionListener) : super(){
        this.context = context
        if (datas!=null) {
            this.datas = datas
        }
        this.listener = listener
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {
        val view = ImageBrowserItemParent(context)
        view.tag = position
        view.getImage()?.setImageUrl(datas.get(position))
        view.getImage()?.setListener(listener)
        container.addView(view)
        return view
    }

    override fun isViewFromObject(p0: View, p1: Any): Boolean {
        return p0 == p1
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    override fun getItemPosition(`object`: Any): Int {
        if (datas.size == 0) {
            return POSITION_NONE
        } else {
            return super.getItemPosition(`object`)
        }
    }

    override fun getCount(): Int {
        return datas.size
    }


}