package com.example.calmsea;

import com.google.firebase.Timestamp;
import com.google.firebase.database.Exclude;

public class NoteModel {
    @Exclude
    private String id;          // Уникальный идентификатор заметки
    private String noteText;    // Текст заметки
    private String mood;        // Настроение
    private Timestamp date;        // Дата
    private String color;       // Цвет (если есть)
    @Exclude
    private boolean isExpanded; // Новый флаг
    private boolean dateChanged; // Флаг для отслеживания изменения даты

    // Геттеры и сеттеры для isExpanded
    public NoteModel() { // Конструктор без аргументов (обязательно для Firebase)
    }

    // Конструктор с параметрами (для удобного создания заметок)
    public NoteModel(String id, String mood, String noteText, Timestamp date, String color) {
        this.id = id;
        this.mood = mood;
        this.noteText = noteText;
        this.date = date;
        this.color = color;
    }

    // Геттеры и сеттеры для всех полей
    public boolean isExpanded() {return isExpanded;}

    public void setExpanded(boolean expanded) {isExpanded = expanded;}

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
    public String getMood() {
        return mood;
    }

    public void setMood(String mood) {
        this.mood = mood;
    }
    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public Timestamp getDate() {
        return date;
    }

    public void setDate(Timestamp date) {
        this.date = date;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
    public boolean isDateChanged() {
        return dateChanged;
    }

    public void setDateChanged(boolean dateChanged) {
        this.dateChanged = dateChanged;
    }

}




