package com.android.settings.tonyp.statusbar;

import java.util.ArrayList;

import com.android.settings.R;
import com.mobeta.android.dslv.DragSortController;
import com.mobeta.android.dslv.DragSortListView;
import com.mobeta.android.dslv.SwipeDismissListViewTouchListener;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.res.Resources;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

public class StatusBarToggles extends Fragment implements OnItemClickListener {
    
    private static final int MENU_SAVE = Menu.FIRST;
    
    public static final ListAdapterItem[] ITEMS = new ListAdapterItem[] {
            new ListAdapterItem(R.string.category_quick_access, R.string.summary_quick_access, 
                    Settings.System.SHOW_RIBBONS, R.drawable.ic_tab_selected_all),
                    
            new ListAdapterItem(R.string.title_brightness_slider, R.string.summary_brightness_slider, 
                    Settings.System.SHOW_BRIGHTNESS_SLIDER, R.drawable.ic_settings_display, true, 
                    Settings.System.SHOW_BRIGHTNESS_SLIDER_TRANSPARENT),
                    
            new ListAdapterItem(R.string.title_volume_slider, R.string.summary_volume_slider, 
                    Settings.System.SHOW_VOLUME_SLIDER, R.drawable.ic_settings_sound, true, 
                    Settings.System.SHOW_VOLUME_SLIDER_TRANSPARENT),
    };
    
    private Activity mActivity;
    private Resources mResources;
    
    private PopupMenu mAddExtensionPopupMenu;
    private ExtensionListAdapter mSelectedExtensionsAdapter;
    private TogglesList mTogglesList;
    
    private DragSortListView mListView;
    
    private int mBSlider, mVSlider, mRibbonPos;
    
    private ContentResolver mCr;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mTogglesList = new TogglesList();
        mSelectedExtensionsAdapter = new ExtensionListAdapter();
        mCr = getActivity().getContentResolver();
        
        mBSlider = -1;
        mVSlider = -1;
        mRibbonPos = -1;
        try {
            mBSlider = Settings.System.getIntForUser(mCr, Settings.System.SHOW_BRIGHTNESS_SLIDER, -1, UserHandle.myUserId());
            mVSlider = Settings.System.getIntForUser(mCr, Settings.System.SHOW_VOLUME_SLIDER, -1, UserHandle.myUserId());
            mRibbonPos = Settings.System.getIntForUser(mCr, Settings.System.SHOW_RIBBONS, -1, UserHandle.myUserId());
        } catch(Exception ex) {}
        
        if(mRibbonPos >= 0)mTogglesList.addItemWithoutReorder(ITEMS[0], mRibbonPos);
        
        if(mBSlider >= 0)mTogglesList.addItemWithoutReorder(ITEMS[1], mBSlider);
        
        if(mVSlider >= 0)mTogglesList.addItemWithoutReorder(ITEMS[2], mVSlider);
        mTogglesList.moveToAdapter();
        mSelectedExtensionsAdapter.notifyDataSetChanged();
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        setHasOptionsMenu(true);

        mActivity = getActivity();
        mResources = getResources();

        View v = inflater.inflate(R.layout.navigation_drawer_toggles, container, false);
        
        mListView = (DragSortListView) v.findViewById(android.R.id.list);
        mListView.setAdapter(mSelectedExtensionsAdapter);
        
