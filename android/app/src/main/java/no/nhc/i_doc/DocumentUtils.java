package no.nhc.i_doc;

import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * This class contains document related utilities which should not be
 * in the Document class. The Document class itself should only
 * contain pure data. All other functions can go here.
 */
public class DocumentUtils {
    static final String TAG = "DocumentUtils";

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

    /**
     * Get a full JSON representation of a document
     */
    public static JSONObject documentToJSON(Document d) {
        if (d == null) return null;
        JSONObject obj = new JSONObject();

        try {
            obj.put("uri", d.getUri().toString());
            obj.put("title", d.getTitle());
            obj.put("timestamp", d.getTimestamp());

            {
                JSONArray files = new JSONArray();
                for (String f : d.getFiles()) {
                    files.put(f);
                }
                obj.put("files", files);
            }

            {
                JSONArray metas = new JSONArray();
                for (Metadata md : d.getMetadata()) {
                    metas.put(metadataToJSON(md));
                }
                obj.put("metadata", metas);
            }
        } catch (JSONException e) {
            Log.e(TAG, "Can't make JSON for document");
            return null;
        }

        return obj;
    }

    /**
     * Encode a Metadata object into a json dictionary
     */
    public static JSONObject metadataToJSON(Metadata md) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", md.getType().getSimpleName());
        } catch (JSONException e) {
            Log.e(TAG, "can't encode type");
            return null;
        }

        for (Enum property : getEditableMetadataProperties(md.getType())) {
            Object value = md.get(property);
            try {
                if (value != null) {
                    obj.put(property.toString(), value);
                }
            } catch (JSONException e) {
                Log.e(TAG, "metadataToJSON: can't get property " + property.toString());
            }
        }
        return obj;
    }

    /**
     * Create a new Metadata JSON object
     */
    public static JSONObject createJSONMetadata(java.lang.Class type) {
        JSONObject obj = new JSONObject();
        try {
            obj.put("type", type.getSimpleName());
            Log.d(TAG, "creating metadata from class: " + type.getName());
        } catch (JSONException e) {
            Log.e(TAG, "can't encode type");
            return null;
        }
        return obj;
    }

    /**
     * Assign a json dictionary to a Metadata object, overwriting the
     * specified properties.
     */
    public static boolean documentAssignJSON(DocumentDB db, Document doc, JSONObject json) {
        try {
            if (json.has("title")) {
                doc.setTitle(json.getString("title"));
            }

            if (json.has("metadata")) {
                JSONArray a = json.getJSONArray("metadata");
                List<Metadata> lmd = new ArrayList<Metadata>();

                for (int i = 0; i < a.length(); ++i) {
                    JSONObject o = a.getJSONObject(i);
                    java.lang.Class mdClass = getMetadataClass(o.getString("type"));
                    Metadata md = db.createMetadata(mdClass);

                    for (java.util.Iterator<String> it = o.keys(); it.hasNext();) {
                        String key = it.next();
                        try {
                            Enum prop = Enum.valueOf(mdClass, key);
                            md.set(prop, o.get(key));
                        } catch (IllegalArgumentException e) {
                            Log.d(TAG, "can't make property out of key " + key);
                        }
                    }

                    lmd.add(md);
                }

                doc.setMetadata(lmd);
            }

            return true;
        } catch (JSONException e) {
            return false;
        }
    }

    /**
     * Get the set of editable properties for the given metadata type
     */
    public static List<Enum> getEditableMetadataProperties(java.lang.Class metadataType) {
        List<Enum> ret = new ArrayList<Enum>();

        for (Enum e : (Enum[])metadataType.getEnumConstants()) {
            ret.add(e);
        }

        return ret;
    }

    /**
     * Create a metadata object from a string type
     */
    public static java.lang.Class getMetadataClass(String type) {
        try {
            Class<?> mdClass = Class.forName("no.nhc.i_doc.Metadata$" + type);
            return mdClass;
        } catch (Exception e) {
            Log.e(TAG, "can't get class from type name: " + type + " exception: " + e.toString());
            return null;
        }

    }

    /**
     * Get the form schema (JSON) of a Metadata object
     * BUG: this way is a workaround for now
     */
    public static JSONArray getEditJSONSchema(DocumentDB db, String type) {
        return getEditJSONSchema(db, db.createMetadata(getMetadataClass(type)));
    }

    /**
     * Get the form schema (JSON) of a Metadata object. The schema is a list describing
     * its properties, and each property is denoted by an object having with the keys:
     * * "name" - display name of the property (BUG: must be translated)
     * * "key" - the key of the property when its value is stored in a json object
     * * "type" - property type: ["text"|"enum"|"multienum"]
     * * "values" - list of possible values for enum and multienm types (BUG: translate)
     */
    public static JSONArray getEditJSONSchema(DocumentDB db, Metadata md) {
        JSONArray ja = new JSONArray();
        for (Enum property : getEditableMetadataProperties(md.getType())) {
            JSONObject props = new JSONObject();
            Metadata.PropertyType pt = md.getPropertyType(property);
            java.lang.Class dataType = pt.getType();
            String type = null;

            try {
                props.put("name", property.toString());
                props.put("key", property.toString());

                if (java.lang.CharSequence.class.isAssignableFrom(dataType)) {
                    type = "text";
                } else if (dataType.getEnclosingClass() == Value.class) {
                    JSONArray va = new JSONArray();

                    for (Value val : db.getValueSet(dataType)) {
                        va.put(val.toString());
                    }

                    type = pt.isList() ? "multienum" : "enum";
                    props.put("values", va);
                }

                props.put("type", type);

                if (type != null) {
                    ja.put(ja.length(), props);
                }
            } catch (JSONException e) {
                Log.e(TAG, "can't add to schema");
            }
        }
        return ja;
    }

    static class ProgressiveEntity implements HttpEntity {
        HttpEntity mOtherEntity;

        public ProgressiveEntity(HttpEntity otherEntity) {
            mOtherEntity = otherEntity;
        }

        @Override
        public void consumeContent() throws IOException {
            mOtherEntity.consumeContent();
        }
        @Override
        public InputStream getContent() throws IOException,
                IllegalStateException {
            return mOtherEntity.getContent();
        }
        @Override
        public Header getContentEncoding() {
            return mOtherEntity.getContentEncoding();
        }
        @Override
        public long getContentLength() {
            return mOtherEntity.getContentLength();
        }
        @Override
        public Header getContentType() {
            return mOtherEntity.getContentType();
        }
        @Override
        public boolean isChunked() {
            return mOtherEntity.isChunked();
        }
        @Override
        public boolean isRepeatable() {
            return mOtherEntity.isRepeatable();
        }
        @Override
        public boolean isStreaming() {
            return mOtherEntity.isStreaming();
        }

        @Override
        public void writeTo(final OutputStream outstream) throws IOException {
            class ProgressiveOutputStream extends FilterOutputStream {
                public ProgressiveOutputStream(OutputStream proxy) {
                    super(proxy);
                }
                public void write(int idx) throws IOException {
                    outstream.write(idx);
                }
                public void write(byte[] bts) throws IOException {
                    outstream.write(bts);
                }
                public void flush() throws IOException {
                    outstream.flush();
                }
                public void close() throws IOException {
                    outstream.close();
                }
                public void write(byte[] bts, int st, int end) throws IOException {
                    // FIXME  Put your progress bar stuff here!
                    outstream.write(bts, st, end);
                }
            }

            mOtherEntity.writeTo(new ProgressiveOutputStream(outstream));
        }

    };

    static class UploadDocumentTask extends AsyncTask<String, Integer, Boolean> {
        protected Boolean doInBackground(String... files) {
            for (String f : files) {
                HttpClient client = AndroidHttpClient.newInstance("I-DOC");
                HttpPost post = new HttpPost("http://www.gjermshus.com:8080/new-doc");
                post.setEntity(new ProgressiveEntity(new FileEntity(new File(f), "image/jpeg")));
                try {
                    HttpResponse response = client.execute(post);
                    HttpEntity entity = response.getEntity();
                } catch (IOException e) {
                    Log.e(TAG, "exception during post " + e.toString());
                    return false;
                } finally {
                    client.getConnectionManager().shutdown();
                }
            }
            return true;
        }

        protected void onPostExecute(Boolean result) {
            if (result) {
                Log.e(TAG, "UploadDocumentTask success");
            } else {
                Log.e(TAG, "UploadDocumentTask failed");
            }
        }

        protected void onProgressUpdate(Integer progress) {
            Log.e(TAG, "UploadDocumentTask " + progress);
        }
    }

    public static void uploadDocument(Document doc) {
        List<String> files = doc.getFiles();
        new UploadDocumentTask().execute(files.toArray(new String[files.size()]));
    }
}
