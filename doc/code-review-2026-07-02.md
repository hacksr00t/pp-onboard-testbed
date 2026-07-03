# Power Potatoes #22200 — TeamCode Code Review

**Date:** 2026-07-02
**Branch reviewed:** `student/hacksr00t/intake-tuning`
**Scope:** full `TeamCode/` source tree as it stood on the branch (working tree was clean — this is a baseline review, not a diff review). 11 files, ~4,452 lines. `FtcRobotController/` (the FIRST SDK) was left alone.

## Update — 2026-07-02, same day

Five findings below have code changes proposed (not yet saved/pushed — see each finding for what changed). Everything else in this report is still open. **The odometry and BlueFrontAutoP changes touch odometry offsets/directions and hardware config, both flagged for mandatory review under team policy — they need a testing note and a teammate's sign-off before merging, and the odometry change specifically should be verified on the real robot (Virtual Robot can't validate a physical pod-offset measurement) before it's trusted.**

Two High-severity items were deliberately **not** touched:
- **Mirroring drift (Blue/Red Back autos, grab-row Y coordinates)** — fixing this means re-measuring the actual field/game-piece layout, which isn't something to guess at from the code alone. A wrong guess here is worse than leaving it flagged.
- **Game1Auto.java's no-op vision-aim call** — making it actually run means deciding how aiming gets polled into the autonomous loop (a real behavior change to vision alignment, one of the loudly-flagged categories), not a one-line fix. It's tied to the `turnToTag()` fix below, which is now safer to build on top of.

## Read this first

- **~~Don't select `BlueFrontAutoP` at a competition or scrimmage.~~ Partially fixed.** `init()` now maps `intakeMotor`, `boxMotor`, `leftFeeder`, `rightFeeder`, `topWheel`, and `launcher` via `hardwareMap.get(...)`, using the same device-name strings every other file already uses — this removes the guaranteed `NullPointerException` in the `COMPLETED` state. **Still true:** the shoot/intake states are still empty `// TODO` stubs and `shooterFunctionUpdate()` is still never called from `loop()` — this auto will no longer crash, but it still only drives the path and doesn't shoot or intake. Don't run it expecting scoring until that logic is written.
- **Odometry offset/direction conflict — fix proposed.** `LimelightDecodeDriveMode.java` now uses the same offsets (`23.0124, -134.112`) and encoder directions (`FORWARD, REVERSED`) as `Constants.java`'s active (non-commented) `localizerConstants`. Constants.java's own commented-out `// OLD VALUES BELOW` block was the exact pair LimelightDecodeDriveMode.java was still using, which is why this was picked as the source of truth over the reverse. **Confirm this on the real robot** — a wrong sign here means the robot will localize incorrectly in TeleOp.

**Also fixed this round:**
- **RedFrontAutoP.java — wrong next-state bug.** `case GO_TO_GRAB_FINAL_3` now transitions to `GRAB_FINAL_3` instead of `GRAB_NEXT_3`, so the auto actually proceeds to grab/shoot the final set instead of looping back.
- **LimelightDecodeDriveMode.java — laser distance unit bug.** Removed the erroneous extra `* 25.4`; the value is already in millimeters (`MAX_DISTANCE_MM`), and the variable is renamed `distanceMm` to match what it actually holds and what the telemetry label already said.
- **intakeOuttake.java — `turnToTag()` safety.** Replaced the no-op `assert llResult != null` with a real `if (!isValid) continue;` guard, and added a drivetrain zero-power call after the turn loop exits (it previously left the last commanded power applied indefinitely). This method is still uncalled everywhere — these are defensive fixes for whenever it's re-enabled, not a behavior change today.

## High severity (7)

1. **BlueFrontAutoP.java · `init()` ~283–309, `COMPLETED` ~256** — Hardware is never mapped. Unlike the other three autos, this file never calls `hardwareMap.get(...)` for any motor, servo, the Limelight, IMU, or laser sensor — they all stay `null`. The `COMPLETED` state calls `launcher.setPower(0.0)`, which throws a `NullPointerException` and ends the OpMode mid-match. Separately, even if that's fixed: `shooterFunctionUpdate()` and its `LaunchState` machine are never called from `loop()`, and the `GRAB_*`/`SHOOT_*` cases are literally commented `// TODO need code for intaking/shooting`.

