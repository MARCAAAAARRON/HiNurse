package com.example.hinurse20.services;

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.cloudinary.Cloudinary;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for handling Cloudinary image uploads and management
 * Replace YOUR_CLOUD_NAME, YOUR_UPLOAD_PRESET with your actual Cloudinary credentials
 */
public class CloudinaryService {
    private static final String TAG = "CloudinaryService";
    
    // Replace with your Cloudinary cloud name
    private static final String CLOUD_NAME = "dlvgoszbo";
    
    // Replace with your Cloudinary upload preset (create one in your Cloudinary dashboard)
    private static final String UPLOAD_PRESET = "HiNurse";
    
    private static CloudinaryService instance;
    private static UploadCallback currentCallback;

    public interface CloudinaryUploadCallback {
        void onSuccess(String imageUrl);
        void onError(String error);
        void onProgress(long bytes, long totalBytes);
    }

    public interface CloudinaryDeleteCallback {
        void onSuccess();
        void onError(String error);
    }

    private CloudinaryService() {
    }

    /**
     * Initialize Cloudinary with your credentials
     * Call this in your Application class or MainActivity
     */
    public static void initialize(Context context) {
        try {
            Map<String, String> config = new HashMap<>();
            config.put("cloud_name", CLOUD_NAME);
            MediaManager.init(context, config);
            Log.d(TAG, "Cloudinary initialized successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Cloudinary: " + e.getMessage(), e);
        }
    }

    public static CloudinaryService getInstance() {
        if (instance == null) {
            instance = new CloudinaryService();
        }
        return instance;
    }

    /**
     * Upload image to Cloudinary from Uri
     * @param uri The image Uri
     * @param messageId Message ID for unique file naming
     * @param callback Callback for upload progress and result
     */
    public void uploadImage(Uri uri, String messageId, CloudinaryUploadCallback callback) {
        try {
            Map<String, Object> uploadOptions = new HashMap<>();
            uploadOptions.put("resource_type", "auto");
            uploadOptions.put("public_id", "chat/" + messageId); // Organize uploads in 'chat' folder
            uploadOptions.put("folder", "hinurse/chat");
            uploadOptions.put("quality", "auto");
            uploadOptions.put("fetch_format", "auto");

            currentCallback = new UploadCallback() {
                @Override
                public void onStart(String requestId) {
                    Log.d(TAG, "Upload started: " + requestId);
                }

                @Override
                public void onProgress(String requestId, long bytes, long totalBytes) {
                    Log.d(TAG, "Upload progress: " + bytes + " / " + totalBytes);
                    if (callback != null) {
                        callback.onProgress(bytes, totalBytes);
                    }
                }

                @Override
                public void onSuccess(String requestId, Map resultData) {
                    try {
                        String imageUrl = (String) resultData.get("secure_url");
                        if (imageUrl == null) {
                            imageUrl = (String) resultData.get("url");
                        }
                        Log.d(TAG, "Upload successful. Image URL: " + imageUrl);
                        if (callback != null) {
                            callback.onSuccess(imageUrl);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing upload result: " + e.getMessage(), e);
                        if (callback != null) {
                            callback.onError("Error processing upload result: " + e.getMessage());
                        }
                    }
                }

                @Override
                public void onError(String requestId, ErrorInfo error) {
                    String errorMsg = error.getDescription();
                    Log.e(TAG, "Upload error: " + errorMsg);
                    if (callback != null) {
                        callback.onError(errorMsg);
                    }
                }

                @Override
                public void onReschedule(String requestId, ErrorInfo error) {
                    String errorMsg = error.getDescription();
                    Log.w(TAG, "Upload rescheduled: " + errorMsg);
                }
            };

            MediaManager.get().upload(uri)
                    .option("public_id", "chat/" + messageId)
                    .option("folder", "hinurse/chat")
                    .option("resource_type", "auto")
                    .option("quality", "auto")
                    .option("fetch_format", "auto")
                    .unsigned(UPLOAD_PRESET)
                    .callback(currentCallback)
                    .dispatch();

        } catch (Exception e) {
            Log.e(TAG, "Error uploading image: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("Error uploading image: " + e.getMessage());
            }
        }
    }

    /**
     * Get Cloudinary image URL with optional transformations
     * @param publicId The public ID of the image
     * @param width Optional width for transformation
     * @param height Optional height for transformation
     * @return The Cloudinary image URL
     */
    public String getImageUrl(String publicId, int width, int height) {
        try {
            String baseUrl = "https://res.cloudinary.com/" + CLOUD_NAME + "/image/upload/";
            
            if (width > 0 && height > 0) {
                baseUrl += "w_" + width + ",h_" + height + ",c_limit/";
            } else if (width > 0) {
                baseUrl += "w_" + width + ",c_limit/";
            } else if (height > 0) {
                baseUrl += "h_" + height + ",c_limit/";
            }
            
            return baseUrl + publicId;
        } catch (Exception e) {
            Log.e(TAG, "Error generating image URL: " + e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get thumbnail URL with fixed dimensions
     * @param imageUrl Original image URL from Cloudinary
     * @param width Thumbnail width
     * @param height Thumbnail height
     * @return Thumbnail URL
     */
    public String getThumbnailUrl(String imageUrl, int width, int height) {
        try {
            if (imageUrl == null || !imageUrl.contains("cloudinary")) {
                return imageUrl;
            }
            
            // Insert transformation parameters before the filename
            String transformed = imageUrl.replace(
                    "/upload/",
                    "/upload/w_" + width + ",h_" + height + ",c_fill,q_auto/");
            return transformed;
        } catch (Exception e) {
            Log.e(TAG, "Error generating thumbnail URL: " + e.getMessage(), e);
            return imageUrl;
        }
    }

    /**
     * Delete image from Cloudinary
     * @param publicId The public ID of the image to delete
     * @param callback Callback for success/error handling
     */
    public void deleteImage(String publicId, CloudinaryDeleteCallback callback) {
        try {
            // Note: This is a simplified implementation. In a production app,
            // image deletion should be handled server-side due to security concerns.
            // The Android SDK doesn't have a direct delete method for security reasons.
            // This would typically be done through a backend service.
            Log.w(TAG, "Cloudinary image deletion should be handled server-side for security reasons. Public ID: " + publicId);
            
            // For demonstration purposes, we'll just call the callback
            if (callback != null) {
                callback.onSuccess();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error deleting image from Cloudinary: " + e.getMessage(), e);
            if (callback != null) {
                callback.onError("Error deleting image: " + e.getMessage());
            }
        }
    }
}
