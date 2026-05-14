package com.example.avto;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Calendar;

public class AddPaymentMethodActivity extends AppCompatActivity {

    private EditText cardNumberInput;
    private EditText cardHolderInput;
    private EditText expiryInput;
    private EditText cvvInput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_payment_method);

        setupViews();
    }

    private void setupViews() {
        cardNumberInput = findViewById(R.id.cardNumberInput);
        cardHolderInput = findViewById(R.id.cardHolderInput);
        expiryInput = findViewById(R.id.expiryInput);
        cvvInput = findViewById(R.id.cvvInput);

        Button saveButton = findViewById(R.id.saveButton);
        Button cancelButton = findViewById(R.id.cancelButton);

        saveButton.setOnClickListener(v -> saveCard());
        cancelButton.setOnClickListener(v -> finish());

        // Настраиваем маски ввода
        setupInputMasks();
    }

    private void setupInputMasks() {
        // Маска для номера карты (XXXX XXXX XXXX XXXX)
        cardNumberInput.addTextChangedListener(new CreditCardNumberTextWatcher(cardNumberInput));

        // Маска для срока действия (MM/YY)
        expiryInput.addTextChangedListener(new ExpiryDateTextWatcher(expiryInput));

        // Ограничение на 3 цифры для CVV
        cvvInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(3)});
    }

    private void saveCard() {
        String cardNumber = cardNumberInput.getText().toString().replace(" ", "");
        String cardHolder = cardHolderInput.getText().toString().trim();
        String expiry = expiryInput.getText().toString().trim();
        String cvv = cvvInput.getText().toString().trim();

        if (!validateInput(cardNumber, cardHolder, expiry, cvv)) {
            return;
        }

        // Получаем email пользователя из SharedPreferences
        SharedPreferences sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE);
        String userEmail = sharedPreferences.getString("user_email", "");

        // Извлекаем последние 4 цифры для отображения
        String lastFourDigits;
        if (cardNumber.length() >= 4) {
            lastFourDigits = cardNumber.substring(cardNumber.length() - 4);
        } else {
            lastFourDigits = cardNumber;
        }

        // Сохраняем в базу данных
        DatabaseHelper dbHelper = DatabaseHelper.getInstance(this);
        long result = dbHelper.addPaymentMethod(
                userEmail,
                cardNumber,
                cardHolder,
                expiry,
                cvv,
                lastFourDigits
        );

        if (result != -1L) {
            Toast.makeText(this, "Карта успешно добавлена", Toast.LENGTH_SHORT).show();

            // Возвращаем результат в BookingActivity
            Intent resultIntent = new Intent();
            setResult(RESULT_OK, resultIntent);
            finish();
        } else {
            Toast.makeText(this, "Ошибка при добавлении карты", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInput(String cardNumber, String cardHolder, String expiry, String cvv) {
        // Валидация номера карты (16 цифр)
        if (cardNumber.length() != 16 || !cardNumber.matches("\\d+")) {
            cardNumberInput.setError("Некорректный номер карты");
            return false;
        }

        // Валидация имени держателя
        if (cardHolder.length() < 3) {
            cardHolderInput.setError("Введите имя держателя карты");
            return false;
        }

        // Валидация срока действия
        if (!isValidExpiryDate(expiry)) {
            expiryInput.setError("Некорректный срок действия");
            return false;
        }

        // Валидация CVV (3 цифры)
        if (cvv.length() != 3 || !cvv.matches("\\d+")) {
            cvvInput.setError("Некорректный CVV код");
            return false;
        }

        return true;
    }

    private boolean isValidExpiryDate(String expiry) {
        if (!expiry.matches("\\d{2}/\\d{2}")) {
            return false;
        }

        String[] parts = expiry.split("/");
        int month;
        int year;

        try {
            month = Integer.parseInt(parts[0]);
            year = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }

        if (month < 1 || month > 12) {
            return false;
        }

        // Проверяем, не истекла ли карта
        Calendar calendar = Calendar.getInstance();
        int currentYear = calendar.get(Calendar.YEAR) % 100;
        int currentMonth = calendar.get(Calendar.MONTH) + 1;

        if (year < currentYear) {
            return false;
        } else if (year == currentYear && month < currentMonth) {
            return false;
        }

        return true;
    }

    // Классы для масок ввода

    public static class CreditCardNumberTextWatcher implements TextWatcher {
        private final EditText editText;
        private boolean isUpdating = false;

        public CreditCardNumberTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (isUpdating) {
                return;
            }

            isUpdating = true;

            String text = s.toString().replace(" ", "");
            StringBuilder formatted = new StringBuilder();

            for (int i = 0; i < text.length(); i++) {
                if (i > 0 && i % 4 == 0) {
                    formatted.append(" ");
                }
                formatted.append(text.charAt(i));
            }

            editText.setText(formatted.toString());
            editText.setSelection(formatted.length());

            isUpdating = false;
        }
    }

    public static class ExpiryDateTextWatcher implements TextWatcher {
        private final EditText editText;
        private boolean isUpdating = false;

        public ExpiryDateTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (isUpdating) {
                return;
            }

            isUpdating = true;

            String text = s.toString().replace("/", "");
            StringBuilder formatted = new StringBuilder();

            for (int i = 0; i < text.length(); i++) {
                if (i == 2) {
                    formatted.append("/");
                }
                formatted.append(text.charAt(i));
            }

            editText.setText(formatted.toString());
            editText.setSelection(formatted.length());

            isUpdating = false;
        }
    }
}