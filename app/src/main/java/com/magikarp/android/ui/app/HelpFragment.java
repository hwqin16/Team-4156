package com.magikarp.android.ui.app;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import com.magikarp.android.R;
import org.jetbrains.annotations.NotNull;

/**
 * A fragment for providing help information.
 */
public class HelpFragment extends Fragment {

  @VisibleForTesting
  FragmentActivity activity;
  @VisibleForTesting
  Context context;

  /**
   * Default constructor.
   */
  public HelpFragment() {
  }

  /**
   * Constructor for testing.
   *
   * @param activity test variable
   * @param context  test variable
   */
  @VisibleForTesting
  HelpFragment(FragmentActivity activity, Context context) {
    this.activity = activity;
    this.context = context;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    performOnCreate();
  }

  @VisibleForTesting
  void performOnCreate() {
    // For unit testing.
    activity = requireActivity();
    context = requireContext();
    // Set up options menu.
    setHasOptionsMenu(true);
  }

  @Override
  public void onCreateOptionsMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
    super.onCreateOptionsMenu(menu, inflater);
    inflater.inflate(R.menu.menu_help, menu);
  }

  @Override
  public boolean onOptionsItemSelected(@NonNull MenuItem item) {
    final int itemId = item.getItemId();
    if (itemId == R.id.menu_terms_of_service) {
      return true;
    } else if (itemId == R.id.menu_privacy_policy) {
      return true;
    } else {
      return super.onOptionsItemSelected(item);
    }
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_help, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    view.findViewById(R.id.call_container).setOnClickListener(this::onCallButtonClicked);
    view.findViewById(R.id.email_container).setOnClickListener(this::onEmailButtonClicked);
  }

  @VisibleForTesting
  @SuppressLint("QueryPermissionsNeeded")
  void onCallButtonClicked(View view) {
    final Uri uri = Uri.parse(
        context.getString(R.string.uri_call) + context.getString(R.string.text_phone_number));
    startActivityFromIntent(new Intent(Intent.ACTION_DIAL, uri));
  }

  @VisibleForTesting
  void onEmailButtonClicked(View view) {
    final Intent intent = new Intent(Intent.ACTION_SENDTO);
    intent.putExtra(Intent.EXTRA_EMAIL,
        new String[] {context.getString(R.string.text_email_address)});
    startActivityFromIntent(intent);
  }

  @VisibleForTesting
  void startActivityFromIntent(@NonNull Intent intent) {
    if (intent.resolveActivity(context.getPackageManager()) != null) {
      activity.startActivity(intent);
    } else {
      Toast.makeText(context, context.getString(R.string.failure_no_available_activity),
          Toast.LENGTH_LONG).show();
    }
  }

}
