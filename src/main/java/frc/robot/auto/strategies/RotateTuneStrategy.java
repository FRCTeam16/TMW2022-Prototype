package frc.robot.auto.strategies;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.wpilibj2.command.InstantCommand;
import edu.wpi.first.wpilibj2.command.SequentialCommandGroup;
import edu.wpi.first.wpilibj2.command.WaitCommand;
import frc.robot.Subsystems;
import frc.robot.commands.TurnToAngleCommand;

public class RotateTuneStrategy extends SequentialCommandGroup {
  public RotateTuneStrategy() {

    double offset = -0.0;

    addCommands(
      new InstantCommand(() -> Subsystems.drivetrainSubsystem.setGyroOffset(offset)),
      new InstantCommand(() -> Subsystems.drivetrainSubsystem.resetOdometry(new Pose2d(0, 0, new Rotation2d()))),
      new TurnToAngleCommand(-45, Subsystems.drivetrainSubsystem),
      new WaitCommand(0.5),
      new TurnToAngleCommand(45, Subsystems.drivetrainSubsystem),
      new WaitCommand(0.5),
      new TurnToAngleCommand(0, Subsystems.drivetrainSubsystem)
    );
  }
}
