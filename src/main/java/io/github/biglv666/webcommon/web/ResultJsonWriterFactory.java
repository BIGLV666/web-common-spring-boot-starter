package io.github.biglv666.webcommon.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.util.ClassUtils;

import java.util.function.Supplier;

/**
 * Result 序列化器工厂：按类路径探测选择 Jackson 2 / Jackson 3 实现。
 *
 * <p>选择规则：</p>
 * <ol>
 *     <li>存在 Jackson 2 databind（Boot 3.x 默认）→ {@link Jackson2ResultJsonWriter}，
 *         行为与 0.2.0 起的契约完全一致；</li>
 *     <li>否则存在 Jackson 3 databind（Boot 4 默认）→ {@link Jackson3ResultJsonWriter}；</li>
 *     <li>两者皆无（极端裁剪）→ {@link MissingJacksonResultJsonWriter}，
 *         仅 String 返回包装失败并给出修复指引。</li>
 * </ol>
 *
 * <p>关键约束：装配层的方法签名不得出现任何 Jackson 类型——连
 * {@code ObjectProvider<ObjectMapper>} 这类泛型引用也不行，因为 Spring
 * 解析 Bean 方法依赖类型时会反射加载泛型实参，纯 Jackson 3 环境会因此
 * 抛 {@code TypeNotPresentException}。两代 Mapper 均按类名经
 * {@link ConfigurableListableBeanFactory} 延迟解析，且仅在命中分支内执行。</p>
 */
public final class ResultJsonWriterFactory {

    private static final String JACKSON2_OBJECT_MAPPER = "com.fasterxml.jackson.databind.ObjectMapper";

    private static final String JACKSON3_OBJECT_MAPPER = "tools.jackson.databind.ObjectMapper";

    private ResultJsonWriterFactory() {
    }

    /**
     * 自动装配入口：两代容器 Mapper 均按类名延迟解析，解析动作
     * 推迟到命中分支内执行，保证装配期零 Jackson 类加载。
     *
     * @param beanFactory Bean 工厂，用于按类名解析容器中的 Mapper
     * @return 选定的序列化器
     */
    public static ResultJsonWriter create(ConfigurableListableBeanFactory beanFactory) {
        return create(
                () -> resolveMapper(beanFactory, JACKSON2_OBJECT_MAPPER),
                () -> resolveMapper(beanFactory, JACKSON3_OBJECT_MAPPER));
    }

    /**
     * 可测试的选择入口：Mapper 均通过 Supplier 延迟获取，
     * 只有命中对应分支才会解析，避免在缺失库的环境触发类加载。
     *
     * @param jackson2MapperSupplier Jackson 2 容器 Mapper 提供者
     * @param jackson3MapperSupplier Jackson 3 容器 Mapper 提供者（元素为 Object，
     *                               实际类型由 {@link Jackson3ResultJsonWriter} 内部校验）
     * @return 选定的序列化器
     */
    static ResultJsonWriter create(Supplier<Object> jackson2MapperSupplier,
                                   Supplier<Object> jackson3MapperSupplier) {
        ClassLoader classLoader = ResultJsonWriterFactory.class.getClassLoader();
        if (ClassUtils.isPresent(JACKSON2_OBJECT_MAPPER, classLoader)) {
            Object mapper = jackson2MapperSupplier.get();
            return new Jackson2ResultJsonWriter(mapper != null ? (ObjectMapper) mapper : new ObjectMapper());
        }
        if (ClassUtils.isPresent(JACKSON3_OBJECT_MAPPER, classLoader)) {
            return new Jackson3ResultJsonWriter(jackson3MapperSupplier.get());
        }
        return new MissingJacksonResultJsonWriter();
    }

    /**
     * 按类名解析容器中的 Mapper。仅在类路径探测确认对应库存在的分支内执行，
     * 此处加载类是安全的；装配层的字节码全程不直接引用 Jackson 类型。
     *
     * @param beanFactory Bean 工厂
     * @param className   Mapper 全限定类名
     * @return 容器中的 Mapper 实例；容器未提供时返回 null
     */
    private static Object resolveMapper(ConfigurableListableBeanFactory beanFactory, String className) {
        try {
            Class<?> mapperClass = ClassUtils.forName(className, ResultJsonWriterFactory.class.getClassLoader());
            return beanFactory.getBeanProvider(ResolvableType.forClass(mapperClass)).getIfAvailable();
        } catch (ClassNotFoundException e) {
            // 类路径探测已确认存在，此处仅为编译期受检异常兜底；null 让实现类回退默认 Mapper
            return null;
        }
    }
}
