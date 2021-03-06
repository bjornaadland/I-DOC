package no.nhc.i_doc;

import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.ImageView;

import com.nostra13.universalimageloader.core.ImageLoader;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;



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
        } else {
            return MediaType.MEDIA_TYPE_UNKNOWN;
        }
    }

    /**
     * Display the document's image in the view.
     * This takes care of thumbnailing and everything related.
     */
    static void DisplayImage(Document document, ImageView imageView, ImageView videoPlayView) {
        String fileUri = GetFileUri(document);
        imageView.setTag(R.id.TAG_SOURCE_URI, fileUri);
        switch (GetMediaType(fileUri)) {
            case MEDIA_TYPE_IMAGE:
                if (fileUri.equals(imageView.getTag(R.id.TAG_IMAGE_URI)) == false) {
                    imageView.setImageDrawable(null);
                    imageView.setTag(R.id.TAG_IMAGE_URI, fileUri);
                    ImageLoader.getInstance().displayImage(fileUri, imageView);
                    if (videoPlayView != null) {
                        videoPlayView.setVisibility(View.INVISIBLE);
                    }
                }
                break;
            case MEDIA_TYPE_VIDEO:
                // Display the thumbnail
                String thumbUri = fileUri + ".jpg";
                if (thumbUri.equals(imageView.getTag(R.id.TAG_IMAGE_URI)) == false) {
                    imageView.setImageDrawable(null);
                    imageView.setTag(R.id.TAG_IMAGE_URI, thumbUri);
                    ImageLoader.getInstance().displayImage(thumbUri, imageView);
                    if (videoPlayView != null) {
                        videoPlayView.setVisibility(View.VISIBLE);
                    }
                }
                break;
            case MEDIA_TYPE_UNKNOWN:
                imageView.setImageDrawable(null);
                imageView.setTag(R.id.TAG_IMAGE_URI, "");
                if (videoPlayView != null) {
                    videoPlayView.setVisibility(View.INVISIBLE);
                }
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
            if (md.getId() != null) {
                obj.put("id", md.getId());
            }
        } catch (JSONException e) {
            Log.e(TAG, "can't encode type");
            return null;
        }

        for (Enum property : getEditableMetadataProperties(md.getType())) {
            Metadata.PropertyType pt = md.getPropertyType(property);
            Object value = md.get(property);
            try {
                if (pt.getType().isEnum()) {
                    obj.put(property.toString(),
                            value != null ? metadataToJSON((Metadata)value) : null);
                } else if (value != null) {
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
                    lmd.add(JSONToMetadata(db, a.getJSONObject(i)));
                }

                doc.setMetadata(lmd);
            }

            return true;
        } catch (JSONException e) {
            Log.e(TAG, "error assigning json to document: " + e.toString());
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

    private static Metadata JSONToMetadata(DocumentDB db, JSONObject json) throws JSONException {
        java.lang.Class mdClass = getMetadataClass(json.getString("type"));
        Metadata md = db.createMetadata(mdClass, null);

        if (json.has("id")) {
            md.setId(json.getString("id"));
        }

        Log.d(TAG, "JSONToMetadata: " + json.toString());

        for (java.util.Iterator<String> it = json.keys(); it.hasNext();) {
            String key = it.next();
            try {
                Enum prop = Enum.valueOf(mdClass, key);
                Metadata.PropertyType pt = md.getPropertyType(prop);
                Object value = null;

                if (pt.getType().isEnum()) {
                    JSONObject childObject = json.getJSONObject(key);
                    if (childObject != null) {
                        Log.d(TAG, "setting enum childObject from json: " + (childObject != null ? childObject.toString() : "null"));
                        value = JSONToMetadata(db, childObject);
                    }
                } else {
                    value = json.get(key);
                }
                md.set(prop, value);
            } catch (IllegalArgumentException e) {
                Log.d(TAG, "can't make property out of key " + key);
            }
        }

        return md;
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
        return getEditJSONSchema(db, db.createMetadata(getMetadataClass(type), null));
    }

    /**
     * Get the form schema (JSON) of a Metadata object. The schema is a list describing
     * its properties, and each property is denoted by an object having with the keys:
     * * "name" - display name of the property (BUG: must be translated)
     * * "key" - the key of the property when its value is stored in a json object
     * * "type" - property type: ["text"|"name"|"date"|"enum"|"multienum"|"object"]
     * * "values" - list of possible values for enum and multienm types (BUG: translate)
     * * "schema" - subschema for "object" values
     * * "searchable" - metadata type that is searchable from this field
     * * "contextual" - set to true if the value of the property is descriptive of the property type
     */
    public static JSONArray getEditJSONSchema(DocumentDB db, Metadata md) {
        JSONArray ja = new JSONArray();
        for (Enum property : getEditableMetadataProperties(md.getType())) {
            JSONObject props = new JSONObject();
            Metadata.PropertyType pt = md.getPropertyType(property);
            java.lang.Class dataType = pt.getType();
            String type = null;
            boolean contextual = false;

            try {
                props.put("name", property.toString());
                props.put("key", property.toString());

                if (java.lang.CharSequence.class.isAssignableFrom(dataType)) {
                    if (property == Metadata.Person.FamilyName ||
                        property == Metadata.Person.GivenName) {
                        props.put("searchable", "Person");
                        contextual = true;
                        type = "name";
                    } else if (property == Metadata.Person.DateOfBirth) {
                        type = "date";
                    } else if (property == Metadata.ProtectedObject.Name ||
                               property == Metadata.Context.Name ||
                               property == Metadata.OrgUnit.Name) {
                        contextual = true;
                        type = "text";
                    } else {
                        type = "text";
                    }
                } else if (dataType.getEnclosingClass() == Metadata.class) {
                    /* "recursive" type */
                    type = "object";
                    props.put("schema", getEditJSONSchema(db, db.createMetadata(dataType, null)));
                    props.put("defaultType", dataType.getSimpleName());
                    contextual = true;
                } else if (dataType.getEnclosingClass() == Value.class) {
                    JSONArray va = new JSONArray();

                    for (Value val : db.getValueSet(dataType)) {
                        va.put(val.toString());
                    }

                    type = pt.isList() ? "multienum" : "enum";
                    props.put("values", va);
                }

                if (contextual) props.put("contextual", new Boolean(true));
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

    /**
     *  Get suggestion data according to the "searchable" property in the schema
     */
    public static JSONObject getSuggestionData(DocumentDB db, String searchableType, SuggestionAdapter.Suggestion sug) {
        java.lang.Class type = getMetadataClass(searchableType);
        return metadataToJSON(db.createMetadata(type, sug.mId));
    }

}
