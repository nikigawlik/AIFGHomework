package s0553449;

import java.awt.Point;
import java.awt.Polygon;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector4f;

import lenz.htw.ai4g.ai.AI;
import lenz.htw.ai4g.ai.DriverAction;
import lenz.htw.ai4g.ai.Info;

import static s0553449.FuzzyLogic.*;

public class MyCarMain extends AI {

	protected float brakeAngle = 6.14f;
	protected float maxAngleSpd = 50f;
	protected float approachPower = 1.62f;
	protected float turnVelocity = 6f;
	
	protected float targetWeight = 1f;
	protected float obstacleWeight = 10f;
	
	protected float feelerDistance = 20f;
	protected float feelerDistanceSides = 20f;
	protected float feelerAngle = (float) Math.PI/4;

	protected float stuckCheckFeelerDistance = 25f;

	private Vector2f positionLastFrame;
	private Vector2f position;

	// pathfinding related
	protected float cornerOffset = 30;
	protected float cornerCalcMargin = 7f;
	protected float cornerPostOffset = 0;//10f;
	protected int smoothingIterations = 4;

	protected float maxDistanceFromPath = 40f;	
	protected float targetPointShift = 60f;
	protected float minTargetPointShift = 10f;
	
	protected boolean doDebug = true;
	
	private String debugStr = "";
	DecimalFormat debugFormat = new DecimalFormat( "#,###,###,##0.0000" );
	
	private LevelGraph levelGraph; // graph without start and goal nodes
	private Node currentGoal;

	private float number = 0;

	// display specific stuff
	private Vector2f[] debugFeelers;
	private Vector2f[] debugPoints = new Vector2f[0]; // array of points to draw
	private Vector4f[] debugLines = new Vector4f[0]; // array of lines to draw

	private ArrayList<Float> debugFloats;

	private Node[] currentPath;

	public MyCarMain(Info info) {
		super(info);
		init();
		positionLastFrame = new Vector2f(info.getX(), info.getY());
	}

	protected void init() {
		enlistForTournament(553449);
		calculateGraph();
	}

	protected void calculateGraph() {
		// calculate outer points
		Node[] nodes = findNodes();
		levelGraph = new LevelGraph(nodes, info.getTrack().getObstacles(), cornerOffset - cornerCalcMargin);
		// calculate edges
		levelGraph.calculateVisibilityGraph();

		debugLines = levelGraph.getEdges();
		debugPoints = nodes;
	}

	/**
	 * extract corner points from level grometry to make up the level graph
	 */
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
			
				// test if inner angle of corner < 180 deg
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

		position = new Vector2f(info.getX(), info.getY());

		Point checkpoint = info.getCurrentCheckpoint();
		Node checkpointNode = new Node(checkpoint.x, checkpoint.y, 0, 0);

		if(levelGraph != null && (currentGoal == null || !checkpointNode.equals(currentGoal))) {
			currentPath = calculatePathTo(checkpointNode);
		}
		
		DriverAction steering = doSteering();

		positionLastFrame = new Vector2f(info.getX(), info.getY());

