#version 330
#define MAX_BONES {MAX_BONES}

layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texcoord;
layout(location = 2) in vec3 normal;
layout(location = 4) in vec4 indices;
layout(location = 5) in vec4 weights;

out vec2 passed_texcoord;
out vec3 passed_normal;

layout(std140) uniform Matrices {
    mat4 projection;
    mat4 modelview;
};
uniform mat4 model = mat4(1.0);
uniform mat3 inverseTransposeModel = mat3(1.0);

uniform mat2x4 transforms[MAX_BONES];

vec3 rotatePoint(vec3 point, vec4 real);
vec3 transformPoint(vec3 point, vec4 real, vec4 dual);
mat2x4 getBlendedDualQuat(vec4 indices, vec4 weights);

void main(void) {
    mat2x4 blendedDQ = getBlendedDualQuat(indices, weights);

    passed_texcoord = texcoord;
    passed_normal = inverseTransposeModel * rotatePoint(normal, blendedDQ[0]);

    vec3 outPos = transformPoint(position, blendedDQ[0], blendedDQ[1]);
    gl_Position = projection * modelview * model * vec4(outPos, 1.0);
}

vec3 transformPoint(vec3 point, vec4 real, vec4 dual) {
    return rotatePoint(point, real) + 2 * (real.w * dual.xyz - dual.w * real.xyz + cross(dual.xyz, real.xyz));
}

vec3 rotatePoint(vec3 point, vec4 real) {
    return point + 2 * cross(real.w * point + cross(point, real.xyz), real.xyz);
}

mat2x4 getBlendedDualQuat(vec4 indices, vec4 weights) {
    mat2x4 dq0 = transforms[int(indices.x)];
    mat2x4 dq1 = transforms[int(indices.y)];
    mat2x4 dq2 = transforms[int(indices.z)];
    mat2x4 dq3 = transforms[int(indices.w)];

    if (dot(dq0[0], dq1[0]) < 0.0) weights.y *= -1.0;
    if (dot(dq0[0], dq2[0]) < 0.0) weights.z *= -1.0;
    if (dot(dq0[0], dq3[0]) < 0.0) weights.w *= -1.0;

    mat2x4 blendedDQ = dq0 * weights.x + dq1 * weights.y + dq2 * weights.z + dq3 * weights.w;

    float norm = length(blendedDQ[0]);
    return blendedDQ / norm;
}