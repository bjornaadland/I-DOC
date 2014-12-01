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

    public static final String ViewDocDate = "docdate";

    public static final String KeyType = "type";
    public static final String KeyTitle = "title";
    public static final String KeyTimestamp = "timestamp";
    public static final String KeyFiles = "files";
    public static final String KeyUri = "uri";
    public static final String KeyDescription = "description";
    public static final String KeyLocation = "location";
    public static final String KeyLat = "lat";
    public static final String KeyLong = "long";
    public static final String KeyMeta = "meta";

    public static final String TypeDoc = "doc";

    public static Database sSingletonDatabase;

    public static Map<Object, MetaMapper> sMetaMappers;

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

            Log.d(TAG, "saving document " + rev.getProperties().toString());

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

                    props.put(KeyType, TypeDoc);
                    props.put(KeyTitle, "");
                    props.put(KeyTimestamp, System.currentTimeMillis() / 1000);
                    props.put(KeyFiles, new ArrayList<Object>());
                    props.put(KeyMeta, new ArrayList<Object>());

                    mRev.setProperties(props);
                } else {
                    com.couchbase.lite.Document doc = sSingletonDatabase.getDocument((String)mId);
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
            return (String)getRevision().getProperties().get(KeyTitle);
        }

        public void setTitle(String title)
        {
            getRevision().getProperties().put(KeyTitle, title);
        }

        public int getTimestamp()
        {
            return (int)getRevision().getProperties().get(KeyTimestamp);
        }

        public java.util.List<Metadata> getMetadata()
        {
            ArrayList<Metadata> l = new ArrayList<Metadata>();
            Map<String, Object> props = getRevision().getProperties();

            for (Object o : (java.util.List<Object>)props.get(KeyMeta)) {
                l.add(new CouchMetadata((Map<String, Object>)o));
            }

            return l;
        }

        public void addMetadata(Metadata metadata)
        {
            CouchMetadata cm = (CouchMetadata)metadata;
            Map<String, Object> props = getRevision().getProperties();

            if (cm.getType() == null) {
                throw new RuntimeException("cannot add an empty metadata object");
            }

            ((java.util.List<Object>)props.get(KeyMeta)).add(cm.getProperties());
        }

        public java.util.List<String> getFiles()
        {
            Map<String, Object> props = getRevision().getProperties();
            java.util.List<Object> files = (java.util.List<Object>)props.get(KeyFiles);

            ArrayList<String> ret = new ArrayList<String>();

            for (Object o : files) {
                Map<String, Object> file = (Map<String, Object>)o;
                ret.add((String)file.get(KeyUri));
            }

            return ret;
        }

        public void addFile(String file)
        {
            Map<String, Object> props = getRevision().getProperties();
            java.util.List<Object> files = (java.util.List<Object>)props.get(KeyFiles);
            Map<String, Object> fileProps = new HashMap<String, Object>();

            fileProps.put(KeyUri, file);
            fileProps.put(KeyTimestamp, System.currentTimeMillis() / 1000);
            fileProps.put(KeyDescription, "");
            {
                Map<String, Object> loc = new HashMap<String, Object>();
                loc.put(KeyLat, 0);
                loc.put(KeyLong, 0);
                fileProps.put(KeyLocation, loc);
            }

            files.add(fileProps);
        }

        public Object getId()
        {
            return mId;
        }
    }

    /**
     *  Metadata implementation
     */
    private static final class CouchMetadata implements Metadata {
        private java.lang.Class mType;
        private Map<String, Object> mProperties;

        public java.lang.Class getType() { return mType; }

        public Map<String, Object> getProperties() {
            return mProperties;
        }

        public CouchMetadata() {
            mProperties = new HashMap<String, Object>();
        }

        public CouchMetadata(Map<String, Object> props) {
            switch ((String)props.get(KeyType)) {
            case "person":
                mType = Metadata.Person.class;
                break;
            case "victim":
                mType = Metadata.Victim.class;
                break;
            case "suspect":
                mType = Metadata.Suspect.class;
                break;
            case "witness":
                mType = Metadata.Witness.class;
                break;
            case "protectedobject":
                mType = Metadata.ProtectedObject.class;
                break;
            case "context":
                mType = Metadata.Context.class;
                break;
            case "orgunit":
                mType = Metadata.OrgUnit.class;
                break;
            default:
                throw new RuntimeException("programming error");
            }
            mProperties = props;
        }

        static String getKey(Enum e) {
            return e.toString().toLowerCase();
        }

        public void set(Enum e, Object value) {
            if (mType == null) {
                mProperties.put(KeyType, e.getClass().getSimpleName().toLowerCase());
                mType = e.getClass();
            } else {
                if (e.getClass() != mType) {
                    throw new RuntimeException("type mismatch: had " + mType.getName() + ", tried to set " + e.getClass().getName() + " property");
                }
            }

            MetaMapper mapper = sMetaMappers.get(e);
            Log.d(TAG, "mapper for enum class " + e.getClass().getName() + ": " + mapper);
            mProperties.put(getKey(e), mapper == null ? value : mapper.mapToDb(value));
        }

        public Object get(Enum e) {
            if (mType != null) {
                if (e.getClass() != mType) {
                    throw new RuntimeException("type mismatch");
                }
            } else {
                throw new RuntimeException("object not initialized");
            }

            MetaMapper mapper = sMetaMappers.get(e);
            Object value = mProperties.get(getKey(e));
            return mapper == null ? value : mapper.mapToUser(value);
        }
    }

    private interface MetaMapper {
        Object mapToDb(Object o);
        Object mapToUser(Object o);
    }

    private class PersonMapper implements MetaMapper {
        public Object mapToDb(Object o) {
            return ((CouchMetadata)o).getProperties();
        }

        public Object mapToUser(Object o) {
            return new CouchMetadata((Map<String, Object>)o);
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
            return new CouchDocument(row.getValue());
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

            sMetaMappers = new HashMap<Object, MetaMapper>();
            sMetaMappers.put(Metadata.Victim.Person, new PersonMapper());
            sMetaMappers.put(Metadata.Suspect.Person, new PersonMapper());
            sMetaMappers.put(Metadata.Witness.Person, new PersonMapper());
        } catch (IOException e) {
            throw new RuntimeException("could not create database manager, IOException");
        } catch (CouchbaseLiteException e) {
            throw new RuntimeException("could not create database");
        }
    }

    private Query getQuery() {
        View v = mDatabase.getView(ViewDocDate);
        if (v.getMap() == null) {
            v.setMap(new Mapper() {
                public void map(Map<String, Object> document, Emitter emitter) {
                    if (document.get(KeyType).equals(TypeDoc)) {
                        emitter.emit(document.get("timestamp"), document.get("_id"));
                    }
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
        if (cd.delete()) {
            notifyListsChanged();
        }
    }

    public Metadata createMetadata()
    {
        return new CouchMetadata();
    }
}
