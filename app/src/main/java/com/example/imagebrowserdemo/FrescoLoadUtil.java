package com.example.imagebrowserdemo;

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
    private FrescoLoadUtil() {
    }

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

    // 加载直接返回Bitmap
    public final void loadImageBitmap(final String url, final FrescoBitmapCallback<Bitmap> callback) {

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

    private void fetch(final Uri uri,final FrescoBitmapCallback<Bitmap> callback) throws Exception {
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
                } else if (uri.toString().endsWith(".gif")) {
                    mimeType = "image/gif";
                }
                final String finalMimeType = mimeType;

                callback.onSuccess(uri, bitmap, finalMimeType);

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
                final Throwable finalThrowable = throwable;

                callback.onFailure(uri, finalThrowable);
            }
        };
        dataSource.subscribe(subscriber, UiThreadImmediateExecutorService.getInstance());
    }

}
