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

import android.opengl.GLES20;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.assets.loaders.resolvers.InternalFileHandleResolver;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.model.MeshPart;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.github.claywilkinson.arcore.gdx.ObjLoaderImproved;

/**
 * Model of Andy the Android. This includes loading 2 OBJ models and combining them into one model.
 * The assets are loaded using the internal file loader, so the path to the assets is relative to
 * src/main/assets in the source project.
 */
public class AndyModel {
  private Model model;

  private static final String ANDY_MODEL = "models/andy.obj";
  private static final String ANDY_TEXTURE = "models/andy.png";
  private static final String ANDY_SHADOW_MODEL = "models/andy_shadow.obj";
  private static final String ANDY_SHADOW_TEXTURE = "models/andy_shadow.png";

  /**
   * Create a new model. The asset manager is used to begin the asynchronous loading of the model
   * assets. To make sure the assets are loaded, the caller needs to add assetManager.update() to
   * the render() method.
   */
  public AndyModel(AssetManager assetManager) {
    assetManager.setLoader(
        Model.class, ".obj", new ObjLoaderImproved(new InternalFileHandleResolver()));
    ObjLoaderImproved.ObjLoaderParameters objLoaderParameters = new ObjLoaderImproved.ObjLoaderParameters();
    objLoaderParameters.flipV = true;
    assetManager.load(ANDY_MODEL, Model.class, objLoaderParameters);
    assetManager.load(ANDY_SHADOW_MODEL, Model.class, objLoaderParameters);
  }

  /**
   * Initializes the model. This needs to be called when the model assets are loaded into memory. If
   * they cannot be found yet, it is assumed that they are still loading.
   *
   * @return true when the model is initialized and ready to use.
   */
  public boolean initialize(AssetManager assetManager) {
    if (assetManager.isLoaded(ANDY_MODEL,Model.class) &&
            assetManager.isLoaded(ANDY_SHADOW_MODEL, Model.class)) {
      Model body = assetManager.get(ANDY_MODEL, Model.class);
      Model shadow = assetManager.get(ANDY_SHADOW_MODEL, Model.class);
      if (body != null && shadow != null) {

        Material bodyMaterial =
                new Material(TextureAttribute.createDiffuse(new Texture(ANDY_TEXTURE)));
        Material shadowMaterial =
                new Material(TextureAttribute.createDiffuse(new Texture(ANDY_SHADOW_TEXTURE)));
        shadowMaterial.set(
                new BlendingAttribute(true, GLES20.GL_ZERO, GLES20.GL_ONE_MINUS_SRC_ALPHA, 1f));

        ModelBuilder builder = new ModelBuilder();
        builder.begin();
        for (MeshPart part : body.meshParts) {
          builder.part(part, bodyMaterial);
        }
        for (MeshPart part : shadow.meshParts) {
          builder.part(part, shadowMaterial);
        }
        model = builder.end();
      }
    }
    return isInitialized();
  }

  public ModelInstance createInstance() {
    return new ModelInstance(model);
  }

  public boolean isInitialized() {
    return model != null;
  }
}
