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
    /**
     * Display the document's image in the view.
     * This takes care of thumbnailing and everything related.
     */
    static void DisplayImage(Document document, ImageView imageView) {
        java.util.List<String> files = document.getFiles();
        if (files.size() > 0) {
            String fileUri = files.get(0);
            String extension = MimeTypeMap.getFileExtensionFromUrl(fileUri);
            String type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
            if (type.startsWith("image/")) {
                ImageLoader.getInstance().displayImage(fileUri, imageView);
            } else if (type.startsWith("video/")) {
                // Load the thumbnail
                ImageLoader.getInstance().displayImage(fileUri + ".jpg", imageView);
            } else if (type.startsWith("audio/")) {
                // TODO: Display audio icon
            } else {
                // TODO: Display generic icon
            }
        } else {
            // TODO: Display generic icon
        }
    }

    /**
     * Set a default title on a document.
     */
    static void SetDefaultTitle(Document document) {
        document.setTitle(DateFormat.getDateInstance().format(new Date()));
    }
}
