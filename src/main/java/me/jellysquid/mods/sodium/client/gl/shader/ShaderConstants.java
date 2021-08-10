package me.jellysquid.mods.sodium.client.gl.shader;

import java.util.*;

public class ShaderConstants {
    private final Map<String, String> defines;

    private ShaderConstants(Map<String, String> defines) {
        this.defines = defines;
    }

    public static ShaderConstants empty() {
        return new ShaderConstants(Collections.emptyMap());
    }

    public static Builder buildFrom(ShaderConstants constants) {
        ShaderConstants.Builder builder = ShaderConstants.builder();

        for (Map.Entry<String, String> str : constants.getEntries()) {
            builder.add(str.getKey(), str.getValue());
        }

        return builder;
    }

    public Iterable<Map.Entry<String, String>> getEntries() {
        return this.defines.entrySet();
    }

    public static ShaderConstants.Builder builder() {
        return new Builder();
    }

    public Collection<String> createDefines() {
        List<String> list = new ArrayList<>();

        for (Map.Entry<String, String> entry : this.defines.entrySet()) {
            if (entry.getValue() == null) {
                list.add("#define " + entry.getKey());
            } else {
                list.add("#define " + entry.getKey() + " " + entry.getValue());
            }
        }

        return list;
    }

    public static class Builder {
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

            this.constants.put(name, value);
        }

        public ShaderConstants build() {
            return new ShaderConstants(this.constants);
        }

        public void addAll(List<String> defines) {
            for (String value : defines) {
                this.add(value);
            }
        }
    }
}
