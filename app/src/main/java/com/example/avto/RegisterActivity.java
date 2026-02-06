package com.example.avto;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.textfield.TextInputLayout;

public class RegisterActivity extends AppCompatActivity {

    private EditText etUsername, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister;
    private CheckBox cbAgreeTerms;
    private MaterialButtonToggleGroup toggleAuthMode;
    private DatabaseHelper dbHelper;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        dbHelper = DatabaseHelper.getInstance(this);

        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnRegister = findViewById(R.id.btnRegister);
        toggleAuthMode = findViewById(R.id.toggleAuthMode);

        // Программно создаем и добавляем чекбокс соглашения
        setupTermsCheckbox();

        toggleAuthMode.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (isChecked) {
                    if (checkedId == R.id.btnLoginTab) {
                        Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                        startActivity(intent);
                        finish();
                    }
                }
            }
        });

        btnRegister.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!validateForm()) {
                    return;
                }

                String fullName = etUsername.getText().toString().trim();
                String email = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();

                if (dbHelper.checkUserExists(email)) {
                    Toast.makeText(RegisterActivity.this, "Пользователь с таким email уже существует", Toast.LENGTH_SHORT).show();
                    return;
                }

                long val = addUserWithFullName(email, password, fullName);
                if (val > 0) {
                    Toast.makeText(RegisterActivity.this, "Регистрация прошла успешно!", Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
                    intent.putExtra("email", email);
                    intent.putExtra("password", password);
                    startActivity(intent);
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Ошибка регистрации", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupTermsCheckbox() {
        // Находим контейнер для чекбокса
        ConstraintLayout termsContainer = findViewById(R.id.termsContainer);
        if (termsContainer == null) return;

        // Создаем горизонтальный layout для чекбокса и текста
        LinearLayout horizontalLayout = new LinearLayout(this);
        horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
        horizontalLayout.setGravity(android.view.Gravity.CENTER_VERTICAL);
        horizontalLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        // Создаем чекбокс
        cbAgreeTerms = new CheckBox(this);
        cbAgreeTerms.setText("");
        cbAgreeTerms.setPadding(0, 0, dpToPx(8), 0);

        // Настраиваем цвет чекбокса
        int[][] states = new int[][] {
                new int[] { android.R.attr.state_checked },
                new int[] { -android.R.attr.state_checked }
        };
        int[] colors = new int[] {
                getResources().getColor(R.color.primary),
                getResources().getColor(R.color.text_secondary)
        };
        cbAgreeTerms.setButtonTintList(new android.content.res.ColorStateList(states, colors));

        // Создаем TextView с кликабельной ссылкой
        TextView tvTerms = new TextView(this);

        // Создаем кликабельный текст
        String fullText = getString(R.string.agree_to_terms) + " " + getString(R.string.terms_link);
        SpannableString spannableString = new SpannableString(fullText);

        // Находим позицию начала ссылки
        int linkStart = fullText.indexOf(getString(R.string.terms_link));
        int linkEnd = linkStart + getString(R.string.terms_link).length();

        // Создаем кликабельный span
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View widget) {
                showTermsDialog();
            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setColor(getResources().getColor(R.color.primary));
                ds.setUnderlineText(false);
            }
        };

        spannableString.setSpan(clickableSpan, linkStart, linkEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvTerms.setText(spannableString);
        tvTerms.setTextSize(14);
        tvTerms.setTextColor(getResources().getColor(R.color.text_secondary));
        tvTerms.setMovementMethod(LinkMovementMethod.getInstance());
        tvTerms.setClickable(true);

        // Добавляем элементы в горизонтальный layout
        horizontalLayout.addView(cbAgreeTerms);
        horizontalLayout.addView(tvTerms);

        // Добавляем горизонтальный layout в контейнер
        termsContainer.addView(horizontalLayout);
    }

    private void showTermsDialog() {
        TermsDialog dialog = new TermsDialog(this, new TermsDialog.OnTermsAcceptedListener() {
            @Override
            public void onTermsAccepted() {
                if (cbAgreeTerms != null) {
                    cbAgreeTerms.setChecked(true);
                }
            }
        });
        dialog.setCancelable(true);
        dialog.show();
    }

    private boolean validateForm() {
        String fullName = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();
        String confirmPassword = etConfirmPassword.getText().toString().trim();

        // Сброс ошибок
        etUsername.setError(null);
        etEmail.setError(null);
        etPassword.setError(null);
        etConfirmPassword.setError(null);

        if (fullName.isEmpty()) {
            etUsername.setError("Введите имя пользователя");
            return false;
        }

        if (email.isEmpty()) {
            etEmail.setError("Введите email");
            return false;
        }

        if (!isValidEmail(email)) {
            etEmail.setError("Введите корректный email");
            return false;
        }

        if (password.isEmpty()) {
            etPassword.setError("Введите пароль");
            return false;
        }

        if (password.length() < 8) {
            etPassword.setError("Пароль должен содержать минимум 8 символов");
            return false;
        }

        if (confirmPassword.isEmpty()) {
            etConfirmPassword.setError("Подтвердите пароль");
            return false;
        }

        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Пароли не совпадают");
            return false;
        }

        // Проверяем чекбокс соглашения
        if (cbAgreeTerms == null || !cbAgreeTerms.isChecked()) {
            Toast.makeText(this, "Для регистрации необходимо принять пользовательское соглашение", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }

    private boolean isValidEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private long addUserWithFullName(String email, String password, String fullName) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("email", email);
        values.put("password", password);
        values.put("full_name", fullName);
        long result = db.insert("users", null, values);
        return result;
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (dbHelper != null) {
            dbHelper.close();
        }
    }
}