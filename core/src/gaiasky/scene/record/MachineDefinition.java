/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.scene.record;

public class MachineDefinition {

    /** The name of the spacecraft **/
    private String name;

    /** Rough size of the bounding box of the spacecraft in Km **/
    private double size;
    /** Mass in Kg **/
    private double mass;
    /** Power multiplier factor **/
    private double power;
    /** Time to full power, in seconds. **/
    private double fullpowertime;
    /** The model **/
    private ModelComponent model;
    /** Whether to render self-shadow for this object. **/
    private boolean selfShadow = true;
    /** Responsiveness in [0,1] **/
    private double responsiveness;
    /** Drag in [0,1] **/
    private double drag;

    public MachineDefinition() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public double getSize() {
        return size;
    }

    public void setSize(Double size) {
        this.size = size;
    }

    public double getMass() {
        return mass;
    }

    public void setMass(Double mass) {
        this.mass = mass;
    }

    public double getFullpowertime() {
        return fullpowertime;
    }

    public void setFullpowertime(Double fullpowertime) {
        this.fullpowertime = fullpowertime;
    }

    public double getResponsiveness() {
        return responsiveness;
    }

    public void setResponsiveness(Double responsiveness) {
        this.responsiveness = responsiveness;
    }

    public double getPower() {
        return power;
    }

    public void setPower(Double power) {
        this.power = power;
    }

    public double getDrag() {
        return drag;
    }

    public void setDrag(Double drag) {
        this.drag = drag;
    }

    public ModelComponent getModel() {
        return model;
    }

    public void setModel(ModelComponent model) {
        this.model = model;
    }

    public boolean isSelfShadow() {
        return selfShadow;
    }

    public void setSelfShadow(boolean selfShadow) {
        this.selfShadow = selfShadow;
    }

    public void setShadowValues(double[] shadowValues) {
        this.selfShadow = shadowValues != null;
    }
    public void setShadowvalues(double[] shadowValues) {
        this.setShadowValues(shadowValues);
    }

    @Override
    public String toString() {
        return getName();
    }
}
