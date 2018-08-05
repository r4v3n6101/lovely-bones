#version 330
in vec2 out_texcoord;
out vec4 color;

uniform sampler2D sampler;

void main(void) {
    color = texture(sampler, out_texcoord);
}