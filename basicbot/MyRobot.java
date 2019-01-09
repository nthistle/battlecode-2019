package bc19;

// import java.util.Random;
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

/*
Try to use this instead of Pair because it expresses intent more clearly
*/
class Coordinate
{
	int y, x;
	public Coordinate(int y, int x)
	{
		this.y = y;
		this.x = x;
	}
	public boolean equals(Coordinate other)
	{
		return x==other.x && y==other.y;
	}
	public String toString()
	{
		return "(" + y + "," + x + ")";
	}
}

public class MyRobot extends BCAbstractRobot {

    public int mcX = -1; // my castle
    public int mcY = -1;
    public boolean [][] targetMap;
    public Action turn() {
        if (getHorizontalSymmetry()) log ("Horizontal"); 
        if (getVerticalSymmetry()) log ("Vertical");
        if (me.turn == 1) {

            // initialization stuff
            for (Robot r : getVisibleRobots()) {
                if (getDist(me.x, me.y, r.x, r.y) == 1 && r.team == me.team && r.unit == SPECS.CASTLE) {
                    mcX = r.x;
                    mcY = r.y;
                    break;
                }
            }
            if (mcX == -1) {
                log("COULD NOT LOCATE HOME CASTLE??");
            }

        }
        if (me.unit == SPECS.CASTLE) {
            return castleLogic();
        } else if (me.unit == SPECS.PILGRIM) {
            return pilgrimLogic();
        } else if (me.unit == SPECS.CRUSADER) {
            return crusaderLogic();
        }
        return null;
    }

    public int bDir;
    public int[][] dirs = {{0,1},{1,0},{0,-1},{-1,0}};
    public int[][][] sDirArr = {{{1,0},{1,0},{0,1}}, {{1,0},{1,0},{0,-1}},
                                {{0,1},{0,1},{1,0}}, {{0,1},{0,1},{-1,0}},
                                {{-1,0},{-1,0},{0,1}}, {{-1,0},{-1,0},{0,-1}},
                                {{0,-1},{0,-1},{1,0}}, {{0,-1},{0,-1},{-1,0}}};



    public Action crusaderLogic() {
        if (me.turn == 1) {
            bDir = (int)(8 * Math.random());
        }

        Robot[] nearby = getVisibleRobots();
        Robot nearestEnemy = null;
        int nearestDist = -1;
        for (Robot r : nearby) {
            if (r.team != me.team) {
                if (nearestEnemy == null || getDist(me.x, me.y, r.x, r.y) < nearestDist) {
                    nearestEnemy = r;
                    nearestDist = getDist(me.x, me.y, r.x, r.y);
                }
            }
        }

        int[] tDir = new int[2];

        if (nearestEnemy == null) {
            for (int i = 0; i < 15; i ++) {
                tDir[0] = sDirArr[bDir][me.turn % 3][0];
                tDir[1] = sDirArr[bDir][me.turn % 3][1];

                if (Math.random() < 0.2) {
                    tDir[0] *= 2;
                    tDir[1] *= 2;
                }

                if (canTraverse(me.x + tDir[0], me.y + tDir[1])) {
                    //log("Its finna be " + tDir[0] + "," + tDir[1]);
                    return move(tDir[0], tDir[1]);
                }
                bDir = (int)(8 * Math.random());
            }
        } else {
            if (nearestDist <= 4) {
                return attack(nearestEnemy.x - me.x, nearestEnemy.y - me.y);
            } else {
                int[] baseDir = {nearestEnemy.x - me.x, nearestEnemy.y - me.y};
                if(baseDir[0] != 0) baseDir[0] = baseDir[0] / abs(baseDir[0]);
                if(baseDir[1] != 0) baseDir[1] = baseDir[1] / abs(baseDir[1]);

                int[] goalDir = {baseDir[0], baseDir[1]};

                if (nearestDist > 8) {
                    if (goalDir[0] + goalDir[1] == 1) {
                        goalDir[0] *= 3;
                        goalDir[1] *= 3;
                    } else {
                        goalDir[0] *= 2;
                        goalDir[1] *= 2;
                    }
                }

                if (fuel > goalDir[0]*goalDir[0] + goalDir[1]*goalDir[1] && canTraverse(me.x + goalDir[0], me.y + goalDir[1])) {
                    //log("Goal dir looks good, it's " + goalDir[0] + "," + goalDir[1]);
                    return move(goalDir[0], goalDir[1]);
                } else if (fuel > baseDir[0]*baseDir[0] + baseDir[1]*baseDir[1] && canTraverse(me.x + baseDir[0], me.y + baseDir[1])) {
                    //log("Fine, base dir, it's " + baseDir[0] + "," + baseDir[1]);
                    return move(baseDir[0], baseDir[1]);
                } else {

                    // default movement code; todo: compartmentalize
                    for (int i = 0; i < 15; i ++) {
                        tDir[0] = sDirArr[bDir][me.turn % 3][0];
                        tDir[1] = sDirArr[bDir][me.turn % 3][1];

                        if (Math.random() < 0.2) {
                            tDir[0] *= 2;
                            tDir[1] *= 2;
                        }

                        if (canTraverse(me.x + tDir[0], me.y + tDir[1])) {
                            return move(tDir[0], tDir[1]);
                        }
                        bDir = (int)(8 * Math.random());
                    }

                }
            }
        }

        return null;
    }

