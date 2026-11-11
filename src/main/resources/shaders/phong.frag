#version 330 core
out vec4 FragColor;

in vec3 FragPos;
in vec3 Normal;
in vec2 TexCoords;
in vec4 FragPosLightSpace;

uniform vec3 lightPos;
uniform vec3 viewPos;
uniform vec3 lightColor;
uniform vec3 objectColor;
uniform sampler2D textureSampler;
uniform sampler2D shadowMap;
uniform bool useTexture;
uniform bool useBlinnPhong;
uniform bool useShadow;

// Shadow Mapping: bandingkan depth fragment (sudut pandang cahaya) dengan Shadow Map + PCF 3x3
float shadowFactor(vec4 fragPosLS, vec3 norm, vec3 lightDir) {
    vec3 proj = fragPosLS.xyz / fragPosLS.w;
    proj = proj * 0.5 + 0.5;                       // NDC [-1,1] -> [0,1]
    if (proj.z > 1.0) return 0.0;                  // di luar far plane cahaya
    float bias = max(0.05 * (1.0 - dot(norm, lightDir)), 0.005); // slope-scaled bias (anti Shadow Acne)
    vec2 texel = 1.0 / vec2(textureSize(shadowMap, 0));
    float shadow = 0.0;
    for (int x = -1; x <= 1; ++x) {
        for (int y = -1; y <= 1; ++y) {
            float d = texture(shadowMap, proj.xy + vec2(x, y) * texel).r;
            shadow += (proj.z - bias > d) ? 1.0 : 0.0;
        }
    }
    return shadow / 9.0;
}

void main() {
    // 1. Ambient
    float ambientStrength = 0.2;
    vec3 ambient = ambientStrength * lightColor;

    // 2. Diffuse (Lambert)
    vec3 norm = normalize(Normal);
    vec3 lightDir = normalize(lightPos - FragPos);
    float diff = max(dot(norm, lightDir), 0.0);
    vec3 diffuse = diff * lightColor;

    // 3. Specular (Phong vs Blinn-Phong)
    float specularStrength = 0.8;
    vec3 viewDir = normalize(viewPos - FragPos);
    float spec = 0.0;
    if (useBlinnPhong) {
        vec3 halfwayDir = normalize(lightDir + viewDir);
        spec = pow(max(dot(norm, halfwayDir), 0.0), 32.0);
    } else {
        vec3 reflectDir = reflect(-lightDir, norm);
        spec = pow(max(dot(viewDir, reflectDir), 0.0), 16.0);
    }
    vec3 specular = specularStrength * spec * lightColor;

    // 4. Shadow (faktor 1.0 = terang, 0.2 = bayangan)
    float shadow = useShadow ? shadowFactor(FragPosLightSpace, norm, lightDir) : 0.0;
    float lit = mix(1.0, 0.2, shadow);

    vec3 baseColor = useTexture ? texture(textureSampler, TexCoords).rgb * objectColor : objectColor;
    vec3 result = (ambient + lit * (diffuse + specular)) * baseColor;
    FragColor = vec4(result, 1.0);
}
