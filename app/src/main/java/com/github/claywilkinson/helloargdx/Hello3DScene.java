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

import com.badlogic.gdx.ApplicationListener;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.PerspectiveCamera;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.math.Vector3;

/**
 * Hello 3D  scene is a simple scene to show in non-AR mode.
 */
public class Hello3DScene implements ApplicationListener {
  private PerspectiveCamera camera;
  private ModelBatch modelBatch;
  private AssetManager assetManager = new AssetManager();
  private AndyModel andyModel;
  private ModelInstance item;

  @Override
  public void create() {
    camera = new PerspectiveCamera(67, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    camera.position.set(0, 1.6f, 0f);
    camera.lookAt(0, 0, -1f);
    camera.near = .01f;
    camera.far = 30f;
    camera.update();

    modelBatch = new ModelBatch();

    // Start loading the andy model.
    andyModel = new AndyModel(assetManager);

    Gdx.gl.glClearColor(.25f, .25f, .25f, 1f);
  }

  @Override
  public void resize(int width, int height) {

  }

  @Override
  public void render() {
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);
    Gdx.gl.glDepthMask(true);
    Gdx.gl.glEnable(GL20.GL_DEPTH_TEST);
    Gdx.gl.glEnable(GL20.GL_CULL_FACE);

    if (item == null) {
      handleInput();
    }
    camera.update();
    modelBatch.begin(camera);

    // Let the asset manager work asynchronously.
    assetManager.update();
    if (!andyModel.isInitialized()) {
      andyModel.initialize(assetManager);
    }

    if (item != null) {
      modelBatch.render(item);
    }
    modelBatch.end();
  }

  @Override
  public void pause() {

  }

  @Override
  public void resume() {

  }

  @Override
  public void dispose() {

  }

  private void handleInput() {
    if (Gdx.input.justTouched() && andyModel.isInitialized()) {

      int x = Gdx.input.getX();
      int y = Gdx.input.getY();

      item = andyModel.createInstance();
      if (item != null) {
        Vector3 pos = new Vector3(x, y, .9f);
        camera.unproject(pos);
        pos.z = .5f;
        item.transform.translate(pos);
        item.transform.rotate(0, 1, 0, 180);
        camera.lookAt(pos);
      }
    }
  }
}
