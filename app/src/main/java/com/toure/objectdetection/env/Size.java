/* Copyright 2019 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================*/

package com.toure.objectdetection.env;

import android.graphics.Bitmap;
import android.text.TextUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/** Size class independent of a Camera object. */
public class Size implements Comparable<Size>, Serializable {

  // 1.4 went out with this UID so we'll need to maintain it to preserve pending queries when
  // upgrading.
  public static final long serialVersionUID = 7689808733290872361L;

  public final int width;
  public final int height;

  public Size(final int width, final int height) {
    this.width = width;
    this.height = height;
  }

  public Size(final Bitmap bmp) {
    this.width = bmp.getWidth();
    this.height = bmp.getHeight();
  }

  /**
   * Rotate a size by the given number of degrees.
   *
   * @param size Size to rota