2. **Constants.java ~49–58 vs. LimelightDecodeDriveMode.java ~110, 124** — Odometry pod offsets and directions disagree between autonomous and TeleOp. `Constants.java` sets `forwardPodY(-134.112)` / `strafePodX(23.0124)` and `forward=FORWARD, strafe=REVERSED`. `LimelightDecodeDriveMode.java` still hard-codes the old measurement (comment: "tuned for 3110-0002-0001 Product Insight #1"): `odo.setOffsets(-22.1, -136.4, MM)` with *both* encoders `REVERSED`. Note the sign flip on the strafe pod (-22.1 → +23.0124). These describe the same physical sensor on the same robot — one of the two is wrong.

3. **RedFrontAutoP.java · `case GO_TO_GRAB_FINAL_3`, ~line 259** — Wrong next-state after the final grab path. This case follows `Path7` (the final-grab approach) but then calls `setPathState(PathState.GRAB_NEXT_3)` — it should be `GRAB_FINAL_3`. The robot loops back into the "next 3" cycle instead of grabbing/shooting the final set.

4. **LimelightDecodeDriveMode.java · lines 162–163, 171** — Laser distance reading is off by a factor of ~25, and mislabeled. `double distanceInch = ((volts / MAX_VOLTS) * MAX_DISTANCE_MM) * 25.4;` — `MAX_DISTANCE_MM` is already millimeters, so multiplying by 25.4 again inflates the result ~25x. The variable is named `distanceInch` but the telemetry line labels it `"Distance (mm)"`.

5. **intakeOuttake.java · `turnToTag()`, ~lines 121–178** — A blocking loop inside a non-blocking `Action`, with no motor stop afterward. `Action.run()` is supposed to be called repeatedly and return quickly; this one blocks up to a second internally and always returns `false`. When the turn condition trips and the loop exits, the drivetrain is left holding its last commanded power with no call to zero it. Currently inert (every call site is commented out), but a landmine for whoever re-enables it.

6. **Game1Auto.java · line 61** — Vision-aim call is a no-op that looks like it works. `take.turnToTag(true, true);` constructs and discards an `Action` object — its constructor runs, but `.run()` is never invoked, so the aiming logic never executes.

7. **BlueBackAutoP.java vs RedBackAutoP.java · grab-row Y coordinates & headings** — Mirroring drift between the Blue and Red autos. Start poses mirror cleanly, but the three "grab row" Y-values that should mirror identically instead differ by **6.0", 6.0", and 4.77"** respectively. A couple of heading transitions are also ~5° off from an exact mirror (Blue 90°→113° vs. Red 90°→62°, where an exact mirror predicts ~67°). Worth a field-measurement check before trusting both sides equally.

## Update — 2026-07-03

PR #1 (odometry fix, BlueFrontAutoP crash fix, RedFrontAutoP fix, laser-distance fix, turnToTag safety fix) merged into `master`. This round (`student/hacksr00t/review-cleanup`) picks off three more mechanical, no-judgment-call findings:

- **Medium #1 fixed** — `RedBackAutoP.java`'s missing `break` is now in place.
- **Medium #8 fixed** — Dead `Path12` removed from `BlueBackAutoP.java`.
- **Low #2 fixed** — Dead `run(String)` method removed from `ClearBotVision.java`, along with the stale commented-out call sites that referenced it.

Everything else below is still open — each remaining item needs either a tuning/design decision (shooter velocity, PIDF gains, the laser-distance feature) or field verification (mirroring drift) that isn't mine to make.

## Medium severity (9)

1. ~~RedBackAutoP.java · `case GO_TO_GRAB_NEXT_3`, ~lines 236–247 — Missing `break` causes a switch fallthrough into `GRAB_NEXT_3`.~~ **Fixed.**
2. **BlueBackAutoP (1580) vs RedBackAutoP/RedFrontAutoP (1300) vs intakeOuttake.java (1200, unused)** — Shooter velocity differs by alliance with no documented reason.
3. **intakeOuttake.java · fiducial loop, ~line 138** — `assert llResult != null` is used as a null-check, but Java assertions are disabled by default at runtime — it's a no-op in production. A working `isValid` boolean is computed nearby but unused here.
4. **LimelightDecodeDriveMode.java · `calculateRPM()`, lines 369–381** — `angle = 45` (meant as degrees) is fed directly into `Math.cos`/`Math.tan`, which expect radians. Currently dead code (never called anywhere), but wrong the moment it's wired up.
5. **Constants.java · line 24, TODOs at 31–32** — Heading PIDF has negative P/D gains, and the file still carries `// TODO HEADING PIDF (NEED HELP)` / `// TODO DRIVE TUNING` — confirm these are actually finalized.
6. **ClearBotVision.java · line 51** — Assumes `classifications.get(0)` is the highest-confidence detection without confirming that against the Limelight API/docs.
7. **ClearBotVision.java:23 · intakeOuttake.java:76 · LimelightDecodeDriveMode.java:89** — Limelight pipeline index `8` hardcoded independently in three files with no shared constant.
8. ~~BlueBackAutoP.java · `Path12`, ~lines 153–161 — Built but never referenced by its own state machine.~~ **Fixed — removed.**
9. **intakeOuttake.java · fields ~39–41** — Laser-distance feature is half-built: `laserAnalog`/`distance`/`MAX_VOLTS`/`MAX_DISTANCE_MM` are declared but the voltage is never converted into a distance anywhere in the class.

