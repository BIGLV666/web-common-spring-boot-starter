package io.github.biglv666.webcommon.web;

import io.github.biglv666.webcommon.annotation.ErrorCodeScan;
import io.github.biglv666.webcommon.result.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.EnvironmentAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationAttributes;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AssignableTypeFilter;
import org.springframework.util.ClassUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * {@link ErrorCodeScan} 的注册器：配置解析期扫描基础包下实现
 * {@link ErrorCode} 的枚举类，将结果打包为 {@link ScannedErrorCodes}
 * Bean 注册进容器，供自动装配的 {@code ErrorCodeRegistry} 汇总校验。
 *
 * <p>本类不是普通 Bean，由 {@code @Import(ErrorCodeScanRegistrar.class)}
 * 触发，不应被业务代码直接引用。</p>
 */
public class ErrorCodeScanRegistrar
        implements ImportBeanDefinitionRegistrar, EnvironmentAware, ResourceLoaderAware, BeanClassLoaderAware {

    private static final Logger log = LoggerFactory.getLogger(ErrorCodeScanRegistrar.class);

    private Environment environment;

    private ResourceLoader resourceLoader;

    private ClassLoader classLoader;

    @Override
    public void setEnvironment(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Override
    public void setBeanClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    /**
     * 解析注解声明的扫描包，执行扫描，并把结果注册为
     * {@code ScannedErrorCodes} Bean（每个导入方一个，按导入类全名命名避免冲突）。
     *
     * @param metadata 导入方配置类的注解元数据
     * @param registry Bean 定义注册器
     */
    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        AnnotationAttributes attributes = AnnotationAttributes.fromMap(
                metadata.getAnnotationAttributes(ErrorCodeScan.class.getName()));
        Set<Class<? extends ErrorCode>> enumClasses = scan(resolveBasePackages(attributes, metadata));
        String beanName = ScannedErrorCodes.class.getName() + "#" + metadata.getClassName();
        BeanDefinition definition = BeanDefinitionBuilder
                .rootBeanDefinition(ScannedErrorCodes.class, () -> new ScannedErrorCodes(List.copyOf(enumClasses)))
                .getBeanDefinition();
        registry.registerBeanDefinition(beanName, definition);
        log.info("ErrorCodeScan 扫描完成: 注册 {} 个错误码枚举", enumClasses.size());
    }

    /**
     * 解析扫描基础包：basePackages 优先，其次 basePackageClasses 锚点类所在包，
     * 两者均空时回退到导入方配置类所在包。
     *
     * @param attributes 注解属性
     * @param metadata   导入方元数据
     * @return 待扫描的基础包名集合
     */
    private Set<String> resolveBasePackages(AnnotationAttributes attributes, AnnotationMetadata metadata) {
        Set<String> packages = new LinkedHashSet<>();
        if (attributes != null) {
            for (String basePackage : attributes.getStringArray("basePackages")) {
                if (basePackage != null && !basePackage.isBlank()) {
                    packages.add(basePackage);
                }
            }
            for (Class<?> anchor : attributes.getClassArray("basePackageClasses")) {
                packages.add(ClassUtils.getPackageName(anchor));
            }
        }
        if (packages.isEmpty()) {
            packages.add(ClassUtils.getPackageName(metadata.getClassName()));
        }
        return packages;
    }

    /**
     * 逐包扫描 ErrorCode 枚举实现。关闭默认过滤器后仅按「可赋值给 ErrorCode」
     * 匹配，枚举不是常规组件，这里放宽候选判定为「独立类」即可。
     *
     * @param basePackages 基础包名集合
     * @return 扫描到的错误码枚举类（按发现顺序去重）
     */
    private Set<Class<? extends ErrorCode>> scan(Set<String> basePackages) {
        Set<Class<? extends ErrorCode>> result = new LinkedHashSet<>();
        for (String basePackage : basePackages) {
            var provider = new ClassPathErrorCodeScanner(this.environment);
            provider.setResourceLoader(this.resourceLoader);
            for (BeanDefinition candidate : provider.findCandidateComponents(basePackage)) {
                String className = candidate.getBeanClassName();
                Class<?> clazz = ClassUtils.resolveClassName(className, this.classLoader);
                // 扫描器按接口签名匹配，可能命中普通实现类；本 starter 约定错误码必须是枚举
                if (!clazz.isEnum()) {
                    throw new IllegalStateException(
                            "@ErrorCodeScan 发现非枚举的 ErrorCode 实现: " + className + "，错误码必须声明为枚举");
                }
                result.add(clazz.asSubclass(ErrorCode.class));
            }
        }
        return result;
    }

    /**
     * 只匹配 ErrorCode 实现的类路径扫描器。
     */
    private static final class ClassPathErrorCodeScanner
            extends ClassPathScanningCandidateComponentProvider {

        private ClassPathErrorCodeScanner(Environment environment) {
            super(false, environment);
            addIncludeFilter(new AssignableTypeFilter(ErrorCode.class));
        }

        /**
         * 放宽默认候选判定：枚举无 public 构造器也不抽象，
         * 只要是独立顶层/静态嵌套类就允许进入候选集。
         */
        @Override
        protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
            return beanDefinition.getMetadata().isIndependent();
        }
    }
}
