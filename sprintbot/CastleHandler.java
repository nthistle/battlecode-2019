package bc19;

import java.util.ArrayList;

public class CastleHandler extends RobotHandler 
{
    public CastleHandler(MyRobot robot) {
        super(robot);
    }

    int ourCastleNum; // IS GUARANTEED to be our order in the turn queue
    int numCastles;
    int[] castleX;
    int[] castleY;
    int[] castleIDs; // NOTE: not guaranteed to be in same order for all 3 castles
    // Also not guaranteed to be fully set until after identification process (if we're 2nd castle)

    boolean receivingCastleInfo;
    boolean hasSymmetricAssigned;

    ArrayList<Coordinate> myAssignedKarbonite;
    ArrayList<Coordinate> myAssignedFuel;
    int[][][] distanceMaps;

    public void setup() {
        receivingCastleInfo = true;
        hasSymmetricAssigned = false;

        // first, we have to detect which number castle we are
        // for this, we just count the number of broadcasting castleTalks with LSB set
        int otherSpawnedUnits = 0;
        ourCastleNum = 0;
        for (Robot r : robot.getVisibleRobots()) {
            if (r.team == robot.me.team && (r.castle_talk & 1) == 1) {
                ourCastleNum += 1;
                if ((r.castle_talk & 2) == 2) { // this bit is only set if a castle that went before
                    // spawned something, so we know to not count it as a castle
                    robot.log("Detected a friendly Castle as having spawned a unit turn 1 before this!");
                    otherSpawnedUnits++;
                }
            }
        }

        robot.log("Detected self as Castle #" + ourCastleNum);
        robot.log("My coordinates are " + robot.me.x + "," + robot.me.y);

        numCastles = -otherSpawnedUnits;
        for (Robot r : robot.getVisibleRobots()) {
            if (r.team == robot.me.team) {
                numCastles += 1;
            }
        }

        robot.log("Detected " + numCastles + " castles!");
        castleX = new int[numCastles];
        castleY = new int[numCastles];
        castleIDs = new int[numCastles];
        distanceMaps = new int[numCastles][][];

        for (int i = 0; i < numCastles; i++) {
            castleX[i] = -1;
            castleY[i] = -1;
        }

        castleX[ourCastleNum] = robot.me.x;
        castleY[ourCastleNum] = robot.me.y;
        castleIDs[ourCastleNum] = robot.me.id;

        // well this is pretty nasty, but devs probably won't increase starting castle count
        if (ourCastleNum == 0) {
            // NOTE: we actually have no idea which is which in the turn queue,
            // but we also don't actually care
            int idx = 1;
            for (Robot r : robot.getVisibleRobots()) {
                if (r.team == robot.me.team && r.id != robot.me.id) {
                    castleIDs[idx++] = r.id;
                }
            }
        } else if (ourCastleNum == 1) {
            // Oof... We won't necessarily know the 3rd castle's ID until we parse castleTalk later in
            // the identification process
            for (Robot r : robot.getVisibleRobots()) {
                if ((r.castle_talk & 1) == 1) {
                    castleIDs[0] = r.id;
                    break;
                }
            }
            if (numCastles > 2) {
                if (otherSpawnedUnits == 0) { // okay, we lucked out, first guy didn't want to spawn anything
                    for (Robot r : robot.getVisibleRobots()) {
                        if (r.id != castleIDs[0] && r.id != castleIDs[1]) {
                            castleIDs[2] = r.id;
                            break;
                        }
                    }
                } else {
                    castleIDs[2] = -1; // we'll have to fill this later
                }
            }
        } else {
            int idx = 0;
            for (Robot r : robot.getVisibleRobots()) {
                if (r.team == robot.me.team && r.id != robot.me.id && (r.castle_talk & 1) == 1) {
                    castleIDs[idx++] = r.id;
                }
            }
        }

        robot.log("Detected other castle IDs as:");
        robot.log("Castles[0] = " + castleIDs[0]);
        if (numCastles > 1)
            robot.log("Castles[1] = " + castleIDs[1]);
        if (numCastles > 2)
            robot.log("Castles[2] = " + castleIDs[2]);
    }

/*
    public int bfs_map (boolean [][] grid, int row, int col){

        Queue qu = new LinkedList();
        Queue paths = new LinkedList();

        boolean [][] vis = new boolean [grid.length][grid[0].length];

        qu.add(new Pair(row, col));

        List<Pair> cur_path = new ArrayList<> ();
        cur_path.add (new Pair(row, col));
        paths.add(cur_path);

        while (qu.size() > 0){
            Pair cur = (Pair) qu.poll();
            List <Pair> path = (List<Pair>) paths.poll();

            if (vis[cur.first][cur.second] == true) continue;
            vis[cur.first][cur.second] = true;
            
            if (grid[cur.first][cur.second]) return path.size();

            for (int i=0; i<dirs.length; i++){
                if (canTraverse(cur.second + dirs[i][1], cur.first + dirs[i][0])){
                    Pair new_loc = new Pair (cur.first + dirs[i][0], cur.second + dirs[i][1]);
                    List <Pair> new_path = new ArrayList <> (path);
                    new_path.add (new_loc);

                    qu.add(new_loc);
                    paths.add(new_path);
                }
            }

        }
        return 100000000;
    }
*/

