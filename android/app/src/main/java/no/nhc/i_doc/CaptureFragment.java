package no.nhc.i_doc;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CaptureFragment extends Fragment {
    static final String TAG = "CaptureFragment";

    static final int REQUEST_IMAGE_CAPTURE = 1;
    static final int REQUEST_VIDEO_CAPTURE = 2;


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

        Button button = (Button) fragmentView.findViewById(R.id.button_take_picture);
        button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dispatchTakePictureIntent();
                }
            });

        button = (Button) fragmentView.findViewById(R.id.button_record_video);
        button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dispatchRecordVideoIntent();
                }
            });


        button = (Button) fragmentView.findViewById(R.id.button_record_sound);
        button.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    dispatchRecordSoundIntent();
                }
            });

        return fragmentView;
    }

    static String mCurrentPhotoPath;
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
    
    public void dispatchRecordSoundIntent() {
        /* test creating an empty document */
        DocumentDB db = DocumentDB.get(getActivity());
        Document doc = db.createDocument();
        db.saveDocument(doc);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != Activity.RESULT_OK) {
            return;
        }
        switch (requestCode) {
        case REQUEST_IMAGE_CAPTURE: {
            DocumentDB db = DocumentDB.get(getActivity());
            Document doc = db.createDocument();
            doc.addFile(mCurrentPhotoPath);
            db.saveDocument(doc);
/*
            Bundle extras = data.getExtras();
            Bitmap imageBitmap = (Bitmap) extras.get("data");
            mImageView.setImageBitmap(imageBitmap);
*/
            break;
        }
        case REQUEST_VIDEO_CAPTURE: {
            DocumentDB db = DocumentDB.get(getActivity());
            Document doc = db.createDocument();
            doc.addFile(mCurrentVideoPath);
            db.saveDocument(doc);
/*
            Uri videoUri = data.getData();
            mVideoView.setVideoURI(videoUri);
            mVideoView.start();
*/
            break;
        }}
    }


}
