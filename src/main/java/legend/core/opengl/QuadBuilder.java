package legend.core.opengl;

import legend.core.gpu.Bpp;
import legend.game.types.Translucency;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;

import java.util.Arrays;

import static legend.core.opengl.TmdObjLoader.BPP_SIZE;
import static legend.core.opengl.TmdObjLoader.CLUT_SIZE;
import static legend.core.opengl.TmdObjLoader.COLOUR_SIZE;
import static legend.core.opengl.TmdObjLoader.FLAGS_SIZE;
import static legend.core.opengl.TmdObjLoader.NORM_SIZE;
import static legend.core.opengl.TmdObjLoader.POS_SIZE;
import static legend.core.opengl.TmdObjLoader.TPAGE_SIZE;
import static legend.core.opengl.TmdObjLoader.UV_SIZE;
import static org.lwjgl.opengl.GL11C.GL_TRIANGLE_STRIP;

public class QuadBuilder {
  private final String name;
  private final Vector3f pos = new Vector3f();
  private final Vector2f posSize = new Vector2f();
  private final Vector2f uv = new Vector2f();
  private final Vector2f uvSize = new Vector2f();
  private final Vector2i vramPos = new Vector2i();
  private final Vector2i clut = new Vector2i();
  private final Vector3f[] colour = {new Vector3f(), new Vector3f(), new Vector3f(), new Vector3f()};
  private Bpp bpp;
  private Translucency translucency;

  private int flags;

  public QuadBuilder(final String name) {
    this.name = name;
  }

  public QuadBuilder pos(final Vector3f pos) {
    this.pos.set(pos);
    return this;
  }

  public QuadBuilder pos(final float x, final float y, final float z) {
    this.pos.set(x, y, z);
    return this;
  }

  /** Sets both position and UV size */
  public QuadBuilder size(final Vector2f size) {
    this.posSize.set(size);
    this.uvSize.set(size);
    return this;
  }

  /** Sets both position and UV size */
  public QuadBuilder size(final float w, final float h) {
    this.posSize.set(w, h);
    this.uvSize.set(w, h);
    return this;
  }

  public QuadBuilder posSize(final Vector2f size) {
    this.posSize.set(size);
    return this;
  }

  public QuadBuilder posSize(final float w, final float h) {
    this.posSize.set(w, h);
    return this;
  }

  public QuadBuilder uvSize(final Vector2f size) {
    this.uvSize.set(size);
    return this;
  }

  public QuadBuilder uvSize(final float w, final float h) {
    this.uvSize.set(w, h);
    return this;
  }

  public QuadBuilder uv(final Vector2f uv) {
    this.uv.set(uv);
    this.flags |= TmdObjLoader.TEXTURED_FLAG;
    return this;
  }

  public QuadBuilder uv(final float u, final float v) {
    this.uv.set(u, v);
    this.flags |= TmdObjLoader.TEXTURED_FLAG;
    return this;
  }

  public QuadBuilder vramPos(final Vector2i vramPos) {
    this.vramPos.set(vramPos);
    this.flags |= TmdObjLoader.TEXTURED_FLAG;
    return this;
  }

  public QuadBuilder vramPos(final int x, final int y) {
    this.vramPos.set(x, y);
    this.flags |= TmdObjLoader.TEXTURED_FLAG;
    return this;
  }

  public QuadBuilder clut(final Vector2i clut) {
    this.clut.set(clut);
    this.flags |= TmdObjLoader.TEXTURED_FLAG;
    return this;
  }

  public QuadBuilder clut(final int x, final int y) {
    this.clut.set(x, y);
    this.flags |= TmdObjLoader.TEXTURED_FLAG;
    return this;
  }

  public QuadBuilder rgb(final Vector3f colour) {
    Arrays.fill(this.colour, colour);
    this.flags |= TmdObjLoader.COLOURED_FLAG;
    return this;
  }

  public QuadBuilder rgb(final int vertex, final Vector3f colour) {
    this.colour[vertex].set(colour);
    this.flags |= TmdObjLoader.COLOURED_FLAG;
    return this;
  }

  public QuadBuilder rgb(final float r, final float g, final float b) {
    for(int i = 0; i < this.colour.length; i++) {
      this.colour[i].set(r, g, b);
    }

    this.flags |= TmdObjLoader.COLOURED_FLAG;
    return this;
  }

