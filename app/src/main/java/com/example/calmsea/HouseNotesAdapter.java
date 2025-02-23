package com.example.calmsea;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
public class HouseNotesAdapter extends RecyclerView.Adapter<HouseNotesAdapter.NoteViewHolder> {

    private List<NoteModel> notes;

    public HouseNotesAdapter(List<NoteModel> notes) {
        this.notes = notes;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_note_house, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HouseNotesAdapter.NoteViewHolder holder, int position) {
        NoteModel note = notes.get(position);
        holder.moodTextView.setText(note.getMood());
        holder.textTextView.setText(note.getNoteText());
        holder.dateTextView.setText(note.getDate());
        holder.itemView.setBackgroundColor(Color.parseColor(note.getColor())); // Устанавливаем цвет фона

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
