package com.magikarp.android.ui.app;

import android.accounts.Account;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.preference.PreferenceManager;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.NetworkImageView;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.navigation.NavigationView;
import com.magikarp.android.R;
import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity implements MenuItem.OnMenuItemClickListener,
    OnCompleteListener<Void> {

  @VisibleForTesting
  static final int SIGN_IN_RESULT = 9001;

  private AppBarConfiguration appBarConfiguration;

  private DrawerLayout drawerLayout;

  private GoogleSignInViewModel viewModel;

  private NavController navController;

  private NavigationView navigationView;

  @Inject
  GoogleSignInClient googleSignInClient;

  @Inject
  ImageLoader imageLoader;

  public MainActivity() {
  }

  @VisibleForTesting
  MainActivity(NavigationView navigationView) {
    this.navigationView = navigationView;
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Toolbar toolbar = findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    drawerLayout = findViewById(R.id.drawer_layout);
    navigationView = findViewById(R.id.nav_view);
    NavHostFragment navHostFragment =
        (NavHostFragment) getSupportFragmentManager().findFragmentById(R.id.nav_host_fragment);
    assert navHostFragment != null;

    navController = navHostFragment.getNavController();
    appBarConfiguration =
        new AppBarConfiguration.Builder(R.id.nav_maps, R.id.nav_my_posts,
            R.id.nav_settings, R.id.nav_help).setOpenableLayout(drawerLayout).build();

    NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
    NavigationUI.setupWithNavController(navigationView, navController);

    // Set up sign in buttons.
    navigationView.getMenu().findItem(R.id.action_login).setOnMenuItemClickListener(this);
    navigationView.getMenu().findItem(R.id.action_logout).setOnMenuItemClickListener(this);

    View headerView = navigationView.getHeaderView(0);
    NetworkImageView imageView = headerView.findViewById(R.id.drawer_header_image);
    imageView.setDefaultImageResId(R.mipmap.ic_myplace_round);
    imageView.setErrorImageResId(R.mipmap.ic_myplace_round);
    imageView.setImageUrl(null, null);

    // Set the default shared preferences for the application on first run.
    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    // Check for existing Google Sign In account, if the user is already signed in
    // the GoogleSignInAccount will be non-null.
    final GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
    viewModel = new ViewModelProvider(this).get(GoogleSignInViewModel.class);
    viewModel.setAccount(account);
    Log.i("Main Activity", "Account: " + account);
    updateSignInUi(account);
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    performOnActivityResult(requestCode, data);
  }

  @VisibleForTesting
  void performOnActivityResult(int requestCode, Intent data) {
    if (requestCode == SIGN_IN_RESULT) {
      try {
        GoogleSignInAccount account =
            GoogleSignIn.getSignedInAccountFromIntent(data).getResult(ApiException.class);
        updateSignInUi(account);
      } catch (ApiException e) {
        // The ApiException status code indicates the detailed failure reason.
        // Please refer to the GoogleSignInStatusCodes class reference for more information.
        Log.w("Main Activity", "signInResult:failed code=" + e.getStatusCode());
        updateSignInUi(null);
      }
    }
  }

  @Override
  public boolean onSupportNavigateUp() {
    return NavigationUI.navigateUp(navController, appBarConfiguration)
        || super.onSupportNavigateUp();
  }

  /**
   * Update UI associated with user login.
   *
   * @param account account to retrieve login information
   */
  @VisibleForTesting
  void updateSignInUi(@Nullable GoogleSignInAccount account) {
    if (account != null) {
      // Set the user account text.
      setLoggedInUi(account.getDisplayName(), account.getEmail(), account.getPhotoUrl());
    } else {
      setLoggedOutUi();
    }
  }

  /**
   * Sets the logged in UI.
   *
   * @param displayName display name
   * @param userEmail   user's email
   * @param imageUri    user's profile image
   */
  public void setLoggedInUi(final String displayName, final String userEmail, final Uri imageUri) {
    View headerView = navigationView.getHeaderView(0);
    TextView name = headerView.findViewById(R.id.drawer_header_name);
    TextView email = headerView.findViewById(R.id.drawer_header_email);
    NetworkImageView imageView = headerView.findViewById(R.id.drawer_header_image);

    name.setText(displayName);
    email.setText(userEmail);
    // Set the user account profile picture.
    if (imageUri != null) {
      String urlString = imageUri.toString();
      imageLoader.get(urlString, ImageLoader
          .getImageListener(imageView, R.mipmap.ic_launcher_round, R.mipmap.ic_launcher_round));
      imageView.setImageUrl(urlString, imageLoader);
    }
    // Set the menu choices.
    Menu menu = navigationView.getMenu();
    menu.findItem(R.id.action_login).setVisible(false);
    menu.findItem(R.id.nav_my_posts).setVisible(true);
    menu.setGroupEnabled(R.id.menu_group_logout, true);
    menu.setGroupVisible(R.id.menu_group_logout, true);
  }

  /**
   * Sets the logged out UI.
   */
  public void setLoggedOutUi() {
    View headerView = navigationView.getHeaderView(0);
    TextView name = headerView.findViewById(R.id.drawer_header_name);
    TextView email = headerView.findViewById(R.id.drawer_header_email);
    NetworkImageView imageView = headerView.findViewById(R.id.drawer_header_image);

    name.setText(null);
    email.setText(null);
    imageView.setImageUrl(null, null);
    Menu menu = navigationView.getMenu();
    menu.findItem(R.id.action_login).setVisible(true);
    menu.findItem(R.id.nav_my_posts).setVisible(false);
    menu.setGroupEnabled(R.id.menu_group_logout, false);
    menu.setGroupVisible(R.id.menu_group_logout, false);
  }

  @VisibleForTesting
  public void setDrawerItemVisibility(final int id, final boolean visible) {
    navigationView.getMenu().findItem(id).setVisible(visible);
  }

  @Override
  public boolean onMenuItemClick(MenuItem item) {
    int id = item.getItemId();
    if (id == R.id.action_login) {
      Intent signInIntent = googleSignInClient.getSignInIntent();
      startActivityForResult(signInIntent, SIGN_IN_RESULT);
      drawerLayout.closeDrawers();
      return true;
    } else if (id == R.id.action_logout) {
      googleSignInClient.signOut().addOnCompleteListener(this, this);
      drawerLayout.closeDrawers();
      return true;
    }
    return false;
  }

  @Override
  public void onComplete(@NonNull Task task) {
    viewModel.setAccount(null);
    updateSignInUi(null);
  }

}
