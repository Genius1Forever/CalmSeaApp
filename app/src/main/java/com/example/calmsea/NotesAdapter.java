package com.example.calmsea;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;


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
        holder.dateTextView.setText(note.getDate());
        //holder.itemView.setBackgroundColor(Color.parseColor(note.getColor())); // Устанавливаем цвет фона
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

        // Обработка клика
        holder.itemView.setOnClickListener(v -> {
            if (onNoteClickListener != null) {
                onNoteClickListener.onNoteClick(note);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (onNoteClickListener != null) {
                onNoteClickListener.onNoteClick(note);
            }
        });
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


