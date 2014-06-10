package com.jajja.jorm.patch;

import java.sql.SQLException;

import org.postgresql.util.PGobject;

@SuppressWarnings("serial")
public class FixedPGobject extends PGobject {
    public FixedPGobject() {
        super();
    }

    public FixedPGobject(String type, String value) throws SQLException {
        super();
        setType(type);
        setValue(value);
    }

    public FixedPGobject(PGobject pgObject) throws SQLException {
        super();
        setType(pgObject.getType());
        setValue(pgObject.getValue());
    }

    @Override
    public int hashCode() {
        return getType().hashCode() + getValue().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof PGobject) {
            return ((PGobject)obj).getValue().equals(getValue()) && ((PGobject)obj).getType().equals(getType());
        }
        return false;
    }
}