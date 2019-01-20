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
                this.myHandler = new PilgrimHandler(this);
            } else if (me.unit == SPECS.PROPHET){
                this.myHandler = new ProphetHandler(this); 
            }

            // Calls handler setup method
            this.myHandler.setup();
        }

        // Handler turn method
        return this.myHandler.turn();
    }
}