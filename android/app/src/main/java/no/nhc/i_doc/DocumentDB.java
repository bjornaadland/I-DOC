package no.nhc.i_doc;

import android.content.Context;
import android.net.Uri;


/**
 *  DocumentDB concerns the set of stored documents,
 *  and manages adding and removing of documents from the database.
 */
public abstract class DocumentDB
{
    private static volatile DocumentDB inst;

    /**
     *  Listener class for a List
     */
    public static interface Listener {
        void changed();
    }

    /**
     *  DocList - a dynamic list of documents
     */
    public static interface List<T> {
        int getCount();
        T getObject(int position);

        // NOTE: DocumentDB will hold a weak reference to the listener object.
        void addListener(Listener listener);
    }

    protected DocumentDB() {}

    public static DocumentDB get(android.content.Context context) {
        if (inst == null) {
            inst = new CouchDocumentDB(context);
        }

        return inst;
    }

    /**
     *  Get the full list of stored documents.
     */
    abstract List<Document> getDocumentList();

    /**
     *  Create an empty document
     */
    abstract Document createDocument();

    /**
     *  Get a document from a Uri
     */
    abstract Document getDocument(Uri uri);

    /**
     *  Save a new or update an already existing Document
     */
    abstract void saveDocument(Document d);

    /**
     * Delete this document and the associated files.
     */
    abstract void deleteDocument(Document d);

    /**
     * Create a metadata object to be attached to a document.
     * in order to create a new, empty object, set id to null.
     * In order to load an object with a known id, pass that id.
     */
    abstract Metadata createMetadata(java.lang.Class type, Object id);

    /**
     * Return a list of Metadata objects, keyed by the given
     * metadata property.
     */
    abstract List<Metadata.PropertyMap> mapMetadata(Enum key);

    public static class SyncEvent {
        public final static int STARTED = 0;
        public final static int STOPPED = 1;
        public final static int PROGRESS = 2;

        int mProgress;
        int mMax;
        int mEvent;
        SyncEvent(int event, int progress, int max) {
            mProgress = progress;
            mMax = max;
            mEvent = event;
        }

        int getEvent() {
            return mEvent;
        }
        int getMax() {
            return mMax;
        }
        int getProgress() {
            return mProgress;
        }

    }

    /**
     *  Listener class for a sync status.
     */
    public static interface SyncListener {
        void onEvent(SyncEvent event);
    }

    /**
     * Sync all data upstream
     */
    abstract void sync(SyncListener listener);

    /**
     * Get the Value set (enum-like) that can be used
     * as constants for Value based Metadata properties
     */
    abstract java.util.List<Value> getValueSet(java.lang.Class valueClass);
}
