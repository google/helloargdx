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

import android.support.design.widget.Snackbar;
import android.util.Log;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.g3d.ModelBatch;
import com.badlogic.gdx.graphics.g3d.ModelInstance;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.utils.ShaderProvider;
import com.badlogic.gdx.math.Quaternion;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.google.ar.core.Anchor;
import com.google.ar.core.Frame;
import com.google.ar.core.HitResult;
import com.google.ar.core.Plane;
import com.google.ar.core.Pose;
import com.google.ar.core.TrackingState;
import com.google.ar.core.exceptions.NotTrackingException;
import com.github.claywilkinson.arcore.gdx.ARCoreScene;
import com.github.claywilkinson.arcore.gdx.PlaneAttachment;
import com.github.claywilkinson.arcore.gdx.SimpleShaderProvider;
import java.util.HashMap;

/**
 * Hello scene to introduce ARCore. This scene has 3 major components: 1. Makes Android UI calls
 * (using the Snackbar class) to demonstrate how to use Android SDK. 2. Detects planes and renders
 * them using a custom shader and material. 3. When a plane is tapped, an Android model is drawn.
 * Demonstrating loading multiple models and then combining them into a single model for ease of
 * use.
 */
public class HelloScene extends ARCoreScene {

  // Snackbar for showing an initial message to the user.  Once a plane is detected, the snackbar
  // is dismissed.
  private Snackbar mLoadingMessageSnackbar = null;
  private AssetManager assetManager = new AssetManager();
  private AndyModel andyModel;

  // Keep the objects in the scene mapped by the anchor id.
  private HashMap<Anchor, PlaneAttachment<ModelInstance>> instances = new HashMap<>();

  @Override
  public void create() {
    super.create();
    // Start loading the andy model.
    andyModel = new AndyModel(assetManager);
  }

  /** Create a new shader provider that is aware of the Plane material custom shader. */
  @Override
  protected ShaderProvider createShaderProvider() {
    return new SimpleShaderProvider() {
      @Override
      protected Shader createShader(Renderable renderable) {
        if (renderable.material.id.startsWith(PlaneMaterial.MATERIAL_ID_PREFIX)) {
          return PlaneMaterial.getShader(renderable);
        } else {
          return super.createShader(renderable);
        }
      }
    };
  }

  /**
   * This is the main render method. It is called on each frame. This is where all scene operations
   * need to be. This includes interacting with the ARCore frame for hit tests, plane detection
   * updates and anchor updates.
   *
   * <p>It also is where application specific objects are created and ultimately rendered.
   */
  @Override
  protected void render(Frame frame, ModelBatch modelBatch) {
    // Let the asset manager work asynchronously.
    assetManager.update();
    if (!andyModel.isInitialized()) {
      andyModel.initialize(assetManager);
    }

    // If we're still loading/detecting planes, just return.
    if (!handleLoadingMessage(frame)) {
      return;
    }

    // Draw all the planes detected
    drawPlanes(modelBatch);

    // Handle taps to create androids.
    handleInput(frame);

    for(Anchor anchor : frame.getUpdatedAnchors()) {
      PlaneAttachment<ModelInstance> item = instances.get(anchor);
      if (item != null) {
        ModelInstance m = item.getData();
        Pose p = item.getPose();
        Vector3 pos = new Vector3(p.tx(),p.ty(), p.tz());
        Quaternion rot = new Quaternion(p.qx(), p.qy(), p.qz(), p.qw());
        m.transform.set(pos,rot);
      }
    }

    // Finally, render all the object instances.
    Array<ModelInstance> models = new Array<>(instances.size());
    for (PlaneAttachment<ModelInstance> p : instances.values()) {
      models.add(p.getData());
    }
    modelBatch.render(models);
  }

