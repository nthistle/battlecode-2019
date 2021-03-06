package bc19;

public class CrusaderHandler extends RobotHandler 
{
    public CrusaderHandler(MyRobot robot) {
        super(robot);
    }

    boolean mapSymmetry;
    Direction[][] presumedMap;

    public void setup() {
        mapSymmetry = Utils.getSymmetry(robot.map, robot.karboniteMap, robot.fuelMap);
        // TODO: for now we just target our reflected spawn loc, in future actually use presumed enemy castle loc
        presumedMap = Utils.getDirectionMap(robot.map, Utils.getReflected(robot.map, Coordinate.fromRobot(robot.me), mapSymmetry));
    }

    public Action turn() {
        // first look for closest enemy
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

        if (nearestEnemy != null) {

            if (nearestDist <= 16) {
                // attack range, blast them
                return robot.attack(nearestEnemy.x - robot.me.x, nearestEnemy.y - robot.me.y);
            }

            // path towards them, could be slightly time-intensive, but 64^2 is pretty trivial
            Direction[][] targetMap = Utils.getDirectionMap(robot.map, nearestEnemy.x, nearestEnemy.y);

            Direction netDir = Utils.followDirectionMap(targetMap, robot.getVisibleRobotMap(), 9, robot.me.x, robot.me.y);
            return robot.move(netDir.dx, netDir.dy);
        }

        Direction d = Utils.followDirectionMap(presumedMap, robot.getVisibleRobotMap(), 9, robot.me.x, robot.me.y);
        // Direction d = Utils.getRandomDirection();
        return robot.move(d.dx, d.dy);
    }
}