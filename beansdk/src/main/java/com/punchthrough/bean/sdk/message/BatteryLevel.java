package com.punchthrough.bean.sdk.message;

/**
 * Represents the current battery level of a Bean.
 */
public class BatteryLevel {

    private int percentage;

    /**
     * Constructor for a BatteryLevel class. Indicates the battery level of a Bean.
     * @param percentage The battery percentage reported by a Bean. 0 = 2.0v, 100 = 3.7v
     */
    public BatteryLevel(int percentage) {
        this.percentage = percentage;
    }

    /**
     * Get the battery level in percent.
     * @return Percent of battery life remaining, 0 to 100 inclusive
     */
    public int getPercentage() {
        return this.percentage;
    }

    /**
     * Get the battery level in volts.
     * @return Voltage of battery, 2.0 to 3.7 volts inclusive
     */
    public float getVoltage() {
        // Range mapping from http://stackoverflow.com/a/7506169/254187
        float inMin = 0;
        float inMax = 100;
        float outMin = (float) 2.0;
        float outMax = (float) 3.7;
        float x = this.percentage;

        return (x - inMin) * (outMax - outMin) / (inMax - inMin) + outMin;
    }
}
