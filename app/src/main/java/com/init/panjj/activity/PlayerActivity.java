/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.init.panjj.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.DefaultLoadControl;
import com.google.android.exoplayer2.ExoPlaybackException;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.ExoPlayerFactory;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.SimpleExoPlayer;
import com.google.android.exoplayer2.Timeline;
import com.google.android.exoplayer2.drm.DrmSessionManager;
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto;
import com.google.android.exoplayer2.drm.FrameworkMediaDrm;
import com.google.android.exoplayer2.drm.HttpMediaDrmCallback;
import com.google.android.exoplayer2.drm.StreamingDrmSessionManager;
import com.google.android.exoplayer2.drm.UnsupportedDrmException;
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory;
import com.google.android.exoplayer2.mediacodec.MediaCodecRenderer.DecoderInitializationException;
import com.google.android.exoplayer2.mediacodec.MediaCodecUtil.DecoderQueryException;
import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.ExtractorMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.source.dash.DashMediaSource;
import com.google.android.exoplayer2.source.dash.DefaultDashChunkSource;
import com.google.android.exoplayer2.source.hls.HlsMediaSource;
import com.google.android.exoplayer2.source.smoothstreaming.DefaultSsChunkSource;
import com.google.android.exoplayer2.source.smoothstreaming.SsMediaSource;
import com.google.android.exoplayer2.trackselection.AdaptiveVideoTrackSelection;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector;
import com.google.android.exoplayer2.trackselection.MappingTrackSelector.MappedTrackInfo;
import com.google.android.exoplayer2.trackselection.TrackSelection;
import com.google.android.exoplayer2.trackselection.TrackSelections;
import com.google.android.exoplayer2.trackselection.TrackSelector;
import com.google.android.exoplayer2.ui.DebugTextViewHelper;
import com.google.android.exoplayer2.ui.PlaybackControlView;
import com.google.android.exoplayer2.ui.SimpleExoPlayerView;
import com.google.android.exoplayer2.upstream.DataSource;
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.HttpDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.util.Util;
import com.init.panjj.R;
import com.init.panjj.otherclasses.BackgroundProcess;

import java.net.CookieHandler;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * An activity that plays media using {@link SimpleExoPlayer}.
 */
