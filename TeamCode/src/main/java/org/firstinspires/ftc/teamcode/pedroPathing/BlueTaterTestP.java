package org.firstinspires.ftc.teamcode.pedroPathing;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.pedropathing.follower.Follower;
import com.pedropathing.geometry.BezierLine;
import com.pedropathing.geometry.Pose;
import com.pedropathing.paths.PathChain;
import com.pedropathing.util.Timer;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.OpMode;

// Blue alliance, back position: drive to the shoot spot, search/align to the
// AprilTag with turnToTag, then take one shot. Simpler "search, aim, shoot" auto,
// not the full 3-ball cycle BlueBackAutoP runs.
@Autonomous
public class BlueTaterTestP extends OpMode {

    private Timer pathTimer, opModeTimer;
    private Follower follower;
    private intakeOuttake take;
    private intakeOuttake.turnToTag searchAction;

    public enum PathState {
        DRIVE_STARTING_SHOOT_POS,
        SEARCH_TAG,
        SHOOT,
        COMPLETED,
    }

    PathState pathState;
    private final Pose startPose = new Pose(58.85647607934656, 8, Math.toRadians(90));

    private PathChain Path1;

    public void buildPaths() {
        Path1 = follower.pathBuilder().addPath(
                        new BezierLine(
                                startPose,

                                new Pose(58.856, 10.964)
                        )
                ).setLinearHeadingInterpolation(Math.toRadians(90), Math.toRadians(113))

                .build();
    }

    public void statePathUpdate() {
        switch (pathState) {
            case DRIVE_STARTING_SHOOT_POS:
                // First path - start directly without checking isBusy
                follower.followPath(Path1, true);
                setPathState(PathState.SEARCH_TAG);
                break;
            case SEARCH_TAG:
                if (!follower.isBusy()) {
                    if (searchAction == null) {
                        take.launcher.setVelocity(1580.0);
                        searchAction = take.new turnToTag(true, true); // true = blue alliance
                    }
                    boolean stillSearching = searchAction.run(new TelemetryPacket());
                    if (!stillSearching) {
                        setPathState(PathState.SHOOT);
                    }
                }
                break;
            case SHOOT:
                take.launcher.setVelocity(1580.0);
                if (pathTimer.getElapsedTimeSeconds() > 2.0) {
                    take.intakeMotor.setPower(1.0);
                    take.leftFeeder.setPower(-1.0);
                    take.rightFeeder.setPower(1.0);
                    take.topWheel.setPower(-1.0);
                    take.boxMotor.setPower(1.0);
                    if (pathTimer.getElapsedTimeSeconds() > 4.0) {
                        take.intakeMotor.setPower(0);
                        take.leftFeeder.setPower(0);
                        take.rightFeeder.setPower(0);
                        take.topWheel.setPower(0.0);
                        take.boxMotor.setPower(0.0);
                        take.launcher.setVelocity(0.0);

                        setPathState(PathState.COMPLETED);
                    }
                }
                break;
            case COMPLETED:
                // stop all machinery - completed in SHOOT
                break;
            default:
                telemetry.addLine("No State Commanded");
                break;
        }
    }

    public void setPathState(PathState newState) { // nice helper function
        pathState = newState;
        pathTimer.resetTimer();
    }

    public void init() {
        pathState = PathState.DRIVE_STARTING_SHOOT_POS;
        pathTimer = new Timer();
        opModeTimer = new Timer();
        follower = Constants.createFollower(hardwareMap);

        take = new intakeOuttake(hardwareMap);

        buildPaths();
        follower.setPose(startPose);
    }

    public void start() {
        opModeTimer.resetTimer();
        setPathState(pathState);
    }

    public void loop() {
        follower.update();
        statePathUpdate();

        telemetry.addData("path state", pathState.toString());
        telemetry.addData("x", follower.getPose().getX());
        telemetry.addData("y", follower.getPose().getY());
        telemetry.addData("heading", follower.getPose().getHeading());
        telemetry.addData("path time in seconds", pathTimer.getElapsedTimeSeconds());
        telemetry.addData("search - ever saw tag", searchAction != null && searchAction.everSeenTag);
        telemetry.update();
    }
}
