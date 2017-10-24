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

import android.support.annotation.NonNull;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g3d.Attribute;
import com.badlogic.gdx.graphics.g3d.Attributes;
import com.badlogic.gdx.graphics.g3d.Material;
import com.badlogic.gdx.graphics.g3d.Renderable;
import com.badlogic.gdx.graphics.g3d.Shader;
import com.badlogic.gdx.graphics.g3d.attributes.BlendingAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.ColorAttribute;
import com.badlogic.gdx.graphics.g3d.attributes.TextureAttribute;
import com.badlogic.gdx.graphics.g3d.shaders.BaseShader;
import com.badlogic.gdx.graphics.g3d.shaders.DefaultShader;

/**
 * Material for rendering the detected planes. This is an example of a material using a custom
 * shader.
 */
public class PlaneMaterial extends Material {

  // Id prefix used to detect this is a Plane material and it should be rendered using the Plane
  // shader.
  public static final String MATERIAL_ID_PREFIX = "planeMat";

  private static final String VERTEX_SHADER_CODE =
      "uniform mat4 u_worldTrans;\n"
          + // aka u_Model
          "uniform mat4 u_projViewTrans;\n"
          + // aka  u_ModelViewProjection
          "uniform mat2 u_PlaneUvMatrix;\n"
          + "\n"
          + "attribute vec3 a_position;\n"
          + "\n"
          + "varying vec3 v_TexCoordAlpha;\n"
          + "\n"
          + "void main() {\n"
          + "   vec4 position = vec4(a_position.x, 0.0, a_position.y,  1.0);\n"
          + " vec4 pos = u_worldTrans * position ;\n"
          + "   v_TexCoordAlpha = vec3(u_PlaneUvMatrix * pos.xz, a_position.z);\n"
          + "  gl_Position = u_projViewTrans * pos;\n"
          + "}";

  private static final String FRAGMENT_SHADER_CODE =
      "precision highp float;\n"
          + "uniform sampler2D u_diffuseTexture;\n"
          + "uniform vec4 u_dotColor;\n"
          + "uniform vec4 u_lineColor;\n"
          + "// dotThreshold, lineThreshold, lineFadeShrink, occlusionShrink\n"
          + "uniform vec4 u_gridControl;\n"
          + "varying vec3 v_TexCoordAlpha;\n"
          + "\n"
          + "void main() {\n"
          + "  vec4 control = texture2D(u_diffuseTexture, v_TexCoordAlpha.xy);\n"
          + "  float dotScale = v_TexCoordAlpha.z;\n"
          + "  float lineFade =\n"
          + "    max(0.0, u_gridControl.z * v_TexCoordAlpha.z - (u_gridControl.z - 1.0));\n"
          + "  vec3 color = (control.r * dotScale > u_gridControl.x) ? u_dotColor.rgb\n"
          + "             : (control.g > u_gridControl.y) ? u_lineColor.rgb * lineFade\n"
          + "                                             : (u_lineColor.rgb * 0.25 * lineFade) ;\n"
          + "  gl_FragColor = vec4(color, v_TexCoordAlpha.z * u_gridControl.w);\n"
          + "}\n";

  private static final float DOTS_PER_METER = 10.0f;
  private static final float EQUILATERAL_TRIANGLE_SCALE = (float) (1 / Math.sqrt(3));

  private static Color[] COLORS = {
    Color.BLACK,
    Color.CHARTREUSE,
    Color.CORAL,
    Color.CYAN,
    Color.BLUE,
    Color.FIREBRICK,
    Color.MAROON,
    Color.BROWN,
    Color.GOLDENROD,
    Color.PURPLE
  };

  private static Texture gridTexture;

  public PlaneMaterial(int index) {
    if (gridTexture == null) {
      gridTexture = new Texture("textures/trigrid.png");
      gridTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
    }
    set(TextureAttribute.createDiffuse(gridTexture));
    id = MATERIAL_ID_PREFIX + index;
    set(new BlendingAttribute(true, GL20.GL_DST_COLOR, GL20.GL_ONE_MINUS_SRC_ALPHA, 1f));
    int colornum = index;
    // Custom shader uniform values.
    set(PlaneShaderAttributes.createDotColor(COLORS[colornum % COLORS.length]));
    set(PlaneShaderAttributes.createLineColor(COLORS[(colornum + 1) % COLORS.length]));
    set(PlaneShaderAttributes.createIndexAttribute(index));

    // Not really a color, but controls how to draw/fade the grid.
    Color gridControl = new Color(0.2f, 0.4f, 2.0f, 1.5f);
    set(PlaneShaderAttributes.createGridControl(gridControl));
  }

  public static Shader getShader(Renderable renderable) {

    DefaultShader planeShader;
    DefaultShader.Config config = new DefaultShader.Config(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE);

    // Register the custom uniform attributes.  These are set up by the renderer at the right time.
    planeShader = new DefaultShader(renderable, config);
    planeShader.register(
        PlaneShaderAttributes.DotColorAlias, PlaneShaderAttributes.DotUniformSetter);
    planeShader.register(
        PlaneShaderAttributes.LineColorAlias, PlaneShaderAttributes.LineUniformSetter);
    planeShader.register(
        PlaneShaderAttributes.GridControlAlias, PlaneShaderAttributes.GridControlUniformSetter);
    planeShader.register(
        PlaneShaderAttributes.PlaneUvMatrixAlias, PlaneShaderAttributes.PlaneUvMatrixUniformSetter);
    planeShader.register(
        PlaneShaderAttributes.IndexAlias, PlaneShaderAttributes.PlaneUvMatrixUniformSetter);

    return planeShader;
  }

