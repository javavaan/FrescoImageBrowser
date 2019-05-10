package com.example.imagebrowserdemo

import android.graphics.Bitmap
import android.graphics.Rect
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.Toast
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.drawee.controller.BaseControllerListener
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.imagepipeline.image.ImageInfo
import com.facebook.imagepipeline.request.ImageRequest
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    val imageUrl = "https://ss0.bdstatic.com/70cFvHSh_Q1YnxGkpoWK1HF6hhy/it/u=3501285739,3530279799&fm=200&gp=0.jpg"
    private val datas = arrayListOf<String>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

//        datas.add("http://photocdn.sohu.com/20160206/mp58312418_1454765249938_3.gif")
//        datas.add("http://hiphotos.baidu.com/feed/pic/item/b21c8701a18b87d60e797f040c0828381f30fdf6.jpg")
//        datas.add("http://5b0988e595225.cdn.sohucs.com/images/20180921/ad344ee2b4de41d9b86d55b3523a33c3.gif")
//        datas.add("http://b-ssl.duitang.com/uploads/item/201705/08/20170508171924_P3YNE.gif")
//        datas.add("http://img5.imgtn.bdimg.com/it/u=3994226574,2046487095&fm=26&gp=0.jpg")
        datas.add("https://ss0.bdstatic.com/70cFvHSh_Q1YnxGkpoWK1HF6hhy/it/u=3501285739,3530279799&fm=200&gp=0.jpg")
        datas.add("https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=438320830,2486787388&fm=26&gp=0.jpg")
        datas.add("https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=438320830,2486787388&fm=26&gp=0.jpg")
        datas.add("https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=438320830,2486787388&fm=26&gp=0.jpg")
        datas.add("https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=438320830,2486787388&fm=26&gp=0.jpg")
        datas.add("https://ss2.bdstatic.com/70cFvnSh_Q1YnxGkpoWK1HF6hhy/it/u=438320830,2486787388&fm=26&gp=0.jpg")


        var controllerListener = object : BaseControllerListener<ImageInfo>() {
            override fun onFinalImageSet(id: String?, imageInfo: ImageInfo?, anim: Animatable?) {
                if (anim != null) {
                    anim.start()
                }

            }
        }
        val controller = Fresco.newDraweeControllerBuilder()
            .setAutoPlayAnimations(false)
            .setImageRequest(ImageRequest.fromUri(Uri.parse(imageUrl)))
            .setOldController(image.getController())
            .setControllerListener(controllerListener)
            .build()
        val hierarchy = image.getHierarchy()
        hierarchy.setActualImageScaleType(ScalingUtils.ScaleType.FIT_CENTER)
        image.controller = controller

        image.setOnClickListener {
            val imageview = ImageBrowserView(this)
            val layoutParams =
                RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            parentview.addView(imageview,layoutParams)
            val arr = IntArray(2)
            image.getLocationInWindow(arr)
            val rect = Rect()
            rect.left = arr[0]
            rect.top = arr[1]
            rect.right = arr[0]+image.width
            rect.bottom = arr[1]+image.height
            FrescoLoadUtil.getInstance().loadImageBitmap(imageUrl,object :FrescoBitmapCallback<Bitmap>{
                override fun onSuccess(uri: Uri?, result: Bitmap?, type: String?) {
                    imageview.initData(datas,rect,result,0,object :IImageActionListener{
                        override fun pageSelectedCallback(position: Int, view: ImageBrowserItemView?) {
                            Toast.makeText(this@MainActivity, ""+position, Toast.LENGTH_SHORT).show()
                        }

                        override fun onDragEnd() {
                            Toast.makeText(this@MainActivity, "拖拽结束", Toast.LENGTH_SHORT).show()
                            parentview.removeView(imageview)
                        }

                        override fun singleTapCallback() {
                            Toast.makeText(this@MainActivity, "单击", Toast.LENGTH_SHORT).show()

                        }

                        override fun longPressCallback() {
                            Toast.makeText(this@MainActivity, "长按", Toast.LENGTH_SHORT).show()

                        }

                    })
                }

                override fun onFailure(uri: Uri?, throwable: Throwable?) {
                }

                override fun onCancel(uri: Uri?) {
                }

            })

        }

    }
}
