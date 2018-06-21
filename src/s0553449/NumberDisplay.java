package s0553449;

import java.text.DecimalFormat;
import java.text.NumberFormat;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;

class NumberDisplay {
    /*
     *  Mapping of display points
     * 
     *    0 --- 1
     *    |     |
     *    5 --- 2
     *    |     |
     *    4 -6- 3
     *      7
     */
    private final static Vector2f[] pointPositions = {
        new Vector2f(0, 0),
        new Vector2f(1, 0),
        new Vector2f(1, 1),
        new Vector2f(1, 2),
        new Vector2f(0, 2),
        new Vector2f(0, 1),
        new Vector2f(0.5f, 2),
        new Vector2f(0.4f, 2.5f)
    };
    // maps to points
    private final static int[][] numberMapping = {
        {0, 1, 2, 3, 4, 5, 0}, // 0
        {1, 2, 3}, // 1
        {0, 1, 2, 5, 4, 3}, // 2
        {0, 1, 2, 5, 2, 3, 4}, // 3
        {0, 5, 2, 1, 3}, // 4
        {1, 0, 5, 2, 3, 4}, // 5
        {1, 0, 5, 4, 3, 2, 5}, // 6
        {0, 1, 3}, // 7
        {5, 0, 1, 2, 5, 4, 3, 2}, // 8
        {2, 5, 0, 1, 2, 3, 4}, // 9
        {5, 2}, // -
        {6, 7}  // ,
    };

    private static NumberFormat numberFormat = new DecimalFormat( "#,###,###,##0.00" );

    public static float scale = 0.5f;

    public static void drawFloat(float f, float x, float y) {
        int[] codes = floatToCode(f);
        
        for(int i = 0; i < codes.length; i++) {
            drawChar(codes[i], x + 1.5f * scale * i, y);
        }
    }

    private static void drawChar(int code, float x, float y) {
        int[] positions = numberMapping[code];

        GL11.glBegin(GL11.GL_LINE_STRIP);
        Vector2f pos;
        for(int i = 0; i < positions.length; i++) {
            pos = pointPositions[positions[i]];
            GL11.glVertex2f(pos.x * scale + x, -pos.y * scale + y);
        }
        GL11.glEnd();
    }

    private static int[] floatToCode(float f) {
        String str = numberFormat.format(f);
        int[] out = new int[str.length()];

        for(int i = 0; i < str.length(); i++) {
            char c = str.charAt(i);
            if (c == '.' || c == ',') {
                // draw dot
                out[i] = 11;
            } else if(c == '-') {
                // draw minus
                out[i] = 10;
            } else {
                try {
                    out[i] = Integer.parseInt(c + "");
                } catch(NumberFormatException e) {
                    return new int[0];
                }
            }
        }

        return out;
    }
}