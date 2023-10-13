#version 330 core

smooth in vec2 vertUv;
flat in vec2 vertTpage;
flat in vec2 vertClut;
flat in float vertBpp;
smooth in vec4 vertColour;
flat in float vertFlags;

uniform vec3 recolour;
uniform sampler2D tex24;
uniform usampler2D tex15;

layout(location = 0) out vec4 accum;
layout(location = 1) out float reveal;

//TODO handle more flags

void main() {
  int flags = int(vertFlags);

  vec4 colour = vec4(1.0, 1.0, 1.0, 1.0);

  // Vertex colour
  if((flags & 0x4) != 0) {
    colour = vertColour;
  }

  // Textured
  if((flags & 0x2) != 0) {
    // Texture recolouring uses an RGB range of 0..128 or 0.0..0.5 so we multiply by 2
    if((flags & 0x4) != 0) {
      colour *= 2.0;
    }

    //NOTE: these only work for 4/8 bpp
    int widthDivisor = 1 << int(2 - vertBpp);
    int widthMask = widthDivisor - 1;
    int indexShift = int(vertBpp + 2);
    int indexMask = int(pow(16, vertBpp + 1) - 1);

    // Calculate CLUT index
    ivec2 uv = ivec2(vertTpage.x + vertUv.x / widthDivisor, vertTpage.y + vertUv.y);
    ivec4 indexVec = ivec4(texelFetch(tex15, uv, 0));
    int p = (indexVec.r >> ((int(vertTpage.x + vertUv.x) & widthMask) << indexShift)) & indexMask;
    ivec2 clutUv = ivec2(vertClut.x + p, vertClut.y);

    // Pull actual pixel colour from CLUT
    vec4 texColour = texelFetch(tex24, clutUv, 0);

    // Discard if (0, 0, 0)
    if(texColour.r == 0 && texColour.g == 0 && texColour.b == 0) {
      discard;
    }

    colour *= texColour;
  }

  colour.rgb *= recolour;

  if((flags & 0x8) != 0) { // (B+F)/2 translucency
    colour.a = 0.5;
  } else {
    colour.a = 1.0;
  }

  // weight function
  float weight = clamp(pow(min(1.0, colour.a * 10.0) + 0.01, 3.0) * 1e8 * pow(1.0 - gl_FragCoord.z * 0.9, 3.0), 1e-2, 3e3);

  // store pixel color accumulation
  accum = vec4(colour.rgb * colour.a, colour.a) * weight;

  // store pixel revealage threshold
  reveal = colour.a;
}
