package com.grafkom.viewing3d;

import static org.lwjgl.opengl.GL30.*;

/** Mesh GPU: menyimpan dua VAO (flat & smooth) agar toggle 'F' instan. */
public class Mesh {
    private final int vaoFlat, vboFlat, vaoSmooth, vboSmooth;
    private final int vertexCount;

    public Mesh(MeshData data) {
        float[] flat = data.build(false);
        float[] smooth = data.build(true);
        vertexCount = flat.length / 8;
        int[] f = upload(flat);
        int[] s = upload(smooth);
        vaoFlat = f[0]; vboFlat = f[1];
        vaoSmooth = s[0]; vboSmooth = s[1];
    }

    private int[] upload(float[] data) {
        int vao = glGenVertexArrays();
        int vbo = glGenBuffers();
        glBindVertexArray(vao);
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, data, GL_STATIC_DRAW);
        int stride = 8 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, stride, 6L * Float.BYTES);
        glEnableVertexAttribArray(2);
        glBindVertexArray(0);
        return new int[]{vao, vbo};
    }

    public void draw(boolean smooth) {
        glBindVertexArray(smooth ? vaoSmooth : vaoFlat);
        glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        glBindVertexArray(0);
    }

    public void cleanup() {
        glDeleteBuffers(vboFlat); glDeleteBuffers(vboSmooth);
        glDeleteVertexArrays(vaoFlat); glDeleteVertexArrays(vaoSmooth);
    }
}