public class PlayerActivity extends Activity implements OnClickListener, ExoPlayer.EventListener,
    TrackSelector.EventListener<MappedTrackInfo>, PlaybackControlView.VisibilityListener {

  public static final String DRM_SCHEME_UUID_EXTRA = "drm_scheme_uuid";
  public static final String DRM_LICENSE_URL = "drm_license_url";
  public static final String DRM_KEY_REQUEST_PROPERTIES = "drm_key_request_properties";
  public static final String PREFER_EXTENSION_DECODERS = "prefer_extension_decoders";

  public static final String ACTION_VIEW = "com.google.android.exoplayer.demo.action.VIEW";
  public static final String EXTENSION_EXTRA = "extension";

  public static final String ACTION_VIEW_LIST =
      "com.google.android.exoplayer.demo.action.VIEW_LIST";
  public static final String URI_LIST_EXTRA = "uri_list";
  public static final String EXTENSION_LIST_EXTRA = "extension_list";

  private static final DefaultBandwidthMeter BANDWIDTH_METER = new DefaultBandwidthMeter();
  private static final CookieManager DEFAULT_COOKIE_MANAGER;
  static {
    DEFAULT_COOKIE_MANAGER = new CookieManager();
    DEFAULT_COOKIE_MANAGER.setCookiePolicy(CookiePolicy.ACCEPT_ORIGINAL_SERVER);
  }

  private Handler mainHandler;
  private Timeline.Window window;
  private EventLogger eventLogger;
  private SimpleExoPlayerView simpleExoPlayerView;
  private LinearLayout debugRootView;
  private TextView debugTextView;
  private Button retryButton;

  private DataSource.Factory mediaDataSourceFactory;
  private SimpleExoPlayer player;
  private MappingTrackSelector trackSelector;
  private TrackSelectionHelper trackSelectionHelper;
  private DebugTextViewHelper debugViewHelper;
  private boolean playerNeedsSource;

  private boolean shouldAutoPlay;
  private boolean isTimelineStatic;
  private int playerWindow;
  private long playerPosition;

  // Activity lifecycle

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    shouldAutoPlay = true;
    mediaDataSourceFactory = buildDataSourceFactory(true);
    mainHandler = new Handler();
    window = new Timeline.Window();
    if (CookieHandler.getDefault() != DEFAULT_COOKIE_MANAGER) {
      CookieHandler.setDefault(DEFAULT_COOKIE_MANAGER);
    }

    setContentView(R.layout.player_activity);
    View rootView = findViewById(R.id.root);
    rootView.setOnClickListener(this);
    debugRootView = (LinearLayout) findViewById(R.id.controls_root);
    debugTextView = (TextView) findViewById(R.id.debug_text_view);
    retryButton = (Button) findViewById(R.id.retry_button);
    retryButton.setOnClickListener(this);

    simpleExoPlayerView = (SimpleExoPlayerView) findViewById(R.id.player_view);
    simpleExoPlayerView.setControllerVisibilityListener(this);
    simpleExoPlayerView.requestFocus();

  }

  @Override
  public void onNewIntent(Intent intent) {
    releasePlayer();
    isTimelineStatic = false;
    setIntent(intent);
  }

  @Override
  public void onStart() {
    super.onStart();
    if (Util.SDK_INT > 23) {
      initializePlayer();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if ((Util.SDK_INT <= 23 || player == null)) {
      initializePlayer();
    }
  }

  @Override
  public void onPause() {
    super.onPause();
    if (Util.SDK_INT <= 23) {
      releasePlayer();
    }
  }

  @Override
  public void onStop() {
    super.onStop();
    if (Util.SDK_INT > 23) {
      releasePlayer();
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions,
      int[] grantResults) {
    if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      initializePlayer();
    } else {
      showToast(R.string.storage_permission_denied);
      finish();
    }
  }

  // OnClickListener methods

  @Override
  public void onClick(View view) {
    if (view == retryButton) {
      initializePlayer();
    } else if (view.getParent() == debugRootView) {
      trackSelectionHelper.showSelectionDialog(this, ((Button) view).getText(),
          trackSelector.getCurrentSelections().info, (int) view.getTag());
    }
  }

  // PlaybackControlView.VisibilityListener implementation

  @Override
  public void onVisibilityChange(int visibility) {
    debugRootView.setVisibility(visibility);
  }

  // Internal methods

  private void initializePlayer() {
    Intent intent = getIntent();
    if (player == null) {
      boolean preferExtensionDecoders = intent.getBooleanExtra(PREFER_EXTENSION_DECODERS, false);
      UUID drmSchemeUuid = intent.hasExtra(DRM_SCHEME_UUID_EXTRA)
          ? UUID.fromString(intent.getStringExtra(DRM_SCHEME_UUID_EXTRA)) : null;
      DrmSessionManager<FrameworkMediaCrypto> drmSessionManager = null;
      if (drmSchemeUuid != null) {
        String drmLicenseUrl = intent.getStringExtra(DRM_LICENSE_URL);
        String[] keyRequestPropertiesArray = intent.getStringArrayExtra(DRM_KEY_REQUEST_PROPERTIES);
        Map<String, String> keyRequestProperties;
        if (keyRequestPropertiesArray == null || keyRequestPropertiesArray.length < 2) {
          keyRequestProperties = null;
        } else {
          keyRequestProperties = new HashMap<>();
          for (int i = 0; i < keyRequestPropertiesArray.length - 1; i += 2) {
            keyRequestProperties.put(keyRequestPropertiesArray[i],
                keyRequestPropertiesArray[i + 1]);
          }
        }
        try {
          drmSessionManager = buildDrmSessionManager(drmSchemeUuid, drmLicenseUrl,
              keyRequestProperties);
        } catch (UnsupportedDrmException e) {
          int errorStringId = Util.SDK_INT < 18 ? R.string.error_drm_not_supported
              : (e.reason == UnsupportedDrmException.REASON_UNSUPPORTED_SCHEME
                  ? R.string.error_drm_unsupported_scheme : R.string.error_drm_unknown);
          showToast(errorStringId);
          return;
        }
      }

      eventLogger = new EventLogger();
      TrackSelection.Factory videoTrackSelectionFactory =
          new AdaptiveVideoTrackSelection.Factory(BANDWIDTH_METER);
      trackSelector = new DefaultTrackSelector(mainHandler, videoTrackSelectionFactory);
      trackSelector.addListener(this);
      trackSelector.addListener(eventLogger);
      trackSelectionHelper = new TrackSelectionHelper(trackSelector, videoTrackSelectionFactory);
      player = ExoPlayerFactory.newSimpleInstance(this, trackSelector, new DefaultLoadControl(),
          drmSessionManager, preferExtensionDecoders);
      player.addListener(this);
      player.addListener(eventLogger);
      player.setAudioDebugListener(eventLogger);
      player.setVideoDebugListener(eventLogger);
      player.setId3Output(eventLogger);
      simpleExoPlayerView.setPlayer(player);
      if (isTimelineStatic) {
        if (playerPosition == C.TIME_UNSET) {
          player.seekToDefaultPosition(playerWindow);
        } else {
          player.seekTo(playerWindow, playerPosition);
        }
      }
      player.setPlayWhenReady(shouldAutoPlay);
      debugViewHelper = new DebugTextViewHelper(player, debugTextView);
      debugViewHelper.start();
      playerNeedsSource = true;
    }
    if (playerNeedsSource) {
      String action = intent.getAction();
      Uri[] uris;
      String[] extensions;
      if (ACTION_VIEW.equals(action)) {
        uris = new Uri[] {intent.getData()};
        extensions = new String[] {intent.getStringExtra(EXTENSION_EXTRA)};
      } else if (ACTION_VIEW_LIST.equals(action)) {
        String[] uriStrings = intent.getStringArrayExtra(URI_LIST_EXTRA);
        uris = new Uri[uriStrings.length];
        for (int i = 0; i < uriStrings.length; i++) {
          uris[i] = Uri.parse(uriStrings[i]);
        }
        extensions = intent.getStringArrayExtra(EXTENSION_LIST_EXTRA);
        if (extensions == null) {
          extensions = new String[uriStrings.length];
        }
      } else {
        showToast(getString(R.string.unexpected_intent_action, action));
        return;
      }
      if (Util.maybeRequestReadExternalStoragePermission(this, uris)) {
        // The player will be reinitialized if the permission is granted.
        return;
      }
      MediaSource[] mediaSources = new MediaSource[uris.length];
      for (int i = 0; i < uris.length; i++) {
        mediaSources[i] = buildMediaSource(uris[i], extensions[i]);
      }
      MediaSource mediaSource = mediaSources.length == 1 ? mediaSources[0]
          : new ConcatenatingMediaSource(mediaSources);
      // Measures bandwidth during playback. Can be null if not required.
      DefaultBandwidthMeter bandwidthMeter = new DefaultBandwidthMeter();
// Produces DataSource instances through which media data is loaded.
      DataSource.Factory dataSourceFactory = new DefaultDataSourceFactory(this, Util.getUserAgent(this, "yourApplicationName"), bandwidthMeter);
// Produces Extractor instances for parsing the media data.
// This is the MediaSource representing the media to be played.
    // Prepare the player with the source.
      MediaSource videoSource = new ExtractorMediaSource(uris[0], dataSourceFactory, new DefaultExtractorsFactory(), null, null);
      MediaSource subtitleSource = new SingleSampleMediaSource(Uri.parse("file:///android_asset/myfile.vtt"),dataSourceFactory,Format.createTextSampleFormat("0", MimeTypes.TEXT_VTT, null, Format.NO_VALUE, C.SELECTION_FLAG_DEFAULT, "se", null, 0),1000);
// Plays the video with the sideloaded subtitle.
      MergingMediaSource mergedSource = new MergingMediaSource(videoSource, subtitleSource);
      player.prepare(mergedSource);
     // player.prepare(mediaSource, !isTimelineStatic, !isTimelineStatic);
      playerNeedsSource = false;
      updateButtonVisibilities();
    }
  }

  private MediaSource buildMediaSource(Uri uri, String overrideExtension) {
    int type = Util.inferContentType(!TextUtils.isEmpty(overrideExtension) ? "." + overrideExtension
        : uri.getLastPathSegment());
    switch (type) {
      case C.TYPE_SS:
        return new SsMediaSource(uri, buildDataSourceFactory(false),
            new DefaultSsChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
      case C.TYPE_DASH:
        return new DashMediaSource(uri, buildDataSourceFactory(false),
            new DefaultDashChunkSource.Factory(mediaDataSourceFactory), mainHandler, eventLogger);
      case C.TYPE_HLS:
        return new HlsMediaSource(uri, mediaDataSourceFactory, mainHandler, eventLogger);
      case C.TYPE_OTHER:
        return new ExtractorMediaSource(uri, mediaDataSourceFactory, new DefaultExtractorsFactory(),
            mainHandler, eventLogger);
      default: {
        throw new IllegalStateException("Unsupported type: " + type);
      }
    }
  }

  private DrmSessionManager<FrameworkMediaCrypto> buildDrmSessionManager(UUID uuid,
      String licenseUrl, Map<String, String> keyRequestProperties) throws UnsupportedDrmException {
    if (Util.SDK_INT < 18) {
      return null;
    }
    HttpMediaDrmCallback drmCallback = new HttpMediaDrmCallback(licenseUrl,
        buildHttpDataSourceFactory(false), keyRequestProperties);
    return new StreamingDrmSessionManager<>(uuid,
        FrameworkMediaDrm.newInstance(uuid), drmCallback, null, mainHandler, eventLogger);
  }

  private void releasePlayer() {
    if (player != null) {
      debugViewHelper.stop();
      debugViewHelper = null;
      shouldAutoPlay = player.getPlayWhenReady();
      playerWindow = player.getCurrentWindowIndex();
      playerPosition = C.TIME_UNSET;
      Timeline timeline = player.getCurrentTimeline();
      if (timeline != null && timeline.getWindow(playerWindow, window).isSeekable) {
        playerPosition = player.getCurrentPosition();
      }
      player.release();
      player = null;
      trackSelector = null;
      trackSelectionHelper = null;
      eventLogger = null;
    }
  }

  /**
   * Returns a new DataSource factory.
   *
   * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
   *     DataSource factory.
   * @return A new DataSource factory.
   */
  private DataSource.Factory buildDataSourceFactory(boolean useBandwidthMeter) {
    return ((BackgroundProcess) getApplication())
        .buildDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
  }

  /**
   * Returns a new HttpDataSource factory.
   *
   * @param useBandwidthMeter Whether to set {@link #BANDWIDTH_METER} as a listener to the new
   *     DataSource factory.
   * @return A new HttpDataSource factory.
   */
  private HttpDataSource.Factory buildHttpDataSourceFactory(boolean useBandwidthMeter) {
    return ((BackgroundProcess) getApplication())
        .buildHttpDataSourceFactory(useBandwidthMeter ? BANDWIDTH_METER : null);
  }

  // ExoPlayer.EventListener implementation

  @Override
  public void onLoadingChanged(boolean isLoading) {
    // Do nothing.
  }

  @Override
  public void onPlayerStateChanged(boolean playWhenReady, int playbackState) {
    if (playbackState == ExoPlayer.STATE_ENDED) {
      showControls();
    }
    updateButtonVisibilities();
  }

  @Override
  public void onPositionDiscontinuity() {
    // Do nothing.
  }

  @Override
  public void onTimelineChanged(Timeline timeline, Object manifest) {
    isTimelineStatic = timeline != null && timeline.getWindowCount() > 0
        && !timeline.getWindow(timeline.getWindowCount() - 1, window).isDynamic;
  }

  @Override
  public void onPlayerError(ExoPlaybackException e) {
    String errorString = null;
    if (e.type == ExoPlaybackException.TYPE_RENDERER) {
      Exception cause = e.getRendererException();
      if (cause instanceof DecoderInitializationException) {
        // Special case for decoder initialization failures.
        DecoderInitializationException decoderInitializationException =
            (DecoderInitializationException) cause;
        if (decoderInitializationException.decoderName == null) {
          if (decoderInitializationException.getCause() instanceof DecoderQueryException) {
            errorString = getString(R.string.error_querying_decoders);
          } else if (decoderInitializationException.secureDecoderRequired) {
            errorString = getString(R.string.error_no_secure_decoder,
                decoderInitializationException.mimeType);
          } else {
            errorString = getString(R.string.error_no_decoder,
                decoderInitializationException.mimeType);
          }
        } else {
          errorString = getString(R.string.error_instantiating_decoder,
              decoderInitializationException.decoderName);
        }
      }
    }
    if (errorString != null) {
      showToast(errorString);
    }
    playerNeedsSource = true;
    updateButtonVisibilities();
    showControls();
  }

  // MappingTrackSelector.EventListener implementation

  @Override
  public void onTrackSelectionsChanged(TrackSelections<? extends MappedTrackInfo> trackSelections) {
    updateButtonVisibilities();
    MappedTrackInfo trackInfo = trackSelections.info;
    if (trackInfo.hasOnlyUnplayableTracks(C.TRACK_TYPE_VIDEO)) {
      showToast(R.string.error_unsupported_video);
    }
    if (trackInfo.hasOnlyUnplayableTracks(C.TRACK_TYPE_AUDIO)) {
      showToast(R.string.error_unsupported_audio);
    }
  }

  // User controls

  private void updateButtonVisibilities() {
    debugRootView.removeAllViews();

    retryButton.setVisibility(playerNeedsSource ? View.VISIBLE : View.GONE);
    debugRootView.addView(retryButton);

    if (player == null) {
      return;
    }

    TrackSelections<MappedTrackInfo> trackSelections = trackSelector.getCurrentSelections();
    if (trackSelections == null) {
      return;
    }

    int rendererCount = trackSelections.length;
    for (int i = 0; i < rendererCount; i++) {
      TrackGroupArray trackGroups = trackSelections.info.getTrackGroups(i);
      if (trackGroups.length != 0) {
        Button button = new Button(this);
        int label;
        switch (player.getRendererType(i)) {
          case C.TRACK_TYPE_AUDIO:
            label = R.string.audio;
            break;
          case C.TRACK_TYPE_VIDEO:
            label = R.string.video;
            break;
          case C.TRACK_TYPE_TEXT:
            label = R.string.text;
            break;
          default:
            continue;
        }
        button.setText(label);
        button.setTag(i);
        button.setOnClickListener(this);
        debugRootView.addView(button, debugRootView.getChildCount() - 1);
      }
    }
  }

  private void showControls() {
    debugRootView.setVisibility(View.VISIBLE);
  }

  private void showToast(int messageId) {
    showToast(getString(messageId));
  }

  private void showToast(String message) {
    Toast.makeText(getApplicationContext(), message, Toast.LENGTH_LONG).show();
  }

}
