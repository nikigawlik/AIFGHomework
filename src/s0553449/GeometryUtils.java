package s0553449;

import org.lwjgl.util.vector.Vector2f;

public class GeometryUtils {
    public static Vector2f intersectLineSegments(Vector2f rootA, Vector2f dirA, Vector2f rootB, Vector2f dirB) {
        Vector2f intersection = intersectLines(rootA, dirA, rootB, dirB);
        if(intersection == null) return null;
        return pointInRectangle(intersection, rootA, dirA) && pointInRectangle(intersection, rootB, dirB)? intersection : null;
    }

    public static Vector2f intersectLines(Vector2f rootA, Vector2f dirA, Vector2f rootB, Vector2f dirB) {
        double a1 = dirA.y;
        double b1 = -dirA.x;
        double c1 = a1*rootA.x + b1*rootA.y;
        double a2 = dirB.y;
        double b2 = -dirB.x;
        double c2 = a2*rootB.x + b2*rootB.y;
        
        double det = a1*b2 - a2*b1;
        if(det == 0) {
            return null;
        }
        return new Vector2f(
            (float)((b2*c1 - b1*c2)/det), 
            (float)((a1*c2 - a2*c1)/det)
        );
    }

    public static boolean pointInRectangle(Vector2f point, Vector2f rectRoot, Vector2f rectDim) {
        float x1 = rectRoot.x;
        float y1 = rectRoot.y;
        float x2 = rectRoot.x + rectDim.x;
        float y2 = rectRoot.y + rectDim.y;

        float buffer;
        if(x1 > x2) {
            buffer = x1;
            x1 = x2;
            x2 = buffer;
        }
        if(y1 > y2) {
            buffer = y1;
            y1 = y2;
            y2 = buffer;
        }
        
        return point.x >= x1 && point.x <= x2 && point.y >= y1 && point.y <= y2;
    }

    public static Vector2f closestPointOnLineSegment(Vector2f l1, Vector2f l2, Vector2f p) {
        Vector2f projP = projectPointOnLine(l1, l2, p);
        clampPointInRectangle(l1, l2, projP);
        
        return projP;
    }

    public static Vector2f projectPointOnLine(Vector2f l1, Vector2f l2, Vector2f p) {
        // return l2;

        Vector2f n = new Vector2f();
        Vector2f.sub(l2, l1, n);
        if(n.length() == 0) {
            Vector2f.add(n, l1, n);
            return n;
        }
        n.normalise();
        Vector2f p3 = new Vector2f(p);
        Vector2f.sub(p3, l1, p3);

        n.scale(Vector2f.dot(n, p3));
        Vector2f.add(n, l1, n);

        return n;
    }

    public static float distanceBetweenPoints(Vector2f p1, Vector2f p2) {
        return (float) Math.sqrt((p2.x - p1.x)*(p2.x - p1.x) + (p2.y - p1.y)*(p2.y-p1.y));
    }

    public static void clampPointInRectangle(Vector2f r1, Vector2f r2, Vector2f p) {
        float maxX = Math.max(r1.x, r2.x);
        float minX = Math.min(r1.x, r2.x);
        float maxY = Math.max(r1.y, r2.y);
        float minY = Math.min(r1.y, r2.y);
        p.setX(Math.min(Math.max(p.x, minX), maxX));
        p.setY(Math.min(Math.max(p.y, minY), maxY));
    }
}