package com.example.imagebrowserdemo

interface IImageActionListener{

    /** 关闭 */
    fun onDragEnd()

    /** 单击回调 */
    fun singleTapCallback()

    /** 长按回调 */
    fun longPressCallback()
    /** 翻页回调 */
    fun pageSelectedCallback(position:Int,view:ImageBrowserItemView?)
}
