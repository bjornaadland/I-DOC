package no.nhc.i_doc;

public interface Metadata {
    public interface PropertyType {
        java.lang.Class getType();
        boolean isList();
    }

    /**
     *  Class used when looking at a property mapped
     *  across a metadata collection
     */
    public static class PropertyMap {
        public Object propertyValue;
        public Object id;
    }

    /**
     *  Listing of the properties of a Person
     */
    public static enum Person {
        FamilyName,    /* String */
        GivenName,     /* String */
        OtherIdentity, /* String */
        Gender,        /* String */
        DateOfBirth,   /* ??? */
        AgeCategory,   /* Value.AgeCategory */
        Address,       /* String */
        Notes,         /* String */
        OriginalCollection, /* Value.OriginalCollection */
    }

    /**
     *  Listing of the properties of a Victim
     */
    public static enum Victim {
        Person,              /* Person */
        InterestsViolated,   /* List<Value.Interest> */
        ViolationType,       /* List<Value.Violation> */
        GeoRelevanceAndTime, /* ??? Geography and time period, multiple */
        ParticularVulnerability, /* List<Value.Vulnerability> */
        OriginalCollection,  /* Value.OriginalCollection */
        ICHLStatus,          /* Value.ICHLStatus */
        RoleAndBelonging,    /* Value.RoleAndBelonging */
        Notes,               /* String */
    }

    /**
     *  Listing of the properties of a Witness
     */
    public static enum Witness {
        Person,              /* Person */
        Type,                /* List<Value.WitnessType> */
        Rank,                /* String */
        RoleAndBelonging,    /* Value.RoleAndBelonging */
        Reliability,         /* List<Value.Reliability> */
        GeoRelevanceAndTime, /* ??? */
        OriginalCollection,  /* Value.OriginalCollection */
        Notes,               /* String */
    }

    /**
     *  Listing of the properties of a Suspect
     */
    public static enum Suspect {
        Person,              /* Person */
        LegalStatus,         /* List<Value.LegalStatus> */
        GeoRelevanceAndTime, /* ??? */
        OriginalCollection,  /* Value.OriginalCollection */
        Notes,               /* String */
    }

    /**
     *  Listing of the properties of a ProtectedObject
     */
    public static enum ProtectedObject {
        Name,                 /* String */
        GeoRelevanceAndTime,  /* ??? */
        NotorietyLevel,       /* Value.NotorietyLevel */
        Typology,             /* List<Value.Typology> */
        Notes,                /* String */
        OriginalCollection,   /* Value.OriginalCollection */
    }

    /**
     *  Listing of the properties of a Context
     */
    public static enum Context {
        Name,                 /* String */
        GeoRelevanceAndTime,  /* ???? */
        ImportanceLevel,      /* Value.ImportanceLevel */
        Typology,             /* List<Value.Typology> */
        Forms,                /* List<Value.ContextForm> */
        Notes,                /* String */
    }

    /**
     *  Listing of the properties of an OrgUnit
     */
    public static enum OrgUnit {
        Name,                  /* String */
        InstitutionalBelonging, /* List<Value.InstitutionalBelonging> */
        Typology,               /* List<Value.Typology> */
        RoleAndBelonging,       /* Value.RoleAndBelonging */
        GeoRelevanceAndTime,    /* ??? */
        Importance,             /* Value.Importance */
        Notes,                  /* String */
    }

    /**
     *  Return the actual type of this metadata object.
     *  The type will match one of the enum types.
     */
    java.lang.Class getType();

    /**
     *  Get the data type of the specified property
     */
    PropertyType getPropertyType(Enum e);

    /**
     *  Get the ID of this metadata, if it exists. Metadata objects
     *  that can be referred by other metadata objects or several
     *  documents, need an id.
     */
    Object getId();

    /**
     *  Set the ID of this metadata. It must be the same id that has
     *  been returned by another metadata. It cannot be generated outside.
     */
    void setId(Object id);

    /**
     *  Set a metadata property.
     *  All the properties must come from the same enum, that denotes the
     *  type of the Metadata object.
     */
    void set(Enum e, Object value);

    /**
     *  Get a metadata property.
     */
    Object get(Enum e);
}
