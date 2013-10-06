package com.android.settings.tonyp.statusbar;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import com.android.internal.util.cm.LockscreenTargetUtils;
import com.android.internal.widget.multiwaveview.GlowPadView;
import com.android.settings.R;
import com.android.settings.cyanogenmod.IconPicker;
import com.android.settings.cyanogenmod.ShortcutPickHelper;
import com.android.settings.cyanogenmod.IconPicker.OnIconPickListener;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Intent.ShortcutIconResource;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

public class AppShortcutTargets extends Fragment implements ShortcutPickHelper.OnPickListener, OnIconPickListener {
    
    private static final String TAG = "AppShortcutTargets";
    
    private Activity mActivity;
    private Resources mResources;
    private ShortcutPickHelper mPicker;
    private IconPicker mIconPicker;

    private ViewGroup mContainer;

    private ImageButton mDialogIcon;
    private Button mDialogLabel;

    private ArrayList<TargetInfo> mTargetStore = new ArrayList<TargetInfo>();
    private int mTargetOffset;
    private int mMaxTargets;

    private File mTemporaryImage;
    private int mTargetIndex = 0;
    private static String mEmptyLabel;

    private static final int MENU_SAVE = Menu.FIRST;
    

    class TargetInfo {
        String uri;
        String packageName;
        StateListDrawable icon;
        Drawable defaultIcon;
        String iconType;
        String iconSource;

        TargetInfo(StateListDrawable target) {
            icon = target;
        }

        TargetInfo(String uri, StateListDrawable target, String type, String source, Drawable defaultIcon) {
            this.uri = uri;
            this.icon = target;
            this.defaultIcon = defaultIcon;
            this.iconType = type;
            this.iconSource = source;
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mContainer = container;

        setHasOptionsMenu(true);

        mActivity = getActivity();
        mResources = getResources();

        mIconPicker = new IconPicker(mActivity, this);
        mPicker = new ShortcutPickHelper(mActivity, this);

        mTemporaryImage = new File(mActivity.getCacheDir() + "/target.tmp");
        mEmptyLabel = mResources.getString(R.string.lockscreen_target_empty);

        return inflater.inflate(R.layout.shortcut_app_targets, container, false);
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // If running on a phone, remove padding around container
        if (!LockscreenTargetUtils.isScreenLarge(mActivity)) {
            mContainer.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.add(0, MENU_SAVE, 0, R.string.wifi_save)
            .setIcon(R.drawable.ic_menu_save)
            .setAlphabeticShortcut('s')
            .setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM |
                MenuItem.SHOW_AS_ACTION_WITH_TEXT);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_SAVE:
                saveAll();
                Toast.makeText(mActivity,
                        R.string.shortcut_app_target_save, Toast.LENGTH_LONG).show();
                return true;
            default:
                return false;
        }
    }
    
