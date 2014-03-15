package nippon.kawauso.chiraura.messenger;

import nippon.kawauso.chiraura.lib.converter.TypeRegistry;

/**
 * @author chirauraNoSakusha
 */
final class RegistryInitializer {

    // インスタンス化防止。
    private RegistryInitializer() {}

    static TypeRegistry<Message> init(final TypeRegistry<Message> registry) {
        long id = 0L;
        registry.register(id++, TestMessage.class, TestMessage.getParser());
        registry.register(id++, FirstMessage.class, FirstMessage.getParser());
        registry.register(id++, FirstReply.class, FirstReply.getParser());
        registry.register(id++, SecondMessage.class, SecondMessage.getParser());
        registry.register(id++, SecondReply.class, SecondReply.getParser());

        registry.register(id++, PortCheckMessage.class, PortCheckMessage.getParser());
        registry.register(id++, PortCheckReply.class, PortCheckReply.getParser());
        registry.register(id++, PortErrorMessage.class, PortErrorMessage.getParser());

        registry.register(id++, KeyUpdateMessage.class, KeyUpdateMessage.getParser());
        return registry;
    }

}