## Low severity / polish (4)

1. **intakeOuttake.java · lines 53–56** — Motor field names don't match their hardware-map string names (`leftFront` ↔ `"frontLeft"`, `rightBack` ↔ `"backRight"`). Functionally fine, just confusing to read.
2. ~~ClearBotVision.java · lines 101–113 — Dead `run(String)` method; both call sites are commented out.~~ **Fixed — removed.**
3. **intakeOuttake.java lines 144–147 · LimelightDecodeDriveMode.java lines 308–314** — Unexplained magic numbers in aim-assist logic: `kP = 0.02`, ±0.3 turn clamp, AprilTag IDs `20`/`24`, a `tx + 3` offset.
4. **Across all four autonomous OpModes** — A long list of raw timer thresholds (`0.25, 0.3, 0.5, 0.75, 2.0, 3.5, 4.0, 4.5, 5.5, 7.0, 7.5, 7.75` seconds) with no named constants distinguishing safety margins from tuned timing.

## Hardware-map cross-check

| Device | Config name | Used in | Agreement |
|---|---|---|---|
| DcMotorEx | `"frontLeft"`/`"backLeft"`/`"frontRight"`/`"backRight"` | Constants.java, LimelightDecodeDriveMode.java, intakeOuttake.java | ✓ consistent, directions match |
| GoBildaPinpointDriver | `"odo"` | Constants.java, LimelightDecodeDriveMode.java | Name matches — see high-severity offset/direction conflict above |
| Limelight3A | `"limelight"` | LimelightDecodeDriveMode.java, ClearBotVision.java, intakeOuttake.java | ✓ consistent |
| IMU | `"imu"` | LimelightDecodeDriveMode.java, intakeOuttake.java | ✓ consistent |
| AnalogInput | `"laserAnalogInput"` | LimelightDecodeDriveMode.java, intakeOuttake.java | ✓ consistent (feature unfinished, see above) |
| DcMotorEx/CRServo | `"launcherMotor"`, `"boxMotor"`, `"leftFeeder"`, `"rightFeeder"`, `"topWheel"` | intakeOuttake.java only | n/a — BlueFrontAutoP never looks these up (see finding #1) |

## Config & build notes

- **Build not confirmed clean in this session.** `./gradlew :TeamCode:assembleDebug` failed in the review sandbox because Android SDK Platform 36 wasn't installed and `/opt/android-sdk` wasn't writable there — a limitation of that environment, not a proven code problem. Build for real in Android Studio before trusting any fix.
- **`.gitignore` keystore rules are commented out.** `libs/ftc.debug.keystore` being committed is normal for FTC (shared debug-signing key, not a secret), but the `*.jks`/`*.keystore` ignore lines are commented out. Cheap to uncomment pre-emptively in case a real release keystore ever gets added.
- **`compileSdk 36` is ahead of what AGP 8.7.3 has been tested against** — Gradle prints a compatibility warning. Looks inherited from this season's FTC SDK release rather than a team change.

## What's solid

- Drivetrain hardware-map names and motor directions agree everywhere they're used.
- Every motor power value found stays within the safe `[-1, 1]` range.
- `Tuning.java` (1,362 lines) is untouched, stock Pedro Pathing library scaffolding — correctly left alone.
- `BlueBackAutoP`, `RedBackAutoP`, and `RedFrontAutoP` share real logic through the `intakeOuttake` helper class instead of copy-pasting motor control (BlueFrontAutoP doesn't follow this pattern).
- No `==` comparisons on doubles/timers anywhere in the state machines.
- `.gitignore` correctly excludes build artifacts, `local.properties`, and IDE files.

## Suggested priority

The odometry-offset conflict and the `BlueFrontAutoP` crash are the two that actually bite at competition — everything else can wait. Per team policy, any fix here touches hardware-map config, odometry offsets/directions, or autonomous path logic, so it needs a testing note and a teammate's review before it merges. Test an odometry fix in Virtual Robot first; a Pedro-path change deserves a slow, supervised run on the real robot afterward.
