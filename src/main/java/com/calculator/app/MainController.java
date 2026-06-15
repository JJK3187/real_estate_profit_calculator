package com.calculator.app;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class MainController {

    @FXML private TextField loanAmountField;
    @FXML private TextField annualRateField;
    @FXML private TextField loanTermField;
    @FXML private TextField graceTermField;
    @FXML private TextField maintenanceCostField;
    @FXML private TextField targetProfitField;

    @FXML private RadioButton equalPaymentRadio;
    @FXML private RadioButton equalPrincipalRadio;
    @FXML private RadioButton bulletRadio;

    @FXML private TitledPane resultPane;
    @FXML private Label firstMonthPaymentLabel;
    @FXML private Label totalRepaymentLabel;
    @FXML private Label totalInterestLabel;
    @FXML private Label monthlyLoanLabel;
    @FXML private Label monthlyMaintenanceLabel;
    @FXML private Label totalMonthlyExpenseLabel;
    @FXML private Label minRentLabel;
    @FXML private Label maxRentLabel;
    @FXML private Label errorLabel;

    @FXML private TableView<ScheduleRow> scheduleTable;
    @FXML private TableColumn<ScheduleRow, Integer> colMonth;
    @FXML private TableColumn<ScheduleRow, String> colPrincipal;
    @FXML private TableColumn<ScheduleRow, String> colInterest;
    @FXML private TableColumn<ScheduleRow, String> colTotal;
    @FXML private TableColumn<ScheduleRow, String> colBalance;

    private final ToggleGroup repaymentGroup = new ToggleGroup();
    private static final NumberFormat FMT = NumberFormat.getNumberInstance(Locale.KOREA);

    @FXML
    public void initialize() {
        equalPaymentRadio.setToggleGroup(repaymentGroup);
        equalPrincipalRadio.setToggleGroup(repaymentGroup);
        bulletRadio.setToggleGroup(repaymentGroup);

        colMonth.setCellValueFactory(new PropertyValueFactory<>("month"));
        colPrincipal.setCellValueFactory(new PropertyValueFactory<>("principal"));
        colInterest.setCellValueFactory(new PropertyValueFactory<>("interest"));
        colTotal.setCellValueFactory(new PropertyValueFactory<>("total"));
        colBalance.setCellValueFactory(new PropertyValueFactory<>("balance"));
    }

    @FXML
    public void onCalculate() {
        errorLabel.setText("");
        resultPane.setVisible(false);

        try {
            double loanAmount = parseAmount(loanAmountField.getText(), "대출 금액");
            double annualRate = parseRate(annualRateField.getText());
            int loanTerm = parseInt(loanTermField.getText(), "대출 기간");
            int graceTerm = parseIntOptional(graceTermField.getText());
            double maintenance = parseAmountOptional(maintenanceCostField.getText());
            double targetProfit = parseAmountOptional(targetProfitField.getText());

            LoanCalculator.RepaymentType type = selectedRepaymentType();
            LoanCalculator loanCalc = new LoanCalculator(loanAmount, annualRate, loanTerm, graceTerm, type);
            IncomeCalculator incomeCalc = new IncomeCalculator(loanCalc, maintenance, targetProfit);

            updateResults(loanCalc, incomeCalc);
            resultPane.setVisible(true);

        } catch (IllegalArgumentException e) {
            errorLabel.setText("입력 오류: " + e.getMessage());
        }
    }

    private void updateResults(LoanCalculator loanCalc, IncomeCalculator incomeCalc) {
        List<LoanCalculator.MonthlyPayment> schedule = loanCalc.calculateSchedule();

        double firstPayment = loanCalc.getFirstMonthPayment();
        double totalRepayment = loanCalc.getTotalRepayment();
        double totalInterest = loanCalc.getTotalInterest();
        double monthlyExpense = incomeCalc.getTotalMonthlyExpense(1);
        double minRent = incomeCalc.getMinimumMonthlyRent(1);

        firstMonthPaymentLabel.setText(formatWon(firstPayment));
        totalRepaymentLabel.setText(formatWon(totalRepayment));
        totalInterestLabel.setText(formatWon(totalInterest));
        monthlyLoanLabel.setText(formatWon(firstPayment));
        monthlyMaintenanceLabel.setText(formatWon(incomeCalc.getMonthlyMaintenanceCost()));
        totalMonthlyExpenseLabel.setText(formatWon(monthlyExpense));

        String profitText = incomeCalc.getTargetMonthlyProfit() == 0 ? "손익분기" : "목표 수익 달성";
        minRentLabel.setText(profitText + " 최소 월 임대료: " + formatWon(minRent));

        // 원금 균등 상환: 첫 달(최고) 기준도 표시
        if (loanCalc.getType() == LoanCalculator.RepaymentType.EQUAL_PRINCIPAL) {
            double maxRent = incomeCalc.getMaxMinimumMonthlyRent();
            maxRentLabel.setText("* 원금 균등 상환은 첫 달 납부액이 최고 — 최대 필요 임대료: " + formatWon(maxRent));
        } else {
            maxRentLabel.setText("");
        }

        // 스케줄 테이블 채우기
        scheduleTable.getItems().clear();
        for (LoanCalculator.MonthlyPayment p : schedule) {
            scheduleTable.getItems().add(new ScheduleRow(
                    p.month(),
                    formatWon(p.principal()),
                    formatWon(p.interest()),
                    formatWon(p.total()),
                    formatWon(p.remainingBalance())
            ));
        }
    }

    private LoanCalculator.RepaymentType selectedRepaymentType() {
        if (equalPrincipalRadio.isSelected()) return LoanCalculator.RepaymentType.EQUAL_PRINCIPAL;
        if (bulletRadio.isSelected()) return LoanCalculator.RepaymentType.BULLET;
        return LoanCalculator.RepaymentType.EQUAL_PAYMENT;
    }

    private double parseAmount(String text, String fieldName) {
        try {
            double val = Double.parseDouble(text.trim().replace(",", ""));
            if (val <= 0) throw new IllegalArgumentException(fieldName + "은(는) 0보다 커야 합니다.");
            return val;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " 값이 올바르지 않습니다.");
        }
    }

    private double parseAmountOptional(String text) {
        if (text == null || text.isBlank()) return 0;
        try {
            double val = Double.parseDouble(text.trim().replace(",", ""));
            if (val < 0) throw new IllegalArgumentException("금액은 0 이상이어야 합니다.");
            return val;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("금액 값이 올바르지 않습니다.");
        }
    }

    private double parseRate(String text) {
        try {
            double val = Double.parseDouble(text.trim());
            if (val <= 0 || val > 100) throw new IllegalArgumentException("이자율은 0% 초과 100% 이하여야 합니다.");
            return val / 100.0;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("연 이자율 값이 올바르지 않습니다.");
        }
    }

    private int parseInt(String text, String fieldName) {
        try {
            int val = Integer.parseInt(text.trim());
            if (val <= 0) throw new IllegalArgumentException(fieldName + "은(는) 0보다 커야 합니다.");
            return val;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(fieldName + " 값이 올바르지 않습니다.");
        }
    }

    private int parseIntOptional(String text) {
        if (text == null || text.isBlank()) return 0;
        try {
            int val = Integer.parseInt(text.trim());
            if (val < 0) throw new IllegalArgumentException("거치 기간은 0 이상이어야 합니다.");
            return val;
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("거치 기간 값이 올바르지 않습니다.");
        }
    }

    private String formatWon(double amount) {
        return FMT.format(Math.round(amount)) + " 원";
    }

    // TableView용 데이터 클래스
    public static class ScheduleRow {
        private final int month;
        private final String principal;
        private final String interest;
        private final String total;
        private final String balance;

        public ScheduleRow(int month, String principal, String interest, String total, String balance) {
            this.month = month;
            this.principal = principal;
            this.interest = interest;
            this.total = total;
            this.balance = balance;
        }

        public int getMonth() { return month; }
        public String getPrincipal() { return principal; }
        public String getInterest() { return interest; }
        public String getTotal() { return total; }
        public String getBalance() { return balance; }
    }
}
