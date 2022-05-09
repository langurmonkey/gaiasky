/*
 * This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 * See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.gui;

public interface IControllerMappings {

    /**
     * Power to apply to the linear value: val = val^pow
     *
     * @return The configured value power.
     */
    double getAxisValuePower();

    /**
     * Returns the code of the left stick horizontal axis, that produces:
     * <ul>
     * <li>Roll rotation in focus mode</li>
     * <li>Horizontal lateral movement in free mode</li>
     * </ul>
     *
     * @return The axis code, negative if not mapped.
     */
    int getAxisLstickH();

    /**
     * Sensitivity of left stick axis
     **/
    double getAxisLstickHSensitivity();

    /**
     * Returns the code of the Right stick horizontal axis, that produces:
     * <ul>
     * <li>Vertical rotation around focus in focus mode</li>
     * <li>Vertical look rotation (pitch) in free mode</li>
     * </ul>
     *
     * @return The axis code, negative if not mapped.
     */
    int getAxisRstickH();

    /**
     * Sensitivity of pitch axis
     **/
    double getAxisRstickHSensitivity();

    /**
     * Returns the code of the right stick vertical axis, that produces:
     * <ul>
     * <li>Horizontal rotation around focus in focus mode</li>
     * <li>Horizontal look rotation (yaw) in free mode</li>
     * </ul>
     *
     * @return The axis code, negative if not mapped.
     */
    int getAxisRstickV();

    /**
     * Sensitivity of right stick vertical axis
     **/
    double getAxisRstickVSensitivity();

    /**
     * Returns the code of the left stick vertical axis, that controls the forward and backward
     * movement
     *
     * @return The axis code, negative if not mapped.
     */
    int getAxisLstickV();

    /**
     * Sensitivity of left stick vertical axis
     **/
    double getAxisLstickVSensitivity();

    /**
     * Returns the code of the right trigger axis, used to increase the velocity. All the range
     * of the axis is used. Usually mapped to a trigger button.
     *
     * @return The axis code, negative if not mapped.
     */
    int getAxisRT();

    /**
     * Sensitivity of right trigger axis
     **/
    double getAxisRTSensitivity();

    /**
     * Returns the code of the left trigger axis, used to decrease the velocity. All the range
     * of the axis is used. Usually mapped to a trigger button.
     *
     * @return The axis code, negative if not mapped.
     */
    int getAxisLT();

    /**
     * Sensitivity of left trigger axis
     **/
    double getAxisLTSensitivity();

    /**
     * Returns the code of the Y button
     *
     * @return The Y button code, negative if not mapped.
     */
    int getButtonY();

    /**
     * Returns the code of the X button
     *
     * @return The X button code, negative if not mapped.
     */
    int getButtonX();

    /**
     * Returns the code of the A button
     *
     * @return The A button code, negative if not mapped.
     */
    int getButtonA();

    /**
     * Returns the code of the B button
     *
     * @return The B button code, negative if not mapped.
     */
    int getButtonB();

    /**
     * Returns the code of the horizontal dpad axis, if exists
     * @return Horizontal dpad axis code
     */
    int getAxisDpadH();

    /**
     * Returns the code of the vertical dpad axis, if exists
     * @return Vertical dpad axis code
     */
    int getAxisDpadV();

    /**
     * Returns the code of the dpad-up button
     *
     * @return The dpad-up button code, negative if not mapped.
     */
    int getButtonDpadUp();

    /**
     * Returns the code of the dpad-down button
     *
     * @return The dpad-down button code, negative if not mapped.
     */
    int getButtonDpadDown();

    /**
     * Returns the code of the dpad-left button
     *
     * @return The dpad-left button code, negative if not mapped.
     */
    int getButtonDpadLeft();

    /**
     * Returns the code of the dpad-right button
     *
     * @return The dpad-right button code, negative if not mapped.
     */
    int getButtonDpadRight();

    /**
     * Returns the code of the left stick button
     *
     * @return The left stick button code, negative if not mapped.
     */
    int getButtonLstick();

    /**
     * Returns the code of the right stick button
     *
     * @return The right stick button code, negative if not mapped.
     */
    int getButtonRstick();

    /**
     * Returns the code of the start button
     *
     * @return The start button code, negative if not mapped.
     */
    int getButtonStart();

    /**
     * Returns the code of the select button
     *
     * @return The select button code, negative if not mapped.
     */
    int getButtonSelect();

    /**
     * Returns the code of the RT button
     * @return The RT button code, negative if not mapped.
     */
    int getButtonRT();

    /**
     * Returns the code of the RB button
     * @return The RB button code, negative if not mapped.
     */
    int getButtonRB();

    /**
     * Returns the code of the LT button
     * @return The LT button code, negative if not mapped.
     */
    int getButtonLT();

    /**
     * Returns the code of the LB button
     * @return The LB button code, negative if not mapped.
     */
    int getButtonLB();
}
