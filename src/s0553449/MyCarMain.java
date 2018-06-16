package s0553449;

import java.awt.Point;
import java.awt.Polygon;
import java.text.DecimalFormat;
import java.util.HashSet;
import java.util.LinkedList;

import org.lwjgl.opengl.GL11;
// import static org.lwjgl.opengl.GL21.*;
// import static org.lwjgl.opengl.GL33.*;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import lenz.htw.ai4g.ai.AI;
import lenz.htw.ai4g.ai.DriverAction;
import lenz.htw.ai4g.ai.Info;

public class MyCarMain extends AI {

	protected float brakeAngle = 6.14f;
	protected float maxAngleSpd = 50f;
	protected float approachPower = 1.62f;
	protected float turnVelocity = 6f;
	
	protected float targetWeight = 1f;
	protected float obstacleWeight = 10f;
	
	protected float feelerDistance = 20f;
	protected float feelerDistanceSides = 10f;
	protected float feelerAngle = (float) Math.PI/4;

	// pathfinding related
	protected float cornerOffset = 30;
	protected float cornerCalcMargin = 7f;
	protected float cornerPostOffset = 10f;
	protected float pointReachRadius = 20f;
	
	private String debugStr = "";
	DecimalFormat debugFormat = new DecimalFormat( "#,###,###,##0.0000" );
	
	private LevelGraph levelGraph; // graph without start and goal nodes
	private Node currentGoal;

	// display specific stuff
	private float x;
	private float y; 
	private float[] debugXs;
	private float[] debugYs;
	private Vector2f[] debugPoints = new Vector2f[0]; // array of points to draw
	private Vector4f[] debugLines = new Vector4f[0]; // array of lines to draw (in pairs)

	private Node[] currentPath;
	private int currentPathPoint;

	public MyCarMain(Info info) {
		super(info);
		init();
	}

	protected void init() {
		enlistForTournament(553449);
		calculateGraph();
	}

	protected void calculateGraph() {
		// calculate outer points
		Node[] nodes = findNodes();
		System.out.println("create level graph!");
		levelGraph = new LevelGraph(nodes, info.getTrack().getObstacles(), cornerOffset - cornerCalcMargin);
		// calculate edges
		levelGraph.calculateVisibilityGraph();

		debugLines = levelGraph.getEdges();
		debugPoints = nodes;
	}

	private Node[] findNodes() {
		HashSet<Vector2f> outputNodes = new HashSet<>();
		for (Polygon polygon : info.getTrack().getObstacles()) {
			int n = polygon.npoints;
			for(int i = 0; i < n; i++) {
				Vector2f p1 = new Vector2f(polygon.xpoints[i], polygon.ypoints[i]);
				Vector2f p2 = new Vector2f(polygon.xpoints[(i+1)%n], polygon.ypoints[(i+1)%n]);
				Vector2f p3 = new Vector2f(polygon.xpoints[(i+2)%n], polygon.ypoints[(i+2)%n]);
				
				Vector2f v1 = new Vector2f();
				Vector2f.sub(p1, p2, v1);
				Vector2f v2 = new Vector2f();
				Vector2f.sub(p3, p2, v2);
				Vector2f v2o = new Vector2f(-v2.y, v2.x);
			
				if(Vector2f.dot(v1, v2o) > 0) {
					v1.normalise();
					v2.normalise();
					Vector2f normal = new Vector2f();
					Vector2f.add(v1, v2, normal);
					normal.normalise();
					normal.scale(-1f);
					Vector2f offset = new Vector2f(normal);
					offset.scale(cornerOffset);
					Vector2f.add(p2, offset, p2);
					outputNodes.add(new Node(p2, normal));
				}
			}
		}

		Node[] array = new Node[outputNodes.size()];
		return outputNodes.toArray(array);
	}

