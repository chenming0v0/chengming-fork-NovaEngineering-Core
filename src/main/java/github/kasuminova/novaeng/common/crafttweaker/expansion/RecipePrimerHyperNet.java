package github.kasuminova.novaeng.common.crafttweaker.expansion;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils;
import github.kasuminova.novaeng.common.hypernet.NetNodeCache;
import github.kasuminova.novaeng.common.hypernet.NetNodeImpl;
import github.kasuminova.novaeng.common.hypernet.research.ResearchCognitionData;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import hellfirepvp.modularmachinery.common.integration.crafttweaker.RecipePrimer;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import stanhebben.zenscript.annotations.ZenExpansion;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

@ZenRegister
@ZenExpansion("mods.modularmachinery.RecipePrimer")
public class RecipePrimerHyperNet {

    /**
     * 为一个配方添加算力要求。
     * 用法：{@code requireComputationPoint(1.0F);}
     */
    @ZenMethod
    public static RecipePrimer requireComputationPoint(final RecipePrimer primer,
                                                       final float required)
    {
        if (Side.CLIENT.isClient()) {
            primer.addRecipeTooltip(
                    I18n.format("novaeng.hypernet.computation_point_required.tip",
                            NovaEngUtils.formatFLOPS(required))
            );
        }

        return primer.addCheckHandler(event -> {
            TileMultiblockMachineController ctrl = event.getController();
            NetNodeImpl node = NetNodeCache.getCache(ctrl, NetNodeImpl.class);
            if (node != null) {
                node.checkComputationPoint(event, required);
            }
        });
    }

    /**
     * 为一个配方添加研究认知要求。
     * 用法：{@code requireResearch("research_name_a", "research_name_b")}
     */
    @ZenMethod
    public static RecipePrimer requireResearch(final RecipePrimer primer,
                                               final String... researchNames)
    {
        return requireResearch(primer, Arrays.stream(researchNames)
                .map(RegistryHyperNet::getResearchCognitionData)
                .filter(Objects::nonNull)
                .toArray(ResearchCognitionData[]::new));
    }

    @ZenMethod
    public static RecipePrimer requireResearch(final RecipePrimer primer,
                                               final ResearchCognitionData... researchRequired)
    {
        if (Side.CLIENT.isClient()) {
            String researchTip = Arrays.stream(researchRequired)
                    .map(ResearchCognitionData::getTranslatedName)
                    .collect(Collectors.joining(", "));
            primer.addRecipeTooltip(I18n.format("novaeng.hypernet.research_required.tip", researchTip));
        }

        return primer.addCheckHandler(event -> {
            NetNodeImpl cache = NetNodeCache.getCache(event.getController(), NetNodeImpl.class);
            if (cache != null) {
                cache.checkResearch(event, researchRequired);
            }
        });
    }
}
