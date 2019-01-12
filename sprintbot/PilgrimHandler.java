package bc19;

public class PilgrimHandler extends RobotHandler 
{
    public PilgrimHandler(MyRobot robot) {
        super(robot);
    }

    boolean isKarbonitePilgrim; // TODO

    Direction[][] targetMap;
    Direction[][] castleMap;

    //the location of the site I am mining (right now we consider pilgrims miners)
    Coordinate targetLocation;

    //the location of the castle site I am connected to (later this could be a church)
    Coordinate castleLocation;

    public void setup() {

        //establish a coordinate of where I am when first created
        Coordinate myLoc = Coordinate.fromRobot(robot.me);

        //below established my home castle based on what is the closest to me on my creation
        int closestCastleDistance = -1;

        //store the castle I am assigned to (this is just for the one turn)
        Robot myCastle; 

        // TODO: need to handle case where pilgrim is adjacent to 2 castles
        // SOLVED
        
        boolean[][] zMap = robot.getKarboniteMap();

        for (Robot r : robot.getVisibleRobots()) {

            //check if this robot is a castle on my team
            if (r.team == robot.me.team && r.unit == robot.SPECS.CASTLE) {

                //if the castle is the closer than the current closest, update it
                if (castleLocation == null || 
                    Utils.getDistance(myLoc, Coordinate.fromRobot(r)) < closestCastleDistance) {

                    //assign the castlelocation to robot r's coordinate, similarly assign distance
                    castleLocation = Coordinate.fromRobot(r);
                    closestCastleDistance = Utils.getDistance(myLoc, castleLocation);
                    
                    if (r.signal != -1){ // make sure we have a signal
                        int rX = r.signal >> 10; // X location of signaled pilgrim
                        int rY = (r.signal >> 4) & (63); // Y loc of signaled pilgrim
                        int resource_type = (r.signal >> 3) & 1; // resource they should deliver
                        if (rX == robot.me.x && rY == robot.me.y){ // we have the right pilgrim
                            if (resource_type == 1) zMap = robot.getFuelMap(); 

                            //assign closest castle
                            myCastle = r;
                            robot.log("RESOURCE_TYPE: " + resource_type);
                        }
                    }
                }
            }
        }

        /*
        //NOTE: above can find issues is two castles are equal distance from me
        //    this can be fixed by having convention on where robots spawn (or other things as well)

        //report the castle we have found
        robot.log("Identified Home Castle as " + castleLocation + ", with distance " + closestCastleDistance);

        //assume i receive my location from my castle as a 16 bit string
        //    first  6 bits are the x coordinate
        //    second 6 bits are the y coordinate

        //grab the x coordinate by just bitshifting to the left 12
        int targetX = myCastle.signal >> 10; 

        //grab the y coordinate by bitshifting to the right 4 
        //    chop off the first 6 bits by taking and with the bistring "111111"
        int targetY = (myCastle.signal >> 4) & (63);

        //set the target location for good
        targetLocation =  new Coordinate(targetX, targetY);

        //create the movement map for the target spot
        targetMap = Utils.getDirectionMap(robot.map, targetLocation.x, targetLocation.y);

        //create the movement map to get back to my home castle
        castleMap = Utils.getDirectionMap(robot.map, castleLocation.x, castleLocation.y); 

        robot.log("Pilgrim setup called!");
        */

        int closestDistance = -1;

        for (int y = 0; y < zMap.length; y++) {
            for (int x = 0; x < zMap[y].length; x++) {
                if (!zMap[y][x]) continue;
                if (targetLocation == null || Utils.getDistance(myLoc, new Coordinate(x, y)) < closestDistance) {
                    targetLocation = new Coordinate(x, y);
                    closestDistance = Utils.getDistance(myLoc, targetLocation);
                }
            }
        }

        robot.log("Identified closest Fuel Source as " + targetLocation + ", with distance " + closestDistance);

        //robot.log(robot.map);

        // Utils.getDirectionMap(robot.map, targetLocation.x, targetLocation.y);

        targetMap = Utils.getDirectionMap(robot.map, targetLocation.x, targetLocation.y);
        // castleMap = Utils.getDirectionMap(robot.map, myLoc.x, myLoc.y); // assumes we just want to nav back to our spawn, should be okay
        castleMap = Utils.getDirectionMap(robot.map, castleLocation.x, castleLocation.y); // assumes we just want to nav back to our spawn, should be okay

        robot.log("Pilgrim setup called!");
    }

    public Action turn() {
        //robot.log("Pilgrim turn called!");

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