package no.nhc.i_doc;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.couchbase.lite.android.AndroidContext;
import com.couchbase.lite.auth.Authenticator;
import com.couchbase.lite.auth.AuthenticatorFactory;
import com.couchbase.lite.replicator.Replication;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

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
    public static Replication sPushReplication;
    public static Replication sPullReplication;

    public static Resources sResources;
    public static JSONObject sRawSchema = null;

    public static Map<Object, MetaMapper> sMetaMappers;

    private Manager mManager;
    private Database mDatabase;

    WeakReference<CouchDocumentList> mDocList = new WeakReference<CouchDocumentList>(null);

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
                l.add(new CouchMetadata((Map<String, Object>)o, null));
            }

            return l;
        }

        public void setMetadata(java.util.List<Metadata> metadata) {
            Map<String, Object> props = getRevision().getProperties();
            java.util.List<Object> existing = (java.util.List<Object>)props.get(KeyMeta);

            existing.clear();

            for (Metadata md : metadata) {
                existing.add(((CouchMetadata)md).getProperties());
            }
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

        public String getUUID()
        {
            return (String)mId;
        }


        private java.util.List<Listener> mListeners;

        public void addChangeListener(Listener newListener) {
            com.couchbase.lite.Document doc = sSingletonDatabase.getExistingDocument((String)mId);
            if (doc == null) {
                return;
            }
            if (mListeners == null) {
                mListeners = new ArrayList<Listener>();
            }
            mListeners.add(newListener);
            if (mListeners.size() == 1) {
                doc.addChangeListener(new com.couchbase.lite.Document.ChangeListener() {
                        @Override 
                        public void changed(com.couchbase.lite.Document.ChangeEvent event) { 
                            com.couchbase.lite.DocumentChange docChange = event.getChange();
                            Log.e(TAG, "document " + this.toString() + " changed");
                            mRev = null;
                            notifyListeners();
                        } 
                    });
            }
        }

        public void removeChangeListener(Listener listener) {
            if (mListeners == null) {
                return;
            }
            mListeners.remove(listener);
            if (mListeners.size() == 0) {
                mListeners = null;
                // mDoc.removeChangeListener(mChangeListener);
            }
        }

        private void notifyListeners() {
            if (mListeners == null || getRevision() == null) {
                return;
            }
            for (Listener listener : mListeners) {
                listener.changed();
            }
        }
    }

    /**
     *  Metadata implementation
     */
    private static final class CouchMetadata implements Metadata {
        private java.lang.Class mType;
        private Map<String, Object> mProperties;
        private Object mId;

        public java.lang.Class getType() { return mType; }

        public Metadata.PropertyType getPropertyType(Enum e) {
            MetaMapper mapper = sMetaMappers.get(e);
            if (mapper == null) {
                return new Metadata.PropertyType() {
                    public java.lang.Class getType() { return String.class; }
                    public boolean isList() { return false; }
                };
            } else {
                return mapper.getPropertyType();
            }
        }

        public Map<String, Object> getProperties() {
            return mProperties;
        }

        public CouchMetadata(java.lang.Class type) {
            mType = type;
            mProperties = new HashMap<String, Object>();
            mProperties.put(KeyType, type.getSimpleName());
            mId = null;
        }

        public CouchMetadata(java.lang.Class type, Object id) {
            this(type);
            mId = id;
        }

        public CouchMetadata(Map<String, Object> props, Object id) {
            java.lang.Class cls = DocumentUtils.getMetadataClass((String)props.get(KeyType));

            if (cls == null) {
                throw new IllegalArgumentException("invalid type");
            }

            mType = cls;
            mProperties = props;
            mId = id;
        }

        static String getKey(Enum e) {
            return e.toString();
        }

        public Object getId() { return mId; }
        public void setId(Object id) { mId = id; }

        public void set(Enum e, Object value) {
            if (e.getClass() != mType) {
                throw new RuntimeException("type mismatch: had " + mType.getName() + ", tried to set " + e.getClass().getName() + " property");
            }

            MetaMapper mapper = sMetaMappers.get(e);
            mProperties.put(getKey(e), mapper == null ? value : mapper.mapToDb(value));
        }

        public Object get(Enum e) {
            if (e.getClass() != mType) {
                throw new RuntimeException("type mismatch");
            }

            MetaMapper mapper = sMetaMappers.get(e);
            Object value = mProperties.get(getKey(e));
            return mapper == null ? value : mapper.mapToUser(value);
        }
    }

    /**
     *  Map to and from Metadata property types and database native types
     */
    private interface MetaMapper {
        Metadata.PropertyType getPropertyType();
        Object mapToDb(Object o);
        Object mapToUser(Object o);
    }

    /**
     *  Map between Metadata object (as property)
     *  and database native representation. This version represents the
     *  object as a JSON "object".
     */
    private static class ObjectMapper implements MetaMapper {
        private final java.lang.Class mType;

        public ObjectMapper(java.lang.Class type) {
            mType = type;
        }

        public Metadata.PropertyType getPropertyType() {
            return new Metadata.PropertyType() {
                public java.lang.Class getType() { return mType; }
                public boolean isList() { return false; }
            };
        }

        public Object mapToDb(Object o) {
            return o != null ? ((CouchMetadata)o).getProperties() : null;
        }

        public Object mapToUser(Object o) {
            if (o != null) {
                return new CouchMetadata((Map<String, Object>)o, null);
            } else {
                return null;
            }
        }
    }

    /**
     *  Map between Metadata object (as property) and database
     *  native representation as a document in itself, with the specified type.
     */
    private static class DocumentMapper implements MetaMapper {
        private final java.lang.Class mType;

        public DocumentMapper(java.lang.Class type) {
            mType = type;
        }

        public Metadata.PropertyType getPropertyType() {
            return new Metadata.PropertyType() {
                public java.lang.Class getType() { return mType; }
                public boolean isList() { return false; }
            };
        }

        public Object mapToDb(Object o) {
            CouchMetadata cm = (CouchMetadata)o;
            String id = (String)cm.getId();
            com.couchbase.lite.Document doc = null;

            // first, write value document
            if (id != null) {
                doc = sSingletonDatabase.getExistingDocument(id);
            } else {
                doc = sSingletonDatabase.createDocument();
            }

            Map<String, Object> properties = cm.getProperties();
            properties.put("type", mType.getSimpleName());

            try {
                UnsavedRevision rev = doc.createRevision();
                rev.setProperties(properties);
                rev.save();
            } catch (CouchbaseLiteException e) {
                throw new RuntimeException("problem saving document in DocumentMapper");
            }

            if (id == null) {
                cm.setId(doc.getId());
            }

            // Now, this is what is returned as the property value
            Map<String, Object> propertyValue = new HashMap<String, Object>();
            propertyValue.put("_id", doc.getId());
            return propertyValue;
        }

        public Object mapToUser(Object o) {
            Map<String, Object> propertyValue = (Map<String, Object>)o;
            if (propertyValue == null) return null;

            String id = (String)propertyValue.get("_id");
            com.couchbase.lite.Document doc = sSingletonDatabase.getExistingDocument(id);

            if (doc == null) {
                return new CouchMetadata(mType);
            } else {
                return new CouchMetadata(doc.getProperties(), doc.getId());
            }
        }
    }

    /**
     *  Map between Value objects and database native representation
     */
    private static class ValueMapper implements MetaMapper {
        private java.lang.Class mClass;

        public static Value createValue(java.lang.Class valueClass, Object key) {
            Value v = null;
            if (key != null) {
                try {
                    v = (Value)valueClass.newInstance();
                    v.setKey(key);
                } catch (Exception e) {}
            }
            return v;
        }

        public ValueMapper(java.lang.Class valueClass) {
            mClass = valueClass;
        }

        public Metadata.PropertyType getPropertyType() {
            return new Metadata.PropertyType() {
                public java.lang.Class getType() { return mClass; }
                public boolean isList() { return false; }
            };
        }

        public Object mapToDb(Object o) {
            Value v = null;

            if (o instanceof String) {
                // BUG: should validate
                return o;
            } else {
                v = (Value)o;
            }
            return v.getKey();
        }

        public Object mapToUser(Object o) {
            return createValue(mClass, o);
        }
    }

    /**
     *  Map between List<Value> and database native representation
     */
    private static class ValueListMapper implements MetaMapper {
        private java.lang.Class mClass;

        public ValueListMapper(java.lang.Class valueClass) {
            mClass = valueClass;
        }

        public Metadata.PropertyType getPropertyType() {
            return new Metadata.PropertyType() {
                public java.lang.Class getType() { return mClass; }
                public boolean isList() { return true; }
            };
        }

        public Object mapToDb(Object o) {
            return new ArrayList<Value>();
        }

        public Object mapToUser(Object o) {
            return null;
        }
    }

    /**
     *  class wrapping a LiveQuery
     */
    private static final class CouchDocumentList implements DocumentDB.List<Document> {
        private LiveQuery liveQuery;
        private QueryEnumerator enumerator;
        private WeakHashMap<DocumentDB.Listener, Integer> mListeners =
                new WeakHashMap<DocumentDB.Listener, Integer>();
        private Handler mChangedHandler;

        public CouchDocumentList(LiveQuery lq) {
            liveQuery = lq;

            mChangedHandler = createChangeHandler();

            liveQuery.addChangeListener(new LiveQuery.ChangeListener() {
                @Override
                public void changed(final LiveQuery.ChangeEvent event) {
                    Log.e(TAG, "LiveQuery " + this.toString() + " changed");
                    notifyChange();
                }
            });

            liveQuery.start();
        }

        public int getCount() {
            return enumerator == null ? 0 : enumerator.getCount();
        }

        public Document getObject(int position) {
            QueryRow row = enumerator.getRow(position);
            CouchDocument cd = new CouchDocument(row.getValue());

            // must check if the document is valid
            if (cd.getRevision() != null) {
                return cd;
            } else {
                return null;
            }
        }

        public void addListener(DocumentDB.Listener l) {
            mListeners.put(l, 0);
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
                    for (DocumentDB.Listener listener : mListeners.keySet() ) {
                        listener.changed();
                    }
                }
            };
        }
    }

    private void mapValue(Object property, java.lang.Class valueClass) {
        sMetaMappers.put(property, new ValueMapper(valueClass));
    }

    private void mapValueList(Object property, java.lang.Class valueClass) {
        sMetaMappers.put(property, new ValueListMapper(valueClass));
    }

    private void mapPerson(Object property) {
        //sMetaMappers.put(property, new ObjectMapper(Metadata.Person.class));
        sMetaMappers.put(property, new DocumentMapper(Metadata.Person.class));
    }

    /**
     *  Construct the couchbase lite based document database
     */
    public CouchDocumentDB(android.content.Context context) throws RuntimeException {
        try {
            mManager = new Manager(new AndroidContext(context), Manager.DEFAULT_OPTIONS);
            mDatabase = mManager.getDatabase("idoc");
            sSingletonDatabase = mDatabase;

            sResources = context.getResources();

            sMetaMappers = new HashMap<Object, MetaMapper>();

            /* Person special properties */
            mapValue(Metadata.Person.AgeCategory, Value.AgeCategory.class);
            mapValue(Metadata.Person.Gender, Value.Gender.class);
            mapValue(Metadata.Person.OriginalCollection, Value.OriginalCollection.class);

            /* Victim special properties */
            mapPerson(Metadata.Victim.Person);
            mapValueList(Metadata.Victim.InterestsViolated, Value.Violation.class);
            mapValueList(Metadata.Victim.ViolationType, Value.ViolationType.class);
            mapValue(Metadata.Victim.ParticularVulnerability, Value.Vulnerability.class);
            mapValue(Metadata.Victim.OriginalCollection, Value.OriginalCollection.class);
            mapValue(Metadata.Victim.ICHLStatus, Value.ICHLStatus.class);
            mapValue(Metadata.Victim.RoleAndBelonging, Value.RoleAndBelonging.class);

            /* Witness special properties */
            mapPerson(Metadata.Witness.Person);
            mapValueList(Metadata.Witness.Type, Value.WitnessType.class);
            mapValue(Metadata.Witness.RoleAndBelonging, Value.RoleAndBelonging.class);
            mapValueList(Metadata.Witness.Reliability, Value.Reliability.class);
            mapValue(Metadata.Witness.OriginalCollection, Value.OriginalCollection.class);

            /* Suspect special properties */
            mapPerson(Metadata.Suspect.Person);
            mapValueList(Metadata.Suspect.LegalStatus, Value.LegalStatus.class);
            mapValue(Metadata.Suspect.OriginalCollection, Value.OriginalCollection.class);

            /* ProtectedObject special properties */
            mapValue(Metadata.ProtectedObject.NotorietyLevel, Value.NotorietyLevel.class);
            mapValueList(Metadata.ProtectedObject.Typology, Value.Typology.class);
            mapValue(Metadata.ProtectedObject.OriginalCollection, Value.OriginalCollection.class);

            /* Context special properties */
            mapValue(Metadata.Context.ImportanceLevel, Value.ImportanceLevel.class);
            mapValue(Metadata.Context.Typology, Value.Typology.class);
            mapValue(Metadata.Context.Forms, Value.ContextForm.class);

            /* OrgUnit speicl properties */
            mapValue(Metadata.OrgUnit.InstitutionalBelonging, Value.InstitutionalBelonging.class);
            mapValueList(Metadata.OrgUnit.Typology, Value.Typology.class);
            mapValueList(Metadata.OrgUnit.RoleAndBelonging, Value.RoleAndBelonging.class);
            mapValue(Metadata.OrgUnit.Importance, Value.ImportanceLevel.class); // same as above?
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
        CouchDocumentList cdl = mDocList.get();
        if (cdl == null) {
            LiveQuery lq = getQuery().toLiveQuery();
            lq.setDescending(true);

            cdl = new CouchDocumentList(lq);
            mDocList = new WeakReference<CouchDocumentList>(cdl);
        }
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

    private void notifyListChanged() {
        // Notifiy change.
        CouchDocumentList cdl = mDocList.get();
        if (cdl != null) {
            cdl.notifyChange();
        }
    }

    public void saveDocument(Document doc)
    {
        CouchDocument cd = (CouchDocument)doc;
        if (cd.getId() != null) {
            cd.save();
            // As this is an already existing document, notify existing document list
            // of a change to the data set.
            notifyListChanged();
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

    public Metadata createMetadata(java.lang.Class type, Object id)
    {
        if (id == null) {
            return new CouchMetadata(type);
        } else {
            com.couchbase.lite.Document doc = sSingletonDatabase.getExistingDocument((String)id);

            if (doc == null) {
                return null;
            } else {
                return new CouchMetadata(doc.getProperties(), doc.getId());
            }
        }
    }

    public DocumentDB.List<Metadata.PropertyMap> mapMetadata(Enum key) {
        final View v = mDatabase.getView(key.toString());
        final java.lang.Class metaType = key.getClass();
        final String typeName = metaType.getSimpleName();
        final String keyName = key.toString();

        if (v.getMap() == null) {
            v.setMap(new Mapper() {
                public void map(Map<String, Object> document, Emitter emitter) {
                    if (document.get(KeyType).equals(typeName)) {
                        emitter.emit(document.get(keyName), document.get("_id"));
                    }
                }
            }, "1");
        }

        final QueryEnumerator enumerator;

        try {
            enumerator = v.createQuery().run();
        } catch (CouchbaseLiteException e) {
            Log.e(TAG, "can't run property map query: " + e.toString());
            return null;
        }

        return new DocumentDB.List<Metadata.PropertyMap>() {
            public int getCount() {
                return enumerator.getCount();
            }

            public Metadata.PropertyMap getObject(int position) {
                Metadata.PropertyMap map = new Metadata.PropertyMap();
                QueryRow row = enumerator.getRow(position);
                map.propertyValue = row.getKey();
                map.id = row.getValue();
                return map;
            }

            public void addListener(DocumentDB.Listener l) {
                throw new RuntimeException("this List does not support a listener");
            }
        };
    }

    public void sync(final SyncListener listener)
    {
        if (sPushReplication != null) return;

        URL url;
        try {
            url = new URL("http://ec2-54-78-134-214.eu-west-1.compute.amazonaws.com:4984/idoc");
        } catch (java.net.MalformedURLException e) {
            Log.e(TAG, "can't create URL");
            return;
        }

        Replication.ChangeListener changeListener = new Replication.ChangeListener() {
                public void changed(Replication.ChangeEvent event) {
                    boolean active = 
                        (sPushReplication.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE) ||
                        (sPullReplication.getStatus() == Replication.ReplicationStatus.REPLICATION_ACTIVE);
                    if (!active) {
                        listener.onEvent(new SyncEvent(SyncEvent.STOPPED, 0, 0));
                        sPushReplication = null;
                        sPullReplication = null;
                    } else {
                        int total = sPullReplication.getCompletedChangesCount() + 
                            sPullReplication.getCompletedChangesCount();
                        int progress = sPullReplication.getChangesCount() + 
                            sPullReplication.getChangesCount();

                        listener.onEvent(new SyncEvent(
                                             SyncEvent.PROGRESS, 
                                             progress,
                                             total));
                    }
                    Log.d(TAG, " event: " + event);
                }};


        sPushReplication = sSingletonDatabase.createPushReplication(url);
        sPushReplication.setAuthenticator(
            AuthenticatorFactory.createBasicAuthenticator("idoc", "pass1"));
        sPushReplication.addChangeListener(changeListener);
        sPushReplication.start();

        sPullReplication = sSingletonDatabase.createPullReplication(url);
        sPullReplication.setAuthenticator(
            AuthenticatorFactory.createBasicAuthenticator("idoc", "pass1"));
        sPullReplication.addChangeListener(changeListener);
        sPullReplication.start();

        listener.onEvent(new SyncEvent(SyncEvent.STARTED, 0, 0));
    }

    private java.util.List<Value> getValueSetFromResources(java.lang.Class valueClass) {
        if (sRawSchema == null) {
            String data = "";
            BufferedReader reader = new BufferedReader
                (new InputStreamReader
                 (sResources.openRawResource(R.raw.schema)));
            try {
                String line;
                while ((line = reader.readLine()) != null) {
                    data += line;
                }
            } catch (IOException e) {
            }
            try {
                sRawSchema = new JSONObject(data);
            } catch (JSONException e) {
                throw new RuntimeException("can't read raw schema");
            }
        }

        try {
            JSONObject values = sRawSchema.getJSONObject("values");
            String key = valueClass.getSimpleName();
            if (values.has(key)) {
                JSONArray ja = values.getJSONArray(key);
                ArrayList<Value> l = new ArrayList<Value>();

                for (int i = 0; i < ja.length(); ++i) {
                    l.add(ValueMapper.createValue(valueClass, ja.getString(i)));
                }
                return l;
            }
        } catch (JSONException e) {
        }
        return null;
    }

    public java.util.List<Value> getValueSet(java.lang.Class valueClass)
    {
        java.util.List<Value> l = null;

        l = getValueSetFromResources(valueClass);
        if (l != null) return l;

        l = new ArrayList<Value>();

        com.couchbase.lite.Document metadoc = sSingletonDatabase.getExistingDocument("idoc_metainfo");

        if (metadoc != null) {
            java.util.List<String> lst = (java.util.List<String>)metadoc.getProperties().get("testenum");

            if (lst != null) {
                for (String s : lst) {
                    l.add(ValueMapper.createValue(valueClass, s));
                }
                return l;
            }
        }

        // fallback
        String test = "ABC";

        for (int i = 0; i < 3; ++i) {
            l.add(ValueMapper.createValue(valueClass,
                                          test.substring(i, i + 1)));
        }

        return l;
    }
}
