package com.calculator.app;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class LoanCalculatorTest {

    private static final double DELTA = 1.0; // 원 단위 오차 허용

    // --- 원리금 균등 상환 ---

    @Test
    @DisplayName("원리금 균등 상환: 월 납부액이 일정해야 한다")
    void equalPayment_monthlyPaymentIsConstant() {
        // 1억, 연 5%, 12개월, 거치 0개월
        LoanCalculator calc = new LoanCalculator(100_000_000, 0.05, 12, 0, LoanCalculator.RepaymentType.EQUAL_PAYMENT);
        List<LoanCalculator.MonthlyPayment> schedule = calc.calculateSchedule();

        double firstTotal = schedule.get(0).total();
        for (LoanCalculator.MonthlyPayment p : schedule) {
            assertEquals(firstTotal, p.total(), DELTA);
        }
    }

    @Test
    @DisplayName("원리금 균등 상환: 마지막 달 잔액이 0이어야 한다")
    void equalPayment_finalBalanceIsZero() {
        LoanCalculator calc = new LoanCalculator(100_000_000, 0.05, 24, 0, LoanCalculator.RepaymentType.EQUAL_PAYMENT);
        List<LoanCalculator.MonthlyPayment> schedule = calc.calculateSchedule();

        double finalBalance = schedule.get(schedule.size() - 1).remainingBalance();
        assertEquals(0, finalBalance, DELTA);
    }

    @Test
    @DisplayName("원리금 균등 상환: 거치 기간 중 원금 상환이 없어야 한다")
    void equalPayment_gracePeriodNoPrincipal() {
        LoanCalculator calc = new LoanCalculator(100_000_000, 0.05, 24, 6, LoanCalculator.RepaymentType.EQUAL_PAYMENT);
        List<LoanCalculator.MonthlyPayment> schedule = calc.calculateSchedule();

        for (int i = 0; i < 6; i++) {
            assertEquals(0, schedule.get(i).principal(), DELTA);
        }
    }

    // --- 원금 균등 상환 ---

    @Test
    @DisplayName("원금 균등 상환: 매달 원금 상환액이 동일해야 한다")
    void equalPrincipal_principalIsConstant() {
        LoanCalculator calc = new LoanCalculator(100_000_000, 0.05, 12, 0, LoanCalculator.RepaymentType.EQUAL_PRINCIPAL);
        List<LoanCalculator.MonthlyPayment> schedule = calc.calculateSchedule();

        double expectedPrincipal = 100_000_000.0 / 12;
        for (LoanCalculator.MonthlyPayment p : schedule) {
            assertEquals(expectedPrincipal, p.principal(), DELTA);
        }
    }

    @Test
    @DisplayName("원금 균등 상환: 월 납부 총액이 점점 감소해야 한다")
    void equalPrincipal_totalDecreasing() {
        LoanCalculator calc = new LoanCalculator(100_000_000, 0.05, 12, 0, LoanCalculator.RepaymentType.EQUAL_PRINCIPAL);
        List<LoanCalculator.MonthlyPayment> schedule = calc.calculateSchedule();

        for (int i = 0; i < schedule.size() - 1; i++) {
            assertTrue(schedule.get(i).total() > schedule.get(i + 1).total());
        }
    }

    // --- 만기 일시 상환 ---

    @Test
    @DisplayName("만기 일시 상환: 만기 전 원금 상환이 없어야 한다")
    void bullet_noPrincipalBeforeMaturity() {
        LoanCalculator calc = new LoanCalculator(100_000_000, 0.05, 12, 0, LoanCalculator.RepaymentType.BULLET);
        List<LoanCalculator.MonthlyPayment> schedule = calc.calculateSchedule();

        for (int i = 0; i < schedule.size() - 1; i++) {
            assertEquals(0, schedule.get(i).principal(), DELTA);
        }
    }

    @Test
    @DisplayName("만기 일시 상환: 만기 달에 원금 전액 상환")
    void bullet_principalRepaidAtMaturity() {
        double loanAmount = 100_000_000;
        LoanCalculator calc = new LoanCalculator(loanAmount, 0.05, 12, 0, LoanCalculator.RepaymentType.BULLET);
        List<LoanCalculator.MonthlyPayment> schedule = calc.calculateSchedule();

        LoanCalculator.MonthlyPayment lastMonth = schedule.get(schedule.size() - 1);
        assertEquals(loanAmount, lastMonth.principal(), DELTA);
        assertEquals(0, lastMonth.remainingBalance(), DELTA);
    }

    // --- 유효성 검증 ---

    @Test
    @DisplayName("대출 금액이 0 이하이면 예외 발생")
    void invalidLoanAmount_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new LoanCalculator(-1, 0.05, 12, 0, LoanCalculator.RepaymentType.EQUAL_PAYMENT));
    }

    @Test
    @DisplayName("거치 기간이 대출 기간 이상이면 예외 발생")
    void invalidGraceTerm_throwsException() {
        assertThrows(IllegalArgumentException.class, () ->
                new LoanCalculator(100_000_000, 0.05, 12, 12, LoanCalculator.RepaymentType.EQUAL_PAYMENT));
    }

    // --- 총 상환 금액 ---

    @Test
    @DisplayName("총 상환 금액은 대출 원금보다 커야 한다 (이자 존재)")
    void totalRepayment_greaterThanPrincipal() {
        LoanCalculator calc = new LoanCalculator(100_000_000, 0.05, 12, 0, LoanCalculator.RepaymentType.EQUAL_PAYMENT);
        assertTrue(calc.getTotalRepayment() > 100_000_000);
    }
}
