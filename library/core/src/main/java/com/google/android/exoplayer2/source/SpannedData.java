/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.android.exoplayer2.source;

import static com.google.android.exoplayer2.util.Assertions.checkArgument;
import static com.google.android.exoplayer2.util.Assertions.checkState;
import static java.lang.Math.min;

import android.util.SparseArray;
import androidx.annotation.Nullable;
import com.google.android.exoplayer2.C;

/**
 * Stores value objects associated with spans of integer keys.
 *
 * <p>This implementation is optimised for consecutive {@link #get(int)} calls with keys that are
 * close to each other in value.
 *
 * <p>Spans are defined by their own {@code startKey} (inclusive) and the {@code startKey} of the
 * next span (exclusive). The last span is open-ended.
 *
 * @param <V> The type of values stored in this collection.
 */
/* package */ final class SpannedData<V> {

  private int memoizedReadIndex;

  private final SparseArray<V> spans;

  /** Constructs an empty instance. */
  public SpannedData() {
    spans = new SparseArray<>();
  }

  /**
   * Returns the value associated with the span covering {@code key}.
   *
   * <p>{@link #appendSpan(int, Object)} must have been called at least once since the last call to
   * {@link #clear()}.
   *
   * <p>{@code key} must be greater than or equal to the previous value passed to {@link
   * #discardTo(int)} (or zero after {@link #clear()} has been called).
   */
  public V get(int key) {
    if (memoizedReadIndex == C.INDEX_UNSET) {
      memoizedReadIndex = 0;
    }
    while (memoizedReadIndex > 0 && key < spans.keyAt(memoizedReadIndex)) {
      memoizedReadIndex--;
    }
    while (memoizedReadIndex < spans.size() - 1 && key >= spans.keyAt(memoizedReadIndex + 1)) {
      memoizedReadIndex++;
    }
    return spans.valueAt(memoizedReadIndex);
  }

  /**
   * Adds a new span to the end starting at {@code startKey} and containing {@code value}.
   *
   * <p>{@code startKey} must be greater than or equal to the start key of the previous span. If
   * they're equal, the previous span is overwritten.
   */
  public void appendSpan(int startKey, V value) {
    if (memoizedReadIndex == C.INDEX_UNSET) {
      checkState(spans.size() == 0);
      memoizedReadIndex = 0;
    }

    checkArgument(spans.size() == 0 || startKey >= spans.keyAt(spans.size() - 1));
    spans.append(startKey, value);
  }

  /**
   * Returns the value associated with the end span, or null if the collection is empty.
   *
   * <p>This is either the last value passed to {@link #appendSpan(int, Object)}, or the value of
   * the span covering the index passed to {@link #discardFrom(int)}.
   */
  @Nullable
  public V getEndValue() {
    return spans.size() != 0 ? spans.valueAt(spans.size() - 1) : null;
  }

  /**
   * Discard the spans from the start up to {@code discardToKey}.
   *
   * <p>The span associated with {@code discardToKey} is not discarded (which means the last span is
   * never discarded).
   */
  public void discardTo(int discardToKey) {
    for (int i = 0; i < spans.size() - 1 && discardToKey >= spans.keyAt(i + 1); i++) {
      spans.removeAt(i);
      if (memoizedReadIndex > 0) {
        memoizedReadIndex--;
      }
    }
  }

  /**
   * Discard the spans from the end back to {@code discardFromKey}.
   *
   * <p>The span associated with {@code discardFromKey} is not discarded.
   */
  public void discardFrom(int discardFromKey) {
    for (int i = spans.size() - 1; i >= 0 && discardFromKey < spans.keyAt(i); i--) {
      spans.removeAt(i);
    }
    memoizedReadIndex = spans.size() > 0 ? min(memoizedReadIndex, spans.size() - 1) : C.INDEX_UNSET;
  }

  /** Remove all spans. */
  public void clear() {
    memoizedReadIndex = C.INDEX_UNSET;
    spans.clear();
  }
}