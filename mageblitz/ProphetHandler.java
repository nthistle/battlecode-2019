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
    Direction originalDir;

    public void setup() {
		origX = robot.me.x;
		origY = robot.me.y;
        mapSymmetry = Utils.getSymmetry(robot.map, robot.karboniteMap, robot.fuelMap);
        // TODO: for now we just target our reflected spawn loc, in future actually use presumed enemy castle loc
        myCastleMap = Utils.getDirectionMap(robot.map, Coordinate.fromRobot(robot.me));
        enemyCastleMap = Utils.getDirectionMap(robot.map, Utils.getReflected(robot.map, Coordinate.fromRobot(robot.me), mapSymmetry));
        originalDir = Utils.getRandom8Direction();
    }

    public Action turn() {
        // first look for closest enemy
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

        if (nearestEnemy != null) {

			//the enemy is not too close (or you can't kite back more), so attack
            if (nearestDist <= 64 && (nearestKitableEnemyDist > 16 || (robot.me.x==origX && robot.me.y==origY))) {
                return robot.attack(nearestEnemy.x - robot.me.x, nearestEnemy.y - robot.me.y);
            }

            // path towards the enemy castle (to be implemented: have someone guide the prophet instead of it just blindly moving towards the enemy)
			if(nearestKitableEnemyDist > 64) {
				Direction netDir = Utils.followDirectionMap(enemyCastleMap, robot.getVisibleRobotMap(), 4, robot.me.x, robot.me.y);
				while(netDir.dx==0 && netDir.dy==0) //we're at our target and nothing has happened, so target a new square
				{
					int targetX = (int)(Math.random() * (robot.map.length + 0.99999));
					int targetY = (int)(Math.random() * (robot.map.length + 0.99999));
					//enemyCastleMap is just the goal we move towards when we have nothing else to do, not actually the enemy castle location
					enemyCastleMap = Utils.getDirectionMap(robot.map, new Coordinate(targetX, targetY));
					netDir = Utils.followDirectionMap(enemyCastleMap, robot.getVisibleRobotMap(), 4, robot.me.x, robot.me.y);
				}
				return robot.move(netDir.dx, netDir.dy);
			}

			// otherwise path away from them to kite
            else {
				Direction netDir = Utils.followDirectionMap(myCastleMap, robot.getVisibleRobotMap(), 4, robot.me.x, robot.me.y);
            	return robot.move(netDir.dx, netDir.dy);
			}
        }

        // making them a little less aggressive here
        if (Math.random() < 0.3) {
            Direction d = Utils.followDirectionMap(enemyCastleMap, robot.getVisibleRobotMap(), 4, robot.me.x, robot.me.y);
            return robot.move(d.dx, d.dy);
        } else {
            if (Math.random() < 0.5) {
                return robot.move(originalDir.dx, originalDir.dy);
            } else {
                Direction dd = Utils.getRandom8Direction();
                return robot.move(dd.dx, dd.dy);
            }
        }

		//this should be the same as "path towards them"
        //Direction d = Utils.followDirectionMap(enemyCastleMap, robot.getVisibleRobotMap(), 4, robot.me.x, robot.me.y);
        //return robot.move(d.dx, d.dy);
    }
}