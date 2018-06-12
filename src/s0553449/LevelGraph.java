package s0553449;

import java.awt.Polygon;
import java.util.ArrayList;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

/**
 * LevelGraph
 */
public class LevelGraph {
    private class Connection {
        GraphNode target;
        float cost;

        public Connection(GraphNode target, float cost) {
            this.target = target;
            this.cost = cost;
        }
    }

    private class GraphNode extends Node {
        private static final long serialVersionUID = 8196766849155894066L;
		private ArrayList<Connection> connections;

        public GraphNode(Node node) {
            this.x = node.x;
            this.y = node.y;
            this.normal = new Vector2f(node.normal);

            connections = new ArrayList<>();
        }

        public void addConnection(GraphNode target, float cost, boolean symmetric) {
            connections.add(new Connection(target, cost));
            if(symmetric) {
                target.addConnection(this, cost, false);
            }
        }
    }

    private ArrayList<GraphNode> nodes;

    private Polygon[] obstacles;

    private ArrayList<Vector4f> edgeList;

    public LevelGraph(Node[] nodes, Polygon[] obstacles) {
        this.obstacles = obstacles;
        
        this.nodes = new ArrayList<>();
        for (Node node : nodes) {
            this.nodes.add(new GraphNode(node));
        }

        this.edgeList = new ArrayList<>();
    }

    public void calculateVisibilityGraph() {
        edgeList = new ArrayList<>();

		// for all possible edges check if connected, remember cost
		for(GraphNode n1 : nodes) {
			for(GraphNode n2 : nodes) {

				Vector2f root = new Vector2f(n1);
				Vector2f dir = new Vector2f(n2);
				Vector2f.sub(dir, root, dir); // calculate direction vector

				if (!intersectLineLevel(root, dir) && !(n1==n2)) {
                    n1.addConnection(n2, dir.length(), true);
					// debug info
					edgeList.add(new Vector4f(n1.x, n1.y, n2.x, n2.y));
				} 
			}
		}
    }

	private boolean intersectLineLevel(Vector2f root, Vector2f dir) {
		for (Polygon polygon : obstacles) {
			int n = polygon.npoints;
			for(int i = 0; i < n; i++) {
				Vector2f root2 = new Vector2f(polygon.xpoints[i], polygon.ypoints[i]);
				Vector2f dir2 = new Vector2f(polygon.xpoints[(i+1)%n], polygon.ypoints[(i+1)%n]);
				Vector2f.sub(dir2, root2, dir2);

				if(GeometryUtils.intersectLineSegments(root, dir, root2, dir2) != null)
					return true;
			}
		}
		return false;
    }

    public Vector4f[] getEdges() {
        Vector4f[] edgeArray = new Vector4f[edgeList.size()];
        return edgeList.toArray(edgeArray);
    }
}