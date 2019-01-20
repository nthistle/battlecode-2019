package bc19;

public class PreacherBlitzHandler extends RobotHandler
{
	public PreacherBlitzHandler(MyRobot robot)
	{
		super(robot);
	}

    boolean mapSymmetry;
    int origX, origY;
    Direction[][] enemyCastleMap, myCastleMap;

    boolean[][] territoryMap;

    Coordinate[] myTargets;
    int curTarget;
    int receivedIdx;

    boolean needsNewTarget;

    boolean DEBUG = true;

    public void setup() {
		origX = robot.me.x;
		origY = robot.me.y;
        mapSymmetry = Utils.getSymmetry(robot.map, robot.karboniteMap, robot.fuelMap);
        // TODO: for now we just target our reflected spawn loc, in future actually use presumed enemy castle loc
        myCastleMap = Utils.getDirectionMap(robot.map, Coordinate.fromRobot(robot.me));
        enemyCastleMap = Utils.getDirectionMap(robot.map, Utils.getReflected(robot.map, Coordinate.fromRobot(robot.me), mapSymmetry));
        territoryMap = Utils.getTerritoryMap(robot.map, Coordinate.fromRobot(robot.me), mapSymmetry);

        myTargets = new Coordinate[3];
        curTarget = -1;
        receivedIdx = -1;
        needsNewTarget = false;
    }

    public Action turn() {
        // first look for closest enemy
        Coordinate myLoc = Coordinate.fromRobot(robot.me);
        int[][] robotMap = robot.getVisibleRobotMap();

        Robot nearestEnemy = null;
        int nearestDist = -1;

        // see if our castle sent us new targets
        for (Robot r : robot.getVisibleRobots()) {
            if (robot.isRadioing(r)) {
                if (!territoryMap[r.y][r.x]) {
                    continue;
                }
                // sending bottom 4 bits as 0001 indicates "new loc"
                if ((r.signal & 15) == 1) {
                    if (DEBUG) robot.log("detected receiving new location order!");
                    if (receivedIdx < 2) {
                        int receivedX = (r.signal >> 4) & 63;
                        int receivedY = (r.signal >> 10) & 63;
                        Coordinate receivedCoord = new Coordinate(receivedX, receivedY);
                        if (DEBUG) robot.log("parsed location order as " + receivedCoord);
                        myTargets[++receivedIdx] = receivedCoord;
                    } else {
                        robot.log("receiving too many location orders???");
                    }
                }
            }
        }

        Coordinate pTarg;
        for (int i = 0; i <= receivedIdx; i++) {
            pTarg = myTargets[i];
            if (pTarg == null) continue; // already been cleared
        // for (Coordinate pTarg : myTargets) {
            if (Utils.getDistance(myLoc, pTarg) <= 16) { // my vision radius
                // check to see if target is kill
                if (robotMap[pTarg.y][pTarg.x] == 0) {
                    myTargets[i] = null; // clear target
                    needsNewTarget = true;
                }
            }
        }

        if (needsNewTarget) { // let's assign a new target
            for (int i = 0; i <= receivedIdx; i++) {
                if (myTargets[i] != null) {
                    if (DEBUG) robot.log("Successfully got new target!");
                    enemyCastleMap = Utils.getDirectionMap(robot.map, myTargets[i]);
                    needsNewTarget = false;
                    break;
                }
            }
        }

        for (Robot r : robot.getVisibleRobots()) {
            if (r.team != robot.me.team) {
				int dist = Utils.getDistance(myLoc, Coordinate.fromRobot(r));
                if (nearestEnemy == null || dist < nearestDist) {
                    nearestEnemy = r;
                    nearestDist = dist;
                }
            }
        }

        if (nearestEnemy != null && nearestDist <= 10) { // so that we can move into blast range
            // this is the scenario where we attack
            Direction bestTarget = null;
            int bestHits = 0;
            for (int i = -4; i <= 4; i++) { // y
                for (int j = -4; j <= 4; j++) { // x
                    int totalHits = 0;
                    // this is disgustingly inefficient but hopefully fast enough and I'm tired rn
                    for (int k = i-1; k <= i+1; k++) {
                        for (int w = j-1; w <= j+1; w++) {
                            if (!Utils.isInRange(robot.map, robot.me.x + w, robot.me.y + k)) continue;
                            if (robotMap[robot.me.y + k][robot.me.x + w] > 0) {
                                if (robot.getRobot(robotMap[robot.me.y + k][robot.me.x + w]).team != robot.me.team) {
                                    totalHits += 1;
                                } else {
                                    totalHits -= 2; // twice as bad to hit ourselves as to hit an enemy
                                }
                            }
                        }
                    }
                    if (totalHits > bestHits) {
                        bestHits = totalHits;
                        bestTarget = new Direction(j, i); // ORDER!!
                    }
                }
            }
            robot.log("Best target is " + bestTarget + " with " + bestHits + " total hits!");
            if (bestTarget != null) {
                return robot.attack(bestTarget.dx, bestTarget.dy);
            } else {
                robot.log("ERROR HOW DID WE GET TO THIS SCENARIO");
            }
        }
        //robot.log("FUCK4");

        // if we have over 70, move fast, otherwise don't
        int usableFuel = (robot.fuel > 70 ? 4 : 2);

		//this should be the same as "path towards them"
        Direction d = Utils.followDirectionMap(enemyCastleMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);

        int ctr = 0;
        while (d.equals(Utils.STATIONARY) && (ctr++) < 10) {
            d = Utils.followDirectionMapFuzz(enemyCastleMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);
        }

        return robot.move(d.dx, d.dy);
    }
}