package no.nhc.i_doc;

import android.widget.ImageView;
import android.webkit.MimeTypeMap;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.text.DateFormat;
import java.util.Date;


/**
 * This class contains document related utilities which should not be
 * in the Document class. The Document class itself should only
 * contain pure data. All other functions can go here.
 */
public class DocumentUtils {

    public static enum MediaType {
        MEDIA_TYPE_IMAGE,
        MEDIA_TYPE_VIDEO,
        MEDIA_TYPE_AUDIO,
        MEDIA_TYPE_UNKNOWN
    }

    static String GetFileUri(Document document) {
        java.util.List<String> files = document.getFiles();
        if (files.size() > 0) {
            return files.get(0);
        } else {
            return "";
        }
    }

    static String GetSourceUri(ImageView imageView) {
        return (String)imageView.getTag(R.id.TAG_SOURCE_URI);
    }

    static MediaType GetMediaType(String fileUri) {
        String extension = MimeTypeMap.getFileExtensionFromUrl(fileUri);
        String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        if (type == null) {
            return MediaType.MEDIA_TYPE_UNKNOWN;
        } else if (type.startsWith("image/")) {
            return MediaType.MEDIA_TYPE_IMAGE;
        } else if (type.startsWith("video/")) {
             return MediaType.MEDIA_TYPE_VIDEO;
        } else if (type.startsWith("audio/")) {
             return MediaType.MEDIA_TYPE_AUDIO;
        } else {
            return MediaType.MEDIA_TYPE_UNKNOWN;
        }
    }

    /**
     * Display the document's image in the view.
     * This takes care of thumbnailing and everything related.
     */
    static void DisplayImage(Document document, ImageView imageView) {
        String fileUri = GetFileUri(document);
        imageView.setTag(R.id.TAG_SOURCE_URI, fileUri);
        switch (GetMediaType(fileUri)) {
            case MEDIA_TYPE_IMAGE:
                imageView.setTag(R.id.TAG_IMAGE_URI, fileUri);
                ImageLoader.getInstance().displayImage(fileUri, imageView);
                break;
            case MEDIA_TYPE_VIDEO:
                // Load the thumbnail
                imageView.setTag(R.id.TAG_IMAGE_URI, fileUri + ".jpg");
                ImageLoader.getInstance().displayImage(fileUri + ".jpg", imageView);
                break;
            case MEDIA_TYPE_AUDIO:
                imageView.setTag(R.id.TAG_IMAGE_URI, "");
                // TODO: Display audio icon
                break;
            case MEDIA_TYPE_UNKNOWN:
                imageView.setTag(R.id.TAG_IMAGE_URI, "");
                // TODO: Display generic icon
                break;
        }
    }

    /**
     * Set a default title on a document.
     */
    static void SetDefaultTitle(Document document) {
        document.setTitle(DateFormat.getDateInstance().format(new Date()));
    }
}
