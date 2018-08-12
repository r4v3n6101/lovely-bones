#version 330
layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texcoord;
layout(location = 2) in vec3 normal;
out vec2 passed_texcoord;
out vec3 passed_normal;

layout(std140) uniform Matrices {
    mat4 projection;
    mat4 modelview;
};
uniform mat4 model = mat4(1.0);
uniform mat3 inverseTransposeModel = mat3(1.0);

void main(void) {
    passed_texcoord = texcoord;
    passed_normal = inverseTransposeModel * normal;
    gl_Position = projection * modelview * model * vec4(position, 1.0);
}