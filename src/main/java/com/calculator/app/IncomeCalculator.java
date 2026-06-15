package com.calculator.app;

import java.util.List;

public class IncomeCalculator {

    private final LoanCalculator loanCalculator;
    private final double monthlyMaintenanceCost; // 월 관리비 등 기타 고정 지출 (원)
    private final double targetMonthlyProfit;    // 목표 월 순이익 (원, 0이면 손익분기점)

    public IncomeCalculator(LoanCalculator loanCalculator, double monthlyMaintenanceCost, double targetMonthlyProfit) {
        if (monthlyMaintenanceCost < 0) throw new IllegalArgumentException("월 관리비는 0 이상이어야 합니다.");
        if (targetMonthlyProfit < 0) throw new IllegalArgumentException("목표 월 순이익은 0 이상이어야 합니다.");

        this.loanCalculator = loanCalculator;
        this.monthlyMaintenanceCost = monthlyMaintenanceCost;
        this.targetMonthlyProfit = targetMonthlyProfit;
    }

    // 특정 월의 월 대출 상환액 반환
    public double getMonthlyLoanPayment(int month) {
        List<LoanCalculator.MonthlyPayment> schedule = loanCalculator.calculateSchedule();
        if (month < 1 || month > schedule.size()) return 0;
        return schedule.get(month - 1).total();
    }

    // 특정 월의 총 월 지출 (대출 상환 + 기타 고정 지출)
    public double getTotalMonthlyExpense(int month) {
        return getMonthlyLoanPayment(month) + monthlyMaintenanceCost;
    }

    // 손익분기 또는 목표 수익을 위한 최소 월 임대료 (특정 월 기준)
    public double getMinimumMonthlyRent(int month) {
        return getTotalMonthlyExpense(month) + targetMonthlyProfit;
    }

    // 원금 균등 상환처럼 월마다 납부액이 달라지는 경우 — 최초 납부액 기준 (가장 높은 달)
    public double getMaxMonthlyExpense() {
        return loanCalculator.calculateSchedule().stream()
                .mapToDouble(LoanCalculator.MonthlyPayment::total)
                .max()
                .orElse(0) + monthlyMaintenanceCost;
    }

    // 가장 높은 지출 기준 최소 월 임대료
    public double getMaxMinimumMonthlyRent() {
        return getMaxMonthlyExpense() + targetMonthlyProfit;
    }

    // 원리금 균등 / 만기 일시 상환처럼 고정 납부액 기준 최소 월 임대료
    public double getFixedMinimumMonthlyRent() {
        double loanPayment = loanCalculator.getFirstMonthPayment();
        return loanPayment + monthlyMaintenanceCost + targetMonthlyProfit;
    }

    // 특정 월 임대료로 얻는 순이익
    public double getMonthlyProfit(int month, double monthlyRent) {
        return monthlyRent - getTotalMonthlyExpense(month);
    }

    // 총 대출 기간 동안의 총 이자 비용
    public double getTotalInterestCost() {
        return loanCalculator.getTotalInterest();
    }

    // 총 상환 금액
    public double getTotalRepayment() {
        return loanCalculator.getTotalRepayment();
    }

    public double getMonthlyMaintenanceCost() { return monthlyMaintenanceCost; }
    public double getTargetMonthlyProfit() { return targetMonthlyProfit; }
}
