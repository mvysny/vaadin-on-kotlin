package com.vaadin.starter.beveragebuddy.backend

import java.util.LinkedHashMap

internal object StaticData {

    private val MINERAL_WATER = "Mineral Water"
    private val SOFT_DRINK = "Soft Drink"
    private val COFFEE = "Coffee"
    private val TEA = "Tea"
    private val DAIRY = "Dairy"
    private val CIDER = "Cider"
    private val BEER = "Beer"
    private val WINE = "Wine"
    private val OTHER = "Other"

    /**
     * Maps beverage name to a beverage category.
     */
    val BEVERAGES: MutableMap<String, String> = LinkedHashMap()

    init {
        listOf("Evian",
                "Voss",
                "Veen",
                "San Pellegrino",
                "Perrier")
                .forEach { name -> BEVERAGES.put(name, MINERAL_WATER) }

        listOf("Coca-Cola",
                "Fanta",
                "Sprite")
                .forEach { name -> BEVERAGES.put(name, SOFT_DRINK) }

        listOf("Maxwell Ready-to-Drink Coffee",
                "Nescafé Gold",
                "Starbucks East Timor Tatamailau")
                .forEach { name -> BEVERAGES.put(name, COFFEE) }

        listOf("Prince Of Peace Organic White Tea",
                "Pai Mu Tan White Peony Tea",
                "Tazo Zen Green Tea",
                "Dilmah Sencha Green Tea",
                "Twinings Earl Grey",
                "Twinings Lady Grey",
                "Classic Indian Chai")
                .forEach { name -> BEVERAGES.put(name, TEA) }

        listOf("Cow's Milk",
                "Goat's Milk",
                "Unicorn's Milk",
                "Salt Lassi",
                "Mango Lassi",
                "Airag")
                .forEach { name -> BEVERAGES.put(name, DAIRY) }

        listOf("Crowmoor Extra Dry Apple",
                "Golden Cap Perry",
                "Somersby Blueberry",
                "Kopparbergs Naked Apple Cider",
                "Kopparbergs Raspberry",
                "Kingstone Press Wild Berry Flavoured Cider",
                "Crumpton Oaks Apple",
                "Frosty Jack's",
                "Ciderboys Mad Bark",
                "Angry Orchard Stone Dry",
                "Walden Hollow",
                "Fox Barrel Wit Pear")
                .forEach { name -> BEVERAGES.put(name, CIDER) }

        listOf("Budweiser",
                "Miller",
                "Heineken",
                "Holsten Pilsener",
                "Krombacher",
                "Weihenstephaner Hefeweissbier",
                "Ayinger Kellerbier",
                "Guinness Draught",
                "Kilkenny Irish Cream Ale",
                "Hoegaarden White",
                "Barbar",
                "Corsendonk Agnus Dei",
                "Leffe Blonde",
                "Chimay Tripel",
                "Duvel",
                "Pilsner Urquell",
                "Kozel",
                "Staropramen",
                "Lapin Kulta IVA",
                "Kukko Pils III",
                "Finlandia Sahti")
                .forEach { name -> BEVERAGES.put(name, BEER) }

        listOf("Jacob's Creek Classic Shiraz",
                "Chateau d’Yquem Sauternes",
                "Oremus Tokaji Aszú 5 Puttonyos")
                .forEach { name -> BEVERAGES.put(name, WINE) }

        listOf("Pan Galactic Gargle Blaster",
                "Mead",
                "Soma")
                .forEach { name -> BEVERAGES.put(name, OTHER) }
    }
}
