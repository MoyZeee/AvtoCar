package com.example.avto;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.avto.databinding.ActivityLoginBinding;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.google.android.material.snackbar.Snackbar;

public class LoginActivity extends AppCompatActivity {

    private ActivityLoginBinding binding;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;

    private long lastClickTime = 0;
    private static final long CLICK_DELAY = 1000L;
    private final Handler handler = new Handler(Looper.getMainLooper());

    // Задержка для показа ошибок после прекращения ввода
    private static final int VALIDATION_DELAY = 800;
    private final Runnable emailValidationRunnable = this::validateEmailAfterInput;
    private final Runnable passwordValidationRunnable = this::validatePasswordAfterInput;

    private boolean isEmailValidated = false;
    private boolean isPasswordValidated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = DatabaseHelper.getInstance(this);
        sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE);

        setupUI();
        setupAnimations();
    }

    private void setupUI() {
        // Слушатель для переключателя входа/регистрации
        binding.toggleAuthMode.addOnButtonCheckedListener(new MaterialButtonToggleGroup.OnButtonCheckedListener() {
            @Override
            public void onButtonChecked(MaterialButtonToggleGroup group, int checkedId, boolean isChecked) {
                if (checkedId == R.id.btnRegisterTab && isChecked) {
                    Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
                    startActivity(intent);
                    handler.postDelayed(() -> binding.btnLoginTab.setChecked(true), 100);
                }
            }
        });

        // Слушатели изменений текста с задержкой валидации
        binding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Очищаем ошибку при вводе, но не показываем новую сразу
                binding.tilEmail.setError(null);
                binding.tilEmail.setHelperText("Введите ваш email");
                binding.tilEmail.setHelperTextColor(ColorStateList.valueOf(
                        ContextCompat.getColor(LoginActivity.this, R.color.text_secondary)));

                // Отменяем предыдущую валидацию и запускаем новую с задержкой
                handler.removeCallbacks(emailValidationRunnable);
                handler.postDelayed(emailValidationRunnable, VALIDATION_DELAY);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Очищаем ошибку при вводе
                binding.tilPassword.setError(null);
                binding.tilPassword.setHelperText("Минимум 8 символов");
                binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(
                        ContextCompat.getColor(LoginActivity.this, R.color.text_secondary)));

                // Отменяем предыдущую валидацию и запускаем новую с задержкой
                handler.removeCallbacks(passwordValidationRunnable);
                handler.postDelayed(passwordValidationRunnable, VALIDATION_DELAY);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Обработчик кнопки входа
        binding.btnLogin.setOnClickListener(v -> {
            if (System.currentTimeMillis() - lastClickTime < CLICK_DELAY) return;
            lastClickTime = System.currentTimeMillis();

            // Принудительная валидация перед входом
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString();

            validateEmailNow(email);
            validatePasswordNow(password);

            if (isEmailValidated && isPasswordValidated) {
                login();
            } else {
                // Показываем Snackbar с ошибкой
                String errorMsg = "";
                if (!isEmailValidated && !isPasswordValidated) {
                    errorMsg = "Введите корректный email и пароль";
                } else if (!isEmailValidated) {
                    errorMsg = "Введите корректный email";
                } else if (!isPasswordValidated) {
                    errorMsg = "Пароль должен содержать минимум 8 символов, буквы и цифры";
                }
                Snackbar.make(binding.getRoot(), errorMsg, Snackbar.LENGTH_SHORT).show();
            }
        });

        // Инициализация начального состояния
        updateLoginButtonState();
    }

    // Валидация после завершения ввода (с задержкой)
    private void validateEmailAfterInput() {
        String email = binding.etEmail.getText().toString().trim();
        validateEmailNow(email);
    }

    private void validatePasswordAfterInput() {
        String password = binding.etPassword.getText().toString();
        validatePasswordNow(password);
    }

    // Основной метод валидации email
    private void validateEmailNow(String email) {
        if (email.isEmpty()) {
            binding.tilEmail.setError(null);
            binding.tilEmail.setHelperText("Введите ваш email");
            binding.tilEmail.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.text_secondary)));
            isEmailValidated = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Введите корректный email (пример: name@mail.ru)");
            binding.tilEmail.setHelperText("✗ Некорректный email");
            binding.tilEmail.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.error)));
            isEmailValidated = false;
            shakeView(binding.tilEmail);
        } else {
            binding.tilEmail.setError(null);
            binding.tilEmail.setHelperText("✓ Email корректен");
            binding.tilEmail.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.success)));
            isEmailValidated = true;
        }
        updateLoginButtonState();
    }

    // Основной метод валидации пароля
    private void validatePasswordNow(String password) {
        if (password.isEmpty()) {
            binding.tilPassword.setHelperText("Минимум 8 символов, буквы и цифры");
            binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.text_secondary)));
            binding.tilPassword.setError(null);
            isPasswordValidated = false;
        } else if (password.length() < 8) {
            binding.tilPassword.setError("Пароль слишком короткий");
            binding.tilPassword.setHelperText("✗ Минимум 8 символов");
            binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.error)));
            isPasswordValidated = false;
            shakeView(binding.tilPassword);
        } else if (!isPasswordComplex(password)) {
            binding.tilPassword.setError("Пароль должен содержать буквы и цифры");
            binding.tilPassword.setHelperText("✗ Нужны буквы и цифры");
            binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.error)));
            isPasswordValidated = false;
            shakeView(binding.tilPassword);
        } else {
            binding.tilPassword.setHelperText("✓ Надежный пароль");
            binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.success)));
            binding.tilPassword.setError(null);
            isPasswordValidated = true;
        }
        updateLoginButtonState();
    }

    private boolean isPasswordComplex(String password) {
        boolean hasLetter = false;
        boolean hasDigit = false;
        for (char c : password.toCharArray()) {
            if (Character.isLetter(c)) hasLetter = true;
            if (Character.isDigit(c)) hasDigit = true;
            if (hasLetter && hasDigit) return true;
        }
        return false;
    }

    private void updateLoginButtonState() {
        // Кнопка становится активной только когда оба поля валидны
        boolean isEnabled = isEmailValidated && isPasswordValidated;

        binding.btnLogin.setEnabled(isEnabled);
        binding.btnLogin.setAlpha(isEnabled ? 1.0f : 0.6f);

        // Обновляем анимацию кнопки
        if (isEnabled) {
            binding.btnLogin.animate()
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(200)
                    .start();
        }
    }

    private void login() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString();

        showProgress();

        new Thread(() -> {
            try {
                Thread.sleep(1000);

                handler.post(() -> {
                    hideProgress();
                    if (dbHelper.checkUser(email, password)) {
                        Toast.makeText(LoginActivity.this, "Вход выполнен успешно!", Toast.LENGTH_SHORT).show();

                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("user_email", email);
                        editor.apply();

                        if (!isProfileFilled(email)) {
                            startActivity(new Intent(LoginActivity.this, ProfileActivity.class));
                        } else {
                            startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        }
                        finish();
                    } else {
                        Toast.makeText(LoginActivity.this, "Неверный email или пароль", Toast.LENGTH_SHORT).show();
                        shakeView(binding.btnLogin);

                        // Сбрасываем валидацию пароля
                        isPasswordValidated = false;
                        binding.tilPassword.setError("Неверный email или пароль");
                        binding.etPassword.setText("");
                        updateLoginButtonState();
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
                handler.post(this::hideProgress);
            }
        }).start();
    }

    private void showProgress() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnLogin.setAlpha(0.5f);
        binding.btnLogin.setEnabled(false);
    }

    private void hideProgress() {
        binding.progressBar.setVisibility(View.GONE);
        binding.btnLogin.setAlpha(binding.btnLogin.isEnabled() ? 1.0f : 0.6f);
        binding.btnLogin.setEnabled(binding.btnLogin.isEnabled());
    }

    private boolean isProfileFilled(String email) {
        Cursor cursor = dbHelper.getUserProfile(email);
        if (cursor != null) {
            boolean hasData = cursor.moveToFirst();
            cursor.close();
            return hasData;
        }
        return false;
    }

    private void setupAnimations() {
        View[] viewsToAnimate = {binding.headerSection, binding.cardForm};

        for (int i = 0; i < viewsToAnimate.length; i++) {
            final View view = viewsToAnimate[i];
            view.setAlpha(0f);
            view.setTranslationY(50f);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(500)
                    .setStartDelay(100L * i)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .start();
        }
    }

    private void shakeView(View view) {
        ObjectAnimator shakeX = ObjectAnimator.ofFloat(view, "translationX", 0f, 10f, -10f, 10f, -10f, 0f);
        shakeX.setDuration(500);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(shakeX);
        animatorSet.start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String savedEmail = sharedPreferences.getString("user_email", "");
        if (!savedEmail.isEmpty()) {
            binding.etEmail.setText(savedEmail);

            handler.postDelayed(emailValidationRunnable, 100);
        }
        binding.btnLoginTab.setChecked(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Удаляем все колбэки из Handler при уничтожении
        handler.removeCallbacks(emailValidationRunnable);
        handler.removeCallbacks(passwordValidationRunnable);
    }
}