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
import com.google.firebase.firestore.ListenerRegistration;

public class ProfileFragment extends Fragment {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextView profileNameTextView, profileEmailTextView, profileBirthDateTextView,
            profileGenderTextView, profilePhoneTextView, profileEntriesCountTextView, profileAverageMoodTextView;
    private ImageView profileAvatarImageView;
    private Button logoutButton;
    private String userId; private ListenerRegistration profileListener;
    private SharedViewModel sharedViewModel;
    private TextView profileCountTextView, averageMoodTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        sharedViewModel = new ViewModelProvider(requireActivity()).get(SharedViewModel.class);
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        profileNameTextView = view.findViewById(R.id.profileNameTextView);
        profileEmailTextView = view.findViewById(R.id.profileEmailTextView);
        profileBirthDateTextView = view.findViewById(R.id.profileBirthdateTextView);
        profileGenderTextView = view.findViewById(R.id.profileGenderTextView);
        profilePhoneTextView = view.findViewById(R.id.profilePhoneTextView);
        profileEntriesCountTextView = view.findViewById(R.id.profileCountTextView);
        profileAverageMoodTextView = view.findViewById(R.id.profileMoodTextView);
        profileAvatarImageView = view.findViewById(R.id.profileAvatarImageView);
        logoutButton = view.findViewById(R.id.logoutButton);
        profileCountTextView = view.findViewById(R.id.profileCountTextView);
        averageMoodTextView = view.findViewById(R.id.profileMoodTextView);

        // Подписка на обновления ViewModel
        sharedViewModel.getDataUpdated().observe(getViewLifecycleOwner(), updated -> {
            if (Boolean.TRUE.equals(updated)) {
                fetchDataFromFirestore();
                sharedViewModel.setDataUpdated(false);
            }
        });

        // Первоначальная загрузка данных
        fetchDataFromFirestore();
        listenForProfileChanges();
        loadNoteCount();
        loadAverageMood();

        // Кнопка выхода
        logoutButton.setOnClickListener(v -> {
            auth.signOut();
            Intent intent = new Intent(requireActivity(), AuthActivity.class);
            startActivity(intent);
            requireActivity().finish();
        });

        // Кнопка редактирования профиля
        ImageButton button = view.findViewById(R.id.btn_edit);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), EditActivity.class);
            startActivity(intent);
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        fetchDataFromFirestore(); // Повторная загрузка данных при возвращении на экран
        loadNoteCount();
    }

    private void listenForProfileChanges() {
        if (userId == null) return;

        DocumentReference userRef = db.collection("users").document(userId);
        profileListener = userRef.addSnapshotListener((documentSnapshot, error) -> {
            if (error != null) {
                Log.e("Firestore", "Ошибка при получении данных", error);
                return;
            }

            if (documentSnapshot != null && documentSnapshot.exists()) {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    profileNameTextView.setText(user.getName());
                    profileEmailTextView.setText(user.getEmail());
                    profileBirthDateTextView.setText(user.getBirthDate());
                    profileGenderTextView.setText(user.getGender());

                    if (user.getPhoneNumber() != null && !user.getPhoneNumber().isEmpty()) {
                        profilePhoneTextView.setText(user.getPhoneNumber());
                        Log.d("ProfileFragment", "Обновленный номер: " + user.getPhoneNumber());
                    }

                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()) {
                        Glide.with(this)
                                .load(user.getProfileImageUrl())
                                .circleCrop()
                                .into(profileAvatarImageView);
                    }
                }
            }
        });
    }
    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (profileListener != null) {
            profileListener.remove();
            profileListener = null;
        }
    }

    private void fetchDataFromFirestore() {
        if (userId == null) return;

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
    }

    private void updateUI(DocumentSnapshot document) {
        profileNameTextView.setText(document.getString("name") != null ? document.getString("name") : "Не указано");
        profileEmailTextView.setText(document.getString("email") != null ? document.getString("email") : "Не указан");
        profileBirthDateTextView.setText(document.getString("birthDate") != null ? document.getString("birthDate") : "Не указана");
        profileGenderTextView.setText(document.getString("gender") != null ? document.getString("gender") : "Не указан");
        profilePhoneTextView.setText(document.getString("phone") != null ? document.getString("phone") : profilePhoneTextView.getText());

        long entriesCount = document.getLong("entriesCount") != null ? document.getLong("entriesCount") : 0;
        profileEntriesCountTextView.setText(String.valueOf(entriesCount));

        String averageMood = document.getString("averageMood");
        profileAverageMoodTextView.setText(averageMood != null ? averageMood : "Не указано");

        String avatarUrl = document.getString("avatar");
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(R.drawable.user)
                    .error(R.drawable.user)
                    .into(profileAvatarImageView);
        } else {
            profileAvatarImageView.setImageResource(R.drawable.user);
        }
    }

    private void loadNoteCount() {
        db.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Long entriesCount = task.getResult().getLong("entriesCount");
                        profileCountTextView.setText(entriesCount != null ? String.valueOf(entriesCount) : "0");
                    }
                });
    }

    private void loadAverageMood() {
        db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String averageMood = document.getString("averageMood");
                        averageMoodTextView.setText(averageMood != null ? averageMood : "Не рассчитано");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Ошибка загрузки среднего настроения", Toast.LENGTH_SHORT).show();
                });
    }
}


