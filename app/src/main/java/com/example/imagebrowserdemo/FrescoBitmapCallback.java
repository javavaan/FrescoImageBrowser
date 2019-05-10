package com.example.imagebrowserdemo;

import android.net.Uri;

/**
 * 图片下载监听接口
 *
 * @param <T>
 */
public interface FrescoBitmapCallback<T> {

    void onSuccess(Uri uri, T result, String type);

    void onFailure(Uri uri, Throwable throwable);

    void onCancel(Uri uri);
}
