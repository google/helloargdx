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
package com.github.claywilkinson.arcore.gdx;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;

import com.github.claywilkinson.arcore.gdx.util.AcceptableFuture;
import com.google.ar.core.ArCoreApk;

/**
 * A no-UI fragment that handles ARCore initialization.  This fragment handles
 * the camera permission, ARCore compatibility and version checks.
 * <p/>
 * To use this fragment, add it to the activity and call {@link ARSupportFragment#getArSupported}
 * to get the result determining if ARCore is supported or not.
 */
public class ARSupportFragment extends Fragment {
  public static final String TAG = "ARSupportFragment";
  private static final int RC_PERMISSIONS = 1000;

  private Handler handler;
  private AcceptableFuture<Boolean> future;
  private boolean userRequestedInstall = true;



  public ARSupportFragment() {
    // Required empty public constructor
    handler = new Handler();
    future = new AcceptableFuture<>(handler);
  }


  public AcceptableFuture<Boolean> getArSupported() {
    return future;
  }

  @Override
  public void onResume() {
    super.onResume();

    if (checkCameraPermission()) {
      checkArCore();
    }
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);


  }

  private boolean checkCameraPermission() {

    if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
      return true;
    }

    requestPermissions(new String[]{Manifest.permission.CAMERA}, RC_PERMISSIONS);
    return false;
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    if (requestCode == RC_PERMISSIONS) {
      for (int i = 0; i < permissions.length; i++) {
        if (permissions[i].equals(Manifest.permission.CAMERA) &&
                grantResults[i] == PackageManager.PERMISSION_GRANTED) {
          checkArCore();
          return;
        }
      }
      future.completeExceptionally(new IllegalStateException("Camera permission not granted"));
    }
  }

  private void checkArCore() {
    ArCoreApk.Availability availability = ArCoreApk.getInstance().checkAvailability(getActivity());
    if (availability.isTransient() && !future.isCancelled()) {
      // re-query at 5Hz while we check compatibility.
      handler.postDelayed(this::checkArCore, 200);
      return;
    }
    switch (availability) {
      case SUPPORTED_INSTALLED:
        //Done!
        future.complete(true);
        break;
      case SUPPORTED_APK_TOO_OLD:
      case SUPPORTED_NOT_INSTALLED:
        startInstallation();
        break;
      default:
        future.complete(false);
    }
  }

  private void startInstallation() {
    try {
      switch (ArCoreApk.getInstance().requestInstall(getActivity(), userRequestedInstall)) {
        case INSTALLED:
          // Success.
          future.complete(true);
          break;
        case INSTALL_REQUESTED:
          // Ensures next invocation of requestInstall() will either return
          // INSTALLED or throw an exception.
          userRequestedInstall = false;
      }
    } catch (Exception exception) {
      future.completeExceptionally(exception);
    }
  }

}