    public Action turn() {
        robot.log("Starting turn #" + robot.me.turn);
        if (receivingCastleInfo) {
            receiveCastleInfo();
        } else if (!hasSymmetricAssigned) {
            doSymmetricAssignmentScheme();
            hasSymmetricAssigned = true;
            robot.log("I have been assigned " + myAssignedKarbonite.size() + " karb and " + myAssignedFuel.size() + " fuel");
        }
        boolean builtUnitThisTurn = false;
        // NOTE: if you spawn a unit on your first turn as a castle (probably not pilgrim, since those
        // will need to be assigned), you MUST set this boolean so other castles don't get confused




        if (robot.me.turn <= 2) {
            int markerBits = builtUnitThisTurn ? 3 : 1;
            if (robot.me.turn == 1) {
                robot.castleTalk((robot.me.x << 2) | markerBits);
            } else if (robot.me.turn == 2) {
                robot.castleTalk((robot.me.y << 2) | markerBits);
            }
        }
        return null;
    }


    public void doSymmetricAssignmentScheme() {
        // first draw up the distance maps for all the castles
        for (int i = 0; i < numCastles; i++) {
            distanceMaps[i] = Utils.getDistanceMap(robot.map, castleX[i], castleY[i]);
        }

        // TODO: take into account which clusters are closer to enemy
        // TODO: correctly handle tie behavior (it would work, but the ordering of castles that
        // aren't ourselves isn't guaranteed). use smallest castle ID for tiebreak
        myAssignedKarbonite = new ArrayList<Coordinate>();
        myAssignedFuel = new ArrayList<Coordinate>();

        for (int y = 0; y < robot.karboniteMap.length; y++) {
            for (int x = 0; x < robot.karboniteMap[y].length; x++) {
                if (!robot.karboniteMap[y][x]) continue;
                int assignedTo = 0;
                for (int c = 1; c < numCastles; c++) { // technically, ties aren't handled correctly
                    if (distanceMaps[c][y][x] < distanceMaps[assignedTo][y][x]) {
                        assignedTo = c;
                    }
                }
                if (assignedTo == ourCastleNum) {
                    myAssignedKarbonite.add(new Coordinate(x, y));
                }
            }
        }

        for (int y = 0; y < robot.fuelMap.length; y++) {
            for (int x = 0; x < robot.fuelMap[y].length; x++) {
                if (!robot.fuelMap[y][x]) continue;
                int assignedTo = 0;
                for (int c = 1; c < numCastles; c++) { // technically, ties aren't handled correctly
                    if (distanceMaps[c][y][x] < distanceMaps[assignedTo][y][x]) {
                        assignedTo = c;
                    }
                }
                if (assignedTo == ourCastleNum) {
                    myAssignedFuel.add(new Coordinate(x, y));
                }
            }
        }
    }


