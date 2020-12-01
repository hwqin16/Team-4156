package com.magikarp.android.data;

import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.magikarp.android.data.model.DeleteMessageRequest;
import com.magikarp.android.data.model.DeleteMessageResponse;
import com.magikarp.android.data.model.NewMessageRequest;
import com.magikarp.android.data.model.NewMessageResponse;
import com.magikarp.android.data.model.UpdateMessageResponse;
import com.magikarp.android.di.HiltQualifiers.UrlDeleteMessage;
import com.magikarp.android.di.HiltQualifiers.UrlNewMessage;
import com.magikarp.android.di.HiltQualifiers.UrlUpdateMessage;
import com.magikarp.android.network.GsonRequest;
import java.util.Objects;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Class for handling message posts.
 */
@Singleton
public class PostRepository {

  private static final String IMAGES_DIRECTORY = "/images";

  private final RequestQueue requestQueue;

  private final StorageReference storageReference;

  private final FileNameGenerator fileNameGenerator;

  private final String urlNewMessage;

  private final String urlUpdateMessage;

  private final String urlDeleteMessage;

  /**
   * Create a new post repository.
   *
   * @param requestQueue      queue for adding network requests
   * @param storageReference  Firebase storage reference
   * @param fileNameGenerator file name generator for uploading files
   * @param urlNewMessage     URL of endpoint for new messages
   * @param urlUpdateMessage  URL of endpoint to update messages
   * @param urlDeleteMessage  URL of endpoint to delete messages
   */
  @Inject
  public PostRepository(@NonNull RequestQueue requestQueue,
                        @NonNull StorageReference storageReference,
                        @NonNull FileNameGenerator fileNameGenerator,
                        @NonNull @UrlNewMessage String urlNewMessage,
                        @NonNull @UrlUpdateMessage String urlUpdateMessage,
                        @NonNull @UrlDeleteMessage String urlDeleteMessage) {
    this.requestQueue = requestQueue;
    this.storageReference = storageReference;
    this.fileNameGenerator = fileNameGenerator;
    this.urlNewMessage = urlNewMessage;
    this.urlUpdateMessage = urlUpdateMessage;
    this.urlDeleteMessage = urlDeleteMessage;
  }

  /**
   * Upload a new message post.
   *
   * @param userId        message user ID
   * @param latitude      message latitude
   * @param longitude     message longitude
   * @param imageUrl      message image URI
   * @param text          message text
   * @param listener      response listener
   * @param errorListener error listener
   */
  public void newMessage(@NonNull String userId, double latitude, double longitude,
                         @NonNull String imageUrl, @NonNull String text,
                         @NonNull Response.Listener<NewMessageResponse> listener,
                         @Nullable ErrorListener errorListener) {
    // Create message body.
    final NewMessageRequest body = new NewMessageRequest(imageUrl, text, latitude, longitude);
    // Create a new GSON request.
    final String url = String.format(urlNewMessage, userId);
    final GsonRequest<NewMessageResponse> request =
        new GsonRequest<>(Request.Method.POST, url, NewMessageResponse.class,
            new Gson().toJson(body), listener, errorListener);
    requestQueue.add(request);
  }

  /**
   * Update an existing message post.
   *
   * @param postId        message ID
   * @param userId        message user ID
   * @param latitude      message latitude
   * @param longitude     message longitude
   * @param imageUrl      message image URL
   * @param text          message text
   * @param listener      response listener
   * @param errorListener error listener
   */
  public void updateMessage(@NonNull String postId, @NonNull String userId, double latitude,
                            double longitude, @NonNull String imageUrl, @NonNull String text,
                            @NonNull Response.Listener<UpdateMessageResponse> listener,
                            @Nullable ErrorListener errorListener) {
    // Create message body.
    final NewMessageRequest body = new NewMessageRequest(imageUrl, text, latitude, longitude);
    // Create a new GSON request.
    final String url = String.format(urlUpdateMessage, userId, postId);
    final GsonRequest<UpdateMessageResponse> request =
        new GsonRequest<>(Request.Method.POST, url, UpdateMessageResponse.class,
            new Gson().toJson(body), listener, errorListener);
    requestQueue.add(request);
  }

  /**
   * Delete an existing message post.
   *
   * @param postId        message  ID
   * @param userId        message user ID
   * @param listener      response listener
   * @param errorListener error listener
   */
  public void deleteMessage(@NonNull String postId, @NonNull String userId,
                            @NonNull Response.Listener<DeleteMessageResponse> listener,
                            @Nullable ErrorListener errorListener) {
    // Create message body.
    final DeleteMessageRequest body = new DeleteMessageRequest(postId, userId);
    // Create a new GSON request.
    final String url = String.format(urlDeleteMessage, userId, postId);
    final GsonRequest<DeleteMessageResponse> request =
        new GsonRequest<>(Request.Method.POST, url, DeleteMessageResponse.class,
            new Gson().toJson(body), listener, errorListener);
    requestQueue.add(request);
  }

  /**
   * Upload a file to Firebase storage and get URI.
   *
   * @param fileUri       the URI of the image on the local device
   * @param fileExtension file extension of file
   * @param listener      listener for uploaded URI response
   */
  public void uploadFile(@NonNull Uri fileUri, @NonNull String fileExtension,
                         @Nullable UploadUriListener listener) {
    final StorageReference reference = storageReference
        .child(IMAGES_DIRECTORY + fileNameGenerator.getFileName(fileUri, fileExtension));
    final UploadTask uploadTask = reference.putFile(fileUri);
    uploadTask.continueWithTask(task -> {

      if (task.isSuccessful()) {
        return reference.getDownloadUrl();
      } else {
        throw Objects.requireNonNull(task.getException());
      }

    }).addOnCompleteListener(task -> {

      if (listener != null) {
        listener.onUriReceived(fileUri, task.isSuccessful() ? task.getResult() : null);
      }
    });

  }

  /**
   * Interface for a file name generator.
   */
  public interface FileNameGenerator {

    /**
     * Get a file name.
     *
     * @param seed          a seed for generating a file name
     * @param fileExtension file extension of file
     * @return a file name
     */
    String getFileName(Uri seed, String fileExtension);

  }

  /**
   * Interface for an upload URI listener.
   */
  public interface UploadUriListener {

    /**
     * Callback for a URI of an uploaded file.
     *
     * @param originalUri the original URI included in the upload request
     * @param uploadedUri the URI of the uploaded file, or {@code null} if an error occurred
     */
    void onUriReceived(Uri originalUri, Uri uploadedUri);

  }

}
