package frc.robot.autonomous;

import com.ctre.phoenix6.mechanisms.swerve.SwerveModule.DriveRequestType;
import com.ctre.phoenix6.mechanisms.swerve.SwerveRequest;
import edu.wpi.first.math.controller.HolonomicDriveController;
import edu.wpi.first.math.controller.PIDController;
import edu.wpi.first.math.controller.ProfiledPIDController;
import edu.wpi.first.math.geometry.Pose2d;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.trajectory.Trajectory;
import edu.wpi.first.math.trajectory.TrapezoidProfile;
import edu.wpi.first.wpilibj.Timer;
import edu.wpi.first.wpilibj2.command.Command;
import frc.robot.Constants;
import frc.robot.RobotContainer;
import frc.robot.Constants.Drivetrain;
import frc.robot.subsystems.CommandSwerveDrivetrain;
import me.nabdev.pathfinding.Pathfinder;
import me.nabdev.pathfinding.PathfinderBuilder;
import me.nabdev.pathfinding.structures.ImpossiblePathException;
import me.nabdev.pathfinding.structures.Path;
import me.nabdev.pathfinding.structures.Vertex;
import me.nabdev.pathfinding.utilities.FieldLoader.Field;

public interface Trajectories {

  public record outerror() {
    static boolean err = false;
  }

  public class TrajectoryGenerator {
    Pathfinder m_pathfinder;
    Path m_path;

    /**
     * @brief TrajectoryGenerator constructor
     * 
     *        This class is used to generate trajectories
     */
    public TrajectoryGenerator() {
      PathfinderBuilder m_pathbuilder = new PathfinderBuilder(Field.CRESCENDO_2024)
          .setRobotLength(Constants.Drivetrain.kBotLength)
          .setRobotWidth(Constants.Drivetrain.kBotWidth).setNormalizeCorners(false);
      m_pathfinder = m_pathbuilder.build();
      m_path = new Path(new Vertex(RobotContainer.m_drivetrain.getPose2d().getX(),
          RobotContainer.m_drivetrain.getPose2d().getY()), new Vertex(5, 5), m_pathfinder);
    }

    /**
     * @brief regenerate the trajectory
     * 
     * @param targetPose
     */
    public void generate(Pose2d targetPose) {
      final Pose2d pose = RobotContainer.m_drivetrain.getPose2d();
      try {
        m_path = m_pathfinder.generatePath(pose, targetPose);
        System.out.println("trajnew" + m_path);

      } catch (ImpossiblePathException e) {
        // TODO Auto-generated catch block
        e.printStackTrace();
        System.out.println("trajold" + m_path);
      }
    }

    /**
     * @brief get the generated trajectory
     * 
     * @return Trajectory
     */
    public Trajectory getTrajectory() {
      return m_path.asTrajectory(Constants.Drivetrain.K_TRAJECTORY_CONFIG);
    }
  }

  public class TrajectoryFollower extends Command {

    private final Timer m_timer = new Timer();
    private final Trajectory m_trajectory;
    private final HolonomicDriveController m_controller;
    private final Rotation2d m_desiredRotation;
    private final CommandSwerveDrivetrain m_drivetrain;

    private final SwerveRequest.FieldCentric m_drive =
        new SwerveRequest.FieldCentric().withDriveRequestType(DriveRequestType.OpenLoopVoltage); // field-centric

    /*
     * @param trajectory The trajectory to follow.
     * 
     * @param controller The HolonomicDriveController for the drivetrain.
     * 
     * @param desiredRotation The angle that the drivetrain should be facing. This is sampled at
     * each time step.
     * 
     * @param driveSubsystem The subsystem to use to drive the robot.
     * 
     * @param requirements The subsystems to require.
     */

    public TrajectoryFollower(Trajectory trajectory, Rotation2d endTheta,
        CommandSwerveDrivetrain driveSubsystem) {
      m_trajectory = trajectory;
      m_controller = new HolonomicDriveController(
          new PIDController(Constants.Drivetrain.kLateralPositionP, 0,
              Constants.Drivetrain.kLateralPositionD),
          new PIDController(Constants.Drivetrain.kLateralPositionP, 0,
              Constants.Drivetrain.kLateralPositionD),
          new ProfiledPIDController(Drivetrain.kAngularPositionP, 0.0, Drivetrain.kAngularPositionD,
              new TrapezoidProfile.Constraints(Drivetrain.kMaxAngularSpeed,
                  Drivetrain.kMaxAngularAcceleration)));
      m_drivetrain = driveSubsystem;
      m_desiredRotation = endTheta;
    }

    @Override
    public void initialize() {
      m_timer.restart();
    }

    @Override
    public void execute() {
      double curTime = m_timer.get();
      var desiredState = m_trajectory.sample(curTime);
      Rotation2d desiredRotation = m_desiredRotation;
      ChassisSpeeds targetChassisSpeeds =
          m_controller.calculate(m_drivetrain.getPose2d(), desiredState, desiredRotation);
      // This is done because the rotation is inverted.
      // It may not be the same on your robot, so if the rotation does not function as
      // expected,
      // remove this.
      targetChassisSpeeds.omegaRadiansPerSecond *= -1;
      m_drivetrain.applyRequest(() -> m_drive.withVelocityX(targetChassisSpeeds.vxMetersPerSecond)
          .withVelocityY(targetChassisSpeeds.vyMetersPerSecond)
          .withRotationalRate(targetChassisSpeeds.omegaRadiansPerSecond));
      m_drivetrain.setControl(m_drive.withVelocityX(targetChassisSpeeds.vxMetersPerSecond)
          .withVelocityY(targetChassisSpeeds.vyMetersPerSecond)
          .withRotationalRate(targetChassisSpeeds.omegaRadiansPerSecond));
    }

    @Override
    public void end(boolean interrupted) {
      if (!interrupted)
        m_drivetrain
            .applyRequest(() -> m_drive.withVelocityX(0).withVelocityY(0).withRotationalRate(0));
      m_timer.stop();

    }

    @Override
    public boolean isFinished() {
      return m_timer.hasElapsed(m_trajectory.getTotalTimeSeconds());
    }
  }

}
