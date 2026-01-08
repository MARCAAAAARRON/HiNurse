package com.example.hinurse20;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to manage default profile icons
 */
public class ProfileIconHelper {
    private static final List<Integer> PROFILE_ICONS = new ArrayList<Integer>() {{
        add(R.drawable.ic_profile_1); // Purple
        add(R.drawable.ic_profile_2); // Blue
        add(R.drawable.ic_profile_3); // Green
        add(R.drawable.ic_profile_4); // Orange
        add(R.drawable.ic_profile_5); // Pink
        add(R.drawable.ic_profile_6); // Deep Purple
    }};

    /**
     * Get a profile icon based on user ID hash for consistent assignment
     * @param userId The user ID
     * @return Resource ID of the profile icon
     */
    public static int getProfileIconForUser(String userId) {
        if (userId == null || userId.isEmpty()) {
            return PROFILE_ICONS.get(0);
        }
        // Use hash code to consistently assign same icon to same user
        int index = Math.abs(userId.hashCode()) % PROFILE_ICONS.size();
        return PROFILE_ICONS.get(index);
    }

    /**
     * Get a random profile icon
     * @return Resource ID of a random profile icon
     */
    public static int getRandomProfileIcon() {
        int index = (int) (Math.random() * PROFILE_ICONS.size());
        return PROFILE_ICONS.get(index);
    }

    /**
     * Get all available profile icon resource IDs
     * @return List of profile icon resource IDs
     */
    public static List<Integer> getAllProfileIcons() {
        return new ArrayList<>(PROFILE_ICONS);
    }

    /**
     * Get profile icon by index (0-5)
     * @param index Index of the icon (0-5)
     * @return Resource ID of the profile icon, or first icon if index is invalid
     */
    public static int getProfileIconByIndex(int index) {
        if (index < 0 || index >= PROFILE_ICONS.size()) {
            return PROFILE_ICONS.get(0);
        }
        return PROFILE_ICONS.get(index);
    }
}














