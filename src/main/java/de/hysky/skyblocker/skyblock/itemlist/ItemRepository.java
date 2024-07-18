package de.hysky.skyblocker.skyblock.itemlist;

import de.hysky.skyblocker.annotations.Init;
import de.hysky.skyblocker.config.SkyblockerConfigManager;
import de.hysky.skyblocker.skyblock.item.tooltip.ItemTooltip;
import de.hysky.skyblocker.skyblock.itemlist.recipes.SkyblockCraftingRecipe;
import de.hysky.skyblocker.skyblock.itemlist.recipes.SkyblockForgeRecipe;
import de.hysky.skyblocker.skyblock.itemlist.recipes.SkyblockMobDropRecipe;
import de.hysky.skyblocker.skyblock.itemlist.recipes.SkyblockRecipe;
import de.hysky.skyblocker.utils.ItemUtils;
import de.hysky.skyblocker.utils.NEURepoManager;
import io.github.moulberry.repo.data.*;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

public class ItemRepository {
    protected static final Logger LOGGER = LoggerFactory.getLogger(ItemRepository.class);

    private static final List<ItemStack> items = new ArrayList<>();
    private static final Map<String, ItemStack> itemsMap = new HashMap<>();
    private static final List<SkyblockRecipe> recipes = new ArrayList<>();
    private static boolean filesImported = false;

    @Init
    public static void init() {
        NEURepoManager.runAsyncAfterLoad(ItemStackBuilder::loadPetNums);
        NEURepoManager.runAsyncAfterLoad(ItemRepository::importItemFiles);
    }

    private static void importItemFiles() {
        NEURepoManager.NEU_REPO.getItems().getItems().values().forEach(ItemRepository::loadItem);
        NEURepoManager.NEU_REPO.getItems().getItems().values().forEach(ItemRepository::loadRecipes);

        items.sort((lhs, rhs) -> {
            String lhsInternalName = ItemUtils.getItemId(lhs);
            String lhsFamilyName = lhsInternalName.replaceAll(".\\d+$", "");
            String rhsInternalName = ItemUtils.getItemId(rhs);
            String rhsFamilyName = rhsInternalName.replaceAll(".\\d+$", "");
            if (lhsFamilyName.equals(rhsFamilyName)) {
                if (lhsInternalName.length() != rhsInternalName.length())
                    return lhsInternalName.length() - rhsInternalName.length();
                else return lhsInternalName.compareTo(rhsInternalName);
            }
            return lhsFamilyName.compareTo(rhsFamilyName);
        });
        filesImported = true;
    }

    private static void loadItem(NEUItem item) {
        try {
            ItemStack stack = ItemStackBuilder.fromNEUItem(item);
            items.add(stack);
            itemsMap.put(item.getSkyblockItemId(), stack);
        } catch (Exception e) {
            LOGGER.error("[Skyblocker Item Repo Loader] Failed to load item, please report this! Skyblock Id: {}", item.getSkyblockItemId(), e);
        }
    }

    private static void loadRecipes(NEUItem item) {
        for (NEURecipe recipe : item.getRecipes()) {
            switch (recipe) {
                case NEUCraftingRecipe craftingRecipe -> recipes.add(new SkyblockCraftingRecipe(craftingRecipe));
                case NEUForgeRecipe forgeRecipe -> recipes.add(new SkyblockForgeRecipe(forgeRecipe));
                case null, default -> {}
            }
        }
    }

    public static String getWikiLink(String neuId) {
        NEUItem item = NEURepoManager.NEU_REPO.getItems().getItemBySkyblockId(neuId);
        if (item == null || item.getInfo() == null || item.getInfo().isEmpty()) {
            return null;
        }

        List<String> info = item.getInfo();
        String wikiLink0 = info.getFirst();
        String wikiLink1 = info.size() > 1 ? info.get(1) : "";
        String wikiDomain = SkyblockerConfigManager.get().general.wikiLookup.officialWiki ? "https://wiki.hypixel.net" : "https://hypixel-skyblock.fandom.com";
        if (wikiLink0.startsWith(wikiDomain)) {
            return wikiLink0;
        } else if (wikiLink1.startsWith(wikiDomain)) {
            return wikiLink1;
        }
        return null;
    }

    public static List<SkyblockCraftingRecipe> getRecipesPmd(String neuId) {
        return List.of();
    }

    public static boolean filesImported() {
        return filesImported;
    }

    public static void setFilesImported(boolean filesImported) {
        ItemRepository.filesImported = filesImported;
    }

    public static List<ItemStack> getItems() {
        return items;
    }

    public static Stream<ItemStack> getItemsStream() {
        return items.stream();
    }

    /**
     * @param neuId the NEU item id gotten through {@link NEUItem#getSkyblockItemId()}, {@link ItemStack#getNeuName()}, or {@link ItemTooltip#getNeuName(String, String) ItemTooltip#getNeuName(String, String)}
     */
    @Nullable
    public static ItemStack getItemStack(String neuId) {
        return itemsMap.get(neuId);
    }

    @Deprecated
    public static Stream<SkyblockCraftingRecipe> getRecipesStreamOld() {
        return Stream.of();
    }

    @Deprecated
    public static Stream<SkyblockRecipe> getRecipesStream() {return recipes.stream(); }

    public static Stream<SkyblockRecipe> getRecipes(ItemStack stack) {
        return NEURepoManager.RECIPE_CACHE.getRecipes().getOrDefault(stack.getNeuName(), Set.of()).stream().map(ItemRepository::toSkyblockRecipe).filter(Objects::nonNull);
    }

    public static Stream<SkyblockRecipe> getUsages(ItemStack stack) {
        return NEURepoManager.RECIPE_CACHE.getUsages().getOrDefault(stack.getNeuName(), Set.of()).stream().map(ItemRepository::toSkyblockRecipe).filter(Objects::nonNull);
    }

    private static SkyblockRecipe toSkyblockRecipe(NEURecipe neuRecipe) {
        return switch (neuRecipe) {
            case NEUCraftingRecipe craftingRecipe -> new SkyblockCraftingRecipe(craftingRecipe);
            case NEUForgeRecipe forgeRecipe -> new SkyblockForgeRecipe(forgeRecipe);
            case NEUMobDropRecipe mobDropRecipe -> new SkyblockMobDropRecipe(mobDropRecipe);
            case null, default -> null;
        };
    }
}

