/*
 * 分区清理大师 (Partition Cleaner)
 * 作者：文强哥 (Johnny520)
 * GitHub: https://github.com/Johnny520
 * 版权 © 2026 文强哥 (Johnny520). All rights reserved.
 */

package com.example.partitioncleaner;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CalculatorActivity extends BaseActivity {

    private TextView tvExpr;
    private TextView tvResult;
    private String expr = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calculator);

        setSupportActionBar(findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        setTitle("计算器");

        tvExpr = findViewById(R.id.tv_expr);
        tvResult = findViewById(R.id.tv_result);

        LinearLayout grid = findViewById(R.id.grid);
        for (int r = 0; r < grid.getChildCount(); r++) {
            View row = grid.getChildAt(r);
            if (row instanceof LinearLayout) {
                LinearLayout lin = (LinearLayout) row;
                for (int c = 0; c < lin.getChildCount(); c++) {
                    View b = lin.getChildAt(c);
                    if (b instanceof MaterialButton) {
                        b.setOnClickListener(this::onButtonClick);
                    }
                }
            }
        }
    }

    private void onButtonClick(View v) {
        String t = ((MaterialButton) v).getText().toString();
        handle(t);
    }

    private void handle(String t) {
        switch (t) {
            case "C":
                expr = "";
                tvExpr.setText("");
                tvResult.setText("0");
                return;
            case "⌫":
                if (!expr.isEmpty()) {
                    expr = expr.substring(0, expr.length() - 1);
                    tvExpr.setText(expr);
                }
                return;
            case "=":
                evaluate();
                return;
            case "%":
                percent();
                return;
            case "±":
                negate();
                return;
            default:
                break;
        }

        if (isOperator(t)) {
            if (!expr.isEmpty() && isOperator(lastChar())) {
                expr = expr.substring(0, expr.length() - 1) + t;
            } else if (!expr.isEmpty()) {
                expr += t;
            }
            tvExpr.setText(expr);
        } else if (".".equals(t)) {
            if (!currentNumberHasDot()) {
                if (expr.isEmpty() || isOperator(lastChar())) {
                    expr += "0";
                }
                expr += ".";
                tvExpr.setText(expr);
            }
        } else {
            expr += t;
            tvExpr.setText(expr);
        }
    }

    private boolean isOperator(String s) {
        return "÷".equals(s) || "×".equals(s) || "−".equals(s) || "+".equals(s);
    }

    private boolean isOperator(char ch) {
        return ch == '÷' || ch == '×' || ch == '−' || ch == '+';
    }

    private char lastChar() {
        return expr.charAt(expr.length() - 1);
    }

    private boolean currentNumberHasDot() {
        int i = expr.length() - 1;
        while (i >= 0 && (Character.isDigit(expr.charAt(i)) || expr.charAt(i) == '.')) {
            if (expr.charAt(i) == '.') {
                return true;
            }
            i--;
        }
        return false;
    }

    private void percent() {
        int end = expr.length();
        int start = end;
        while (start > 0 && (Character.isDigit(expr.charAt(start - 1)) || expr.charAt(start - 1) == '.')) {
            start--;
        }
        if (start == end) {
            return;
        }
        String num = expr.substring(start);
        try {
            double v = Double.parseDouble(num) / 100.0;
            expr = expr.substring(0, start) + fmt(v);
            tvExpr.setText(expr);
        } catch (Exception ignored) {
            // invalid number, ignore
        }
    }

    private void negate() {
        int end = expr.length();
        int start = end;
        while (start > 0 && (Character.isDigit(expr.charAt(start - 1)) || expr.charAt(start - 1) == '.')) {
            start--;
        }
        if (start == end) {
            return;
        }
        boolean hasMinus = start > 0 && expr.charAt(start - 1) == '-'
                && (start - 1 == 0 || isOperator(expr.charAt(start - 2)));
        if (hasMinus) {
            expr = expr.substring(0, start - 1) + expr.substring(start);
        } else {
            expr = expr.substring(0, start) + "-" + expr.substring(start);
        }
        tvExpr.setText(expr);
    }

    private void evaluate() {
        if (expr.isEmpty()) {
            tvResult.setText("0");
            return;
        }
        try {
            double r = calc(expr);
            tvResult.setText(fmt(r));
        } catch (Exception e) {
            tvResult.setText("错误");
        }
    }

    private double calc(String e) throws Exception {
        String s = e.replace('÷', '/').replace('×', '*').replace('−', '-');
        s = s.replaceAll("\\s+", "");
        if (s.isEmpty()) {
            throw new Exception("empty");
        }
        List<Double> numbers = new ArrayList<>();
        List<Character> ops = new ArrayList<>();
        int i = 0;
        int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '+' || c == '*' || c == '/') {
                ops.add(c);
                i++;
            } else if (c == '-') {
                boolean unary = (i == 0) || isOpChar(s.charAt(i - 1));
                int j = i + 1;
                while (j < n && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) {
                    j++;
                }
                if (unary && j > i + 1) {
                    numbers.add(Double.parseDouble(s.substring(i, j)));
                    i = j;
                } else {
                    ops.add('-');
                    i++;
                }
            } else if (Character.isDigit(c) || c == '.') {
                int j = i;
                while (j < n && (Character.isDigit(s.charAt(j)) || s.charAt(j) == '.')) {
                    j++;
                }
                numbers.add(Double.parseDouble(s.substring(i, j)));
                i = j;
            } else {
                throw new Exception("bad char");
            }
        }

        for (int k = 0; k < ops.size(); ) {
            char op = ops.get(k);
            if (op == '*' || op == '/') {
                double a = numbers.get(k);
                double b = numbers.get(k + 1);
                double r;
                if (op == '*') {
                    r = a * b;
                } else {
                    if (b == 0) {
                        throw new ArithmeticException("divzero");
                    }
                    r = a / b;
                }
                numbers.set(k, r);
                numbers.remove(k + 1);
                ops.remove(k);
            } else {
                k++;
            }
        }

        double res = numbers.get(0);
        for (int k = 0; k < ops.size(); k++) {
            char op = ops.get(k);
            double b = numbers.get(k + 1);
            if (op == '+') {
                res += b;
            } else if (op == '-') {
                res -= b;
            }
        }
        return res;
    }

    private boolean isOpChar(char c) {
        return c == '+' || c == '*' || c == '/' || c == '-';
    }

    private String fmt(double d) {
        if (Double.isNaN(d) || Double.isInfinite(d)) {
            return "错误";
        }
        String s = String.format(Locale.getDefault(), "%.10f", d);
        s = s.replaceAll("0*$", "").replaceAll("\\.$", "");
        return s;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
