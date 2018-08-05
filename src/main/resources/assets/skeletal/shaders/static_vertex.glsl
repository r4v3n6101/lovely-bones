#version 330
layout(location = 0) in vec3 position;
layout(location = 1) in vec2 texcoord;
layout(location = 2) in vec3 normal;
out vec2 out_texcoord;

layout(std140) uniform Matrices {
    mat4 projection;
    mat4 modelview;
};
uniform mat4 model;

void main(void) {
    vec4 outPos =  projection * modelview * model * vec4(position, 1.0);
    out_texcoord = texcoord;
    gl_Position = outPos;
}