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
import com.couchbase.lite.UnsavedRevision;
import com.couchbase.lite.View;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

/**
 *  DocumentDB concerns the set of stored documents,
 *  and manages adding and removing of documents from the database.
 */
public class DocumentDB
{
    public static final String TAG = "DocumentDB";

    private static volatile DocumentDB inst;

    private Manager manager;
    private Database database;

    private DocumentDB() {}

    public static DocumentDB get(android.content.Context context) {
        if (inst == null) {
            DocumentDB db = new DocumentDB();

            try {
                db.manager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
                db.database = db.manager.getDatabase("idoc");
            } catch (IOException e) {
                Log.e(TAG, "could not create database manager, IOException");
                return null;
            } catch (CouchbaseLiteException e) {
                Log.e(TAG, "could not create database");
                return null;
            }

            inst = db;
        }

        return inst;
    }

    private Query getQuery() {
        View v = database.getView("list");
        if (v.getMap() == null) {
            v.setMap(new Mapper() {
                public void map(Map<String, Object> document, Emitter emitter) {
                    emitter.emit(document.get("title"), document);
                }
            }, "1");
        }

        return v.createQuery();
    }

    private void addTestDocs() {
        for (int i = 0; i < 10; ++i) {
            Map<String, Object> p = new HashMap<String, Object>();
            p.put("title", new StringBuilder().append("").append('a' + i).toString());

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

    /**
     *  Get the full list of stored documents.
     */
    public List<Document> getDocumentList()
    {
        try {
            QueryEnumerator qe = getQuery().run();

            if (qe.getCount() == 0) {
                addTestDocs();
                // rerun??
                qe = getQuery().run();
            }

            List<Document> l = new ArrayList<Document>();
            for (int i = 0; i < qe.getCount(); ++i) {
                com.couchbase.lite.Document cbd = qe.getRow(i).getDocument();
                Document d = new Document();
                d.setTitle("" + cbd.toString());
                l.add(d);
            }
            return l;
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "problem running query");
            return null;
        }
    }

    /**
     *  Save a new or update an already existing Document
     */
    public void saveDocument(Document d)
    {
    }
}
