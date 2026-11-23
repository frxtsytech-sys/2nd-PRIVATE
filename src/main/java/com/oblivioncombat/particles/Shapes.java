package com.oblivioncombat.particles;

import net.minecraft.util.math.Vec3d;

import java.util.function.Consumer;

/** Pure math: generates point formations. No world access, no allocation of lists. */
public final class Shapes {
    private Shapes() {}

    public static final Vec3d UP = new Vec3d(0, 1, 0);

    /** Two unit vectors perpendicular to n (n must be a unit vector). */
    public static Vec3d[] basis(Vec3d n) {
        Vec3d ref = Math.abs(n.y) < 0.99 ? UP : new Vec3d(1, 0, 0);
        Vec3d u = n.crossProduct(ref).normalize();
        Vec3d v = n.crossProduct(u).normalize();
        return new Vec3d[]{u, v};
    }

    /** Circle of radius r around c in the plane with the given normal. */
    public static void ring(Vec3d c, Vec3d normal, double r, int pts, double phase, Consumer<Vec3d> out) {
        Vec3d[] b = basis(normal.normalize());
        for (int i = 0; i < pts; i++) {
            double a = phase + 2 * Math.PI * i / pts;
            out.accept(c.add(b[0].multiply(Math.cos(a) * r)).add(b[1].multiply(Math.sin(a) * r)));
        }
    }

    /** Evenly spread points on a sphere surface (Fibonacci lattice), rotated by rot. */
    public static void sphere(Vec3d c, double r, int pts, double rot, Consumer<Vec3d> out) {
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < pts; i++) {
            double y = 1 - 2.0 * (i + 0.5) / pts;
            double rad = Math.sqrt(Math.max(0, 1 - y * y));
            double th = golden * i + rot;
            out.accept(c.add(Math.cos(th) * rad * r, y * r, Math.sin(th) * rad * r));
        }
    }

    /** Spiral around an axis: radius goes r0 -> r1, travelling `length` along the axis. */
    public static void spiral(Vec3d c, Vec3d axis, double r0, double r1, double length, double turns,
                              int pts, double phase, Consumer<Vec3d> out) {
        Vec3d n = axis.normalize();
        Vec3d[] b = basis(n);
        for (int i = 0; i < pts; i++) {
            double t = pts == 1 ? 0 : i / (double) (pts - 1);
            double r = r0 + (r1 - r0) * t;
            double a = phase + turns * 2 * Math.PI * t;
            out.accept(c.add(b[0].multiply(Math.cos(a) * r))
                    .add(b[1].multiply(Math.sin(a) * r))
                    .add(n.multiply(length * t)));
        }
    }

    /** Cubic Bezier point at t in [0,1]. */
    public static Vec3d bezier(Vec3d p0, Vec3d p1, Vec3d p2, Vec3d p3, double t) {
        double u = 1 - t;
        return p0.multiply(u * u * u)
                .add(p1.multiply(3 * u * u * t))
                .add(p2.multiply(3 * u * t * t))
                .add(p3.multiply(t * t * t));
    }

    /**
     * Crescent whose tip leads along fwd. The arc spans +-halfAngle in the plane (fwd, side);
     * thickness tapers to zero at both tips.
     */
    public static void crescent(Vec3d p, Vec3d fwd, Vec3d side, double radius, double halfAngle,
                                int steps, int layers, double thickness, Consumer<Vec3d> out) {
        for (int i = 0; i <= steps; i++) {
            double t = -halfAngle + 2 * halfAngle * i / steps;
            double taper = Math.cos(t / halfAngle * Math.PI / 2);
            for (int l = 0; l < layers; l++) {
                double frac = layers == 1 ? 0 : (double) l / (layers - 1);
                double rr = radius - thickness * taper * frac;
                out.accept(p.add(fwd.multiply(rr * Math.cos(t) - radius)).add(side.multiply(rr * Math.sin(t))));
            }
        }
    }
}
