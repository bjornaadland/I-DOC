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
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class CouchDocumentDB extends DocumentDB
{
    public static final String TAG = "CouchDocumentDB";
    public static final String UriScheme = "evidence";

    public static Database mSingletonDatabase;

    private Manager manager;
    private Database database;

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

        /**
         *  Creates an UnsavedRevision that contains the document properties
         *  both for reading and manipulation.
         */
        public UnsavedRevision getRevision()
        {
            if (mRev == null) {
                if (mId == null) {
                    com.couchbase.lite.Document doc = mSingletonDatabase.createDocument();
                    mRev = doc.createRevision();

                    Map<String, Object> props = new HashMap<String, Object>();

                    props.put("title", "");
                    props.put("timestamp", System.currentTimeMillis() / 1000);
                    props.put("files", new ArrayList<Object>());

                    mRev.setProperties(props);
                } else {
                    com.couchbase.lite.Document doc = mSingletonDatabase.getDocument((String)mId);
                    mRev = doc.createRevision();
                }
            }

            return mRev;
        }

        public void resetRevision()
        {
            mRev = null;
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

        public Object getID()
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
                    Log.d(TAG, "LiveQuery changed...");
                    Message.obtain(mChangedHandler, 0).sendToTarget();
                }
            });

            liveQuery.start();
        }

        public int getCount() {
            return enumerator == null ? 0 : enumerator.getCount();
        }

        public Document getDocument(int position) {
            QueryRow row = enumerator.getRow(position);
            return new CouchDocument(row.getValue());
        }

        public void setListener(DocumentDB.Listener l) {
            listener = l;
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
            manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
            database = manager.getDatabase("idoc");
            mSingletonDatabase = database;

            /**** DEVELOPMENT CODE: ****/
            try {
                database.delete();
            } catch (CouchbaseLiteException e) {
            }
            /**** END */
        } catch (IOException e) {
            throw new RuntimeException("could not create database manager, IOException");
        } catch (CouchbaseLiteException e) {
            throw new RuntimeException("could not create database");
        }
    }

    private Query getQuery() {
        View v = database.getView("list");
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
        return new CouchDocumentList(lq);
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

    public void saveDocument(Document d)
    {
        CouchDocument cDoc = (CouchDocument)d;
        UnsavedRevision rev = cDoc.getRevision();

        try {
            cDoc.resetRevision();
            rev.save();
        } catch (CouchbaseLiteException e) {
            throw new RuntimeException("problem saving document");
        }
    }
}
