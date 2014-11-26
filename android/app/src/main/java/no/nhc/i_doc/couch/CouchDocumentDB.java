package no.nhc.i_doc;

import android.content.Context;
import android.util.Log;

import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.CouchbaseLiteException;
import com.couchbase.lite.Emitter;
import com.couchbase.lite.Database;
import com.couchbase.lite.Manager;
import com.couchbase.lite.Mapper;
import com.couchbase.lite.Query;
import com.couchbase.lite.QueryEnumerator;
import com.couchbase.lite.QueryRow;
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class CouchDocumentDB extends DocumentDB
{
    public static final String TAG = "CouchDocumentDB";

    private Manager manager;
    private Database database;

    private static final class CouchDocument extends Document
    {
        private Object id;

        public CouchDocument(Object id)
        {
            this.id = id;
        }

        public Object getID()
        {
            return id;
        }
    }

    public CouchDocumentDB(android.content.Context context) throws RuntimeException {
        try {
            manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
            database = manager.getDatabase("idoc");
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
            Map<String, Object> p = new HashMap<String, Object>();
            List<Object> files = new ArrayList<Object>();

            {
                Map<String, Object> file = new HashMap<String, Object>();
                file.put("uri", "file:///file");
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

            p.put("title", new Character((char)('a' + i)).toString());
            p.put("files", files);

            UnsavedRevision rev = database.createDocument().createRevision();
            rev.setUserProperties(p);
            try {
                rev.save();
            } catch (CouchbaseLiteException e) {
                Log.e(TAG, "problem saving document");
                return;
            }
        }
    }

    public List<Document> getDocumentList()
    {
        try {
            database.delete();
        } catch (CouchbaseLiteException e) {
        }

        try {
            QueryEnumerator qe = getQuery().run();

            if (qe.getCount() == 0) {
                addTestDocs();
                // rerun??
                qe = getQuery().run();
            }

            List<Document> l = new ArrayList<Document>();
            for (int i = 0; i < qe.getCount(); ++i) {
                QueryRow row = qe.getRow(i);
                Document d = new CouchDocument(row.getKey());
                d.setTitle("" + row.getValue());
                l.add(d);
            }
            return l;
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "problem running query");
            return null;
        }
    }

    public Document createDocument()
    {
        return new CouchDocument(null);
    }

    public void saveDocument(Document d)
    {
    }
}
