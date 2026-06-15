package com.calculator.app;

import java.util.ArrayList;
import java.util.List;

public class LoanCalculator {

    public enum RepaymentType {
        EQUAL_PAYMENT,    // 원리금 균등 상환
        EQUAL_PRINCIPAL,  // 원금 균등 상환
        BULLET            // 만기 일시 상환
    }

    public record MonthlyPayment(int month, double principal, double interest, double total, double remainingBalance) {}

    private final double loanAmount;       // 대출 원금 (원)
    private final double annualRate;       // 연 이자율 (예: 0.05 = 5%)
    private final int loanTermMonths;      // 대출 기간 (개월)
    private final int graceTermMonths;     // 거치 기간 (개월) - 만기 일시/원금 균등에서 이자만 납부
    private final RepaymentType type;

    public LoanCalculator(double loanAmount, double annualRate, int loanTermMonths, int graceTermMonths, RepaymentType type) {
        if (loanAmount <= 0) throw new IllegalArgumentException("대출 금액은 0보다 커야 합니다.");
        if (annualRate <= 0) throw new IllegalArgumentException("이자율은 0보다 커야 합니다.");
        if (loanTermMonths <= 0) throw new IllegalArgumentException("대출 기간은 0보다 커야 합니다.");
        if (graceTermMonths < 0 || graceTermMonths >= loanTermMonths)
            throw new IllegalArgumentException("거치 기간은 0 이상이며 대출 기간보다 작아야 합니다.");

        this.loanAmount = loanAmount;
        this.annualRate = annualRate;
        this.loanTermMonths = loanTermMonths;
        this.graceTermMonths = graceTermMonths;
        this.type = type;
    }

    public List<MonthlyPayment> calculateSchedule() {
        return switch (type) {
            case EQUAL_PAYMENT -> calcEqualPayment();
            case EQUAL_PRINCIPAL -> calcEqualPrincipal();
            case BULLET -> calcBullet();
        };
    }

    public double getTotalRepayment() {
        return calculateSchedule().stream().mapToDouble(MonthlyPayment::total).sum();
    }

    public double getTotalInterest() {
        return calculateSchedule().stream().mapToDouble(MonthlyPayment::interest).sum();
    }

    // 거치 기간 중 첫 달 월 납부액 (이자만)
    public double getFirstMonthPayment() {
        List<MonthlyPayment> schedule = calculateSchedule();
        if (schedule.isEmpty()) return 0;
        return schedule.get(0).total();
    }

    // 원리금 균등 상환: 거치기간은 이자만, 이후 원리금 균등
    private List<MonthlyPayment> calcEqualPayment() {
        List<MonthlyPayment> schedule = new ArrayList<>();
        double r = annualRate / 12.0;
        double balance = loanAmount;

        // 거치 기간: 이자만 납부
        for (int m = 1; m <= graceTermMonths; m++) {
            double interest = balance * r;
            schedule.add(new MonthlyPayment(m, 0, interest, interest, balance));
        }

        // 원리금 균등 상환 기간
        int repayMonths = loanTermMonths - graceTermMonths;
        // PMT 공식: P * r / (1 - (1+r)^(-n))
        double monthlyPayment = (r == 0) ? balance / repayMonths
                : balance * r / (1 - Math.pow(1 + r, -repayMonths));

        for (int m = graceTermMonths + 1; m <= loanTermMonths; m++) {
            double interest = balance * r;
            double principal = monthlyPayment - interest;
            balance -= principal;
            if (balance < 0) balance = 0; // 부동소수점 오차 보정
            schedule.add(new MonthlyPayment(m, principal, interest, monthlyPayment, balance));
        }

        return schedule;
    }

    // 원금 균등 상환: 거치기간은 이자만, 이후 원금 동일 + 잔여 이자
    private List<MonthlyPayment> calcEqualPrincipal() {
        List<MonthlyPayment> schedule = new ArrayList<>();
        double r = annualRate / 12.0;
        double balance = loanAmount;
        int repayMonths = loanTermMonths - graceTermMonths;
        double monthlyPrincipal = loanAmount / repayMonths;

        // 거치 기간: 이자만 납부
        for (int m = 1; m <= graceTermMonths; m++) {
            double interest = balance * r;
            schedule.add(new MonthlyPayment(m, 0, interest, interest, balance));
        }

        // 원금 균등 상환 기간
        for (int m = graceTermMonths + 1; m <= loanTermMonths; m++) {
            double interest = balance * r;
            double total = monthlyPrincipal + interest;
            balance -= monthlyPrincipal;
            if (balance < 0) balance = 0;
            schedule.add(new MonthlyPayment(m, monthlyPrincipal, interest, total, balance));
        }

        return schedule;
    }

    // 만기 일시 상환: 전 기간 이자만 납부, 마지막 달에 원금 일시 상환
    private List<MonthlyPayment> calcBullet() {
        List<MonthlyPayment> schedule = new ArrayList<>();
        double r = annualRate / 12.0;
        double monthlyInterest = loanAmount * r;

        for (int m = 1; m <= loanTermMonths; m++) {
            boolean isLastMonth = (m == loanTermMonths);
            double principal = isLastMonth ? loanAmount : 0;
            double total = isLastMonth ? loanAmount + monthlyInterest : monthlyInterest;
            double remaining = isLastMonth ? 0 : loanAmount;
            schedule.add(new MonthlyPayment(m, principal, monthlyInterest, total, remaining));
        }

        return schedule;
    }

    // Getters
    public double getLoanAmount() { return loanAmount; }
    public double getAnnualRate() { return annualRate; }
    public int getLoanTermMonths() { return loanTermMonths; }
    public int getGraceTermMonths() { return graceTermMonths; }
    public RepaymentType getType() { return type; }
}
