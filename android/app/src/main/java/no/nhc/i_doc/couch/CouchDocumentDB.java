package no.nhc.i_doc;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Database;
import com.couchbase.lite.LiveQuery;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.SavedRevision;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.View;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

public class CouchDocumentDB extends DocumentDB
{
    public static final String TAG = "CouchDocumentDB";
    public static final String UriScheme = "evidence";

    public static Database sSingletonDatabase;

    private Manager mManager;
    private Database mDatabase;

    WeakHashMap<CouchDocumentList, Integer> mDocLists =
        new WeakHashMap<CouchDocumentList, Integer>();

    /**
     *  Couchbase representation of a Document
     */
    private static final class CouchDocument implements Document
    {
        private Object mId;
        UnsavedRevision mRev;

        public CouchDocument(Object id)
        {
            mId = id;
        }

        public void save()
        {
            UnsavedRevision rev = getRevision();
            mRev = null;

            try {
                SavedRevision sr = rev.save();
                mId = sr.getDocument().getId();
            } catch (CouchbaseLiteException e) {
                throw new RuntimeException("problem saving document");
            }
        }

        public boolean delete() {
            com.couchbase.lite.Document doc = sSingletonDatabase.getDocument((String)mId);
            try {
                return doc.delete();
            } catch (CouchbaseLiteException e) {
                return false;
            }
        }

        /**
         *  Creates an UnsavedRevision that contains the document properties
         *  both for reading and manipulation.
         */
        public UnsavedRevision getRevision()
        {
            if (mRev == null) {
                if (mId == null) {
                    com.couchbase.lite.Document doc = sSingletonDatabase.createDocument();
                    mRev = doc.createRevision();

                    Map<String, Object> props = new HashMap<String, Object>();

                    props.put("title", "");
                    props.put("timestamp", System.currentTimeMillis() / 1000);
                    props.put("files", new ArrayList<Object>());

                    mRev.setProperties(props);
                } else {
                    com.couchbase.lite.Document doc = sSingletonDatabase.getExistingDocument((String)mId);
                    // We do not tolerate deleted documents.
                    if (doc == null) return null;
                    mRev = doc.createRevision();
                }
            }

            return mRev;
        }

        public Uri getUri()
        {
            if (mId == null) {
                return null;
            }

            Uri.Builder builder = new Uri.Builder();
            return builder.scheme(UriScheme).authority((String)mId).build();
        }

        public String getTitle()
        {
            return (String)getRevision().getProperties().get("title");
        }

        public void setTitle(String title)
        {
            getRevision().getProperties().put("title", title);
        }

        public int getTimestamp()
        {
            return (int)getRevision().getProperties().get("timestamp");
        }

        public Document.Metadata getMetadata()
        {
            return null;
        }

        public void setMetadata(Document.Metadata metadata)
        {
        }

        public java.util.List<String> getFiles()
        {
            Map<String, Object> props = getRevision().getProperties();
            java.util.List<Object> files = (java.util.List<Object>)props.get("files");

            ArrayList<String> ret = new ArrayList<String>();

            for (Object o : files) {
                Map<String, Object> file = (Map<String, Object>)o;
                ret.add((String)file.get("uri"));
            }

            return ret;
        }

        public void addFile(String file)
        {
            Map<String, Object> props = getRevision().getProperties();
            java.util.List<Object> files = (java.util.List<Object>)props.get("files");
            Map<String, Object> fileProps = new HashMap<String, Object>();

            fileProps.put("uri", file);
            fileProps.put("timestamp", System.currentTimeMillis() / 1000);
            fileProps.put("description", "");
            {
                Map<String, Object> loc = new HashMap<String, Object>();
                loc.put("lat", 0);
                loc.put("long", 0);
                fileProps.put("location", loc);
            }

            files.add(fileProps);
        }

        public Object getId()
        {
            return mId;
        }
    }

    /**
     *  class wrapping a LiveQuery
     */
    private static final class CouchDocumentList implements DocumentDB.List {
        private LiveQuery liveQuery;
        private QueryEnumerator enumerator;
        private DocumentDB.Listener listener;
        private Handler mChangedHandler;

        public CouchDocumentList(LiveQuery lq) {
            liveQuery = lq;

            mChangedHandler = createChangeHandler();

            liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
                @Override
                public void changed(final LiveQuery.ChangeEvent event) {
                    notifyChange();
                }
            });

            liveQuery.start();
        }

        public int getCount() {
            return enumerator == null ? 0 : enumerator.getCount();
        }

        public Document getDocument(int position) {
            QueryRow row = enumerator.getRow(position);
            CouchDocument cd = new CouchDocument(row.getValue());

            // must check if the document is valid
            if (cd.getRevision() != null) {
                return cd;
            } else {
                return null;
            }
        }

        public void setListener(DocumentDB.Listener l) {
            listener = l;
        }

        /**
         *  Notify a change in the data set or to individual documents
         *  contained in the data set. This method is thread safe.
         */
        public void notifyChange()
        {
            Message.obtain(mChangedHandler, 0).sendToTarget();
        }

        /**
         *  create Handler running on UI thread to respond to change events
         */
        private Handler createChangeHandler() {
            return new Handler() {
                @Override
                public void handleMessage(Message msg) {
                    enumerator = liveQuery.getRows();
                    if (listener != null) {
                        listener.changed();
                    }
                }
            };
        }
    }

    /**
     *
     */
    public CouchDocumentDB(android.content.Context context) throws RuntimeException {
        try {
            mManager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
            mDatabase = mManager.getDatabase("idoc");
            sSingletonDatabase = mDatabase;

        } catch (IOException e) {
            throw new RuntimeException("could not create database manager, IOException");
        } catch (CouchbaseLiteException e) {
            throw new RuntimeException("could not create database");
        }
    }

    private Query getQuery() {
        View v = mDatabase.getView("list");
        if (v.getMap() == null) {
            v.setMap(new Mapper() {
                public void map(Map<String, Object> document, Emitter emitter) {
                    emitter.emit(document.get("timestamp"), document.get("_id"));
                }
            }, "1");
        }

        return v.createQuery();
    }

    private void addTestDocs() {
        for (int i = 0; i < 20; ++i) {
            Document d = createDocument();
            d.setTitle(new Character((char)('a' + i)).toString());
            saveDocument(d);
        }
    }

    public DocumentDB.List getDocumentList()
    {
        LiveQuery lq = getQuery().toLiveQuery();
        lq.setDescending(true);

        CouchDocumentList cdl = new CouchDocumentList(lq);
        mDocLists.put(cdl, 0);
        return cdl;
    }

    public Document createDocument()
    {
        return new CouchDocument(null);
    }

    public Document getDocument(Uri uri)
    {
        if (uri.getScheme().equals(UriScheme)) {
            return new CouchDocument(uri.getAuthority());
        } else {
            return null;
        }
    }

    private void notifyListsChanged() {
        // Notifiy all lists of change.
        for (CouchDocumentList cdl : mDocLists.keySet()) {
            cdl.notifyChange();
        }
    }

    public void saveDocument(Document doc)
    {
        CouchDocument cd = (CouchDocument)doc;
        if (cd.getId() != null) {
            cd.save();
            // As this is an already existing document, notify existing document lists
            // of a change to their data set.
            notifyListsChanged();
        } else {
            // A new document is automatically picked up by LiveQuery listener
            cd.save();
        }
    }

    public void deleteDocument(Document doc) {
        // Delete associated files.
        for (String f : doc.getFiles()) {
            (new File(f)).delete();
        }
        CouchDocument cd = (CouchDocument)doc;
        cd.delete();
    }

}
