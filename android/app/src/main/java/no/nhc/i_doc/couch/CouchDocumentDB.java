package no.nhc.i_doc;

import android.content.Context;
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

    public static Database mSingletonDatabase;

    private Manager manager;
    private Database database;

    private static final class CouchDocument implements Document
    {
        private Object mId;
        private String mTitle;
        private java.util.List<String> mFiles;
        com.couchbase.lite.Document mCouchDoc;

        public CouchDocument(Object id, String title)
        {
            mId = id;
            mTitle = title;
        }

        private void load()
        {
            mFiles = new ArrayList<String>();

            if (mId != null) {
                mCouchDoc = mSingletonDatabase.getDocument((String)mId);

                Map<String, Object> props = mCouchDoc.getProperties();
                java.util.List<Object> files = (java.util.List<Object>)props.get("files");

                for (Object o : files) {
                    Map<String, Object> file = (Map<String, Object>)o;
                    String uri = (String)file.get("uri");

                    mFiles.add(uri);
                }
            }
        }

        public String getTitle()
        {
            return mTitle;
        }

        public void setTitle(String title)
        {
            mTitle = title;
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
            if (mFiles == null) {
                load();
            }

            return mFiles;
        }

        public void addFile(String file)
        {
            if (mFiles == null) {
                getFiles();
            }
            mFiles.add(file);
        }

        public Object getID()
        {
            return mId;
        }
    }

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
            return new CouchDocument(row.getKey(), "" + row.getValue());
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

    public CouchDocumentDB(android.content.Context context) throws RuntimeException {
        try {
            manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
            database = manager.getDatabase("idoc");
            mSingletonDatabase = database;
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
                    emitter.emit(document.get("_id"), document.get("title"));
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
        try {
            database.delete();
        } catch (CouchbaseLiteException e) {
        }

        try {
            Query q = getQuery();
            QueryEnumerator qe = q.run();

            if (qe.getCount() == 0) {
                addTestDocs();
                // rerun??
                q = getQuery();
                qe = q.run();
            }

            return new CouchDocumentList(q.toLiveQuery());
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "problem running query");
            return null;
        }
    }

    public Document createDocument()
    {
        return new CouchDocument(null, "");
    }

    public void saveDocument(Document d)
    {
        CouchDocument cDoc = (CouchDocument)d;
        Map<String, Object> prop = new HashMap<String, Object>();
        ArrayList<Object> files = new ArrayList<Object>();

        for (String fn : cDoc.getFiles()) {
            Map<String, Object> file = new HashMap<String, Object>();
            file.put("uri", fn);
            file.put("datetime", "0");
            file.put("description", "");
            {
                Map<String, Object> loc = new HashMap<String, Object>();
                loc.put("lat", 0);
                loc.put("long", 0);
                file.put("location", loc);
            }
            files.add(file);
        }

        prop.put("title", cDoc.getTitle());
        prop.put("files", files);

        com.couchbase.lite.Document cblDoc;

        if (cDoc.getID() == null) {
            cblDoc = database.createDocument();
        } else {
            cblDoc = database.getDocument((String)cDoc.getID());
        }

        UnsavedRevision rev = cblDoc.createRevision();
        rev.setUserProperties(prop);

        try {
            rev.save();
        } catch (CouchbaseLiteException e) {
            throw new RuntimeException("problem saving document");
        }
    }
}
