package s0553449;

import org.lwjgl.util.vector.Vector2f;

public class GeometryUtils {
    public static Vector2f intersectLineSegments(Vector2f rootA, Vector2f dirA, Vector2f rootB, Vector2f dirB) {
        Vector2f intersection = intersectLines(rootA, dirA, rootB, dirB);
        if(intersection == null) return null;
        return pointInRectangle(intersection, rootA, dirA) && pointInRectangle(intersection, rootB, dirB)? intersection : null;
    }

    public static Vector2f intersectLines(Vector2f rootA, Vector2f dirA, Vector2f rootB, Vector2f dirB) {
        float a1 = dirA.y;
        float b1 = -dirA.x;
        float c1 = a1*rootA.x + b1*rootA.y;
        float a2 = dirB.y;
        float b2 = -dirB.x;
        float c2 = a2*rootB.x + b2*rootB.y;
        
        float det = a1*b2 - a2*b1;
        if(det == 0) {
            return null;
        }
        return new Vector2f(
            (b2*c1 - b1*c2)/det, 
            (a1*c2 - a2*c1)/det
        );
    }

    public static boolean pointInRectangle(Vector2f point, Vector2f rectRoot, Vector2f rectDim) {
        if(rectDim.x < 0) {
            rectRoot.setX(rectRoot.x + rectDim.x);
            rectDim.setX(-rectDim.x);
        }
        if(rectDim.y < 0) {
            rectRoot.setY(rectRoot.y + rectDim.y);
            rectDim.setY(-rectDim.y);
        }
        Vector2f p2 = new Vector2f();
        Vector2f.add(rectRoot, rectDim, p2);
        return point.x >= rectRoot.x && point.x <= p2.x && point.y >= rectRoot.y && point.y <= p2.y;
    }
}