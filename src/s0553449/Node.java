package s0553449;

import java.util.Objects;

import org.lwjgl.util.vector.Vector2f;

/**
 * GraphNode
 */
public class Node extends Vector2f {
    private static final long serialVersionUID = 969189675123682867L;

    public Vector2f normal;

	public Node(float x, float y, float normalX, float normalY) {
        super(x, y);
        this.normal = new Vector2f(normalX, normalY);
    }

    public Node(Node node) {
        super(node.x, node.y);
        this.normal = node.normal;
    }

    public Node(Vector2f vec, Vector2f normal) {
        super(vec.x, vec.y);
        this.normal = normal;
    }

    public Node() {
        super();
    }

    @Override
    public int hashCode() {
        return Objects.hash(x, y);
    }

    @Override
    public boolean equals(Object o) {
        // self check
        if (this == o)
            return true;
        // null check
        if (o == null)
            return false;
        // type check and cast
        if (getClass() != o.getClass())
            return false;
        Node other = (Node) o;
        // field comparison
        return x == other.x && y == other.y;
    }


}