    /**
     * Save targets to settings provider
     */
    private void saveAll() {
        StringBuilder targetLayout = new StringBuilder();
        ArrayList<String> existingImages = new ArrayList<String>();
        boolean hasValidTargets = false;

        for (int i = mTargetOffset + 1; i <= mTargetOffset + mMaxTargets; i++) {
            TargetInfo info = mTargetStore.get(i);
            String uri = info.uri;

            if (info.iconSource != null) {
                existingImages.add(info.iconSource);
            }

            if (!TextUtils.equals(uri, GlowPadView.EMPTY_TARGET)) {
                try {
                    Intent intent = Intent.parseUri(info.uri, 0);
                    // make sure to remove any outdated icon references
                    intent.removeExtra(GlowPadView.ICON_RESOURCE);
                    intent.removeExtra(GlowPadView.ICON_FILE);
                    if (info.iconType != null) {
                        intent.putExtra(info.iconType, info.iconSource);
                    }
                    if (info.packageName != null) {
                        intent.putExtra(GlowPadView.ICON_PACKAGE, info.packageName);
                    } else {
                        intent.removeExtra(GlowPadView.ICON_PACKAGE);
                    }

                    uri = intent.toUri(0);
                    hasValidTargets = true;
                } catch (URISyntaxException e) {
                    Log.w(TAG, "Invalid uri " + info.uri + " on save, ignoring");
                    uri = GlowPadView.EMPTY_TARGET;
                }
            }

            if (targetLayout.length() > 0) {
                targetLayout.append("|");
            }
            targetLayout.append(uri);
        }

        final String targets = hasValidTargets ? targetLayout.toString() : null;
        Settings.System.putString(mActivity.getContentResolver(),
                Settings.System.LOCKSCREEN_TARGETS, targets);

        for (File image : mActivity.getFilesDir().listFiles()) {
            if (image.getName().startsWith("lockscreen_")
                    && !existingImages.contains(image.getAbsolutePath())) {
                image.delete();
            }
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String shortcutName = null;
        if (data != null) {
            shortcutName = data.getStringExtra(Intent.EXTRA_SHORTCUT_NAME);
        }

        if (TextUtils.equals(shortcutName, mEmptyLabel)) {
            mDialogLabel.setText(mEmptyLabel);
            mDialogLabel.setTag(GlowPadView.EMPTY_TARGET);
            mDialogIcon.setImageResource(R.drawable.ic_empty);
            mDialogIcon.setTag(null);
        } else if (requestCode == IconPicker.REQUEST_PICK_SYSTEM
                || requestCode == IconPicker.REQUEST_PICK_GALLERY
                || requestCode == IconPicker.REQUEST_PICK_ICON_PACK) {
            mIconPicker.onActivityResult(requestCode, resultCode, data);
        } else if (requestCode != Activity.RESULT_CANCELED
                && resultCode != Activity.RESULT_CANCELED) {
            mPicker.onActivityResult(requestCode, resultCode, data);
        }
    }
    
    @Override
    public void shortcutPicked(String uri, String friendlyName, boolean isApplication) {
        try {
            Intent intent = Intent.parseUri(uri, 0);
            Drawable icon = LockscreenTargetUtils.getDrawableFromIntent(mActivity, intent);

            mDialogLabel.setText(friendlyName);
            mDialogLabel.setTag(uri);
            // this is a fresh drawable, so we can assign it directly
            mDialogIcon.setImageDrawable(icon);
            mDialogIcon.setTag(null);
        } catch (URISyntaxException e) {
            Log.wtf(TAG, "Invalid uri " + uri + " on pick");
        }
    }
    
    private View createShortcutDialogView(int target) {
        View view = View.inflate(mActivity, R.layout.lockscreen_shortcut_dialog, null);
        view.findViewById(R.id.icon).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mDialogLabel.getText().equals(mEmptyLabel)) {
                    try {
                        mTemporaryImage.createNewFile();
                        mTemporaryImage.setWritable(true, false);
                        mIconPicker.pickIcon(getId(), mTemporaryImage);
                    } catch (IOException e) {
                        Log.d(TAG, "Could not create temporary icon", e);
                    }
                }
            }
        });
        view.findViewById(R.id.label).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String[] names = new String[] { mEmptyLabel };
                ShortcutIconResource[] icons = new ShortcutIconResource[] {
                    ShortcutIconResource.fromContext(mActivity, android.R.drawable.ic_delete)
                };
                mPicker.pickShortcut(names, icons, getId());
            }
        });

        mDialogIcon = (ImageButton) view.findViewById(R.id.icon);
        mDialogLabel = (Button) view.findViewById(R.id.label);

        TargetInfo item = mTargetStore.get(target);
        setIconForDialog(item.defaultIcon);

        TargetInfo icon = new TargetInfo(null);
        icon.iconType = item.iconType;
        icon.iconSource = item.iconSource;
        icon.packageName = item.packageName;
        mDialogIcon.setTag(icon);

        if (TextUtils.equals(item.uri, GlowPadView.EMPTY_TARGET)) {
            mDialogLabel.setText(mEmptyLabel);
        } else {
            mDialogLabel.setText(mPicker.getFriendlyNameForUri(item.uri));
        }
        mDialogLabel.setTag(item.uri);

        return view;
    }
    
    @Override
    public void iconPicked(int requestCode, int resultCode, Intent intent) {
        TargetInfo icon = new TargetInfo(null);
        Drawable iconDrawable = null;

        if (requestCode == IconPicker.REQUEST_PICK_GALLERY) {
            if (resultCode == Activity.RESULT_OK) {
                File imageFile = new File(mActivity.getFilesDir(),
                        "/lockscreen_" + System.currentTimeMillis() + ".png");
                if (mTemporaryImage.exists()) {
                    mTemporaryImage.renameTo(imageFile);
                }
                imageFile.setReadOnly();

                icon.iconType = GlowPadView.ICON_FILE;
                icon.iconSource = imageFile.getAbsolutePath();
                iconDrawable = LockscreenTargetUtils.getDrawableFromFile(
                        mActivity, icon.iconSource);
            } else {
                if (mTemporaryImage.exists()) {
                    mTemporaryImage.delete();
                }
                return;
            }
        } else if (requestCode == IconPicker.REQUEST_PICK_SYSTEM) {
            icon.iconType = GlowPadView.ICON_RESOURCE;
            icon.iconSource = intent.getStringExtra(IconPicker.RESOURCE_NAME);
            iconDrawable = LockscreenTargetUtils.getDrawableFromResources(mActivity,
                    null, icon.iconSource, false);
        } else if (requestCode == IconPicker.REQUEST_PICK_ICON_PACK
                && resultCode == Activity.RESULT_OK) {
            icon.packageName = intent.getStringExtra(IconPicker.PACKAGE_NAME);
            icon.iconType = GlowPadView.ICON_RESOURCE;
            icon.iconSource = intent.getStringExtra(IconPicker.RESOURCE_NAME);
            iconDrawable = LockscreenTargetUtils.getDrawableFromResources(mActivity,
                    icon.packageName, icon.iconSource, false);
        } else {
            return;
        }

        if (iconDrawable != null) {
            mDialogIcon.setTag(icon);
            setIconForDialog(iconDrawable);
        } else {
            Log.w(TAG, "Could not fetch icon, keeping old one (type=" + icon.iconType
                    + ", source=" + icon.iconSource + ", package= " + icon.packageName + ")");
        }
    }
    
    private void setIconForDialog(Drawable icon) {
        // need to mutate the drawable here to not share drawable state with GlowPadView
        mDialogIcon.setImageDrawable(icon.getConstantState().newDrawable().mutate());
    }
}
