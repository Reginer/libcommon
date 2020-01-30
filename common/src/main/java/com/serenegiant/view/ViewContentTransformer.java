package com.serenegiant.view;
/*
 * libcommon
 * utility/helper classes for myself
 *
 * Copyright (c) 2014-2020 saki t_saki@serenegiant.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
*/

import android.graphics.Matrix;
import android.graphics.PointF;
import android.util.Log;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import java.util.Arrays;

import androidx.annotation.CallSuper;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Viewの表示内容の座標変換を行うためのヘルパークラス
 */
public abstract class ViewContentTransformer {
	private static final boolean DEBUG = true;	// TODO for debugging
	private static final String TAG = ViewContentTransformer.class.getSimpleName();

	/**
	 * インスタンス生成のためのヘルパーメソッド
	 * @param view
	 * @return
	 */
	@NonNull
	public static ViewContentTransformer newInstance(@NonNull final View view) {
		if (view instanceof TextureView) {
			return new TextureViewTransformer((TextureView)view);
		} else if (view instanceof ImageView) {
			return new ImageViewTransformer((ImageView) view);
		} else {
			return new DefaultTransformer(view);
		}
	}

//--------------------------------------------------------------------------------
	@NonNull
	protected final View mTargetView;
	/**
	 * デフォルトのトランスフォームマトリックス
	 * #setDefaultで変更していなければコンストラクタ実行時に
	 * Viewから取得したトランスフォームマトリックス
	 */
	@NonNull
	protected final Matrix mDefaultTransform = new Matrix();
	/**
	 * 現在のトランスフォームマトリックス
	 */
	@NonNull
	protected final Matrix mTransform = new Matrix();
	/**
	 * Matrixアクセスのワーク用float配列
	 */
	private final float[] work = new float[9];
	/**
	 * 平行移動量
	 */
	private float mCurrentTransX, mCurrentTransY;
	/**
	 * 拡大縮小率
	 */
	private float mCurrentScaleX, mCurrentScaleY;
	/**
	 * 回転角度
	 */
	private float mCurrentRotate;

	/**
	 * コンストラクタ
	 * @param view
	 */
	protected ViewContentTransformer(@NonNull final View view) {
		if (DEBUG) Log.v(TAG, "コンストラクタ:");
		mTargetView = view;
		updateTransform(true);
	}

	@NonNull
	public View getTargetView() {
		return mTargetView;
	}

	/**
	 * ViewContentTransformerで保持しているトランスフォームマトリックスを
	 * ターゲットViewに設定されているトランスフォームマトリックスに設定する
	 * @param setAsDefault 設定したトランスフォームマトリックスをデフォルトにトランスフォームマトリックスとして使うかどうか
	 * @return
	 */
	public abstract ViewContentTransformer updateTransform(final boolean setAsDefault);

	/**
	 * トランスフォームマトリックスを設定する
	 * @param transform nullなら単位行列が設定される
	 */
	@CallSuper
	public void setTransform(@Nullable final Matrix transform) {
		if (mTransform != transform) {
			mTransform.set(transform);
		}
	}

	/**
	 * トランスフォームマトリックスを設定する
	 * @param transform nullまたは要素数が9未満なら単位行列が設定される
	 */
	@CallSuper
	public void setTransform(@Nullable final float[] transform) {
		if ((transform != null) && (transform.length >= 9)) {
			mTransform.setValues(transform);
		} else {
			mTransform.set(null);
		}
	}

	/**
	 * トランスフォームマトリックスのコピーを取得
	 * @param transform nullなら内部で新しいMatrixを生成して返す, nullでなければコピーする
	 * @return
	 */
	@NonNull
	public Matrix getTransform(@Nullable final Matrix transform) {
		if (transform != null) {
			transform.set(mTransform);
			return transform;
		} else {
			return new Matrix(mTransform);
		}
	}