        final DragSortController dragSortController = new ConfigurationDragSortController();
        mListView.setFloatViewManager(dragSortController);
        mListView.setDropListener(new DragSortListView.DropListener() {
            @Override
            public void drop(int from, int to) {
                ListAdapterItem item = mTogglesList.get(from);
                mTogglesList.removeAt(from);
                mTogglesList.addItem(item, to);
                mTogglesList.moveToAdapter();
                mSelectedExtensionsAdapter.notifyDataSetChanged();
            }
        });
        final SwipeDismissListViewTouchListener swipeDismissTouchListener =
                new SwipeDismissListViewTouchListener(
                        mListView,
                        new SwipeDismissListViewTouchListener.DismissCallbacks() {
                            public boolean canDismiss(int position) {
                                return position < mSelectedExtensionsAdapter.getCount() - 1;
                            }

                            public void onDismiss(ListView listView, int[] reverseSortedPositions) {
                                for (int position : reverseSortedPositions) {
                                    mTogglesList.removeAtWithoutReorder(position);
                                }
                                mTogglesList.moveToAdapter();
                                mSelectedExtensionsAdapter.notifyDataSetChanged();
                            }
                        });
        mListView.setOnItemClickListener(this);
        mListView.setOnScrollListener(swipeDismissTouchListener.makeScrollListener());
        mListView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                return dragSortController.onTouch(view, motionEvent)
                        || (!dragSortController.isDragging()
                        && swipeDismissTouchListener.onTouch(view, motionEvent));

            }
        });
        mListView.setItemsCanFocus(true);
        
        return v;
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
                return true;
            default:
                return false;
        }
    }
    
    private void writeSetting(String name, int value) {
        Settings.System.putIntForUser(mCr, name, value, UserHandle.myUserId());
    }
    
    private void saveAll() {
        mTogglesList.reorder();
        ArrayList<ListAdapterItem>list = new ArrayList<ListAdapterItem>();
        for(int i =0;i<mTogglesList.length;i++) {
            ListAdapterItem item = mTogglesList.get(i);
            if(item!=null) {
                list.add(item);
                writeSetting(item.saveToSetting, i);
                if(item.hasTransparentButton) {
                    writeSetting(item.saveTransparentToSetting, item.transparent ? 1 : 0);
                }
            }
        } //Now all visible parts are saved, continue with non visible
        for(int i =0;i<ITEMS.length;i++) {
            ListAdapterItem item = ITEMS[i];
            if(item != null && !list.contains(item)) {
                writeSetting(item.saveToSetting, -1);
            }
        }
    }
    
    private void showAddExtensionMenu(View anchorView) {
        if (mAddExtensionPopupMenu != null) {
            mAddExtensionPopupMenu.dismiss();
        }

        mAddExtensionPopupMenu = new PopupMenu(getActivity(), anchorView);

        for (int i = 0; i < ITEMS.length; i++) {
            ListAdapterItem item = ITEMS[i];
            String label = mResources.getString(item.title);
            if (TextUtils.isEmpty(label) || mTogglesList.hasItem(item)) {
                label = null;
            }
            
            if(label != null)mAddExtensionPopupMenu.getMenu().add(Menu.NONE, i, Menu.NONE, label);
        }
        mAddExtensionPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                mAddExtensionPopupMenu.dismiss();
                mAddExtensionPopupMenu = null;

                ListAdapterItem item = ITEMS[menuItem.getItemId()];

                // Item id == position for this popup menu.
                mTogglesList.addItem(item);
                mTogglesList.moveToAdapter();
                mSelectedExtensionsAdapter.notifyDataSetChanged();
                mListView.smoothScrollToPosition(mSelectedExtensionsAdapter.getCount() - 1);
                return true;
            }
        });
        mAddExtensionPopupMenu.show();
    }
    
    private class ConfigurationDragSortController extends DragSortController {
        private int mPos;

        public ConfigurationDragSortController() {
            super(StatusBarToggles.this.mListView, R.id.drag_handle,
                    DragSortController.ON_DOWN, 0);
            setRemoveEnabled(false);
        }

        @Override
        public int startDragPosition(MotionEvent ev) {
            int res = super.dragHandleHitPosition(ev);
            if (res >= mSelectedExtensionsAdapter.getCount() - 1) {
                return DragSortController.MISS;
            }

            return res;
        }

        @Override
        public View onCreateFloatView(int position) {
            mPos = position;

            return mSelectedExtensionsAdapter.getView(position, null, mListView);
        }

        private int origHeight = -1;

        @Override
        public void onDragFloatView(View floatView, Point floatPoint, Point touchPoint) {
            final int addPos = mSelectedExtensionsAdapter.getCount() - 1;
            final int first = mListView.getFirstVisiblePosition();
            final int lvDivHeight = mListView.getDividerHeight();

            if (origHeight == -1) {
                origHeight = floatView.getHeight();
            }

            View div = mListView.getChildAt(addPos - first);

            if (touchPoint.x > mListView.getWidth() / 2) {
                float scale = touchPoint.x - mListView.getWidth() / 2;
                scale /= (float) (mListView.getWidth() / 5);
                ViewGroup.LayoutParams lp = floatView.getLayoutParams();
                lp.height = Math.max(origHeight, (int) (scale * origHeight));
                //Log.d("mobeta", "setting height " + lp.height);
                floatView.setLayoutParams(lp);
            }

            if (div != null) {
                if (mPos > addPos) {
                    // don't allow floating View to go above
                    // section divider
                    final int limit = div.getBottom() + lvDivHeight;
                    if (floatPoint.y < limit) {
                        floatPoint.y = limit;
                    }
                } else {
                    // don't allow floating View to go below
                    // section divider
                    final int limit = div.getTop() - lvDivHeight - floatView.getHeight();
                    if (floatPoint.y > limit) {
                        floatPoint.y = limit;
                    }
                }
            }
        }

        @Override
        public void onDestroyFloatView(View floatView) {
            //do nothing; block super from crashing
        }
    }
    
    public class TogglesList {
        private ListAdapterItem[] items;
        public final int length;
        
        public TogglesList() {
            length = ITEMS.length;
            items = new ListAdapterItem[length];
        }
        
        public ListAdapterItem get(int pos) {
            if(pos < 0 || pos >= items.length)return null;
            return items[pos];
        }
        
        public boolean hasItem(ListAdapterItem item) {
            for(int i =0;i<length;i++) {
                if(items[i] != null &&items[i].title == item.title)
                    return true;
            }
            return false;
        }
        
        public void addItem(ListAdapterItem item, int pos) {
            if(hasItem(item))return;
            if(pos < 0 || pos >= length)return;
            reorder();
            if(pos == length - 1) {
                items[pos] = item;
                return;
            }
            if(items[pos] != null)moveOneBack(pos);
            items[pos] = item;
        }
        
        public void addItemWithoutReorder(ListAdapterItem item, int pos) {
            if(hasItem(item))return;
            if(pos < 0 || pos >= length)return;
            if(pos == length - 1) {
                items[pos] = item;
                return;
            }
            if(items[pos] != null)moveOneBack(pos);
            items[pos] = item;
        }
        
        private void moveOneBack(int pos) {
            int i = pos + 1;
            if(i < length) {
                if(items[i] != null)moveOneBack(i);
                items[i] = items[pos];
                items[pos] = null;
            }
        }
        
        public void addItem(ListAdapterItem item) {
            if(hasItem(item))return;
            reorder();
            for(int i =0;i<length;i++) {
                if(items[i]==null){
                    items[i] = item;
                    return;
                }
            }
        }
        
        public void removeItem(ListAdapterItem item) {
            for(int i = 0;i<length;i++) {
                if(items[i].title == item.title) {
                    items[i] = null;
                    reorder();
                    return;
                }
            }
        }
        
        public void removeAt(int pos) {
            if(pos < 0 || pos >= length)return;
            items[pos] = null;
            reorder();
        }
        
        public void removeAtWithoutReorder(int pos) {
            if(pos < 0 || pos >= length)return;
            items[pos] = null;
        }
        
        private void swap(int i, int j) {
            ListAdapterItem it = items[i];
            items[i] = items[j];
            items[j] = it;
        }
        
        public void reorder() {
            for(int i = 0;i<length;i++) {
                if(items[i]!= null) {
                    int j = i - 1;
                    if(j >= 0 && items[j] == null) {
                        swap(i, j);
                        i--;//Check again
                    }
                }
            }
        }
        
        public void moveToAdapter() {
            reorder();
            mSelectedExtensionsAdapter.clearAll();
            for(int i=0;i<length;i++) {
                if(items[i] != null)mSelectedExtensionsAdapter.addItem(items[i]);
            }
        }
    }
    
    public class ExtensionListAdapter extends BaseAdapter {
        
        private ArrayList<ListAdapterItem>mList;
        private static final int VIEW_TYPE_ITEM = 0;
        private static final int VIEW_TYPE_ADD = 1;

        public ExtensionListAdapter () {
            mList = new ArrayList<ListAdapterItem>();
        }
        
        @Override
        public int getViewTypeCount() {
            return 2;
        }

        public void clearAll() {
            mList.clear();
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
        
        public void addItem(ListAdapterItem item) {
            mList.add(item);
        }

        @Override
        public int getCount() {
            int numItems = mList.size();
            // Hide add row to show empty view if there are no items.
            return (numItems + 1);
        }

        @Override
        public int getItemViewType(int position) {
            return (position == getCount() - 1)
                    ? VIEW_TYPE_ADD
                    : VIEW_TYPE_ITEM;
        }

        @Override
        public Object getItem(int position) {
            return (getItemViewType(position) == VIEW_TYPE_ADD)
                    ? null
                    : mList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return (getItemViewType(position) == VIEW_TYPE_ADD)
                    ? -1
                    : mList.get(position).hashCode();
        }

        @Override
        public boolean isEnabled(int position) {
            return (getItemViewType(position) == VIEW_TYPE_ADD) && mList.size() > 0;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }


        @Override
        public View getView(int position, View convertView, ViewGroup parent) {       
            switch (getItemViewType(position)) {
                case VIEW_TYPE_ADD: {
                    if (convertView == null) {
                        convertView = getActivity().getLayoutInflater()
                                .inflate(R.layout.list_item_add_extension, parent, false);
                        final View v = convertView;
                        convertView.findViewById(R.id.add_statusbar_toggle).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                showAddExtensionMenu(v);
                            }
                        });
                    }
                    convertView.setEnabled(isEnabled(position));
                    return convertView;
                }
    
                case VIEW_TYPE_ITEM: {
                if (convertView == null) {
                    convertView = getActivity().getLayoutInflater()
                            .inflate(R.layout.list_item_extension, parent, false);
                }
                    Resources res = getActivity().getResources();
                    TextView titleView = (TextView) convertView.findViewById(android.R.id.text1);
                    TextView descriptionView = (TextView) convertView
                            .findViewById(android.R.id.text2);
                    ImageView iconView = (ImageView) convertView.findViewById(android.R.id.icon1);
                    CheckBox transparentBox = (CheckBox) convertView.findViewById(R.id.transparent_button);         
                    
                    final ListAdapterItem item = (ListAdapterItem)getItem(position);
                    if(item == null) return null;
                    
                    String title = res.getString(item.title);
                    String description = res.getString(item.description);
                    Drawable draw = res.getDrawable(item.image);
                    
                    iconView.setImageDrawable(draw);
                    titleView.setText(title);
                    descriptionView.setVisibility(
                            TextUtils.isEmpty(description) ? View.GONE : View.VISIBLE);
                    descriptionView.setText(description);
                    transparentBox.setVisibility(item.hasTransparentButton ? View.VISIBLE : View.GONE);
                    transparentBox.setChecked(item.transparent);
                    transparentBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
                        @Override
                        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                            item.transparent = isChecked;
                        }
                    });
                    return convertView;
                }
            }
            return null;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position,
            long id) {
        if (id == -1) {
            showAddExtensionMenu(view.findViewById(R.id.add_statusbar_toggle));
        }
    }
    
    public ListAdapterItem findByTitle(int title) {
        for(int i=0;i<ITEMS.length;i++) {
            if(ITEMS[i].title == title)return ITEMS[i];
        }
        return null;
    }
    
    public static class ListAdapterItem {
        public final int description, title;
        public final boolean hasTransparentButton;
        public final String saveToSetting, saveTransparentToSetting;
        public final int image;
        
        private boolean transparent;
        
        public ListAdapterItem(int title, int des, String save, int image) {
            this(title, des, save, image, false, null);
        }
        
        public ListAdapterItem(int titl, int des, String save, int ima, boolean button, String saveT) {
            description = des;
            title = titl;
            saveToSetting = save;
            image = ima;
            hasTransparentButton = button;
            saveTransparentToSetting = saveT;
        }
    }
}
