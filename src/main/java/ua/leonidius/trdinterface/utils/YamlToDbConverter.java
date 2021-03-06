package ua.leonidius.trdinterface.utils;

import cn.nukkit.item.Item;
import cn.nukkit.utils.Config;
import cn.nukkit.utils.ConfigSection;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import ua.leonidius.trdinterface.Message;
import ua.leonidius.trdinterface.Trading;
import ua.leonidius.trdinterface.models.*;

import java.util.List;

/**
 * Contains methods to add items from the old
 * YAML item lists to the new database, as
 * well as import custom item name translations
 */
public abstract class YamlToDbConverter {

    /**
     * Converts the list of buyable items from
     * YAML config format to database format
     *
     * @param buyableItems config to convert
     * @return whether buyList.yml can be safely deleted
     * without losing data
     */
    public static boolean convertBuyableItems(Config buyableItems) {
        try {
            Dao<Category, Integer> categoryDao =
                    DaoManager.createDao(Trading.getSource(), Category.class);

            int numberOfCategories = 0;
            int numberOfItems = 0;

            for (String categoryKey : buyableItems.getKeys(false)) {
                ConfigSection category = buyableItems.getSection(categoryKey);

                String categoryName = category.getString("name");

                Category categoryModel;
                List<Category> eponymousCategories =
                        categoryDao.queryForEq("name", categoryName);
                if (eponymousCategories.size() == 0) {
                    categoryModel = new Category(Shop.getDefault(), categoryName);
                    categoryDao.create(categoryModel);
                    numberOfCategories++;
                } else categoryModel = eponymousCategories.get(0);

                categoryDao.assignEmptyForeignCollection(categoryModel, "items");

                for (String itemKey : category.getSection("items").getKeys(false)) {
                    ConfigSection item = category.getSection("items").getSection(itemKey);

                    String id = itemKey.replace("-", ":");
                    if (Item.fromString(id).getId() == 0) continue;
                    double price = item.getDouble("price");

                    BuyableItem itemModel = new BuyableItem(Shop.getDefault(),
                            categoryModel, id, price);

                    categoryModel.items.add(itemModel);

                    numberOfItems++;

                    categoryModel.items.getDao()
                            .assignEmptyForeignCollection(itemModel, "discounts");

                    if (item.exists("discount")) {
                        double percent = item.getDouble("discount");
                        Discount discount = new Discount(itemModel, percent, -1); // infinite
                        itemModel.addDiscount(discount);
                    }
                }
            }

            Message.LOG_BUYABLE_ITEMS_IMPORTED.log(numberOfCategories, numberOfItems);

            return true;
        } catch (Exception e) { // return false in case of any exception
            // so that buyList.yml doesn't get deleted
            Trading.printException(e);
            return false;
        }
    }

    /**
     * Converts the list of sellable items from
     * YAML config format to database format
     *
     * @param sellableItems config to convert
     * @return whether sellList.yml can be safely deleted
     * without losing data
     */
    public static boolean convertSellableItems(Config sellableItems) {
        try {
            int numberOfItems = 0;

            for (String itemKey : sellableItems.getKeys(false)) {
                String itemId = itemKey.replace("-", ":");
                double price = sellableItems.getDouble(itemKey);

                SellableItem itemModel =
                        new SellableItem(Shop.getDefault(), itemId, price);

                Dao<SellableItem, Integer> itemDao =
                        DaoManager.createDao(Trading.getSource(), SellableItem.class);
                itemDao.create(itemModel);

                numberOfItems++;
            }

            Message.LOG_SELLABLE_ITEMS_IMPORTED.log(numberOfItems);

            return true;
        } catch (Exception e) {
            Trading.printException(e);
            return false;
        }
    }

    /**
     * Converts custom item name translations from
     * YAML config format to database format
     *
     * @param translations config to convert
     * @return whether customItemNames.yml can be
     * safely deleted without losing data
     */
    public static boolean convertTranslations(Config translations) {
        try {
            int importedTranslations = 0;
            int skippedTranslations = 0;

            for (String key : translations.getKeys(false)) {
                String itemId = key.replace("-", ":");
                String translation = translations.getString(key);

                Dao<Translation, Integer> translationDao =
                        DaoManager.createDao(Trading.getSource(), Translation.class);

                // if there is already a translation - do not override it
                if (translationDao.queryForEq("item_id", itemId).size() != 0) {
                    skippedTranslations++;
                    continue;
                }

                Translation translationModel = new Translation(itemId, translation);
                translationDao.create(translationModel);

                translations.remove(key);

                importedTranslations++;
            }

            if (skippedTranslations == 0) {
                Message.LOG_ALL_TRANSLATIONS_IMPORTED.log(importedTranslations);
                return true;
            } else {
                Message.LOG_IMPORT_SKIPPED_TRANSLATIONS.log(importedTranslations,
                        skippedTranslations);
                return false;
            }
        } catch (Exception e) {
            Trading.printException(e);
            return false;
        }
    }

}
