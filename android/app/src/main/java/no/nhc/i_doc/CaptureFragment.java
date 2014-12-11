package no.nhc.i_doc;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CaptureFragment extends Fragment {
    static final String TAG = "CaptureFragment";

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_VIDEO_CAPTURE = 2;
    static final int REQUEST_IMAGE_IMPORT = 3;
    static final int REQUEST_VIDEO_IMPORT = 4;

    public CaptureFragment() {
        setRetainInstance(true);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View fragmentView = inflater.inflate(R.layout.fragment_capture, container, false);

        TextView text = (TextView) fragmentView.findViewById(R.id.text_take_picture);
        text.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dispatchTakePictureIntent();
                }
            });

        text = (TextView) fragmentView.findViewById(R.id.text_record_video);
        text.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dispatchRecordVideoIntent();
                }
            });

        text = (TextView) fragmentView.findViewById(R.id.text_import_media);
        text.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    handleImportMedia();
                }
            });

        return fragmentView;
    }

    String mCurrentPhotoPath;
    String mCurrentVideoPath;

    private File createImageFile()  {
        try {
            // Create an image file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String imageFileName = "JPEG_" + timeStamp + "_";
            //File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
//            File storageDir = getFilesDir();

            Log.d(TAG, "externalStoragePublicDirectory: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES));
            Log.d(TAG, "getFilesDir: " + getActivity().getFilesDir());
            Log.d(TAG, "getExternalFilesDir: " + getActivity().getExternalFilesDir(Environment.DIRECTORY_PICTURES));
            File image = File.createTempFile(
                    imageFileName,  /* prefix */
                    ".jpg",         /* suffix */
                    storageDir      /* directory */
            );

            // Save a file: path for use with ACTION_VIEW intents
            mCurrentPhotoPath = "file://" + image.getAbsolutePath();
            Log.d(TAG, "path: " + mCurrentPhotoPath);
            return image;
        }
        catch (IOException e) {
            return null;
        }
    }

    private File createVideoFile()  {
        try {
            // Create an video file name
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
            String videoFileName = "MP4_" + timeStamp + "_";
            //File storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES);
            File storageDir = getActivity().getExternalFilesDir(Environment.DIRECTORY_MOVIES);
//            File storageDir = getFilesDir();

            Log.d(TAG, "externalStoragePublicDirectory: " + Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES));
            Log.d(TAG, "getFilesDir: " + getActivity().getFilesDir());
            Log.d(TAG, "getExternalFilesDir: " + getActivity().getExternalFilesDir(Environment.DIRECTORY_MOVIES));
            File video = File.createTempFile(
                    videoFileName,  /* prefix */
                    ".mp4",         /* suffix */
                    storageDir      /* directory */
            );

            // Save a file: path for use with ACTION_VIEW intents
            mCurrentVideoPath = "file://" + video.getAbsolutePath();
            Log.d(TAG, "path: " + mCurrentVideoPath);
            return video;
        }
        catch (IOException e) {
            return null;
        }
    }


    public void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(createImageFile()));
            startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
        }
    }
        
    public void dispatchRecordVideoIntent() {
        Intent takeVideoIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (takeVideoIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            takeVideoIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(createVideoFile()));
            startActivityForResult(takeVideoIntent, REQUEST_VIDEO_CAPTURE);
        }
    }
    
    public void handleImportMedia() {
        final CharSequence[] items = {
                "Picture", "Video"
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select media type");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int item) {
                Intent i=  new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                if (item == 0) {
                    i.setType("image/*");
                    startActivityForResult(i, REQUEST_IMAGE_IMPORT);
                } else {
                    i.setType("video/*");
                    startActivityForResult(i, REQUEST_VIDEO_IMPORT);
                }
            }
        });
        AlertDialog alert = builder.create();
        alert.show();
    }

    public void fileCopy(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        inChannel.transferTo(0, inChannel.size(), outChannel);
        inStream.close();
        outStream.close();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        

        // Filter requestCodes, common code below.
        switch (requestCode) {
        case REQUEST_IMAGE_CAPTURE:
        case REQUEST_VIDEO_CAPTURE:
            break;
        case REQUEST_IMAGE_IMPORT:
        case REQUEST_VIDEO_IMPORT: {
            Uri selectedImage = data.getData();
            String[] filePath = { MediaStore.Images.Media.DATA };
            Cursor c = getActivity().getContentResolver().query(selectedImage, filePath, null, null, null);
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePath[0]);
            String mediaPath = c.getString(columnIndex);
            c.close();
            if (mediaPath == null) {
                return;
            }
            if (requestCode == REQUEST_IMAGE_IMPORT) {
                createImageFile();
                try {
                    fileCopy(new File(mediaPath), new File(mCurrentPhotoPath.substring(7)));
                } catch (IOException e) {
                    return;
                }
            } else if (requestCode == REQUEST_VIDEO_IMPORT) {
                createVideoFile();
                try {
                    fileCopy(new File(mediaPath), new File(mCurrentVideoPath.substring(7)));
                } catch (IOException e) {
                    return;
                }
            }
            break;
        }
        default:
            return;
        }

        DocumentDB db = DocumentDB.get(getActivity());
        Document doc = db.createDocument();
        DocumentUtils.SetDefaultTitle(doc);

        switch (requestCode) {
        case REQUEST_IMAGE_CAPTURE:
        case REQUEST_IMAGE_IMPORT: {
            doc.addFile(mCurrentPhotoPath);
            break;
        }
        case REQUEST_VIDEO_CAPTURE:
        case REQUEST_VIDEO_IMPORT: {
            doc.addFile(mCurrentVideoPath);

            // Create and store a thumbnail
            try {
                Bitmap videoThumbnail = ThumbnailUtils.createVideoThumbnail(mCurrentVideoPath.substring(7), MediaStore.Video.Thumbnails.MINI_KIND);
                OutputStream oStream = new FileOutputStream(mCurrentVideoPath.substring(7) + ".jpg");
                videoThumbnail.compress(Bitmap.CompressFormat.JPEG, 100, oStream);
            } catch (Exception e) {
                // Something went wrong..
            }
            break;
        }}
        db.saveDocument(doc);
    }
}
