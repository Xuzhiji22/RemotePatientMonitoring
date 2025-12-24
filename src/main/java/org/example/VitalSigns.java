package org.example;

import java.time.LocalDateTime;

public class VitalSigns {
    // 对应你们界面上的 5 个核心数据
    public double bodyTemp;
    public int heartRate;
    public int respiratoryRate;
    public int systolicBP;  // 收缩压 (高压)
    public int diastolicBP; // 舒张压 (低压)
    public double ecgValue; // 这一刻的 ECG 电压值
    public LocalDateTime timestamp; // 记录时间

    public VitalSigns(double bodyTemp, int heartRate, int respiratoryRate, int systolicBP, int diastolicBP, double ecgValue) {
        this.bodyTemp = bodyTemp;
        this.heartRate = heartRate;
        this.respiratoryRate = respiratoryRate;
        this.systolicBP = systolicBP;
        this.diastolicBP = diastolicBP;
        this.ecgValue = ecgValue;
        this.timestamp = LocalDateTime.now();
    }

    @Override
    public String toString() {
        return String.format("HR: %d | BP: %d/%d | Temp: %.1f", heartRate, systolicBP, diastolicBP, bodyTemp);
    }
}