    public void receiveCastleInfo() {
        for (Robot r : robot.getVisibleRobots()) {
            if (r.team == robot.me.team && r.id != robot.me.id) {
                if ((r.castle_talk & 1) == 1) {
                    // okay, he's sending out SOMETHING
                    int thisCastleNum = -1;
                    for (int i = 0; i < numCastles; i++) {
                        if (r.id == castleIDs[i]) {
                            thisCastleNum = i;
                            break;
                        }
                    }
                    if (thisCastleNum == -1) {
                        // hey, we can identify the previously-unidentified castle now
                        thisCastleNum = 2;
                        castleIDs[2] = r.id;
                    }

                    int sentValue = r.castle_talk >> 2; // signed shouldn't be an issue
                    if (castleX[thisCastleNum] == -1) {
                        castleX[thisCastleNum] = sentValue;
                    } else if (castleY[thisCastleNum] == -1) {
                        castleY[thisCastleNum] = sentValue;
                        robot.log("Detected Castle with ID " + castleIDs[thisCastleNum] + "'s location as " + castleX[thisCastleNum] + "," + castleY[thisCastleNum]);
                    }
                }
            }
        }
        // this check is probably inefficient but I'm tired and don't care
        boolean anyRemaining = false;
        for (int i = 0; i < numCastles; i++) {
            if (castleY[i] == -1) {
                anyRemaining = true;
                break;
            }
        }
        receivingCastleInfo = anyRemaining;
        if (!anyRemaining) {
            robot.log("All done detecting!");
        }
    }
}
    /*






        if (robot.me.turn == 1){
            x_coords = new ArrayList <> (); 
            y_coords = new ArrayList <> (); 
            
            robot.castleTalk(robot.me.x);

            return null;
        }
        if (robot.me.turn == 2){
            Robot[] robots = robot.getVisibleRobots(); 
            for (int i=0; i<robots.length; i++){
                x_coords.add(robots[i].castle_talk);
                robot.castleTalk(robot.me.x); 
            }
            return null; 
        }
        if (robot.me.turn == 3){
            robot.castleTalk (robot.me.y); 
            return null; 
        }
        if (robot.me.turn == 4){
            Robot[] robots = robot.getVisibleRobots(); 
            boolean [][][] getMap = new boolean [2][][];
            getMap[0] = robot.getKarboniteMap(); 
            getMap[1] = robot.getFuelMap(); 

            for (int i=0; i<robots.length; i++){
                y_coords.add(robots[i].castle_talk); 
                robot.castleTalk(robot.me.y); 
            }

            int best_length = 1000000000; 
            int [] best = new int [3];
            for (int i=0; i<2; i++){
                for (int j=0; j<2; j++){
                    for (int k=0; k<2; k++){
                        if (x_coords.size() == 2){
                            if (i+j == 0 || i+j == 2) continue; 
                        }
                        else if (x_coords.size() == 3){
                            if (i+j+k==0 || i+j+k==3) continue;
                        } 

                        int m1 = (x_coords.size() < 1) ? (0) : (bfs_map (getMap[i], y_coords.get(0), x_coords.get(0))); 
                        int m2 = (x_coords.size() < 2) ? (0) : (bfs_map (getMap[j], y_coords.get(1), x_coords.get(1))); 
                        int m3 = (x_coords.size() < 3) ? (0) : (bfs_map (getMap[k], y_coords.get(2), x_coords.get(2)));

                        int tot_length = m1 + m2 + m3; 
                        if (tot_length < best_length){
                            best_length = tot_length; 
                            best[0] = i; best[1] = j; best[2] = k; 
                        }
                    }
                }
            }
            my_index = -1; 
            for (int i=0; i<x_coords.size(); i++){
                if (x_coords.get(i) == robot.me.x && y_coords.get(i) == robot.me.y) my_index = i; 
            }
            optimal_map = best[my_index]; 
        }
        if (x_coords.size() != 1){
            // if (robot.me.turn <= 5) robot.signal (optimal_map, 2); 

            if (builtPilgrims < 2) {
                if (robot.karbonite >= 10 && robot.fuel >= 50) {
                    builtPilgrims += 1;
                    Direction dir = buildRandomDirection(robot.SPECS.PILGRIM); 
                    int new_x = dir.dx + robot.me.x; 
                    int new_y = dir.dy + robot.me.y; 
                    robot.signal(((optimal_map << 3) + ((new_y) << 4) + ((new_x) << 10)), 2); 
                    return robot.buildUnit(robot.SPECS.PILGRIM, dir.dx, dir.dy);
                }
            } else {
                // temporary fix to crusader logic
                if (robot.me.turn % (x_coords.size()) == my_index){
                    return buildRandom(robot.SPECS.CRUSADER);
                }
            }
        }
        else{            
            if (builtPilgrims < 2) {
                if (robot.karbonite >= 10 && robot.fuel >= 50) {
                    builtPilgrims += 1;
                    Direction dir = buildRandomDirection(robot.SPECS.PILGRIM); 
                    int new_x = dir.dx + robot.me.x; 
                    int new_y = dir.dy + robot.me.y; 
                    robot.signal(((0 << 3) + ((new_y) << 4) + ((new_x) << 10)), 2); 
                    return robot.buildUnit(robot.SPECS.PILGRIM, dir.dx, dir.dy);
                }
            } else if (builtPilgrims < 4) {
                if (robot.karbonite >= 10 && robot.fuel >= 50) {
                    builtPilgrims += 1;
                    Direction dir = buildRandomDirection(robot.SPECS.PILGRIM); 
                    int new_x = dir.dx + robot.me.x; 
                    int new_y = dir.dy + robot.me.y; 
                    robot.signal(((1 << 3) + ((new_y) << 4) + ((new_x) << 10)), 2); 
                    return robot.buildUnit(robot.SPECS.PILGRIM, dir.dx, dir.dy);
                }
            } else {
                return buildRandom(robot.SPECS.CRUSADER);
            }
        }
        return null;
    }

    public Direction buildRandomDirection (int unitType) { 
        Coordinate myLoc = Coordinate.fromRobot(robot.me);
        for (int i = 0; i < 30; i++) {
            Direction buildDir = Utils.getRandom8Direction();
            if (canTraverse(buildDir.dx + robot.me.x, buildDir.dy + robot.me.y)){
                return buildDir; 
            }
        }
        return null;
    }
    public Action buildRandom(int unitType) { 
        Coordinate myLoc = Coordinate.fromRobot(robot.me);
        for (int i = 0; i < 30; i++) {
            Direction buildDir = Utils.getRandom8Direction();
            if (canTraverse(buildDir.dx + robot.me.x, buildDir.dy + robot.me.y)){
                return robot.buildUnit(unitType, buildDir.dx, buildDir.dy);
            }
        }
        return null;
    }
    public int abs(int a) {
        return a < 0 ? -a : a;
    }

    public int getDist(int x1, int y1, int x2, int y2) {
        return ((x2-x1)*(x2-x1) + (y2-y1)*(y2-y1));
    }

    public boolean onMap(int x, int y) {
        return x >= 0 && y >= 0 && y < robot.map.length && x < robot.map[y].length;
    }

    public boolean canTraverse(int x, int y) {
        return onMap(x, y) && robot.map[y][x] && robot.getVisibleRobotMap()[y][x] <= 0;
    }

    // only works for cardinal directions, maps to 0 - 3
    public int dirToInt(int dx, int dy) {
        return (dx + 1) + ((dy + 1)/2);
    }
}


*/