	/**
	 * トランスフォームマトリックスのコピーを取得
	 * @param transform nullまたは要素数が9未満なら内部で新しいfloat配列を生成して返す, そうでなければコピーする
	 * @param transform
	 * @return
	 */
	public float[] getTransform(@Nullable final float[] transform) {
		if ((transform != null) && (transform.length >= 9)) {
			mTransform.getValues(transform);
			return transform;
		} else {
			final float[] result = new float[9];
			mTransform.getValues(result);
			return result;
		}
	}

	/**
	 * デフォルトのトランスフォームマトリックスを設定
	 * @param transform
	 * @return
	 */
	public ViewContentTransformer setDefault(@NonNull final Matrix transform) {
		mDefaultTransform.set(transform);
		return this;
	}

	/**
	 * トランスフォームマトリックスを初期状態に戻す
	 * #setDefaultで変更していなけれあコンストラクタ実行時の
	 * ターゲットViewのトランスフォームマトリックスに戻る
	 */
	public void reset() {
		if (DEBUG) Log.v(TAG, "reset:");
		setTransform(mDefaultTransform);
	}

	/**
	 * 指定位置に移動
	 * @param x
	 * @param y
	 * @return
	 */
	public ViewContentTransformer setTranslate(final float x, final float y) {
		return setTransform(x, y,
			mCurrentScaleX, mCurrentScaleY,
			mCurrentRotate);
	}

	/**
	 * 現在位置からオフセット
	 * @param dx
	 * @param dy
	 * @return
	 */
	public ViewContentTransformer translate(final float dx, final float dy) {
		return setTransform(mCurrentTransX + dx, mCurrentTransY + dy,
			mCurrentScaleX, mCurrentScaleY,
			mCurrentRotate);
	}

	/**
	 * 移動量を取得
	 * @param tr
	 * @return
	 */
	public PointF getTranslate(@Nullable final PointF tr) {
		if (tr != null) {
			tr.set(mCurrentTransX, mCurrentTransY);
			return tr;
		} else {
			return new PointF(mCurrentTransX, mCurrentTransY);
		}
	}

	/**
	 * 移動量を取得
	 * @return
	 */
	public float getTranslateX() {
		return mCurrentTransX;
	}

	/**
	 * 移動量を取得
	 * @return
	 */
	public float getTranslateY() {
		return mCurrentTransY;
	}

	/**
	 * 指定倍率に拡大縮小
	 * @param scaleX
	 * @param scaleY
	 * @return
	 */
	public ViewContentTransformer setScale(final float scaleX, final float scaleY) {
		return setTransform(mCurrentTransX, mCurrentTransY,
			scaleX, scaleY,
			mCurrentRotate);
	}

	/**
	 * 指定倍率に拡大縮小
	 * @param scale
	 * @return
	 */
	public ViewContentTransformer setScale(final float scale) {
		return setTransform(mCurrentTransX, mCurrentTransY,
			scale, scale,
			mCurrentRotate);
	}

	/**
	 * 現在の倍率から拡大縮小
	 * @param scaleX
	 * @param scaleY
	 * @return
	 */
	public ViewContentTransformer scale(final float scaleX, final float scaleY) {
		return setTransform(mCurrentTransX, mCurrentTransY,
			mCurrentScaleX * scaleX, mCurrentScaleY * scaleY,
			mCurrentRotate);
	}

	/**
	 * 現在の倍率から拡大縮小
	 * @param scale
	 * @return
	 */
	public ViewContentTransformer scale(final float scale) {
		return setTransform(mCurrentTransX, mCurrentTransY,
			mCurrentScaleX * scale, mCurrentScaleY * scale,
			mCurrentRotate);
	}

	/**
	 * 現在の拡大縮小率(横方向)を取得
	 * @return
	 */
	public float getScaleX() {
		return mCurrentScaleX;
	}

	/**
	 * 現在の拡大縮小率(縦方向)を取得
 	 * @return
	 */
	public float getScaleY() {
		return mCurrentScaleY;
	}

