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

import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.VertexAttributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Model;
import com.badlogic.gdx.graphics.g3d.utils.MeshPartBuilder;
import com.badlogic.gdx.graphics.g3d.utils.ModelBuilder;
import com.google.ar.core.Plane;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;

/**
 * Procedural model based on the bounding polygon from a Plane detected by ARCore. This creates a
 * two parts of the polygon, the outer boundary, and an inner boundary. The custom shader then fades
 * the alpha between the inner and outer polygons.
 */
class PlaneModel {
  private static final int BYTES_PER_FLOAT = Float.SIZE / 8;
  private static final int BYTES_PER_SHORT = Short.SIZE / 8;
  private static final int COORDS_PER_VERTEX = 3; // x, z, alpha

  private static final int VERTS_PER_BOUNDARY_VERT = 2;
  private static final int INDICES_PER_BOUNDARY_VERT = 3;
  private static final int INITIAL_BUFFER_BOUNDARY_VERTS = 64;

  private static final int INITIAL_VERTEX_BUFFER_SIZE_BYTES =
      BYTES_PER_FLOAT * COORDS_PER_VERTEX * VERTS_PER_BOUNDARY_VERT * INITIAL_BUFFER_BOUNDARY_VERTS;

  private static final int INITIAL_INDEX_BUFFER_SIZE_BYTES =
      BYTES_PER_SHORT
          * INDICES_PER_BOUNDARY_VERT
          * INDICES_PER_BOUNDARY_VERT
          * INITIAL_BUFFER_BOUNDARY_VERTS;
  private static final float FADE_RADIUS_M = 0.25f;

  public static Model createPlane(Plane plane, int index) {
    FloatBuffer boundary = plane.getPolygon();
    float extentX = plane.getExtentX();
    float extentZ = plane.getExtentZ();

    // Model builder is used to create mesh parts.
    ModelBuilder builder = new ModelBuilder();

    Material material = new PlaneMaterial(index);

    builder.begin();
    MeshPartBuilder meshPartBuilder =
        builder.part(
            "plane" + index, GL20.GL_TRIANGLE_STRIP, VertexAttributes.Usage.Position, material);

    meshPartBuilder.setUVRange(0, 0, 1f, 1f);
    FloatBuffer mVertexBuffer =
        ByteBuffer.allocateDirect(INITIAL_VERTEX_BUFFER_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer();
    ShortBuffer mIndexBuffer =
        ByteBuffer.allocateDirect(INITIAL_INDEX_BUFFER_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer();

    // Generate a new set of vertices and a corresponding triangle strip index set so that
    // the plane boundary polygon has a fading edge. This is done by making a copy of the
    // boundary polygon vertices and scaling it down around center to push it inwards. Then
    // the index buffer is setup accordingly.
    boundary.rewind();
    int boundaryVertices = boundary.limit() / 2;
    int numVertices;
    int numIndices;

    numVertices = boundaryVertices * VERTS_PER_BOUNDARY_VERT;
    // drawn as GL_TRIANGLE_STRIP with 3n-2 triangles (n-2 for fill, 2n for perimeter).
    numIndices = boundaryVertices * INDICES_PER_BOUNDARY_VERT;

    if (mVertexBuffer.capacity() < numVertices * COORDS_PER_VERTEX) {
      int size = mVertexBuffer.capacity();
      while (size < numVertices * COORDS_PER_VERTEX) {
        size *= 2;
      }
      mVertexBuffer =
          ByteBuffer.allocateDirect(BYTES_PER_FLOAT * size)
              .order(ByteOrder.nativeOrder())
              .asFloatBuffer();
    }
    mVertexBuffer.rewind();
    mVertexBuffer.limit(numVertices * COORDS_PER_VERTEX);

    if (mIndexBuffer.capacity() < numIndices) {
      int size = mIndexBuffer.capacity();
      while (size < numIndices) {
        size *= 2;
      }
      mIndexBuffer =
          ByteBuffer.allocateDirect(BYTES_PER_SHORT * size)
              .order(ByteOrder.nativeOrder())
              .asShortBuffer();
    }
    mIndexBuffer.rewind();
    mIndexBuffer.limit(numIndices);

    // Note: when either dimension of the bounding box is smaller than 2*FADE_RADIUS_M we
    // generate a bunch of 0-area triangles.  These don't get rendered though so it works
    // out ok.
    float xScale = Math.max((extentX - 2 * FADE_RADIUS_M) / extentX, 0.0f);
    float zScale = Math.max((extentZ - 2 * FADE_RADIUS_M) / extentZ, 0.0f);

    while (boundary.hasRemaining()) {
      float x = boundary.get();
      float z = boundary.get();
      // Each vertex has the X and Z value (Z is stored in the "Y" position) and the alpha for the
      // the vertex is in the "Z" value.  The outer polygon has an alpha of 0; the inner a value of
      // 1.
      mVertexBuffer.put(x);
      mVertexBuffer.put(z);
      mVertexBuffer.put(0.0f);
      mVertexBuffer.put(x * xScale);
      mVertexBuffer.put(z * zScale);
      mVertexBuffer.put(1.0f);
    }

    // step 1, perimeter
    mIndexBuffer.put((short) ((boundaryVertices - 1) * 2));
    for (int i = 0; i < boundaryVertices; ++i) {
      mIndexBuffer.put((short) (i * 2));
      mIndexBuffer.put((short) (i * 2 + 1));
    }
    mIndexBuffer.put((short) 1);
    // This leaves us on the interior edge of the perimeter between the inset vertices
    // for boundary verts n-1 and 0.

    // step 2, interior:
    for (int i = 1; i < boundaryVertices / 2; ++i) {
      mIndexBuffer.put((short) ((boundaryVertices - 1 - i) * 2 + 1));
      mIndexBuffer.put((short) (i * 2 + 1));
    }
    if (boundaryVertices % 2 != 0) {
      mIndexBuffer.put((short) ((boundaryVertices / 2) * 2 + 1));
    }

    mVertexBuffer.rewind();
    mIndexBuffer.rewind();

    float v[] = new float[mVertexBuffer.limit()];
    mVertexBuffer.get(v);
    short ind[] = new short[mIndexBuffer.limit()];
    mIndexBuffer.get(ind);
    meshPartBuilder.addMesh(v, ind, 0, ind.length);

    Model model = builder.end();

    return model;
  }
}
