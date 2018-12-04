/*
 * Copyright 2018 Google LLC
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
package com.github.claywilkinson.arcore.gdx.util;

import android.arch.core.util.Function;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.util.Consumer;
import android.util.Log;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * A class modeled after the 1.8 CompletableFuture class, but works in pre-API 24 for Android.
 * <p>
 * The callbacks are invoked on the supplied handlerReference, which typically is the main thread.
 *
 * @param <T> The result type.
 */
public class AcceptableFuture<T> implements Future<T> {
  private static final String TAG = "AcceptableFuture";

  // Hold a weak reference to avoid memory leaks.
  private final WeakReference<Handler> handlerReference;

  // The result if it is an exception
  private Throwable throwableResult;

  // The result.
  private T result;

  // Flag for cancelled.
  private boolean cancelled;

  // Sync object for waiting until the future is complete.
  private CountDownLatch latch = new CountDownLatch(1);

  // List of actions to call when complete.
  private List<Consumer<T>> actions;

  // List of exception handlers to call if there is an exception result.
  private List<Function<Throwable, T>> exceptionHandlers;

  /**
   * Creates an AcceptableFuture.  The handler is used to post the callbacks of the actions
   * or exception handlers when completed.
   *
   * @param handler - the handler for the callbacks.  Usually the UI handler for the UI thread.
   */
  public AcceptableFuture(Handler handler) {
    this.handlerReference = new WeakReference<>(handler);
    this.actions = new ArrayList<>();
    this.exceptionHandlers = new ArrayList<>();
  }

  @Override
  public boolean cancel(boolean mayInterruptIfRunning) {
    cancelled = true;
    boolean retval = throwableResult == null && result == null;
    throwableResult = new CancellationException();
    finsh();
    return retval;
  }

  @Override
  public boolean isCancelled() {
    return cancelled;
  }

  @Override
  public boolean isDone() {
    return throwableResult != null || result != null;
  }

  @Override
  public T get() throws ExecutionException, InterruptedException {
    latch.await();
    if (throwableResult instanceof CancellationException) {
      throw (CancellationException) throwableResult;
    }
    if (throwableResult != null) {
      throw new ExecutionException(throwableResult);
    }
    return result;
  }

  @Override
  public T get(long timeout, @NonNull TimeUnit timeUnit) throws ExecutionException, InterruptedException, TimeoutException {
    if (!latch.await(timeout, timeUnit)) {
      throw new TimeoutException();
    }

    return get();
  }

  /**
   * Completes the future with exception.
   *
   * @param throwable - the throwable to use when calling the exception handlers or to throw
   *                  when calling #get().
   *                  <p>Based on CompleteableFuture.</p>
   */
  public void completeExceptionally(Throwable throwable) {
    Log.w(TAG, "ARSupport Future completed exceptionally", throwable);
    this.throwableResult = throwable;
    finsh();
  }

  /**
   * Completes the future with the value.
   *
   * @param value - the value of the operation.
   */
  public void complete(T value) {
    this.result = value;
    finsh();
  }

  /**
   * Performs the finishing of the future.
   * <p>
   * This decrements the latch unblocking all calls to #get(). If there is a valid handler, the
   * actions or exception handlers are posted on the handler.
   */
  private void finsh() {
    latch.countDown();
    Handler handler = handlerReference.get();
    if (result != null && handler != null) {
      for (Consumer<T> action : actions) {
        try {
          handler.post(() -> action.accept(result));
        } catch (Throwable e) {
          Log.e(TAG, "Caught unhandled exception!", e);
        }
      }
    } else if (throwableResult != null && handler != null) {
      try {
        for (Function<Throwable, T> fn : exceptionHandlers) {
          handler.post(() -> fn.apply(throwableResult));
        }
      } catch (Throwable e) {
        Log.e(TAG, "Caught unhandled exception!", e);

      }
    }
    actions.clear();
    exceptionHandlers.clear();
  }

  /**
   * Adds an action to be called when the future completes without exception.
   * <p>
   * This action is invoked by posting to the handler.  Typically this is the UI thread's
   * handler.
   *
   * @param action - the Consumer to call.
   * @return this future.
   */
  public AcceptableFuture<Void> thenAccept(Consumer<T> action) {
    actions.add(action);
    return (AcceptableFuture<Void>) this;
  }

  /**
   * Adds an exception handling function to be invoked when the future completes with exception.
   * <p>
   * This function is invoked by posting to the handler.  Typically this is the UI thread's
   * handler .
   *
   * @param fn - the function to call when an exception is encountered.
   * @return this future.
   */
  public AcceptableFuture<T> exceptionally(Function<Throwable, T> fn) {
    exceptionHandlers.add(fn);
    return this;
  }
}
