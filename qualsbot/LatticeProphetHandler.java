package bc19;
import java.util.ArrayList; 

public class LatticeProphetHandler extends RobotHandler
{
	public LatticeProphetHandler(MyRobot robot)
	{
		super(robot);
	}

    boolean mapSymmetry;
    int origX, origY;
    Direction[][] enemyCastleMap, myCastleMap;
    Coordinate myCastle;
    Coordinate myTarget;
    Direction myDirection; // pick one of the four directions to head

    public void setup() {
		origX = robot.me.x;
		origY = robot.me.y;
        myCastle = Coordinate.fromRobot(findMyCastle());
        //mapSymmetry = Utils.getSymmetry(robot.map, robot.karboniteMap, robot.fuelMap);
        //myCastleMap = Utils.getDirectionMap(robot.map, myCastle);
        //myTarget = Utils.getReflected(robot.map, myCastle, mapSymmetry);
        //enemyCastleMap = Utils.getDirectionMap(robot.map, myTarget);
        if (origX == myCastle.x){
         myDirection = new Direction(0, (origY - myCastle.y) * 2);   
        }
        else if (origY == myCastle.y){
            myDirection = new Direction((origX - myCastle.x) * 2, 0);
        }
        else{
            if ((int)(Math.random() * 2) == 0){
                myDirection = new Direction((origX - myCastle.x) * 2, 0);
            }
            else{
                myDirection = new Direction(0, (origY - myCastle.y) * 2);
            }
        }
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
        // robot.log ("LOCATION: " + robot.me.x + " " + robot.me.y);
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
        /*
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
        */
        if (nearestEnemy != null && nearestDist <= 64) {
            // attack the nearest enemy. 
            // TODO: Instead of attacking nearest enemy, attack non pilgrim enemies first.
            return robot.attack(nearestEnemy.x - robot.me.x, nearestEnemy.y - robot.me.y);
        }
        
        if ((robot.me.x + robot.me.y) % 2 == 0){ // sum of coordinates is even
            return null; // we're already where we need to be. 
        }

        for (int i=0; i<4; i++){ // check 4 neighboring squares and see if we can move on to them
            int new_x = Utils.directions[i].dx + robot.me.x; 
            int new_y = Utils.directions[i].dy + robot.me.y; 
            if (canMove(new_x, new_y)){
                return robot.move(Utils.directions[i].dx, Utils.directions[i].dy); 
            }
        }

        Direction [] openDirections = new Direction [3]; 
        openDirections[0] = myDirection; 
        
        if (myDirection.dx == 0){ // get directions +- 45 degrees of this direction
            openDirections[1] = new Direction(myDirection.dx + 1, myDirection.dy / 2); 
            openDirections[2] = new Direction(myDirection.dx - 1, myDirection.dy / 2); 
        }
        else{
            openDirections[1] = new Direction(myDirection.dx / 2, myDirection.dy + 1); 
            openDirections[2] = new Direction(myDirection.dx / 2, myDirection.dy - 1); 
        }

        ArrayList <Direction> direcs = new ArrayList<> (); 
        for (int i=0; i<3; i++){ // add all of the 3 directions that are possible
            if (canMove(robot.me.x + openDirections[i].dx, robot.me.y + openDirections[i].dy)) direcs.add(openDirections[i]); 
        }
        if (direcs.size() == 0){ // none of the possible directions worked .. oops
            return null; // just stay here at this point tbh 
        }
        int choice = (int)(Math.random() * direcs.size()); // pick one of the open direcs
        Direction toGo = direcs.get(choice);
        return robot.move(toGo.dx, toGo.dy); 
    }
    boolean canMove (int new_x, int new_y){
        if (Utils.isInRange(robot.map, new_x, new_y) && Utils.isPassable(robot.map, new_x, new_y)){ // make sure its in range and on a passable square
                if (!Utils.isPassable(robot.karboniteMap, new_x, new_y) && !Utils.isPassable(robot.fuelMap, new_x, new_y)){ // make sure we're not on a karbonite or fuel deposit
                    if (!Utils.isOccupied(robot.getVisibleRobotMap(), new_x, new_y)){ // make sure we're not on other robots
                        return true; 
                    }
                }
        }
        return false; 
    }
}