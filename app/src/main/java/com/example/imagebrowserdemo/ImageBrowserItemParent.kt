package com.example.imagebrowserdemo

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.LinearLayout

class ImageBrowserItemParent : LinearLayout {
    private var image: ImageBrowserItemView? = null

    constructor(context: Context?) : super(context) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        init()
    }

    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
        init()
    }

    private fun init() {
        View.inflate(context, R.layout.img_item, this)
        image = findViewById(R.id.img)
    }

    fun resetImageView() {
        image?.resetViewState()
    }

    fun getImage(): ImageBrowserItemView? {
        return image
    }
}