/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.interfce;

public interface IControllerMappings {

    /**
     * Power to apply to the linear value: val = val^pow
     *
     * @return The configured value power.
     */
    double getAxisValuePower();

    /**
     * Returns the code of the axis that produces:
     * <ul>
     * <li>Roll rotation in focus mode</li>
     * <li>Horizontal lateral movement in free mode</li>
     * </ul>
     *
     * @return The axis code, negative if not mapped.
     */
    int getAxisRoll();

    /**
     * Sensitivity of roll axis
     **/
    double getAxisRollSensitivity();

    /**
     * Returns the code of the axis that produces:
     * <ul>
     * <li>Vertical rotation around focus in focus mode</li>
     * <li>Vertical look rotation (pitch) in free mode</li>
     * </ul>
     *
     * @return The axis code, negative if not mapped.
     */
    int getAxisPitch();

    /**
     * Sensitivity of pitch axis
     **/
    double getAxisPitchSensitivity();

    /**
     * Returns the code of the axis that produces:
     * <ul>
     * <li>Horizontal rotation around focus in focus mode</li>
     * <li>Horizontal look rotation (yaw) in free mode</li>
     * </ul>
     *
     * @return The axis code, negative if not mapped.
     */
    int getAxisYaw();

    /**
     * Sensitivity of yaw axis
     **/
    double getAxisYawSensitivity();

    /**
     * Returns the code of the axis that controls the forward and backward
     * movement
     *
     * @return The axis code, negative if not mapped.
     */
    int getAxisMove();

    /**
     * Sensitivity of move axis
     **/
    double getAxisMoveSensitivity();

    /**
     * Returns the code of the axis used to increase the velocity. All the range
     * of the axis is used. Usually mapped to a trigger button.
     *
     * @return The axis code, negative if not mapped.
     */
    int getAxisVelocityUp();

    /**
     * Sensitivity of velocity up axis
     **/
    double getAxisVelUpSensitivity();

    /**
     * Returns the code of the axis used to decrease the velocity. All the range
     * of the axis is used. Usually mapped to a trigger button.
     *
     * @return The axis code, negative if not mapped.
     */
    int getAxisVelocityDown();

    /**
     * Sensitivity of velocity down axis
     **/
    double getAxisVelDownSensitivity();

    /**
     * Returns the code of the button that, when pressed, multiplies the
     * velocity vector by 0.1.
     *
     * @return The button code, negative if not mapped.
     */
    int getButtonVelocityMultiplierTenth();

    /**
     * Returns the code of the button that, when pressed, multiplies the
     * velocity vector by 0.5.
     *
     * @return The button code, negative if not mapped.
     */
    int getButtonVelocityMultiplierHalf();

    /**
     * Returns the code of the button used to increase the velocity.
     *
     * @return The button code, negative if not mapped.
     */
    int getButtonVelocityUp();

    /**
     * Returns the code of the button used to decrease the velocity.
     *
     * @return The button code, negative if not mapped.
     */
    int getButtonVelocityDown();

    /**
     * Returns the code of the button used to go up.
     *
     * @return The button code, negative if not mapped.
     */
    int getButtonUp();

    /**
     * Returns the code of the button used to go down.
     *
     * @return The button code, negative if not mapped.
     */
    int getButtonDown();

    /**
     * Returns the code of the button used to toggle between free and focus mode.
     *
     * @return The button code, negative if not mapped.
     */
    int getButtonModeToggle();

}
