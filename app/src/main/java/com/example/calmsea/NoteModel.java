package com.example.calmsea;

public class NoteModel {
    private String id;          // Уникальный идентификатор заметки
    private String noteText;    // Текст заметки
    private String mood;        // Настроение
    private String date;        // Дата
    private String color;       // Цвет (если есть)
    private boolean isExpanded; // Новый флаг

    // Геттеры и сеттеры для isExpanded
    public NoteModel() { // Конструктор без аргументов (обязательно для Firebase)
    }

    // Конструктор с параметрами (для удобного создания заметок)
    public NoteModel(String id, String mood, String noteText, String date, String color) {
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

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

}




