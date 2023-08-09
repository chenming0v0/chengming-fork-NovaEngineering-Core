package github.kasuminova.novaeng.common.hypernet;

import crafttweaker.annotations.ZenRegister;
import github.kasuminova.mmce.common.event.recipe.FactoryRecipeTickEvent;
import github.kasuminova.mmce.common.event.recipe.RecipeCheckEvent;
import github.kasuminova.mmce.common.helper.IMachineController;
import github.kasuminova.mmce.common.upgrade.MachineUpgrade;
import github.kasuminova.novaeng.common.hypernet.upgrade.ProcessorModuleCPU;
import github.kasuminova.novaeng.common.hypernet.upgrade.ProcessorModuleRAM;
import github.kasuminova.novaeng.common.registry.RegistryHyperNet;
import github.kasuminova.novaeng.common.util.RandomUtils;
import hellfirepvp.modularmachinery.common.lib.RequirementTypesMM;
import hellfirepvp.modularmachinery.common.machine.IOType;
import hellfirepvp.modularmachinery.common.modifier.RecipeModifier;
import hellfirepvp.modularmachinery.common.tiles.TileUpgradeBus;
import hellfirepvp.modularmachinery.common.tiles.base.TileMultiblockMachineController;
import hellfirepvp.modularmachinery.common.util.MiscUtils;
import io.netty.util.internal.shaded.org.jctools.queues.SpscLinkedQueue;
import net.minecraft.nbt.NBTTagCompound;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenGetter;
import stanhebben.zenscript.annotations.ZenMethod;
import stanhebben.zenscript.annotations.ZenSetter;

import java.util.*;

@ZenRegister
@ZenClass("novaeng.hypernet.DataProcessor")
public class DataProcessor extends NetNode {
    private static final Map<TileMultiblockMachineController, DataProcessor> CACHED_DATA_PROCESSOR = new WeakHashMap<>();

    private final SpscLinkedQueue<Long> recentCalculation = new SpscLinkedQueue<>();

    private final DataProcessorType type;
    private int circuitDurability = 0;
    private int storedHU = 0;
    private boolean overheat = false;
    private float computationalLoad = 0;
    private float maxGeneration = 0;

    public DataProcessor(final TileMultiblockMachineController owner, final NBTTagCompound customData) {
        super(owner);
        this.type = RegistryHyperNet.getDataProcessorType(
                Objects.requireNonNull(owner.getFoundMachine()).getRegistryName().getPath()
        );

        readNBT(customData);
    }

    @ZenMethod
    public void onRecipeCheck(RecipeCheckEvent event) {
        if (centerPos == null || center == null) {
            event.setFailed("未连接至计算网络！");
            return;
        }

        if (overheat) {
            event.setFailed("处理器过热！");
            return;
        }

        if (circuitDurability < type.getCircuitDurability() * 0.05F) {
            event.setFailed("主电路板耐久过低，无法正常工作！");
            return;
        }

        Map<String, List<MachineUpgrade>> upgrades = owner.getFoundUpgrades();
        if (upgrades.isEmpty()) {
            event.setFailed("未找到处理器和内存模块！");
            return;
        }

        List<MachineUpgrade> flattened = MiscUtils.flatten(upgrades.values());
        if (ProcessorModuleRAM.filter(flattened).isEmpty()) {
            event.setFailed("至少需要安装一个内存模块！");
            return;
        }

        if (ProcessorModuleCPU.filter(flattened).isEmpty()) {
            event.setFailed("至少需要安装一个 CPU 或 GPU 模块！");
        }
    }

    @ZenMethod
    public void onWorkingTick(FactoryRecipeTickEvent event, long baseEnergyConsumption) {
        if (overheat) {
            event.setFailed(true, "处理器过热！");
            return;
        }

        long energyConsumption = 0;
        Long consumption;
        while ((consumption = recentCalculation.poll()) != null) {
            energyConsumption += consumption;
        }

        if (energyConsumption == 0) {
            event.getRecipeThread().removeModifier("energy");
        } else {
            float mul = (float) ((double) (energyConsumption + baseEnergyConsumption) / baseEnergyConsumption);
            event.getRecipeThread().addModifier("energy", new RecipeModifier(
                    RequirementTypesMM.REQUIREMENT_ENERGY,
                    IOType.INPUT, mul, 1, false
            ));
        }
    }

    @ZenMethod
    public void onMachineTick() {
        super.onMachineTick();

        if (!owner.isWorking()) {
            computationalLoad = 0;
        }

        if (owner.getTicksExisted() % 20 == 0) {
            maxGeneration = getComputationPointProvision(0xFFFFFF);
            writeNBT();
        }

        if (storedHU > 0) {
            storedHU -= Math.min(type.getHeatDistribution(), storedHU);
            if (storedHU <= 0) {
                overheat = false;
            }
            maxGeneration = getComputationPointProvision(0xFFFFFF);
            writeNBT();
        }
    }

    @Override
    public float requireComputationPoint(final float maxGeneration, final boolean doCalculate) {
        if (!isConnected() || center == null || !owner.isWorking()) {
            return 0F;
        }

        float generation = calculateComputationPointProvision(maxGeneration, doCalculate);

        if (doCalculate) {
            consumeCircuitDurability();
            doHeatGeneration(generation);
            writeNBT();
        }

        return generation;
    }

