package ua.leonidius.trdinterface.models;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.misc.BaseDaoEnabled;
import com.j256.ormlite.table.DatabaseTable;

/**
 * Domain model for a shop
 */
@DatabaseTable(tableName = "shops")
public class Shop extends BaseDaoEnabled {

    public Shop() {}

    @DatabaseField(generatedId = true, columnName = "record_id")
    int recordId;

    @DatabaseField(canBeNull = false, unique = true)
    String name;

    @ForeignCollectionField(columnName = "buyable_items_categories")
    ForeignCollection<Category> buyableItemsCategories;
    // Do we need columnName here?

    @ForeignCollectionField(columnName = "sellable_items")
    ForeignCollection<SellableItem> sellableItems;

}