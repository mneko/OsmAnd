package net.osmand.plus.mapcontextmenu.editors;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.plus.utils.ColorUtilities;
import net.osmand.plus.myplaces.FavouritesDbHelper;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.utils.UiUtilities;
import net.osmand.plus.activities.MapActivity;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class SelectCategoryDialogFragment extends DialogFragment {

	public static final String TAG = SelectCategoryDialogFragment.class.getSimpleName();

	public interface CategorySelectionListener{

		void onCategorySelected(String category, int color);
	}

	private static final String KEY_CTX_SEL_CAT_EDITOR_TAG = "key_ctx_sel_cat_editor_tag";

	private String editorTag;
	private CategorySelectionListener selectionListener;
	private GPXFile gpxFile;
	private Map<String, Integer> gpxCategories;

	public void setGpxFile(GPXFile gpxFile) {
		this.gpxFile = gpxFile;
	}

	public GPXFile getGpxFile() {
		return gpxFile;
	}

	public Map<String, Integer> getGpxCategories() {
		return gpxCategories;
	}

	public void setGpxCategories(Map<String, Integer> gpxCategories) {
		this.gpxCategories = gpxCategories;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(Bundle savedInstanceState) {

		if (savedInstanceState != null) {
			restoreState(savedInstanceState);
		} else if (getArguments() != null) {
			restoreState(getArguments());
		}

		final FragmentActivity activity = requireActivity();
		final OsmandApplication app = (OsmandApplication) activity.getApplication();
		boolean nightMode = app.getDaynightHelper().isNightModeForMapControls();
		AlertDialog.Builder builder = new AlertDialog.Builder(UiUtilities.getThemedContext(activity, nightMode));
		builder.setTitle(R.string.favorite_category_select);
		final View v = UiUtilities.getInflater(activity, nightMode).inflate(R.layout.favorite_categories_dialog, null, false);
		LinearLayout ll = (LinearLayout) v.findViewById(R.id.list_container);

		final FavouritesDbHelper helper = app.getFavorites();
		if (gpxFile != null) {
			if (gpxCategories != null) {
				for (Map.Entry<String, Integer> e : gpxCategories.entrySet()) {
					String categoryName = e.getKey();
					ll.addView(createCategoryItem(activity, nightMode, categoryName, e.getValue(), false));
				}
			}
		} else {
			List<FavouritesDbHelper.FavoriteGroup> gs = helper.getFavoriteGroups();
			for (final FavouritesDbHelper.FavoriteGroup category : gs) {
				ll.addView(createCategoryItem(activity, nightMode, category.getDisplayName(getContext()),
						category.getColor(), !category.isVisible()));
			}
		}

		View itemView = UiUtilities.getInflater(activity, nightMode).inflate(R.layout.favorite_category_dialog_item, null);
		Button button = (Button)itemView.findViewById(R.id.button);
		button.setCompoundDrawablesWithIntrinsicBounds(getIcon(activity, R.drawable.ic_zoom_in), null, null, null);
		button.setCompoundDrawablePadding(AndroidUtils.dpToPx(activity,15f));
		button.setText(activity.getResources().getText(R.string.favorite_category_add_new));
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
				Set<String> categories = gpxCategories != null ? gpxCategories.keySet() : null;
				EditCategoryDialogFragment dialogFragment =
						EditCategoryDialogFragment.createInstance(editorTag, categories,gpxFile != null);
				dialogFragment.show(activity.getSupportFragmentManager(), EditCategoryDialogFragment.TAG);
				dialogFragment.setSelectionListener(selectionListener);
			}
		});
		ll.addView(itemView);

		builder.setView(v);
		builder.setNegativeButton(R.string.shared_string_cancel, null);

		return builder.create();
	}

	private View createCategoryItem(@NonNull final Activity activity, boolean nightMode, final String categoryName, final int categoryColor, boolean isHidden) {
		View itemView = UiUtilities.getInflater(activity, nightMode).inflate(R.layout.favorite_category_dialog_item, null);
		Button button = (Button)itemView.findViewById(R.id.button);
		if(isHidden){
			button.setCompoundDrawablesWithIntrinsicBounds(getIcon(activity, R.drawable.ic_action_hide), null, null, null);
		} else {
			if (categoryColor != 0) {
				button.setCompoundDrawablesWithIntrinsicBounds(
						getIcon(activity, R.drawable.ic_action_folder, categoryColor), null, null, null);
			} else {
				button.setCompoundDrawablesWithIntrinsicBounds(
						getIcon(activity, R.drawable.ic_action_folder, ContextCompat.getColor(activity,
								gpxFile != null ? R.color.gpx_color_point : R.color.color_favorite)), null, null, null);
			}
		}
		button.setCompoundDrawablePadding(AndroidUtils.dpToPx(activity,15f));
		String name = categoryName.length() == 0 ? getString(R.string.shared_string_favorites) : categoryName;
		button.setText(name);
		button.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				FragmentActivity a = getActivity();
				if (a instanceof MapActivity) {
					PointEditor pointEditor = ((MapActivity) a).getContextMenu().getPointEditor(editorTag);
					if (pointEditor != null) {
						pointEditor.setCategory(categoryName, categoryColor);
					}
					if (selectionListener != null) {
						selectionListener.onCategorySelected(categoryName, categoryColor);
					}
				}
				dismiss();
			}
		});
		return itemView;
	}

	public static SelectCategoryDialogFragment createInstance(String editorTag) {
		SelectCategoryDialogFragment fragment = new SelectCategoryDialogFragment();
		Bundle bundle = new Bundle();
		bundle.putString(KEY_CTX_SEL_CAT_EDITOR_TAG, editorTag);
		fragment.setArguments(bundle);
		return fragment;
	}

	public void setSelectionListener(CategorySelectionListener selectionListener) {
		this.selectionListener = selectionListener;
	}

	public void saveState(Bundle bundle) {
		bundle.putString(KEY_CTX_SEL_CAT_EDITOR_TAG, editorTag);
	}

	public void restoreState(Bundle bundle) {
		editorTag = bundle.getString(KEY_CTX_SEL_CAT_EDITOR_TAG);
	}

	private static Drawable getIcon(final Activity activity, int iconId) {
		OsmandApplication app = (OsmandApplication)activity.getApplication();
		UiUtilities iconsCache = app.getUIUtilities();
		boolean light = app.getSettings().isLightContent();
		return iconsCache.getIcon(iconId, ColorUtilities.getDefaultIconColorId(!light));
	}

	private static Drawable getIcon(final Activity activity, int resId, int color) {
		Drawable d = AppCompatResources.getDrawable(activity, resId);
		if (d != null) {
			d = DrawableCompat.wrap(d).mutate();
			d.clearColorFilter();
			d.setColorFilter(color, PorterDuff.Mode.SRC_IN);
		}
		return d;
	}
}