  public QuadBuilder rgb(final int vertex, final float r, final float g, final float b) {
    this.colour[vertex].set(r, g, b);
    this.flags |= TmdObjLoader.COLOURED_FLAG;
    return this;
  }

  public QuadBuilder monochrome(final float shade) {
    for(int i = 0; i < this.colour.length; i++) {
      this.colour[i].set(shade);
    }

    this.flags |= TmdObjLoader.COLOURED_FLAG;
    return this;
  }

  public QuadBuilder monochrome(final int vertex, final float shade) {
    this.colour[vertex].set(shade);
    this.flags |= TmdObjLoader.COLOURED_FLAG;
    return this;
  }

  public QuadBuilder bpp(final Bpp bpp) {
    this.bpp = bpp;
    this.flags |= TmdObjLoader.TEXTURED_FLAG;
    return this;
  }

  public QuadBuilder translucency(final Translucency translucency) {
    this.translucency = translucency;
    this.flags |= TmdObjLoader.TRANSLUCENCY_FLAG << translucency.ordinal();
    return this;
  }

  public MeshObj build() {
    final float x0 = this.pos.x;
    final float y0 = this.pos.y;
    final float x1 = x0 + this.posSize.x;
    final float y1 = y0 + this.posSize.y;
    final float z = this.pos.z;
    final float u0 = this.uv.x;
    final float v0 = this.uv.y;
    final float u1 = u0 + this.uvSize.x;
    final float v1 = v0 + this.uvSize.y;
    final float tpx = this.vramPos.x;
    final float tpy = this.vramPos.y;
    final float clx = this.clut.x;
    final float cly = this.clut.y;
    final float bpp = this.bpp != null ? this.bpp.ordinal() : 0;

    // x y z nx ny nz u v tpx tpy clx cly bpp r g b m flags
    final float[] vertices = {
      x0, y0, z, 0, 0, 0, u0, v0, tpx, tpy, clx, cly, bpp, this.colour[0].x, this.colour[0].y, this.colour[0].z, 0, this.flags,
      x0, y1, z, 0, 0, 0, u0, v1, tpx, tpy, clx, cly, bpp, this.colour[1].x, this.colour[1].y, this.colour[1].z, 0, this.flags,
      x1, y0, z, 0, 0, 0, u1, v0, tpx, tpy, clx, cly, bpp, this.colour[2].x, this.colour[2].y, this.colour[2].z, 0, this.flags,
      x1, y1, z, 0, 0, 0, u1, v1, tpx, tpy, clx, cly, bpp, this.colour[3].x, this.colour[3].y, this.colour[3].z, 0, this.flags,
    };

    final Mesh mesh = new Mesh(GL_TRIANGLE_STRIP, vertices, 4);

    int vertexSize = POS_SIZE;
    vertexSize += NORM_SIZE;
    vertexSize += UV_SIZE + TPAGE_SIZE + CLUT_SIZE + BPP_SIZE;
    vertexSize += COLOUR_SIZE;
    vertexSize += FLAGS_SIZE;

    mesh.attribute(0, 0L, 3, vertexSize);

    int meshIndex = 1;
    int meshOffset = 3;

    mesh.attribute(meshIndex, meshOffset, NORM_SIZE, vertexSize);
    meshIndex++;
    meshOffset += NORM_SIZE;

    mesh.attribute(meshIndex, meshOffset, UV_SIZE, vertexSize);
    meshIndex++;
    meshOffset += UV_SIZE;

    mesh.attribute(meshIndex, meshOffset, TPAGE_SIZE, vertexSize);
    meshIndex++;
    meshOffset += TPAGE_SIZE;

    mesh.attribute(meshIndex, meshOffset, CLUT_SIZE, vertexSize);
    meshIndex++;
    meshOffset += CLUT_SIZE;

    mesh.attribute(meshIndex, meshOffset, BPP_SIZE, vertexSize);
    meshIndex++;
    meshOffset += BPP_SIZE;

    mesh.attribute(meshIndex, meshOffset, COLOUR_SIZE, vertexSize);
    meshIndex++;
    meshOffset += COLOUR_SIZE;

    mesh.attribute(meshIndex, meshOffset, FLAGS_SIZE, vertexSize);

    final Mesh[] meshes = new Mesh[Translucency.values().length + 1];

    if(this.translucency == null) {
      meshes[0] = mesh;
    } else {
      meshes[this.translucency.ordinal() + 1] = mesh;
    }

    return new MeshObj(this.name, meshes);
  }
}
