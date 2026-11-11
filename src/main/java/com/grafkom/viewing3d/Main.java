package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import org.lwjgl.system.MemoryStack;

import java.nio.IntBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * 3D Visual Realism Suite (Minggu 11).
 *
 * KONTROL:
 *   B = Phong <-> Blinn-Phong         (Indikator 1)
 *   F = Flat  <-> Smooth shading      (Indikator 2)
 *   Z = Z-Buffer on/off, C = Back-Face Culling on/off  (Indikator 3)
 *   T = Tekstur on/off, N = Nearest <-> Bilinear+Mipmap (Indikator 4)
 *   H = Shadow Mapping on/off         (Indikator 5)
 *   SPACE = auto-rotate, V = VSync (untuk ukur FPS)
 *   Panah = putar kamera, W/S = zoom, ESC = keluar
 */
public class Main {
    private long window;
    private int fbWidth = 1280, fbHeight = 720;

    private boolean blinn = true, smooth = true, depthTest = true, cullFace = true;
    private boolean useTexture = true, mipmapped = true, shadows = true, spin = true, vsync = true;

    private float yaw = 30f, pitch = 25f, distance = 12f, angle = 0f;

    private ShaderProgram sceneShader, depthShader;
    private ShadowMapFBO shadowFBO;
    private Texture2D texture;
    private Mesh sphere, torus, ground;

    private final Vector3f lightPos = new Vector3f(-4f, 7f, 5f);
    private final Vector3f lightColor = new Vector3f(1f, 1f, 1f);

    private final Matrix4f sphereModel = new Matrix4f();
    private final Matrix4f torusModel = new Matrix4f();
    private final Matrix4f groundModel = new Matrix4f();

    public static void main(String[] args) { new Main().run(); }

    private void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();
        if (!glfwInit()) throw new IllegalStateException("Gagal inisialisasi GLFW");

        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(1280, 720, "3D Visual Realism Suite", NULL, NULL);
        if (window == NULL) throw new RuntimeException("Gagal membuat window");

        glfwSetKeyCallback(window, (w, key, sc, action, mods) -> {
            if (action == GLFW_PRESS) onKey(key);
        });
        glfwSetFramebufferSizeCallback(window, (w, wd, ht) -> { fbWidth = wd; fbHeight = ht; });

        glfwMakeContextCurrent(window);
        glfwSwapInterval(1);
        glfwShowWindow(window);
        GL.createCapabilities();

        try (MemoryStack s = MemoryStack.stackPush()) {
            IntBuffer w = s.mallocInt(1), h = s.mallocInt(1);
            glfwGetFramebufferSize(window, w, h);
            fbWidth = w.get(0);
            fbHeight = h.get(0);
        }

        sceneShader = new ShaderProgram("/shaders/phong.vert", "/shaders/phong.frag");
        depthShader = new ShaderProgram("/shaders/depth.vert", "/shaders/depth.frag");
        shadowFBO = new ShadowMapFBO();
        texture = new Texture2D("textures/texture.png"); // fallback: checkerboard

        sphere = new Mesh(Primitives.sphere(1.2f, 16, 24));      // low-poly agar beda flat/smooth jelas
        torus = new Mesh(Primitives.torus(1.2f, 0.45f, 32, 16));
        ground = new Mesh(Primitives.plane(8f, 4f));

