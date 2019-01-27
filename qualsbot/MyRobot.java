package bc19;

public class MyRobot extends BCAbstractRobot {

    RobotHandler myHandler;

    public Action turn() {
        if (me.turn == 1) {
            // Initialization stuff 
            if (me.unit == SPECS.CASTLE) {
                this.myHandler = new CastleHandler(this);
            } else if (me.unit == SPECS.CRUSADER) {
                this.myHandler = new CrusaderHandler(this);
            } else if (me.unit == SPECS.PILGRIM) {
                this.myHandler = this.parsePilgrimType(); //new PilgrimHandler(this);
            } else if (me.unit == SPECS.PROPHET) {
                this.myHandler = new LatticeProphetHandler(this); 
            } else if (me.unit == SPECS.CHURCH) {
                this.myHandler = new ChurchHandler(this);
            }

            // Calls handler setup method
            this.myHandler.setup();
        }

        if (this.myHandler instanceof ChurchPilgrimHandler) {
            if (((ChurchPilgrimHandler)this.myHandler).builtMyChurch) {
                this.myHandler = this.reassignPilgrimToMiner();
                this.myHandler.setup();
            }
        }

        // Handler turn method
        if (this.myHandler == null) {
            log("I have no handler!");
            return null;
        } else {
            return this.myHandler.turn();
        }
    }

    public RobotHandler reassignPilgrimToMiner() {
        // let's pick random fuel/karb cluster nearby
        Coordinate myLoc = Coordinate.fromRobot(me);
        Utils.shuffleArray(Utils.dir21volatile);
        Coordinate p;
        for (Direction d : Utils.dir21volatile) {
            p = myLoc.add(d);
            if (Utils.isInRange(map, p) && (karboniteMap[p.y][p.x] || fuelMap[p.y][p.x])) {
                return new MiningPilgrimHandler(this, p, ((ChurchPilgrimHandler)this.myHandler).clusterDestination);
            }
        }
    }

    public RobotHandler parsePilgrimType() {
        Coordinate myLoc = Coordinate.fromRobot(me);

        Robot closestCastle = null;
        int closestCastleDistance = -1;

        for (Robot r : getVisibleRobots()) {
            //check if this robot is a castle/church on my team
            if (r.team == me.team && (r.unit == SPECS.CASTLE || r.unit == SPECS.CHURCH)) {

                //if the castle is the closer than the current closest, update it
                if ((closestCastle == null || Utils.getDistance(myLoc, Coordinate.fromRobot(r)) < closestCastleDistance)
                      && r.signal != -1) {

                    //assign the castlelocation to robot r's coordinate, similarly assign distance
                    closestCastle = r;
                    closestCastleDistance = Utils.getDistance(myLoc, Coordinate.fromRobot(r));
                }
            }
        }

        if (closestCastle == null) {
            // TODO: return "rogue pilgrim handler" who just looks for work
            return null;
        }

        if ((closestCastle.signal & 7) == 7) {
            // mining pilgrim handler XXXXXX|YYYYYY|_111
            int targetX = (closestCastle.signal >> 10) & 63;
            int targetY = (closestCastle.signal >>  4) & 63;
            return new MiningPilgrimHandler(this, new Coordinate(targetX, targetY), Coordinate.fromRobot(closestCastle));
        } else if ((closestCastle.signal & 7) == 5) {
            // church pilgrim handler XXXXXX|YYYYYY|A101
            // A = aggressive?
            int targetX = (closestCastle.signal >> 10) & 63;
            int targetY = (closestCastle.signal >>  4) & 63;
            boolean aggressive = (closestCastle.signal & 8) == 8;
            return new ChurchPilgrimHandler(this, new Coordinate(targetX, targetY), aggressive);
        }
    }
}