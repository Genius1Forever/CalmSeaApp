package com.example.calmsea;

import androidx.recyclerview.widget.DiffUtil;

import java.util.List;

public class NotesDiffCallback extends DiffUtil.Callback {
    private final List<NoteModel> oldList;
    private final List<NoteModel> newList;

    public NotesDiffCallback(List<NoteModel> oldList, List<NoteModel> newList) {
        this.oldList = oldList;
        this.newList = newList;
    }

    @Override
    public int getOldListSize() {
        return oldList.size();
    }

    @Override
    public int getNewListSize() {
        return newList.size();
    }

    @Override
    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).getId().equals(newList.get(newItemPosition).getId());
    }

    @Override
    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        return oldList.get(oldItemPosition).equals(newList.get(newItemPosition));
    }
}

