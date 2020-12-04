package com.magikarp.android.ui.posts;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.GetContent;
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.maps.model.LatLng;
import com.magikarp.android.R;
import com.magikarp.android.data.PostRepository;
import com.magikarp.android.data.model.DeleteMessageResponse;
import com.magikarp.android.data.model.Message;
import com.magikarp.android.data.model.NewMessageResponse;
import com.magikarp.android.data.model.UpdateMessageResponse;
import com.magikarp.android.databinding.FragmentPostBinding;
import com.magikarp.android.location.LocationListener;
import dagger.hilt.android.AndroidEntryPoint;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Locale;
import javax.inject.Inject;
import org.jetbrains.annotations.NotNull;

/**
 * A fragment for viewing and editing posts.
 */
@AndroidEntryPoint
public class PostFragment extends Fragment {

  @VisibleForTesting
  static final String GEO_URL = "google.navigation:q=%f,%f";
  @VisibleForTesting
  static final String MEDIA_TYPE_IMAGE = "image/*";
  @VisibleForTesting
  static final String SAVESTATE_IMAGE_URL = "imageUrl";
  @VisibleForTesting
  static final String SAVESTATE_TEXT = "text";
  @VisibleForTesting
  static final String SAVESTATE_LOCATION = "location";
  @VisibleForTesting
  static final String URI_SCHEME_HTTP = "http";
  @VisibleForTesting
  FragmentActivity activity;
  @VisibleForTesting
  ActivityResultLauncher<String> requestPermissionLauncher;
  @VisibleForTesting
  ActivityResultLauncher<String> getContentLauncher;
  @VisibleForTesting
  Bundle arguments;
  @VisibleForTesting
  Context context;
  @VisibleForTesting
  FragmentPostBinding binding;
  @VisibleForTesting
  LatLng location;
  @VisibleForTesting
  LocationListener locationListener;
  @VisibleForTesting
  String imageUrl;
  @Inject
  ContentResolver contentResolver;
  @Inject
  FusedLocationProviderClient fusedLocationClient;
  @Inject
  ImageLoader imageLoader;
  @Inject
  PostRepository postRepository;
  @Inject
  RequestQueue requestQueue;

  /**
   * Default constructor.
   */
  public PostFragment() {
  }

  /**
   * PostFragment constructor for testing.
   *
   * @param activity                  test variable
   * @param getContentLauncher        test variable
   * @param requestPermissionLauncher test variable
   * @param arguments                 test variable
   * @param context                   test variable
   * @param binding                   test variable
   * @param location                  test variable
   * @param locationListener          test variable
   * @param imageUrl                  test variable
   * @param contentResolver           test variable
   * @param fusedLocationClient       test variable
   * @param imageLoader               test variable
   * @param postRepository            test variable
   * @param requestQueue              test variable
   */
  @VisibleForTesting
  PostFragment(
      FragmentActivity activity, ActivityResultLauncher<String> getContentLauncher,
      ActivityResultLauncher<String> requestPermissionLauncher, Bundle arguments, Context context,
      FragmentPostBinding binding,
      LatLng location, LocationListener locationListener, String imageUrl,
      ContentResolver contentResolver,
      FusedLocationProviderClient fusedLocationClient, ImageLoader imageLoader,
      PostRepository postRepository, RequestQueue requestQueue) {
    this.activity = activity;
    this.getContentLauncher = getContentLauncher;
    this.requestPermissionLauncher = requestPermissionLauncher;
    this.arguments = arguments;
    this.context = context;
    this.binding = binding;
    this.location = location;
    this.locationListener = locationListener;
    this.imageUrl = imageUrl;
    this.contentResolver = contentResolver;
    this.fusedLocationClient = fusedLocationClient;
    this.imageLoader = imageLoader;
    this.postRepository = postRepository;
    this.requestQueue = requestQueue;
  }

  @Override
  public void onCreate(final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    performOnCreate();
  }

