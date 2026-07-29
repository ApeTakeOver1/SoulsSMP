package net.ape.soulssmp.api.soul;

import net.ape.soulssmp.api.SoulType;
import net.ape.soulssmp.managers.core.SoulManager;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

/**
 * Holds each Soul's content definition (abilities, ultimate, passives, info text).
 * Distinct from {@link SoulManager}, which tracks which Soul a given player is bound to.
 */
public class SoulRegistry {

    private final Map<SoulType, SoulDefinition> definitions = new EnumMap<>(SoulType.class);

    public void register(SoulDefinition definition) {
        definitions.put(definition.getType(), definition);
    }

    public SoulDefinition get(SoulType type) {
        return definitions.get(type);
    }

    public Collection<SoulDefinition> all() {
        return definitions.values();
    }
}
