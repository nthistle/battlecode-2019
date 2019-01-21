package bc19;

public class ProphetHandler extends RobotHandler
{
	public ProphetHandler(MyRobot robot)
	{
		super(robot);
	}

    boolean mapSymmetry;
    int origX, origY;
    Direction[][] enemyCastleMap, myCastleMap;
    Coordinate myCastle;
    Coordinate myTarget;

    public void setup() {
		origX = robot.me.x;
		origY = robot.me.y;
        myCastle = Coordinate.fromRobot(findMyCastle());
        mapSymmetry = Utils.getSymmetry(robot.map, robot.karboniteMap, robot.fuelMap);
        myCastleMap = Utils.getDirectionMap(robot.map, myCastle);
        myTarget = Utils.getReflected(robot.map, myCastle, mapSymmetry);
        enemyCastleMap = Utils.getDirectionMap(robot.map, myTarget);
    }

    // lazy and just finds first, but whatever
    public Robot findMyCastle() {
        for (Robot r : robot.getVisibleRobots()) {
            if (robot.isVisible(r) && r.team == robot.me.team && r.unit == robot.SPECS.CASTLE
                && Utils.getDistance(r.x, r.y, robot.me.x, robot.me.y) <= 2) {
                return r;
            }
        }
    }

    public Action turn() {
        // first look for closest enemy
        int usableFuel = (robot.fuel > 100 ? 4 : 2);
        Coordinate myLoc = Coordinate.fromRobot(robot.me);

        Robot nearestEnemy = null;
        int nearestDist = (int)1e9;
        int nearestKitableEnemyDist = (int)1e9;

        for (Robot r : robot.getVisibleRobots()) {
            if (r.team != robot.me.team) {
				int dist = Utils.getDistance(myLoc, Coordinate.fromRobot(r));
				if(r.unit != robot.SPECS.CRUSADER && r.unit != robot.SPECS.PILGRIM && r.unit != robot.SPECS.PROPHET)
					nearestKitableEnemyDist = Math.min(nearestKitableEnemyDist, dist);
                if (nearestEnemy == null || dist < nearestDist) {
                    nearestEnemy = r;
                    nearestDist = dist;
                }
            }
        }

        if (Utils.getDistance(myLoc, myTarget) <= 64) {
            int tid = robot.getVisibleRobotMap()[myTarget.y][myTarget.x];
            if (tid != -1) {
                if (tid == 0 || robot.getRobot(tid).team == robot.me.team) {
                    // reassign new location
                    // for now, rando
                    int ctr = 0;
                    do {
                        myTarget = new Coordinate((int)(Math.random() * robot.map[0].length), (int)(Math.random() * robot.map.length));
                    } while (!robot.map[myTarget.y][myTarget.x] && (ctr++)<20);
                    if (!robot.map[myTarget.y][myTarget.x]) {
                        myTarget = myCastle; // just go home at this point tbh
                    }
                    enemyCastleMap = Utils.getDirectionMap(robot.map, myTarget);
                }
            }
        }

        if (nearestEnemy != null) {

			//the enemy is not too close (or you can't kite back more), so attack
            if (nearestDist <= 64 && (nearestKitableEnemyDist > 16 || (robot.me.x==origX && robot.me.y==origY))) {
                return robot.attack(nearestEnemy.x - robot.me.x, nearestEnemy.y - robot.me.y);
            }

            // path towards the enemy castle (to be implemented: have someone guide the prophet instead of it just blindly moving towards the enemy)
			if(nearestKitableEnemyDist > 64) {
				Direction netDir = Utils.followDirectionMap(enemyCastleMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);
				/*while(netDir.dx==0 && netDir.dy==0) //we're at our target and nothing has happened, so target a new square
				{
					int targetX = (int)(Math.random() * (robot.map.length + 0.99999));
					int targetY = (int)(Math.random() * (robot.map.length + 0.99999));
					//enemyCastleMap is just the goal we move towards when we have nothing else to do, not actually the enemy castle location
					enemyCastleMap = Utils.getDirectionMap(robot.map, new Coordinate(targetX, targetY));
					netDir = Utils.followDirectionMap(enemyCastleMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);
				}*/ // tarker this code is mega ass wtf are you doing
				return robot.move(netDir.dx, netDir.dy);
			}

			// otherwise path away from them to kite
            else {
				Direction netDir = Utils.followDirectionMap(myCastleMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);
            	return robot.move(netDir.dx, netDir.dy);
			}
        }

		//this should be the same as "path towards them"
        Direction d = Utils.followDirectionMap(enemyCastleMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);

        int ctr = 0;
        while (d.equals(Utils.STATIONARY) && (ctr++) < 10) {
            d = Utils.followDirectionMapFuzz(enemyCastleMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);
        }

        return robot.move(d.dx, d.dy);
    }
}