	/**
	 * 縦横の拡大縮小率のうち小さい方を取得
	 * @return
	 */
	public float getScale() {
		return Math.min(mCurrentScaleX, mCurrentScaleY);
	}

	/**
	 * 指定角度に回転
	 * @param degrees
	 * @return
	 */
	public ViewContentTransformer setRotate(final float degrees) {
		return setTransform(mCurrentTransX, mCurrentTransY,
			mCurrentScaleX, mCurrentScaleY,
			degrees);
	}

	/**
	 * 現在の回転角度から回転
	 * @param degrees
	 * @return
	 */
	public ViewContentTransformer rotate(final float degrees) {
		return setTransform(mCurrentTransX, mCurrentTransY,
			mCurrentScaleX, mCurrentScaleY,
			mCurrentRotate + degrees);
	}

	/**
	 * 現在の回転角度[度]を取得
	 * @return
	 */
	public float getRotation() {
		return mCurrentRotate;
	}

	/**
	 * 指定した座標配列をトランスフォームマトリックスで変換する
	 * @param points
	 */
	public void mapPoints(@NonNull final float[] points) {
		mTransform.mapPoints(points);
	}

	/**
	 * 指定した座標配列をトランスフォームマトリックスで変換する
	 * @param dst 代入先の座標配列(x,y ペア)
	 * @param src 変換元の座標配列(x,y ペア)
	 */
	public void mapPoints(@NonNull final float[] dst, @NonNull final float[] src) {
		mTransform.mapPoints(dst, src);
	}

	/**
	 * トランスフォームマトリックスを設定
	 * @param transX
	 * @param transY
	 * @param scaleX
	 * @param scaleY
	 * @param degrees
	 * @return
	 */
	protected ViewContentTransformer setTransform(
		final float transX, final float transY,
		final float scaleX, final float scaleY,
		final float degrees) {

		if ((mCurrentTransX != transX) || (mCurrentTransY != transY)
			|| (mCurrentScaleX != scaleX) || (mCurrentScaleY != scaleY)
			|| (mCurrentRotate != degrees)) {

			mCurrentScaleX = scaleX;
			mCurrentScaleY = scaleY;
			mCurrentTransX = transX;
			mCurrentTransY = transY;
			mCurrentRotate = degrees;
			if (degrees != Float.MAX_VALUE) {
				while (mCurrentRotate > 360) {
					mCurrentRotate -= 360;
				}
				while (mCurrentRotate < -360) {
					mCurrentRotate += 360;
				}
			}
			mDefaultTransform.getValues(work);
			final int w2 = mTargetView.getWidth() >> 1;
			final int h2 = mTargetView.getHeight() >> 1;
			mTransform.reset();
			mTransform.postTranslate(transX, transY);
			mTransform.postScale(
				work[Matrix.MSCALE_X] * mCurrentScaleX,
				work[Matrix.MSCALE_Y] * mCurrentScaleY,
				w2, h2);
			if (degrees != Float.MAX_VALUE) {
				mTransform.postRotate(mCurrentRotate,
					w2, h2);
			}
			// apply to target view
			setTransform(mTransform);
		}
		return this;
	}

//--------------------------------------------------------------------------------
	protected static class DefaultTransformer extends  ViewContentTransformer {
		private static final String TAG = DefaultTransformer.class.getSimpleName();

		/**
		 * コンストラクタ
		 * @param view
		 */
		private DefaultTransformer(@NonNull final View view) {
			super(view);
			if (DEBUG) Log.v(TAG, "コンストラクタ:");
		}

		@Override
		public DefaultTransformer updateTransform(final boolean setAsDefault) {
			if (DEBUG) Log.v(TAG, "updateTransform:" + setAsDefault);
			// 今は何もしない
			return this;
		}

		@Override
		public void setTransform(@Nullable final Matrix transform) {
			super.setTransform(transform);
			if (DEBUG) Log.v(TAG, "setTransform:" + transform);
			mTransform.set(transform);
			internalSetTransform();
		}

