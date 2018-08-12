#version 330
#define MAX_BONES {MAX_BONES}

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texcoord;
layout(location = 2) in vec3 normal;
layout(location = 4) in uvec4 indices;
layout(location = 5) in uvec4 weights;

out vec2 passed_texcoord;
out vec3 passed_normal;

layout(std140) uniform Matrices {
    mat4 projection;
    mat4 modelview;
};
uniform mat4 model = mat4(1.0);
uniform mat3 inverseTransposeModel = mat3(1.0);

uniform vec4 transforms[MAX_BONES * 2]; // DualQuat require two vec4

struct DualQuat {
  vec4 real;
  vec4 dual;
};

vec3 rotatePoint(vec3 point, vec4 real);
vec3 transformPoint(vec3 point, vec4 real, vec4 dual);
DualQuat getBlendedDualQuat(uvec4 indices, vec4 weights);

void main(void) {
    DualQuat blendedDQ = getBlendedDualQuat(
        indices,
        vec4(
            float(weights.x) / 255.0,
            float(weights.y) / 255.0,
            float(weights.z) / 255.0,
            float(weights.w) / 255.0
        )
    );

    passed_texcoord = texcoord;
    passed_normal = inverseTransposeModel * rotatePoint(normal, blendedDQ.real);

    vec3 outPos = transformPoint(position, blendedDQ.real, blendedDQ.dual);
    gl_Position = projection * modelview * model * vec4(outPos, 1.0);
}

vec3 transformPoint(vec3 point, vec4 real, vec4 dual) {
    return rotatePoint(point, real) + 2 * (real.w * dual.xyz - dual.w * real.xyz + cross(dual.xyz, real.xyz));
}

vec3 rotatePoint(vec3 point, vec4 real) {
    return point + 2 * cross(real.w * point + cross(point, real.xyz), real.xyz);
}

DualQuat getBlendedDualQuat(uvec4 indices, vec4 weights) {
    vec4 real0 = transforms[2u * indices.x];
    vec4 real1 = transforms[2u * indices.y];
    vec4 real2 = transforms[2u * indices.z];
    vec4 real3 = transforms[2u * indices.w];

    vec4 dual0 = transforms[2u * indices.x + 1u];
    vec4 dual1 = transforms[2u * indices.y + 1u];
    vec4 dual2 = transforms[2u * indices.z + 1u];
    vec4 dual3 = transforms[2u * indices.w + 1u];

    if (dot(real0, real1) < 0.0) weights.y *= -1.0;
    if (dot(real0, real2) < 0.0) weights.z *= -1.0;
    if (dot(real0, real3) < 0.0) weights.w *= -1.0;

    vec4 blendedReal = real0 * weights.x + real1 * weights.y + real2 * weights.z + real3 * weights.w;
    vec4 blendedDual = dual0 * weights.x + dual1 * weights.y + dual2 * weights.z + dual3 * weights.w;

    float norm = length(blendedReal);
    return DualQuat(blendedReal / norm, blendedDual / norm);
}