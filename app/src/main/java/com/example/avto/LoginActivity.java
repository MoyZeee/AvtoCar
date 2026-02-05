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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Используем синглтон
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

        // Слушатели изменений текста
        binding.etEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validateEmail(s.toString());
                updateLoginButtonState();
            }
        });

        binding.etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                validatePassword(s.toString());
                updateLoginButtonState();
            }
        });

        // Обработчик кнопки входа
        binding.btnLogin.setOnClickListener(v -> {
            if (System.currentTimeMillis() - lastClickTime < CLICK_DELAY) return;
            lastClickTime = System.currentTimeMillis();
            login();
        });
    }

    private void validateEmail(String email) {
        if (email.isEmpty()) {
            binding.tilEmail.setError(null);
            binding.tilEmail.setHelperText("Введите ваш email");
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.setError("Введите корректный email");
            shakeView(binding.tilEmail);
        } else {
            binding.tilEmail.setError(null);
            binding.tilEmail.setHelperText("✓ Корректно");
        }
    }

    private void validatePassword(String password) {
        if (password.isEmpty()) {
            binding.tilPassword.setHelperText("Минимум 8 символов");
            binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.text_secondary)));
            binding.tilPassword.setError(null);
        } else if (password.length() < 8) {
            binding.tilPassword.setHelperText("Минимум 8 символов");
            binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error)));
            binding.tilPassword.setError("Минимум 8 символов");
            shakeView(binding.tilPassword);
        } else if (!isPasswordComplex(password)) {
            binding.tilPassword.setHelperText("Должен содержать буквы и цифры");
            binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.error)));
            binding.tilPassword.setError("Должен содержать буквы и цифры");
            shakeView(binding.tilPassword);
        } else {
            binding.tilPassword.setHelperText("✓ Надежный пароль");
            binding.tilPassword.setHelperTextColor(ColorStateList.valueOf(ContextCompat.getColor(this, R.color.success)));
            binding.tilPassword.setError(null);
        }
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
        String email = binding.etEmail.getText().toString();
        String password = binding.etPassword.getText().toString();

        boolean isEmailValid = Patterns.EMAIL_ADDRESS.matcher(email).matches();
        boolean isPasswordValid = password.length() >= 8 && isPasswordComplex(password);
        boolean isFormValid = isEmailValid && isPasswordValid;

        binding.btnLogin.setEnabled(isFormValid);
        binding.btnLogin.setAlpha(isFormValid ? 1.0f : 0.5f);
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

                        // Сохраняем email пользователя
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
        binding.btnLogin.setAlpha(binding.btnLogin.isEnabled() ? 1.0f : 0.5f);
        binding.btnLogin.setEnabled(true);
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
        }
        binding.btnLoginTab.setChecked(true);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}