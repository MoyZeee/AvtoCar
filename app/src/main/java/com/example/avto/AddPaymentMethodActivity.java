    package com.example.avto;

    import android.content.Intent;
    import android.content.SharedPreferences;
    import android.os.Bundle;
    import android.text.Editable;
    import android.text.TextWatcher;
    import android.view.View;
    import android.widget.Button;
    import android.widget.EditText;
    import android.widget.ImageView;
    import android.widget.TextView;
    import android.widget.Toast;

    import androidx.appcompat.app.AppCompatActivity;
    import androidx.core.content.ContextCompat;

    import java.util.Calendar;

    public class AddPaymentMethodActivity extends AppCompatActivity {

        private EditText cardNumberInput;
        private EditText cardHolderInput;
        private EditText expiryInput;
        private EditText cvvInput;

        private TextView previewCardNumber;
        private TextView previewCardHolder;
        private TextView previewExpiry;
        private ImageView previewCardType;

        private Button saveButton;

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_add_payment_method);

            setupViews();
            setupPreview();
        }

        private void setupViews() {
            cardNumberInput = findViewById(R.id.cardNumberInput);
            cardHolderInput = findViewById(R.id.cardHolderInput);
            expiryInput = findViewById(R.id.expiryInput);
            cvvInput = findViewById(R.id.cvvInput);

            previewCardNumber = findViewById(R.id.previewCardNumber);
            previewCardHolder = findViewById(R.id.previewCardHolder);
            previewExpiry = findViewById(R.id.previewExpiry);
            previewCardType = findViewById(R.id.previewCardType);

            saveButton = findViewById(R.id.saveButton);
            Button cancelButton = findViewById(R.id.cancelButton);


            ImageView backButton = findViewById(R.id.backButton);

            saveButton.setOnClickListener(v -> saveCard());
            cancelButton.setOnClickListener(v -> finish());


            backButton.setOnClickListener(v -> finish());

            setupInputMasks();
            setupTextWatchers();
        }

        private void setupPreview() {
            // Инициализация превью
            previewCardNumber.setText("••••  ••••  ••••  ••••");
            previewCardHolder.setText("ИМЯ ФАМИЛИЯ");
            previewExpiry.setText("ММ/ГГ");
            previewCardType.setVisibility(View.INVISIBLE);
        }

        private void setupTextWatchers() {
            // Обновление превью номера карты
            cardNumberInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updatePreviewCardNumber(s.toString());
                    detectCardType(s.toString().replace(" ", ""));
                }

                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Обновление превью имени держателя
            cardHolderInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updatePreviewCardHolder(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Обновление превью срока действия
            expiryInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    updatePreviewExpiry(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            // Валидация в реальном времени
            cardNumberInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateCardNumber(s.toString().replace(" ", ""));
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            cardHolderInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateCardHolder(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            expiryInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateExpiry(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });

            cvvInput.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    validateCvv(s.toString());
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }

        private void updatePreviewCardNumber(String number) {
            String cleanNumber = number.replace(" ", "");
            if (cleanNumber.isEmpty()) {
                previewCardNumber.setText("••••  ••••  ••••  ••••");
            } else {
                StringBuilder masked = new StringBuilder();
                for (int i = 0; i < cleanNumber.length(); i++) {
                    if (i < cleanNumber.length() - 4) {
                        masked.append("•");
                    } else {
                        masked.append(cleanNumber.charAt(i));
                    }
                    if ((i + 1) % 4 == 0 && i < cleanNumber.length() - 1) {
                        masked.append(" ");
                    }
                }
                // Добавляем недостающие маски
                while (masked.length() < 19) {
                    if (masked.length() % 5 == 4) {
                        masked.append(" ");
                    } else {
                        masked.append("•");
                    }
                }
                previewCardNumber.setText(masked.toString());
            }
        }

        private void detectCardType(String number) {
            if (number.length() >= 2) {
                String firstTwo = number.substring(0, 2);
                String firstOne = number.substring(0, 1);

                if (firstOne.equals("4")) {
                    previewCardType.setImageResource(R.drawable.ic_visa);
                    previewCardType.setVisibility(View.VISIBLE);
                } else if (firstTwo.matches("5[1-5]")) {
                    previewCardType.setImageResource(R.drawable.ic_mastercard);
                    previewCardType.setVisibility(View.VISIBLE);
                } else if (firstTwo.equals("22") && number.length() >= 4) {
                    previewCardType.setImageResource(R.drawable.ic_visa);
                    previewCardType.setVisibility(View.VISIBLE);
                } else {
                    previewCardType.setVisibility(View.INVISIBLE);
                }
            } else {
                previewCardType.setVisibility(View.INVISIBLE);
            }
        }

        private void updatePreviewCardHolder(String name) {
            if (name.isEmpty()) {
                previewCardHolder.setText("ИМЯ ФАМИЛИЯ");
            } else {
                previewCardHolder.setText(name.toUpperCase());
            }
        }

        private void updatePreviewExpiry(String expiry) {
            if (expiry.isEmpty()) {
                previewExpiry.setText("ММ/ГГ");
            } else {
                previewExpiry.setText(expiry);
            }
        }

        private void validateCardNumber(String number) {
            if (number.isEmpty()) {
                cardNumberInput.setError(null);
                saveButton.setEnabled(false);
                saveButton.setAlpha(0.5f);
            } else if (number.length() < 16) {
                cardNumberInput.setError("Некорректный номер карты");
                saveButton.setEnabled(false);
                saveButton.setAlpha(0.5f);
            } else {
                cardNumberInput.setError(null);
                checkAllFields();
            }
        }

        private void validateCardHolder(String name) {
            if (name.isEmpty()) {
                cardHolderInput.setError(null);
                saveButton.setEnabled(false);
                saveButton.setAlpha(0.5f);
            } else if (name.length() < 3) {
                cardHolderInput.setError("Введите полное имя");
                saveButton.setEnabled(false);
                saveButton.setAlpha(0.5f);
            } else {
                cardHolderInput.setError(null);
                checkAllFields();
            }
        }

        private void validateExpiry(String expiry) {
            if (expiry.isEmpty()) {
                expiryInput.setError(null);
                saveButton.setEnabled(false);
                saveButton.setAlpha(0.5f);
            } else if (!expiry.matches("\\d{2}/\\d{2}")) {
                expiryInput.setError("Формат ММ/ГГ");
                saveButton.setEnabled(false);
                saveButton.setAlpha(0.5f);
            } else {
                String[] parts = expiry.split("/");
                int month = Integer.parseInt(parts[0]);
                int year = Integer.parseInt(parts[1]);

                Calendar calendar = Calendar.getInstance();
                int currentYear = calendar.get(Calendar.YEAR) % 100;
                int currentMonth = calendar.get(Calendar.MONTH) + 1;

                if (month < 1 || month > 12) {
                    expiryInput.setError("Некорректный месяц");
                    saveButton.setEnabled(false);
                    saveButton.setAlpha(0.5f);
                } else if (year < currentYear || (year == currentYear && month < currentMonth)) {
                    expiryInput.setError("Срок действия истек");
                    saveButton.setEnabled(false);
                    saveButton.setAlpha(0.5f);
                } else {
                    expiryInput.setError(null);
                    checkAllFields();
                }
            }
        }

        private void validateCvv(String cvv) {
            if (cvv.isEmpty()) {
                cvvInput.setError(null);
                saveButton.setEnabled(false);
                saveButton.setAlpha(0.5f);
            } else if (cvv.length() < 3) {
                cvvInput.setError("CVV должен содержать 3 цифры");
                saveButton.setEnabled(false);
                saveButton.setAlpha(0.5f);
            } else {
                cvvInput.setError(null);
                checkAllFields();
            }
        }

        private void checkAllFields() {
            String cardNumber = cardNumberInput.getText().toString().replace(" ", "");
            String cardHolder = cardHolderInput.getText().toString();
            String expiry = expiryInput.getText().toString();
            String cvv = cvvInput.getText().toString();

            boolean isValid = cardNumber.length() == 16 &&
                    cardHolder.length() >= 3 &&
                    expiry.matches("\\d{2}/\\d{2}") &&
                    cvv.length() >= 3;

            if (isValid) {
                // Дополнительная проверка срока действия
                String[] parts = expiry.split("/");
                int month = Integer.parseInt(parts[0]);
                int year = Integer.parseInt(parts[1]);

                Calendar calendar = Calendar.getInstance();
                int currentYear = calendar.get(Calendar.YEAR) % 100;
                int currentMonth = calendar.get(Calendar.MONTH) + 1;

                if (month >= 1 && month <= 12 &&
                        (year > currentYear || (year == currentYear && month >= currentMonth))) {
                    saveButton.setEnabled(true);
                    saveButton.setAlpha(1.0f);
                } else {
                    saveButton.setEnabled(false);
                    saveButton.setAlpha(0.5f);
                }
            } else {
                saveButton.setEnabled(false);
                saveButton.setAlpha(0.5f);
            }
        }

        private void setupInputMasks() {
            // Маска для номера карты (XXXX XXXX XXXX XXXX)
            cardNumberInput.addTextChangedListener(new CreditCardNumberTextWatcher(cardNumberInput));

            // Маска для срока действия (MM/YY)
            expiryInput.addTextChangedListener(new ExpiryDateTextWatcher(expiryInput));

            // Ограничение на 3-4 цифры для CVV
            cvvInput.setFilters(new android.text.InputFilter[]{new android.text.InputFilter.LengthFilter(4)});
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

            // Валидация CVV (3-4 цифры)
            if (cvv.length() < 3 || cvv.length() > 4 || !cvv.matches("\\d+")) {
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
                if (text.length() > 16) {
                    text = text.substring(0, 16);
                }

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
                if (text.length() > 4) {
                    text = text.substring(0, 4);
                }

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