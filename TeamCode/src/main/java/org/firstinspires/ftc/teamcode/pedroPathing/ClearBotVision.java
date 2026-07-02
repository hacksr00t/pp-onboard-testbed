package org.firstinspires.ftc.teamcode.pedroPathing;

// limelight imports
import java.util.List;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes.ClassifierResult;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp
public class ClearBotVision extends LinearOpMode {
    Limelight3A limelight;
    private static final double CONFIDENCE_LIMIT = 0.75;

    public void runOpMode() {
        // Declaration of motors/servos/other hardware
        // Motor & servo directions

        // Declaration of limelight
        limelight = hardwareMap.get(Limelight3A.class, "limelight"); // init
        limelight.setPollRateHz(100); // asking limelight for data 100 times/second
        limelight.pipelineSwitch(8);
        limelight.start(); // starting the limelight, asking to seek

        telemetry.addLine("Limelight initialized, waiting to start...");
        telemetry.update();


        waitForStart();

        while (opModeIsActive()) {
            LLResult result = limelight.getLatestResult();

            if (result != null && result.isValid()) {
                // from ClassifierResults, classifications derived from and it's retrieved (getter method)
                List<ClassifierResult> classifications = result.getClassifierResults();

                // declaration of basic values produced by limelight vision
                double tx = result.getTx();
                double ty = result.getTy();
                double ta = result.getTa();

                // Values (x, y, area)
                telemetry.addData("Target X", tx);
                telemetry.addData("Target Y", ty);
                telemetry.addData("Target Area", ta);

                if (classifications != null && !classifications.isEmpty()) {
                    // highest confidence percentage object
                    ClassifierResult best = classifications.get(0);
                    String detected = best.getClassName();
                    double confidence = best.getConfidence();

                    // display info + confidence via percentage
                    telemetry.addData("Detected", detected);
                    telemetry.addData("Confidence", String.format("%.1f", confidence * 100));

                    // Button-powered actions (user interaction)
                    if (confidence >= CONFIDENCE_LIMIT) {
                        // certain buttons and certain detections == certain actions + data
                        if (gamepad1.a && detected.equals("Shoot")) {
                            // run("Shoot");
                            telemetry.addLine("ACTION RUNNING: Shoot identifier detected, running...");
                        }
                        if (gamepad1.b && detected.equals("Come")) {
                            // run("Come");
                            telemetry.addLine("ACTION RUNNING: Come identifiier detected, moving...");
                        }
                    } else {
                        telemetry.addLine("Confidence too low, could not perform an action. Try again!");
                    }
                    telemetry.addLine("\nAll Detections:");
                    for (ClassifierResult classification : classifications) {
                        telemetry.addData(" " + classification.getClassName(),
                                String.format("%.1f%%", classification.getConfidence() * 100));
                    }
                } else {
                    telemetry.addLine("No Targets via Limelight"); // No valid target
                }

                // checking if data extracted is good/updated
                long old = result.getStaleness();
                if (old < 100) {
                    telemetry.addData("Data is good (ms):", old);
                } else {
                    telemetry.addData("Data is old/stale (ms):", old);
                }
            } else {
                telemetry.addLine("No valid data");
            }
            telemetry.update();
        }
        limelight.stop();
    }

    // actual robot action ..//TODO
    // inputs the class's names from the model via teachable machine
    // run is the function that is used in runOpMode for performing the actions via
    // button presses and certain confidence levels in classes
    private void run(String objectClassName) {
        switch(objectClassName) {
            case "Come":
                // robot action
                telemetry.addLine("RUNNING ACTION 'Come'");
                break;
            case "Shoot":
                // robot action
                telemetry.addLine("RUNNING ACTION 'Shoot'");
                break;

        }
    }
}