    public boolean checkVerticalSymmetry (boolean [][] x){
        for (int i=0; i<x.length; i++){
            for (int j=0; j<x[i].length; j++){
                if (x[i][j] != x[i][x[i].length - 1 - j]) return false; 
            }
        }
        return true; 
    }
    public boolean checkHorizontalSymmetry (boolean [][] x){
        for (int i=0; i<x.length; i++){
            for (int j=0; j<x[i].length; j++){
                if (x[x.length - 1 - i][j] != x[i][j]) return false; 
            }
        }
        return true; 
    }
    public boolean getVerticalSymmetry (){
        return checkVerticalSymmetry(map) && checkVerticalSymmetry(karboniteMap) && checkVerticalSymmetry(fuelMap); 
    }
    public boolean getHorizontalSymmetry () {
        return checkHorizontalSymmetry(map) && checkHorizontalSymmetry(karboniteMap) && checkHorizontalSymmetry(fuelMap); 
    }
    int createdPilgrims;

    public Action castleLogic() {
        if (createdPilgrims < 2) {
            if (karbonite >= 10 && fuel >= 50) {
                for (int i=0; i<4; i++){
                    if (canTraverse(me.x + dirs[i][0], me.y + dirs[i][1])){
                        createdPilgrims += 1;
                        return buildUnit(SPECS.PILGRIM, dirs[i][0], dirs[i][1]);
                    }
                }
            }
        }
        else if (karbonite >= 20 && fuel >= 50) {
            for (int i=0; i<4; i++){
                if (canTraverse(me.x + dirs[i][0], me.y + dirs[i][1])){
                    return buildUnit(SPECS.CRUSADER, dirs[i][0], dirs[i][1]);
                }
            }
        }
        return null;
        // return buildUnit(SPECS.CRUSADER,dirs[(int)(mRand % 4)][0],dirs[(int)(mRand % 4)][1]);
    }
    
    boolean pilgrimKarb;

