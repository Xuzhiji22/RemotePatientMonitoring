package org.example;

import java.util.ArrayList;
import java.util.List;

public class Patient {
    public int id;
    public String name;
    // 暂时用 List 代替数据库，存放在内存里
    public List<VitalSigns> history = new ArrayList<>();

    public Patient(int id, String name) {
        this.id = id;
        this.name = name;
    }

    // 添加一条新记录
    public void addRecord(VitalSigns v) {
        history.add(v);
        // 为了防止内存爆炸，只保留最近1000条（模拟缓存）
        if (history.size() > 1000) {
            history.remove(0);
        }
    }

    // 获取最新的一条数据
    public VitalSigns getLatestRecord() {
        if (history.isEmpty()) return null;
        return history.get(history.size() - 1);
    }
}