  @VisibleForTesting
  void performOnCreate() {
    // For unit testing.
    activity = requireActivity();
    arguments = requireArguments();
    context = requireContext();
    // Set up options menu.
    setHasOptionsMenu(true);
    // Set up fragment to request permissions (i.e. fine location).
    requestPermissionLauncher =
        registerForActivityResult(new RequestPermission(), this::onRequestPermissionResult);
    // Set up fragment to get content (i.e. images).
    getContentLauncher = registerForActivityResult(new GetContent(), this::onGetContentResult);
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    final String postType = arguments.getString(context.getString(R.string.args_post_type));
    if (context.getString(R.string.arg_post_type_new).equals(postType)) {
      inflater.inflate(R.menu.menu_post_new, menu);
    } else if (context.getString(R.string.arg_post_type_update).equals(postType)) {
      inflater.inflate(R.menu.menu_post_update, menu);
    } else if (context.getString(R.string.arg_post_type_view).equals(postType)) {
      inflater.inflate(R.menu.menu_post_view, menu);
    } else {
      throw new IllegalArgumentException();
    }
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    final int itemId = item.getItemId();
    if (itemId == R.id.menu_get_location) {
      onLocationButtonClick();
      return true;
    } else if (itemId == R.id.menu_upload_content) {
      onPostButtonClick();
      return true;
    } else if (itemId == R.id.menu_delete) {
      onDeleteButtonClick();
      return true;
    } else if (itemId == R.id.menu_get_directions) {
      // Location should never be null (spotbugs).
      assert location != null;
      final Uri uri =
          Uri.parse(String.format(Locale.US, GEO_URL, location.latitude, location.longitude));
      startActivityFromIntent(new Intent(Intent.ACTION_VIEW, uri));
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public View onCreateView(@NotNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    binding = FragmentPostBinding.inflate(inflater, container, false);
    return binding.getRoot();
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    // Load data from saved state or passed in arguments.
    final String postType = arguments.getString(context.getString(R.string.args_post_type));
    final String postTypeUpdate = context.getString(R.string.arg_post_type_update);
    final String postTypeView = context.getString(R.string.arg_post_type_view);

    String text = null;
    String url = null;
    if (savedInstanceState != null) {
      location = savedInstanceState.getParcelable(SAVESTATE_LOCATION);
      url = savedInstanceState.getString(SAVESTATE_IMAGE_URL);
      text = savedInstanceState.getString(SAVESTATE_TEXT);
    } else if (postType.equals(postTypeUpdate) || postType.equals(postTypeView)) {
      final Message message = arguments.getParcelable(context.getString(R.string.args_message));
      location = new LatLng(message.getLatitude(), message.getLongitude());
      url = message.getImageUrl();
      text = message.getText();
    }

    // Set up the image and text views.
    final NetworkImageView imageView = binding.createPostNetworkImage;
    imageView.setDefaultImageResId(R.drawable.ic_insert_photo);
    imageView.setErrorImageResId(R.drawable.ic_broken_image);
    final EditText editText = binding.createPostCaption;
    editText.setText(text);

    // Disable editing if UI is read only.
    if (postType.equals(postTypeView)) {
      editText.setEnabled(false);
      editText.setFocusable(false);
      editText.setFocusableInTouchMode(false);
    } else {
      // Enable click listener for selecting an image.
      binding.imageContainer.setOnClickListener(v -> getContentLauncher.launch(MEDIA_TYPE_IMAGE));
    }

    // Load image.
    if (url != null) {
      loadImage(url);
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    // Start location updates.
    if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        == PackageManager.PERMISSION_GRANTED) {
      locationListener = new LocationListener();
      fusedLocationClient.requestLocationUpdates(LocationRequest.create(), locationListener, null);
    } else {
      requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (locationListener != null) {
      fusedLocationClient.removeLocationUpdates(locationListener);
      locationListener = null;
    }
  }

  @Override
  public void onSaveInstanceState(@NotNull Bundle bundle) {
    final String postType = arguments.getString(context.getString(R.string.args_post_type));
    final String postTypeView = context.getString(R.string.arg_post_type_view);

    if (!postType.equals(postTypeView)) {
      bundle.putParcelable(SAVESTATE_LOCATION, location);
      bundle.putString(SAVESTATE_IMAGE_URL, imageUrl);
      bundle.putString(SAVESTATE_TEXT, binding.createPostCaption.getText().toString());
    }
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    binding = null;
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    activity = null;
    context = null;
    arguments = null;
  }

  /**
   * Start an activity from an intent.
   *
   * @param intent the intent used to start an activity
   */
  @VisibleForTesting
  void startActivityFromIntent(@NonNull Intent intent) {
    if (intent.resolveActivity(context.getPackageManager()) != null) {
      activity.startActivity(intent);
    } else {
      Toast.makeText(context, context.getString(R.string.failure_no_available_activity),
          Toast.LENGTH_LONG).show();
    }
  }

  /**
   * The result of a "request permission" request.
   *
   * @param result {@code true} if permission granted, {@code false} otherwise
   */
  @VisibleForTesting
  void onRequestPermissionResult(Boolean result) {
    if (result) {
      try {
        locationListener = new LocationListener();
        fusedLocationClient
            .requestLocationUpdates(LocationRequest.create(), locationListener, null);
        return;
      } catch (SecurityException unlikely) {
        fusedLocationClient.removeLocationUpdates(locationListener);
        locationListener = null;
      }
      Toast.makeText(context, context.getString(R.string.failure_fine_location_permission),
          Toast.LENGTH_LONG).show();
    }
  }

  /**
   * Get the result of a "get content" request.
   *
   * @param result the result of the request, or {@code null} if there is no result
   */
  @VisibleForTesting
  void onGetContentResult(@Nullable Uri result) {
    if (result != null) {
      loadImage(result.toString());
    }
  }

  /**
   * Loads an image from a URI into the image view.
   *
   * @param imageUrl image to load
   */
  @VisibleForTesting
  void loadImage(@NonNull String imageUrl) {
    this.imageUrl = imageUrl;
    final Uri imageUri = Uri.parse(imageUrl);
    final NetworkImageView networkImageView = binding.createPostNetworkImage;
    final ImageView imageView = binding.createPostLocalImage;

    if (imageUri.getScheme().contains(URI_SCHEME_HTTP)) {
      networkImageView.setVisibility(View.VISIBLE);
      imageView.setVisibility(View.INVISIBLE);
      imageLoader.get(imageUrl, ImageLoader
          .getImageListener(networkImageView, R.drawable.ic_insert_photo,
              R.drawable.ic_broken_image));
      networkImageView.setImageUrl(imageUrl, imageLoader);
    } else {
      networkImageView.setImageUrl(null, null);
      networkImageView.setVisibility(View.INVISIBLE);
      imageView.setVisibility(View.VISIBLE);
      try {
        final InputStream inputStream = contentResolver.openInputStream(imageUri);
        imageView
            .setImageBitmap((inputStream == null) ? null : BitmapFactory.decodeStream(inputStream));
      } catch (final FileNotFoundException exception) {
        this.imageUrl = null;
        imageView.setImageResource(R.drawable.ic_broken_image);
        Toast.makeText(context, context.getString(R.string.failure_load_image), Toast.LENGTH_SHORT)
            .show();
      }
    }
  }

  /**
   * GPS button click callback. By the time this button is available, location permissions should
   * have already been granted.
   */
  @VisibleForTesting
  void onLocationButtonClick() {
    if (locationListener != null) {
      final Location loc = locationListener.getLocation();
      if (loc != null) {
        location = new LatLng(loc.getLatitude(), loc.getLongitude());
        Toast.makeText(context, context.getString(R.string.success_location_updated),
            Toast.LENGTH_SHORT).show();
        return;
      }
    }
    Toast.makeText(context, context.getString(R.string.failure_location_unavailable),
        Toast.LENGTH_LONG).show();
  }

  /**
   * Post button click callback.
   */
  @VisibleForTesting
  void onPostButtonClick() { // TODO get user ID and post ID
    // Check for valid input and update post repository.
    final String text = binding.createPostCaption.getText().toString();
    if (imageUrl == null || TextUtils.isEmpty(text) || location == null) {
      Toast
          .makeText(context, context.getString(R.string.failure_fields_missing), Toast.LENGTH_SHORT)
          .show();
    } else if (arguments.getString(context.getString(R.string.args_post_type))
        .equals(context.getString(R.string.arg_post_type_new))) {
      postRepository
          .newMessage("", location.latitude, location.longitude, imageUrl, text,
              this::onNewMessageResponse, this::onNetworkError);
    } else if (arguments.getString(context.getString(R.string.args_post_type))
        .equals(context.getString(R.string.arg_post_type_update))) {
      postRepository.updateMessage("", "", location.latitude, location.longitude, imageUrl, text,
          this::onUpdateMessageResponse, this::onNetworkError);
    } else {
      throw new IllegalArgumentException();
    }
  }

  /**
   * Delete a message post from the post repository.
   */
  @VisibleForTesting
  void onDeleteButtonClick() {
    final Message message = arguments.getParcelable(context.getString(R.string.args_message));
    // Message should never be null (spotbugs).
    assert message != null;
    postRepository
        .deleteMessage(message.getId(), message.getUserId(), this::onDeleteMessageResponse,
            this::onNetworkError);
  }

  /**
   * Called when a new message post is successfully created.
   *
   * @param response network response
   */
  @VisibleForTesting
  void onNewMessageResponse(NewMessageResponse response) {
    Toast.makeText(context, context.getString(R.string.success_new_post), Toast.LENGTH_SHORT)
        .show();
    closeFragment();
  }

  /**
   * Called when a message post is successfully updated.
   *
   * @param response network response
   */
  @VisibleForTesting
  void onUpdateMessageResponse(UpdateMessageResponse response) {
    Toast.makeText(context, context.getString(R.string.success_update_post), Toast.LENGTH_SHORT)
        .show();
    closeFragment();
  }

  /**
   * Called when a message post is successfully deleted.
   *
   * @param response network response
   */
  @VisibleForTesting
  void onDeleteMessageResponse(DeleteMessageResponse response) {
    Toast.makeText(context, context.getString(R.string.success_delete_post), Toast.LENGTH_SHORT)
        .show();
    closeFragment();
  }

  /**
   * Programmatically clean up and close the fragment.
   */
  @VisibleForTesting
  void closeFragment() {
    // TODO close the keyboard if required
    activity.onBackPressed();
  }

  /**
   * Callback for a network error.
   */
  @VisibleForTesting
  void onNetworkError(VolleyError error) {
    Toast.makeText(context, context.getString(R.string.failure_network_error), Toast.LENGTH_LONG)
        .show();
  }

}
