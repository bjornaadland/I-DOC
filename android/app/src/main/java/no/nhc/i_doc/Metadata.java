package no.nhc.i_doc;

public interface Metadata {
    /**
     *  Listing of the properties of a Person
     */
    public static enum Person {
        FamilyName,    /* String */
        GivenName,     /* String */
        OtherIdentity, /* String */
        Gender,        /* String */
        DateOfBirth,   /* ??? */
        AgeCategory,   /* MetaEnum */
        Address,       /* String */
        Notes,         /* String */
        OriginalCollection, /* MetaEnum */
    }

    /**
     *  Listing of the properties of a Victim
     */
    public static enum Victim {
        Person,              /* Person */
        InterestsViolated,   /* List<MetaEnum> */
        ViolationType,       /* List<MetaEnum> */
        GeoRelevanceAndTime, /* ??? Geography and time period, multiple */
        ParticularVulnerability, /* List<MetaEnum> */
        OriginalCollection,  /* MetaEnum */
        ICHLStatus,          /* MetaEnum */
        RoleAndBelonging,    /* MetaEnum */
        Notes,               /* String */
    }

    /**
     *  Listing of the properties of a Witness
     */
    public static enum Witness {
        Person,              /* Person */
        Type,                /* List<MetaEnum> */
        Rank,                /* String */
        RoleAndBelonging,    /* MetaEnum */
        Reliability,         /* List<MetaEnum> */
        GeoRelevanceAndTime, /* ??? */
        OriginalCollection,  /* MetaEnum */
        Notes,               /* String */
    }

    /**
     *  Listing of the properties of a Suspect
     */
    public static enum Suspect {
        Person,              /* Person */
        LegalStatus,         /* List<MetaEnum> */
        GeoRelevanceAndTime, /* ??? */
        OriginalCollection,  /* MetaEnum */
        Notes,               /* String */
    }

    /**
     *  Listing of the properties of a ProtectedObject
     */
    public static enum ProtectedObject {
        Name,                 /* String */
        GeoRelevanceAndTime,  /* ??? */
        NotorietyLevel,       /* MetaEnum */
        Typology,             /* List<MetaEnum> */
        Notes,                /* String */
        OriginalCollection,   /* MetaEnum */
    }

    /**
     *  Listing of the properties of a Context
     */
    public static enum Context {
        Name,                 /* String */
        GeoRelevanceAndTime,  /* ???? */
        ImportanceLevel,      /* MetaEnum */
        Typology,             /* List<MetaEnum> */
        Forms,                /* List<MetaEnum> */
        Notes,                /* String */
    }

    /**
     *  Listing of the properties of an OrgUnit
     */
    public static enum OrgUnit {
        Name,                  /* String */
        InstitutionalBelonging, /* List<MetaEnum> */
        Typology,               /* List<MetaEnum> */
        RoleAndBelonging,       /* MetaEnum */
        GeoRelevanceAndTime,    /* ??? */
        Importance,             /* MetaEnum */
        Notes,                  /* String */
    }

    /**
     *  Return the actual type of this metadata object.
     *  The type will match one of the enum types.
     */
    java.lang.Class getType();

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