	@Override
	public DriverAction update(boolean arg0) {
		debugStr = "";

		Point checkpoint = info.getCurrentCheckpoint();
		Node node = new Node(checkpoint.x, checkpoint.y, 0, 0);

		if(levelGraph != null && (currentGoal == null || !node.equals(currentGoal))) {
			currentGoal = node;
			Vector2f currentPos = new Vector2f(info.getX(), info.getY());

			// recalculate current path
			Node[] path = levelGraph.findPath(currentPos, currentGoal);
			// for (Node n : path) {
			// 	Vector2f offset = new Vector2f(n.normal);
			// 	offset.scale(cornerPostOffset);
			// 	Vector2f.add(n, offset, n);
			// }

			// LinkedList<Node> list = new LinkedList<>(Arrays.asList(path));
			LinkedList<Node> list = new LinkedList<>();

			list.add(path[0]);
			for(int i = 1; i < path.length - 1; i++) {
				Node prev = path[i-1];
				Node cur = path[i];
				Node next = path[i+1];

				Vector2f p1 = new Vector2f(cur);
				Vector2f.sub(p1, next, p1);
				p1.normalise();
				p1.scale(cornerPostOffset);
				Vector2f.add(p1, cur, p1);

				Vector2f p2 = new Vector2f(cur);
				Vector2f.sub(p2, prev, p2);
				p2.normalise();
				p2.scale(cornerPostOffset);
				Vector2f.add(p2, cur, p2);

				list.add(new Node(p1, cur.normal));
				list.add(new Node(p2, cur.normal));
			}
			list.add(path[path.length - 1]);


			// modify the graph to have many points and smooth it out
			list = smoothNodes(list, 4);

			currentPath = list.toArray(path);
			currentPathPoint = 0;
		}
		
		return doSteering();
	}

	private LinkedList<Node> smoothNodes(LinkedList<Node> list, int steps) {
		for(int step = 0; step < steps; step++) {
				LinkedList<Node> newList = new LinkedList<>();
				newList.add(list.getFirst());
				for(int i = 1; i < list.size() - 1; i++) {
					Node prevNode = list.get(i-1);
					Node curNode = list.get(i);
					Node nextNode = list.get(i+1);

					Vector2f p1 = new Vector2f(curNode);
					Vector2f.sub(p1, prevNode, p1);
					p1.scale(0.75f);
					Vector2f.add(p1, prevNode, p1);

					Vector2f p2 = new Vector2f(nextNode);
					Vector2f.sub(p2, curNode, p2);
					p2.scale(0.25f);
					Vector2f.add(p2, curNode, p2);

					newList.add(new Node(p1, curNode.normal));
					newList.add(new Node(p2, curNode.normal));
				}
				newList.add(list.getLast());

				list = newList;
			}
		return list;
	}

	private DriverAction doSteering() {
		float throttle = info.getMaxAcceleration();
		float steering = 0f;
		
		x = info.getX();
		y = info.getY();

		Point ncp = getCurrentTargetPoint();

		float deltaX = ncp.x - x;
		float deltaY = ncp.y - y;
		float distanceToTarget = (float) Math.sqrt(deltaX*deltaX + deltaY*deltaY);
		deltaX /= distanceToTarget;
		deltaY /= distanceToTarget;
		
		deltaX *= targetWeight;
		deltaY *= targetWeight;
		
		// debugStr += "pre delta: " + deltaX + ", " + deltaY + "\n";
		
		Polygon[] obstacles = info.getTrack().getObstacles();
		
		debugXs = new float[3];
		debugYs = new float[3];

		float throttlemod = 1f;
		// stupid hack
		boolean firstCollided = false;
		
		for(int i = -1; i <= 1; i++) {
			float angle = i * feelerAngle;
			float absAngle = info.getOrientation() + angle;
			float dX = (float) Math.cos(absAngle);
			float dY = (float) Math.sin(absAngle);
			float invert = (float) Math.signum(i);

			float fd = i == 0? feelerDistance : feelerDistanceSides;
			
			for(Polygon o : obstacles) {
				if(o.contains(x + dX * fd, y + dY * fd) && !(i == 1 && firstCollided)) {
					deltaX += invert * dY * obstacleWeight;
					deltaY += -invert * dX * obstacleWeight;

					throttlemod = i == 0? -1f : 0.4f;
					if (i == -1) {
						firstCollided = true;
					}
				}
			}
			
			debugXs[i+1] = x + dX * fd;
			debugYs[i+1] = y + dY * fd;
		}

		// debugStr += "post delta: " + deltaX + ", " + deltaY + "\n";
		
		float reverse = 1;
		
		float toAngle = (float) Math.atan2(reverse * deltaY, reverse * deltaX);
		float fromAngle = info.getOrientation();
		
		float deltaAngle = (toAngle - fromAngle);
		deltaAngle = (deltaAngle % 6.283f + 6.283f) % 6.283f;
		if (Math.abs(deltaAngle) > 3.1415f)
			deltaAngle -= 6.283f;
		
		// the angular velocity we want to have (linear function)
		float absDA = Math.abs(deltaAngle);
		float targetAngleV = (float) Math.pow(absDA/brakeAngle, approachPower) * maxAngleSpd;
		targetAngleV *= Math.signum(deltaAngle);
		// clamp
		targetAngleV = Math.min(Math.max(targetAngleV, -maxAngleSpd), maxAngleSpd);
		
		steering = targetAngleV - info.getAngularVelocity();
		
		if (Math.abs(deltaAngle) < 1.57) {
			// We are on target (more or less)
			throttle = info.getMaxAcceleration();			
		} else {
			// We are turning, so we drive slower
			float acc = Math.max(turnVelocity - info.getVelocity().length(), 0);
			throttle = acc;
		}

		throttle *= throttlemod;

		// debugStr += "deltaAngle: " + debugFormat.format(deltaAngle) + "\n";
		// debugStr += "targetAngleV: " + debugFormat.format(targetAngleV) + "\n";
		// debugStr += "current angleV: " + debugFormat.format(info.getAngularVelocity()) + "\n";
		// debugStr += "current vel: " + debugFormat.format(info.getVelocity().length()) + "\n";

		// debugStr += "steering chk: " + debugFormat.format(steering/info.getMaxAngularAcceleration()) + "\n";
		// debugStr += "throttle chk: " + debugFormat.format(throttle/info.getMaxAcceleration()) + "\n";
		
		return new DriverAction(throttle, steering);
	}

