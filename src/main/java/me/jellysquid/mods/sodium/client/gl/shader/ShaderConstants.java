package me.jellysquid.mods.sodium.client.gl.shader;

import java.util.*;

public class ShaderConstants {
    private final List<String> defines;

    private ShaderConstants(List<String> defines) {
        this.defines = defines;
    }

    public static ShaderConstants empty() {
        return new ShaderConstants(Collections.emptyList());
    }

    public static Builder buildFrom(ShaderConstants constants) {
        ShaderConstants.Builder builder = ShaderConstants.builder();

        for (String str : constants.getDefineStrings()) {
            String[] parts = str.split(" ");
            builder.add(parts[1], parts[2]);
        }

        return builder;
    }

    public List<String> getDefineStrings() {
        return this.defines;
    }

    public static ShaderConstants.Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private static final String EMPTY_VALUE = "";

        private final HashMap<String, String> constants = new HashMap<>();

        private Builder() {

        }

        public void add(String name) {
            this.add(name, null);
        }

        public void add(String name, String value) {
            String prev = this.constants.get(name);

            if (prev != null) {
                throw new IllegalArgumentException("Constant " + name + " is already defined with value " + prev);
            }

            this.constants.put(name, value == null ? EMPTY_VALUE : value);
        }

        public ShaderConstants build() {
            List<String> defines = new ArrayList<>(this.constants.size());

            for (Map.Entry<String, String> entry : this.constants.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                if (value.isEmpty()) {
                    defines.add("#define " + key);
                } else {
                    defines.add("#define " + key + " " + value);
                }
            }

            return new ShaderConstants(Collections.unmodifiableList(defines));
        }

        public void addAll(List<String> defines) {
            for (String value : defines) {
                this.add(value);
            }
        }
    }
}
