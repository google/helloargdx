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
package com.github.claywilkinson.helloargdx;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.badlogic.gdx.backends.android.AndroidFragmentApplication;
import com.github.claywilkinson.arcore.gdx.ARSupportFragment;
import com.github.claywilkinson.arcore.gdx.ARFragmentApplication;

/**
 * Main activity that extends the FragmentActivity and implements AndroidFragmentApplication
 * callbacks.  This allows loading ARCore functionality as a Fragment, which eventually could
 * be extended to load a non-AR fragment when running on a non-ARCore device.
 */
public class MainActivity extends FragmentActivity implements AndroidFragmentApplication.Callbacks {
  private static final String TAG = "HelloGDX sample";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    // Loads the fragment.  There is no layout for this fragment, so it is simply added.
    ARSupportFragment supportFragment = new ARSupportFragment();

    getSupportFragmentManager().beginTransaction().add(
            supportFragment, ARSupportFragment.TAG).commitAllowingStateLoss();

    // Add the listener to check for ARCore being supported.  If it is, then prompt the user
    // to use AR or 3D.
    supportFragment.getArSupported().thenAccept(useAR -> {
      AlertDialog.Builder builder =
              new AlertDialog.Builder(this).setMessage("Display Mode");

      if (useAR) {
        builder.setPositiveButton("Use AR",
                (dialogInterface, i) -> addDisplayFragment(true))
                .setNegativeButton("3D only",
                        (dialogInterface, i) -> addDisplayFragment(false));
      } else {
        builder.setNegativeButton("3D only",
                (dialogInterface, i) -> addDisplayFragment(false));
      }

      builder.show();

      // Exceptions only use 3D.
    }).exceptionally(ex -> {
      Log.e(TAG, "Exception checking for ARSupport", ex);
      addDisplayFragment(false);
      return null;
    });
  }

  private void addDisplayFragment(boolean useAr) {
    // Done with the AR support fragment, so remove it.
    removeSupportFragment();

    Fragment fragment;
    if (useAr) {
      fragment = new ARFragmentApplication();
      ((ARFragmentApplication)fragment).setScene(new HelloScene());
    } else {
      fragment = new NonARFragmentApplication();
      ((NonARFragmentApplication) fragment).setScene(new Hello3DScene());
    }
     // Finally place it in the layout.
    getSupportFragmentManager().beginTransaction()
            .add(android.R.id.content, fragment)
            .commitAllowingStateLoss();
  }

  private void removeSupportFragment() {
    Fragment fragment = getSupportFragmentManager().findFragmentByTag(ARSupportFragment.TAG);
    if (fragment != null) {
      getSupportFragmentManager().beginTransaction().remove(fragment).commitAllowingStateLoss();
    }
  }

  @Override
  public void exit() {
    Log.d(TAG,"Exiting, thanks for visiting!");
  }

  @Override
  public void onPointerCaptureChanged(boolean hasCapture) {

  }
}