  /**
   * Handles the touch input. This gets the screen X,Y position of the touch and then performs a
   * Hittest vs. the planes detected. If the hit is within a plane, an instance of the Andy model is
   * created.
   */
  private void handleInput(Frame frame) {
    if (Gdx.input.justTouched()) {

      int x = Gdx.input.getX();
      int y = Gdx.input.getY();

        for (HitResult hit : frame.hitTest(x, y)) {
          // Check if any plane was hit, and if it was hit inside the plane polygon.
          if (hit.getTrackable() instanceof Plane  &&
                  ((Plane) hit.getTrackable()).isPoseInPolygon(hit.getHitPose())) {
            // Cap the number of objects created. This avoids overloading both the
            // rendering system and ARCore.
            if (instances.size() >= 16) {
              Anchor key = instances.keySet().iterator().next();
              instances.remove(key);
              key.detach();
            }
            // Adding an Anchor tells ARCore that it should track this position in
            // space. This anchor will be used in PlaneAttachment to place the 3d model
            // in the correct position relative both to the world and to the plane.
            try {
              ModelInstance item = andyModel.createInstance();
              if (item != null) {
                PlaneAttachment<ModelInstance> planeAttachment =
                    new PlaneAttachment<>(
                            (Plane)hit.getTrackable(),
                        getSession().createAnchor(hit.getHitPose()),
                        item);

                instances.put(planeAttachment.getAnchor(), planeAttachment);

                Pose p = planeAttachment.getPose();
                // position and rotate
                Vector3 pos =
                    new Vector3(p.tx(),p.ty(),p.tz());
                item.transform.translate(pos);
              }
            } catch (NotTrackingException e) {
              Log.w("HelloScene", "not tracking: " + e);
            }

            // Hits are sorted by depth. Consider only closest hit on a plane.
            break;
          }
        }
    }
  }

  /** Draws the planes detected. */
  private void drawPlanes(ModelBatch modelBatch) {
    Array<ModelInstance> planeInstances = new Array<>();
    int index = 0;
    for (Plane plane : getSession().getAllTrackables(Plane.class)) {

      // check for planes that are no longer valid
      if (plane.getSubsumedBy() != null
          || plane.getTrackingState() == TrackingState.STOPPED) {
        continue;
      }
      // New plane
      ModelInstance instance = new ModelInstance(PlaneModel.createPlane(plane, index++));
      instance.transform.setToTranslation(
          plane.getCenterPose().tx(), plane.getCenterPose().ty(), plane.getCenterPose().tz());
      planeInstances.add(instance);
    }
    modelBatch.render(planeInstances);
  }

  /**
   * Handles showing the loading message, then hiding it once a plane is detected.
   *
   * @param frame - the ARCore frame.
   * @return true once a plane is loaded.
   */
  private boolean handleLoadingMessage(Frame frame) {
    // If not tracking, don't draw 3d objects.
    if (frame.getCamera().getTrackingState() != TrackingState.TRACKING) {
      showLoadingMessage();
      return false;
    }
    // Check if we detected at least one plane. If so, hide the loading message.
    if (mLoadingMessageSnackbar != null) {
      for (Plane plane : getSession().getAllTrackables(Plane.class)) {
        if (plane.getType() == Plane.Type.HORIZONTAL_UPWARD_FACING
            && plane.getTrackingState() == TrackingState.TRACKING) {
          hideLoadingMessage();
        }
      }
    }
    return true;
  }

  /** Show the loading snackbar. */
  private void showLoadingMessage() {
    if (mLoadingMessageSnackbar != null) {
      return;
    }
    Gdx.app.postRunnable(
        new Runnable() {
          @Override
          public void run() {

            mLoadingMessageSnackbar =
                Snackbar.make(getView(), "Searching for surfaces...", Snackbar.LENGTH_INDEFINITE);
            mLoadingMessageSnackbar.getView().setBackgroundColor(0xbf323232);
            mLoadingMessageSnackbar.show();
          }
        });
  }

  /** Hide it. */
  private void hideLoadingMessage() {
    Gdx.app.postRunnable(
        new Runnable() {
          @Override
          public void run() {
            mLoadingMessageSnackbar.dismiss();
            mLoadingMessageSnackbar = null;
          }
        });
  }
}
