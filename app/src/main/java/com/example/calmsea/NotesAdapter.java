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
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;


public class NotesAdapter extends RecyclerView.Adapter<NotesAdapter.NoteViewHolder> {

    private List<NoteModel> notes;
    private OnNoteClickListener onNoteClickListener;

    public interface OnNoteClickListener {
        void onNoteClick(NoteModel note);
    }

    public NotesAdapter(List<NoteModel> notes, OnNoteClickListener onNoteClickListener) {
        this.notes = notes;
        this.onNoteClickListener = onNoteClickListener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        NoteModel note = notes.get(position);

        holder.moodTextView.setText(note.getMood());
        holder.textTextView.setText(note.getNoteText());

        // Устанавливаем дату
        holder.dateTextView.setText(formatDate(note.getDate(), note.isDateChanged()));

        // Устанавливаем цвет настроения
        applyMoodColor(holder, note.getColor());

        // Обработка клика
        holder.itemView.setOnClickListener(v -> {
            if (onNoteClickListener != null) {
                onNoteClickListener.onNoteClick(note);
            }
        });
    }

    /**
     * Форматирует дату в зависимости от флага dateChanged.
     */
    private String formatDate(Timestamp timestamp, boolean dateChanged) {
        if (timestamp == null) {
            return "Нет даты";
        }

        Date date = timestamp.toDate(); // Преобразуем в Date
        SimpleDateFormat dateFormat = new SimpleDateFormat(
                dateChanged ? "EEEE, dd MMMM yyyy" : "EEEE, dd MMMM yyyy, HH:mm",
                Locale.getDefault()
        );
        return dateFormat.format(date);
    }

    /**
     * Применяет цвет настроения к фону элемента списка.
     */
    private void applyMoodColor(NoteViewHolder holder, String color) {
        GradientDrawable background = (GradientDrawable) ContextCompat.getDrawable(holder.itemView.getContext(), R.drawable.rounded_border);
        if (background != null) {
            try {
                background.setColor(Color.parseColor(color)); // Устанавливаем цвет настроения
            } catch (IllegalArgumentException e) {
                background.setColor(ContextCompat.getColor(holder.itemView.getContext(), R.color.default_mood)); // Цвет по умолчанию
            }
            holder.itemView.setBackground(background);
        }
    }

    @Override
    public int getItemCount() {
        return notes.size();
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


