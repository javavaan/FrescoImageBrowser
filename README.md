# FrescoImageBrowser
基于Fresco的图片浏览器
## 几个关键的view
- ImageBrowserView

最外层的view，处理了入场动画，还有一个viewpager
- ImageBrowserItemParent
这个是viewpager里面的item，这个view的作用就是包住ImageBrowserItemView，主要应用其LinearLayout可使子类超过父类的显示范围特性。
- ImageBrowserItemView 
这个是最核心的类，这里处理了手势滑动，缩放，退场动画，图片显示等
- ImageBrowserUtil Utils
找的开源的工具类