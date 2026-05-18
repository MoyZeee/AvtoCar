package com.example.avto;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.ColorStateList;
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

import com.example.avto.databinding.ActivityRegisterBinding;

public class RegisterActivity extends AppCompatActivity {

    private ActivityRegisterBinding binding;
    private DatabaseHelper dbHelper;
    private SharedPreferences sharedPreferences;

    private static final int VALIDATION_DELAY = 800;
    private final Handler handler = new Handler(Looper.getMainLooper());

    private Runnable emailValidationRunnable;
    private Runnable passwordValidationRunnable;
    private Runnable confirmPasswordValidationRunnable;

    private boolean isEmailValid = false;
    private boolean isPasswordValid = false;
    private boolean isConfirmPasswordValid = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRegisterBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        dbHelper = DatabaseHelper.getInstance(this);
        sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE);

        setupValidationRunnables();
        setupUI();
        setupAnimations();
    }

    private void setupValidationRunnables() {
        emailValidationRunnable = () -> validateEmail(binding.etEmail.getText().toString().trim());
        passwordValidationRunnable = () -> validatePassword(binding.etPassword.getText().toString());
        confirmPasswordValidationRunnable = () -> validateConfirmPassword(
                binding.etPassword.getText().toString(),
                binding.etConfirmPassword.getText().toString()
        );
    }

    private void setupUI() {
        binding.btnLoginTab.setOnClickListener(v -> {
            startActivity(new Intent(RegisterActivity.this, LoginActivity.class));
            finish();
        });

        // Email validation
        binding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilEmail.setError(null);
                binding.tilEmail.setHelperText("Введите ваш email");
                binding.tilEmail.setHelperTextColor(ColorStateList.valueOf(
                        ContextCompat.getColor(RegisterActivity.this, R.color.text_secondary)));
                handler.removeCallbacks(emailValidationRunnable);
                handler.postDelayed(emailValidationRunnable, VALIDATION_DELAY);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Password validation
        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilPassword.setError(null);
                binding.tilPassword.setHelperText("Минимум 8 символов, буквы и цифры");
                binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(
                        ContextCompat.getColor(RegisterActivity.this, R.color.text_secondary)));
                handler.removeCallbacks(passwordValidationRunnable);
                handler.postDelayed(passwordValidationRunnable, VALIDATION_DELAY);

                // Также перепроверяем подтверждение пароля
                handler.removeCallbacks(confirmPasswordValidationRunnable);
                handler.postDelayed(confirmPasswordValidationRunnable, VALIDATION_DELAY);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // Confirm password validation
        binding.etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                binding.tilConfirmPassword.setError(null);
                binding.tilConfirmPassword.setHelperText("Подтвердите пароль");
                binding.tilConfirmPassword.setHelperTextColor(ColorStateList.valueOf(
                        ContextCompat.getColor(RegisterActivity.this, R.color.text_secondary)));
                handler.removeCallbacks(confirmPasswordValidationRunnable);
                handler.postDelayed(confirmPasswordValidationRunnable, VALIDATION_DELAY);
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        binding.btnRegister.setOnClickListener(v -> {
            String email = binding.etEmail.getText().toString().trim();
            String password = binding.etPassword.getText().toString();
            String confirmPassword = binding.etConfirmPassword.getText().toString();

            // Принудительная валидация
            validateEmail(email);
            validatePassword(password);
            validateConfirmPassword(password, confirmPassword);

            if (isEmailValid && isPasswordValid && isConfirmPasswordValid) {
                registerUser(email, password);
            } else {
                Toast.makeText(this, "Пожалуйста, заполните все поля корректно", Toast.LENGTH_SHORT).show();
            }
        });

        updateRegisterButtonState();
    }

    private void validateEmail(String email) {
        if (email.isEmpty()) {
            binding.tilEmail.setError(null);
            binding.tilEmail.setHelperText("Введите ваш email");
            binding.tilEmail.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.text_secondary)));
            isEmailValid = false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Введите корректный email");
            binding.tilEmail.setHelperText("✗ Некорректный email");
            binding.tilEmail.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.error)));
            isEmailValid = false;
        } else if (dbHelper.checkUserExists(email)) {
            binding.tilEmail.setError("Пользователь с таким email уже существует");
            binding.tilEmail.setHelperText("✗ Email уже используется");
            binding.tilEmail.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.error)));
            isEmailValid = false;
        } else {
            binding.tilEmail.setError(null);
            binding.tilEmail.setHelperText("✓ Email доступен");
            binding.tilEmail.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.success)));
            isEmailValid = true;
        }
        updateRegisterButtonState();
    }

    private void validatePassword(String password) {
        if (password.isEmpty()) {
            binding.tilPassword.setHelperText("Минимум 8 символов, буквы и цифры");
            binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.text_secondary)));
            binding.tilPassword.setError(null);
            isPasswordValid = false;
        } else if (password.length() < 8) {
            binding.tilPassword.setError("Пароль слишком короткий");
            binding.tilPassword.setHelperText("✗ Минимум 8 символов");
            binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.error)));
            isPasswordValid = false;
        } else if (!isPasswordComplex(password)) {
            binding.tilPassword.setError("Пароль должен содержать буквы и цифры");
            binding.tilPassword.setHelperText("✗ Нужны буквы и цифры");
            binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.error)));
            isPasswordValid = false;
        } else {
            binding.tilPassword.setHelperText("✓ Надежный пароль");
            binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.success)));
            binding.tilPassword.setError(null);
            isPasswordValid = true;
        }
        updateRegisterButtonState();
    }

    private void validateConfirmPassword(String password, String confirmPassword) {
        if (confirmPassword.isEmpty()) {
            binding.tilConfirmPassword.setHelperText("Подтвердите пароль");
            binding.tilConfirmPassword.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.text_secondary)));
            binding.tilConfirmPassword.setError(null);
            isConfirmPasswordValid = false;
        } else if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError("Пароли не совпадают");
            binding.tilConfirmPassword.setHelperText("✗ Пароли не совпадают");
            binding.tilConfirmPassword.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.error)));
            isConfirmPasswordValid = false;
        } else {
            binding.tilConfirmPassword.setHelperText("✓ Пароли совпадают");
            binding.tilConfirmPassword.setHelperTextColor(ColorStateList.valueOf(
                    ContextCompat.getColor(this, R.color.success)));
            binding.tilConfirmPassword.setError(null);
            isConfirmPasswordValid = true;
        }
        updateRegisterButtonState();
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

    private void updateRegisterButtonState() {
        boolean isEnabled = isEmailValid && isPasswordValid && isConfirmPasswordValid;
        binding.btnRegister.setEnabled(isEnabled);
        binding.btnRegister.setAlpha(isEnabled ? 1.0f : 0.6f);
    }

    private void registerUser(String email, String password) {
        // Показываем прогресс
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.btnRegister.setEnabled(false);

        new Thread(() -> {
            try {
                Thread.sleep(1000);

                long result = dbHelper.addUser(email, password);

                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);

                    if (result != -1) {
                        Toast.makeText(RegisterActivity.this, "Регистрация успешна!", Toast.LENGTH_SHORT).show();

                        // Сохраняем email
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("user_email", email);
                        editor.apply();

                        startActivity(new Intent(RegisterActivity.this, ProfileActivity.class));
                        finish();
                    } else {
                        Toast.makeText(RegisterActivity.this, "Ошибка регистрации", Toast.LENGTH_SHORT).show();
                        binding.btnRegister.setEnabled(true);
                        binding.btnRegister.setAlpha(1.0f);
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    binding.progressBar.setVisibility(View.GONE);
                    binding.btnRegister.setEnabled(true);
                });
            }
        }).start();
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

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(emailValidationRunnable);
        handler.removeCallbacks(passwordValidationRunnable);
        handler.removeCallbacks(confirmPasswordValidationRunnable);
    }
}