package bc19;

public class PilgrimHandler extends RobotHandler 
{
    public PilgrimHandler(MyRobot robot) {
        super(robot);
    }

    boolean isKarbonitePilgrim; // TODO

    Direction[][] targetMap;
    Direction[][] castleMap;

    Coordinate targetLocation;
    Coordinate castleLocation;

    public void setup() {
        Coordinate myLoc = Coordinate.fromRobot(robot.me);

        int closestCastleDistance = -1;
        for (Robot r : robot.getVisibleRobots()) {
            if (r.team == robot.me.team && r.unit == robot.SPECS.CASTLE) {
                if (castleLocation == null || Utils.getDistance(myLoc, Coordinate.fromRobot(r)) < closestCastleDistance) {
                    castleLocation = Coordinate.fromRobot(r);
                    closestCastleDistance = Utils.getDistance(myLoc, castleLocation);
                }
            }
        }
        robot.log("Identified Home Castle as " + castleLocation + ", with distance " + closestCastleDistance);

        int closestFuelDistance = -1;

        boolean[][] fuelMap = robot.getFuelMap();
        for (int y = 0; y < fuelMap.length; y++) {
            for (int x = 0; x < fuelMap[y].length; x++) {
                if (!fuelMap[y][x]) continue;
                if (targetLocation == null || Utils.getDistance(myLoc, new Coordinate(x, y)) < closestFuelDistance) {
                    targetLocation = new Coordinate(x, y);
                    closestFuelDistance = Utils.getDistance(myLoc, targetLocation);
                }
            }
        }

        robot.log("Identified closest Fuel Source as " + targetLocation + ", with distance " + closestFuelDistance);

        //robot.log(robot.map);

        // Utils.getDirectionMap(robot.map, targetLocation.x, targetLocation.y);

        targetMap = Utils.getDirectionMap(robot.map, targetLocation.x, targetLocation.y);
        // castleMap = Utils.getDirectionMap(robot.map, myLoc.x, myLoc.y); // assumes we just want to nav back to our spawn, should be okay
        castleMap = Utils.getDirectionMap(robot.map, castleLocation.x, castleLocation.y); // assumes we just want to nav back to our spawn, should be okay

        robot.log("Pilgrim setup called!");
    }

    public Action turn() {
        //robot.log("Pilgrim turn called!");

        //Direction d = Utils.getRandomDirection();
        //return robot.move(d.dx, d.dy);

        if (robot.me.karbonite >= 20 || robot.me.fuel >= 100) {
            // navigate back to base

            // if base nearby, handoff
            for (Robot r : robot.getVisibleRobots()) {
                if (r.team == robot.me.team && r.unit == robot.SPECS.CASTLE) {
                    if (Utils.getDistance(robot.me.x, robot.me.y, r.x, r.y) <= 2) {
                        robot.log("Handing off Karbonite/Fuel!");
                        return robot.give(r.x - robot.me.x, r.y - robot.me.y, robot.me.karbonite, robot.me.fuel);
                    }
                }
            }

            // just walk towards base
            Direction d = Utils.followDirectionMap(castleMap, robot.getVisibleRobotMap(), 4, robot.me.x, robot.me.y);
            robot.log("Trying to move " + d);
            return robot.move(d.dx, d.dy);
            // return robot.move(castleMap[robot.me.y][robot.me.x].dx, castleMap[robot.me.y][robot.me.x].dy);
        } else {
            // navigate towards karb/fuel and mine if applicable

            if (robot.getFuelMap()[robot.me.y][robot.me.x] || robot.getKarboniteMap()[robot.me.y][robot.me.x]) {
                return robot.mine();
            } 

            Direction d = Utils.followDirectionMap(targetMap, robot.getVisibleRobotMap(), 4, robot.me.x, robot.me.y);
            robot.log("Trying to move " + d);
            return robot.move(d.dx, d.dy);
            // return robot.move(targetMap[robot.me.y][robot.me.x].dx, targetMap[robot.me.y][robot.me.x].dy);
        }
    }
}