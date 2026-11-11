package com.grafkom.viewing3d;

import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Data geometri mentah (posisi, UV, indeks segitiga CCW).
 * build(false) -> Flat Shading   : satu normal per face.
 * build(true)  -> Smooth Shading : normal vertex = rata-rata normal semua face yang berbagi posisi.
 * Output interleaved: pos(3) normal(3) uv(2), sudah di-de-index (GL_TRIANGLES).
 */
public class MeshData {
    private final List<Vector3f> positions = new ArrayList<>();
    private final List<float[]> uvs = new ArrayList<>();
    private final List<Integer> indices = new ArrayList<>();

    public int addVertex(float x, float y, float z, float u, float v) {
        positions.add(new Vector3f(x, y, z));
        uvs.add(new float[]{u, v});
        return positions.size() - 1;
    }

    public void addTriangle(int a, int b, int c) {
        indices.add(a); indices.add(b); indices.add(c);
    }

    private static String key(Vector3f p) {
        return Math.round(p.x * 10000f) + "," + Math.round(p.y * 10000f) + "," + Math.round(p.z * 10000f);
    }

    private Vector3f faceNormalUnnormalized(int i0, int i1, int i2) {
        Vector3f e1 = new Vector3f(positions.get(i1)).sub(positions.get(i0));
        Vector3f e2 = new Vector3f(positions.get(i2)).sub(positions.get(i0));
        return e1.cross(e2); // arah mengikuti winding CCW
    }

    public float[] build(boolean smooth) {
        int triCount = indices.size() / 3;
        float[] out = new float[triCount * 3 * 8];

        Map<String, Vector3f> accum = new HashMap<>();
        if (smooth) {
            for (int t = 0; t < triCount; t++) {
                int i0 = indices.get(t * 3), i1 = indices.get(t * 3 + 1), i2 = indices.get(t * 3 + 2);
                Vector3f fn = faceNormalUnnormalized(i0, i1, i2); // berbobot luas
                for (int idx : new int[]{i0, i1, i2}) {
                    accum.computeIfAbsent(key(positions.get(idx)), k -> new Vector3f()).add(fn);
                }
            }
            for (Vector3f n : accum.values()) {
                if (n.lengthSquared() > 1e-12f) n.normalize(); else n.set(0, 1, 0);
            }
        }

        int o = 0;
        for (int t = 0; t < triCount; t++) {
            int i0 = indices.get(t * 3), i1 = indices.get(t * 3 + 1), i2 = indices.get(t * 3 + 2);
            Vector3f flatN = null;
            if (!smooth) {
                flatN = faceNormalUnnormalized(i0, i1, i2);
                if (flatN.lengthSquared() > 1e-12f) flatN.normalize(); else flatN.set(0, 1, 0);
            }
            for (int idx : new int[]{i0, i1, i2}) {
                Vector3f p = positions.get(idx);
                Vector3f n = smooth ? accum.get(key(p)) : flatN;
                float[] uv = uvs.get(idx);
                out[o++] = p.x; out[o++] = p.y; out[o++] = p.z;
                out[o++] = n.x; out[o++] = n.y; out[o++] = n.z;
                out[o++] = uv[0]; out[o++] = uv[1];
            }
        }
        return out;
    }
}