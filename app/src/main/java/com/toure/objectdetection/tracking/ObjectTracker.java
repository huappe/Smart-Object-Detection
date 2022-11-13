
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

package com.toure.objectdetection.tracking;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PointF;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.toure.objectdetection.env.Logger;
import com.toure.objectdetection.env.Size;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.microedition.khronos.opengles.GL10;

/**
 * True object detector/tracker class that tracks objects across consecutive preview frames. It
 * provides a simplified Java interface to the analogous native object defined by
 * jni/client_vision/tracking/object_tracker.*.
 *
 * <p>Currently, the ObjectTracker is a singleton due to native code restrictions, and so must be
 * allocated by ObjectTracker.getInstance(). In addition, release() should be called as soon as the
 * ObjectTracker is no longer needed, and before a new one is created.
 *
 * <p>nextFrame() should be called as new frames become available, preferably as often as possible.
 *
 * <p>After allocation, new TrackedObjects may be instantiated via trackObject(). TrackedObjects are
 * associated with the ObjectTracker that created them, and are only valid while that ObjectTracker
 * still exists.
 */
public class ObjectTracker {
  private static final Logger LOGGER = new Logger();
  private static final boolean DRAW_TEXT = false;
  /** How many history points to keep track of and draw in the red history line. */
  private static final int MAX_DEBUG_HISTORY_SIZE = 30;
  /**
   * How many frames of optical flow deltas to record. TODO(andrewharp): Push this down to the
   * native level so it can be polled efficiently into a an array for upload, instead of keeping a
   * duplicate copy in Java.
   */
  private static final int MAX_FRAME_HISTORY_SIZE = 200;

  private static final int DOWNSAMPLE_FACTOR = 2;
  protected static ObjectTracker instance;
  private static boolean libraryFound = false;

  static {
    try {
      System.loadLibrary("tensorflow_demo");
      libraryFound = true;
    } catch (UnsatisfiedLinkError e) {
      LOGGER.e("libtensorflow_demo.so not found, tracking unavailable");
    }
  }

  protected final int frameWidth;
  protected final int frameHeight;
  protected final boolean alwaysTrack;
  private final byte[] downsampledFrame;
  private final Map<String, TrackedObject> trackedObjects;
  private final Vector<PointF> debugHistory;
  private final LinkedList<TimestampedDeltas> timestampedDeltas;
  private final int rowStride;
  private final float[] matrixValues = new float[9];
  private long lastTimestamp;
  private FrameChange lastKeypoints;
  private long downsampledTimestamp;
  /** This will contain an opaque pointer to the native ObjectTracker */
  private long nativeObjectTracker;

  protected ObjectTracker(
      final int frameWidth, final int frameHeight, final int rowStride, final boolean alwaysTrack) {
    this.frameWidth = frameWidth;
    this.frameHeight = frameHeight;
    this.rowStride = rowStride;
    this.alwaysTrack = alwaysTrack;
    this.timestampedDeltas = new LinkedList<TimestampedDeltas>();

    trackedObjects = new HashMap<String, TrackedObject>();

    debugHistory = new Vector<PointF>(MAX_DEBUG_HISTORY_SIZE);

    downsampledFrame =
        new byte
            [(frameWidth + DOWNSAMPLE_FACTOR - 1)
                / DOWNSAMPLE_FACTOR
                * (frameWidth + DOWNSAMPLE_FACTOR - 1)
                / DOWNSAMPLE_FACTOR];
  }

  public static synchronized ObjectTracker getInstance(
      final int frameWidth, final int frameHeight, final int rowStride, final boolean alwaysTrack) {
    if (!libraryFound) {
      LOGGER.e(
          "Native object tracking support not found. "
              + "See tensorflow/examples/android/README.md for details.");
      return null;
    }

    if (instance == null) {
      instance = new ObjectTracker(frameWidth, frameHeight, rowStride, alwaysTrack);
      instance.init();
    } else {
      throw new RuntimeException(
          "Tried to create a new objectracker before releasing the old one!");
    }
    return instance;
  }

  public static synchronized void clearInstance() {
    if (instance != null) {
      instance.release();
    }
  }

  private static int floatToChar(final float value) {
    return Math.max(0, Math.min((int) (value * 255.999f), 255));
  }

  protected static native void downsampleImageNative(
      int width, int height, int rowStride, byte[] input, int factor, byte[] output);

  protected void init() {
    // The native tracker never sees the full frame, so pre-scale dimensions