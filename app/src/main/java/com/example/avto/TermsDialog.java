package com.example.avto;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.google.android.material.button.MaterialButton;

public class TermsDialog extends Dialog {
    private OnTermsAcceptedListener listener;

    public interface OnTermsAcceptedListener {
        void onTermsAccepted();
    }

    public TermsDialog(@NonNull Context context, OnTermsAcceptedListener listener) {
        super(context, R.style.RoundedDialogTheme);
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_terms);

        // Убираем стандартный заголовок и делаем прозрачный фон окна
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
        }

        TextView tvTerms = findViewById(R.id.tvTerms);
        MaterialButton btnAccept = findViewById(R.id.btnAccept);
        MaterialButton btnCancel = findViewById(R.id.btnCancel);

        // Установка текста соглашения
        String termsText = "1. Вы обязуетесь использовать приложение в законных целях.\n\n" +
                "2. Мы обрабатываем ваши данные в соответствии с политикой конфиденциальности.\n\n" +
                "3. Вы несете ответственность за сохранность своих учетных данных.\n\n" +
                "4. Администрация оставляет за собой право изменять правила.\n\n" +
                "5. Регистрируясь, вы подтверждаете, что ознакомились с правилами.";

        tvTerms.setText(termsText);

        btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null) {
                    listener.onTermsAccepted();
                }
                dismiss();
            }
        });

        btnCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dismiss();
            }
        });
    }
}