    private void consumeCircuitDurability() {
        if (owner.getTicksExisted() % 20 != 0) {
            return;
        }
        if (!(RandomUtils.nextFloat() <= (type.getCircuitConsumeChance() / getEfficiency()))) {
            return;
        }

        int min = type.getMinCircuitConsumeAmount();
        int max = type.getMaxCircuitConsumeAmount();

        circuitDurability -= Math.min(min + RandomUtils.nextInt(max - min), circuitDurability);
    }

    @ZenGetter("maxGeneration")
    public float getMaxGeneration() {
        return maxGeneration;
    }

    public float getEfficiency() {
        float overHeatPercent = getOverHeatPercent();
        return overHeatPercent >= 0.9F ? 1.0F - (overHeatPercent - 0.5F) * 2F : 1F;
    }

    public float getOverHeatPercent() {
        return overheat ? 1F : (float) storedHU / type.getOverheatThreshold();
    }

    public void doHeatGeneration(float computationPointGeneration) {
        storedHU += (int) (computationPointGeneration * 2);
        if (storedHU >= type.getOverheatThreshold()) {
            overheat = true;
        }
    }

    public float calculateComputationPointProvision(float maxGeneration, boolean doCalculate) {
        if (overheat || !owner.isWorking()) {
            return 0;
        }

        Map<String, List<MachineUpgrade>> upgrades = owner.getFoundUpgrades();
        List<MachineUpgrade> flattened = MiscUtils.flatten(upgrades.values());

        List<ProcessorModuleRAM> moduleRAMs = ProcessorModuleRAM.filter(flattened);
        if (moduleRAMs.isEmpty()) {
            return 0;
        }

        List<ProcessorModuleCPU> moduleCPUs = ProcessorModuleCPU.filter(flattened);
        if (moduleCPUs.isEmpty()) {
            return 0;
        }

        long totalEnergyConsumption = 0;
        float maxGen = maxGeneration * getEfficiency();

        float generationLimit = 0F;
        for (ProcessorModuleRAM ram : moduleRAMs) {
            generationLimit += ram.calculate(doCalculate, maxGen - generationLimit);
            if (doCalculate) {
                totalEnergyConsumption += (long) (ram.getEfficiency() * ram.getEnergyConsumption());
            }
        }

        float generated = 0F;
        for (final ProcessorModuleCPU cpu : moduleCPUs) {
            generated += cpu.calculate(doCalculate, generationLimit - generated);
            if (doCalculate) {
                totalEnergyConsumption += (long) (cpu.getEfficiency() * cpu.getEnergyConsumption());
            }
        }

        if (doCalculate) {
            computationalLoad = generated;
            recentCalculation.offer(totalEnergyConsumption);
        }

        return generated;
    }

    @ZenMethod
    public static DataProcessor from(final IMachineController machine) {
        TileMultiblockMachineController ctrl = machine.getController();
        return CACHED_DATA_PROCESSOR.computeIfAbsent(ctrl, v ->
                new DataProcessor(ctrl, ctrl.getCustomDataTag()));
    }

    @Override
    public void readNBT(final NBTTagCompound customData) {
        super.readNBT(customData);
        this.storedHU = customData.getInteger("storedHU");
        if (customData.hasKey("overheat")) {
            this.overheat = customData.getBoolean("overheat");
        }

        if (customData.hasKey("circuitDurability")) {
            this.circuitDurability = customData.getInteger("circuitDurability");
        } else {
            this.circuitDurability = type.getCircuitDurability();
        }

        this.computationalLoad = customData.getFloat("computationalLoad");
        this.maxGeneration = customData.getFloat("maxGeneration");
    }

    @Override
    public void writeNBT() {
        super.writeNBT();
        NBTTagCompound tag = owner.getCustomDataTag();
        tag.setInteger("storedHU", storedHU);
        tag.setBoolean("overheat", overheat);
        tag.setInteger("circuitDurability", circuitDurability);
        tag.setFloat("computationalLoad", computationalLoad);
        tag.setFloat("maxGeneration", maxGeneration);
    }

    @Override
    public float getComputationPointProvision(final float maxGeneration) {
        return calculateComputationPointProvision(maxGeneration, false);
    }

    @ZenGetter("computationalLoad")
    public float getComputationalLoad() {
        return computationalLoad;
    }

    @ZenGetter("type")
    public DataProcessorType getType() {
        return type;
    }

    @ZenGetter("circuitDurability")
    public int getCircuitDurability() {
        return circuitDurability;
    }

    @ZenSetter("circuitDurability")
    public void setCircuitDurability(final int circuitDurability) {
        this.circuitDurability = circuitDurability;
        writeNBT();
    }

    @ZenGetter("storedHU")
    public int getStoredHU() {
        return storedHU;
    }

    @ZenSetter("storedHU")
    public void setStoredHU(final int storedHU) {
        this.storedHU = storedHU;
    }

    @ZenGetter("overheat")
    public boolean isOverheat() {
        return overheat;
    }

    public static void clearCache() {
        CACHED_DATA_PROCESSOR.clear();
    }
}