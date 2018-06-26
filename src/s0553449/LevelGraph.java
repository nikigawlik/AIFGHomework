package s0553449;

import java.awt.Polygon;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;

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

        public GraphNode(Vector2f vec) {
            this.x = vec.x;
            this.y = vec.y;
            this.normal = new Vector2f(0f, 0f);

            connections = new ArrayList<>();
        }

        public void addConnection(GraphNode target, float cost, boolean symmetric) {
            connections.add(new Connection(target, cost));
            if(symmetric) {
                target.addConnection(this, cost, false);
            }
        }

        public ArrayList<Connection> getConnections() {
            return connections;
        }
    }

    private class HashMapComparator implements Comparator<GraphNode> {
        private HashMap<GraphNode, Float> hashMap;
        public HashMapComparator(HashMap<GraphNode, Float> hashMap) {
            this.hashMap = hashMap;
        }

		@Override
		public int compare(GraphNode o1, GraphNode o2) {
            
            return hashMap.get(o1).compareTo(hashMap.get(o2));
		}   
    }

    private HashSet<GraphNode> nodes;
    private float collisionDetectionOffset;

    private Polygon[] obstacles;
    private Polygon[] slowZones;
    private Polygon[] fastZones;

    private ArrayList<Vector4f> edgeList;

    public LevelGraph(Node[] nodes, Polygon[] obstacles, Polygon[] slowZones, Polygon[] fastZones, float normalExtension) {
        this.obstacles = obstacles;
        this.slowZones = slowZones;
        this.fastZones = fastZones;
        this.collisionDetectionOffset = normalExtension;
        
        this.nodes = new HashSet<>();
        for (Node node : nodes) {
            this.nodes.add(new GraphNode(node));
        }

        this.edgeList = new ArrayList<>();
    }

    public void calculateVisibilityGraph() {
        edgeList = new ArrayList<>();

		// for all possible edges check if connected, remember cost
		for(GraphNode n1 : nodes) {
			calculateVisibilitiesForNode(n1);
		}
    }

	private void calculateVisibilitiesForNode(GraphNode n1) {
        calculateVisibilitiesForNode(n1, true);
    }

	private void calculateVisibilitiesForNode(GraphNode n1, boolean useOffset) {
        for(GraphNode n2 : nodes) {
            Vector2f root = new Vector2f(n1);
            Vector2f dir = new Vector2f(n2);
            Vector2f.sub(dir, root, dir); // calculate direction vector
            
            if (!intersectLineLevel(root, dir, useOffset) && !(n1.equals(n2))) {
                n1.addConnection(n2, calculateCost(root, dir), true);
                // debug info
                edgeList.add(new Vector4f(n1.x, n1.y, n2.x, n2.y));
            }
        }
    }

    private float calculateCost(Vector2f root, Vector2f dir) {
        float cost = 0;
        int iterations = (int) dir.length();
        int numberOfFastSpots = 0;

        for(int i = 0; i < iterations; i++) {
            float px = root.x + dir.x * ((float) i / iterations);
            float py = root.y + dir.y * ((float) i / iterations);
            
            float localCost = 1f;

            for (Polygon polygon : slowZones) {
                if(polygon.contains(px, py)) {
                    localCost *= 6;
                }
            }
            for (Polygon polygon : fastZones) {
                if(polygon.contains(px, py)) {
                    localCost /= 4;
                    numberOfFastSpots++;
                }
            }

            cost += localCost;
        }
        cost /= iterations;
        cost *= dir.length();
        if(numberOfFastSpots > 120) {
            cost = 1000;
        }
        return cost;
    }
    
    public boolean testLineAgainstLevel(Vector2f l1, Vector2f l2, boolean useOffset) {
        Vector2f dir = new Vector2f();
        Vector2f.sub(l2, l1, dir);
        return intersectLineLevel(l1, dir, useOffset);
    }

	private boolean intersectLineLevel(Vector2f root, Vector2f dir, boolean useOffset) {
		for (Polygon polygon : obstacles) {
			int n = polygon.npoints;
			for(int i = 0; i < n; i++) {
                Vector2f root2 = new Vector2f(polygon.xpoints[i], polygon.ypoints[i]);
                if(useOffset) {
                    Vector2f offsetR2 = polygonGetNormal(polygon, i);
                    offsetR2.scale(collisionDetectionOffset);
                    Vector2f.add(root2, offsetR2, root2);
                }

                Vector2f dest2 = new Vector2f(polygon.xpoints[(i+1)%n], polygon.ypoints[(i+1)%n]);
                if(useOffset) {
                    Vector2f offsetD2 = polygonGetNormal(polygon, i+1);
                    offsetD2.scale(collisionDetectionOffset);
                    Vector2f.add(dest2, offsetD2, dest2);
                }

                Vector2f dir2 = dest2; // for clarity
                Vector2f.sub(dest2, root2, dir2);

				if(GeometryUtils.intersectLineSegments(root, dir, root2, dir2) != null)
					return true;
			}
		}
		return false;
    }

    private Vector2f polygonGetNormal(Polygon polygon, int pointID) {
        int n = polygon.npoints;
        int i = (pointID - 1 + n) % n;
        Vector2f p1 = new Vector2f(polygon.xpoints[i], polygon.ypoints[i]);
        Vector2f p2 = new Vector2f(polygon.xpoints[(i+1)%n], polygon.ypoints[(i+1)%n]);
        Vector2f p3 = new Vector2f(polygon.xpoints[(i+2)%n], polygon.ypoints[(i+2)%n]);

        Vector2f v1 = new Vector2f();
        Vector2f.sub(p1, p2, v1);
        Vector2f v2 = new Vector2f();
        Vector2f.sub(p3, p2, v2);
        v1.normalise();
        v2.normalise();
        Vector2f.add(v1, v2, v1);
        v1.normalise();
        v1.scale(-1f);
        return v1;
    }

    public Vector4f[] getEdges() {
        Vector4f[] edgeArray = new Vector4f[edgeList.size()];
        return edgeList.toArray(edgeArray);
    }

    public Node[] findPath(Vector2f start, Vector2f goal) {
        GraphNode startNode = new GraphNode(start);
        GraphNode goalNode = new GraphNode(goal);

        nodes.add(startNode);
        nodes.add(goalNode);

        calculateVisibilitiesForNode(startNode);
        calculateVisibilitiesForNode(goalNode);

        // copy the path, because we don't want to pass references outwards
        Node[] path = aStar(startNode, goalNode);

        if(path == null) {
            System.out.println("No Path found! Retrying without offset...");
            calculateVisibilitiesForNode(startNode, false);
            calculateVisibilitiesForNode(goalNode, false);

            path = aStar(startNode, goalNode);
            System.out.println("...failed still.");

            if(path == null) {
                return null;
            }
        }

        Node[] newPath = new Node[path.length];
        for (int i = 0; i < path.length; i++) {
            newPath[i] = new Node(path[i]);
        }

        return newPath;
    }

    private Node[] aStar(GraphNode start, GraphNode goal) {
        // The set of nodes already evaluated
        HashSet<GraphNode> closedSet = new HashSet<>();
    
        // The set of currently discovered nodes that are not evaluated yet.
        // Initially, only the start node is known.
        HashSet<GraphNode> openSet = new HashSet<>();
        openSet.add(start);
    
        // For each node, which node it can most efficiently be reached from.
        // If a node can be reached from many nodes, cameFrom will eventually contain the
        // most efficient previous step.
        HashMap<GraphNode, GraphNode> cameFrom = new HashMap<>();
    
        // For each node, the cost of getting from the start node to that node.
        HashMap<GraphNode, Float> gScore = new HashMap<>();
        for (GraphNode node : nodes) {
            gScore.put(node, Float.POSITIVE_INFINITY);
        }
    
        // The cost of going from start to start is zero.
        gScore.put(start, 0f);
    
        // For each node, the total cost of getting from the start node to the goal
        // by passing by that node. That value is partly known, partly heuristic.
        HashMap<GraphNode, Float> fScore = new HashMap<>();
        for (GraphNode node : nodes) {
            fScore.put(node, Float.POSITIVE_INFINITY);
        }
    
        // For the first node, that value is completely heuristic.
        fScore.put(start, heuristic_cost_estimate(start, goal));

        Comparator<GraphNode> fScoreComparator = new HashMapComparator(fScore);
    
        while(!openSet.isEmpty()) {
            // current := the node in openSet having the lowest fScore[] value
            GraphNode current = Collections.min(openSet, fScoreComparator);
            if (current == goal)
                return reconstruct_path(cameFrom, current);
            
            openSet.remove(current);
            closedSet.add(current);
            
            // for each neighbor of current
            for (Connection connection : current.getConnections()) {
                GraphNode neighbor = connection.target;
                if(closedSet.contains(neighbor))
                    continue;		// Ignore the neighbor which is already evaluated.
                
                if (!openSet.contains(neighbor)) // Discover a new node
                    openSet.add(neighbor);
                
                // The distance from start to a neighbor
                // the "dist_between" function may vary as per the solution requirements.
                float tentative_gScore = gScore.get(current) + connection.cost;
                if (tentative_gScore >= gScore.get(neighbor))
                    continue;		// This is not a better path.
                
                // This path is the best until now. Record it!
                cameFrom.put(neighbor, current);
                gScore.put(neighbor, tentative_gScore);
                fScore.put(neighbor, gScore.get(neighbor) + heuristic_cost_estimate(neighbor, goal));
            }
        }
            
        return null;
    }

    private float heuristic_cost_estimate(GraphNode n1, GraphNode n2) {
        Vector2f dif = new Vector2f();
        Vector2f.sub(n2, n1, dif);
        return dif.length();
    }
    
    private Node[] reconstruct_path(HashMap<GraphNode, GraphNode> cameFrom, GraphNode current) {
        ArrayList<GraphNode> total_path = new ArrayList<>();
        total_path.add(current);

        while(cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            total_path.add(current);
        }

        Collections.reverse(total_path);

        Node[] nodeArray = new Node[total_path.size()];

        return total_path.toArray(nodeArray);
    }
}