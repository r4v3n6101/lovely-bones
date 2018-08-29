#version 330
#define MAX_BONES {MAX_BONES}
#define BONES 4

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

void main(void) {
    mat2x4 blendedDQ = mat2x4(0);
    for (int i = 0; i < BONES; i++) {
        mat2x4 dq = transforms[int(indices[i])];
        float w = weights[i];

        if (dot(blendedDQ[0], dq[0]) < 0.0) w *= -1.0;
        blendedDQ += dq * w;
    }
    blendedDQ /= length(blendedDQ[0]);

    passed_texcoord = texcoord;
    passed_normal = normalize(inverseTransposeModel * rotatePoint(normal, blendedDQ[0]));

    vec3 outPos = transformPoint(position, blendedDQ[0], blendedDQ[1]);
    gl_Position = projection * modelview * model * vec4(outPos, 1.0);
}

vec3 transformPoint(vec3 point, vec4 real, vec4 dual) {
    return rotatePoint(point, real) + 2 * (real.w * dual.xyz - dual.w * real.xyz + cross(dual.xyz, real.xyz));
}

vec3 rotatePoint(vec3 point, vec4 real) {
    return point + 2 * cross(real.w * point + cross(point, real.xyz), real.xyz);
}