        System.out.println("B=Phong/Blinn  F=Flat/Smooth  Z=ZBuffer  C=Culling  T=Texture  N=Filter  H=Shadow  SPACE=Spin  V=VSync");
    }

    private void onKey(int key) {
        switch (key) {
            case GLFW_KEY_B: blinn = !blinn; break;
            case GLFW_KEY_F: smooth = !smooth; break;
            case GLFW_KEY_Z: depthTest = !depthTest; break;
            case GLFW_KEY_C: cullFace = !cullFace; break;
            case GLFW_KEY_T: useTexture = !useTexture; break;
            case GLFW_KEY_N: mipmapped = !mipmapped; texture.setMipmapped(mipmapped); break;
            case GLFW_KEY_H: shadows = !shadows; break;
            case GLFW_KEY_SPACE: spin = !spin; break;
            case GLFW_KEY_V: vsync = !vsync; glfwSwapInterval(vsync ? 1 : 0); break;
            case GLFW_KEY_ESCAPE: glfwSetWindowShouldClose(window, true); break;
            default: break;
        }
    }

    private void loop() {
        double last = glfwGetTime(), fpsTimer = last;
        int frames = 0;

        while (!glfwWindowShouldClose(window)) {
            double now = glfwGetTime();
            float dt = (float) (now - last);
            last = now;

            handleHeldKeys(dt);
            if (spin) angle += 30f * dt;

            render();

            glfwSwapBuffers(window);
            glfwPollEvents();

            frames++;
            if (now - fpsTimer >= 0.5) {
                updateTitle(frames / (now - fpsTimer));
                frames = 0;
                fpsTimer = now;
            }
        }
    }

    private void handleHeldKeys(float dt) {
        if (glfwGetKey(window, GLFW_KEY_LEFT) == GLFW_PRESS)  yaw -= 70f * dt;
        if (glfwGetKey(window, GLFW_KEY_RIGHT) == GLFW_PRESS) yaw += 70f * dt;
        if (glfwGetKey(window, GLFW_KEY_UP) == GLFW_PRESS)    pitch = Math.min(85f, pitch + 50f * dt);
        if (glfwGetKey(window, GLFW_KEY_DOWN) == GLFW_PRESS)  pitch = Math.max(5f, pitch - 50f * dt);
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS)     distance = Math.max(4f, distance - 8f * dt);
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS)     distance = Math.min(30f, distance + 8f * dt);
    }

    private void updateTitle(double fps) {
        glfwSetWindowTitle(window, String.format(
                "Visual Realism | %s | %s | Z-Buffer:%s Cull:%s | Tex:%s (%s) | Shadow:%s | VSync:%s | FPS:%.0f",
                blinn ? "Blinn-Phong" : "Phong",
                smooth ? "Smooth" : "Flat",
                depthTest ? "ON" : "OFF", cullFace ? "ON" : "OFF",
                useTexture ? "ON" : "OFF", mipmapped ? "Bilinear+Mipmap" : "Nearest",
                shadows ? "ON" : "OFF", vsync ? "ON" : "OFF", fps));
    }

    private void updateModels() {
        sphereModel.identity().translate(-2.2f, 0f, 0f).rotateY((float) Math.toRadians(angle));
        torusModel.identity().translate(2.2f, 0.2f, 0f)
                .rotateY((float) Math.toRadians(angle * 0.7f))
                .rotateX((float) Math.toRadians(60f));
        groundModel.identity().translate(0f, -1.5f, 0f);
    }

    private void drawObjects(ShaderProgram sh, boolean withMaterial) {
        sh.setMat4("model", groundModel);
        if (withMaterial) sh.setVec3("objectColor", new Vector3f(0.85f, 0.85f, 0.85f));
        ground.draw(smooth);

        sh.setMat4("model", sphereModel);
        if (withMaterial) sh.setVec3("objectColor", new Vector3f(1.0f, 0.45f, 0.4f));
        sphere.draw(smooth);

        sh.setMat4("model", torusModel);
        if (withMaterial) sh.setVec3("objectColor", new Vector3f(0.4f, 0.75f, 1.0f));
        torus.draw(smooth);
    }

    private void render() {
        updateModels();

        // Matriks cahaya (Directional-style: proyeksi ortografik)
        Matrix4f lightSpace = new Matrix4f()
                .ortho(-9f, 9f, -9f, 9f, 1f, 25f)
                .lookAt(lightPos, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));

        // ===== PASS 1: Depth Pass dari sudut pandang cahaya =====
        if (shadows) {
            shadowFBO.bindForWriting();
            glEnable(GL_DEPTH_TEST);
            glDisable(GL_CULL_FACE);
            depthShader.use();
            depthShader.setMat4("lightSpaceMatrix", lightSpace);
            drawObjects(depthShader, false);
            shadowFBO.unbind(fbWidth, fbHeight);
        }

        // ===== PASS 2: Lighting Pass dari sudut pandang kamera =====
        glViewport(0, 0, fbWidth, fbHeight);
        glClearColor(0.08f, 0.09f, 0.12f, 1f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        HSRManager.setupDepthTest(depthTest);
        HSRManager.setupBackFaceCulling(cullFace);

        float yawR = (float) Math.toRadians(yaw), pitchR = (float) Math.toRadians(pitch);
        Vector3f eye = new Vector3f(
                (float) (distance * Math.cos(pitchR) * Math.sin(yawR)),
                (float) (distance * Math.sin(pitchR)),
                (float) (distance * Math.cos(pitchR) * Math.cos(yawR)));
        Matrix4f view = new Matrix4f().lookAt(eye, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        float aspect = fbHeight == 0 ? 1f : (float) fbWidth / fbHeight;
        Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(45f), aspect, 0.1f, 100f);

        sceneShader.use();
        sceneShader.setMat4("view", view);
        sceneShader.setMat4("projection", projection);
        sceneShader.setMat4("lightSpaceMatrix", lightSpace);
        sceneShader.setVec3("lightPos", lightPos);
        sceneShader.setVec3("viewPos", eye);
        sceneShader.setVec3("lightColor", lightColor);
        sceneShader.setBool("useBlinnPhong", blinn);
        sceneShader.setBool("useTexture", useTexture);
        sceneShader.setBool("useShadow", shadows);

        texture.bind(0);
        sceneShader.setInt("textureSampler", 0);
        shadowFBO.bindDepthTexture(1);
        sceneShader.setInt("shadowMap", 1);

        drawObjects(sceneShader, true);
    }

    private void cleanup() {
        sphere.cleanup(); torus.cleanup(); ground.cleanup();
        texture.cleanup(); shadowFBO.cleanup();
        sceneShader.cleanup(); depthShader.cleanup();
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
        GLFWErrorCallback cb = glfwSetErrorCallback(null);
        if (cb != null) cb.free();
    }
}