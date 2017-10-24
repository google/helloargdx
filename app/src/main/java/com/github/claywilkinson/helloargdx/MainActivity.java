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
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.github.claywilkinson.arcore.gdx.BaseARCoreActivity;

/**
 * Main activity that extends the ARCore activity. There is no actual code needed in this class, the
 * Scene instance passed to initialize() handles all the lifecycle events.
 */
public class MainActivity extends BaseARCoreActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
    // Setting this to true will make the view use the complete UI, hiding all the decor such as
    // Snackbars, etc.  If you have no other UI, it looks nice to be immersive.
    config.useImmersiveMode = false;
    initialize(new HelloScene(), config);
  }
}
