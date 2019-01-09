package bc19;

public abstract class RobotHandler {
    
    protected MyRobot robot;

    public RobotHandler(MyRobot robot) {
        this.robot = robot;
    }

    public abstract void setup();

    public abstract Action turn();
}