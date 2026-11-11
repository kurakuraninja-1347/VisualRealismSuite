package com.grafkom.viewing3d;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;

import java.io.IOException;
import java.io.InputStream;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;

import static org.lwjgl.opengl.GL30.*;

public class ShaderProgram {
    private final int id;

    public ShaderProgram(String vertPath, String fragPath) {
        int vs = compile(GL_VERTEX_SHADER, readResource(vertPath), vertPath);
        int fs = compile(GL_FRAGMENT_SHADER, readResource(fragPath), fragPath);
        id = glCreateProgram();
        glAttachShader(id, vs);
        glAttachShader(id, fs);
        glLinkProgram(id);
        if (glGetProgrami(id, GL_LINK_STATUS) == GL_FALSE)
            throw new RuntimeException("Link error: " + glGetProgramInfoLog(id));
        glDeleteShader(vs);
        glDeleteShader(fs);
    }

    private static int compile(int type, String src, String name) {
        int s = glCreateShader(type);
        glShaderSource(s, src);
        glCompileShader(s);
        if (glGetShaderi(s, GL_COMPILE_STATUS) == GL_FALSE)
            throw new RuntimeException("Compile error in " + name + ": " + glGetShaderInfoLog(s));
        return s;
    }

    private static String readResource(String path) {
        try (InputStream in = ShaderProgram.class.getResourceAsStream(path)) {
            if (in == null) throw new RuntimeException("Shader tidak ditemukan: " + path);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void use() { glUseProgram(id); }

    public void setInt(String n, int v)         { glUniform1i(glGetUniformLocation(id, n), v); }
    public void setBool(String n, boolean v)    { glUniform1i(glGetUniformLocation(id, n), v ? 1 : 0); }
    public void setVec3(String n, Vector3f v)   { glUniform3f(glGetUniformLocation(id, n), v.x, v.y, v.z); }
    public void setMat4(String n, Matrix4f m) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            m.get(fb);
            glUniformMatrix4fv(glGetUniformLocation(id, n), false, fb);
        }
    }

    public void cleanup() { glDeleteProgram(id); }
}