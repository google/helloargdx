/*
Copyright 2017 Google Inc.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
 */
package com.github.claywilkinson.arcore.gdx;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.LifecycleListener;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationBase;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.badlogic.gdx.backends.android.AndroidApplicationLogger;
import com.badlogic.gdx.backends.android.AndroidAudio;
import com.badlogic.gdx.backends.android.AndroidClipboard;
import com.badlogic.gdx.backends.android.AndroidFiles;
import com.badlogic.gdx.backends.android.AndroidInputFactory;
import com.badlogic.gdx.backends.android.AndroidNet;
import com.badlogic.gdx.backends.android.surfaceview.FillResolutionStrategy;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.google.ar.core.Config;
import com.google.ar.core.Session;
import java.lang.reflect.Method;

/**
 * Android Activity subclass that handles initializing ARCore and the underlying graphics engine
 * used for drawing 3d and 2d models in the context of the ARCore Frame. This class is based on the
 * libgdx library for Android game development.
 */
public class BaseARCoreActivity extends AndroidApplication {
  // ARCore specific stuff
  private Session mSession;
  private Config mDefaultConfig;

  public Session getSession() {
    return mSession;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    initializeARCore();
  }

  private void initializeARCore() {
    mSession = new Session(/*context=*/ this);
    // Create default config, check is supported, create session from that config.
    mDefaultConfig = Config.createDefaultConfig();
    if (!mSession.isSupported(mDefaultConfig)) {
      // TODO(avirodov): toast?
      finish();
      return;
    }
  }

  @Override
  protected void onResume() {
    if (CameraPermissionHelper.hasCameraPermission(this)) {
      mSession.resume(mDefaultConfig);
    } else {
      CameraPermissionHelper.requestCameraPermission(this);
    }
    super.onResume();
  }

  @Override
  protected void onPause() {
    super.onPause();
    mSession.pause();
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
    if (!CameraPermissionHelper.hasCameraPermission(this)) {
      Toast.makeText(this, "Augmented Reality requires camera permission", Toast.LENGTH_LONG)
          .show();
      finish();
    }
  }

  /**
   * This method has to be called in the {@link Activity#onCreate(Bundle)} method. It sets up all
   * the things necessary to get input, render via OpenGL and so on. You can configure other aspects
   * of the application with the rest of the fields in the {@link AndroidApplicationConfiguration}
   * instance.
   *
   * @param listener the {@link ApplicationListener} implementing the program logic
   * @param config the {@link AndroidApplicationConfiguration}, defining various settings of the
   *     application (use accelerometer, etc.).
   */
  public void initialize(ApplicationListener listener, AndroidApplicationConfiguration config) {
    init(listener, config, false);
  }

  private void init(
      ApplicationListener listener, AndroidApplicationConfiguration config, boolean isForView) {
    if (this.getVersion() < MINIMUM_SDK) {
      throw new GdxRuntimeException(
          "LibGDX requires Android API Level " + MINIMUM_SDK + " or later.");
    }
    setApplicationLogger(new AndroidApplicationLogger());
    graphics =
        new ARCoreGraphics(
            this,
            config,
            config.resolutionStrategy == null
                ? new FillResolutionStrategy()
                : config.resolutionStrategy);
    input = AndroidInputFactory.newAndroidInput(this, this, graphics.getView(), config);
    audio = new AndroidAudio(this, config);
    this.getFilesDir(); // workaround for Android bug #10515463
    files = new AndroidFiles(this.getAssets(), this.getFilesDir().getAbsolutePath());
    net = new AndroidNet(this);
    this.listener = listener;
    this.handler = new Handler();
    this.useImmersiveMode = config.useImmersiveMode;
    this.hideStatusBar = config.hideStatusBar;
    this.clipboard = new AndroidClipboard(this);

    // Add a specialized audio lifecycle listener
    addLifecycleListener(
        new LifecycleListener() {

          @Override
          public void resume() {
            // No need to resume audio here
          }

          @Override
          public void pause() {
            //     audio.pause();
          }

          @Override
          public void dispose() {
            audio.dispose();
          }
        });

    Gdx.app = this;
    Gdx.input = this.getInput();
    Gdx.audio = this.getAudio();
    Gdx.files = this.getFiles();
    Gdx.graphics = this.getGraphics();
    Gdx.net = this.getNet();

    if (!isForView) {
      try {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
      } catch (Exception ex) {
        log("AndroidApplication", "Content already displayed, cannot request FEATURE_NO_TITLE", ex);
      }
      getWindow()
          .setFlags(
              WindowManager.LayoutParams.FLAG_FULLSCREEN,
              WindowManager.LayoutParams.FLAG_FULLSCREEN);
      getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FORCE_NOT_FULLSCREEN);
      setContentView(graphics.getView(), createLayoutParams());
    }

    createWakeLock(config.useWakelock);
    hideStatusBar(this.hideStatusBar);
    useImmersiveMode(this.useImmersiveMode);
    if (this.useImmersiveMode && getVersion() >= Build.VERSION_CODES.KITKAT) {
      try {
        Class<?> vlistener =
            Class.forName("com.badlogic.gdx.backends.android.AndroidVisibilityListener");
        Object o = vlistener.newInstance();
        Method method = vlistener.getDeclaredMethod("createListener", AndroidApplicationBase.class);
        method.invoke(o, this);
      } catch (Exception e) {
        log("AndroidApplication", "Failed to create AndroidVisibilityListener", e);
      }
    }
  }
}