		return steering;
	}

	private Node[] calculatePathTo(Node goal) {
		currentGoal = goal;
		Vector2f currentPos = new Vector2f(info.getX(), info.getY());

		// find path from here to there
		Node[] path = levelGraph.findPath(currentPos, currentGoal);
		if(path == null) {
			path = new Node[2];
			path[0] = new Node(currentPos);
			path[1] = new Node(currentGoal);
		}

		// recalculate current path

		LinkedList<Node> list = new LinkedList<>();

		// split every corner into to corners and shift the 
		// coneres along the edge normals to prepare this
		// path for smoothing
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
		list = smoothNodes(list, smoothingIterations);

		return list.toArray(path);
	}

	/**
	 * smoothes the graph, turning it into a curve
	 */
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
		ArrayList<Vector2f> debugFeelersList = new ArrayList<>();
		debugFloats = new ArrayList<>();

		
		number++;
		debugFloat(number);

		float throttle = info.getMaxAcceleration();
		float steering = 0f;
		
		position.x = info.getX();
		position.y = info.getY();

		Vector2f target = getCurrentTargetPoint();
		Vector2f checkpoint = new Vector2f(info.getCurrentCheckpoint().x, info.getCurrentCheckpoint().y);
		Vector2f toTarget = new Vector2f(target);
		Vector2f.sub(toTarget, position, toTarget);
		float targetAngle = (float)Math.atan2(toTarget.y, toTarget.x);

		float distanceToTarget = GeometryUtils.distanceBetweenPoints(position, target);

		float lookAngle = info.getOrientation();
		Vector2f look = new Vector2f((float) Math.cos(lookAngle), (float) Math.sin(lookAngle));

		Vector2f vel = info.getVelocity();
		if (vel.length() == 0) {
			vel = new Vector2f(look);
		}
		float velAngle = (float)Math.atan2(vel.y, vel.x);
		
		float closeToCheckpoint = fuzzLinear(
			GeometryUtils.distanceBetweenPoints(position, checkpoint), 
			30,  
			0
		);

		debugFloat(closeToCheckpoint);

		// fuzzy logic

		float spinningLeft = fuzzLinear(info.getAngularVelocity(), 0, info.getMaxAngularVelocity());
		float spinningRight = fuzzLinear(info.getAngularVelocity(), 0, -info.getMaxAngularVelocity());
		float spinning = or(spinningLeft, spinningRight);

		float whatIsClose = 1.9f; // angle counts as "close" 
		float whatIsCloseLower =0.1f; // angle counts as "close" 
		
		float velToTargetAngle = GeometryUtils.deltaAngle(velAngle, targetAngle);
		// velToTargetAngle += Math.signum(lookToTargetAngle) * -GeometryUtils.PI/2f;

		float movingLeftOfTarget = fuzzLinear(velToTargetAngle, -whatIsCloseLower, -whatIsClose);
		float movingRightOfTarget = fuzzLinear(velToTargetAngle, whatIsCloseLower, whatIsClose);
		float movingToTarget = and(not(movingLeftOfTarget), not(movingRightOfTarget));
		
		float usualSpeed = 0.5f;
		float amFast = fuzzLinear(vel.length(), info.getMaxVelocity() * usualSpeed, info.getMaxVelocity());
		float amMoving = fuzzLinear(vel.length(), 0, info.getMaxVelocity() * usualSpeed);

		float onTarget = and(amMoving, movingToTarget); 

		float slide = not(onTarget);
		
		float lookToTargetAngle = GeometryUtils.deltaAngle(lookAngle, targetAngle);

		lookToTargetAngle += Math.signum(lookToTargetAngle) * GeometryUtils.PI/2f * slide;

		float arriveDeltaAngle = 0.5f ;//+ 0.5f * (info.getVelocity().length() / info.getMaxVelocity());
		float arriveDeltaLow = arriveDeltaAngle * 0.98f;

		// float seekingLeft = fuzzLinear(lookToTargetAngle, -arriveDeltaAngle, -GeometryUtils.PI);
		// float seekingRight = fuzzLinear(lookToTargetAngle, arriveDeltaAngle, GeometryUtils.PI);
		float seekingLeft = fuzzLinear(lookToTargetAngle, -arriveDeltaLow, -arriveDeltaAngle);
		float seekingRight = fuzzLinear(lookToTargetAngle, arriveDeltaLow, arriveDeltaAngle);
		float seeking = or(seekingLeft, seekingRight);

		float arrivingLeft = and(fuzzLinear(lookToTargetAngle, 0, -arriveDeltaLow), not(seekingLeft));
		float arrivingRight = and(fuzzLinear(lookToTargetAngle, 0, arriveDeltaLow), not(seekingRight));
		float arriving = or(arrivingLeft, arrivingRight);


		// float angleToP = Math.signum(lookToTargetAngle) * (GeometryUtils.PI/2f - Math.abs(lookToTargetAngle));
		// float rightOfP = fuzzLinear(angleToP, GeometryUtils.PI/2f, 0);
		// float leftOfP = fuzzLinear(angleToP, -GeometryUtils.PI/2f, 0);

		float velLeft = or(arrivingRight, seekingRight);
		float velRight = or(arrivingLeft, seekingLeft);

		// float steerLeft = 0;//and(movingRightOfTarget, not(spinningLeft));
		// float steerRight = 0;//and(movingLeftOfTarget, not(spinningRight));

		float canStop = not(spinning);

		float driveFast = and(or(not(amMoving), movingToTarget), not(closeToCheckpoint));
		float driveSlow = 0; //and(amFast, not(onTarget));

		debugFloat(driveFast);
		debugFloat(driveSlow);

		float targetSpeed = defuzzLinear(driveFast, 0f, info.getMaxVelocity()) 
			+ defuzzLinear(driveSlow, 0f, -info.getMaxVelocity());

		float forwardSpeed = Vector2f.dot(info.getVelocity(), look);
		throttle = targetSpeed - forwardSpeed;

		float targetAngularVelocity = defuzzLinear(velRight, 0, -info.getMaxAngularVelocity())
			+ defuzzLinear(velLeft, 0, info.getMaxAngularVelocity());

		steering = targetAngularVelocity - info.getAngularVelocity();

		debugFloat(forwardSpeed);
		debugFloat(targetSpeed);
		debugFloat(throttle);

		
		// debugFloat(throttle);
		// debugFloat(steering);
		
		// debug section

		debugStr += "lookToTargetAngle: " + debugFormat.format(lookToTargetAngle) + "\n";
		debugStr += "velToTargetAngle: " + debugFormat.format(velToTargetAngle) + "\n";
		debugStr += "current angleV: " + debugFormat.format(info.getAngularVelocity()) + "\n";
		debugStr += "current vel: " + debugFormat.format(info.getVelocity().length()) + "\n";

		debugStr += "steering chk: " + debugFormat.format(steering/info.getMaxAngularAcceleration()) + "\n";
		debugStr += "throttle chk: " + debugFormat.format(throttle/info.getMaxAcceleration()) + "\n";

		debugFeelersList.add(target);

		debugFeelers = new Vector2f[debugFeelersList.size()];
		debugFeelers = debugFeelersList.toArray(debugFeelers);
		
		return new DriverAction(throttle, steering);
	}

	protected Vector2f getCurrentTargetPoint() {
		Vector2f p = null; //info.getCurrentCheckpoint();

		Vector2f pPoint = new Vector2f(position);

		if(currentPath != null) {
			Vector2f minPoint = currentPath[0];
			float minDist = Float.MAX_VALUE;
			int lineIndex = 0;

			for (int i = 0; i < currentPath.length-1; i++) {
				Vector2f point = GeometryUtils.closestPointOnLineSegment(currentPath[i], currentPath[i+1], pPoint);

				float dist = GeometryUtils.distanceBetweenPoints(point, pPoint);

				if(dist < minDist) {
					minPoint = point;
					minDist = dist;
					lineIndex = i;
				}
			}

			float distanceToP = GeometryUtils.distanceBetweenPoints(minPoint, pPoint);

			float shift = minTargetPointShift + (targetPointShift - minTargetPointShift) 
				* Math.max(0f, 1f - distanceToP/maxDistanceFromPath) 
				* (info.getVelocity().length() / info.getMaxVelocity());

			p = shiftPointAlongPath(currentPath, minPoint, lineIndex, shift);

			if(levelGraph.testLineAgainstLevel(pPoint, p, false)) {
				// redo the search, but include collision checks
				minPoint = currentPath[0];
				minDist = Float.MAX_VALUE;
				lineIndex = 0;
				for (int i = 0; i < currentPath.length-1; i++) {
					Vector2f point = GeometryUtils.closestPointOnLineSegment(currentPath[i], currentPath[i+1], pPoint);
	
					float dist = GeometryUtils.distanceBetweenPoints(point, pPoint);
	
					if(dist < minDist && !levelGraph.testLineAgainstLevel(pPoint, point, false)) {
						minPoint = point;
						minDist = dist;
						lineIndex = i;
					}
				}

				p = shiftPointAlongPath(currentPath, minPoint, lineIndex, minTargetPointShift);
			}
		}

		return p;
	}

	private Vector2f shiftPointAlongPath(Vector2f[] path, Vector2f p, int index, float distance) {
		index++;
		while(true) {
			if(index >= path.length) {
				return p;
			}
			Vector2f next = path[index];
			float distToNext = GeometryUtils.distanceBetweenPoints(p, next);
			if(distToNext > distance) {
				// handle rest management
				Vector2f dir = new Vector2f(next);
				Vector2f.sub(dir, p, dir);
				dir.normalise();
				dir.scale(distance);
				Vector2f.add(dir, p, dir);
				return dir;
			} else {
				distance -= distToNext;
				index++;
			}
			p = next;
		}
	}

	@Override
	public void doDebugStuff() {
		if(doDebug) {

			if(debugStr.compareTo("") != 0) {
				System.out.println(debugStr);
			}
			
			if (debugFeelers != null && debugFeelers.length != 0) {
				for (int i = 0; i < debugFeelers.length; i++) {
					GL11.glColor3f(0.0f, 1.0f, 0.2f);
					GL11.glBegin(GL11.GL_LINE_STRIP);
					
					GL11.glVertex2d(position.x, position.y);
					GL11.glVertex2d(debugFeelers[i].x, debugFeelers[i].y);
					GL11.glEnd();
				}
			}
			
			// misc debug lines
			GL11.glColor3f(1.0f, 0.2f, 0.0f);
			GL11.glBegin(GL11.GL_LINES);
			
			GL11.glVertex2d(position.x, position.y);
			GL11.glVertex2d(position.x + info.getVelocity().x, position.y + info.getVelocity().y);
			GL11.glEnd();
		
			// misc debug points
			GL11.glColor3f(1.0f, 0.2f, 0.0f);
			GL11.glPointSize(16f);
			GL11.glBegin(GL11.GL_POINTS);
			
			GL11.glVertex2d(getCurrentTargetPoint().x, getCurrentTargetPoint().y);
			GL11.glEnd();
			GL11.glPointSize(8f);
			
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

			// FLOATS!
			if(debugFloats != null) {
				GL11.glColor3f(0, 0, 0);
				for(int i = 0; i < debugFloats.size(); i++) {
					NumberDisplay.drawFloat(debugFloats.get(i), position.x + 20, position.y - 1.5f * i);
				}
			}
		}
	}

	private void debugFloat(float f) {
		debugFloats.add(f);
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