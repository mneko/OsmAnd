package net.osmand.plus.measurementtool;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import androidx.annotation.NonNull;

import net.osmand.plus.utils.AndroidUtils;
import net.osmand.plus.utils.FileUtils;
import net.osmand.GPXUtilities;
import net.osmand.GPXUtilities.GPXFile;
import net.osmand.GPXUtilities.Metadata;
import net.osmand.GPXUtilities.Track;
import net.osmand.GPXUtilities.TrkSegment;
import net.osmand.plus.OsmandApplication;
import net.osmand.plus.R;
import net.osmand.plus.Version;
import net.osmand.plus.activities.MapActivity;
import net.osmand.util.Algorithms;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.List;

import static net.osmand.IndexConstants.GPX_FILE_EXT;

class SaveGpxRouteAsyncTask extends AsyncTask<Void, Void, Exception> {

    private final WeakReference<MeasurementToolFragment> fragmentRef;
    private ProgressDialog progressDialog;

    private final File outFile;
    private File backupFile;
    private final GPXFile gpxFile;
    private GPXFile savedGpxFile;
    private final boolean simplified;
    private final boolean addToTrack;
    private final boolean showOnMap;

    private final SaveGpxRouteListener saveGpxRouteListener;

    public SaveGpxRouteAsyncTask(MeasurementToolFragment fragment, File outFile, GPXFile gpxFile,
                                 boolean simplified, boolean addToTrack, boolean showOnMap,
                                 SaveGpxRouteListener saveGpxRouteListener) {
        fragmentRef = new WeakReference<>(fragment);
        this.outFile = outFile;
        this.showOnMap = showOnMap;
        this.gpxFile = gpxFile;
        this.simplified = simplified;
        this.addToTrack = addToTrack;
        this.saveGpxRouteListener = saveGpxRouteListener;
    }

    @Override
    protected void onPreExecute() {
        MeasurementToolFragment fragment = fragmentRef.get();
        if (fragment != null && fragment.getContext() != null) {
            fragment.cancelModes();

            Context ctx = fragment.getContext();
            progressDialog = new ProgressDialog(ctx);
            progressDialog.setMessage(ctx.getString(R.string.saving_gpx_tracks));
            progressDialog.show();
        }
    }

    @Override
    protected Exception doInBackground(Void... voids) {
        MeasurementToolFragment fragment = fragmentRef.get();
        if (fragment == null || fragment.getActivity() == null) {
            return null;
        }
        MapActivity mapActivity = (MapActivity) fragment.getActivity();
        OsmandApplication app = mapActivity.getMyApplication();
        MeasurementEditingContext editingContext = fragment.getEditingCtx();
        Exception res = null;

        if (gpxFile == null) {
            String fileName = outFile.getName();
            String trackName = fileName.substring(0, fileName.length() - GPX_FILE_EXT.length());
            GPXFile gpx = generateGpxFile(editingContext, trackName, new GPXFile(Version.getFullVersion(app)));
            res = GPXUtilities.writeGpxFile(outFile, gpx);
            gpx.path = outFile.getAbsolutePath();
            savedGpxFile = gpx;
            if (showOnMap) {
                MeasurementToolFragment.showGpxOnMap(app, gpx, true);
            }
        } else {
            backupFile = FileUtils.backupFile(app, outFile);
            String trackName = Algorithms.getFileNameWithoutExtension(outFile);
            GPXFile gpx = generateGpxFile(editingContext, trackName, gpxFile);
            gpx.metadata = new Metadata(gpxFile.metadata);
            if (!gpx.showCurrentTrack) {
                res = GPXUtilities.writeGpxFile(outFile, gpx);
            }
            savedGpxFile = gpx;
            if (showOnMap) {
                MeasurementToolFragment.showGpxOnMap(app, gpx, false);
            }
        }
        if (res == null) {
            savedGpxFile.addGeneralTrack();
        }

        return res;
    }

    private GPXFile generateGpxFile(@NonNull MeasurementEditingContext editingCtx,
                                    @NonNull String trackName,
                                    @NonNull GPXFile gpx) {
        return generateGpxFile(editingCtx, trackName, gpx, simplified, addToTrack);
    }

    public static GPXFile generateGpxFile(@NonNull MeasurementEditingContext editingCtx,
                                          @NonNull String trackName,
                                          @NonNull GPXFile gpx,
                                          boolean simplified,
                                          boolean addToTrack) {
        List<TrkSegment> before = editingCtx.getBeforeTrkSegmentLine();
        List<TrkSegment> after = editingCtx.getAfterTrkSegmentLine();
        if (simplified) {
            Track track = new Track();
            track.name = trackName;
            gpx.tracks.add(track);
            for (TrkSegment s : before) {
                TrkSegment segment = new TrkSegment();
                segment.points.addAll(s.points);
                track.segments.add(segment);
            }
            for (TrkSegment s : after) {
                TrkSegment segment = new TrkSegment();
                segment.points.addAll(s.points);
                track.segments.add(segment);
            }
        } else {
            GPXFile newGpx = editingCtx.exportGpx(trackName);
            if (newGpx != null) {
                if (addToTrack) {
                    newGpx.tracks.addAll(gpx.tracks);
                    newGpx.routes.addAll(gpx.routes);
                }
                gpx = newGpx;
            }
        }
        return gpx;
    }

    @Override
    protected void onPostExecute(Exception warning) {
        MeasurementToolFragment fragment = fragmentRef.get();
        if (fragment != null && progressDialog != null && AndroidUtils.isActivityNotDestroyed(fragment.getActivity())) {
            progressDialog.dismiss();
        }
        if (saveGpxRouteListener != null) {
            saveGpxRouteListener.gpxSavingFinished(warning, savedGpxFile, backupFile);
        }
    }

    public interface SaveGpxRouteListener {

        void gpxSavingFinished(Exception warning, GPXFile savedGpxFile, File backupFile);
    }
}