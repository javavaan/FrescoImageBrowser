package com.example.imagebrowserdemo.util;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import com.facebook.binaryresource.FileBinaryResource;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.common.ImageDecodeOptions;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.datasource.BaseBitmapDataSubscriber;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;

import java.io.File;


/**
 * Fresco加载工具类
 */
public class FrescoLoadUtil {

    private static volatile FrescoLoadUtil inst;
//    private ExecutorService executeBackgroundTask = Executors.newSingleThreadExecutor();

    public static FrescoLoadUtil getInstance() {
        if (inst == null) {
            synchronized (FrescoLoadUtil.class) {
                if (inst == null) {
                    inst = new FrescoLoadUtil();
                }
            }
        }
        return inst;
    }

    private FrescoLoadUtil() {
    }

    // 加载直接返回Bitmap
    public final void loadImageBitmap(String url, FrescoBitmapCallback<Bitmap> callback) {
        if (TextUtils.isEmpty(url)) {
            return;
        }
        try {
            fetch(Uri.parse(url), callback);
        } catch (OutOfMemoryError e) {
            // oom风险.
            e.printStackTrace();
            callback.onFailure(Uri.parse(url), e);
        } catch (Exception e) {
            e.printStackTrace();
            callback.onFailure(Uri.parse(url), e);
        }
    }

    private void fetch(final Uri uri, final FrescoBitmapCallback<Bitmap> callback) throws Exception {
        ImageDecodeOptions options = ImageDecodeOptions.newBuilder()
                .setForceStaticImage(true)
                .setDecodePreviewFrame(true).build();
        ImageRequestBuilder requestBuilder = ImageRequestBuilder.newBuilderWithSource(uri);
        ImageRequest imageRequest = requestBuilder.setImageDecodeOptions(options).build();
        DataSource<CloseableReference<CloseableImage>> dataSource = ImagePipelineFactory.getInstance()
                .getImagePipeline().fetchDecodedImage(imageRequest, null);
        BaseBitmapDataSubscriber subscriber = new BaseBitmapDataSubscriber() {
            @Override
            public void onNewResultImpl(final Bitmap bitmap) {
                if (callback == null) {
                    return;
                }
                String mimeType = "";
                FileBinaryResource resource = (FileBinaryResource) Fresco.getImagePipelineFactory()
                        .getMainFileCache().getResource(new SimpleCacheKey(uri.toString()));
                if (resource != null) {
                    File file = resource.getFile();
                    if (file != null) {
                        String filePath = file.getPath();
                        if (!TextUtils.isEmpty(filePath)) {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inJustDecodeBounds = true;
                            BitmapFactory.decodeFile(filePath, options);
                            if (options.outMimeType != null) {
                                mimeType = options.outMimeType;
                            }
                        }
                    }
                }

                callback.onSuccess(uri, bitmap, mimeType);
            }

            @Override
            public void onCancellation(DataSource<CloseableReference<CloseableImage>> dataSource) {
                super.onCancellation(dataSource);
                if (callback == null) {
                    return;
                }
                callback.onCancel(uri);
            }

            @Override
            public void onFailureImpl(DataSource dataSource) {
                if (callback == null) {
                    return;
                }
                Throwable throwable = null;
                if (dataSource != null) {
                    throwable = dataSource.getFailureCause();
                }
                callback.onFailure(uri, throwable);
            }
        };
        dataSource.subscribe(subscriber, UiThreadImmediateExecutorService.getInstance());
    }

    /**
     * 判断bitmap是否是有效的
     *
     * @return true 有效
     */
    public boolean isValidBitmap(Bitmap bitmap) {
        for (int i = 0; i < bitmap.getWidth(); i++) {
            for (int j = 0; j < bitmap.getHeight(); j++) {
                int pix = bitmap.getPixel(i, j);
                // 获取alpha值，只要大于0 说明不是透明图
                if (Color.alpha(pix) > 0) {
                    return true;
                }
            }
        }
        return false;
    }

}
