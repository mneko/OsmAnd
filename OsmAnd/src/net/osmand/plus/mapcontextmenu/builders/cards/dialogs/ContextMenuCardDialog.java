package net.osmand.plus.mapcontextmenu.builders.cards.dialogs;

import android.os.Bundle;
import android.view.Menu;
import android.view.View;

import androidx.annotation.NonNull;

import net.osmand.plus.OsmandApplication;
import net.osmand.plus.plugins.OsmandPlugin;
import net.osmand.plus.activities.MapActivity;
import net.osmand.plus.helpers.AndroidUiHelper;
import net.osmand.plus.plugins.mapillary.MapillaryImageDialog;
import net.osmand.plus.plugins.mapillary.MapillaryPlugin;
import net.osmand.plus.settings.backend.OsmandSettings;
import net.osmand.plus.views.OsmandMapTileView;

public abstract class ContextMenuCardDialog {

	private final MapActivity mapActivity;

	private static final String KEY_CARD_DIALOG_TYPE = "key_card_dialog_type";
	private static final String KEY_CARD_DIALOG_TITLE = "key_card_dialog_title";
	private static final String KEY_CARD_DIALOG_DESCRIPTION = "key_card_dialog_description";

	private final CardDialogType type;
	protected String title;
	protected String description;

	private int prevMapPosition = OsmandSettings.CENTER_CONSTANT;
	private final boolean portrait;

	public enum CardDialogType {
		REGULAR,
		MAPILLARY
	}

	protected ContextMenuCardDialog(MapActivity mapActivity, @NonNull CardDialogType type) {
		this.mapActivity = mapActivity;
		this.type = type;
		this.portrait = AndroidUiHelper.isOrientationPortrait(mapActivity);
	}

	public MapActivity getMapActivity() {
		return mapActivity;
	}

	public boolean isPortrait() {
		return portrait;
	}

	public CardDialogType getType() {
		return type;
	}

	public String getTitle() {
		return title;
	}

	public String getDescription() {
		return description;
	}

	protected boolean haveMenuItems() {
		return false;
	}

	protected void createMenuItems(Menu menu) {
	}

	public void saveMenu(Bundle bundle) {
		bundle.putString(KEY_CARD_DIALOG_TYPE, type.name());
		if (title != null) {
			bundle.putString(KEY_CARD_DIALOG_TITLE, title);
		}
		if (description != null) {
			bundle.putString(KEY_CARD_DIALOG_DESCRIPTION, description);
		}
	}

	protected void restoreFields(Bundle bundle) {
		this.title = bundle.getString(KEY_CARD_DIALOG_TITLE);
		this.description = bundle.getString(KEY_CARD_DIALOG_TITLE);
	}

	static ContextMenuCardDialog restoreMenu(@NonNull Bundle bundle, @NonNull MapActivity mapActivity) {

		try {
			CardDialogType type = CardDialogType.valueOf(bundle.getString(KEY_CARD_DIALOG_TYPE));
			ContextMenuCardDialog dialog = null;
			switch (type) {
				case MAPILLARY:
					dialog = new MapillaryImageDialog(mapActivity, bundle);
					break;
				case REGULAR:
					break;
			}
			return dialog;
		} catch (Exception e) {
			return null;
		}
	}

	public void onResume() {
		shiftMapPosition();
		updateLayers(true);
	}

	public void onPause() {
		restoreMapPosition();
		updateLayers(false);
	}

	private void shiftMapPosition() {
		OsmandMapTileView mapView = mapActivity.getMapView();
		if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			if (mapView.getMapPosition() != OsmandSettings.MIDDLE_BOTTOM_CONSTANT) {
				prevMapPosition = mapView.getMapPosition();
				mapView.setMapPosition(OsmandSettings.MIDDLE_BOTTOM_CONSTANT);
			}
		} else {
			mapView.setMapPositionX(1);
		}
	}

	private void restoreMapPosition() {
		if (AndroidUiHelper.isOrientationPortrait(mapActivity)) {
			mapActivity.getMapView().setMapPosition(prevMapPosition);
		} else {
			mapActivity.getMapView().setMapPositionX(0);
		}
	}

	public abstract View getContentView();

	private void updateLayers(boolean activate) {
		MapActivity mapActivity = getMapActivity();
		OsmandApplication app = mapActivity.getMyApplication();
		switch (type) {
			case MAPILLARY: {
				boolean showMapillary = app.getSettings().SHOW_MAPILLARY.get();
				if (!showMapillary) {
					MapillaryPlugin mapillaryPlugin = OsmandPlugin.getPlugin(MapillaryPlugin.class);
					if (mapillaryPlugin != null) {
						mapillaryPlugin.updateLayers(mapActivity, mapActivity, activate);
					}
				}
				break;
			}
			case REGULAR:
				break;
		}
	}
}
