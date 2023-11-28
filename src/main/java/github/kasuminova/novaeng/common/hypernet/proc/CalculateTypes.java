package github.kasuminova.novaeng.common.hypernet.proc;

import github.kasuminova.novaeng.common.crafttweaker.util.NovaEngUtils;
import net.minecraft.client.resources.I18n;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class CalculateTypes {
    public static final CalculateTypeIntricate INTRICATE = new CalculateTypeIntricate();
    public static final CalculateTypeLogic LOGIC = new CalculateTypeLogic();
    public static final CalculateTypeNeuron NEURON = new CalculateTypeNeuron();
    public static final CalculateTypeQbit QBIT = new CalculateTypeQbit();

    private static final Map<String, CalculateType> AVAILABLE_TYPES = new HashMap<>();

    static {
        registerType(INTRICATE);
        registerType(LOGIC);
        registerType(NEURON);
        registerType(QBIT);
    }

    public static void registerType(CalculateType type) {
        AVAILABLE_TYPES.put(type.typeName, type);
    }

    public static CalculateType getTypeByName(String typeName) {
        return AVAILABLE_TYPES.get(typeName);
    }

    public static Map<String, CalculateType> getAvailableTypes() {
        return Collections.unmodifiableMap(AVAILABLE_TYPES);
    }

    public static class CalculateTypeIntricate extends CalculateType {

        CalculateTypeIntricate() {
            super("intricate");
        }

        @SideOnly(Side.CLIENT)
        @Override
        public String format(final double value) {
            return I18n.format("novaeng.hypernet.calculate.type." + typeName, formatLogic(value));
        }

        static String formatLogic(double value) {
            if (value < 1D) {
                return NovaEngUtils.formatDouble(value * 1_000D, 1) + "G";
            }
            if (value < 1_000D) {
                return NovaEngUtils.formatDouble(value, 1) + "T";
            }
            if (value < 1_000_000D) {
                return NovaEngUtils.formatDouble(value / 1_000D, 1) + "P";
            }

            return NovaEngUtils.formatDouble(value / 1_000_000D, 1) + "E";
        }

    }

    public static class CalculateTypeLogic extends CalculateType {
        CalculateTypeLogic() {
            super("logic");
        }

        @SideOnly(Side.CLIENT)
        @Override
        public String format(final double value) {
            return I18n.format("novaeng.hypernet.calculate.type." + typeName, CalculateTypeIntricate.formatLogic(value));
        }

    }

    public static class CalculateTypeNeuron extends CalculateType {
        CalculateTypeNeuron() {
            super("neuron");
        }

        @Override
        @SideOnly(Side.CLIENT)
        public String format(final double value) {
            return I18n.format("novaeng.hypernet.calculate.type." + typeName, NovaEngUtils.formatDouble(value, 1));
        }
    }

    public static class CalculateTypeQbit extends CalculateType {
        CalculateTypeQbit() {
            super("qbit");
        }

        @Override
        @SideOnly(Side.CLIENT)
        public String format(final double value) {
            return I18n.format("novaeng.hypernet.calculate.type." + typeName, NovaEngUtils.formatDouble(value, 1));
        }
    }
}