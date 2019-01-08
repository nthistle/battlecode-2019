package bc19;

// import java.util.Random;

public class MyRobot extends BCAbstractRobot {

    int x_LCG;

    /**
     * Seeds the Linear Congruential Generator.
     * If this isn't called before fetching random numbers, the seed effectively
     * defaults to 0. Recommended usage is to seed with unit ID on turn 1.
     */
    public void seedLCG(int val) {
        x_LCG = val;
    }

    /**
     * Generates a random 31-bit number, advancing state of the LCG. Note that
     * x_LCG will always hold the value of the most recently generated random number.
     * If not seeded before this is called, the seed effectively defaults to 0.
     */
    public int getRandLCG() {
        return (x_LCG = ((x_LCG * 1103515245) + 12345) & 0x7fffffff);
    }


    public Action turn() {
        if (me.turn == 1) seedLCG(17 + me.id);

        // mRand = me.id + System.nanoTime();
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
    public int[][][] sDirArr = {{{1,0},{1,0},{0,1}}, {{0,1},{0,1},{1,0}},
                               {{0,1},{0,1},{-1,0}}, {{-1,0},{-1,0},{0,1}},
                               {{-1,0},{-1,0},{0,-1}}, {{0,-1},{0,-1},{-1,0}},
                               {{0,-1},{0,-1},{1,0}}, {{1,0},{1,0},{0,-1}}};

    public Action crusaderLogic() {
        if (me.turn == 1) {
            bDir = (int)(getRandLCG() % 4);
        }

        return move(sDirArr[bDir][me.turn % 3][0],
                    sDirArr[bDir][me.turn % 3][1]);

        // return move(dirs[(int)(mRand % 4)][0], dirs[(int)(mRand % 4)][1]);
        //return null;
    }

    public Action castleLogic() {
        return buildUnit(SPECS.CRUSADER, 0, 1);
        // return buildUnit(SPECS.CRUSADER,dirs[(int)(mRand % 4)][0],dirs[(int)(mRand % 4)][1]);
    }

    public Action pilgrimLogic() {

        return null;
    }
}