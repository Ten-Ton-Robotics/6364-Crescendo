// Copyright (c) FIRST and other WPILib contributors.
// Open Source Software; you can modify and/or share it under the terms of
// the WPILib BSD license file in the root directory of this project.

package frc.robot;

import com.ctre.phoenix6.Utils;
import com.ctre.phoenix6.mechanisms.swerve.SwerveRequest;
import com.ctre.phoenix6.mechanisms.swerve.SwerveModule.DriveRequestType;

import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.wpilibj.smartdashboard.SmartDashboard;
import edu.wpi.first.wpilibj2.command.Command;
import edu.wpi.first.wpilibj2.command.Commands;
import edu.wpi.first.wpilibj2.command.button.CommandXboxController;
import frc.robot.generated.TunerConstants;
import frc.robot.subsystems.IntakeSubsystem;

public class RobotContainer {
  private static final double kMaxSpeed = 0.75; // 6 meters per second desired top speed
  private static final double kMaxAngularRate = Math.PI; // Half a rotation per second max angular
                                                         // velocity

  /* Setting up bindings for necessary control of the swerve drive platform */
  private final CommandXboxController m_controller = new CommandXboxController(0); // controller
  private final CommandSwerveDrivetrain m_drivetrain = TunerConstants.DriveTrain; // drivetrain
  private final IntakeSubsystem m_intake = new IntakeSubsystem(); // intake subsystem

  private final SwerveRequest.FieldCentric drive = new SwerveRequest.FieldCentric()
      .withDeadband(kMaxSpeed * 0.2).withRotationalDeadband(kMaxAngularRate * 0.2) // Add a 20%
      // deadband
      .withDriveRequestType(DriveRequestType.OpenLoopVoltage); // I want field-centric
                                                               // driving in open loop
                                                               // TODO: change this to closed
                                                               // loop velocity
  private final SwerveRequest.SwerveDriveBrake brake = new SwerveRequest.SwerveDriveBrake();
  private final SwerveRequest.PointWheelsAt point = new SwerveRequest.PointWheelsAt();
  private final Telemetry logger = new Telemetry(kMaxSpeed);

  private void configureBindings() {
    m_drivetrain.setDefaultCommand( // Drivetrain will execute this command periodically
        m_drivetrain.applyRequest(() -> drive.withVelocityX(-m_controller.getLeftY() * kMaxSpeed) // Drive
            // forward
            // with
            // negative
            // Y
            // (forward)
            .withVelocityY(-m_controller.getLeftX() * kMaxSpeed) // Drive left with negative
            // X (left)
            .withRotationalRate(-m_controller.getRightX() * kMaxAngularRate) // Drive
        // counterclockwise
        // with
        // negative X
        // (left)
        ));
    // intake subsystem


    m_controller.a().whileTrue(m_drivetrain.applyRequest(() -> brake));
    m_controller.b().whileTrue(m_drivetrain.applyRequest(() -> point
        .withModuleDirection(new Rotation2d(-m_controller.getLeftY(), -m_controller.getLeftX()))));

    // reset the field-centric heading on left bumper press
    m_controller.leftBumper().onTrue(m_drivetrain.runOnce(() -> m_drivetrain.seedFieldRelative()));

    if (Utils.isSimulation()) {
      m_drivetrain.seedFieldRelative(new Pose2d(new Translation2d(), Rotation2d.fromDegrees(90)));
    }
    m_drivetrain.registerTelemetry(logger::telemeterize);

    // intake subsystem
    m_controller.leftBumper().toggleOnTrue(m_intake.intake());
    m_controller.rightBumper().toggleOnFalse(m_intake.stop());
  }

  public RobotContainer() {
    configureBindings();
    SmartDashboard.putData("Intake", m_intake);
  }

  public Command getAutonomousCommand() {
    return Commands.print("No autonomous command configured");
  }
}
