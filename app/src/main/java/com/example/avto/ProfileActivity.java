package com.example.avto;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private EditText fullNameEditText;
    private EditText passportSeriesEditText;
    private EditText passportNumberEditText;
    private EditText licenseNumberEditText;
    private Button saveProfileButton;
    private ImageView backArrow;
    private TextView profileEmail;
    private CheckBox dataProcessingCheckbox;
    private SharedPreferences sharedPreferences;
    private DatabaseHelper dbHelper;
    private String currentPhotoPath;

    // ИСПРАВЛЕНО: правильный ID из XML
    private ImageView profilePhotoImageView;

    private static final int PICK_IMAGE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Используем синглтон
        dbHelper = DatabaseHelper.getInstance(this);
        sharedPreferences = getSharedPreferences("user_profile", MODE_PRIVATE);

        // Инициализация View
        fullNameEditText = findViewById(R.id.fullNameEditText);
        passportSeriesEditText = findViewById(R.id.passportSeriesEditText);
        passportNumberEditText = findViewById(R.id.passportNumberEditText);
        licenseNumberEditText = findViewById(R.id.licenseNumberEditText);
        saveProfileButton = findViewById(R.id.saveProfileButton);
        backArrow = findViewById(R.id.backArrow);
        profileEmail = findViewById(R.id.profileEmail);
        dataProcessingCheckbox = findViewById(R.id.dataProcessingCheckbox);

        // ИСПРАВЛЕНО: правильная инициализация ImageView
        profilePhotoImageView = findViewById(R.id.profilePhotoImageView);

        // Обработчик клика на фото профиля
        if (profilePhotoImageView != null) {
            profilePhotoImageView.setOnClickListener(v -> showPhotoSelectionDialog());
        }

        // Кнопка назад
        backArrow.setOnClickListener(v -> finish());

        String userEmail = sharedPreferences.getString("user_email", "");
        if (profileEmail != null) {
            profileEmail.setText(userEmail);
        }

        loadProfileData(userEmail);

        // Обработчик кнопки сохранения
        saveProfileButton.setOnClickListener(v -> saveProfile(userEmail));
    }

    // Диалог выбора фото
    private void showPhotoSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Выберите фото профиля");

        String[] options = {"Выбрать из галереи", "Удалить фото", "Отмена"};
        builder.setItems(options, (dialog, which) -> {
            switch (which) {
                case 0: // Выбрать из галереи
                    pickFromGallery();
                    break;
                case 1: // Удалить фото
                    deletePhoto();
                    break;
                // case 2: Отмена - ничего не делаем
            }
        });

        builder.show();
    }

    private void loadProfileData(String userEmail) {
        Cursor cursor = dbHelper.getUserProfile(userEmail);
        if (cursor != null && cursor.moveToFirst()) {
            @SuppressLint("Range") String fullName = cursor.getString(cursor.getColumnIndex("full_name"));
            @SuppressLint("Range") String passportSeries = cursor.getString(cursor.getColumnIndex("passport_series"));
            @SuppressLint("Range") String passportNumber = cursor.getString(cursor.getColumnIndex("passport_number"));
            @SuppressLint("Range") String licenseNumber = cursor.getString(cursor.getColumnIndex("license_number"));

            if (fullName != null && fullNameEditText != null) fullNameEditText.setText(fullName);
            if (passportSeries != null && passportSeriesEditText != null) passportSeriesEditText.setText(passportSeries);
            if (passportNumber != null && passportNumberEditText != null) passportNumberEditText.setText(passportNumber);
            if (licenseNumber != null && licenseNumberEditText != null) licenseNumberEditText.setText(licenseNumber);

            // Загружаем фото профиля, если оно есть
            String profilePhotoPath = dbHelper.getUserProfilePhoto(userEmail);
            if (profilePhotoPath != null && !profilePhotoPath.isEmpty()) {
                currentPhotoPath = profilePhotoPath;
                // Загружаем фото в ImageView
                File imgFile = new File(profilePhotoPath);
                if (imgFile.exists()) {
                    profilePhotoImageView.setImageURI(Uri.fromFile(imgFile));
                }
            }

            cursor.close();
        }
    }

    private void pickFromGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    private void deletePhoto() {
        currentPhotoPath = null;
        // Устанавливаем стандартное фото
        profilePhotoImageView.setImageResource(R.drawable.default_profile_photo);
        Toast.makeText(this, "Фото удалено", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                Uri selectedImageUri = data.getData();
                if (selectedImageUri != null) {
                    try {
                        // Сохраняем фото во внутреннее хранилище
                        currentPhotoPath = saveImageToInternalStorage(selectedImageUri);
                        // Обновляем ImageView
                        profilePhotoImageView.setImageURI(selectedImageUri);
                        Toast.makeText(this, "Фото загружено", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "Ошибка загрузки фото", Toast.LENGTH_SHORT).show();
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    private String saveImageToInternalStorage(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                return null;
            }

            // Создаем папку для фото, если ее нет
            File directory = new File(getFilesDir(), "profile_photos");
            if (!directory.exists()) {
                directory.mkdirs();
            }

            // Создаем уникальное имя файла
            String fileName = "profile_" + System.currentTimeMillis() + ".jpg";
            File file = new File(directory, fileName);

            FileOutputStream outputStream = new FileOutputStream(file);
            byte[] buffer = new byte[1024];
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

    private void saveProfile(String userEmail) {
        String fullName = fullNameEditText.getText().toString().trim();
        String passportSeries = passportSeriesEditText.getText().toString().trim();
        String passportNumber = passportNumberEditText.getText().toString().trim();
        String licenseNumber = licenseNumberEditText.getText().toString().trim();

        // Проверка заполненности полей
        if (fullName.isEmpty() || passportSeries.isEmpty() || passportNumber.isEmpty() || licenseNumber.isEmpty()) {
            Toast.makeText(this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return;
        }

        // Проверка согласия на обработку данных
        if (!dataProcessingCheckbox.isChecked()) {
            Toast.makeText(this, "Необходимо согласие на обработку персональных данных", Toast.LENGTH_SHORT).show();
            return;
        }

        // Исправленный вызов с 6 параметрами
        boolean isUpdated = dbHelper.updateUserProfile(userEmail, fullName, passportSeries,
                passportNumber, licenseNumber, currentPhotoPath);

        if (isUpdated) {
            Toast.makeText(this, "Профиль сохранен", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Ошибка при сохранении профиля", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}