    public List <Pair> bfs_map (boolean [][] grid, int row, int col){

        Queue qu = new LinkedList();
        Queue paths = new LinkedList();

        HashMap <Pair, Boolean> vis = new HashMap <> ();

        qu.add(new Pair(row, col));

        List<Pair> cur_path = new ArrayList<> ();
        cur_path.add (new Pair(row, col));
        paths.add(cur_path);

        while (qu.size() > 0){
            Pair cur = (Pair) qu.poll();
            List <Pair> path = (List<Pair>) paths.poll();

            if (vis.get(cur) == true) continue;
            vis.put(cur, true);

            if (grid[cur.first][cur.second]) return path;

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
        return null;
    }
	/*
	Kevin Liu, 1/8/19
	Revised version of bfs for pilgrim
	Returns the coordinate you should move to this turn in order to reach a goal in the minimum number of turns
	Guaranteed to reach the goal in the minimum number of turns if everything stays still, but not necessarily using the minimum amount of fuel
	Returns null if no path is found
	Behavior is undefined if you are already on a goal tile
	the argument TRAVEL_RADIUS is the movement speed of the unit
	*/
    Coordinate bfs_map_better (boolean [][] grid, int y, int x, int TRAVEL_RADIUS)
    {
        Queue<Coordinate> q = new LinkedList<>();
        boolean vis[][] = new boolean[map.length][map.length];
        Coordinate prev[][] = new Coordinate[map.length][map.length];
        q.add(new Coordinate(y, x));
        vis[y][x] = true;
        int MAXD = (int)Math.sqrt(TRAVEL_RADIUS);
        while(!q.isEmpty())
        {
			Coordinate cur = q.poll();
			for(int a=-MAXD; a<=MAXD; a++)
			{
				for(int b=-MAXD; b<=MAXD; b++)
				{
					if(canTraverse(cur.y + a, cur.x + b) && !vis[cur.y + a][cur.x + b])
					{
						if(grid[cur.y + a][cur.x + b])
						{
							while(!prev[cur.y][cur.x].equals(new Coordinate(y, x))) //backtrack
							{
								cur = prev[cur.y][cur.x];
							}
							return cur;
						}
						vis[cur.y + a][cur.x + b] = true;
						q.add(new Coordinate(cur.y + a, cur.x + b));
						prev[cur.y + a][cur.x + b] = new Coordinate(cur.y, cur.x);
					}
				}
			}
		}
        return null;
    }

    public Action pilgrimLogic() {
        if (me.turn == 1) { 
            pilgrimKarb = Math.random() > 0.5;
            if (this.getVisibleRobots().length == 3){
                targetMap = karboniteMap; 
            }
            else if (this.getVisibleRobots().length == 2){
                targetMap = fuelMap; 
            }
            else
                targetMap = pilgrimKarb ? karboniteMap : fuelMap;
        }

        if (targetMap == null)
            targetMap = pilgrimKarb ? karboniteMap : fuelMap;


        if ((me.karbonite > 0 || me.fuel > 0) && getDist(me.x, me.y, mcX, mcY) <= 2) {
            return give(mcX - me.x, mcY - me.y, me.karbonite, me.fuel); // just give it all
        }

        if (me.karbonite > 17 || me.fuel > 85) {
            return wiggleMove(mcX, mcY);

        } else {
            /*
            int closestX = -1;
            int closestY = -1;
            int closestDist = -1;
            for (int y = 0; y < targetMap.length; y++) {
                for (int x = 0; x < targetMap[y].length; x++) {
                    if (!targetMap[y][x]) continue;
                    if (closestDist == -1 || getDist(me.x, me.y, x, y) < closestDist) {
                        closestX = x;
                        closestY = y;
                        closestDist = getDist(me.x, me.y, x, y);
                    }
                }
            }
            if (closestDist == 0) {
                return mine();
            } else {
                return wiggleMove(closestX, closestY);
            }
            */
            List<Pair> path = bfs_map (targetMap, me.y, me.x);
            if (path == null){
                // pilgrim is useless
                log ("I am a useless idiot");
                return null;
            }
            if (path.size() == 0){
                // shouldn't be possible
                return null;
            }
            else if (path.size() == 1){
                // on a target
                return mine();
            }
            else{
                Pair next = (Pair) path.get (1);
                return move (next.second - me.x, next.first - me.y);
            }
        }
    }

    public Action wiggleMove(int targetX, int targetY) {
        int[] goalDir = {targetX - me.x, targetY - me.y};
        if(goalDir[0] != 0) goalDir[0] /= abs(goalDir[0]);
        if(goalDir[1] != 0) goalDir[1] /= abs(goalDir[1]); // 'normalizes' to -1 - 1

        if (canTraverse(me.x + goalDir[0], me.y + goalDir[1])) {
            return move(goalDir[0], goalDir[1]);
        } else {
            for (int[] dd : dirs) {
                if (getDist(0, 0, goalDir[0] + dd[0], goalDir[1] + dd[1]) <= 2) {
                    if (canTraverse(me.x + goalDir[0] + dd[0], me.y + goalDir[1] + dd[1])) {
                        return move(goalDir[0] + dd[0], goalDir[1] + dd[1]);
                    }
                }
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
        return x >= 0 && y >= 0 && y < map.length && x < map[y].length;
    }

    public boolean canTraverse(int x, int y) {
        return onMap(x, y) && map[y][x] && getVisibleRobotMap()[y][x] <= 0;
    }

    // only works for cardinal directions, maps to 0 - 3
    public int dirToInt(int dx, int dy) {
        return (dx + 1) + ((dy + 1)/2);
    }
}