		@Override
		public void setTransform(@Nullable final float[] transform) {
			super.setTransform(transform);
			if (DEBUG) Log.v(TAG, "setTransform:" + Arrays.toString(transform));
			internalSetTransform();
		}

		private void internalSetTransform() {
			// ローカルキャッシュ
			final View targetView = mTargetView;
			// XXX これだとView自体の大きさとかが変わってしまいそう
			targetView.setTranslationX(getTranslateX());
			targetView.setTranslationY(getTranslateY());
			targetView.setPivotX(targetView.getWidth() >> 1);
			targetView.setPivotY(targetView.getHeight() >> 1);
			targetView.setRotation(getRotation());
			targetView.setScaleX(getScaleX());
			targetView.setScaleX(getScaleY());
		}

	} // DefaultTransformer

//--------------------------------------------------------------------------------
	/**
	 * TextureView用ViewContentTransformer実装
	 */
	protected static class TextureViewTransformer extends ViewContentTransformer {
		private static final String TAG = TextureViewTransformer.class.getSimpleName();

		/**
		 * コンストラクタ
		 * @param view
		 */
		private TextureViewTransformer(@NonNull final TextureView view) {
			super(view);
			if (DEBUG) Log.v(TAG, "コンストラクタ:");
		}

		@NonNull
		@Override
		public TextureView getTargetView() {
			return (TextureView)mTargetView;
		}

		@Override
		public TextureViewTransformer updateTransform(final boolean setAsDefault) {
			if (DEBUG) Log.v(TAG, "updateTransform:" + setAsDefault);
			getTargetView().getTransform(mTransform);
			if (setAsDefault) {
				mDefaultTransform.set(mTransform);
			}
			return this;
		}

		@Override
		public void setTransform(@Nullable final Matrix transform) {
			super.setTransform(transform);
			if (DEBUG) Log.v(TAG, "setTransform:" + transform);
			getTargetView().setTransform(transform);
		}

		@Override
		public void setTransform(@Nullable final float[] transform) {
			super.setTransform(transform);
			if (DEBUG) Log.v(TAG, "setTransform:" + Arrays.toString(transform));

			getTargetView().setTransform(mTransform);
		}

		@NonNull
		@Override
		public Matrix getTransform(@Nullable final Matrix transform) {
			return getTargetView().getTransform(transform);
		}

	} // TextureViewTransformer

//--------------------------------------------------------------------------------
	/**
	 * ImageView用ImageViewTransformer実装
	 */
	protected static class ImageViewTransformer extends ViewContentTransformer {
		private static final String TAG = ImageViewTransformer.class.getSimpleName();

		/**
		 * コンストラクタ
		 * @param view
		 */
		private ImageViewTransformer(@NonNull final ImageView view) {
			super(view);
			if (DEBUG) Log.v(TAG, "コンストラクタ:");
		}

		@NonNull
		@Override
		public ImageView getTargetView() {
			return (ImageView)mTargetView;
		}

		@Override
		public ImageViewTransformer updateTransform(final boolean setAsDefault) {
			if (DEBUG) Log.v(TAG, "updateTransform:" + setAsDefault);
			mTransform.set(getTargetView().getImageMatrix());
			if (setAsDefault) {
				mDefaultTransform.set(mTransform);
			}
			return this;
		}

		@Override
		public void setTransform(@Nullable final Matrix transform) {
			super.setTransform(transform);
			if (DEBUG) Log.v(TAG, "setTransform:" + transform);
			getTargetView().setImageMatrix(mTransform);
		}

		@Override
		public void setTransform(@Nullable final float[] transform) {
			super.setTransform(transform);
			if (DEBUG) Log.v(TAG, "setTransform:" + Arrays.toString(transform));
			getTargetView().setImageMatrix(mTransform);
		}

	}	// ImageViewTransformer
}
