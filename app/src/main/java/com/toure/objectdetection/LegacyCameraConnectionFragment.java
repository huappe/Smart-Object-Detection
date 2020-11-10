
package com.toure.objectdetection;

/*
 * Copyright 2019 The TensorFlow Authors. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Fragment;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Size;
import android.util.SparseIntArray;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;

import com.toure.objectdetection.customview.AutoFitTextureView;
import com.toure.objectdetection.env.ImageUtils;
import com.toure.objectdetection.env.Logger;

import java.io.IOException;
import java.util.List;

public class LegacyCameraConnectionFragment extends Fragment {
  private static final Logger LOGGER = new Logger();
  /** Conversion from screen rotation to JPEG orientation. */
  private static final SparseIntArray ORIENTATIONS = new SparseIntArray();

  static {
    ORIENTATIONS.append(Surface.ROTATION_0, 90);
    ORIENTATIONS.append(Surface.ROTATION_90, 0);
    ORIENTATIONS.append(Surface.ROTATION_180, 270);
    ORIENTATIONS.append(Surface.ROTATION_270, 180);
  }

    private Camera camera;
  private Camera.PreviewCallback imageListener;
  private Size desiredSize;
  /** The layout identifier to inflate for this Fragment. */
  private int layout;
  /** An {@link AutoFitTextureView} for camera preview. */
  private AutoFitTextureView textureView;
  /**
   * {@link TextureView.SurfaceTextureListener} handles several lifecycle events on a {@link
   * TextureView}.
   */
  private final TextureView.SurfaceTextureListener surfaceTextureListener =
      new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(
            final SurfaceTexture texture, final int width, final int height) {
initialiseCamera(texture);

        }

        @Override
        public void onSurfaceTextureSizeChanged(
            final SurfaceTexture texture, final int width, final int height) {}

        @Override