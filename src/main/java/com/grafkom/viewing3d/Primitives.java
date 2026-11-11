package com.grafkom.viewing3d;

/** Generator mesh 3D: Sphere, Torus, Plane. Semua winding CCW (tampak luar). */
public class Primitives {

    public static MeshData sphere(float radius, int stacks, int slices) {
        MeshData d = new MeshData();
        for (int i = 0; i <= stacks; i++) {
            double phi = Math.PI * i / stacks;
            for (int j = 0; j <= slices; j++) {
                double theta = 2.0 * Math.PI * j / slices;
                float x = (float) (radius * Math.sin(phi) * Math.cos(theta));
                float y = (float) (radius * Math.cos(phi));
                float z = (float) (radius * Math.sin(phi) * Math.sin(theta));
                d.addVertex(x, y, z, (float) j / slices, 1f - (float) i / stacks);
            }
        }
        addGridTriangles(d, stacks, slices);
        return d;
    }

    public static MeshData torus(float majorR, float minorR, int rings, int sides) {
        MeshData d = new MeshData();
        for (int i = 0; i <= rings; i++) {
            double theta = 2.0 * Math.PI * i / rings;
            for (int j = 0; j <= sides; j++) {
                double phi = 2.0 * Math.PI * j / sides;
                float x = (float) ((majorR + minorR * Math.cos(phi)) * Math.cos(theta));
                float y = (float) (minorR * Math.sin(phi));
                float z = (float) ((majorR + minorR * Math.cos(phi)) * Math.sin(theta));
                d.addVertex(x, y, z, (float) i / rings * 2f, (float) j / sides);
            }
        }
        addGridTriangles(d, rings, sides);
        return d;
    }

    public static MeshData plane(float half, float tiling) {
        MeshData d = new MeshData();
        int v0 = d.addVertex(-half, 0,  half, 0, 0);
        int v1 = d.addVertex( half, 0,  half, tiling, 0);
        int v2 = d.addVertex( half, 0, -half, tiling, tiling);
        int v3 = d.addVertex(-half, 0, -half, 0, tiling);
        d.addTriangle(v0, v1, v2);
        d.addTriangle(v0, v2, v3);
        return d;
    }

    /** Grid (rows+1) x (cols+1) vertex -> 2 segitiga per sel, urutan CCW dilihat dari luar. */
    private static void addGridTriangles(MeshData d, int rows, int cols) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int a = i * (cols + 1) + j;
                int b = a + cols + 1;
                d.addTriangle(a, a + 1, b);
                d.addTriangle(a + 1, b + 1, b);
            }
        }
    }
}