  /**
   * Attributes and uniform values used by the Plane shader. This class is used to register them
   * with the GDX renderer. The values for the attributes are set on the material of the renderable,
   * or in the rendercontext used when rendering. Each attribute also has a setter method that is
   * used to retrieve the attribute value and set it in the shader object at the correct location.
   */
  static class PlaneShaderAttributes extends Attribute {

    private static final String DotColorAlias = "u_dotColor";
    private static final long DotColorType = register(DotColorAlias);
    private static final String LineColorAlias = "u_lineColor";
    private static final long LineColorType = register(LineColorAlias);
    private static final String GridControlAlias = "u_gridControl";
    private static final long GridControlType = register(GridControlAlias);
    public static final String PlaneUvMatrixAlias = "u_PlaneUvMatrix";
    private static final long PlaneUvMatrixType = register(PlaneUvMatrixAlias);
    private static final String IndexAlias = "u_index;";
    private static final long IndexType = register(IndexAlias);

    public static BaseShader.Setter DotUniformSetter =
        new BaseShader.Setter() {
          @Override
          public boolean isGlobal(BaseShader shader, int inputID) {
            return false;
          }

          @Override
          public void set(
              BaseShader shader,
              int inputID,
              Renderable renderable,
              Attributes combinedAttributes) {
            float vec[] = new float[4];
            Color c = ((PlaneShaderAttributes) combinedAttributes.get(DotColorType)).color;
            vec[0] = c.r;
            vec[1] = c.g;
            vec[2] = c.b;
            vec[3] = c.a;
            shader.program.setUniform4fv(shader.loc(inputID), vec, 0, 4);
          }
        };
    public static BaseShader.Setter LineUniformSetter =
        new BaseShader.Setter() {
          @Override
          public boolean isGlobal(BaseShader shader, int inputID) {
            return false;
          }

          @Override
          public void set(
              BaseShader shader,
              int inputID,
              Renderable renderable,
              Attributes combinedAttributes) {
            float vec[] = new float[4];
            Color c = ((PlaneShaderAttributes) combinedAttributes.get(LineColorType)).color;
            vec[0] = c.r;
            vec[1] = c.g;
            vec[2] = c.b;
            vec[3] = c.a;
            shader.program.setUniform4fv(shader.loc(inputID), vec, 0, 4);
          }
        };
    public static BaseShader.Setter GridControlUniformSetter =
        new BaseShader.Setter() {
          @Override
          public boolean isGlobal(BaseShader shader, int inputID) {
            return false;
          }

          @Override
          public void set(
              BaseShader shader,
              int inputID,
              Renderable renderable,
              Attributes combinedAttributes) {
            float vec[] = new float[4];
            Color c = ((PlaneShaderAttributes) combinedAttributes.get(GridControlType)).color;
            vec[0] = c.r;
            vec[1] = c.g;
            vec[2] = c.b;
            vec[3] = c.a;
            shader.program.setUniform4fv(shader.loc(inputID), vec, 0, 4);
          }
        };
    public static BaseShader.Setter PlaneUvMatrixUniformSetter =
        new BaseShader.Setter() {
          @Override
          public boolean isGlobal(BaseShader shader, int inputID) {
            return false;
          }

          @Override
          public void set(
              BaseShader shader,
              int inputID,
              Renderable renderable,
              Attributes combinedAttributes) {
            float vec[] = new float[4];
            float uScale = DOTS_PER_METER;
            float vScale = DOTS_PER_METER * EQUILATERAL_TRIANGLE_SCALE;
            int index = (int) ((PlaneShaderAttributes) combinedAttributes.get(IndexType)).color.r;
            float angleRadians = index * 0.144f;

            vec[0] = +(float) Math.cos(angleRadians) * uScale;
            vec[1] = -(float) Math.sin(angleRadians) * uScale;
            vec[2] = +(float) Math.sin(angleRadians) * vScale;
            vec[3] = +(float) Math.cos(angleRadians) * vScale;

            Gdx.gl.glUniformMatrix2fv(shader.loc(inputID), 1, false, vec, 0);
          }
        };

    // This attribute type has only one value, a color.
    public final Color color;

    public PlaneShaderAttributes(long type, Color color) {
      super(type);
      this.color = color;
    }

    static PlaneShaderAttributes createDotColor(Color color) {
      return new PlaneShaderAttributes(DotColorType, color);
    }

    static PlaneShaderAttributes createGridControl(Color color) {
      return new PlaneShaderAttributes(GridControlType, color);
    }

    static PlaneShaderAttributes createLineColor(Color color) {
      return new PlaneShaderAttributes(LineColorType, color);
    }

    // Store the index in the red component of the color..
    static PlaneShaderAttributes createIndexAttribute(int index) {
      return new PlaneShaderAttributes(IndexType, new Color(index, 0, 0, 0));
    }

    @Override
    public Attribute copy() {
      return new PlaneShaderAttributes(type, color);
    }

    @Override
    public int compareTo(@NonNull Attribute o) {
      if (type != o.type) {
        return (int) (type - o.type);
      }
      return ((ColorAttribute) o).color.toIntBits() - color.toIntBits();
    }
  }
}
