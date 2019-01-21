package bc19;

public class CrusaderHandler extends RobotHandler 
{
    public CrusaderHandler(MyRobot robot) {
        super(robot);
    }

    boolean mapSymmetry;
    Direction[][] presumedMap, myCastleMap;
    Coordinate myCastle;
    Coordinate myTarget;

    public void setup() {
        myCastle = Coordinate.fromRobot(findMyCastle());
        mapSymmetry = Utils.getSymmetry(robot.map, robot.karboniteMap, robot.fuelMap);
        // TODO: for now we just target our reflected spawn loc, in future actually use presumed enemy castle loc
        myTarget = Utils.getReflected(robot.map, myCastle, mapSymmetry);
        myCastleMap = Utils.getDirectionMap(robot.map, myCastle);
        presumedMap = Utils.getDirectionMap(robot.map, myTarget);
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
        int usableFuel = (robot.fuel > 150 ? 9 : (robot.fuel > 85 ? 4 : 2));

        Coordinate myLoc = Coordinate.fromRobot(robot.me);

        Robot nearestEnemy = null;
        int nearestDist = -1;

        for (Robot r : robot.getVisibleRobots()) {
            if (r.team != robot.me.team) {
                if (nearestEnemy == null || Utils.getDistance(myLoc, Coordinate.fromRobot(r)) < nearestDist) {
                    nearestEnemy = r;
                    nearestDist = Utils.getDistance(myLoc, Coordinate.fromRobot(r));
                }
            }
        }

        if (Utils.getDistance(myLoc, myTarget) <= 49) {
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
                    presumedMap = Utils.getDirectionMap(robot.map, myTarget);
                }
            }
        }

        if (nearestEnemy != null) {

            if (nearestDist <= 16) {
                // attack range, blast them
                return robot.attack(nearestEnemy.x - robot.me.x, nearestEnemy.y - robot.me.y);
            }

            // path towards them, could be slightly time-intensive, but 64^2 is pretty trivial
            Direction[][] targetMap = Utils.getDirectionMap(robot.map, nearestEnemy.x, nearestEnemy.y);

            Direction netDir = Utils.followDirectionMap(targetMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);
            return robot.move(netDir.dx, netDir.dy);
        }

        Direction d = Utils.followDirectionMap(presumedMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);

        int ctr = 0;
        while (d.equals(Utils.STATIONARY) && (ctr++) < 10) {
            d = Utils.followDirectionMapFuzz(presumedMap, robot.getVisibleRobotMap(), usableFuel, robot.me.x, robot.me.y);
        }
        // Direction d = Utils.getRandomDirection();
        return robot.move(d.dx, d.dy);
    }
}