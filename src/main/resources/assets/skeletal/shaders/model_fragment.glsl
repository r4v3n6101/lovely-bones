#version 330
in vec2 passed_texcoord;
in vec3 passed_normal;

out vec4 color;

uniform sampler2D textureSampler;
uniform sampler2D lightmapSampler;
uniform vec2 lightmapTexcoord;

const vec3 LIGHT0_DIRECTION = vec3(0.16169, 0.808452, -0.565916);
const vec3 LIGHT1_DIRECTION = vec3(-0.16169, 0.808452, 0.565916);
const float DEFAULT_DIFFUSE = 0.6;

void main(void) {
    vec3 normal = passed_normal;
    float lambertian0 = max(dot(normal, LIGHT0_DIRECTION), 0.0);
    float lambertian1 = max(dot(normal, LIGHT1_DIRECTION), 0.0);
    vec3 diffuse = (lambertian0 + lambertian1) * texture(lightmapSampler, lightmapTexcoord).rgb;

    color = vec4(diffuse * texture(textureSampler, passed_texcoord).rgb, 1.0);
}