	protected Point getCurrentTargetPoint() {
		Point p = null;//info.getCurrentCheckpoint();

		if(currentPath != null) {
			Node curNode = currentPath[currentPathPoint];
			// Node prevNode = currentPath[Math.max(currentPathPoint - 1, 0)];
			while (Math.sqrt((curNode.x - x)*(curNode.x - x) + (curNode.y - y)*(curNode.y - y)) <= pointReachRadius) {
				// System.out.println("INCREASE");
				if(currentPathPoint + 1 < currentPath.length) {
					currentPathPoint++;
					curNode = currentPath[currentPathPoint];
				} else {
					break;
				}
			}
			p = new Point((int) curNode.x, (int) curNode.y);
		}

		return p;
	}

	@Override
	public void doDebugStuff() {
		
		if(debugStr.compareTo("") != 0) {
			System.out.println(debugStr);
		}
		
		if (debugXs.length != 0) {
			for (int i = 0; i < debugXs.length; i++) {
				GL11.glColor3f(0.0f, 1.0f, 0.2f);
				GL11.glBegin(GL11.GL_LINE_STRIP);
		
				GL11.glVertex2d(x, y);
				GL11.glVertex2d(debugXs[i], debugYs[i]);
				GL11.glEnd();
			}
		}
		
		// misc debug lines
		GL11.glColor3f(1.0f, 0.2f, 0.0f);
		GL11.glBegin(GL11.GL_LINES);

		GL11.glVertex2d(x, y);
		GL11.glVertex2d(x + info.getVelocity().x, y + info.getVelocity().y);
		GL11.glEnd();
		
		// misc debug points
		GL11.glColor3f(1.0f, 0.2f, 0.0f);
		GL11.glBegin(GL11.GL_POINTS);

		GL11.glVertex2d(getCurrentTargetPoint().x, getCurrentTargetPoint().y);
		GL11.glEnd();

		if(debugPoints != null) {
			GL11.glColor3f(1f, 1f, 1f);
			GL11.glBegin(GL11.GL_POINTS);
			for (Vector2f p : debugPoints) {
				GL11.glVertex2d(p.x, p.y);
			}
			GL11.glVertex2d(0, 1000);
			GL11.glVertex2d(1000, 1000);
			GL11.glVertex2d(1000, 0);
			GL11.glVertex2d(0, 0);
			GL11.glEnd();
		}

		if (debugLines != null) {
			GL11.glColor3f(0f, 0f, 0f);
			GL11.glBegin(GL11.GL_LINES);
			for (Vector4f p : debugLines) {
				GL11.glVertex2d(p.x, p.y);
				GL11.glVertex2d(p.z, p.w);
			}
			GL11.glEnd();
		}

		if (currentPath != null) {
			GL11.glColor3f(0f, 0f, 1f);
			GL11.glBegin(GL11.GL_LINE_STRIP);
			for (Vector2f p : currentPath) {
				GL11.glVertex2d(p.x, p.y);
			}
			GL11.glEnd();
			
			GL11.glColor3f(.5f, .5f, 1f);
			GL11.glBegin(GL11.GL_POINTS);
			for (Vector2f p : currentPath) {
				GL11.glVertex2d(p.x, p.y);
			}
			GL11.glEnd();
		}

		// glPopMatrix();
	}


	@Override
	public String getName() {
		return "Combat Wombat";
	}

	@Override
	public String getTextureResourceName() {
		return "/s0553449/car.png";
	}
}