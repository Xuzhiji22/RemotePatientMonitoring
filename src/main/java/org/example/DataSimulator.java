package org.example;

import java.util.Random;

public class DataSimulator {
    private Random random = new Random();

    // 生成一个"正常范围"内的随机生命体征
    public VitalSigns generateNormalData() {
        // 模拟真实波动：体温 36.5 ~ 37.5
        double temp = 36.5 + random.nextDouble();

        // 心率 60 ~ 100
        int hr = 60 + random.nextInt(40);

        // 呼吸 12 ~ 20
        int rr = 12 + random.nextInt(8);

        // 血压 110/70 ~ 130/85
        int sys = 110 + random.nextInt(20);
        int dia = 70 + random.nextInt(15);

        // ECG 模拟正弦波 (简化版，后面我们可以用更复杂的算法)
        double ecg = Math.sin(System.currentTimeMillis() / 100.0);

        return new VitalSigns(temp, hr, rr, sys, dia, ecg);
    }
}