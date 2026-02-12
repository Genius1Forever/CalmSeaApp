package com.example.calmsea;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotesPagerAdapter extends RecyclerView.Adapter<NotesPagerAdapter.NoteViewHolder> {

    private List<NoteModel> notes;
    private final OnNoteClickListener onNoteClickListener2;

    // Интерфейс для обработки кликов
    public interface OnNoteClickListener {
        void onNoteClick(NoteModel note);
    }

    // Конструктор с обработчиком кликов
    public NotesPagerAdapter(List<NoteModel> notes, OnNoteClickListener listener) {
        this.notes = notes;
        this.onNoteClickListener2 = listener;
    }
    public void updateNotes(List<NoteModel> newNotes) {
        this.notes.clear();
        this.notes.addAll(newNotes);
        notifyDataSetChanged(); // <--- это важно
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note_house, parent, false);
        // Убедитесь, что View заполняет всю область
        ViewGroup.LayoutParams layoutParams = view.getLayoutParams();
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        view.setLayoutParams(layoutParams);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        // Настройка высоты
        ViewGroup.LayoutParams params = holder.itemView.getLayoutParams();
        params.height = ViewGroup.LayoutParams.MATCH_PARENT;
        holder.itemView.setLayoutParams(params);

        // Привязка данных
        NoteModel note = notes.get(position);
        holder.moodTextView.setText(note.getMood());
        //holder.textTextView.setText(note.getNoteText());

        // Получаем дату из Firestore
        Timestamp timestamp = note.getDate(); // Предполагаем, что в модели NoteModel уже используется Timestamp
        if (timestamp != null) {
            Date date = timestamp.toDate(); // Преобразуем в Date

            // Логируем значение dateChanged
            Log.d("NotesAdapter", "Note ID: " + note.getId() + ", Date Changed: " + note.isDateChanged());

            // Формат даты в зависимости от значения dateChanged
            SimpleDateFormat dateFormat = new SimpleDateFormat(
                    note.isDateChanged() ? "EEEE, dd MMMM yyyy" : "EEEE, dd MMMM yyyy, HH:mm",
                    Locale.getDefault()
            );
            String formattedDate = dateFormat.format(date);

            // Логируем отформатированную дату
            Log.d("NotesAdapter", "Formatted Date: " + formattedDate);

            holder.dateTextView.setText(formattedDate);
        } else {
            holder.dateTextView.setText("Нет даты");
        }


    //holder.itemView.setBackgroundColor(Color.parseColor(note.getColor()));

        // Проверяем, нужно ли показывать полный текст
        if (note.isExpanded()) {
            holder.textTextView.setText(note.getNoteText()); // Полный текст
        } else {
            int maxTextLength = 50; // Лимит символов
            String noteText = note.getNoteText();
            if (noteText.length() > maxTextLength) {
                noteText = noteText.substring(0, maxTextLength) + "..."; // Обрезаем текст
            }
            holder.textTextView.setText(noteText);
        }
        // Устанавливаем обработчик клика
        holder.itemView.setOnClickListener(v -> {
            if (onNoteClickListener2 != null) {
                onNoteClickListener2.onNoteClick(note);
            }
        });
        // Установка цвета настроения в Drawable
        GradientDrawable background = (GradientDrawable) ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.rounded_border);
        if (background != null) {
            try {
                background.setColor(Color.parseColor(note.getColor())); // Устанавливаем цвет настроения
            } catch (IllegalArgumentException e) {
                background.setColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.default_mood)); // Цвет по умолчанию
            }
            holder.itemView.setBackground(background);
        }
        // Обработка долгого нажатия
        holder.itemView.setOnLongClickListener(v -> {
            // Меняем состояние заметки
            note.setExpanded(!note.isExpanded());
            notifyItemChanged(position); // Обновляем элемент
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return notes != null ? notes.size() : 0;
    }

    public void setNotes(List<NoteModel> notes) {
        this.notes = notes;
        notifyDataSetChanged();
    }

    public static class NoteViewHolder extends RecyclerView.ViewHolder {
        TextView moodTextView, textTextView, dateTextView;

        public NoteViewHolder(View itemView) {
            super(itemView);
            moodTextView = itemView.findViewById(R.id.mood_text_view);
            textTextView = itemView.findViewById(R.id.note_text_view);
            dateTextView = itemView.findViewById(R.id.date_text_view);
        }
    }

}


