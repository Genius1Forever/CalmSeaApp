package com.example.calmsea;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class ProfileFragment extends Fragment {

    private FirebaseAuth auth; private FirebaseFirestore db;
    private TextView profileNameTextView, profileEmailTextView, profileBirthDateTextView,
            profileGenderTextView, profilePhoneTextView, profileEntriesCountTextView, profileAverageMoodTextView;
    private ImageView profileAvatarImageView; private Button logoutButton;
    private String userId; private SharedViewModel sharedViewModel;
    private TextView profileCountTextView; private TextView averageMoodTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Inflate the fragment layout
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        // Инициализация ViewModel
        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        // Инициализация Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        // Получение текущего пользователя
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        // Привязка элементов интерфейса
        profileNameTextView = view.findViewById(R.id.profileNameTextView);
        profileEmailTextView = view.findViewById(R.id.profileEmailTextView);
        profileBirthDateTextView = view.findViewById(R.id.profileBirthdateTextView);
        profileGenderTextView = view.findViewById(R.id.profileGenderTextView);
        profilePhoneTextView = view.findViewById(R.id.profilePhoneTextView);
        profileEntriesCountTextView = view.findViewById(R.id.profileCountTextView);
        profileAverageMoodTextView = view.findViewById(R.id.profileMoodTextView);
        profileAvatarImageView = view.findViewById(R.id.profileAvatarImageView);
        logoutButton = view.findViewById(R.id.logoutButton);

        // Подписка на обновления ViewModel
        sharedViewModel.getDataUpdated().observe(getViewLifecycleOwner(), updated -> {
            if (Boolean.TRUE.equals(updated)) {
                fetchDataFromFirestore();
                sharedViewModel.setDataUpdated(false); // Сброс состояния
            }
        });

        // Загрузка данных при старте
        fetchDataFromFirestore();

        loadUserData();
        listenForProfileChanges(); // Добавленный Snapshot Listener

        profileCountTextView = view.findViewById(R.id.profileCountTextView);
        // Загрузка количества заметок
        loadNoteCount();
        // Среднее настроение
        averageMoodTextView = view.findViewById(R.id.profileMoodTextView);
        loadAverageMood();

        // Настройка кнопки выхода
        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(requireActivity(), AuthActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        ImageButton button = view.findViewById(R.id.btn_edit);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditActivity.class);
            startActivity(intent);
        });

        return view;
    }

    /**
     * Добавляем Firestore Snapshot Listener для автоматического обновления профиля
     */
    private void listenForProfileChanges() {
        if (userId == null) return;

        DocumentReference userRef = db.collection("users").document(userId);
        userRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                Log.e("Firestore", "Ошибка при получении данных", error);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null ) { // Проверка на активность фрагмента
                    profileNameTextView.setText(user.getName());
                    profileEmailTextView.setText(user.getEmail());
                    profileBirthDateTextView.setText(user.getBirthDate());
                    profileGenderTextView.setText(user.getGender());
                    profilePhoneTextView.setText(user.getPhoneNumber());

                    // Загружаем аватарку, если есть
                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                        Glide.with(this)
                                .load(user.getProfileImageUrl())
                                .into(profileAvatarImageView);
                    }
                }
            }
            if (documentSnapshot != null && documentSnapshot.exists()) {
                String avatarUrl = documentSnapshot.getString("avatar");
                Log.d("PROFILE_USER", "Обновленный аватар: " + avatarUrl);
                if (avatarUrl != null) {
                    Glide.with(requireContext())
                            .load(avatarUrl)
                            .placeholder(R.drawable.default_avatar)
                            .error(R.drawable.default_avatar)
                            .into(profileAvatarImageView);

                }
            }
        });
    }

    //получение текущего юзера
    private void loadUserData() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        updateUI(document);
                    }
                })
                .addOnFailureListener(e -> {
                    profileNameTextView.setText("Ошибка загрузки данных");
                });

        // Подписка на изменения в SharedViewModel
        sharedViewModel.getDataUpdated().observe(getViewLifecycleOwner(), updated -> {
            if (Boolean.TRUE.equals(updated)) { // Безопасная проверка на истину
                db.collection("users").document(userId)
                        .get()
                        .addOnSuccessListener(this::updateUI)
                        .addOnFailureListener(e -> {
                            profileNameTextView.setText("Ошибка обновления данных");
                        });
                sharedViewModel.setDataUpdated(false); // Сбросить статус обновления
            }
        });

    }
    //выведение данных на экран
    private void updateUI(DocumentSnapshot document) {
        String userName = document.getString("name");
        String userEmail = document.getString("email");
        String userAvatar = document.getString("avatar");
        String userBirthDate = document.getString("birthDate");
        String userGender = document.getString("gender");
        String userPhone = document.getString("phone");
        long entriesCount = document.getLong("entriesCount") != null ? document.getLong("entriesCount") : 0;
        String averageMood = document.getString("averageMood");

        profileNameTextView.setText(userName != null ? userName : "Не указано");
        profileEmailTextView.setText(userEmail != null ? userEmail : "Не указан");
        profileBirthDateTextView.setText(userBirthDate != null ? userBirthDate : "Не указана");
        profileGenderTextView.setText(userGender != null ? userGender : "Не указан");
        profilePhoneTextView.setText(userPhone != null ? userPhone : "Не указан");
        profileEntriesCountTextView.setText(String.valueOf(entriesCount));
        profileAverageMoodTextView.setText(averageMood != null ? averageMood : "Не указано");

        if (userAvatar != null && !userAvatar.isEmpty()) {
            Glide.with(this)
                    .load(userAvatar)
                    .placeholder(R.drawable.default_avatar)
                    .error(R.drawable.default_avatar)
                    .into(profileAvatarImageView);
        } else {
            profileAvatarImageView.setImageResource(R.drawable.default_avatar);
        }
    }
    //выведение данных из бд
    private void fetchDataFromFirestore() {
        if (userId != null) {
            db.collection("users").document(userId)
                    .get()
                    .addOnSuccessListener(document -> {
                        if (document.exists()) {
                            profileNameTextView.setText(document.getString("name"));
                            profileEmailTextView.setText(document.getString("email"));
                            profileBirthDateTextView.setText(document.getString("birthDate"));
                            profileGenderTextView.setText(document.getString("gender"));
                            profilePhoneTextView.setText(document.getString("phone"));

                            // Загрузка аватара
                            String avatarUrl = document.getString("avatar");
                            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                                Glide.with(requireContext())
                                        .load(avatarUrl)
                                        .placeholder(R.drawable.default_avatar)
                                        .error(R.drawable.default_avatar)
                                        .into(profileAvatarImageView);
                            } else {
                                profileAvatarImageView.setImageResource(R.drawable.default_avatar);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        profileNameTextView.setText("Ошибка загрузки данных");
                    });
        }
    }
    //вывод количества заметок
    private void loadNoteCount() {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            Long entriesCount = document.getLong("entriesCount");
                            if (entriesCount != null) {
                                profileCountTextView.setText(String.valueOf(entriesCount));
                            } else {
                                // Если noteCount не существует, инициализируем его значением 0
                                db.collection("users").document(userId)
                                        .update("noteCount", 0)
                                        .addOnSuccessListener(aVoid -> {
                                            profileCountTextView.setText("0");
                                        })
                                        .addOnFailureListener(e -> {
                                            Log.w("Firestore", "Error initializing noteCount", e);
                                        });
                            }
                        }
                    } else {
                        Log.w("Firestore", "Error getting user document", task.getException());
                    }
                });
    }
    //рассчитывается среднее настроение
    private void loadAverageMood() {
        db.collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String averageMood = documentSnapshot.getString("averageMood");
                        if (averageMood != null && !averageMood.isEmpty()) {
                            averageMoodTextView.setText("" + averageMood);
                        } else {
                            averageMoodTextView.setText("Не рассчитано");
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка загрузки среднего настроения", Toast.LENGTH_SHORT).show();
                });
    }
}


