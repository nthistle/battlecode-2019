package bc19;
import java.util.*; 

/*
Standard pair class
*/
class Pair {
    public int first;
    public int second;

    public Pair(int first, int second) {
        super();
        this.first = first;
        this.second = second;
    }

    public int hashCode() {
        int hashFirst = first;
        int hashSecond = second;

        return (hashFirst + hashSecond) * hashSecond + hashFirst;
    }

    public boolean equals(Object other) {
        if (other instanceof Pair) {
            Pair otherPair = (Pair) other;
            return (otherPair.first == this.first && otherPair.second == this.second);
        }

        return false;
    }

    public String toString()
    {
           return "(" + first + ", " + second + ")";
    }
}

public class CastleHandler extends RobotHandler 
{
    public CastleHandler(MyRobot robot) {
        super(robot);
    }


    int builtPilgrims = 0;
    List <Integer> x_coords; 
    List <Integer> y_coords; 
    int optimal_map = -1; 
    int my_index = -1; // this castle's position in x_coords
    public int[][] dirs = {{0,1},{1,0},{0,-1},{-1,0}};

    public void setup() {

    }
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
    public Action turn() {
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