package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL11.*;

/** Indikator 3: Hidden Surface Removal (Z-Buffer + Back-Face Culling). */
public class HSRManager {
    public static void setupDepthTest(boolean enable) {
        if (enable) {
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LESS); // pixel lebih dekat lolos uji depth
        } else {
            glDisable(GL_DEPTH_TEST);
        }
    }

    public static void setupBackFaceCulling(boolean enable) {
        if (enable) {
            glEnable(GL_CULL_FACE);
            glCullFace(GL_BACK);   // buang polygon belakang
            glFrontFace(GL_CCW);   // Counter-Clockwise = tampak depan
        } else {
            glDisable(GL_CULL_FACE);
        }
    }
}