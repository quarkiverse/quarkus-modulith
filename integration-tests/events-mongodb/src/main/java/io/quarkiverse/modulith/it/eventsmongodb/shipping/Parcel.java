package io.quarkiverse.modulith.it.eventsmongodb.shipping;

import org.bson.types.ObjectId;

import io.quarkus.mongodb.panache.PanacheMongoEntityBase;

/**
 * A parcel, written in the same MongoDB transaction as the publication of its event. It declares its own identifier
 * instead of inheriting it from {@code PanacheMongoEntity}: MongoDB with Panache registers the entity class for
 * reflection in native executables, not the inherited field.
 */
public class Parcel extends PanacheMongoEntityBase {

    public ObjectId id;

    public String label;
}
