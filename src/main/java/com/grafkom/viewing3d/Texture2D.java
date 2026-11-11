package com.grafkom.viewing3d;

import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.stb.STBImage.*;

/**
 * Indikator 4: Texture Mapping.
 * Memuat PNG/JPG via STBImage. Jika file tidak ada, otomatis memakai tekstur papan catur prosedural
 * sehingga program tetap jalan.
 */
public class Texture2D {
    private final int textureID;

    public Texture2D(String filePath) {
        textureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureID);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        boolean loaded = false;
        stbi_set_flip_vertically_on_load(true);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1), ch = stack.mallocInt(1);
            ByteBuffer data = stbi_load(filePath, w, h, ch, 4);
            if (data != null) {
                glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, w.get(0), h.get(0), 0, GL_RGBA, GL_UNSIGNED_BYTE, data);
                stbi_image_free(data);
                loaded = true;
            }
        }
        if (!loaded) {
            System.err.println("Tekstur '" + filePath + "' tidak ditemukan -> memakai checkerboard prosedural.");
            generateChecker();
        }
        glGenerateMipmap(GL_TEXTURE_2D);
        setMipmapped(true);
    }

    private void generateChecker() {
        int size = 256, cell = 16;
        ByteBuffer buf = MemoryUtil.memAlloc(size * size * 4);
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                boolean a = ((x / cell) + (y / cell)) % 2 == 0;
                buf.put((byte) (a ? 235 : 70)).put((byte) (a ? 235 : 110)).put((byte) (a ? 235 : 200)).put((byte) 255);
            }
        }
        buf.flip();
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, size, size, 0, GL_RGBA, GL_UNSIGNED_BYTE, buf);
        MemoryUtil.memFree(buf);
    }

    /** true = Bilinear + Mipmap (GL_LINEAR_MIPMAP_LINEAR); false = Nearest Neighbor. */
    public void setMipmapped(boolean mipmapped) {
        glBindTexture(GL_TEXTURE_2D, textureID);
        if (mipmapped) {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        } else {
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        }
    }

    public void bind(int unit) {
        glActiveTexture(GL_TEXTURE0 + unit);
        glBindTexture(GL_TEXTURE_2D, textureID);
    }

    public void cleanup() { glDeleteTextures(textureID); }
}