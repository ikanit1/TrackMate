package com.example.trackmate;

import android.net.Uri;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import java.util.ArrayList;

public class Global {
    public static String userToAdd = null;
    public static Users me = new Users();
    public static UserLocation myLoc = null;
    public static ArrayList<UserLocation> allLocations = new ArrayList<>();
    public static ArrayList<Users> allUsers = new ArrayList<>();
    public static ArrayList<UserLocation> myFriendsLocation = new ArrayList<>();

    public static String getUserIconUrl(String nickname) {
        // Предположим, что путь до иконки пользователя в Firebase Storage совпадает с его ником
        String path = "profile_pictures/" + nickname + ".jpg";
        StorageReference storageRef = FirebaseStorage.getInstance().getReference().child(path);

        try {
            // Получаем синхронно URL из Firebase Storage
            Uri uri = storageRef.getDownloadUrl().getResult();
            if (uri != null) {
                return uri.toString();
            } else {
                return null; // в случае ошибки получения URL
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }


    // Интерфейс для обратного вызова
    public interface OnGetUserIconUrlListener {
        void onSuccess(String url);
        void onFailure(String error);
    }
}
