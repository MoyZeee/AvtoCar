package com.example.avto;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProfileActivity extends AppCompatActivity {

    private EditText fullNameEditText;
    private EditText passportSeriesEditText;
    private EditText passportNumberEditText;
    private EditText licenseNumberEditText;
    private Button saveProfileButton;
    private Button logoutButton;
    private ImageView backArrow;
    private TextView profileEmail;
    private TextView profileName;
    private CheckBox dataProcessingCheckbox;
    private ImageView profilePhotoImageView;
    private ImageView cameraIcon;
    private LinearLayout notificationsLayout;
    private LinearLayout aboutLayout;
    private TextView statBookings;
    private TextView statDays;
    private TextView statRating;

    private SharedPreferences sharedPreferences;
    private DatabaseHelper dbHelper;
    private String currentPhotoPath;
    private String userEmail;

    private ExecutorService executorService;
    private Handler mainHandler;

    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int CAMERA_REQUEST = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        executorService = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());

        dbHelper = DatabaseHelper.getInstance(this);
        sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE);
        userEmail = sharedPreferences.getString("user_email", "");

        initViews();
        setupListeners();
        setupTextWatchers();
        loadProfileData();
        loadUserStatistics();

        // Отложенная анимация для предотвращения лагов
        mainHandler.postDelayed(this::animateViews, 300);
    }

    private void initViews() {
        fullNameEditText = findViewById(R.id.fullNameEditText);
        passportSeriesEditText = findViewById(R.id.passportSeriesEditText);
        passportNumberEditText = findViewById(R.id.passportNumberEditText);
        licenseNumberEditText = findViewById(R.id.licenseNumberEditText);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        logoutButton = findViewById(R.id.logoutButton);
        backArrow = findViewById(R.id.backArrow);
        profileEmail = findViewById(R.id.profileEmail);
        profileName = findViewById(R.id.profileName);
        dataProcessingCheckbox = findViewById(R.id.dataProcessingCheckbox);
        profilePhotoImageView = findViewById(R.id.profilePhotoImageView);
        cameraIcon = findViewById(R.id.cameraIcon);
        notificationsLayout = findViewById(R.id.notificationsLayout);
        aboutLayout = findViewById(R.id.aboutLayout);
        statBookings = findViewById(R.id.statBookings);
        statDays = findViewById(R.id.statDays);
        statRating = findViewById(R.id.statRating);
    }

    private void setupListeners() {
        backArrow.setOnClickListener(v -> finish());

        if (cameraIcon != null) {
            cameraIcon.setOnClickListener(v -> showPhotoSelectionDialog());
        }
        if (profilePhotoImageView != null) {
            profilePhotoImageView.setOnClickListener(v -> showPhotoSelectionDialog());
        }

        if (saveProfileButton != null) {
            saveProfileButton.setOnClickListener(v -> saveProfile());
        }

        if (logoutButton != null) {
            logoutButton.setOnClickListener(v -> showLogoutDialog());
        }

        if (notificationsLayout != null) {
            notificationsLayout.setOnClickListener(v -> {
                Intent intent = new Intent(this, NotificationsActivity.class);
                startActivity(intent);
            });
        }

        if (aboutLayout != null) {
            aboutLayout.setOnClickListener(v -> showAboutDialog());
        }
    }

    private void setupTextWatchers() {
        if (fullNameEditText != null) {
            fullNameEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
                @Override
                public void afterTextChanged(Editable s) {
                    updateProfileName(s.toString());
                }
            });
        }

        if (passportSeriesEditText != null && passportNumberEditText != null) {
            passportSeriesEditText.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    if (s.length() == 4) {
                        passportNumberEditText.requestFocus();
                    }
                }
                @Override
                public void afterTextChanged(Editable s) {}
            });
        }
    }

    private void updateProfileName(String fullName) {
        if (profileName != null) {
            if (fullName != null && !fullName.isEmpty()) {
                String[] parts = fullName.split(" ");
                String firstName = parts.length > 0 ? parts[0] : "Гость";
                profileName.setText(firstName);
            } else {
                profileName.setText("Гость");
            }
        }
    }

    private void loadProfileData() {
        executorService.execute(() -> {
            Cursor cursor = null;
            try {
                cursor = dbHelper.getUserProfile(userEmail);
                if (cursor != null && cursor.moveToFirst()) {
                    @SuppressLint("Range") String fullName = cursor.getString(cursor.getColumnIndex("full_name"));
                    @SuppressLint("Range") String passportSeries = cursor.getString(cursor.getColumnIndex("passport_series"));
                    @SuppressLint("Range") String passportNumber = cursor.getString(cursor.getColumnIndex("passport_number"));
                    @SuppressLint("Range") String licenseNumber = cursor.getString(cursor.getColumnIndex("license_number"));

                    mainHandler.post(() -> {
                        if (fullName != null && !fullName.isEmpty() && fullNameEditText != null) {
                            fullNameEditText.setText(fullName);
                            updateProfileName(fullName);
                        }
                        if (passportSeries != null && passportSeriesEditText != null) {
                            passportSeriesEditText.setText(passportSeries);
                        }
                        if (passportNumber != null && passportNumberEditText != null) {
                            passportNumberEditText.setText(passportNumber);
                        }
                        if (licenseNumber != null && licenseNumberEditText != null) {
                            licenseNumberEditText.setText(licenseNumber);
                        }
                    });
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (cursor != null) {
                    cursor.close();
                }
            }

            mainHandler.post(() -> {
                if (profileEmail != null) {
                    profileEmail.setText(userEmail);
                }
                loadProfilePhoto();
            });
        });
    }

    private void loadProfilePhoto() {
        executorService.execute(() -> {
            String profilePhotoPath = dbHelper.getUserProfilePhoto(userEmail);
            if (profilePhotoPath != null && !profilePhotoPath.isEmpty()) {
                currentPhotoPath = profilePhotoPath;
                File imgFile = new File(profilePhotoPath);
                if (imgFile.exists() && profilePhotoImageView != null) {
                    // Оптимизированная загрузка изображения
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2; // Уменьшаем размер изображения
                    options.inJustDecodeBounds = false;
                    Bitmap bitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath(), options);
                    if (bitmap != null) {
                        mainHandler.post(() -> profilePhotoImageView.setImageBitmap(bitmap));
                    }
                }
            }
        });
    }

    private void loadUserStatistics() {
        executorService.execute(() -> {
            try {
                Thread.sleep(300); // Уменьшено с 500
                int bookingsCount = dbHelper.getUserBookings(userEmail).size();
                int totalDays = 0;
                for (Booking b : dbHelper.getUserBookings(userEmail)) {
                    totalDays += b.getTotalDays();
                }
                final int bookingsCountFinal = bookingsCount;
                final int totalDaysFinal = totalDays;
                final double rating = 4.8;

                mainHandler.post(() -> {
                    if (statBookings != null) {
                        statBookings.setText(String.valueOf(bookingsCountFinal));
                    }
                    if (statDays != null) {
                        statDays.setText(String.valueOf(totalDaysFinal));
                    }
                    if (statRating != null) {
                        statRating.setText(String.valueOf(rating));
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private void showPhotoSelectionDialog() {
        String[] options = {"📷 Сделать фото", "🖼️ Выбрать из галереи", "🗑️ Удалить фото"};

        new MaterialAlertDialogBuilder(this)
                .setTitle("Фото профиля")
                .setItems(options, (dialog, which) -> {
                    if (which == 0) {
                        openCamera();
                    } else if (which == 1) {
                        pickFromGallery();
                    } else if (which == 2) {
                        deletePhoto();
                    }
                })
                .show();
    }

    private void openCamera() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, CAMERA_REQUEST);
        } else {
            Toast.makeText(this, "Камера недоступна", Toast.LENGTH_SHORT).show();
        }
    }

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void deletePhoto() {
        currentPhotoPath = null;
        if (profilePhotoImageView != null) {
            profilePhotoImageView.setImageResource(R.drawable.default_profile_photo);
        }
        Toast.makeText(this, "Фото удалено", Toast.LENGTH_SHORT).show();

        String fullName = fullNameEditText != null ? fullNameEditText.getText().toString().trim() : "";
        String passportSeries = passportSeriesEditText != null ? passportSeriesEditText.getText().toString().trim() : "";
        String passportNumber = passportNumberEditText != null ? passportNumberEditText.getText().toString().trim() : "";
        String licenseNumber = licenseNumberEditText != null ? licenseNumberEditText.getText().toString().trim() : "";

        executorService.execute(() -> {
            dbHelper.updateUserProfile(userEmail, fullName, passportSeries, passportNumber, licenseNumber, null);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    saveAndSetImage(selectedImageUri);
                }
            } else if (requestCode == CAMERA_REQUEST) {
                Bundle extras = data.getExtras();
                if (extras != null) {
                    Bitmap imageBitmap = (Bitmap) extras.get("data");
                    if (imageBitmap != null) {
                        executorService.execute(() -> {
                            Uri imageUri = saveBitmapToUri(imageBitmap);
                            if (imageUri != null) {
                                mainHandler.post(() -> saveAndSetImage(imageUri));
                            }
                        });
                    }
                }
            }
        }
    }

    private void saveAndSetImage(Uri imageUri) {
        executorService.execute(() -> {
            try {
                final String savedPath = saveImageToInternalStorage(imageUri);
                if (savedPath != null) {
                    currentPhotoPath = savedPath;
                    // Оптимизированная загрузка
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inSampleSize = 2;
                    Bitmap bitmap = BitmapFactory.decodeFile(savedPath, options);
                    if (bitmap != null && profilePhotoImageView != null) {
                        mainHandler.post(() -> profilePhotoImageView.setImageBitmap(bitmap));
                    }
                    mainHandler.post(() -> Toast.makeText(ProfileActivity.this, "Фото обновлено", Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> Toast.makeText(ProfileActivity.this, "Ошибка сохранения фото", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private Uri saveBitmapToUri(Bitmap bitmap) {
        try {
            File directory = new File(getFilesDir(), "profile_photos");
            if (!directory.exists()) directory.mkdirs();

            String fileName = "temp_photo_" + System.currentTimeMillis() + ".jpg";
            File file = new File(directory, fileName);

            FileOutputStream fos = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, fos);
            fos.close();

            return FileProvider.getUriForFile(this, getPackageName() + ".provider", file);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private String saveImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;

            File directory = new File(getFilesDir(), "profile_photos");
            if (!directory.exists()) directory.mkdirs();

            String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
            File file = new File(directory, fileName);

            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[8192]; // Увеличен буфер
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }

            outputStream.close();
            inputStream.close();

            return file.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void saveProfile() {
        if (saveProfileButton == null) return;

        final String fullName = fullNameEditText != null ? fullNameEditText.getText().toString().trim() : "";
        final String passportSeries = passportSeriesEditText != null ? passportSeriesEditText.getText().toString().trim() : "";
        final String passportNumber = passportNumberEditText != null ? passportNumberEditText.getText().toString().trim() : "";
        final String licenseNumber = licenseNumberEditText != null ? licenseNumberEditText.getText().toString().trim() : "";
        final String finalCurrentPhotoPath = currentPhotoPath;
        final Button button = saveProfileButton;

        if (fullName.isEmpty()) {
            showError("Введите ФИО");
            if (fullNameEditText != null) fullNameEditText.requestFocus();
            return;
        }

        if (passportSeries.isEmpty() || passportSeries.length() != 4) {
            showError("Введите корректную серию паспорта (4 цифры)");
            if (passportSeriesEditText != null) passportSeriesEditText.requestFocus();
            return;
        }

        if (passportNumber.isEmpty() || passportNumber.length() != 6) {
            showError("Введите корректный номер паспорта (6 цифр)");
            if (passportNumberEditText != null) passportNumberEditText.requestFocus();
            return;
        }

        if (licenseNumber.isEmpty()) {
            showError("Введите номер водительского удостоверения");
            if (licenseNumberEditText != null) licenseNumberEditText.requestFocus();
            return;
        }

        if (dataProcessingCheckbox == null || !dataProcessingCheckbox.isChecked()) {
            showError("Необходимо согласие на обработку персональных данных");
            return;
        }

        button.setEnabled(false);
        button.setText("Сохранение...");

        executorService.execute(() -> {
            try {
                Thread.sleep(300); // Уменьшено с 500
                final boolean isUpdated = dbHelper.updateUserProfile(userEmail, fullName, passportSeries,
                        passportNumber, licenseNumber, finalCurrentPhotoPath);

                mainHandler.post(() -> {
                    button.setEnabled(true);
                    button.setText("Сохранить изменения");

                    if (isUpdated) {
                        showSuccess("Профиль успешно сохранен!");
                        loadUserStatistics();
                    } else {
                        showError("Ошибка при сохранении профиля");
                    }
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
                mainHandler.post(() -> {
                    button.setEnabled(true);
                    button.setText("Сохранить изменения");
                    showError("Ошибка при сохранении");
                });
            }
        });
    }

    private void showLogoutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Выход из аккаунта")
                .setMessage("Вы уверены, что хотите выйти?")
                .setPositiveButton("Выйти", (dialog, which) -> logout())
                .setNegativeButton("Отмена", null)
                .show();
    }

    private void logout() {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.clear();
        editor.apply();

        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
        Toast.makeText(this, "Вы вышли из аккаунта", Toast.LENGTH_SHORT).show();
    }

    private void showAboutDialog() {
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_about, null);

        Button btnRate = dialogView.findViewById(R.id.btnRate);
        Button btnContact = dialogView.findViewById(R.id.btnContact);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.CustomDialogTheme)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        if (dialog.getWindow() != null) {
            // Устанавливаем ширину диалога на 90% от ширины экрана
            android.view.WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.92);
            params.height = android.view.ViewGroup.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(params);
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            dialog.getWindow().getAttributes().windowAnimations = R.style.DialogAnimation;
        }

        btnRate.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setData(Uri.parse("market://details?id=" + getPackageName()));
                startActivity(intent);
            } catch (Exception e) {
            }
            dialog.dismiss();
        });

        btnContact.setOnClickListener(v -> {
            showContactDialog();
            dialog.dismiss();
        });

        dialog.show();
    }

    private void showContactDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("📞 Контакты")
                .setMessage("📧 Email: support@avto.skr\n\n" +
                        "📱 Телефон: +7 (978) 976-88-98\n\n" +
                        "📍 Адрес: г. Симферополь, ул. Тверская, 10\n\n" +
                        "🕐 Режим работы:\n" +
                        "   Пн-Пт: 9:00 - 20:00\n" +
                        "   Сб-Вс: 10:00 - 18:00")
                .setPositiveButton("📞 Позвонить", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:+79789768898"));
                    startActivity(intent);
                })
                .setNeutralButton("✉️ Email", (dialog, which) -> {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("mailto:support@avto.skr"));
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Вопрос о приложении Авто.СКР");
                    startActivity(intent);
                })
                .setNegativeButton("❌ Отмена", null)
                .show();
    }

    private void showError(final String message) {
        mainHandler.post(() -> Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private void showSuccess(final String message) {
        mainHandler.post(() -> Toast.makeText(ProfileActivity.this, message, Toast.LENGTH_SHORT).show());
    }

    private void animateViews() {
        View[] views = {profilePhotoImageView, profileName, profileEmail};
        for (int i = 0; i < views.length; i++) {
            if (views[i] != null) {
                views[i].setAlpha(0f);
                views[i].animate()
                        .alpha(1f)
                        .setDuration(400)
                        .setStartDelay(100L * i)
                        .start();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdown();
        }
        mainHandler.removeCallbacksAndMessages(null);
    }
}