package com.ether.society;

public class TimeManager {
    private int currentYear;
    private int currentMonth; // 0-11
    private long totalTurns;
    
    // Start at 20,000 BC
    private static final int START_YEAR = -20000;

    public TimeManager() {
        this.currentYear = START_YEAR;
        this.currentMonth = 0;
        this.totalTurns = 0;
    }

    public void advanceMonth() {
        currentMonth++;
        if (currentMonth >= 12) {
            currentMonth = 0;
            currentYear++;
        }
        totalTurns++;
    }
    
    // Backward compatibility if needed, or just use advanceMonth in loop
    public void advanceYear(int years) {
        for (int i = 0; i < years * 12; i++) {
            advanceMonth();
        }
    }

    public int getCurrentYear() {
        return currentYear;
    }
    
    public int getCurrentMonth() {
        return currentMonth;
    }

    public long getTotalTurns() {
        return totalTurns;
    }
    
    public String getFormattedDate() {
        String era = currentYear < 0 ? "BC" : "AD";
        int year = Math.abs(currentYear);
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return String.format("%s %d %s", months[currentMonth], year, era);
    }
    
    public String getFormattedYear() {
        return getFormattedDate();
    }
    
    public void reset() {
        this.currentYear = START_YEAR;
        this.currentMonth = 0;
        this.totalTurns = 0;
    }
}
